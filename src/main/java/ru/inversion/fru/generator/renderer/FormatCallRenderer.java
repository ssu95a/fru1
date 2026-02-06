package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.FruFormatCall;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruText;
import ru.inversion.utils.io.RawCAW;

import java.io.Writer;
import java.util.List;

public class FormatCallRenderer implements IRenderer<FruFormatCall> {

    @Override
    public void render( FruContext context, FruFormatCall formatCall )
    {
        final FruFormat      format = formatCall.getFormat();
        final List<FruField> fields = formatCall.getFields();
        final List<FruItem>  items = format.getItems();
        final FruFormatter   containing = formatCall.getContaining();

        int i = 0;

        Writer w = null;

        if( containing != null )
        {
            w = new RawCAW();
            context.writer().startBuffer(w);
        }

        try {

            IRenderer<FruField> fieldRenderer = context.renderers().get(FruField.class);

            for( FruItem item : items )
            {
                if( item instanceof FruText ) {
                    context.renderers().render(context, item);
                }
                else
                {
                    final FruField fruField = fields.get(i++);
                    fieldRenderer.render(context, fruField);
                }
            }//end for

        }
        finally
        {
            if( context.writer().isBufferEnabled() )
                context.writer().discardBuffer();
        }

        if( w != null ) {
            context.writer().print( containing.format( context, w.toString(), null ) );
        }
    }
}
