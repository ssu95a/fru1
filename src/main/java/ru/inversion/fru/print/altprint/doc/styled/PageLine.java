package ru.inversion.fru.print.altprint.doc.styled;

import java.awt.font.TextLayout;

public class PageLine {

    private final TextLayout layout;
    private final float x;
    private final float baselineY;
    private final StyleState style;

    public PageLine( TextLayout layout, float x, float baselineY, StyleState style )
    {
        this.layout = layout;
        this.x      = x;
        this.baselineY = baselineY;
        this.style  = style;
    }

    /** */
    public TextLayout layout() {
        return layout;
    }

    /** */
    public float x() {
        return x;
    }

    /** */
    public float baselineY() {
        return baselineY;
    }

    /** */
    public StyleState style() {
        return style;
    }
}
