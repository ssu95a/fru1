package ru.inversion.fru.print.naltprn;

import ru.inversion.fru.print.altprint.ALTException;
import ru.inversion.fru.print.altprint.ALTLog;
import ru.inversion.utils.Pair;
import ru.inversion.utils.ini.IniFileEvent;
import ru.inversion.utils.ini.IniFileEventReader;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** */
public class AltPrnt5Ini {

    public static class IniParameter {

        private final String name;
        private final String value;

        public IniParameter( String name, String value ) {
            this.name  = name;
            this.value = value;
        }

        public String getName()  {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        /** */
        private Map<String, String> parseForMap() {

            final Map<String, String> values = new HashMap<>();

            int ix;

            for( String paramInfo : value.split(";") ) {
                ix = paramInfo.indexOf('=');

                if (ix == -1)
                    ALTLog.warning("Не правильный формат параметра графической команды в файле. Параметр игнорируется. Параметр: " + name);
                else {
                    String paramName  = paramInfo.substring(0, ix);
                    String paramValue = paramInfo.substring(ix + 1);

                    values.put(paramName, paramValue);
                }
            }//end for

            return values;
        }

        /** */
        public Map<String, String> parseForGraphic() {
            return parseForMap();
        }

        /** */
        public List<String> parseForMatrix() {

            int ix = value.indexOf('`');

            if (ix == -1) {
                return Collections.singletonList(value);
            }
            else
            {
                final List<String> values = new ArrayList<>();

                int start = 0;

                for( int i = 0; i < value.length(); i++ )
                {
                    char c = value.charAt(i);

                    if( c == '`' && i > 0 )
                    {
                        values.add( value.substring(start, i + 1) );
                        start = i + 1;
                    }
                }

                if( start < value.length() )
                    values.add( value.substring(start) );

                return values;
            }
        }

        /** */
        public Map<String, String> parseForPrinter() {
            return parseForMap();
        }
    }

    /** */
    public static class IniSection {

        private final String name;
        private Map<String, IniParameter> parameterMap = new LinkedHashMap();

        public IniSection(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public Collection<IniParameter> getParameterList() {
            return this.parameterMap.values();
        }

        public void setParameter( String parameter, String value)
        {
            IniParameter item = this.parameterMap.get(parameter);

            if (item == null) {
                item = new IniParameter(parameter, value);
                this.parameterMap.put(item.getName(), item);
            } else
                throw new IllegalStateException("Дублирование параметра '" + parameter + "' в секции '" + name + "'");
        }

        public String getParameter(String name) {
            IniParameter item = this.parameterMap.get(name);
            if (item != null) {
                return item.getValue();
            }
            return null;
        }

        public String getParameter(String name, String defaultValue) {
            String value = getParameter(name);
            if (value == null)
                value = defaultValue;

            return value;
        }
    }

    private final Map<String, IniSection> sectionMap = new LinkedHashMap<>();


    /** */
    public IniSection getSection( String name )
    {
        IniSection section = this.sectionMap.get(name);

        if( section == null )
        {
            section = new IniSection(name);
            this.sectionMap.put(section.getName(), section);
        }
        return section;
    }


    /** */
    public Map<String,String> getPrinterMap( )
    {
        IniSection section = this.sectionMap.get( "DriverRef" );

        if( section == null )
            throw new IllegalStateException("Нет секции 'DriverRef'. Возможно файл не тот");
            // logger.info("Нет принтеров для производителя " + company );

        return
            section.parameterMap.entrySet().stream().collect(
                Collectors.toMap( Map.Entry::getKey, e -> e.getValue().value )
            );
    }

    /** */
    public List< Pair<String, Map<String,String>> > getGraphicCommands( )
    {
        IniSection section = this.sectionMap.get( "CodeGraphincs" );

        if( section == null )
            // logger.info("Нет принтеров для производителя " + company );
            return Collections.emptyList();

        return
                section.parameterMap.entrySet().stream().map(new Function<Map.Entry<String, IniParameter>, Pair<String, Map<String,String>>>() {
                    @Override
                    public Pair<String, Map<String,String>> apply(Map.Entry<String, IniParameter> e ) {
                        return Pair.makePair( e.getKey(), e.getValue().parseForGraphic() );
                    }
                }).collect( Collectors.toList() );
    }

    /** */
    public List< Pair<String, List<String>> > getMatrixCommands( )
    {
        IniSection section = this.sectionMap.get( "CodeText" );

        if( section == null )
            // logger.info("Нет команд для принтера " + printer );
            return Collections.emptyList();

        return
                section.parameterMap.entrySet().stream().map(new Function<Map.Entry<String, IniParameter>, Pair<String, List<String>>>() {
                    @Override
                    public Pair<String, List<String>> apply(Map.Entry<String, IniParameter> e ) {
                        return Pair.makePair( e.getKey(), e.getValue().parseForMatrix() );
                    }
                }).collect( Collectors.toList() );
    }

    /** */
    public static AltPrnt5Ini loadFile(File file)
    {
        final AltPrnt5Ini iniFile = new AltPrnt5Ini();

        AltPrnt5Ini.IniSection currentSection = null;

        try( IniFileEventReader r = IniFileEventReader.newBuilder().iniFile(file).semicolonPartOfValue(true).build() )
        {
            for (IniFileEvent e : r)
            {
                if (e.type() == IniFileEvent.Type.Section) {
                    currentSection = iniFile.getSection(e.value());
                } else if (e.type() == IniFileEvent.Type.Parameter) {
                    if (currentSection != null)
                        currentSection.setParameter(e.key(), e.value());
                }
            }
        } catch (Exception ex) {
            throw new ALTException("Ошибка при разборе INI фала " + file, ex);
        }

        return iniFile;
    }
}