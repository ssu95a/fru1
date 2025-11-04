package ru.inversion.fru.model.items;

/** */
public class FruText extends FruItem {
    /** */
    final private String text;
    /** */
    public FruText( String text ) {
        this.text = text;
    }
    /** */
    public String getText() { return text; }
}
