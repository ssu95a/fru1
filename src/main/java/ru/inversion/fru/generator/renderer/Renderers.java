package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.items.*;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.sections.FruSectionTable;

public class Renderers {

    // Лямбда-рендереры для простых случаев
    private final IRenderer<FruText> textRenderer = (context, item) ->context.writer().print(item.getText());

    private final IRenderer<FruLine> lineRenderer = new IRenderer<FruLine>() {
        @Override
        public void render( FruContext context, FruLine line ) {
            for( FruItem i : line.getItems() ) {
                 context.renderers().render(context, i );
            }
        }
    };

    private final IRenderer<FruField> fieldRenderer = new IRenderer<FruField>() {
        @Override
        public void render(FruContext context, FruField field ) {

            String value = field.getValue(context);
            if( field.getFormatter() != null )
                context.writer().print( field.getFormatter().format( context, value, field ));
            else
                context.writer().print( value );
        }
    } ;

    private final IRenderer<FruScript> scriptRenderer = new IRenderer<FruScript>() {
        @Override
        public void render(FruContext context, FruScript script ) {
            context.executeScript(script);
        }
    };

    private final IRenderer<FruSectionTable> tableRenderer = new TableSectionRenderer();

    private final IRenderer<FruFormatCall> formatCallIRenderer = new FormatCallRenderer();

    private final IRenderer<FruPaging> pagingRenderer =new PageNumRenderer();

    public <T extends FruItem> void render( FruContext context, T item ) {

        if( item == null )
            return;

        final IRenderer<T> renderer = get( (Class<T>)item.getClass() );

        renderer.render(context, item);
    }


    // Сложные рендереры как внутренние классы
    //private final IRenderer<FruFormat> formatRenderer = new FormatRenderer();
    public <T extends FruItem> IRenderer<T> get( Class<T > clazz )
    {
        if( clazz == null)
            throw new IllegalArgumentException("'clazz' cannot be null");

        if( clazz == FruText.class)
            return (IRenderer<T>) textRenderer;
        else if (clazz ==  FruLine.class)
            return (IRenderer<T>) lineRenderer;
         else if (FruField.class.isAssignableFrom(clazz))
             return (IRenderer<T>) fieldRenderer;
         else if (clazz == FruSectionTable.class)
             return (IRenderer<T>) tableRenderer;
        else if (clazz == FruFormatCall.class)
            return (IRenderer<T>) formatCallIRenderer;
        else if (clazz == FruScript.class)
            return (IRenderer<T>) scriptRenderer;
        else if (clazz == FruPaging.class)
            return (IRenderer<T>) pagingRenderer;

        throw new IllegalArgumentException("No renderer registered for: " + clazz );
    }
}
