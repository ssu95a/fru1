package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.FruFormatCall;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruText;
import ru.inversion.utils.io.RawCAW;
import ru.inversion.utils.S;

import java.io.Writer;
import java.util.List;

public class FormatCallRenderer implements IRenderer<FruFormatCall> {

    @Override
    public void render(FruContext context, FruFormatCall formatCall)
    {
        final FruLineRenderSession lineSession = context.getLineRenderSession();

        if( lineSession != null && lineSession.isContinuationPass() && hasPendingInFormatCall(formatCall, lineSession) )
        {
            renderContinuation( context, formatCall, lineSession );
            return;
        }
        renderNormal(context, formatCall);
    }

    /** */
    private void renderNormal(FruContext context, FruFormatCall formatCall)
    {
        final FruFormat format = formatCall.getFormat();
        final List<FruField> fields = formatCall.getFields();
        final List<FruItem> items = format.getItems();
        final FruFormatter containing = formatCall.getContaining();

        Writer w = null;

        if (containing != null) {
            w = new RawCAW();
            context.writer().startBuffer(w);
        }

        try {
            final IRenderer<FruField> fieldRenderer = context.renderers().get(FruField.class);
            final IRenderer<FruText> textRenderer = context.renderers().get(FruText.class);

            int i = 0;

            for (FruItem item : items) {
                if (item instanceof FruText) {
                    textRenderer.render(context, (FruText) item);
                } else {
                    final FruField fruField = fields.get(i++);
                    fieldRenderer.render(context, fruField);
                }
            }
        }
        finally {
            if (context.writer().isBufferEnabled()) {
                context.writer().discardBuffer();
            }
        }

        if (w != null) {
            context.writer().print(containing.format(context, w.toString(), null).first);
        }
    }

    private void renderContinuation(
            FruContext context,
            FruFormatCall formatCall,
            FruLineRenderSession lineSession
    ) {
        final FruFormat format = formatCall.getFormat();
        final List<FruField> fields = formatCall.getFields();
        final List<FruItem> items = format.getItems();
        final IRenderer<FruField> fieldRenderer = context.renderers().get(FruField.class);

        int i = 0;

        for (FruItem item : items) {
            if (item instanceof FruText) {
                context.writer().print(maskFrameText(((FruText) item).getText()));
            } else {
                final FruField field = fields.get(i++);

                if (lineSession.hasPendingFor(field)) {
                    fieldRenderer.render(context, field);
                }
                else {
                    context.writer().print(renderEmptyField(context, field));
                }
            }
        }
    }

    private boolean hasPendingInFormatCall(FruFormatCall formatCall, FruLineRenderSession lineSession)
    {
        if (formatCall == null || formatCall.getFields() == null) {
            return false;
        }

        for (FruField field : formatCall.getFields()) {
            if (field != null && field.hasFieldSplit() && lineSession.hasPendingFor(field)) {
                return true;
            }
        }

        return false;
    }

    private String renderEmptyField(FruContext context, FruField field)
    {
        if (field.getFormatter() != null) {
            return field.getFormatter().format(context, "", field).first;
        }

        int width = field.getWidth();
        return width > 0 ? S.space(width, ' ') : S.EMPTY_STRING;
    }

    private String maskFrameText(String text)
    {
        if (S.isNullOrEmpty(text)) {
            return S.EMPTY_STRING;
        }

        StringBuilder sb = new StringBuilder(text.length());

        for (int j = 0; j < text.length(); j++) {
            char ch = text.charAt(j);

            if (Character.isWhitespace(ch) || ch == '|' || ch == '│' || ch == '║' || ch == '¦') {
                sb.append(ch);
            } else {
                sb.append(' ');
            }
        }

        return sb.toString();
    }
}