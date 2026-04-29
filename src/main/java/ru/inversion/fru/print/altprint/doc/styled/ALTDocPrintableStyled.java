package ru.inversion.fru.print.altprint.doc.styled;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.IAltPrintListener;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.ALTDocPrintable;
import ru.inversion.fru.print.altprint.doc.MatrixRawWriter;
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
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * AWT Printable для styled ALT-документа.
 *
 * Отвечает за:
 * - lifecycle печати
 * - вызов planner-а
 * - применение Graphics2D transform
 * - вызов renderer-а
 * - matrix/raw-печать styled-документа
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - постраничный layout
 * - shrink decision logic
 * - кэш prepared pages
 * - draw-детали underline/text
 */
public final class ALTDocPrintableStyled extends ALTDocPrintable
{
     private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AltStyledPagePlanner planner;
    private final AltStyledPageRenderer renderer;

    private boolean beginPrintNotified;

    public ALTDocPrintableStyled(ALTDoc altDoc, IAltPrintListener listener, AltPrintPageConfig pageConfig) {
        super(altDoc, listener, pageConfig);
        this.planner = new AltStyledPagePlanner(altDoc);
        this.renderer= new AltStyledPageRenderer();
    }


    private void logPrintPageFormat(String label, PageFormat pf) {
        System.out.println(
                label
                        + " orientation=" + pf.getOrientation()
                        + " imageableX=" + pf.getImageableX()
                        + " imageableY=" + pf.getImageableY()
                        + " imageableW=" + pf.getImageableWidth()
                        + " imageableH=" + pf.getImageableHeight()
                        + " pageW=" + pf.getWidth()
                        + " pageH=" + pf.getHeight()
        );
    }

    /** */
    @Override
    public int print( Graphics graphics, PageFormat pageFormat, int pageIndex ) throws PrinterException
    {
        try {

            //logPrintPageFormat("PRINTABLE ACTUAL PF", pageFormat);

            if( listener != null && listener.isCancelled() ) {
                finishPrint();
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics.create();

            try {
                //System.out.println("BEFORE translate transform=" + g2d.getTransform());
                g2d.translate( pageFormat.getImageableX(), pageFormat.getImageableY() );
                //System.out.println("AFTER translate transform=" + g2d.getTransform());

                if(!beginPrintNotified )
                {
                    beginPrintNotified = true;

                    if( listener != null )
                        listener.onBeginPrint();
                }

                AltStyledPreparedPage prepared = planner.preparePage(g2d, pageFormat, pageIndex);

                if( prepared == null ) {
                    finishPrint();
                    return NO_SUCH_PAGE;
                }

                if( prepared.getScale() < 0.999f )
                    g2d.scale(prepared.getScale(), prepared.getScale());

                renderer.drawPage( g2d, prepared );

                if( listener != null )
                    listener.onPagePrinted(pageIndex);

                return PAGE_EXISTS;

            } finally {
                g2d.dispose();
            }

        } catch (Exception e) {
            finishPrint();

            PrinterException pe = new PrinterException (
                "Print failed at pageIndex=" + pageIndex + ": " + e.getMessage()
            );
            pe.initCause(e);
            throw pe;
        }
    }

    @Override
    public void close() {
        planner.clearCache();
        beginPrintNotified = false;
    }

    /** */
    @Override
    public void printToMatrix( PrintService printer ) throws PrintException, IOException {

        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;

        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(MediaSizeName.ISO_A4);
        aset.add(this.altDoc.getOrientation());
        aset.add(this.altDoc.getCopies());

        try (
            BufferedReader  reader = Files.newBufferedReader( altDoc.getAltFile(), altDoc.getCharset() );
            MatrixRawWriter writer = new MatrixRawWriter( new RawBAOS(), altDoc.getCharset() )
        )
        {
            final MatrixTextParser mtp = new MatrixTextParser( reader, AltSettings.INSTANCE().commandDict() );

            for( IStyledTextParser.ParsedElement pe : U.iterable(mtp) )
            {
                if( pe == null )
                    continue;

                pe.matrixWrite(writer);
            }

//byte[] bytes = writer.bytea();
//Files.write(Paths.get("d:\\XXI\\fru\\ap\\matrix-java-output.prn"), bytes);
//try( OutputStream os =  Files.newOutputStream(Paths.get("d:\\XXI\\fru\\ap\\matrix-java-output.hex.txt")) ) {
//    HexDump.dump(bytes, 0L, os, 0);
//}
            final Doc doc = new SimpleDoc( writer.is(), flavor, null);
            final DocPrintJob pj = printer.createPrintJob();

            pj.print(doc, aset);
        }
    }
}