package ru.inversion.fru.api;

import ru.inversion.fru.api.exceptions.FruCommandLineException;

import javax.print.attribute.standard.Copies;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.utils.U;

import static ru.inversion.fru.api.FruEngine.csDos866;
import static ru.inversion.fru.api.FruEngine.csWin1251;

/** */
public class FruEngineConfig {

    private static final Logger log = LoggerFactory.getLogger(FruEngineConfig.class);

    public enum GenerateModeEnum {

        //G[D|F|P] Generate to Display, File or Printer

        Display('D'),
        File('F'),
        Printer('P');

        private final char ch;

        GenerateModeEnum( char ch) {
            this.ch = ch;
        }

        /** */
        public static GenerateModeEnum of( char ch )
        {
            switch ( ch ) {
                case 'D': return Display;
                case 'F': return File;
                case 'P': return Printer;
            }
            throw new NoSuchElementException("GenerateModeEnum for " + ch );
        }

    }

    /** */
    private Copies copies = null;

    /** */
    private boolean allowEditing = false;

    /** */
    private boolean lightView = false;

    /** */
    private Charset charset = csWin1251;

    /** */
    private int printerIndex = -1;

    /** */
    private boolean silentMode = false;

    /** */
    private Path datFile = null;

    /** */
    private Path fruFile = null;

    /** */
    private Path outFile = null;

    /** */
    private GenerateModeEnum generateMode;

    /** */
    private boolean outExplicitlySet = false;

    private FruEngineConfig()
    { }

    public Copies getCopies() {
        return copies;
    }

    public boolean isAllowEditing() {
        return allowEditing;
    }

    public boolean isLightView() {
        return lightView;
    }

    public Charset getCharset() {
        return charset;
    }

    public int getPrinterIndex() {
        return printerIndex;
    }

    public boolean isSilentMode() {
        return silentMode;
    }

    public Path getDatFile() {
        return datFile;
    }

    public Path getFruFile() {
        return fruFile;
    }

    public Path getOutFile() {
        return U.nvl( outFile, datFile );
    }

    public boolean isOem()
    {
        return charset == csDos866;
    }

    public boolean useFru()
    {
        return fruFile != null;
    }

    public GenerateModeEnum getGenerateMode() {
        return generateMode;
    }

    public boolean isOutExplicitlySet() {  return outExplicitlySet; }

    static private FruEngineConfig instance;

    /** */
    public static FruEngineConfig instance() {

        if( instance == null )
            throw new IllegalStateException( "FruEngineConfig is not initialized yet. instance is null." );

        return instance;
    }

    /**

     pap_0.exe [options] <report file with cursor data> [<ufs file>]

     [options]

     -C<num> Copies
     -E Allow editing
     -G[D|F|P] Generate to Display, File or Printer
     -L Light view
     -O OEM encoding
     -P<idx> Printer index
     -S Silent mode
    */

    private static void parseOption( FruEngineConfig config, String option ) {

        if( option.length() < 2 ) {
            log.warn("Bad option value: '{}' (only '-' provided)", option);
            return;
        }

        char ch = option.charAt(1);

        switch( ch ) {
            case 'C':
                config.copies = new Copies( Integer.parseInt(option.substring(2)) );
            break;
            case 'E':
                config.allowEditing = true;
            break;
            case 'L':
                config.lightView = true;
            break;
            case 'O':
                config.charset = csDos866;
            break;
            case 'U':
                config.charset = StandardCharsets.UTF_8;
            break;
            case 'P':
                config.printerIndex = Integer.parseInt( option.substring(2) );
            break;
            case 'S':
                config.silentMode = true;
            break;
            case 'G':
                config.generateMode = GenerateModeEnum.of( option.charAt(2) );
            break;
        }

    }

    /** */
    private static String normalizeFru( String s ) {

        if( s.length() > 3 )
        {
            String s1 = s.substring( s.length() - 4 );

            if (".UFS".equalsIgnoreCase( s1 ))
            {
                String r = s.substring( 0, s.length() - 4 ) + ".fru";
                log.info("Файл '{}' пришел с расширением .ufs, меняем на .fru - {}", s, r );
                return r;
            }
        }

        return s;
    }

    /** */
    public static FruEngineConfig fromCommandLine( String[] args )
    {
        if( args.length < 2 )
            throw new FruCommandLineException("Отсутствуют обязательные параметры в командной строке.");

        FruEngineConfig config = new FruEngineConfig();

        for( String s : args ) {

            if( s.startsWith("-") )
                parseOption( config, s );
            else
            {
                final Path file = Paths.get( normalizeFru(s) );

                if (config.datFile == null)
                    config.datFile = file;
                else if (config.fruFile == null)
                    config.fruFile = file;
                else
                    config.outFile = file;
            }
        }

        if( config.datFile == null )
            throw new FruCommandLineException("Не задано имя файла с данными для отчета");

        if( !Files.exists(config.datFile) || !Files.isRegularFile(config.datFile) || !Files.isReadable(config.datFile) )
            throw new FruCommandLineException("Переданное имя файла с данными не является файлом или не существует!", new NoSuchFileException( config.datFile.getFileName().toString()) );

        config.outExplicitlySet = config.outFile != null;

        if( config.outFile == null && config.useFru() )
        {
            try {
                config.outFile = Files.createTempFile( "alt", "fru" );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            /*
            if( config.generateMode == GenerateModeEnum.File )
            {
                String fileName = config.datFile.getFileName().toString();
                int index = fileName.lastIndexOf('.');
                if (index > 0)
                    config.outFile = config.datFile.resolveSibling(fileName.substring(0, index) + ".txt");
                else
                    config.outFile = config.datFile.resolveSibling(fileName + ".txt");
            }
            else {
                try {
                    config.outFile = Files.createTempFile("alt", "fru");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            */
        }

        FruEngineConfig.instance = config;

        return config;
    }

    void normalizeOutFile() throws IOException {

        if( !isOutExplicitlySet() )
        {
            if( Boolean.getBoolean("ru.inversion.alt.store_src_data") )
            {
                Path srcFile = datFile.getParent().resolve( datFile.getFileName() + ".src" );
                Files.copy( datFile, srcFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING );
            }

            Files.copy( outFile, datFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING );
            outFile = datFile;
        }

    }


}
