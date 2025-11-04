package ru.inversion.fru.model.fields.functions;

import java.util.function.Supplier;

/** */
public enum FunctionCategoryEnum {

    REPORT     ( ScopeEnum.REPORT,  "Report Functions", "Global functions that work across the entire report context" ),

    SECTION    ( ScopeEnum.SECTION, "Section Functions", "Functions that work within the context of a specific section" ),

    AGGREGATE  ( ScopeEnum.SECTION_DATA, "Aggregate Functions", "Functions for calculating data aggregates and summaries within sections" ),

    STRING     ( ScopeEnum.UNIVERSAL, "String Functions", "Functions for string manipulation, text processing and transformation" ),

    DATE_TIME  ( ScopeEnum.UNIVERSAL, "Date and Time Functions", "Functions for working with dates, time values and temporal operations" ),

    CALCULATION( ScopeEnum.UNIVERSAL, "Calculation Functions", "Mathematical, arithmetic and numerical calculations" ),

    FORMATTING ( ScopeEnum.UNIVERSAL, "Formatting Functions", "Functions for value formatting, presentation and visual enhancement" ),

    CONVERSION ( ScopeEnum.UNIVERSAL, "Conversion Functions", "Functions for data type conversion, casting and transformation"),

    CONDITIONAL( ScopeEnum.UNIVERSAL, "Conditional Functions", "Functions for conditional logic, branching and decision making"),

    DATA_ACCESS( ScopeEnum.SECTION_DATA, "Data Access Functions", "Functions for accessing, retrieving and manipulating data fields")
    ;
    /**
     * Область видимости функции
     */
    public enum ScopeEnum {
        REPORT,         // Глобальная область отчета
        SECTION,        // В рамках секции
        SECTION_DATA,   // В рамках данных секции
        UNIVERSAL       // Универсальная область
    };

    private final String    name;
    private final String    note;
    private final ScopeEnum scope;

    /** */
    FunctionCategoryEnum( ScopeEnum scope, String name, String note ) {
        this.name = name;
        this.note = note;
        this.scope = scope;


    }
}
