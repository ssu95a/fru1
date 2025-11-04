package ru.inversion.fru.model.fields;


import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.model.exceptions.FruFieldFactoryException;
import ru.inversion.fru.model.exceptions.FruFieldNotFoundException;
import ru.inversion.fru.model.fields.functions.BuiltinFunctionEnum;
import ru.inversion.fru.model.fields.types.FruFieldArg;
import ru.inversion.fru.model.fields.types.FruFieldFun;
import ru.inversion.fru.model.fields.types.FruFieldStr;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruText;
import ru.inversion.utils.S;

/** */
public abstract class FruField extends FruItem {

    /** */
    public enum Type {
        Data,
        Str,
        Arg,
        Func,
    };

    final protected String name;
    final protected FruFormatter formatter;

    /** */
    protected FruField( String name, FruFormatter formatter ) {
        this.name      = name;
        this.formatter = formatter;
    }

    /** */
    public boolean hasFieldSplit( )
    {
        return formatter != null && formatter.getSplitMode() == 1;
    }

    /** */
    public String getName() { return name; }

    /** */
    public abstract Type getType( );

    /** */
    protected abstract String getValueImpl( FruContext context );

    /** */
    public int getWidth() {
        return this.formatter == null ? -1 :formatter.getWidth();
    }

    /** */
    public FruFormatter getFormatter() {
        return formatter;
    }

    /** */
    public String getValue( FruContext context )
    {
        return getValueImpl(context);
    }

    /** */
    public static FruField make(FruBuilder fruBuilder, String name, FruFormatter formatter, int dataIndex )
    {
        try {

            // 1. Проверка встроенных функций
            final BuiltinFunctionEnum func = BuiltinFunctionEnum.find(name);
            if( func != null )
                return new FruFieldFun( func, formatter );

            // 2. Проверка строк и аргументов
            String value = fruBuilder.strings.get(name);
            if( !S.isNullOrEmpty(value) )
                return new FruFieldStr( name, formatter );

            if( fruBuilder.argumentList.stream().anyMatch(fn->fn.equals(name) ) )
                return new FruFieldArg( name, formatter );

            if( dataIndex >= 0 )
                return new FruFieldVal( name, dataIndex, formatter );

            FruFormat f = fruBuilder.formats.get(name);
            if( f.getItems().size() == 1 && f.getItems().get(0) instanceof FruText) {
                fruBuilder.strings.put( name, ((FruText) f.getItems().get(0)).getText() );
                return new FruFieldStr( name, null );
            }


            throw new FruFieldNotFoundException(name);

        } catch( Exception e ) {
            throw new FruFieldFactoryException( name, e.getMessage() );
        }
    }

}
