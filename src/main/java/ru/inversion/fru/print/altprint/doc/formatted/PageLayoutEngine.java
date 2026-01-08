package ru.inversion.fru.print.altprint.doc.formatted;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.List;

public final class PageLayoutEngine {

    private final IStyledTextParser parser;

    private final float pageWidth;
    private final float pageHeight;

    private final float startX;
    private final float startY;
    private final float endY;

    private final FontRenderContext frc;

    private float cursorY;
    private boolean eof;


    public PageLayoutEngine( IStyledTextParser parser, Graphics2D g2d, PageFormat pf )
    {
        this.parser = parser;

        this.startX = (float) pf.getImageableX();
        this.startY = (float) pf.getImageableY();

        this.pageWidth  = (float) pf.getImageableWidth();
        this.pageHeight = (float) pf.getImageableHeight();

        this.endY = startY + pageHeight;
        this.cursorY = startY;

        this.frc = g2d.getFontRenderContext();
    }

    /**
     * Формирует следующую страницу.
     * @return список строк страницы или null — если страниц больше нет
     */
    public List<PageLine> nextPage() {

        if( eof )
            return null;

        while(true) // цикл пропуска пустых страниц
        {
            final List<PageLine> page = new ArrayList<>();
            boolean pageBreak = false;

            while( parser.hasNext() )
            {

                IStyledTextParser.ParsedElement el = parser.next();

                if(el == null ) {
                    eof = true;
                    break;
                }


                if (el instanceof IStyledTextParser.TextFlowControl) {

                    IStyledTextParser.TextFlowControl fc =
                            (IStyledTextParser.TextFlowControl) el;

                    if( fc == IStyledTextParser.TextFlowControl.PAGE_FEED ) {
                        pageBreak = true;
                        break;
                    }

                    if( fc == IStyledTextParser.TextFlowControl.LINE_FEED ) {

                        cursorY += defaultLineHeight();

                        if (cursorY >= endY) {
                            pageBreak = true;
                            break;
                        }
                    }

                    continue;
                }


                if (el instanceof IStyledTextParser.TextChunk) {

                    IStyledTextParser.TextChunk chunk = (IStyledTextParser.TextChunk) el;

                    if (!layoutChunk(chunk, page)) {
                        pageBreak = true;
                        break;
                    }
                }
            }

            if (!page.isEmpty()) {
                return page;              // нормальная страница
            }

            if (eof) {
                return null;              // больше ничего нет
            }

            // пустая страница — пропускаем и идём дальше
            cursorY = startY;

            if (!pageBreak)
                return null;
        }
    }


    /** */
    private boolean layoutChunk( IStyledTextParser.TextChunk chunk, List<PageLine> page )
    {
        final String text = chunk.text();

        for (IStyledTextParser.Span span : chunk.spans()) {

            StyleState style = span.style();

            TextLayout layout = new TextLayout(
                    text.substring(span.start(), span.end()),
                    style.font(),
                    frc
            );

            float ascent  = layout.getAscent();
            float descent = layout.getDescent();

            float baselineY = cursorY + ascent;

            if (baselineY + descent > endY)
                return false;

            float x = startX + style.leftIndent();

            page.add(new PageLine( layout, x, baselineY, style ));

            cursorY = baselineY + descent + style.spaceAfter();
        }

        return true;
    }


    private transient float defaultLineHeight = Float.NaN;

    private float defaultLineHeight() {

        if(Float.isNaN(defaultLineHeight))
        {
            Font f = new Font("Monospaced", Font.PLAIN, 10);
            LineMetrics lm = f.getLineMetrics("Ag", frc);
            defaultLineHeight = lm.getHeight();
        }

        return defaultLineHeight;
    }
}
