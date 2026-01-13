package ru.inversion.fru.model.script;

import javax.script.SimpleBindings;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class FruBindings extends SimpleBindings {

    private Function<String,Object> valuesSupplier;

    //private final Set<String> names = new HashSet<>();

    @Override
    public Object get(Object key)
    {
        Object v = super.get(key);

        if( v == null && !super.containsKey(key) )
        {
            if( valuesSupplier != null )
            {
                v = valuesSupplier.apply((String) key);
//                if (v != null)
//                    names.add((String) key);
            }
        }
        return v;
    }

    /** */
    public void setValuesSupplier( Function<String, Object> valuesSupplier ) {
        this.valuesSupplier = valuesSupplier;
    }

    /** */
    public void clearValuesSupplier( ) {
        this.valuesSupplier = null;
    }

}
