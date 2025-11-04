package ru.inversion.fru.model.sections;


import ru.inversion.fru.utils.constants.SectionTypeEnum;

/** */
public class FruSectionHeader extends FruSection {

    public FruSectionHeader(int num ) {
        super( num );
    }
    /** */
    @Override
    public SectionTypeEnum getType() {
        return SectionTypeEnum.HEAD;
    }
}
