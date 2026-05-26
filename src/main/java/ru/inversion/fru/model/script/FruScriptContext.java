package ru.inversion.fru.model.script;

import ru.inversion.utils.Pair;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import java.util.function.Function;

public class FruScriptContext extends SimpleScriptContext {

    public FruScriptContext() {
        super();
        this.globalScope = new FruBindings();
    }

    @Override
    public void setBindings(Bindings bindings, int scope)
    {
        if( bindings != null )
            globalScope.putAll( bindings );
    }

    @Override
    public Bindings getBindings(int scope) {
        return globalScope;
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        globalScope.put(name, value);
    }

    @Override
    public Object getAttribute( String name, int scope ) {
        return globalScope.get(name);
    }

    @Override
    public Object removeAttribute(String name, int scope) {
        return globalScope.remove(name);
    }

    @Override
    public int getAttributesScope(String name) {
        return globalScope.containsKey(name) ? ScriptContext.GLOBAL_SCOPE : -1;
    }

    /** */
    public void setValuesSupplier( Function<String, Pair<Object,Boolean>> valuesSupplier ) {
        ((FruBindings)this.globalScope).setValuesSupplier(valuesSupplier);
    }

    /** */
    public void clearValuesSupplier( ) {
        ((FruBindings)this.globalScope).clearValuesSupplier();
    }
}
