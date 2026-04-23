package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSectionHeader;
import ru.inversion.utils.S;

public class HeaderSectionRenderer implements IRenderer<FruSectionHeader>{

    @Override
    public void render( FruContext context, FruSectionHeader header ) {

        // Рендеринг только на первой странице или по условию
        //if( shouldRenderHeader(context) )

        final IRenderer<FruLine> lr = context.renderers().get(FruLine.class);

        for( FruLine l : header.getLines() ) {
             lr.render(context, l);
context.writer().marker = "HeaderSectionRenderer: " + header.getNum();
             context.writer().newLine();
context.writer().marker = S.EMPTY_STRING;
        }
    }
}
