package ru.inversion.fru.print.naltprn.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fru.print.altprint.*;
import ru.inversion.fru.print.naltprn.AltPrnt5Ini;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import java.util.*;
import java.util.function.Consumer;

/** Словарь команд для принтера Альтпринтер 5 */
public class AltCommandDict
{
    private static final Logger log = LoggerFactory.getLogger(AltCommandDict.class);

    public static final char   COMMAND_SYMBOL  = '`';
//    public static final String COMMAND_STRING  = "`";
    private static final String SECTION_COMMANDS = "Commands";
    private static final String SECTION_MATRIX   = "CodeText";
    private static final String SECTION_GRAPHIC  = "CodeGraphincs";

    private final Map<String, AltCommand> commandMap = new LinkedHashMap<>();

    private AltInitCommand initCommand = null;

    /** */
    public Collection<AltCommand> getCommandList()
    {
        return this.commandMap.values();
    }

    /** */
    public AltInitCommand getInitCommand( )
    {
        return this.initCommand;
    }

    /** */
    public AltPrintPageConfig.Builder getInitAltPrintPageConfig()
    {
        AltPrintPageConfig.Builder b = AltPrintPageConfig.builder();
        if( initCommand != null )
            initCommand.initPrintPageConfig( b );
        return b;
    }

    /** */
    public AltCommand getCommand( String name, boolean add )
    {
        AltCommand command = this.commandMap.get( name );

        if( command == null )
        {
            if( PageEndCommand.isPageEnd (name) )
            {
                return PageEndCommand.instance;
            }

            if( LineFeedCommand.isLineFeed(name) )
            {
                return LineFeedCommand.instance;
            }

            if( add )
            {
                log.warn("Команда не найдена в словаре: Команда {}", name);

                command = new AltCommand( name, null );
                this.commandMap.put( command.getName(), command );
            }
        }
        return command;
    }

    /** */
    public Optional<AltParameter<?>> resolveCommand( String str )
    {
        if( S.isNullOrEmpty(str) )
            return Optional.empty();

        String cmd;
        String prm = null;

        if( S.contains( str, ',', '=' ) )
        {
            String[] as = str.split("[,=]");

            if( as.length > 1 ) {
                cmd = as[0];
                prm = as[1];
            }
            else
                cmd = as[0];
        }
        else
            cmd = str;

        AltParameter<?> parameter = AltParameter.createParameter( cmd, prm, this, true );

        if( parameter == null )
            return Optional.empty();

        return Optional.of(parameter);
    }

    /** */
    public AltCommand getCommand( String name )
    {
        return this.commandMap.get( name );
    }

    /** */
    public String getCSSStylesList()
    {
        final StringBuilder sbCss = new StringBuilder();

        commandMap.values().stream().filter(AltCommand::isCssSupported).forEach(v->sbCss.append(v.getCssStyleValue()) );

        if( sbCss.length() > 0 )
            return sbCss.toString();

        return null;
    }

    /** */
    public static AltCommandDict load( AltPrnt5Ini ini ) throws ALTException
    {
        try {

            final AltCommandDict dict = new AltCommandDict();

            AltPrnt5Ini.IniSection section = ini.getSection( SECTION_COMMANDS );

            if( section == null )
            {
                log.warn("В INI файле нет секции с описанием команд: {}", SECTION_COMMANDS );
            }
            else
            {
                for( AltPrnt5Ini.IniParameter p : section.getParameterList() )
                     dict.commandMap.put( p.getName(), new AltCommand( p.getName(), p.getValue() ));

                dict.initCommand = new AltInitCommand("INIT", null );
                dict.commandMap.put( "INIT", dict.initCommand );
            }

            final List< Pair<String, Map<String, String>> > graphicCommands = ini.getGraphicCommands();

            graphicCommands.forEach( new Consumer<Pair<String, Map<String, String>>>() {
                @Override
                public void accept( Pair< String, Map<String, String>> cmdData ) {

                    final List< AltParameter<?> > parameterList = new ArrayList<>();

                    cmdData.second.entrySet().forEach(new Consumer<Map.Entry<String, String>>() {
                        @Override
                        public void accept(Map.Entry<String, String> e) {
                            AltParameter<?> parameter = AltParameter.createParameter( e.getKey(), e.getValue(), dict, false );
                            if( parameter != null )
                                parameterList.add( parameter );
                        }
                    });

                    AltCommand command = dict.getCommand( cmdData.first, true );
                    command.setGraphicData( null, parameterList.toArray( new AltParameter[0]) );
                }
            });

            final List<Pair<String, List<String>>> matrixCommands = ini.getMatrixCommands();
            matrixCommands.forEach( new Consumer<Pair<String, List<String>>>() {
                @Override
                public void accept( Pair<String, List<String> > cmdData )
                {
                    final List<Object> parameterList = new ArrayList<>();

                    for( String s : cmdData.second ) {

                        if( s.charAt(0) == COMMAND_SYMBOL && S.lastChar(s) == COMMAND_SYMBOL )
                        {
                            s = s.substring( 1, s.length() - 1 );
                            parameterList.add( dict.getCommand(s,true) );
                        }
                        else
                            parameterList.add(s);
                    }//end for
                    AltCommand command = dict.getCommand( cmdData.first, true );
                    command.setMatrixData( parameterList );
                }
            });

            dict.commandMap.values().forEach( AltCommand::makeCSStyle );

            return dict;
        }
        catch( Exception ex )
        {
            throw new ALTException( "Ошибка при загрузке словаря команд", ex );
        }
    }

}
