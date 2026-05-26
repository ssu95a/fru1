package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.FruPaging;
import ru.inversion.utils.S;

public class PageNumRenderer implements IRenderer<FruPaging> {

    @Override
    public void render( FruContext context, FruPaging paging ) {

        final int pageNum = context.getCurrentPageNum();

        if( pageNum == 1 && paging.isFirstOff() )
            return;

        final FruFormatter formatter = paging.getFormatter();

        String pageEnd = paging.getPageEnd();
        if( S.isNullOrEmpty(pageEnd) )
            pageEnd = "`PAGE_END`";

        context.writer().print( formatter.format( context, pageEnd, null ).first, true );
        context.writer().newLine();
    }
}
