package ru.inversion.fru.model.fields.types;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.utils.converter.TypeConverter;

/** Поле для аргументов скрипта */
public class FruFieldScr extends FruField {

    /** */
    public FruFieldScr(String name, FruFormatter formatter) {
        super(name, formatter);
    }

    /** */
    @Override
    public Type getType( ) {
        return Type.ScriptArg;
    }

    /** */
    @Override
    protected String getValueImpl( FruContext context ) {
        return TypeConverter.convert( context.globalScriptContext().getAttribute( getName() ), String.class );
    }
}
