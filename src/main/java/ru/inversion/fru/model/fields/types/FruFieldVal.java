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

        LocalSplitState state = context.findLocalSplitState(valIndex);

        // Если local split-flow уже активирован для этого поля в рамках текущей записи,
        // то следующие вхождения берут либо pending-хвост, либо уже пусто.
        if (state != null && state.isActive()) {
            if (S.isNotNullOrEmpty(state.getPending())) {
                return state.getPending();
            }

            if (state.isConsumed()) {
                return S.EMPTY_STRING;
            }
        }

        // Важно брать сырое значение, а не старую data-layer split-логику.
        return context.data().currentRow().getValue(valIndex);
    }
}