package ru.inversion.fru.print.naltprn.cmd;

import ru.inversion.fru.print.altprint.ALTException;
import ru.inversion.fru.print.altprint.ALTLog;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;
import ru.inversion.utils.Pair;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;

import java.math.BigDecimal;

import static ru.inversion.fru.print.naltprn.cmd.AltParameterTypeEnum.*;


public abstract class AltParameter<T>
{
    private final T value;
    private final AltParameterTypeEnum type;

    /** */
    protected AltParameter( AltParameterTypeEnum type, T value)
    {
        this.type  = type;
        this.value = value;
    }

    /** */
    public T getValue()
    {
        return this.value;
    }

    /** */
    public AltParameterTypeEnum getType()
    {
        return this.type;
    }

    /** */
    public void toCSStyle( StringBuilder sb, Object paramObject ) { }

    /** */
    public StyleState applyTo( StyleState style, Object param )
    {
        return style.toBuilder().build();
    }

    /** */
    public String toString()
    {
        return getType().toString() + " : " + getValue().toString();
    }


    /** */
    public static class CommandParameter extends AltParameter< Pair<AltCommand, Object> >
    {
        private CommandParameter( Pair<AltCommand, Object> value)
        {
            super( FRU_COMMAND, value );
        }

        public AltCommand getCommand()
        {
            return getValue().first;
        }

        public Object getParam()
        {
            return getValue().second;
        }

        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            getCommand().toCSStyle( sb, paramObject);
        }

        public String toString()
        {
            return getType().toString() + " : " + getCommand().getName() + ", param: " + getParam();
        }

        /** */
        public StyleState applyTo(StyleState style, Object param)
        {
            try
            {
                return getCommand().applyTo( style, U.nvl( param, this.getParam() ) );
            }
            catch (Exception ex)
            {
                throw new RuntimeException( "cdm: " + getCommand().getName(), ex);
            }
        }
    }


    /** */
    public static class FontParameter extends AltParameter<String>
    {
        /** */
        private FontParameter( String fontName )
        {
            super( FONT_NAME, fontName );
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            sb.append("-fx-font-family:").append( getValue() ).append(" !important; ");
        }

        @Override
        public StyleState applyTo( StyleState style, Object param )
        {
            return style.toBuilder().fontName( getValue() ).build();
        }
    }


    /** */
    public static class FontSizeParameter extends AltParameter<Integer>
    {
        private FontSizeParameter(Integer fontSize)
        {
            super(FONT_SIZE,fontSize);
        }

        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            sb.append("-fx-font-size:").append( getValue() ).append(" !important;");
        }

        @Override
        public StyleState applyTo( StyleState style, Object param )
        {
            return style.toBuilder().fontSize( getValue() ).build();
        }
    }

    private static class FontBoldParameter extends AltParameter<Boolean>
    {
        private FontBoldParameter( Boolean isBold)
        {
            super(BOLD,isBold);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            if( getValue() )
                sb.append("-fx-font-weight:800 !important; ");
        }

        @Override
        public StyleState applyTo(StyleState style, Object param) {
            boolean on = param == null ? getValue() : Boolean.TRUE.equals(param);
            return style.toBuilder().bold(on).build();
        }
    }

    private static final FontBoldParameter g_boldOn  = new FontBoldParameter(true );
    private static final FontBoldParameter g_boldOff = new FontBoldParameter(false);


    /** */
    private static class FontUnderlineParameter
            extends AltParameter<Boolean>
    {
        private FontUnderlineParameter(Boolean isUnderline)
        {
            super(UNDERLINE,isUnderline);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            if( getValue() )
                sb.append("-fx-underline:true !important; ");
        }

        public StyleState applyTo(StyleState style, Object param)
        {
            boolean on = Boolean.TRUE.equals(param); return style.toBuilder().underline(on).build();
        }
    }

    private static final FontUnderlineParameter g_underLineOn  = new FontUnderlineParameter(true );
    private static final FontUnderlineParameter g_underLineOff = new FontUnderlineParameter(false);


    /** */
    private static class FontItalicParameter
            extends AltParameter<Boolean>
    {
        private FontItalicParameter(Boolean isItalic)
        {
            super(ITALIC,isItalic);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            if( getValue() )
                sb.append("-fx-font-style:italic !important; ");

        }

        /** */
        public StyleState applyTo(StyleState style, Object param)
        {
            boolean on = Boolean.TRUE.equals(param); return style.toBuilder().italic(on).build();
        }
    }

    private static final FontItalicParameter g_italicOn  = new FontItalicParameter(true );
    private static final FontItalicParameter g_italicOff = new FontItalicParameter(false);


    /** */
    public static class LeftIndentParameter
            extends AltParameter<Float>
    {
        private LeftIndentParameter(Float indentSize)
        {
            super( LEFT, indentSize );
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        { }
        /** */
        public StyleState applyTo(StyleState style, Object param)
        {
            return style.toBuilder().leftIndent( getValue() ).build();
        }
    }

    /** */
    public static class UpperIndentParameter
            extends AltParameter<Float>
    {
        private UpperIndentParameter(Float indentSize)
        {
            super( UP, indentSize );
        }
        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        { }

        /** */
        public StyleState applyTo( StyleState style, Object param)
        {
            return style.toBuilder().upperIndent( getValue() ).build();
        }
    }


    /** */
    public static class PageOrientationParameter extends AltParameter<OrientationRequested>
    {
        private PageOrientationParameter(OrientationRequested v)
        {
            super( ORIENTATION, v );
        }

        public StyleState applyTo(StyleState style, Object param)
        { return style; }
    }

    private static final PageOrientationParameter g_landscape = new PageOrientationParameter(OrientationRequested.LANDSCAPE);
    private static final PageOrientationParameter g_portrait  = new PageOrientationParameter(OrientationRequested.PORTRAIT );

    /** */
    static class SpaceAfterParameter extends AltParameter<Pair<Float, Float>>
    {
        public SpaceAfterParameter(Pair<Float, Float> p)
        {
            super(SPACE_AFTER,p);
        }

        public Float getFirst()
        {
            return getValue().first;
        }

        public Float getSecond()
        {
            return getValue().second;
        }

        public StyleState applyTo( StyleState style, Object param)
        {
            try
            {
                float p1 = ( param == null ? getFirst() : TypeConverter.convert( param, BigDecimal.class ).floatValue() );
                float p2 = getSecond();

                float result = p1 * 72.0F / p2;

                return style.toBuilder().spaceAfter(result).build();
            }
            catch (Exception ex)
            {
                throw new RuntimeException( "prm: " + getType().toString(), ex);
            }
        }

        @Override
        public void toCSStyle( StringBuilder sb, Object param ) {

            float p1 = ( param == null ? getFirst() : ((Integer)param).floatValue() );
            float p2 = getSecond();

            float pt = p1 * 72.0F / p2;
            sb.append( String.format( "line-height: %.4fpt !important;", pt) );
        }
    }


    /** */
    public static class CopiesParameter extends AltParameter<Copies>
    {
        private CopiesParameter(Copies copies)
        {
            super(COPIES,copies);
        }
    }


    /** */
    private static class PageEndParameter extends AltParameter<Integer>
    {
        public PageEndParameter()
        {
            super(PAGE_END,null);
        }
    }

    /** */
    private static class LineFeedParameter extends AltParameter<Integer>
    {
        public LineFeedParameter()
        {
            super(LF,null);
        }
    }


    public static final AltParameter<Integer> g_pageEnd  = new PageEndParameter ();
    public static final AltParameter<Integer> g_lineFeed = new LineFeedParameter();

    public static AltParameter<?> createParameter( String name, String value, AltCommandDict dict)
            throws ALTException
    {
        AltParameter<?> parameter = null;
        try
        {
            AltParameterTypeEnum type = AltParameterTypeEnum.fromString(name);

            if (type == null)
            {
                if( "LANDSCAPE".equalsIgnoreCase(name) )
                    return g_landscape;
                if( "PORTRAIT".equalsIgnoreCase(name) )
                    return g_portrait;

                return new CommandParameter( Pair.makePair( dict.getCommand(name, true), value) );
            }

            switch (type)
            {
                case FONT_NAME:
                    parameter = new FontParameter(value);
                    break;
                case FONT_SIZE:
                    parameter = new FontSizeParameter(Integer.parseInt(value));
                    break;
                case ITALIC:
                    parameter = ( value == null || value.compareToIgnoreCase("YES") == 0 ) ? g_italicOn : g_italicOff;
                    break;
                case BOLD:
                    parameter = ( value == null || value.compareToIgnoreCase("YES")  == 0 ) ? g_boldOn : g_boldOff;
                    break;
                case UNDERLINE:
                    parameter = ( value == null || value.compareToIgnoreCase("YES") == 0  )? g_underLineOn : g_underLineOff;
                    break;
                case ORIENTATION:
                    parameter = value.compareToIgnoreCase("Portrait") == 0 ? g_portrait : g_landscape;
                    break;
                case LEFT:
                    parameter = new LeftIndentParameter( Float.parseFloat(value) );
                    break;
                case UP:
                    parameter = new UpperIndentParameter( Float.parseFloat(value) );
                    break;
                case COPIES:
                    parameter = new CopiesParameter( new Copies(Integer.parseInt(value)) );
                    break;
                case SPACE_AFTER: {

                    String p1 = null; String p2 = null;

                    int ix = value.indexOf('/');

                    if (ix == -1)
                    {
                        ALTLog.warning("Не правильный формат для параметра вертикального отступа: " + value);
                        return null;
                    }

                    p1 = value.substring(0, ix ).trim();
                    p2 = value.substring(ix + 1).trim();

                    if( p1.charAt(0) == 'n' )
                        p1 = null;

                    parameter = new SpaceAfterParameter( Pair.makePair(p1 == null ? null : Float.parseFloat(p1), Float.parseFloat(p2)) );

                }
                break;
                case FRU_COMMAND: {
                    String cmd = value.replace('`', ' ').trim();
                    Object param = null;

                    int ix = cmd.indexOf(',');
                    if (ix != -1) {
                        param = Float.parseFloat(cmd.substring(ix + 1));
                        cmd = cmd.substring(0, ix);
                    }
                    parameter = new CommandParameter( Pair.makePair( dict.getCommand(cmd, true), param) );
                }
                break;
                case PAGE_END:
                    parameter = g_pageEnd;
                    break;
                case LF:
                    parameter = g_lineFeed;
                    break;
            }

            if( parameter == null )
                ALTLog.warning("Не возможно обработать параметр команды: " + name);

        }
        catch (Exception ex)
        {
            throw new ALTException("Не возможно обработать параметр команды: " + name + " = " + value, ex);
        }
        return parameter;
    }
}
