package ru.inversion.fru.model.fields;


import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.generator.renderer.LocalSplitState;
import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.model.exceptions.FruModelException;
import ru.inversion.fru.model.fields.functions.BuiltinFunctionEnum;
import ru.inversion.fru.model.fields.types.*;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruText;
import ru.inversion.fru.parser.model.SectionNode;
import ru.inversion.utils.S;

/** */
public abstract class FruField extends FruItem {

    /** */
    public enum Type {
        Data,
        Str,
        Arg,
        Func,
        Text,
        ScriptArg // аргумент скрипта
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
        return formatter != null && formatter.getSplitMode() > 0; //
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
        // Обычное поле без /z не должно попадать в local split-flow.
        if( !hasFieldSplit() )
            return getValueImpl(context);

        // LocalSplitState state = context.getOrCreateLocalSplitState(this);
        LocalSplitState state = context.findLocalSplitState(this);

        if( state != null && state.isActive() )
        {
            if( S.isNotNullOrEmpty(state.getPending()) )
                return state.getPending();

            if( state.isConsumed() )
                return S.EMPTY_STRING;
        }

        return getValueImpl(context);
    }

    @Override
    public String toString() {
        return "FruField{name='" + name + "', type = " + getType() + " }";
    }

    /** */
    public String getKey()
    {
        return name + ":" + getType();
    }

    /** */
    public static FruField make( FruBuilder fruBuilder, String name, FruFormatter formatter, int dataIndex, SectionNode sectionNode)
    {
        try {

            if( S.isNullOrEmpty(name) )
                throw new IllegalArgumentException("'name' is null");

            // Текстовые константы, строки в кавычках
            if( name.charAt(0) == '"' && S.lastChar(name) == '"' )
                return new FruFieldTxt( name.substring(1,name.length()-1), formatter );

            // Проверка встроенных функций
            final BuiltinFunctionEnum func = BuiltinFunctionEnum.find(name);
            if( func != null )
                return new FruFieldFun( func, formatter );

            // Строки
            if( fruBuilder.strings.containsKey(name) )
                return new FruFieldStr( name, formatter );

            // Аргументы формы
            if( fruBuilder.argumentList.stream().anyMatch(fn->fn.equals(name) ) )
                return new FruFieldArg( name, formatter );

            // Поля с данными
            if( dataIndex >= 0 )
                return new FruFieldVal( name, dataIndex, formatter );

            // Форматы
            final FruFormat f = fruBuilder.formats.get(name);
            if( f != null ) {
                if (f.getItems().size() == 1 && f.getItems().get(0) instanceof FruText) {
                    fruBuilder.strings.put(name, ((FruText) f.getItems().get(0)).getText());
                    //если в формате только текст, заменяем на строку
                    return new FruFieldStr(name, null);
                }
            }

            // Аргумент скрипта
            if( fruBuilder.findScriptParameter(name) )
                return new FruFieldScr( name, formatter);

            if( sectionNode != null  && sectionNode.findScriptParameter(name) )
                return new FruFieldScr( name, formatter);

            return new FruFieldArg( name, formatter );

        } catch( Exception e ) {
            throw new FruModelException( "Error on make field: " + name, e );
        }
    }
}
