package ru.inversion.fru.model.formats;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.utils.Holder;
import ru.inversion.utils.S;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Character.isDigit;

/** */
public class FruFormatter extends FruItem {

    private AlignEnum align = AlignEnum.Left;

    private CaseModeEnum caseMode;

    private int width = -1;

    private boolean excludeSymb = false;

    private char fillChar = ' ';

    // 1 - в поле
    // 2 - в форматной строке
    private int split = 0;

    private boolean fillRight = false;

    /** */
    public int getSplitMode( ) { return split; }

    /** */
    public int getWidth( ) {
        return width;
    }

    /** */
    private String prepareValue( FruContext context, String value )
    {
        if( value == null )
            return S.EMPTY_STRING;

        if( excludeSymb && context.excludeSymbols() != null) {

            // если есть исключаем
            final Set<Character> es = context.excludeSymbols();
            boolean doIt = false;

            for(int i = 0; i < value.length() && !doIt; i++)
                doIt = es.contains( value.charAt(i) );

            if (doIt)
                value = value.chars().filter(c -> !context.excludeSymbols().contains((char) c)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        }


        if( width > 0 && value.length() > width && split == 0 )
            value = value.substring(0,width);

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
    public String format( FruContext context, String value, FruField fruField ) {

        if( S.isNullOrEmpty(value) )
        {
            if( width > 0 )
                return S.space( width, fillChar);
            else
                return S.EMPTY_STRING;
        }

        value = prepareValue( context, value );

        if( width > 0 )
        {
            Holder<String> rem = new Holder<>();
            value = FormatHelper.formatString( value, width, align, fillChar, rem );

            if( rem.isPresent() )
            {
                if( split == 2 && fruField instanceof FruFieldVal )
                    context.data().put2CacheRow( rem.get(), ((FruFieldVal)fruField).getValIndex() );
            }
        }
        else
            if( fillRight && context.getWidth() > 0 )
            {
                int fillLength = context.getWidth() - context.getCurrentPosition();
                if( fillLength > value.length() )
                    value += S.space( fillLength - value.length(), fillChar);
            }

        return value;
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

        final List<String> items = Arrays.stream( fmtStr.split("/") ).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        final FruFormatter formatter = new FruFormatter();

        for( String s : items )
        {
            if( s.length() == 1 )
            {
                char ch = s.charAt(0);

                if( CaseModeEnum.isCaseMode(ch) )
                    formatter.caseMode = CaseModeEnum.of(ch);
                if( AlignEnum.isAlign(ch) )
                    formatter.align = AlignEnum.of(ch);
                else if( ch == 'v' )
                    formatter.excludeSymb = true;
                else if( ch == 'x' )
                    formatter.split = 2;
                else if( ch == 'w' )
                    formatter.fillRight = true;
                else if( isDigit(ch) )
                    formatter.width = Integer.parseInt(s);
            }

            else if( s.length() == 2 )
            {
                if( s.charAt(0) == 'e' )
                    formatter.fillChar = s.charAt(1);
                else if( s.charAt(1) == 'z' ) {
                    formatter.split = 1;
                    formatter.align = AlignEnum.of( s.charAt(0) );
                }
                else if( isDigit(s.charAt(0) ) && isDigit(s.charAt(1) ) )
                    formatter.width = Integer.parseInt(s);
            }

            else if( isDigitAll(s) )
                formatter.width = Integer.parseInt(s);
        }
        return formatter;
    }
}
