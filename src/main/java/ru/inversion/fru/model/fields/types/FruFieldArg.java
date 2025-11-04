package ru.inversion.fru.model.fields.types;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormatter;

/** */
public class FruFieldArg extends FruField {

    /** */
    public FruFieldArg( String name, FruFormatter formatter ) {
        super( name, formatter );
    }

    @Override
    public Type getType( ) {
        return Type.Arg;
    }

    @Override
    protected String getValueImpl( FruContext context ) {
        return context.getArgument(name);
    }

}
