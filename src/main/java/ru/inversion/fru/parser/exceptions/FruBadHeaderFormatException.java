package ru.inversion.fru.parser.exceptions;

import ru.inversion.fru.api.exceptions.FruException;

/** */
public class FruBadHeaderFormatException extends FruException {

    final private String header;

    /** */
    public FruBadHeaderFormatException( String msg, String h, int sectionNum ) {
        super(msg + "\n" + h);
        header = h;
    }

    /** */
    public String getHeader() {
        return header;
    }
}
