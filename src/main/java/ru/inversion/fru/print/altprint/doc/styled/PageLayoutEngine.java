package ru.inversion.fru.print.altprint.doc.styled;

import ru.inversion.fru.print.altprint.AltPrintPageConfig;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.List;

public final class PageLayoutEngine {

    private static final class LinePart {

        private final TextLayout layout;
        private final float x;
        private final StyleState style;

        private LinePart(
                TextLayout layout,
                float x,
                StyleState style
        ) {
            this.layout = layout;
            this.x = x;
            this.style = style;
        }
    }

    private static final class ChunkLayoutResult {

        private final int     nextSpanIndex;
        private final float   requiredHeightPt;
        private final boolean overflowByHeight;

        private final float   requiredWidthPt;
        private final boolean overflowByWidth;

        private ChunkLayoutResult (
            int nextSpanIndex,
            float requiredHeightPt,
            boolean overflowByHeight,
            float requiredWidthPt,
            boolean overflowByWidth
        )
        {
            this.nextSpanIndex    = nextSpanIndex;
            this.requiredHeightPt = requiredHeightPt;
            this.overflowByHeight = overflowByHeight;
            this.requiredWidthPt  = requiredWidthPt;
            this.overflowByWidth  = overflowByWidth;
        }

        public int getNextSpanIndex() {
            return nextSpanIndex;
        }

        public float getRequiredHeightPt() {
            return requiredHeightPt;
        }

        public boolean isOverflowByHeight() {
            return overflowByHeight;
        }

        public float getRequiredWidthPt() {
            return requiredWidthPt;
        }

        public boolean isOverflowByWidth() {
            return overflowByWidth;
        }
    }

    public static final class LaidOutPage {

        private final List<PageLine> lines;
        private final float   requiredHeightPt;
        private final boolean overflowByHeight;

        private final float   requiredWidthPt;
        private final boolean overflowByWidth;

        public LaidOutPage (
                List<PageLine> lines,
                float requiredHeightPt,
                boolean overflowByHeight,
                float requiredWidthPt,
                boolean overflowByWidth
        ) {
            this.lines = lines;
            this.requiredHeightPt = requiredHeightPt;
            this.overflowByHeight = overflowByHeight;
            this.requiredWidthPt  = requiredWidthPt;
            this.overflowByWidth  = overflowByWidth;
        }

        public List<PageLine> getLines() {
            return lines;
        }

        public float getRequiredHeightPt() {
            return requiredHeightPt;
        }

        public boolean isOverflowByHeight() {
            return overflowByHeight;
        }

        public float getRequiredWidthPt() {
            return requiredWidthPt;
        }

        public boolean isOverflowByWidth() {
            return overflowByWidth;
        }
    }

    /** */
    private final IStyledTextParser parser;

    private final float pageWidth;
    private final float pageHeight;

    private final float startX;
    private final float startY;
    private final float endY;

    private final float scale;
    private final FontRenderContext frc;

    private float cursorY;

    private boolean eof;

    /*
     * Если chunk не влез целиком, переносим остаток на следующую страницу.
     * pendingSpanIndex = индекс span, с которого надо продолжить.
     */
    private IStyledTextParser.StyledTextChunk pendingChunk;
    private int pendingSpanIndex;

    private transient float defaultLineHeight = Float.NaN;

    /** */
    public PageLayoutEngine(
            IStyledTextParser parser,
            Graphics2D g2d,
            PageFormat pf,
            AltPrintPageConfig cfg,
            float scale
    )
    {
        if( parser == null )
            throw new IllegalArgumentException("parser == null");
        if( g2d == null )
            throw new IllegalArgumentException("g2d == null");
        if( pf == null )
            throw new IllegalArgumentException("pf == null");
        if( cfg == null )
            throw new IllegalArgumentException("cfg == null");
        if( scale <= 0f )
            throw new IllegalArgumentException("scale must be > 0");

        this.parser = parser;
        this.scale  = scale;

        this.startX = cfg.getMarginLeftPtOrZero();
        this.startY = cfg.getMarginTopPtOrZero();

        /*
         * Рисование потом будет через g2d.scale(scale, scale),
         * поэтому в логических координатах доступная область становится больше:
         * printable / scale
         */
        this.pageWidth  = cfg.getContentWidthPt(pf) / scale;
        this.pageHeight = cfg.getContentHeightPt(pf) / scale;

        if( pageWidth <= 0f )
            throw new IllegalStateException("Document margins exceed printable width");
        if( pageHeight <= 0f )
            throw new IllegalStateException("Document margins exceed printable height");

        this.endY    = startY + pageHeight;
        this.cursorY = startY;

        this.frc = g2d.getFontRenderContext();
    }

    public float getScale() {
        return scale;
    }

    /**
     * Формирует следующую страницу.
     * @return LaidOutPage или null, если страниц больше нет
     */
    public LaidOutPage nextPreparedPage() {

        if( eof && pendingChunk == null )
            return null;

        while( true )
        {
            cursorY = startY;

            List<PageLine> page = new ArrayList<PageLine>();

            boolean pageBreak = false;
            boolean overflowByHeight = false;

            float requiredHeightPt = 0f;

            boolean overflowByWidth = false;
            float requiredWidthPt = 0f;

            while( true )
            {
                //Сначала продолжаем незавершённый chunk
                if( pendingChunk != null )
                {
                    ChunkLayoutResult r = layoutChunk( pendingChunk, pendingSpanIndex, page );

                    if( r.getRequiredHeightPt() > requiredHeightPt )
                        requiredHeightPt = r.getRequiredHeightPt();

                    if( r.getRequiredWidthPt() > requiredWidthPt )
                        requiredWidthPt = r.getRequiredWidthPt();

                    if( r.isOverflowByWidth() )
                        overflowByWidth = true;

                    if( r.getNextSpanIndex() >= 0)
                    {
                        pendingSpanIndex = r.getNextSpanIndex();
                        pageBreak = true;
                        overflowByHeight = r.isOverflowByHeight();
                        break;
                    }
                    else
                    {
                        pendingChunk = null;
                        pendingSpanIndex = 0;
                        continue;
                    }
                }

                // 2. Читаем новый элемент из parser
                if( !parser.hasNext() ) {
                    eof = true;
                    break;
                }

                IStyledTextParser.ParsedElement el = parser.next();

                if( el == null ) {
                    eof = true;
                    break;
                }

                if( el instanceof IStyledTextParser.TextFlowControl )
                {
                    IStyledTextParser.TextFlowControl fc = (IStyledTextParser.TextFlowControl) el;

                    if( fc == IStyledTextParser.TextFlowControl.PAGE_FEED) {
                        pageBreak = true;
                        break;
                    }

                    if( fc == IStyledTextParser.TextFlowControl.LINE_FEED )
                    {
                        float newCursorY   = cursorY + defaultLineHeight();
                        float neededHeight = newCursorY - startY;

                        if( neededHeight > requiredHeightPt )
                            requiredHeightPt = neededHeight;

                        if( newCursorY > endY ) {
                            pageBreak        = true;
                            overflowByHeight = true;

                            break;
                        }

                        cursorY = newCursorY;
                    }

                    continue;
                }

                if( el instanceof IStyledTextParser.StyledTextChunk )
                {
                    IStyledTextParser.StyledTextChunk chunk = (IStyledTextParser.StyledTextChunk) el;

                    ChunkLayoutResult r = layoutChunk(chunk, 0, page);

                    if( r.getRequiredHeightPt() > requiredHeightPt )
                        requiredHeightPt = r.getRequiredHeightPt();

                    if( r.getRequiredWidthPt() > requiredWidthPt )
                        requiredWidthPt = r.getRequiredWidthPt();

                    if( r.isOverflowByWidth() )
                        overflowByWidth = true;

                    if( r.getNextSpanIndex() >= 0 )
                    {
                        pendingChunk     = chunk;
                        pendingSpanIndex = r.getNextSpanIndex();

                        pageBreak = true;
                        overflowByHeight = r.isOverflowByHeight();

                        break;
                    }
                }
            }

            if( !page.isEmpty() )
            {
                if (requiredHeightPt <= 0f)
                {
                    requiredHeightPt = cursorY - startY;
                    if (requiredHeightPt < 0f) {
                        requiredHeightPt = 0f;
                    }
                }

                return new LaidOutPage( page, requiredHeightPt, overflowByHeight, requiredWidthPt, overflowByWidth );
            }

            if( eof )
                return null;

            if( !pageBreak )
                return null;

            // пустую страницу пропускаем и идём дальше
        }
    }

    /**
     * @return результат layout chunk:
     *         - nextSpanIndex == -1, если chunk обработан целиком
     *         - иначе индекс span, с которого надо продолжить на следующей странице.
     *
     * ВАЖНО:
     * В новой логике StyledTextChunk считается одной логической строкой.
     * Span внутри chunk — это горизонтальный styled-фрагмент, а не отдельная строка.
     */
    private ChunkLayoutResult layoutChunk(
            IStyledTextParser.StyledTextChunk chunk,
            int startSpanIndex,
            List<PageLine> page
    )
    {
        if (chunk == null) {
            return new ChunkLayoutResult(
                    -1,
                    cursorY - startY,
                    false,
                    0f,
                    false
            );
        }

        final String text = chunk.text();
        final List<IStyledTextParser.Span> spans = chunk.spans();

        float localRequiredHeightPt = cursorY - startY;

        if (localRequiredHeightPt < 0f) {
            localRequiredHeightPt = 0f;
        }

        if (text == null || text.length() == 0 || spans == null || spans.isEmpty()) {
            return new ChunkLayoutResult(
                    -1,
                    localRequiredHeightPt,
                    false,
                    0f,
                    false
            );
        }

        if (startSpanIndex < 0) {
            startSpanIndex = 0;
        }

        if (startSpanIndex >= spans.size()) {
            return new ChunkLayoutResult(
                    -1,
                    localRequiredHeightPt,
                    false,
                    0f,
                    false
            );
        }

        /*
         * StyledTextChunk — одна логическая строка.
         * Все spans должны лечь на один baseline.
         */
        final List<LinePart> lineParts = new ArrayList<LinePart>();

        float maxAscent = 0f;
        float maxDescent = 0f;
        float maxLeading = 0f;

        float requiredWidthPt = 0f;
        boolean overflowByWidth = false;

        /*
         * LEFT по текущей ALT-семантике — indent строки.
         * Берём indent первого span-а строки.
         *
         * Если когда-нибудь выяснится, что LEFT внутри строки означает
         * абсолютную табуляцию/позиционирование, это надо будет вынести
         * в отдельный draw/layout primitive. Сейчас это не трогаем.
         */
        StyleState firstStyle = spans.get(startSpanIndex).style();
        float xCursor = startX + firstStyle.leftIndent();

        for (int i = startSpanIndex; i < spans.size(); i++) {
            IStyledTextParser.Span span = spans.get(i);

            if (span == null) {
                continue;
            }

            int spanStart = span.start();
            int spanEnd   = span.end();

            if (spanStart < 0) {
                spanStart = 0;
            }

            if (spanEnd > text.length()) {
                spanEnd = text.length();
            }

            if (spanStart >= spanEnd) {
                continue;
            }

            StyleState style = span.style();

            if (style == null || style.font() == null) {
                continue;
            }

            String spanText = text.substring(spanStart, spanEnd);

            if (spanText.length() == 0) {
                continue;
            }

            TextLayout layout = new TextLayout(spanText, style.font(), frc);

            float ascent  = layout.getAscent();
            float descent = layout.getDescent();
            float leading = layout.getLeading();

            if (ascent > maxAscent) {
                maxAscent = ascent;
            }

            if (descent > maxDescent) {
                maxDescent = descent;
            }

            if (leading > maxLeading) {
                maxLeading = leading;
            }

            float rightX = xCursor + layout.getAdvance();
            float neededWidth = rightX - startX;

            if (neededWidth > requiredWidthPt) {
                requiredWidthPt = neededWidth;
            }

            if (neededWidth > pageWidth) {
                overflowByWidth = true;
            }

            lineParts.add(new LinePart(layout, xCursor, style));

            xCursor = rightX;
        }

        if (lineParts.isEmpty()) {
            return new ChunkLayoutResult(
                    -1,
                    localRequiredHeightPt,
                    false,
                    requiredWidthPt,
                    overflowByWidth
            );
        }

        float baselineY = cursorY + maxAscent;
        float bottomY = baselineY + maxDescent + maxLeading;
        float neededHeight = bottomY - startY;

        if (neededHeight > localRequiredHeightPt) {
            localRequiredHeightPt = neededHeight;
        }

        /*
         * Если строка целиком не помещается на текущую страницу,
         * переносим весь chunk.
         *
         * Нельзя переносить только с i-го span-а:
         * spans — это части одной строки, а не строки документа.
         */
        if (bottomY > endY) {

            /*
             * Строка не помещается даже на пустую страницу.
             * Кладём её принудительно один раз, чтобы не получить
             * бесконечный перенос страниц.
             */
            if (page.isEmpty() && cursorY == startY) {
                appendLineParts(page, lineParts, baselineY);
                cursorY = bottomY;

                return new ChunkLayoutResult(
                        -1,
                        neededHeight,
                        true,
                        requiredWidthPt,
                        overflowByWidth
                );
            }

            return new ChunkLayoutResult(
                    startSpanIndex,
                    neededHeight,
                    true,
                    requiredWidthPt,
                    overflowByWidth
            );
        }

        appendLineParts(page, lineParts, baselineY);

        /*
         * Cursor двигается один раз за всю логическую строку.
         * Раньше он двигался после каждого span-а — из-за этого
         * styled-фрагменты типа kassir уезжали вниз.
         */
        cursorY = bottomY;

        return new ChunkLayoutResult(
                -1,
                localRequiredHeightPt,
                false,
                requiredWidthPt,
                overflowByWidth
        );
    }

    private void appendLineParts(
            List<PageLine> page,
            List<LinePart> lineParts,
            float baselineY
    ) {
        for (LinePart part : lineParts) {
            page.add(new PageLine(
                    part.layout,
                    part.x,
                    baselineY,
                    part.style
            ));
        }
    }

    private float defaultLineHeight() {
        if (Float.isNaN(defaultLineHeight)) {
            Font f = new Font("Monospaced", Font.PLAIN, 10);
            LineMetrics lm = f.getLineMetrics("Ag", frc);
            defaultLineHeight = lm.getHeight();
        }

        return defaultLineHeight;
    }
}