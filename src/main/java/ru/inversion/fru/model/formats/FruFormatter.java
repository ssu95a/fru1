package ru.inversion.fru.model.formats;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.items.FruItem;
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

//    private AlignEnum align = AlignEnum.Left;

//    private CaseModeEnum caseMode;

//    private int width = -1;

    //private boolean excludeSymb = false;

    //private char fillChar = ' ';

    // 1 - в поле
    // 2 - в форматной строке
    // private int split = 0;

    //private boolean fillRight = false;

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

        if( width > 0 && value.length() > width && getSplitMode( ) == 0 )
            value = value.substring(0,width);

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

    /** */
    public Pair<String,String> format( FruContext context, String value, FruField fruField ) {

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
            Holder<String> rem = new Holder<>();
            value = FormatHelper.formatString( value, width, align(), fillChar(), rem );

            if( fruField != null && rem.isPresent() )
            {
                if( getSplitMode( ) == 2 && fruField instanceof FruFieldVal ) {

                    context.data().put2CacheRow (
                        rem.get(),
                        ( (FruFieldVal) fruField ).getValIndex()
                    );

                    restValue = rem.get();
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
}
