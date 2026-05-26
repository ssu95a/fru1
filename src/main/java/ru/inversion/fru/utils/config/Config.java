package ru.inversion.fru.utils.config;

import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.utils.Checks;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

import java.util.*;
import java.util.function.Predicate;


public final class Config implements Configuration, AutoCloseable {

    /** */
    private static final class InstanceHolder {
        static final Config instance = make();
    }

    /** */
    public static Config instance()
    {
        return InstanceHolder.instance;
    }


    /*
    public enum Namespace {

        BOOT,
        SERVER,
        ALIAS,
        AUTH,
        ADMIN,
        METRICS,
        POOL,
        DS,
        TARGET;

        public String resolve(String key) {
            return name().toLowerCase(Locale.ROOT) + '.' + key;
        }
    }
    */

    private final List<ConfigSource> sources;

    /** */
    private Config( List<ConfigSource> sources ) {
        this.sources = sources;
    }

    /** */
    public static String normalizeKey(String key)
    {
        final String k = Checks.Require.text( key, "Config key").trim();

        for( int i = 0; i < k.length(); i++ )
        {
            char ch = k.charAt(i);
            if( ch < 0x20 || ch == 0x7F || Character.isWhitespace(ch) )
                throw new IllegalArgumentException("Config key contains forbidden whitespace/control chars: " + key);
        }

        return k;
    }

    /** */
    public static Config make() {

//        final String externalConfig = System.getProperty("xxi.properties.file");
//        final Path configFile = S.isNullOrEmpty(externalConfig) ? null : Paths.get(externalConfig).toAbsolutePath().normalize();

        final List<ConfigSource> src = new ArrayList<>(
            Arrays.asList (
                new ProcessorCacheSource(),
                new SystemPropertySource(null),
                FruEngineConfig.instance(),
                new EnvSource(null),
                new PreferencesSource(true),
                new PreferencesSource(false),
                new DefaultSource( new LinkedHashMap<>() )
            )
        );

//        if( configFile != null && Files.isRegularFile(configFile) )
//            src.add(new FilePropertiesSource(configFile));


        return new Config(src);
    }

    @Override
    public String getString(String key, boolean required) {

        key = normalizeKey(key);

        for( ConfigSource s : sources ) {
            Object v = s.get(key);
            if( v != null ) {
                if( !(v instanceof String) || !((String)v).isEmpty() )
                    return v.toString();
            }
        }

        if( required )
            throw new IllegalArgumentException("Missing config key: " + key);

        return null;
    }

    /** */
    @Override
    public <T> T get( String key, Class<T> typeClass, boolean require ) {

        key = normalizeKey(key);

        for( ConfigSource s : sources )
        {
            Object v = s.get(key);

            if (v != null) {
                try {
                    return TypeConverter.convert(v, typeClass);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to convert config value for key: " + key, e);
                }
            }
        }

        if( require )
            throw new IllegalArgumentException( "Missing config key: " + key );

        return null;
    }


    @Override
    public Map<String, Object> snapshot( String pf )
    {
        final String prefixFor = S.isNullOrEmpty(pf) ? null :  normalizeKey(pf);

        final Predicate<String> filter =
            prefixFor == null
                ? k -> true
                : k -> k != null && k.startsWith(prefixFor);

        final Map<String, Object> snapshot = new HashMap<>();
        final ListIterator<ConfigSource> it = sources.listIterator(sources.size());

        while (it.hasPrevious()) {
            ConfigSource cs = it.previous();
            cs.snapshotTo(filter, snapshot);
        }

        return Collections.unmodifiableMap(snapshot);
    }

    /** */
    @Override
    public void close() {
        sources.forEach(ConfigSource::close);
    }

}