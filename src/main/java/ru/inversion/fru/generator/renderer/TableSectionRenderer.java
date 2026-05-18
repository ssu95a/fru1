package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSectionLine;
import ru.inversion.fru.model.sections.FruSectionTable;
import ru.inversion.utils.S;


/** */
public class TableSectionRenderer implements IRenderer<FruSectionTable> {


    private final FruTableBodyLineRenderer tableBodyLineRenderer;

    public TableSectionRenderer()
    {
        this( new FruTableBodyLineRenderer() );
    }

    public TableSectionRenderer(FruTableBodyLineRenderer tableBodyLineRenderer)
    {
        this.tableBodyLineRenderer = tableBodyLineRenderer;
    }
    
    @Override
    public void render( FruContext context, FruSectionTable table ) {

        table.incrementUse();

        final IRenderer<FruSectionLine> lineSectionRenderer = table.getLine() != null ? context.renderers().get(FruSectionLine.class) : null;

        // Рендерим каждую строку таблицы
        renderTableLine( context, table, lineSectionRenderer );
    }

    // Рендеринг строки с учетом переносов
    private void renderTableLine(
        FruContext context, FruSectionTable table,
        IRenderer<FruSectionLine> lineSectionRenderer
    )
    {
        for( FruLine line : table.getLines() )
        {
            tableBodyLineRenderer.render( context, line );
context.writer().marker = "TableSectionRenderer: " + table.getNum();
            context.writer().newLine();
context.writer().marker = S.EMPTY_STRING;
            if( lineSectionRenderer != null )
                lineSectionRenderer.render( context, table.getLine() );
        }

    }
}
