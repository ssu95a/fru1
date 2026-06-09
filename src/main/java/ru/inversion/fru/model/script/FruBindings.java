package ru.inversion.fru.model.script;

import ru.inversion.utils.Pair;

import javax.script.SimpleBindings;
import java.util.function.Function;

public class FruBindings extends SimpleBindings {

    private Function<String, Pair<Object,Boolean>> valuesSupplier;

    //private final Set<String> names = new HashSet<>();

    @Override
    public Object get(Object key)
    {
        if( key == null )
            return null;

        Object v = super.get(key);

        if( v == null && !super.containsKey(key) )
        {
            if( valuesSupplier != null )
            {
                Pair<Object, Boolean> pair = valuesSupplier.apply( (String) key );
                if( pair != null )
                {
                    v = pair.first;
                    if(!pair.second )
                        super.put( key.toString(), pair.first );
                }
//                if (v != null)
//                    names.add((String) key);
            }
        }
        return v;
    }

    /** */
    public void setValuesSupplier( Function<String, Pair<Object,Boolean>> valuesSupplier ) {
        this.valuesSupplier = valuesSupplier;
    }

    /** */
    public void clearValuesSupplier( ) {
        this.valuesSupplier = null;
    }

}
