package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.FruPaging;

public class PageNumRenderer implements IRenderer<FruPaging> {

    @Override
    public void render( FruContext context, FruPaging paging ) {

        final int pageNum = context.getCurrentPageNum();

        if( pageNum == 1 && paging.isFirstOff() )
            return;

        final FruFormatter formatter = paging.getFormatter();

        context.writer().print( formatter.format( context, "`PAGE_END`", null ), true );
    }
}
