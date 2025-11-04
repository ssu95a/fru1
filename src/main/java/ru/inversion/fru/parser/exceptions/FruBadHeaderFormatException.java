package ru.inversion.fru.parser.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

/** */
public class FruBadHeaderFormatException extends FruException {

    final private String header;
    final private int sectionNum;

    /** */
    public FruBadHeaderFormatException( String msg, String h, int n ) {
        super(msg + "\n" + h);
        header = h;
        sectionNum = n;
    }

    /** */
    public int getSectionNum() {
        return sectionNum;
    }

    /** */
    public String getHeader() {
        return header;
    }
}
