package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSectionLine;
import ru.inversion.fru.model.sections.FruSectionTail;

public class LineSectionRenderer implements IRenderer<FruSectionLine>{
    @Override
    public void render( FruContext context, FruSectionLine line )
    {
        final IRenderer<FruLine> lr = context.renderers().get(FruLine.class);

        for( FruLine l : line.getLines() ) {
             lr.render( context, l );
        }
    }
}
