package ru.inversion.fru.print.altprint;

import java.awt.print.PageFormat;
import java.awt.Font;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

public final class AltPrintPageConfig {

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * <br>
     * Immutable-конфигурация страницы и layout-параметров документа:
     * - media
     * - orientation
     * - margins
     * - font
     * - shrink policy
     * <p>
     * НЕ ОТВЕЧАЕТ ЗА:
     * - взаимодействие с PrinterJob
     * - взаимодействие с PrintService
     * - подбор MediaPrintableArea
     * - отправку задания на печать
     */
    public enum HeightOverflowPolicy {
        NONE,
        SHRINK_TO_FIT
    }

    public static final class Builder {

        private MediaSizeName media = MediaSizeName.ISO_A4;
        private OrientationRequested orientation;

        // поля документа, не hardware margins принтера
        private Double marginLeftMm;
        private Double marginTopMm;
        private Double marginRightMm;
        private Double marginBottomMm;

        private String fontFamily;
        private int fontStyle = Font.PLAIN;
        private Integer fontSizePt;

        private int copies = 1;

        private HeightOverflowPolicy heightOverflowPolicy = HeightOverflowPolicy.SHRINK_TO_FIT;

        // не ужимать ниже этого значения
        private Double minShrinkScale = 0.5d;

        // запас, чтобы не печатать "в кончик страницы"
        private Double shrinkReserveMm = 2.0d;

        public Builder copies( int v) {
            this.copies = v;
            return this;
        }

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
            this.fontStyle = style;
            this.fontSizePt = sizePt;
            return this;
        }

        public Builder fontName(String v) {
            this.fontFamily = v;
            return this;
        }

        public Builder fontSize(int v) {
            this.fontSizePt = v;
            return this;
        }

        public Builder bold(boolean on) {
            if (on) {
                this.fontStyle |= Font.BOLD;
            } else {
                this.fontStyle &= ~Font.BOLD;
            }
            return this;
        }

        public Builder italic(boolean on) {
            if (on) {
                this.fontStyle |= Font.ITALIC;
            } else {
                this.fontStyle &= ~Font.ITALIC;
            }
            return this;
        }

        public Builder heightOverflowPolicy(HeightOverflowPolicy v) {
            this.heightOverflowPolicy = (v != null) ? v : HeightOverflowPolicy.NONE;
            return this;
        }

        public Builder shrinkIfNeeded(boolean on) {
            this.heightOverflowPolicy = on
                    ? HeightOverflowPolicy.SHRINK_TO_FIT
                    : HeightOverflowPolicy.NONE;
            return this;
        }

        public Builder minShrinkScale(double v) {
            this.minShrinkScale = v;
            return this;
        }

        public Builder shrinkReserveMm(double v) {
            this.shrinkReserveMm = v;
            return this;
        }

        public AltPrintPageConfig build() {
            return new AltPrintPageConfig(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final MediaSizeName media;
    private final OrientationRequested orientation;
    private final Copies copies;

    private final Double marginLeftMm;
    private final Double marginTopMm;
    private final Double marginRightMm;
    private final Double marginBottomMm;

    private final String fontFamily;
    private final Integer fontStyle;
    private final Integer fontSizePt;

    private final HeightOverflowPolicy heightOverflowPolicy;
    private final Double minShrinkScale;
    private final Double shrinkReserveMm;

    private AltPrintPageConfig(Builder b) {

        this.media       = b.media;
        this.orientation = b.orientation;

        this.marginLeftMm  = b.marginLeftMm;
        this.marginTopMm   = b.marginTopMm;
        this.marginRightMm = b.marginRightMm;
        this.marginBottomMm= b.marginBottomMm;

        this.fontFamily = b.fontFamily;
        this.fontStyle  = b.fontStyle;
        this.fontSizePt = b.fontSizePt;

        this.heightOverflowPolicy = b.heightOverflowPolicy;
        this.minShrinkScale = b.minShrinkScale;
        this.shrinkReserveMm = b.shrinkReserveMm;

        this.copies = b.copies < 1 ? new Copies(1) : new Copies(b.copies);
    }

    /** */
    public Copies getCopies() { return copies; }

    /** */
    public MediaSizeName getMedia() {
        return media;
    }

    /** */
    public OrientationRequested getOrientation() {
        return orientation;
    }

    /** */
    public HeightOverflowPolicy getHeightOverflowPolicy() {
        return heightOverflowPolicy;
    }

    public boolean isShrinkEnabled() {
        return heightOverflowPolicy == HeightOverflowPolicy.SHRINK_TO_FIT;
    }

    /** */
    public double getMinShrinkScaleOrDefault()
    {
        double v = (minShrinkScale != null) ? minShrinkScale.doubleValue() : 0.5d;

        if( v <= 0.5d )
            return 0.5d;

        if( v > 1.0d )
            return 1.0d;

        return v;
    }

    public float getShrinkReservePtOrZero() {
        return shrinkReserveMm != null ? (float) mmToPt(shrinkReserveMm.doubleValue()) : 0f;
    }

    public float getMarginLeftPtOrZero() {
        return marginLeftMm != null ? (float) mmToPt(marginLeftMm.doubleValue()) : 0f;
    }

    public float getMarginTopPtOrZero() {
        return marginTopMm != null ? (float) mmToPt(marginTopMm.doubleValue()) : 0f;
    }

    public float getMarginRightPtOrZero() {
        return marginRightMm != null ? (float) mmToPt(marginRightMm.doubleValue()) : 0f;
    }

    public float getMarginBottomPtOrZero() {
        return marginBottomMm != null ? (float) mmToPt(marginBottomMm.doubleValue()) : 0f;
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Полезная ширина контента внутри printable area принтера.
     */
    public float getContentWidthPt(PageFormat pf) {
        float width = (float) pf.getImageableWidth()
                - getMarginLeftPtOrZero()
                - getMarginRightPtOrZero();

        if (width <= 0f) {
            throw new IllegalStateException("Document margins exceed printable width");
        }

        return width;
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Полезная высота контента внутри printable area принтера.
     */
    public float getContentHeightPt(PageFormat pf) {
        float height = (float) pf.getImageableHeight()
                - getMarginTopPtOrZero()
                - getMarginBottomPtOrZero();

        if (height <= 0f) {
            throw new IllegalStateException("Document margins exceed printable height");
        }

        return height;
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Безопасная ширина с запасом для shrink.
     */
    public float getSafeContentWidthPt(PageFormat pf) {
        float safe = getContentWidthPt(pf) - getShrinkReservePtOrZero();
        return (safe > 1f) ? safe : getContentWidthPt(pf);
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Безопасная высота с запасом для shrink.
     */
    public float getSafeContentHeightPt(PageFormat pf) {
        float safe = getContentHeightPt(pf) - getShrinkReservePtOrZero();
        return (safe > 1f) ? safe : getContentHeightPt(pf);
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Рассчитать shrink-scale так, чтобы контент влез по width/height,
     * но не увеличивать его и не опускаться ниже minShrinkScale.
     */
    public float resolveShrinkScale(PageFormat pf, float requiredWidthPt, float requiredHeightPt) {

        if( !isShrinkEnabled() )
            return 1.0f;

        float safeWidthPt  = getSafeContentWidthPt(pf);
        float safeHeightPt = getSafeContentHeightPt(pf);

        double scaleByWidth = 1.0d;
        if( requiredWidthPt > 0.01f )
            scaleByWidth = safeWidthPt / requiredWidthPt;

        double scaleByHeight = 1.0d;
        if( requiredHeightPt > 0.01f )
            scaleByHeight = safeHeightPt / requiredHeightPt;

        double scale = Math.min( 1.0d, Math.min(scaleByWidth, scaleByHeight) );
               scale = Math.max( scale, getMinShrinkScaleOrDefault() );

        return (float) scale;
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Собрать итоговый Font документа с fallback на printer default.
     */
    public Font resolveFont( Font printerDefault )
    {
        String family = fontFamily != null ? fontFamily : printerDefault.getFamily();
        int style = fontStyle != null ? fontStyle.intValue() : printerDefault.getStyle();
        int size = fontSizePt != null ? fontSizePt.intValue() : printerDefault.getSize();

        return new Font(family, style, size);
    }

    private static double mmToPt(double mm) {
        return mm * 72.0d / 25.4d;
    }
}