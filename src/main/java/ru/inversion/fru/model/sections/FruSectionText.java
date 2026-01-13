package ru.inversion.fru.model.sections;

import ru.inversion.fru.utils.constants.SectionTypeEnum;

import java.util.List;

public class FruSectionText extends FruSection {

    /** */
    final private List<String> fieldList;

    /** */
    public FruSectionText( int num, List<String> fieldList ) {
        super(num);
        this.fieldList = fieldList;
    }

    /** */
    @Override
    public SectionTypeEnum getType( ) {
        return SectionTypeEnum.TEXT;
    }
}
