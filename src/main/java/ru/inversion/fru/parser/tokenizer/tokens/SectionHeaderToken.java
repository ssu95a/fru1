package ru.inversion.fru.parser.tokenizer.tokens;

import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.parser.nprsr.NewToken;

public class SectionHeaderToken extends NewToken<String> {

    /** */
    final private SectionTypeEnum sectionTypeEnum;

    /** */
    public SectionHeaderToken( String value, SectionTypeEnum type ) {
        super(NewToken.TypeEnum.FRU_SECTION_HEADER, value);
        this.sectionTypeEnum = type;
    }

    /** */
    public SectionTypeEnum getSectionType() {
        return sectionTypeEnum;
    }
}
