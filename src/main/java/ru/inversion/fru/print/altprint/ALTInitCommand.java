package ru.inversion.fru.print.altprint;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;

class ALTInitCommand
      extends ALTCommand
{
    public ALTInitCommand(String name, String note)
    {
        super(name, note);
    }

    public boolean isCssSupported() {
        return false;
    }


    public OrientationRequested getOrientation()
    {
        for (ALTParameter<?> p : getGraphicData().getParameters()) {
            if (p.getType() == ALTParameterTypeEnum.ORIENTATION) {
                return ((ALTParameter.PageOrientationParameter)p).getValue();
            }
        }
        return ALTSettings.INSTANCE().defSetting().getOrientation();
    }

    public Copies getCopies()
    {
        for (ALTParameter<?> p : getGraphicData().getParameters() ) {
            if (p.getType() == ALTParameterTypeEnum.COPIES) {
                return ((ALTParameter.CopiesParameter)p).getValue();
            }
        }
        return ALTSettings.INSTANCE().defSetting().getCopies();
    }
}
