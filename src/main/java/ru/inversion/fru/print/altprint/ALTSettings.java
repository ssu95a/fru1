package ru.inversion.fru.print.altprint;


import ru.inversion.fx.form.FileChooserDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.prefs.Preferences;


public class ALTSettings
{
    public static final String INI_FILE_NAME = "ALTPRNT5.INI";

    private static ALTSettings instance;

    private DefSettings defSettings = new DefSettings();
    private ALTCommandDict commandDict;
    private File altprnt5File;

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
            final FileChooserDialog fcd = new FileChooserDialog("ALTPRNT5.INI");
            fcd.setTitle("Выберите файл с параметрами ALTPRNT5.INI");
            file = fcd.showOpenDialog(null);

            if( file != null )
                Preferences.userRoot().node("ALTPRINT").put("PATH_ALTPRINT", file.getAbsolutePath() );
        }

        return file;
    }

    /** */
    private static ALTSettings loadIniFile()
            throws ALTException
    {
        ALTSettings altSettings = null;

        try
        {
            File fileName = getINIFilePath();

            if( fileName == null )
                throw new FileNotFoundException( "Путь до файла ALTPRNT5.INI не задан" );

            ALTPrnt5Ini iniFile = ALTPrnt5Ini.loadFile(fileName);

            ALTPrnt5Ini.INISection logSection = iniFile.getSection("ALTPRINTLOG");

            ALTLog.configure((logSection != null) && (logSection.getParameter("ALTPRINT_LOG", "NO").compareToIgnoreCase("YES") == 0));

            altSettings = new ALTSettings();
            altSettings.commandDict  = ALTCommandDict.load( iniFile );

            altSettings.altprnt5File = fileName;
        }
        catch (Exception ex)
        {
            throw new ALTException("���������� ��������� ��������� �� ����� �������� ALTPRNT5.INI", ex);
        }
        return altSettings;
    }

    /** */
    public static ALTSettings INSTANCE()
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
                ALTLog.tech_error("��������� ����� �� ���������", ex);
                //ALTApp.APP().exit();
            }
        }
        return instance;
    }

    public DefSettings defSetting()
    {
        return this.defSettings;
    }

    public ALTCommandDict commandDict()
    {
        return this.commandDict;
    }

    public File getINIFileName()
    {
        return this.altprnt5File;
    }
}
