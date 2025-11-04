package ru.inversion.fru.model.sections;


import ru.inversion.fru.utils.constants.SectionTypeEnum;

/** */
public class FruSectionTail extends FruSection {

    public FruSectionTail(int num ) {
        super(num);
    }

    /**
     *
     */
    @Override
    public SectionTypeEnum getType() {
        return SectionTypeEnum.TAIL;
    }
}
