package ru.inversion.fru.print.naltprn.cmd;

public enum AltParameterTypeEnum
{
    FONT_NAME,  FONT_SIZE,  ITALIC,  BOLD,  UNDERLINE,  ORIENTATION,  LEFT,  COPIES,  SPACE_AFTER,  FRU_COMMAND,  PAGE_END,  LF;

    AltParameterTypeEnum() {}

    static AltParameterTypeEnum fromString(String name)
    {
        if( name.equalsIgnoreCase("Name Font") ) {
            return FONT_NAME;
        }
        if (name.equalsIgnoreCase("Size Font")) {
            return FONT_SIZE;
        }
        if (name.equalsIgnoreCase("Italic")) {
            return ITALIC;
        }
        if (name.equalsIgnoreCase("Bold")) {
            return BOLD;
        }
        if (name.equalsIgnoreCase("Under")) {
            return UNDERLINE;
        }
        if (name.equalsIgnoreCase("Orientation")) {
            return ORIENTATION;
        }
        if (name.equalsIgnoreCase("Left")) {
            return LEFT;
        }
        if (name.equalsIgnoreCase("Set Copies")) {
            return COPIES;
        }
        if (name.equalsIgnoreCase("Vertical Move")) {
            return SPACE_AFTER;
        }
        if (name.equalsIgnoreCase("Cmd")) {
            return FRU_COMMAND;
        }
        if (name.equalsIgnoreCase("Page End")) {
            return PAGE_END;
        }
        if (name.equalsIgnoreCase("Lf")) {
            return LF;
        }
        return null;
    }
}
