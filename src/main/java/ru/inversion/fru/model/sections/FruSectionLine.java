package ru.inversion.fru.model.sections;

import ru.inversion.fru.utils.constants.SectionTypeEnum;

import static ru.inversion.fru.utils.constants.SectionTypeEnum.LINE;

/** */
public class FruSectionLine extends FruSection{

    /**  @param num */
    public FruSectionLine(int num) {
        super( num );
    }
    /** */
    @Override
    public SectionTypeEnum getType() {
        return LINE;
    }
}
