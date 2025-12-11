package ru.inversion.fru.print.altprint;

import ru.inversion.utils.ini.IniFileEvent;
import ru.inversion.utils.ini.IniFileEventReader;

import java.io.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


public class ALTPrnt5Ini
{
    public static class INIFileException
            extends Exception
    {
        public INIFileException(String message, Throwable cause)
        {
            super(cause);
        }

        public INIFileException(String message)
        {
            super();
        }
    }

    public static class INIParameter
    {
        private String name;
        private String value;

        public INIParameter() {}

        public INIParameter(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        public String getName()
        {
            return this.name;
        }

        public String getValue()
        {
            return this.value;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        public void write( Writer out ) throws IOException
        {
            out.write(this.name);out.write(61);out.write(this.value);out.write(10);
        }
    }

    public static class INISection
    {
        private final String name;
        private Map<String, ALTPrnt5Ini.INIParameter> parameterMap = new LinkedHashMap();

        public INISection(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }

        public Collection<ALTPrnt5Ini.INIParameter> getParameterList()
        {
            return this.parameterMap.values();
        }

        public void setParameter( String name, String value )
        {
            ALTPrnt5Ini.INIParameter item = this.parameterMap.get(name);
            if (item == null)
            {
                item = new ALTPrnt5Ini.INIParameter(name, value);
                this.parameterMap.put(item.getName(), item);
            }
            else
            {
                item.setValue(value);
            }
        }

        public String getParameter(String name)
        {
            ALTPrnt5Ini.INIParameter item = (ALTPrnt5Ini.INIParameter)this.parameterMap.get(name);
            if (item != null) {
                return item.getValue();
            }
            return null;
        }

        public String getParameter(String name, String defaultValue)
        {
            String value = getParameter(name);
            if (value == null) {
                value = defaultValue;
            }
            return value;
        }

        /** */
        public void write( Writer out )
                throws IOException
        {
            out.write(10);out.write(91);out.write(this.name);out.write(93);out.write(10);
            for (ALTPrnt5Ini.INIParameter item : this.parameterMap.values()) {
                item.write(out);
            }
        }
    }

    private final Map<String, INISection> sectionMap = new LinkedHashMap();

    public void saveFile(String fileName)
            throws ALTPrnt5Ini.INIFileException
    {
        Writer fw = null;
        try
        {
            fw = new BufferedWriter(new FileWriter(fileName));
            save(fw); return;
        }
        catch (IOException ex)
        {
            throw new INIFileException( "������ ��� ������ ������ � ���� " + fileName, ex);
        }
        finally
        {
            if (fw != null) {
                try
                {
                    fw.close();
                }
                catch (IOException ex) {}
            }
        }
    }

    public void save(Writer out)
            throws IOException
    {
        for (INISection section : this.sectionMap.values()) {
            section.write(out);
        }
    }

    public INISection getSection(String name)
    {
        INISection section = this.sectionMap.get(name);
        if (section == null)
        {
            section = new INISection(name);
            this.sectionMap.put(section.getName(), section);
        }
        return section;
    }

    public void setParameter(String sectionName, String paramName, String paramValue)
    {
        getSection(sectionName).setParameter(paramName, paramValue);
    }

    public String getParameter(String sectionName, String parameterName)
    {
        INISection section = (INISection)this.sectionMap.get(sectionName);
        if (section != null) {
            return section.getParameter(parameterName);
        }
        return null;
    }

    public Collection<INIParameter> getParameterList(String sectionName)
    {
        INISection section = (INISection)this.sectionMap.get(sectionName);
        if (section != null) {
            return section.getParameterList();
        }
        return null;
    }

    /** */
    public static ALTPrnt5Ini createEmpty()
    {
        return new ALTPrnt5Ini();
    }

    /** */
    public static ALTPrnt5Ini loadFile(File file) throws ALTPrnt5Ini.INIFileException
    {
        final ALTPrnt5Ini iniFile = new ALTPrnt5Ini();
        INISection currentSection = null;
        try( IniFileEventReader r = IniFileEventReader.newBuilder().iniFile(file).semicolonPartOfValue(true).build() ) {

            for( IniFileEvent e : r ) {

                 if( e.type() == IniFileEvent.Type.Section ) {
                     currentSection = iniFile.getSection(e.value());
                 }
                 else
                     if( e.type() == IniFileEvent.Type.Parameter )
                     {
                         if( currentSection != null )
                             currentSection.setParameter( e.key(), e.value() );
                     }
            }
        }
        catch( Exception ex ) {
            throw new INIFileException( "Ошибка при разборе INI фала " + file, ex );
        }

        return iniFile;
        /*
        try( FileReader fr = new FileReader(fileName) ) {
            return load(fr);
        } catch (IOException ex) {
            throw new INIFileException( "Ошибка при разборе INI фала " + fileName, ex );
        }
        */
    }
}
