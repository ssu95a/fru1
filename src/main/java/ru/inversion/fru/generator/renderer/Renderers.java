package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.items.*;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.sections.*;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

public class Renderers {

    private boolean isLineLocalSplitField(FruField field) {
        return field instanceof FruFieldVal && field.getFormatter() != null && ( field.getFormatter().getSplitMode() == 1 /*|| field.getFormatter().getSplitMode() == 2*/ );
    }

    private final IRenderer<FruFieldVal> fieldGrpRenderer = new FruFieldGrpRenderer();

    private static final class FruFieldGrpRenderer implements IRenderer<FruFieldVal> {

        @Override
        public void render(FruContext context, FruFieldVal field) {

            if (context == null) {
                throw new IllegalArgumentException("context == null");
            }

            if (field == null) {
                return;
            }

            if (field.getFormatter() != null
                    && field.getFormatter().getSplitMode() == 2) {
                throw new IllegalStateException(
                        "FruFieldGrp must not render /x field: " + field
                );
            }

            /*
             * ВАЖНО:
             * renderFieldGroupSlot(...) должен вернуть уже готовый visible text
             * для конкретного slot-а группы.
             *
             * Grouped fields не должны попадать в:
             * - FruLineRenderSession
             * - field.getValue(context)
             * - LocalSplitState
             * - cacheRow
             */
            String value = context.renderFieldGroupSlot(field);

            if (value == null) {
                value = S.EMPTY_STRING;
            }

            context.writer().print(value);
        }
    }

    // Лямбда-рендереры для простых случаев
    private final IRenderer<FruText> textRenderer = (context, item) ->context.writer().print(item.getText());

    private final IRenderer<FruLine> lineRenderer = new IRenderer<FruLine>() {
        @Override
        public void render( FruContext context, FruLine line )
        {
            for( FruItem i : line.getItems( ) )
                 context.renderers().render( context, i );
        }
    };

    private final IRenderer<FruField> fieldRenderer = new IRenderer<FruField>() {
        @Override
        public void render(FruContext context, FruField field) {

            if (context == null) {
                throw new IllegalArgumentException("context == null");
            }

            if (field == null) {
                return;
            }

            /*
             * HOTFIX:
             * FruFieldGrp перехватывает только конкретные FruFieldVal instances,
             * заранее найденные FruFieldGrpPlanner через IdentityHashMap.
             *
             * Этот блок обязан стоять ДО:
             * - lineSession.resolveValue(...)
             * - field.getValue(context)
             * - field.getFormatter().format(...)
             *
             * Иначе @S6/74/l@ tail-slot снова прочитает raw value
             * и продублирует сумму.
             */
            if (field instanceof FruFieldVal) {
                FruFieldVal fv = (FruFieldVal) field;

                if (context.hasFieldGroup(fv)) {
                    fieldGrpRenderer.render(context, fv);
                    return;
                }
            }

            final FruLineRenderSession lineSession = context.getLineRenderSession();

            final String value = lineSession != null
                    ? lineSession.resolveValue(context, field)
                    : field.getValue(context);

            if (field.getFormatter() != null) {
                Pair<String, String> pv = field.getFormatter().format(context, value, field);

                context.writer().print(pv.first);

                if (lineSession != null && isLineContinuationField(field)) {
                    if (S.isNotNullOrEmpty(pv.second)
                            && !pv.second.equals(value)
                            && pv.second.length() < value.length()) {
                        lineSession.storeRemainder(field, pv.second);
                    }
                }

                if (isRecordLocalSplitField(field)) {
                    FruFieldVal fv = (FruFieldVal) field;

                    LocalSplitState state = context.getOrCreateLocalSplitState(fv.getValIndex());
                    state.setActive(true);
                    state.setConsumed(true);
                    state.setPending(S.isNotNullOrEmpty(pv.second) ? pv.second : null);
                }
            }
            else {
                context.writer().print(value);
            }
        }

        private boolean isLineContinuationField(FruField field) {
            if (!(field instanceof FruFieldVal)) {
                return false;
            }

            if (field.getFormatter() == null) {
                return false;
            }

            int splitMode = field.getFormatter().getSplitMode();

            /*
             * PROD HOTFIX:
             * splitMode=2 (/x) оставляем в старом legacy path.
             *
             * Не трогать /x через FruFieldGrp.
             * Не переводить /x на LocalSplitState.
             * Не пытаться сейчас переписать /x line expansion.
             */
            return splitMode == 1 || splitMode == 2;
        }

        private boolean isRecordLocalSplitField(FruField field) {
            return field instanceof FruFieldVal
                    && field.getFormatter() != null
                    && field.getFormatter().getSplitMode() == 1;
        }
    };

    private final IRenderer<FruScript> scriptRenderer = new IRenderer<FruScript>() {
        @Override
        public void render( FruContext context, FruScript script ) {
            context.executeScript(script);
        }
    };

    private final IRenderer<FruSectionTable>  tableRenderer         = new TableSectionRenderer();
    private final IRenderer<FruSectionHeader> headerSectionRenderer = new HeaderSectionRenderer();
    private final IRenderer<FruSectionTail>   tailSectionRenderer   = new TailSectionRenderer();
    private final IRenderer<FruSectionLine>   lineSectionRenderer   = new LineSectionRenderer();

    private final IRenderer<FruFormatCall>    formatCallRenderer    = new FormatCallRenderer();

    private final IRenderer<FruPaging>        pagingRenderer        = new PageNumRenderer();

    public <T extends FruItem> void render( FruContext context, T item ) {

        if( item == null )
            return;

        final IRenderer<T> renderer = get( (Class<T>)item.getClass() );

        renderer.render( context, item );
    }

    //Рендереры как внутренние классы
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
        else if (clazz == FruSectionText.class)
            return (IRenderer<T>) tableRenderer;
        else if (clazz == FruSectionHeader.class)
            return (IRenderer<T>)headerSectionRenderer;
        else if (clazz == FruSectionTail.class)
            return (IRenderer<T>)tailSectionRenderer;
        else if (clazz == FruFormatCall.class)
            return (IRenderer<T>) formatCallRenderer;
        else if (clazz == FruScript.class)
            return (IRenderer<T>) scriptRenderer;
        else if (clazz == FruPaging.class)
            return (IRenderer<T>) pagingRenderer;
        else if (clazz == FruSectionLine.class)
            return (IRenderer<T>) lineSectionRenderer;

        throw new IllegalArgumentException( "No renderer registered for: " + clazz );
    }
}
