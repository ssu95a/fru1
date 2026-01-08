package ru.inversion.fru.print.naltprn.cmd;

import ru.inversion.utils.S;

public class PageEndCommand extends AltCommand {

    public static PageEndCommand instance = new PageEndCommand();

    /** */
    private PageEndCommand( )
    {
        super( "PAGE_END", "Окончание страницы" );
    }

    /** */
    static boolean isPageEnd( String name )
    {
        return !S.isNullOrEmpty(name) && "PAGE_END".equalsIgnoreCase(name);
    }
}
