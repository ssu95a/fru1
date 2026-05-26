package ru.inversion.fru.model.fields.functions;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.utils.S;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static ru.inversion.fru.model.fields.functions.FunctionCategoryEnum.*;

/** */
public enum BuiltinFunctionEnum {

    PAGE( "Номер текущей страницы", REPORT, (IFunctionImpl<Void, Integer>) (context, unused) -> context.getCurrentPageNum()),

    TOTAL_PAGES("Общее количество страниц", REPORT, new IFunctionImpl<Void, Integer>() {
        @Override
        public Integer apply(FruContext context, Void unused) {
            return context.getCurrentPageNum() + 1;
        }
    }),

    COUNT( "Счетчик использования секции", SECTION, new IFunctionImpl<Void, Integer>() {
        @Override
        public Integer apply(FruContext context, Void unused) {
            return context.currentSection() != null ? context.currentSection().getUseCount() : 0;
        }
    }),

    SECTION_NUM( "Номер текущей секции", SECTION, new IFunctionImpl<Void, Integer>() {
        @Override
        public Integer apply(FruContext context, Void unused) {
            return context.currentSection() != null ? context.currentSection().getNum() : -1;
        }
    }),

    ROW("Номер строки данных", SECTION, new IFunctionImpl<Void, Integer>() {
        @Override
        public Integer apply(FruContext context, Void unused) {
            return 0; //context.data() != null ? context.data().currentRow(). : 0;
        }
    }),

    NULL("Пустое значение", SECTION, new IFunctionImpl<Void, String>() {
        @Override
        public String apply(FruContext context, Void unused) {
            return S.EMPTY_STRING;
        }
    }),

    UPPER("Верхний регистр", STRING, new IFunctionImpl<String, String>() {
        @Override
        public String apply(FruContext context, String fieldName) {
            if( fieldName == null || context.currentSection() == null )
                return S.EMPTY_STRING;
            Object value = context.currentSection().getFieldValue( context, fieldName );
            return value != null ? value.toString().toUpperCase() : S.EMPTY_STRING;
        }
    }),

    LOWER("Нижний регистр", STRING, new IFunctionImpl<String, String>() {
        @Override
        public String apply(FruContext context, String fieldName) {
            if( fieldName == null || context.currentSection() == null)
                return S.EMPTY_STRING;
            Object value = context.currentSection().getFieldValue(context, fieldName);
            return value != null ? value.toString().toLowerCase() : S.EMPTY_STRING;
        }
    }),

    TRIM( "Удаление пробелов", STRING, new IFunctionImpl<String, String>() {
        @Override
        public String apply(FruContext context, String fieldName) {
            if (fieldName == null || context.currentSection() == null)
                return S.EMPTY_STRING;
            Object value = context.currentSection().getFieldValue(context, fieldName);
            return value != null ? value.toString().trim() : S.EMPTY_STRING;
        }
    }),

    SUBSTRING( "Извлечение подстроки", STRING, new IFunctionImpl<String, String>() {
        @Override
        public String apply( FruContext context, String params ) {

            if( params == null || context.currentSection() == null )
                return S.EMPTY_STRING;

            String[] parts = params.split(",");
            if( parts.length < 2 )
                return S.EMPTY_STRING;

            final String fieldName = parts[0].trim();
            Object value = context.currentSection().getFieldValue( context, fieldName );
            if( value == null )
                return S.EMPTY_STRING;

            String text = value.toString();

            try {

                int start = Integer.parseInt(parts[1].trim());
                if( parts.length > 2 ) {
                    int length = Integer.parseInt(parts[2].trim());
                    int end = Math.min(start + length, text.length());
                    return text.substring(start, end);
                }
                else
                {
                    return text.substring(start);
                }
            } catch (NumberFormatException e) {
                return S.EMPTY_STRING;
            }
        }
    }),

    LENGTH("Длина строки", STRING, new IFunctionImpl<String, Integer>() {
        @Override
        public Integer apply(FruContext context, String fieldName) {
            if (fieldName == null || context.currentSection() == null) return 0;
            Object value = context.currentSection().getFieldValue(context, fieldName);
            return value != null ? value.toString().length() : 0;
        }
    }),

    CURRENT_DATE("Текущая дата", DATE_TIME, new IFunctionImpl<Void, LocalDate>() {
        @Override
        public java.time.LocalDate apply(FruContext context, Void unused) {
            return java.time.LocalDate.now();
        }
    }),

    CURRENT_TIME("Текущее время", DATE_TIME, new IFunctionImpl<Void, LocalTime>() {
        @Override
        public java.time.LocalTime apply(FruContext context, Void unused) {
            return java.time.LocalTime.now();
        }
    }),

    CURRENT_DATETIME("Текущие дата и время", DATE_TIME, new IFunctionImpl<Void, LocalDateTime>() {
        @Override
        public java.time.LocalDateTime apply(FruContext context, Void unused) {
            return java.time.LocalDateTime.now();
        }
    }),

    COALESCE( "Первое непустое значение", CONDITIONAL, new IFunctionImpl<String, String>() {
        @Override
        public String apply (FruContext context, String params ) {

            if( params == null || context.currentSection() == null )
                return S.EMPTY_STRING;

            String[] fieldNames = params.split(",");
            for( String fieldName : fieldNames)
            {
                Object value = context.currentSection().getFieldValue( context, fieldName.trim() );
                if (value != null && !value.toString().isEmpty()) {
                    return value.toString();
                }
            }
            return S.EMPTY_STRING;
        }
    });

    private final String note;
    private final FunctionCategoryEnum category;
    private final IFunctionImpl function;

    <P,V> BuiltinFunctionEnum( String note, FunctionCategoryEnum category, IFunctionImpl<P,V> function) {
        this.note = note;
        this.category = category;
        this.function = function;
    }

    /** */
    public <P,V> V execute( FruContext context, P param )
    {
        return (V) this.function.apply( context, param );
    }

    /** */
    public static BuiltinFunctionEnum find( String name ) {

        if( S.isNullOrEmpty(name) )
            return null;

        for( BuiltinFunctionEnum value : BuiltinFunctionEnum.class.getEnumConstants() )  {
            if( value.name().equals(name))
                return value;
        }

        return null;
    }

}
