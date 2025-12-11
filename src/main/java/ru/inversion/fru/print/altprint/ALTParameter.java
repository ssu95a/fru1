package ru.inversion.fru.print.altprint;

import ru.inversion.utils.Pair;

import java.io.IOException;
import java.io.Writer;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;

import static ru.inversion.fru.print.altprint.ALTParameterTypeEnum.*;

public abstract class ALTParameter<T>
{
    private T value;
    private ALTParameterTypeEnum type;

    protected ALTParameter(ALTParameterTypeEnum type, T value)
    {
        this.type  = type;
        this.value = value;
    }

    /** */
    public T getValue()
    {
        return this.value;
    }

    public ALTParameterTypeEnum getType()
    {
        return this.type;
    }

    // public abstract void toTextStyle(MutableAttributeSet paramMutableAttributeSet, Object paramObject);
    public abstract void toCSStyle( StringBuilder sb, Object paramObject );

    public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param) {}

    public void dump(Writer writer)
            throws IOException
    {
        writer.write("           ALTParam: ");writer.write(toString());writer.write("\n");
    }

    public String toString()
    {
        return getType().toString() + " : " + getValue().toString();
    }

    /** */
    public static class CommandParameter extends ALTParameter<Pair<ALTCommand, Object>>
    {
        private CommandParameter(Pair<ALTCommand, Object> value)
        {
            super(FRU_COMMAND,value);
        }

        public ALTCommand getCommand()
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

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            try
            {
                getCommand().toPrintParam(printParam, param == null ? getParam() : param);
            }
            catch (Exception ex)
            {
                throw new RuntimeException("cdm: " + getCommand().getName(), ex);
            }
        }
    }


    /** */
    public static class FontParameter extends ALTParameter<String>
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

        /** */
        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            printParam.setFontName(getValue());
        }
    }


    /** */
    public static class FontSizeParameter extends ALTParameter<Integer>
    {
        private FontSizeParameter(Integer fontSize)
        {
            super(FONT_SIZE,fontSize);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            sb.append("-fx-font-size:").append( getValue() ).append(" !important;");
        }

//        public void toTextStyle(MutableAttributeSet attributes, Object param)
//        {
//            StyleConstants.setFontSize(attributes, ((Integer)getValue()).intValue());
//        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            printParam.setFontSize(getValue());
        }
    }

    /** */
    private static class FontBoldParameter extends ALTParameter<Boolean>
    {
        private FontBoldParameter(Boolean isBold)
        {
            super(BOLD,isBold);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
            if( getValue() )
                sb.append("-fx-font-weight:800 !important; ");
//            else
//                sb.append("-fx-font-style:normal; ");
        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            printParam.setBold(getValue());
        }
    }

    private static final FontBoldParameter g_boldON  = new FontBoldParameter(true );
    private static final FontBoldParameter g_boldOFF = new FontBoldParameter(false);


    /** */
    private static class FontUnderlineParameter
            extends ALTParameter<Boolean>
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
//            else
//                sb.append("-fx-underline:false; ");
        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            printParam.setUnderline(getValue());
        }
    }

    private static final FontUnderlineParameter g_underLineON  = new FontUnderlineParameter(true );
    private static final FontUnderlineParameter g_underLineOFF = new FontUnderlineParameter(false);


    /** */
    private static class FontItalicParameter
            extends ALTParameter<Boolean>
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
//            else
//                sb.append("-fx-font-style:normal; ");

        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            printParam.setItalic(getValue());
        }
    }

    private static final FontItalicParameter g_italicON  = new FontItalicParameter(true);
    private static final FontItalicParameter g_italicOFF = new FontItalicParameter(false);


    /** */
    public static class LeftIndentParameter
            extends ALTParameter<Float>
    {
        private LeftIndentParameter(Float indentSize)
        {
            super(LEFT,indentSize);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            printParam.setLeftIndent( getValue() );
        }
    }


    /** */
    public static class PageOrientationParameter extends ALTParameter<OrientationRequested>
    {
        private PageOrientationParameter(OrientationRequested v)
        {
            super( ORIENTATION, v );
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
        }
    }

    private static final PageOrientationParameter g_landscape = new PageOrientationParameter(OrientationRequested.LANDSCAPE);
    private static final PageOrientationParameter g_portrait  = new PageOrientationParameter(OrientationRequested.PORTRAIT );


    /** */
    static class SpaceAfterParameter extends ALTParameter<Pair<Float, Float>>
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

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
        }

        public void toPrintParam(ALTDocPrintable.PrintParameters printParam, Object param)
        {
            try
            {
                float p1 = (param == null ? getFirst() : (Float)param);
                float p2 = getSecond();

                float result = p1 * 72.0F / p2;

                printParam.setSpaseAfter(result);
            }
            catch (Exception ex)
            {
                throw new RuntimeException("prm: " + getType().toString(), ex);
            }
        }
    }


    /** */
    public static class CopiesParameter extends ALTParameter<Copies>
    {
        private CopiesParameter(Copies copies)
        {
            super(COPIES,copies);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        { }
    }


    /** */
    private static class PageEndParameter extends ALTParameter<Integer>
    {
        public PageEndParameter()
        {
            super(PAGE_END,null);
        }

        /** */
        @Override
        public void toCSStyle( StringBuilder sb, Object paramObject )
        {
        }
    }

    public static final ALTParameter<Integer> g_pageEnd = new PageEndParameter();

    public static ALTParameter createParameter(String name, String value, ALTCommandDict dict)
            throws ALTException
    {
        ALTParameter parameter = null;
        try
        {
            ALTParameterTypeEnum type = ALTParameterTypeEnum.fromString(name);
            if (type == null)
            {
                ALTLog.warning("����������� �������� ��� ������� ������������ ��������: " + name);
                return null;
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
                    parameter = value.compareToIgnoreCase("YES") == 0 ? g_italicON : g_italicOFF;
                    break;
                case BOLD:
                    parameter = value.compareToIgnoreCase("YES") == 0 ? g_boldON : g_boldOFF;
                    break;
                case UNDERLINE:
                    parameter = value.compareToIgnoreCase("YES") == 0 ? g_underLineON : g_underLineOFF;
                    break;
                case ORIENTATION:
                    parameter = value.compareToIgnoreCase("Portrait") == 0 ? g_portrait : g_landscape;
                    break;
                case LEFT:
                    parameter = new LeftIndentParameter(Float.parseFloat(value));
                    break;
                case COPIES:
                    parameter = new CopiesParameter(new Copies(Integer.parseInt(value)));
                    break;
                case SPACE_AFTER: {

                    String p1 = null;String p2 = null;

                    int ix = value.indexOf('/');
                    if (ix == -1)
                    {
                        ALTLog.warning("�� ���������� ������ ��� ��������� ������������� �������: " + value);
                        return null;
                    }
                    p1 = value.substring(0, ix).trim();
                    p2 = value.substring(ix + 1).trim();
                    if (p1.charAt(0) == 'n') {
                        p1 = null;
                    }
                    parameter = new SpaceAfterParameter(Pair.makePair(p1 == null ? null : Float.valueOf(Float.parseFloat(p1)), Float.valueOf(Float.parseFloat(p2))));

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
                    parameter = new CommandParameter(Pair.makePair(dict.getCommand(cmd, true), param));

                }
                break;
                case PAGE_END:
                    parameter = g_pageEnd;
                    break;
            }
            if (parameter == null) {
                ALTLog.warning("�� �������� ���������� �������� �������: " + name);
            }
        }
        catch (Exception ex)
        {
            throw new ALTException("���������� ���������� �������� �������: " + name + " = " + value, ex);
        }
        return parameter;
    }
}
