package ru.inversion.fru.model.fields.types;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.generator.renderer.LocalSplitState;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.utils.S;

public class FruFieldVal extends FruField {

    private final int valIndex;

    public FruFieldVal(String name, int vi, FruFormatter formatter) {
        super(name, formatter);
        this.valIndex = vi;
    }

    public int getValIndex() {
        return valIndex;
    }

    @Override
    public Type getType() {
        return Type.Data;
    }

    @Override
    protected String getValueImpl(FruContext context) {
        if (valIndex == -1)
            return null;

        // Обычное поле без /z не должно попадать в local split-flow.
        if (!hasFieldSplit()) {
            return context.data().currentRow().getValue(valIndex);
        }

        LocalSplitState state = context.findLocalSplitState(valIndex);

        if (state != null && state.isActive()) {
            if (S.isNotNullOrEmpty(state.getPending())) {
                return state.getPending();
            }

            if (state.isConsumed()) {
                return S.EMPTY_STRING;
            }
        }

        return context.data().currentRow().getValue(valIndex);
    }
}