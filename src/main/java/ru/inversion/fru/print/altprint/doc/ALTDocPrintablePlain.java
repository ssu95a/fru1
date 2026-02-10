package ru.inversion.fru.print.altprint.doc;

import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.IAltPrintListener;
import ru.inversion.fru.print.altprint.doc.formatted.StyleState;
import ru.inversion.utils.Pair;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class ALTDocPrintablePlain extends ALTDocPrintable  {

    final private StyleState baseStyle;
    final private Font font;

    /** */
    public ALTDocPrintablePlain(ALTDoc altDoc, IAltPrintListener listener, AltPrintPageConfig pageConfig, StyleState baseStyle ) {
        super( altDoc, listener, pageConfig );
        this.baseStyle = baseStyle;
        this.font      = baseStyle.font();
    }

    @Override
    public void printToMatrix( PrintService printer ) throws PrintException {

        try {

            final DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;

            final PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet( );
            aset.add( MediaSizeName.ISO_A4 );
            aset.add( this.altDoc.getOrientation() );
            aset.add( this.altDoc.getCopies() );

            try( InputStream is = Files.newInputStream( altDoc.getAltFile() ) ) {

                if( altDoc.getContentState() > 0 )
                    is.skip( altDoc.getContentState() );

                final Doc doc        = new SimpleDoc( is, flavor, null );
                final DocPrintJob pj = printer.createPrintJob( );

                pj.print( doc, aset );
            }

        } catch( Exception e ) {
            throw new PrintException( "Ошибка при печати в режиме матричного принтера", e );
        }
    }

    private BufferedReader reader = null;
    private boolean eof = false;

    private int lastLoadedPage = -1;

    final private Deque<Pair<Integer,List<String>>> pagesCache = new ArrayDeque<>();
    final static private int PAGES_CACHE_SIZE = 3;

    private int linesPerPage = -1;
    private int lineHeight   = -1;
    private int ascent       = 0;
    /** */
    private void initReader( ) throws IOException
    {
        if( reader == null ) {
            reader = Files.newBufferedReader(altDoc.getAltFile(), altDoc.getCharset());

            if( altDoc.getContentState() > 0 )
                reader.skip( altDoc.getContentState() );

            if( listener != null )
                listener.onBeginPrint();
        }
    }

    /** */
    private void initPageLayout( Graphics g, PageFormat pf) {

        if( linesPerPage > 0 )
            return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics(font);

        lineHeight   = fm.getHeight();
        linesPerPage = (int) (pf.getImageableHeight() / lineHeight);
        ascent       = fm.getAscent();

        if( linesPerPage <= 0 )
            throw new IllegalStateException( "Invalid page layout: linesPerPage <= 0");
    }

    /** */
    private void drawPage( Graphics g, PageFormat pf, List<String> pageData )
    {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont ( font );

//        double translateX = pf.getImageableX();
//        double translateY = pf.getImageableY();
//        g2d.translate(translateX, translateY);

        g2d.setFont ( font );
        g2d.setColor( Color.BLACK );

        int x = (int)( pf.getImageableX() + baseStyle.leftIndent() );
        int y = (int)  pf.getImageableY() + ascent;

//int x = (int) baseStyle.leftIndent();
//int y = ascent;

        for( String line : pageData )
        {
            g2d.drawString( line, x, y );

            y += ( lineHeight + baseStyle.spaceAfter() );
        }

        //g2d.dispose();
    }

    /** */
    private List<String> preparePage( int pageIndex ) throws IOException
    {
        Optional<Pair<Integer, List<String>>> page = pagesCache.stream().filter(p -> p.first == pageIndex).findFirst();

        if( page.isPresent() )
            return page.get().second;

        final List<String> newPage = new ArrayList<>();

        if( lastLoadedPage + 1 == pageIndex )
        {
            for( int i = 0; i < linesPerPage; i++) {

                String line = reader.readLine();

                if( line == null ) {
                    eof = true;
                    break;
                }

                newPage.add(line);
            }

            if( newPage.isEmpty() )
                return null;

            if( pagesCache.size() > PAGES_CACHE_SIZE )
                pagesCache.removeLast();

            pagesCache.addFirst( Pair.makePair(pageIndex, newPage) );

            lastLoadedPage++;
        }
        else
        {
            return readUnavailablePage( pageIndex );
        }

        return newPage;
    }

    /** */
    private int printGraphics( Graphics g, PageFormat pf, int pageIndex ) throws IOException {

        initPageLayout( g, pf );

        final List<String> page = preparePage( pageIndex );
        if( page == null ) {
            return NO_SUCH_PAGE;
        }

        drawPage( g, pf, page );

        return PAGE_EXISTS;
    }

    /** */
    @Override
    public int print( Graphics graphics, PageFormat pageFormat, int pageIndex ) throws PrinterException {

        try {

            if( listener != null && listener.isCancelled() ) {
                finishPrint();
                return NO_SUCH_PAGE;
            }

            if( pageIndex > 0 && eof )
            {
                finishPrint();
                return NO_SUCH_PAGE;
            }

            initReader();

            int ret = printGraphics( graphics, pageFormat, pageIndex );

            if( listener != null && ret == PAGE_EXISTS )
                listener.onPagePrinted( pageIndex );

            return ret;

        } catch( Exception e ) {
            finishPrint();
            throw new PrinterException( e.getLocalizedMessage() );
        }
    }

    /** */
    private List<String> readUnavailablePage( int targetPageIndex) throws IOException {

        for( Pair<Integer, List<String>> p : pagesCache )
            if( p.first == targetPageIndex )
                return p.second;

        final List<String> page = new ArrayList<>(linesPerPage);

        try (BufferedReader tmp = Files.newBufferedReader( altDoc.getAltFile(), altDoc.getCharset()))
        {
            int currentPage = 0;

            // Пропускаем страницы
            while( currentPage < targetPageIndex )
            {
                for( int i = 0; i < linesPerPage; i++ )
                {
                    if( tmp.readLine() == null )
                        return null;
                }

                currentPage++;
            }

            // Читаем страницу
            for( int i = 0; i < linesPerPage; i++ )
            {
                String line = tmp.readLine();

                if( line == null)
                     break;

                page.add(line);
            }
        }

        if( page.isEmpty() )
            return null;

        pagesCache.addFirst( Pair.makePair(targetPageIndex, page) );

        return page;
    }


    /** */
    @Override
    public void close()
    {
        if( reader != null)
        {
            try {
                reader.close();
                reader = null;
            } catch (IOException ignored) { }
        }
    }
}
