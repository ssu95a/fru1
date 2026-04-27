package ru.inversion.fru.print.altprint.doc.styled;

import java.awt.*;

/** */
public class StyleState {

    private final String  fontName;
    private final int     fontSize;
    private final int     fontStyle;
    private final boolean underline;
    private final float   verticalMovePt;
    private final float   leftIndent;
    private final float   upperIndent;

    private transient Font cachedFont;

    /** */
    public StyleState(String fontName, int fontSize, int fontStyle, boolean underline, float verticalMovePt, float leftIndent, float upperIndent) {
        this.fontName   = fontName;
        this.fontSize   = fontSize;
        this.fontStyle  = fontStyle;
        this.underline  = underline;
        this.verticalMovePt = verticalMovePt;
        this.leftIndent = leftIndent;
        this.upperIndent = upperIndent;
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
    public float verticalMovePt() {
        return verticalMovePt;
    }

    /** */
    public float leftIndent() {
        return leftIndent;
    }
    /** */
    public float upperIndent() {
        return upperIndent;
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
        private float verticalMovePt;
        private float   leftIndent;
        private float   upperIndent;

        private Builder(StyleState base) {
            this.fontName    = base.fontName;
            this.fontSize    = base.fontSize;
            this.fontStyle   = base.fontStyle;
            this.underline   = base.underline;
            this.verticalMovePt = base.verticalMovePt;
            this.leftIndent  = base.leftIndent;
            this.upperIndent = base.upperIndent;
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

        public Builder verticalMovePt(float v) {
            this.verticalMovePt = v;
            return this;
        }

        public Builder leftIndent(float v) {
            this.leftIndent = v;
            return this;
        }

        public Builder upperIndent(float v) {
            this.upperIndent = v;
            return this;
        }

        public StyleState build() {
            return new StyleState( fontName, fontSize, fontStyle, underline, verticalMovePt, leftIndent, upperIndent);
        }
    }

    /** */
    private static StyleState defaultPlainStyle () {

        return new StyleState( "Monospaced", 10, Font.PLAIN, false, 0.0f, 0.5f, 0.0f );
    }
}
