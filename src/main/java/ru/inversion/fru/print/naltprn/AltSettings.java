package ru.inversion.fru.print.naltprn;

import javafx.stage.FileChooser;
import ru.inversion.fru.print.altprint.ALTException;
import ru.inversion.fru.print.altprint.ALTLog;
import ru.inversion.fru.print.altprint.PrintSettings;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/** */
public class AltSettings
{
    public static final String INI_FILE_NAME = "ALTPRNT5.INI";

    private static AltSettings instance;

    private final PrintSettings defSettings = new PrintSettings();

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

        try
        {
            String INIFileName = null;

            for( int i = 0; (INIFileName == null) && (i < 2); i++ )
            {
                Preferences preferences = i == 0 ? Preferences.userRoot() : Preferences.systemRoot();

                if( preferences.nodeExists("ALTPRINT") )
                    INIFileName = preferences.node("ALTPRINT").get("PATH_ALTPRINT", null);
            }

            if( INIFileName != null )
                file = new File(INIFileName);
        }
        catch( Exception ex )
        {
            ALTLog.tech_info("Поиск в реестре пути до файла ALTPRNT5.INI", ex );
        }

        if( file == null || !file.exists() )
        {
            final FileChooser fcd = new FileChooser();
            fcd.setTitle("Выберите файл с параметрами ALTPRNT5.INI");
            file = fcd.showOpenDialog(null);

            if( file != null )
                Preferences.userRoot().node("ALTPRINT").put("PATH_ALTPRINT", file.getAbsolutePath() );
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
        catch (Exception ex)
        {
            throw new ALTException("Ошибка при разборе ALTPRNT5.INI", ex);
        }
        return altSettings;
    }

    /** */
    public static AltSettings INSTANCE()
    {
        if( instance == null )
        {
            ALTLog.tech_info("init: ALTSettings", null);

            try
            {
                instance = loadIniFile();
            }
            catch (ALTException ex)
            {
                ALTLog.tech_error("Ошибка при загрузке файла с настройками", ex);
                throw ex;
                //ALTApp.APP().exit();
            }
        }
        return instance;
    }

    /** */
    public PrintSettings defSetting()
    {
        return this.defSettings;
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
        name = name.replaceAll("[^A-Za-zА-Яа-яЁё]", "").toLowerCase();
        return printerMap.getOrDefault( name, Boolean.FALSE );
    }
}
