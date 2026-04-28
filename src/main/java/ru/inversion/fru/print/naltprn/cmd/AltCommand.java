package ru.inversion.fru.print.naltprn.cmd;

import ru.inversion.fru.print.altprint.ALTException;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;

import java.io.ByteArrayOutputStream;
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
    public static class AltMatrixData {
        private Object command;
        private boolean resolving;

        public AltMatrixData(Object command) {
            this.command = command;
        }

        public synchronized byte[] getPrinterCommand() {

            if( command instanceof byte[] )
                return (byte[]) command;


            if( resolving )
                throw new IllegalStateException("Cyclic matrix command reference detected");

            resolving = true;

            try {

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                if (command instanceof List)
                {
                    List<?> cmdList = (List<?>) command;

                    for (Object part : cmdList) {
                        writePart(out, part);
                    }
                } else {
                    writePart(out, command);
                }

                command = out.toByteArray();
                return (byte[]) command;
            }
            finally {
                resolving = false;
            }
        }

        private static void writePart( ByteArrayOutputStream out, Object part) {

            if( part == null ) {
                return;
            }

            if (part instanceof byte[]) {
                byte[] bytes = (byte[]) part;
                out.write(bytes, 0, bytes.length);
                return;
            }

            if (part instanceof String) {
                byte[] bytes = MatrixCommandBytes.compile((String) part);
                out.write(bytes, 0, bytes.length);
                return;
            }

            if (part instanceof AltCommand) {
                AltCommand cmd = (AltCommand) part;
                if (cmd.getMatrixData() == null) {
                    return;
                }

                byte[] bytes = cmd.getMatrixData().getPrinterCommand();
                out.write(bytes, 0, bytes.length);
                return;
            }
            throw new IllegalArgumentException( "Unsupported matrix command part: " + part.getClass() );
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
