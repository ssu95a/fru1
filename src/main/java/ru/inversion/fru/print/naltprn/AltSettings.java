package ru.inversion.fru.print.naltprn;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.ALTException;
import ru.inversion.fru.print.altprint.ALTLog;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;
import ru.inversion.utils.S;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/** */
public class AltSettings
{
    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    public static final String INI_FILE_NAME = "ALTPRNT5.INI";

    private static AltSettings instance;

    private AltCommandDict commandDict;

    private Map<String,Boolean> printerMap;

    private Path altPrnt5File;

    /** */
    private static File getINIFilePath()
    {
        File file = new File(INI_FILE_NAME);

        if( file.exists() )
            return file;

        file = null;

        for( int i = 0; (file == null) && (i < 3); i++ )
        {
            try {

                String iniDir = null;

                if( i == 2 )
                    iniDir = System.getProperty("PATH_ALTPRINT");
                else {
                    Preferences preferences = i == 0 ? Preferences.userRoot() : Preferences.systemRoot();

                    if( preferences.nodeExists("ALTPRINT") ) {
                        iniDir = preferences.node("ALTPRINT").get("PATH_ALTPRINT", null);
                    }
                }

                if( !S.isNullOrEmpty( iniDir ) )
                    file = new File ( iniDir, INI_FILE_NAME );
            }
            catch( Exception ex ) {
                logger.trace("Ошибка при поиске в реестре пути до файла ALTPRNT5.INI", ex );
            }
        }

        if( file == null || !file.exists() || !file.isFile() )
        {
            file = AltPrintFileChooser.chooseAltPrint();

            if( file != null )
                Preferences.userRoot().node("ALTPRINT").put( "PATH_ALTPRINT", file.getParent() );
        }

        return file;
    }


    /** */
    private static AltSettings loadIniFile( ) throws ALTException
    {
        AltSettings altSettings = null;

        try
        {
            File fileName = getINIFilePath();

            if( fileName == null )
                throw new FileNotFoundException( "Путь до файла ALTPRNT5.INI не задан" );

            AltPrnt5Ini iniFile = AltPrnt5Ini.loadFile( fileName );

            AltPrnt5Ini.IniSection logSection = iniFile.getSection("ALTPRINTLOG");

            ALTLog.configure((logSection != null) && (logSection.getParameter("ALTPRINT_LOG", "NO").compareToIgnoreCase("YES") == 0));

            altSettings = new AltSettings();
            altSettings.commandDict = AltCommandDict.load( iniFile );
            altSettings.printerMap  = iniFile.getPrinterMap().entrySet().stream().collect( Collectors.toMap(Map.Entry::getKey, e -> "CodeText".equalsIgnoreCase( e.getValue() )));
            altSettings.altPrnt5File= fileName.toPath();
        }
        catch( Exception ex ) {
            throw new ALTException("Ошибка при разборе ALTPRNT5.INI", ex );
        }
        return altSettings;
    }

    /** */
    public static AltSettings INSTANCE()
    {
        if( instance == null )
        {
            logger.debug("init: ALTSettings" );
            instance = loadIniFile();
        }
        return instance;
    }

    /** */
    public AltCommandDict commandDict()
    {
        return this.commandDict;
    }

    /** */
    public Path getINIFileName()
    {
        return this.altPrnt5File;
    }

    /** */
    public boolean isMatrixPrinter( String name )
    {
        return printerMap.getOrDefault( name, Boolean.FALSE );
    }
}
