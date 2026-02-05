package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSectionLine;
import ru.inversion.fru.model.sections.FruSectionTable;


/** */
public class TableSectionRenderer implements IRenderer<FruSectionTable> {

    @Override
    public void render( FruContext context, FruSectionTable table ) {

        table.incrementUse();

        final IRenderer<FruLine> lineRenderer = context.renderers().get( FruLine.class );

        final IRenderer<FruSectionLine> lineSectionRenderer =
              table.getLine() != null ? context.renderers().get(FruSectionLine.class) : null;

        // Рендерим каждую строку таблицы
        renderTableLine( context, table, lineRenderer, lineSectionRenderer );

    }

    // Рендеринг строки с учетом переносов
    private void renderTableLine(FruContext context, FruSectionTable table, IRenderer<FruLine> lineRenderer, IRenderer<FruSectionLine> lineSectionRenderer) {

        for( FruLine line : table.getLines() )
        {
            lineRenderer.render( context, line );
            context.writer().newLine(); // Переход на следующую строку для сплитов
            if( lineSectionRenderer != null )
                lineSectionRenderer.render( context, table.getLine() );
        }

    }
}
