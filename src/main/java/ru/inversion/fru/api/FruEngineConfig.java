package ru.inversion.fru.api;

import ru.inversion.fru.api.exceptions.FruCommandLineException;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.inversion.fru.api.FruEngine.csDos866;
import static ru.inversion.fru.api.FruEngine.csWin1251;

/** */
public class FruEngineConfig {

    /** */
    private int copies = 1;

    /** */
    private boolean allowEditing = false;

    /** */
    private boolean lightView = false;

    /** */
    private Charset charset = csWin1251;

    /** */
    private int printerIndex = 0;

    /** */
    private boolean silentMode = false;

    /** */
    private Path datFile = null;

    /** */
    private Path fruFile = null;

    /** */
    private Path outFile = null;


    private FruEngineConfig()
    { }

    public int getCopies() {
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
        return outFile;
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
            System.out.println("WARN: bad value option, '-' char only!");
            return;
        }

        char ch = option.charAt(1);

        switch( ch ) {
            case 'C':
                config.copies = Integer.parseInt(option.substring(2));
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
            case 'P':
                config.printerIndex = Integer.parseInt( option.substring(2) );
            break;
            case 'S':
                config.silentMode = true;
            break;
        }

    }

    /** */
    public static FruEngineConfig fromCommandLine( String[] args )
    {
        if( args.length < 3 ) {
            throw new FruCommandLineException("Отсутствуют обязательные параметры в командной строке.");
        }

        FruEngineConfig config = new FruEngineConfig();

        for( String s : args ) {

            if( s.startsWith("-") )
                parseOption( config, s );
            else
            {
                final Path file = Paths.get(s);

                if (config.datFile == null)
                    config.datFile = file;
                else if (config.fruFile == null)
                    config.fruFile = file;
                else
                    config.outFile = file;
            }
        }

        return config;
    }
}
