package ru.inversion.fru.model.fields.types;


import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormatter;

/** */
public class FruFieldVal extends FruField {

    /** */
    final private int valIndex;

    /** */
    public FruFieldVal( String name, int vi, FruFormatter formatter ) {
        super( name, formatter);
        this.valIndex = vi;
    }

    /** */
    public int getValIndex( ) {
        return valIndex;
    }

    /** */
    @Override
    public Type getType() {
        return Type.Data;
    }

    /** */
    @Override
    protected String getValueImpl( FruContext context ) {

        if( valIndex == -1 )
            return null;

        if( hasFieldSplit() )
            return context.data().currentRow().getValue( valIndex, getWidth() );
        else
            return context.data().currentRow().getValue( valIndex );
    }
}
