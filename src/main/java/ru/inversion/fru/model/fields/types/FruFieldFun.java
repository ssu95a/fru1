package ru.inversion.fru.model.fields.types;

import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.functions.BuiltinFunctionEnum;
import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.utils.converter.TypeConverter;


public class FruFieldFun extends FruField {

    private final BuiltinFunctionEnum fun;
    private final String parameters;

    /** */
    public FruFieldFun(BuiltinFunctionEnum fun, FruFormatter formatter, String parameters ) {
        super( fun.name(), formatter);
        this.fun = fun;
        this.parameters = parameters;
    }

    /** */
    public FruFieldFun(BuiltinFunctionEnum fun, FruFormatter formatter ) {
        super( fun.name(), formatter);
        this.fun = fun;
        this.parameters = null;
    }

    /** */
    @Override
    public Type getType( ) {
        return Type.Func;
    }

    /** */
    @Override
    protected String getValueImpl( FruContext context ) {
        return TypeConverter.convert( fun.execute( context, parameters ), String.class );
    }
}
