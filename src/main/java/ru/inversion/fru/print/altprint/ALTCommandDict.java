package ru.inversion.fru.print.altprint;

import java.util.*;

public class
ALTCommandDict
{
    public static final char COMMAND_SYMBOL = '`';
    public static final String COMMAND_STRING = "`";
    private static final String SECTION_COMMANDS = "Commands";
    private static final String SECTION_MATRIX = "CodeText";
    private static final String SECTION_GRAPHIC = "CodeGraphincs";

    private Map<String, ALTCommand> commandMap = new LinkedHashMap();

    private ALTInitCommand initCommand = null;

    public Collection<ALTCommand> getCommandList()
    {
        return this.commandMap.values();
    }

    public ALTInitCommand getInitCommand()
    {
        return this.initCommand;
    }

    public ALTCommand getCommand(String name, boolean add)
    {
        ALTCommand command = this.commandMap.get(name);

        if( command == null && add )
        {
            ALTLog.info("Команда не найдена в словаре: Команда " + name);
            command = new ALTCommand(name, null);
            this.commandMap.put( command.getName(), command );
        }
        return command;
    }

    public ALTCommand getCommand(String name)
    {
        return this.commandMap.get(name);
    }

    /** */
    public String getCSSStylesList()
    {
        final StringBuilder sbCss = new StringBuilder();

        commandMap.values().stream().filter(ALTCommand::isCssSupported).forEach(v->sbCss.append(v.getCssStyleValue()) );

        if( sbCss.length() > 0 )
            return sbCss.toString();

        return null;
    }

    /** */
    public static ALTCommandDict load(ALTPrnt5Ini ini)
            throws ALTException
    {
        try
        {
            ALTCommandDict dict = new ALTCommandDict();

            ALTPrnt5Ini.INISection section = ini.getSection("Commands");
            if( section == null )
            {
                //ALTLog.warning("� INI ����� ��� ������ � ��������� ������");
            }
            else
            {
                for( ALTPrnt5Ini.INIParameter p : section.getParameterList()) {
                     dict.commandMap.put(p.getName(), new ALTCommand(p.getName(), p.getValue()));
                }
                dict.initCommand = new ALTInitCommand("INIT", null);

                dict.commandMap.put("INIT", dict.initCommand);
            }

            section = ini.getSection("CodeText");

            if( section == null ) {
                ALTLog.warning("� INI ����� ��� ������ � ��������� ��� ���������� ��������");
            } else {
                loadMatrix(section, dict);
            }

            section = ini.getSection("CodeGraphincs");

            if (section == null) {
                ALTLog.warning("� INI ����� ��� ������ � ��������� ��� ������������ ��������");
            } else {
                loadGraphic(section, dict);
            }
            return dict;
        }
        catch (Exception ex)
        {
            throw new ALTException("������ ��� �������� ������ �� INI", ex);
        }
    }

    private static void loadMatrix(ALTPrnt5Ini.INISection section, ALTCommandDict dict)
            throws ALTException
    {
        try
        {
            String commandName  = null;
            String commandValue = null;

            int ix = 0;
            int ix2 = 0;

            ALTCommand command = null;

            for( ALTPrnt5Ini.INIParameter p : section.getParameterList() )
            {
                command      = dict.getCommand( p.getName(), true);
                commandValue = p.getValue();

                ix = commandValue.indexOf('`');
                if( ix == -1 )
                {
                    command.setMatrixData( commandValue, commandValue, null );
                }
                else
                {
                    ix2 = commandValue.indexOf('`', ix + 1);

                    if( ix2 == -1 )
                    {
                        ALTLog.warning("Не правильный формат команда матричного принтера. Команда игнорируется. Команда " + commandValue );
                    }
                    else
                    {
                        commandName  = commandValue.substring(ix + 1, ix2);
                        commandValue = commandValue.substring(ix2 + 1);

                        command.setMatrixData( p.getValue(), commandValue, dict.getCommand( commandName, true));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new ALTException("Ошибка при загрузке команд матричного принтера", ex);
        }
    }

    /** */
    private static void loadGraphic( ALTPrnt5Ini.INISection section, ALTCommandDict dict ) throws ALTException
    {
        try {

            final List<ALTParameter<?>> parameterList = new ArrayList<>();

            ALTParameter<?> parameter = null;

            ALTCommand command = null;

            int ix = 0;

            for( ALTPrnt5Ini.INIParameter p : section.getParameterList() )
            {
                String commandName  = p.getName();
                String commandValue = p.getValue();

                parameterList.clear();

                for( String paramInfo : commandValue.split(";") )
                {
                    ix = paramInfo.indexOf('=');

                    if( ix == -1 ) {
                        ALTLog.warning("Не правильный формат параметра графической команды в файле. Параметр игнорируется. Параметр: " + paramInfo);
                    }
                    else
                    {
                        String paramName = paramInfo.substring (0, ix);
                        String paramValue = paramInfo.substring(ix + 1);

                        parameter = ALTParameter.createParameter( paramName, paramValue, dict );

                        if( parameter != null)
                            parameterList.add(parameter);
                    }
                }

                command = dict.getCommand( commandName, true);
                command.setGraphicData(p.getValue(), parameterList.toArray( new ALTParameter[parameterList.size()]) );
            }

            dict.commandMap.values().forEach( ALTCommand::makeCSStyle );

        }
        catch( Exception ex ) {
            throw new ALTException( "Ошибка при загрузке графической части команды", ex);
        }
    }
}
