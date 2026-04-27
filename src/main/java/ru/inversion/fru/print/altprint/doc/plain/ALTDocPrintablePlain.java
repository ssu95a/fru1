package ru.inversion.fru.print.altprint.doc.plain;

import org.apache.commons.io.HexDump;
import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.IAltPrintListener;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.ALTDocPrintable;
import ru.inversion.fru.print.altprint.doc.MatrixRawWriter;
import ru.inversion.fru.print.altprint.doc.styled.IStyledTextParser;
import ru.inversion.fru.print.altprint.doc.styled.MatrixTextParser;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.utils.U;
import ru.inversion.utils.io.RawBAOS;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static ru.inversion.fru.api.FruEngine.csDos866;

/**
 * AWT Printable для plain ALT-документа.
 *
 * Отвечает за:
 * - lifecycle печати
 * - вызов planner-а
 * - вызов renderer-а
 * - matrix/raw-печать plain-документа
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - постраничный layout
 * - кэш страниц
 * - draw строк
 */
public class ALTDocPrintablePlain extends ALTDocPrintable {

    private final StyleState baseStyle;

    private final AltPlainPagePlanner  planner;
    private final AltPlainPageRenderer renderer;

    private boolean beginPrintNotified;

    public ALTDocPrintablePlain (
            ALTDoc             altDoc,
            IAltPrintListener  listener,
            AltPrintPageConfig pageConfig,
            StyleState         baseStyle
    )
    {
        super( altDoc, listener, pageConfig );
        this.baseStyle = baseStyle;
        this.planner   = new AltPlainPagePlanner ( altDoc, baseStyle );
        this.renderer  = new AltPlainPageRenderer( baseStyle );
    }

    /** */
    @Override
    public void printToMatrix( PrintService printer ) throws PrintException
    {
        try {

            final DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;

            final PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            aset.add( MediaSizeName.ISO_A4 );
            aset.add( this.altDoc.getOrientation() );
            aset.add( this.altDoc.getCopies() );

            try( InputStream is = Files.newInputStream( altDoc.getAltFile() ) )
            {

                if( altDoc.getContentMode() == ALTDoc.AltDocContentMode.PLAIN_WITH_HEADER ) {
                    is.skip( altDoc.getContentOffset() );

                }

                final Doc doc = new SimpleDoc(is, flavor, null);
                final DocPrintJob pj = printer.createPrintJob();

                pj.print(doc, aset);
            }

        } catch (Exception e) {
            throw new PrintException("Ошибка при печати в режиме матричного принтера", e);
        }
    }

    @Override
    public int print( Graphics graphics, PageFormat pageFormat, int pageIndex ) throws PrinterException {
        try {

            if( listener != null && listener.isCancelled() ) {
                finishPrint();
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics.create();

            try {

                if(!beginPrintNotified )
                {
                    beginPrintNotified = true;

                    if( listener != null )
                        listener.onBeginPrint();
                }

                AltPlainPreparedPage page = planner.preparePage(g2d, pageFormat, pageIndex);

                if( page == null || page.isEmpty() ) {
                    finishPrint();
                    return NO_SUCH_PAGE;
                }

                renderer.drawPage( g2d, pageFormat, page );

                if( listener != null )
                    listener.onPagePrinted(pageIndex);

                return PAGE_EXISTS;

            } finally {
                g2d.dispose();
            }

        } catch (Exception e) {
            finishPrint();

            PrinterException pe = new PrinterException( e.getLocalizedMessage() );
            pe.initCause(e);
            throw pe;
        }
    }

    /** */
    @Override
    public void close() {
        planner.clearCache();
        beginPrintNotified = false;
    }
}