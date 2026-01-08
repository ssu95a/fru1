package ru.inversion.fru.print.naltprn.cmd;

import ru.inversion.utils.S;

public class LineFeedCommand extends AltCommand {

    public static LineFeedCommand instance = new LineFeedCommand();

    /** */
    private LineFeedCommand( )
    {
        super( "LF", "Перевод строки" );
    }

    /** */
    static boolean isLineFeed( String name )
    {
        return !S.isNullOrEmpty(name) && "LF".equalsIgnoreCase(name);
    }
}
