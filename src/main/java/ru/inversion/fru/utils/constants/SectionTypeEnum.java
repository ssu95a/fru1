package ru.inversion.fru.utils.constants;

import ru.inversion.utils.U;

/** */
public enum SectionTypeEnum {

    ENTRY,      // #entry
    HEAD,       // #head
    LINE,       // #line
    TABLE,      // #table или #virtual table
    TEXT,       // #text или #virtual text
    TAIL,       // #tail
    END,
    COMMENT,    // комментарии
    UNKNOWN,    // неизвестный тип

    SCRIPT,
    CONTENT;

    /** */
    public boolean isSection()
    {
        return U.notIn( this, COMMENT, UNKNOWN, SCRIPT, CONTENT, END );
    }

    /** */
    public static SectionTypeEnum getType( String s )
    {
        if( s == null || s.isEmpty() )
            return null;

        if( s.startsWith("entry")) return ENTRY;
        if( s.startsWith("head" )) return HEAD;
        if( s.startsWith("line" )) return LINE;
        if( s.startsWith("table") || s.startsWith("virtual table")) return TABLE;
        if( s.startsWith("text" ) || s.startsWith("virtual text") ) return TEXT;
        if( s.startsWith("tail" )) return TAIL;
        if( s.startsWith("end"  )) return END;

        return null;
    }

}