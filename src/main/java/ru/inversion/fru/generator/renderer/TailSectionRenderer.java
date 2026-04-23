package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSectionTail;
import ru.inversion.utils.S;

public class TailSectionRenderer implements IRenderer<FruSectionTail>{
    @Override
    public void render( FruContext context, FruSectionTail header ) {
        final IRenderer<FruLine> lr = context.renderers().get(FruLine.class);
        for( FruLine l : header.getLines() ) {
            lr.render(context, l);
context.writer().marker = "TailSectionRenderer:" + header.getNum();
            context.writer().newLine();
context.writer().marker = S.EMPTY_STRING;
        }
    }
}
