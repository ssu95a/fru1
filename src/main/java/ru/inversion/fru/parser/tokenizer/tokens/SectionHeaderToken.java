package ru.inversion.fru.parser.tokenizer.tokens;

import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.utils.parser.Token;

public class SectionHeaderToken extends Token<String> {

    /** */
    final private SectionTypeEnum sectionTypeEnum;

    /** */
    public SectionHeaderToken( String value, SectionTypeEnum type ) {
        super(Token.TypeEnum.FRU_SECTION_HEADER, value);
        this.sectionTypeEnum = type;
    }

    /** */
    public SectionTypeEnum getSectionType() {
        return sectionTypeEnum;
    }
}
