package ru.inversion.fru.print.altprint.doc.formatted;

import ru.inversion.fru.print.altprint.*;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.ALTDocPrintable;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.utils.Pair;

import javax.print.PrintException;
import javax.print.PrintService;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/** */
public final class ALTDocPrintableStyled extends ALTDocPrintable
{
    private IStyledTextParser parser;
    private PageLayoutEngine  layoutEngine;

    private boolean eof;
    private int lastLoadedPage = -1;

    private final Deque<Pair<Integer, List<PageLine>>> pagesCache = new ArrayDeque<>();

    private static final int PAGES_CACHE_SIZE = 3;


    public ALTDocPrintableStyled( ALTDoc altDoc, IAltPrintListener listener, AltPrintPageConfig pageConfig ) {
        super(altDoc, listener, pageConfig);
    }

    @Override
    public void printToMatrix(PrintService printer) throws PrintException {
        throw new UnsupportedOperationException("printToMatrix");
    }

    @Override
    public int print( Graphics graphics, PageFormat pageFormat, int pageIndex ) throws PrinterException
    {
        try {

            if( listener != null && listener.isCancelled() )
            {
                finishPrint();
                return NO_SUCH_PAGE;
            }

            if (eof) {
                finishPrint();
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics;

            initParserIfNeeded(g2d, pageFormat);

            List<PageLine> page = preparePage(g2d, pageFormat, pageIndex);

            if (page == null) {
                eof = true;
                finishPrint();
                return NO_SUCH_PAGE;
            }

            drawPage(g2d, page);

            if (listener != null)
                listener.onPagePrinted(pageIndex);

            return PAGE_EXISTS;

        } catch (Exception e) {
            finishPrint();
            e.printStackTrace();
            throw new PrinterException(e.getMessage());
        }
    }

    /** */
    private void initParserIfNeeded( Graphics2D g2d, PageFormat pf ) throws IOException
    {
        if( parser != null )
            return;

        BufferedReader reader = Files.newBufferedReader( altDoc.getAltFile(), altDoc.getCharset() );

        parser = new StyledTextParser ( reader, AltSettings.INSTANCE().commandDict() );

        layoutEngine = new PageLayoutEngine( parser, g2d, pf );

        if( listener != null )
            listener.onBeginPrint();
    }


    /** */
    private List<PageLine> preparePage( Graphics2D g2d, PageFormat pf, int pageIndex ) throws IOException
    {

        // 1. Проверка кэша
        Optional<Pair<Integer, List<PageLine>>> cached = pagesCache.stream().filter(p -> p.first == pageIndex).findFirst();

        if( cached.isPresent() )
            return cached.get().second;

        // 2. Последовательное чтение
        if( lastLoadedPage + 1 == pageIndex )
        {
            List<PageLine> page = layoutEngine.nextPage();

            if( page == null )
                return null;

            cachePage(pageIndex, page);

            lastLoadedPage++;

            return page;
        }

        // 3. Fallback — перечитать файл
        return readUnavailablePage(g2d, pf, pageIndex);
    }

    private void cachePage(int pageIndex, List<PageLine> page) {

        if (pagesCache.size() > PAGES_CACHE_SIZE)
            pagesCache.removeLast();

        pagesCache.addFirst(Pair.makePair(pageIndex, page));
    }

    /** */
    private List<PageLine> readUnavailablePage( Graphics2D g2d, PageFormat pf, int targetPage ) throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader( altDoc.getAltFile(), altDoc.getCharset() ))
        {
            final IStyledTextParser tmpParser = new StyledTextParser( reader, AltSettings.INSTANCE().commandDict() );

            PageLayoutEngine tmpEngine = new PageLayoutEngine(tmpParser, g2d, pf);

            int currentPage = 0;
            List<PageLine> page;

            while ((page = tmpEngine.nextPage()) != null) {

                if (currentPage == targetPage) {
                    cachePage(targetPage, page);
                    return page;
                }

                currentPage++;
            }
        }

        return null;
    }


    /** */
    private void drawPage( Graphics2D g2d, List<PageLine> page )
    {
        g2d.setColor(Color.BLACK);

        for (PageLine line : page)
        {
            line.layout().draw( g2d, line.x(), line.baselineY() );

            if( line.style().underline() ) {
                drawUnderline(g2d, line);
            }
        }
    }

    private void drawUnderline( Graphics2D g2d, PageLine line )
    {
        TextLayout layout = line.layout();

        float x1 = line.x();
        float x2 = x1 + layout.getAdvance();

        float y = line.baselineY() + 1.0f;

        g2d.draw( new java.awt.geom.Line2D.Float(x1, y, x2, y) );
    }


    /** */
    @Override
    public void close() {
        parser       = null;
        layoutEngine = null;
    }
}
