package ru.inversion.fru.print.naltprn.cmd;

import ru.inversion.fru.print.altprint.ALTException;
import ru.inversion.fru.print.altprint.doc.formatted.StyleState;

import java.util.List;

/** */
public class AltCommand
{
    private final String name;
    private final String note;
    private AltMatrixData       matrixData;
    private AbstractGraphicData graphicData;

    private String cssStyleValue = null;
    private String cssStyleName  = null;

    /** */
    public String getCssStyleValue( ) {
        return cssStyleValue;
    }

    /** */
    public boolean isCssSupported ( )
    {
        return cssStyleValue != null;
    }

    /** */
    public String getCssStyleName ( ) {
        return cssStyleName;
    }


    /** */
    public static abstract class AbstractGraphicData
    {
        private final String commandString;

        public AbstractGraphicData(String commandString)
        {
            this.commandString = commandString;
        }

        public String getCommandString()
        {
            return this.commandString;
        }

        /** */
        public abstract void toCSStyle( StringBuilder sb, Object paramObject );

        /** */
        public abstract AltParameter<?>[] getParameters();

        public abstract StyleState applyTo( StyleState style, Object param );
    }


    /** */
    public static class AltGraphicData extends AbstractGraphicData
    {
        private final AltParameter<?>[] parameters;

        public AltGraphicData( String commandString, AltParameter<?>[] parameters)
        {
            super(commandString);
            this.parameters = parameters;
        }

        /** */
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            for( AltParameter<?> p : this.parameters ) {
                 p.toCSStyle(sb, paramObject );
            }
        }

        public AltParameter<?>[] getParameters()
        {
            return this.parameters;
        }

        public StyleState applyTo( StyleState style, Object param)
        {
            for( AltParameter<?> p : this.parameters) {
                style = p.applyTo( style, param );
            }

            return style;
        }
    }


    /** */
    public static class AltGraphicDataParam extends AbstractGraphicData
    {
        private final AltParameter<?>[] parameter = new AltParameter[1];

        public AltGraphicDataParam(String commandString, AltParameter<?> parameter)
        {
            super(commandString);
            this.parameter[0] = parameter;
        }

        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            this.parameter[0].toCSStyle(sb, paramObject );
        }

        /** */
        public AltParameter<?>[] getParameters()
        {
            return this.parameter;
        }

        /** */
        public StyleState applyTo( StyleState style, Object param )
        {
            return this.parameter[0].applyTo( style, param );
        }
    }


    /** */
    public static class AltMatrixData
    {
        private Object command;

        public AltMatrixData( Object command )
        {
            this.command = command;
        }

        /** */
        public String getPrinterCommand()
        {
            if( command instanceof String )
                return (String)command;

            List cmdList = (List)command;

            final StringBuilder sb = new StringBuilder();

            for( Object o : cmdList )
            {
                if( o instanceof String )
                    sb.append((String)o);
                else if( o instanceof AltCommand )
                    sb.append( ( (AltCommand)o).getMatrixData().getPrinterCommand() );
            }

            command = sb.toString();

            return (String) command;
        }
    }


    /** */
    public AltCommand(String name, String note)
    {
        this.name = name;
        this.note = note;
    }

    public String getName()
    {
        return this.name;
    }

    public String getNote()
    {
        return this.note;
    }

    /** Возвращает true, если команда реально меняет StyleState */
    public boolean isStyleChanging( ) {
        return graphicData != null;
    }

    /** */
    public AltMatrixData getMatrixData()
    {
        return this.matrixData;
    }

    /** */
    public AbstractGraphicData getGraphicData()
    {
        return this.graphicData;
    }

    /** */
    public void setMatrixData( List list )
    {
        this.matrixData = new AltMatrixData( list );
    }

    /** */
    public void setGraphicData( String command, AltParameter<?>[] parameters)
    {
        this.graphicData = (parameters.length == 1 ? new AltGraphicDataParam(command, parameters[0]) : new AltGraphicData( command, parameters));
    }

    /** */
    public StyleState applyTo( StyleState style, Object param)
    {
        return getGraphicData().applyTo(style, param);
    }

    /** */
    private String normalizeTagName( String name )
    {
        return name.toLowerCase().replace( '-','m' ).replace('_','-').replace( '+','p' ) + "-text";
    }

    /** */
    void toCSStyle( StringBuilder sb, Object paramObject )
    {
        AbstractGraphicData gd = getGraphicData();

        if( gd != null )
        {
            int l = (Integer) paramObject;

            final StringBuilder sbCss = new StringBuilder();
            gd.toCSStyle(sbCss, l + 1);

            if( sbCss.length() == 0 )
                return;

            if( l == 0 )
                sb.append(".code-area .text.").append( normalizeTagName(name) ).append(" {\n");

            sb.append( sbCss );

            if( l == 0 )
                sb.append("\n}\n");
        }
    }//end ALTCommand


    /** */
    public void makeCSStyle( )
    {
        try {

            StringBuilder sb = new StringBuilder();
            toCSStyle(sb, 0);

            if (sb.length() > 0) {
                this.cssStyleValue = sb.toString();
                this.cssStyleName = normalizeTagName(name);
            }
        } catch (Exception e) {
            throw new ALTException("Ошибка при установлении CSS в команду: " + this.name, e );
        }
    }


    /** Чтение команды из потока
    public static AltCommand readCommand(Reader reader, int maxRemaining ) throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        int ch;
        int nRead = 0;

        while( nRead < maxRemaining && ( ch = reader.read() ) != -1 )
        {
            nRead++;

            if( ch == '`' )
                break;

            sb.append( (char) ch );
        }

        final String s = sb.toString().trim();

        if( S.isNullOrEmpty(s) )
            return null;

//        final ALTCommandDict d = ALTSettings.INSTANCE().commandDict();
//
//        return d.getCommand( s, true );
        return null;
    }
     */
}
