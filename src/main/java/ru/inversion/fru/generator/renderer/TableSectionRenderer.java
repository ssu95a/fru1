package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSectionHeader;
import ru.inversion.fru.model.sections.FruSectionTable;
import ru.inversion.fru.model.sections.FruSectionTail;


/** */
public class TableSectionRenderer implements IRenderer<FruSectionTable> {

    @Override
    public void render( FruContext context, FruSectionTable table ) {

        table.incrementUse();

        final IRenderer<FruLine> lineRenderer = context.renderers().get( FruLine.class );

        // Рендерим каждую строку таблицы
        renderTableLine( context, table, lineRenderer );

    }

    // Рендеринг строки с учетом переносов
    private void renderTableLine( FruContext context, FruSectionTable table, IRenderer<FruLine> lineRenderer ) {

        for( FruLine line : table.getLines() )
        {
            lineRenderer.render( context, line );
            context.writer().newLine(); // Переход на следующую строку для сплитов
        }

        /*
            boolean hasMoreSplits;

        do {

            hasMoreSplits = false;


            for( FruLine line : table.getLines() ) {

                 lineRenderer.render( context, line );
                 context.writer().newLine(); // Переход на следующую строку для сплитов


                // Проверяем есть ли еще сплиты для рендеринга
                for (int i = 0; i < context.data().currentRow().getData().size(); i++) {
                    if (context.data().currentRow().hasMoreSplits(i)) {
                        hasMoreSplits = true;
                        break;
                    }
                }
            }

            if (hasMoreSplits) {
                context.writer().newLine(); // Переход на следующую строку для сплитов
            }

        } while (hasMoreSplits);
         */
    }
}
