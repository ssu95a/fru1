package ru.inversion.fru.model.formats;

import ru.inversion.utils.Holder;

/** */
public class FormatHelper {

    public static String formatString( String value, int width, AlignEnum align, char fillChar, Holder<String> rmd )
    {
        if( value.length() == width )
            return value;

        if( value.length() > width )
        {
            rmd.set(value.substring( width ) );
            return value.substring( 0, width );
        }

        if( align == AlignEnum.None )
            align = AlignEnum.Left;
            //return value;

        final int padding = width - value.length();
        final StringBuilder sb = new StringBuilder(width);

        if( align == AlignEnum.Left )
        {
            sb.append(value);
            for (int i = 0; i < padding; i++)
                 sb.append(fillChar);

       } else if ( align == AlignEnum.Right ) {

            for( int i = 0; i < padding; i++)
                 sb.append(fillChar);

            sb.append(value);
       }
       else if( align == AlignEnum.Center ) {

            int leftPadding = padding / 2;
            int rightPadding = padding - leftPadding;

            for(int i = 0; i < leftPadding; i++) {
                sb.append(fillChar);
            }

            sb.append(value);

            for(int i = 0; i < rightPadding; i++) {
                sb.append(fillChar);
            }
       }

       return sb.toString();
    }
}
