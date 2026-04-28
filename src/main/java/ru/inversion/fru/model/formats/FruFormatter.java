package ru.inversion.fru.model.formats;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.utils.FruUtils;
import ru.inversion.utils.Holder;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Character.isDigit;
import static ru.inversion.fru.model.formats.FormatParameterEnum.*;

/** */
public class FruFormatter extends FruItem {

    private final Map<FormatParameterEnum,Object> parameters = new EnumMap<>(FormatParameterEnum.class);

    private final FruFormatter parent;

    public FruFormatter()
    {
        this(null);
    }

    /** */
    public FruFormatter( FruFormatter parent )
    {
        this.parent = parent;
    }

    /** */
    public int getSplitMode( ) { return parameter(Split); }

    /** */
    public int getWidth( ) {
        return parameter(Width);
    }

    /** */
    private char fillChar() { return  parameter(FillChar); }

    /** */
    private AlignEnum align() { return parameter(Align); }

    /** */
    private <T> T parameter( FormatParameterEnum p ) {

        T value = (T)parameters.get(p);

        if( value == null && parent != null ) {
            value = (T)parent.parameters.get(p);
        }

        if( value == null )
            value = (T)defaultValue(p);

        return value;
    }

    private Object defaultValue(FormatParameterEnum p) {
        switch (p) {
            case Align:
                return AlignEnum.None;
            case Split:
                return 0;
            case Width:
                return -1;
            case CaseMode:
                return null;
            case FillChar:
                return ' ';
            case FillRight:
                return false;
            case ExcludeSymbol:
                return false;
        }
        return null;
    }

    /** */
    private String prepareValue( FruContext context, String value )
    {
        if( value == null )
            return S.EMPTY_STRING;

        if( Boolean.TRUE.equals( parameter(ExcludeSymbol) )
            &&
            context.excludeSymbols() != null
        )
        {
            // если есть исключаем
            final Set<Character> es = context.excludeSymbols();
            boolean doIt = false;

            for(int i = 0; i < value.length() && !doIt; i++)
                doIt = es.contains( value.charAt(i) );

            if (doIt)
                value = value.chars().filter(c -> !context.excludeSymbols().contains((char) c)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        }

        final int width = getWidth();

        if( width > 0 && value.length() > width && getSplitMode() == 0 ) {
            String trimmed = value.trim();

            if (trimmed.length() <= width) {
                value = trimmed;
            }
            else {
                value = value.substring(0, width);
            }
        }

        CaseModeEnum caseMode = parameter(CaseMode);

        if( caseMode != null )
        {
            switch( caseMode ) {
                case Lower:
                    value = value.toLowerCase();
                    break;
                case Upper:
                    value = value.toUpperCase();
                    break;
                case FirstU:
                {
                    char[]a = value.toLowerCase().toCharArray();
                    a[0] = Character.toUpperCase(a[0]);
                    value = String.valueOf(a);
                }
                break;
            }
        }
        return value;
    }

    /*
    public Pair<String,String> format( FruContext context, String value, FruField fruField )
    {

        int width = getWidth();

        if( S.isNullOrEmpty(value) )
        {
            if( width > 0 )
                return Pair.makePair( S.space( width, fillChar() ), null );
            else
                return Pair.makePair( S.EMPTY_STRING, null );
        }

        value = prepareValue( context, value );

        if( width <= 0 )
            width = context.getWidth();

        String restValue = null;

        if( width > 0 )
        {
            if( getSplitMode() == 1 && fruField instanceof FruFieldVal && value.length() > width )
            {
                // Split=1: только inline-продолжение в рендерере строки.
                // НИЧЕГО не кладём в cacheRow, иначе верхний pipeline
                // повторно прогонит всю секцию/форму.
                final Pair<String, String> splitPair = FruUtils.splitString( value, width );
                restValue = splitPair.second;
                value     = FormatHelper.formatString( splitPair.first, width, align(), fillChar(), new Holder<>() );
            }
            else
            {
                Holder<String> rem = new Holder<>();
                value = FormatHelper.formatString( value, width, align(), fillChar(), rem );

                if( fruField != null && rem.isPresent() )
                {
                    if( getSplitMode() == 2 && fruField instanceof FruFieldVal )
                    {
                        // Split=2: continuation как synthetic row через cacheRow.
                        context.data().put2CacheRow( rem.get(), ((FruFieldVal) fruField).getValIndex() );
                        restValue = rem.get();
                    }
                }
            }
        }
        else
            if( Boolean.TRUE.equals( parameter(FillRight)) && context.getWidth() > 0 )
            {
                int fillLength = context.getWidth() - context.getCurrentPosition();
                if( fillLength > value.length() )
                    value = S.space( fillLength - value.length(), fillChar() ) + value;
            }

        return Pair.makePair( value, restValue );
    }
    */

    /** */
    private static boolean isDigitAll( String s )
    {
        if( S.isNullOrEmpty(s) )
            return false;

        for( int i = 0; i < s.length(); i++ )
        {
            if( !isDigit( s.charAt(i) ) )
                return false;
        }

        return true;
    }

    /** */
    public static FruFormatter make( String fmtStr )
    {
        if( S.isNullOrEmpty(fmtStr) )
            return null;

        final List<String> items = Arrays.stream( fmtStr.split("/") ).map(String::trim).filter(s -> !s.isEmpty()).collect( Collectors.toList() );

        final FruFormatter formatter = new FruFormatter();

        for( String s : items )
        {
            if( s.length() == 1 )
            {
                char ch = s.charAt(0);

                if( CaseModeEnum.isCaseMode(ch) )
                    formatter.parameters.put( CaseMode,CaseModeEnum.of(ch) );
                if( AlignEnum.isAlign(ch) )
                    formatter.parameters.put( Align, AlignEnum.of(ch) );
                else if( ch == 'v' )
                    formatter.parameters.put( ExcludeSymbol, Boolean.TRUE );
                else if( ch == 'x' )
                    formatter.parameters.put( Split, 2 );
                else if( ch == 'w' )
                    formatter.parameters.put( FillRight, Boolean.TRUE );
                else if( isDigit(ch) )
                    formatter.parameters.put( Width, Integer.parseInt(s) );
            }

            else if( s.length() == 2 )
            {
                if( s.charAt(0) == 'e' )
                    formatter.parameters.put( FillChar, s.charAt(1) );
                else if( s.charAt(1) == 'z' ) {
                    formatter.parameters.put( Split, 1 );
                    formatter.parameters.put( Align, AlignEnum.of( s.charAt(0) ) );
                }
                else if( isDigit(s.charAt(0) ) && isDigit(s.charAt(1) ) )
                    formatter.parameters.put( Width, Integer.parseInt(s) );
            }

            else if( isDigitAll(s) )
                formatter.parameters.put( Width, Integer.parseInt(s) );
        }
        return formatter;
    }

    /** */
    public Pair<String, String> format(FruContext context, String value, FruField fruField)
    {
        int width = resolveEffectiveWidth(context);

        if( S.isNullOrEmpty(value) )
            return formatEmptyValue(width);

        value = prepareValue(context, value);

        if (width <= 0)
            return Pair.makePair( applyFillRightIfNeeded(context, value), null );


        final boolean split1 = isSplitMode1FieldValueOverflow(value, width, fruField);
        final boolean split2 = isSplitMode2FieldValue(fruField);

        String visibleValue = value;
        String restValue = null;

        if (split1)
        {
            Pair<String, String> splitPair = FruUtils.splitString(value, width);
            visibleValue = splitPair.first;
            restValue = splitPair.second;

            visibleValue = formatVisiblePart(visibleValue, width);
            return Pair.makePair(visibleValue, restValue);
        }

        Holder<String> remainderHolder = new Holder<>();
        visibleValue = FormatHelper.formatString(
                value,
                width,
                align(),
                fillChar(),
                remainderHolder
        );

        if (split2 && remainderHolder.isPresent() && S.isNotNullOrEmpty(remainderHolder.get())) {
            restValue = remainderHolder.get();

            if (context.getLineRenderSession() == null) {
                context.data().put2CacheRow(
                        restValue,
                        ((FruFieldVal) fruField).getValIndex()
                );
            }
        }

        return Pair.makePair(visibleValue, restValue);
    }

    /** */
    private String normalizeLocalSplitCandidate(String value, int width, FruField fruField)
    {
        if (!(fruField instanceof FruFieldVal) || S.isNullOrEmpty(value)) {
            return value;
        }

        String trimmed = value.trim();

        // Если переполнение вызвано только внешним padding из DAT,
        // считаем его незначимым и не уводим поле на continuation.
        if (!trimmed.equals(value) && trimmed.length() <= width) {
            return trimmed;
        }

        return value;
    }

    /** */
    private int resolveEffectiveWidth(FruContext context)
    {
        int width = getWidth();

        if (width <= 0) {
            width = context.getWidth();
        }

        return width;
    }

    private Pair<String, String> formatEmptyValue(int width)
    {
        if (width > 0)
            return Pair.makePair( S.space(width, fillChar()), null );

        return Pair.makePair(S.EMPTY_STRING, null );
    }

    /** */
    private boolean isSplitMode1FieldValueOverflow(String value, int width, FruField fruField)
    {
        return getSplitMode() == 1
                && fruField instanceof FruFieldVal
                && value.length() > width;
    }

    /** */
    private boolean isSplitMode2FieldValue(FruField fruField)
    {
        return getSplitMode() == 2
                && fruField instanceof FruFieldVal;
    }

    /** */
    private String formatVisiblePart(String value, int width)
    {
        return FormatHelper.formatString( value, width, align(), fillChar(), new Holder<String>() );
    }

    /** */
    private String applyFillRightIfNeeded(FruContext context, String value)
    {
        if( Boolean.TRUE.equals(parameter(FillRight)) && context.getWidth() > 0)
        {
            int fillLength = context.getWidth() - context.getCurrentPosition();

            if (fillLength > value.length())
                return S.space(fillLength - value.length(), fillChar()) + value;
        }
        return value;
    }
}
