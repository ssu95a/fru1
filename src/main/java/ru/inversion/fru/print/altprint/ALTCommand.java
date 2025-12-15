package ru.inversion.fru.print.altprint;

import java.io.IOException;
import java.io.Writer;

/** */
public class ALTCommand
{
    private final String name;
    private final String note;
    private ALTMatrixData       matrixData;
    private AbstractGraphicData graphicData;

    private String cssStyleValue = null;
    private String cssStyleName  = null;

    /** */
    public String getCssStyleValue( ) {
        return cssStyleValue;
    }
    /** */
    public boolean isCssSupported()
    {
        return cssStyleValue != null;
    }
    /** */
    public String getCssStyleName() {
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
        public abstract void toPrintParam(ALTDocPrintable.PrintParameters paramPrintParam, Object paramObject);

        public abstract ALTParameter<?>[] getParameters();

        public abstract void dump(Writer paramWriter) throws IOException;
    }


    /** */
    public static class ALTGraphicData extends ALTCommand.AbstractGraphicData
    {
        private final ALTParameter<?>[] parameters;

        public ALTGraphicData( String commandString, ALTParameter<?>[] parameters)
        {
            super(commandString);
            this.parameters = parameters;
        }

        /** */
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            for( ALTParameter<?> p : this.parameters ) {
                 p.toCSStyle(sb, paramObject );
            }
        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            for (ALTParameter<?> p : this.parameters) {
                p.toPrintParam(printParam, param);
            }
        }

        public ALTParameter<?>[] getParameters()
        {
            return this.parameters;
        }

        public void dump(Writer writer)
                throws IOException
        {
            for (ALTParameter<?> p : this.parameters) {
                p.dump(writer);
            }
        }
    }


    /** */
    public static class ALTGraphicDataParam extends ALTCommand.AbstractGraphicData
    {
        private final ALTParameter<?>[] parameter = new ALTParameter[1];

        public ALTGraphicDataParam(String commandString, ALTParameter<?> parameter)
        {
            super(commandString);
            this.parameter[0] = parameter;
        }

        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            this.parameter[0].toCSStyle(sb, paramObject );
        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            this.parameter[0].toPrintParam(printParam, param);
        }

        public ALTParameter<?>[] getParameters()
        {
            return this.parameter;
        }

        public void dump(Writer writer)
                throws IOException
        {
            this.parameter[0].dump(writer);
        }
    }


    /** */
    public static class ALTMatrixData
    {
        final private ALTCommand command;
        final private String     printerCommand;

        public ALTMatrixData(String command)
        {
            this( command, null );
        }

        public ALTMatrixData( String command, ALTCommand parent )
        {
            this.printerCommand = command;
            this.command = parent;
        }

        public ALTCommand getCommand()
        {
            return this.command;
        }

        /** */
        public String getPrinterCommand()
        {
            return command == null || command.matrixData == null ? this.printerCommand : command.matrixData.printerCommand + this.printerCommand;
        }
    }

    /** */
    public ALTCommand(String name, String note)
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

    public ALTMatrixData getMatrixData()
    {
        return this.matrixData;
    }

    public AbstractGraphicData getGraphicData()
    {
        return this.graphicData;
    }

    /** */
    public void setMatrixData( String printerCommand, ALTCommand parent )
    {
        this.matrixData = new ALTMatrixData( printerCommand, parent );
    }

    /** */
    public void setGraphicData(String command, ALTParameter<?>[] parameters)
    {
        this.graphicData = (parameters.length == 1 ? new ALTGraphicDataParam(command, parameters[0]) : new ALTGraphicData(command, parameters));
    }

    /** */
    public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
    {
        getGraphicData().toPrintParam(printParam, param);
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
        StringBuilder sb = new StringBuilder();
        toCSStyle( sb, 0 );

        if( sb.length() > 0 )
        {
            this.cssStyleValue = sb.toString();
            this.cssStyleName  = normalizeTagName(name);
        }
    }
}
