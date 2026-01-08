package ru.inversion.fru.print.altprint;

import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;

public class AltPrintPageConfig {

    public static class Builder {

        private MediaSizeName media;
        private OrientationRequested orientation;

        private Double marginLeftMm;
        private Double marginTopMm;
        private Double marginRightMm;
        private Double marginBottomMm;

        private String fontFamily;
        private Integer fontStyle;
        private Integer fontSizePt;

        public Builder media(MediaSizeName v) {
            this.media = v;
            return this;
        }

        public Builder orientation(OrientationRequested v) {
            this.orientation = v;
            return this;
        }

        public Builder marginLeftMm(double v) {
            this.marginLeftMm = v;
            return this;
        }

        public Builder marginTopMm(double v) {
            this.marginTopMm = v;
            return this;
        }

        public Builder marginRightMm(double v) {
            this.marginRightMm = v;
            return this;
        }

        public Builder marginBottomMm(double v) {
            this.marginBottomMm = v;
            return this;
        }

        public Builder font(String family, int style, int sizePt) {
            this.fontFamily = family;
            this.fontStyle  = style;
            this.fontSizePt = sizePt;
            return this;
        }

        public AltPrintPageConfig build() {
            return new AltPrintPageConfig(this);
        }
    }


    /** */
    public static Builder builder() {
        return new Builder();
    }

    /* page overrides (null = взять из принтера) */
    private final MediaSizeName media;
    private final OrientationRequested orientation;

    private final Double marginLeftMm;
    private final Double marginTopMm;
    private final Double marginRightMm;
    private final Double marginBottomMm;

    /* font overrides */
    private final String  fontFamily;
    private final Integer fontStyle;
    private final Integer fontSizePt;

    /** */
    private AltPrintPageConfig( Builder b)
    {
        this.media          = b.media;
        this.orientation    = b.orientation;
        this.marginLeftMm   = b.marginLeftMm;
        this.marginTopMm    = b.marginTopMm;
        this.marginRightMm  = b.marginRightMm;
        this.marginBottomMm = b.marginBottomMm;
        this.fontFamily     = b.fontFamily;
        this.fontStyle      = b.fontStyle;
        this.fontSizePt     = b.fontSizePt;
    }

    public PageFormat merge( PageFormat printerPf )
    {

        PageFormat pf = (PageFormat) printerPf.clone();
        Paper paper = pf.getPaper();

        if( media != null )
        {
            MediaSize ms = MediaSize.getMediaSizeForName(media);
            double wPt = mmToPt(ms.getX(MediaSize.MM));
            double hPt = mmToPt(ms.getY(MediaSize.MM));

            paper.setSize(wPt, hPt);
        }

        if( orientation != null )
            pf.setOrientation( orientation == OrientationRequested.LANDSCAPE ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT );

        double left   = marginLeftMm   != null ? mmToPt(marginLeftMm)   : paper.getImageableX();
        double top    = marginTopMm    != null ? mmToPt(marginTopMm)    : paper.getImageableY();

        double right  = marginRightMm  != null ? mmToPt(marginRightMm)  : paper.getWidth() - (paper.getImageableX() + paper.getImageableWidth());

        double bottom = marginBottomMm != null ? mmToPt(marginBottomMm) : paper.getHeight() - (paper.getImageableY() + paper.getImageableHeight());

        double width  = paper.getWidth()  - left - right;
        double height = paper.getHeight() - top  - bottom;

        if( width <= 0 || height <= 0 )
            throw new IllegalStateException("Margins exceed printable area");

        paper.setImageableArea(left, top, width, height);
        pf.setPaper(paper);

        return pf;
    }

    /** */
    private static double mmToPt(double mm) {
        return mm * 72.0 / 25.4;
    }

    /** */
    public Font resolveFont(Font printerDefault) {

        String family = fontFamily != null ? fontFamily : printerDefault.getFamily();
        int style     = fontStyle  != null ? fontStyle  : printerDefault.getStyle();
        int size      = fontSizePt != null ? fontSizePt : printerDefault.getSize();

        return new Font(family, style, size);
    }

}
