package ru.inversion.fru.model.fields.types;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormatter;

import java.util.concurrent.atomic.AtomicInteger;

public class FruFieldTxt extends FruField {

    static private final AtomicInteger COUNTER = new AtomicInteger(0);

    private final String text;

    public FruFieldTxt( String text, FruFormatter formatter ) {
        super( "#tx" + COUNTER.incrementAndGet(), formatter );
        this.text = text;
    }

    @Override
    public Type getType() {
        return Type.Text;
    }

    @Override
    protected String getValueImpl(FruContext context) {
        return text;
    }
}
