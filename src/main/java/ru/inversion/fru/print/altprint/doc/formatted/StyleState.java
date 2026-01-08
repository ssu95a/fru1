package ru.inversion.fru.print.altprint.doc.formatted;

import java.awt.*;

/** */
public class StyleState {

    private final String  fontName;
    private final int     fontSize;
    private final int     fontStyle;
    private final boolean underline;
    private final float   spaceAfter;
    private final float   leftIndent;

    private transient Font cachedFont;

    /** */
    public StyleState( String fontName, int fontSize, int fontStyle, boolean underline, float spaceAfter, float leftIndent) {
        this.fontName   = fontName;
        this.fontSize   = fontSize;
        this.fontStyle  = fontStyle;
        this.underline  = underline;
        this.spaceAfter = spaceAfter;
        this.leftIndent = leftIndent;
    }

    /** */
    public Font font( )
    {
        Font f = cachedFont;

        if( f == null)
        {
            f = new Font(fontName, fontStyle, fontSize);
            cachedFont = f;
        }

        return f;
    }

    /** */
    public boolean underline() {
        return underline;
    }

    /** */
    public float spaceAfter() {
        return spaceAfter;
    }

    /** */
    public float leftIndent() {
        return leftIndent;
    }

    // точка входа для ALTCommand
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** */
    public static final class Builder {

        private String  fontName;
        private int     fontSize;
        private int     fontStyle;
        private boolean underline;
        private float   spaceAfter;
        private float   leftIndent;

        private Builder(StyleState base) {
            this.fontName   = base.fontName;
            this.fontSize   = base.fontSize;
            this.fontStyle  = base.fontStyle;
            this.underline  = base.underline;
            this.spaceAfter = base.spaceAfter;
            this.leftIndent = base.leftIndent;
        }

        public Builder fontName(String v) {
            this.fontName = v;
            return this;
        }

        public Builder fontSize(int v) {
            this.fontSize = v;
            return this;
        }

        public Builder bold(boolean on) {
            if (on)
                this.fontStyle |= Font.BOLD;
            else
                this.fontStyle &= ~Font.BOLD;
            return this;
        }

        public Builder italic(boolean on) {
            if (on)
                this.fontStyle |= Font.ITALIC;
            else
                this.fontStyle &= ~Font.ITALIC;
            return this;
        }

        public Builder underline(boolean v) {
            this.underline = v;
            return this;
        }

        public Builder spaceAfter(float v) {
            this.spaceAfter = v;
            return this;
        }

        public Builder leftIndent(float v) {
            this.leftIndent = v;
            return this;
        }

        public StyleState build() {
            return new StyleState( fontName, fontSize, fontStyle, underline, spaceAfter, leftIndent );
        }
    }
}
