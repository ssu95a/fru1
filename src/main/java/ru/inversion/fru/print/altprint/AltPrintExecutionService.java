package ru.inversion.fru.print.altprint;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.ALTDocPrintable;
import ru.inversion.fru.print.naltprn.AltSettings;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.lang.invoke.MethodHandles;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Единая точка исполнения печати ALT-документа.
 */
public final class AltPrintExecutionService {

   private static final Logger log = getLogger( MethodHandles.lookup().lookupClass() );

   private final AltPrintPageResolver pageResolver = new AltPrintPageResolver();

   /**
    * Исполнить печать документа на уже выбранный принтер.
    */
   public void print(ALTDoc doc, PrintService printer, PrintRequestAttributeSet printAttributes, IAltPrintListener listener ) throws Exception {

      if( doc == null )
         throw new IllegalArgumentException("doc == null");
      if( printer == null )
         throw new IllegalArgumentException("printer == null");


      PrintRequestAttributeSet attrs =
         printAttributes == null
            ? new HashPrintRequestAttributeSet()
            : new HashPrintRequestAttributeSet(printAttributes);

      boolean matrix = isMatrixPrinter(printer);

      log.info( "Печать на принтер: {}, матричный: {}", printer.getName(), matrix ? "Y" : "N" );

      try (ALTDocPrintable printable = doc.makePrintable(listener))
      {
         if( matrix )
            printMatrix(printable, printer);
         else
            printGraphic( doc, printable, printer, attrs );
      }
   }

   /**
    * Проверка, относится ли принтер к matrix
    */
   public boolean isMatrixPrinter(PrintService printer) {

      if( printer == null )
          return false;

      return AltSettings.INSTANCE().isMatrixPrinter(printer.getName());
   }

   /**
    * Запуск matrix/raw-печати.
    */
   private void printMatrix(ALTDocPrintable printable, PrintService printer) throws Exception {
      printable.printToMatrix(printer);
   }

   /**
    * Запуск AWT/WYSIWYG-печати.
    */
   private void printGraphic(
           ALTDoc doc,
           ALTDocPrintable printable,
           PrintService printer,
           PrintRequestAttributeSet dialogAttributes
   ) throws Exception {

      final PrinterJob job = PrinterJob.getPrinterJob();

      job.setPrintService(printer);
      job.setJobName("ALT: " + doc.getAltFile());

      AltPrintPageResolver.ResolvedPageSetup setup =
              pageResolver.resolve(
                      job,
                      printer,
                      doc.getPageConfig(),
                      doc.getCopies().getValue(),
                      dialogAttributes
              );

      PrintRequestAttributeSet attrs =
              setup.getAttributes();

      PageFormat pageFormat =
              setup.getPageFormat();

      Printable fixedPrintable =
              new FixedPageFormatPrintable(
                      printable,
                      pageFormat
              );

      job.setPrintable(
              fixedPrintable,
              pageFormat
      );

      log.info(
              "Graphic print attrs before job.print: sides={}, copies={}, collate={}, quality={}, ranges={}",
              attrs.get(Sides.class),
              attrs.get(Copies.class),
              attrs.get(SheetCollate.class),
              attrs.get(javax.print.attribute.standard.PrintQuality.class),
              attrs.get(PageRanges.class)
      );

      log.info(
              "Graphic print pageFormat before job.print: w={}, h={}, ix={}, iy={}, iw={}, ih={}, orient={}",
              Double.valueOf(pageFormat.getWidth()),
              Double.valueOf(pageFormat.getHeight()),
              Double.valueOf(pageFormat.getImageableX()),
              Double.valueOf(pageFormat.getImageableY()),
              Double.valueOf(pageFormat.getImageableWidth()),
              Double.valueOf(pageFormat.getImageableHeight()),
              Integer.valueOf(pageFormat.getOrientation())
      );

      logSidesSupport(printer, attrs);

      job.print(attrs);
   }

   private static final class FixedPageFormatPrintable implements Printable {

      private final Printable delegate;
      private final PageFormat fixedPageFormat;

      private FixedPageFormatPrintable(
              Printable delegate,
              PageFormat fixedPageFormat
      ) {
         if (delegate == null) {
            throw new IllegalArgumentException("delegate == null");
         }

         if (fixedPageFormat == null) {
            throw new IllegalArgumentException("fixedPageFormat == null");
         }

         this.delegate = delegate;
         this.fixedPageFormat = fixedPageFormat;
      }

      @Override
      public int print(
              Graphics graphics,
              PageFormat incomingPageFormat,
              int pageIndex
      ) throws PrinterException {

         /*
          * TEMP DEBUG:
          * Оставить на пару прогонов, потом убрать или перевести в log.debug.
         System.out.println(
                 "FIXED PF WRAPPER pageIndex=" + pageIndex
                         + " incomingX=" + incomingPageFormat.getImageableX()
                         + " incomingY=" + incomingPageFormat.getImageableY()
                         + " fixedX=" + fixedPageFormat.getImageableX()
                         + " fixedY=" + fixedPageFormat.getImageableY()
         );
          */

         return delegate.print(
                 graphics,
                 fixedPageFormat,
                 pageIndex
         );
      }
   }

   private static void logSidesSupport(
           PrintService printer,
           PrintRequestAttributeSet attrs
   ) {
      Class<Sides> category = Sides.class;

      Object supported =
              printer.getSupportedAttributeValues(
                      category,
                      DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                      attrs
              );

      boolean oneSidedSupported =
              printer.isAttributeValueSupported(
                      Sides.ONE_SIDED,
                      DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                      attrs
              );

      boolean duplexSupported =
              printer.isAttributeValueSupported(
                      Sides.TWO_SIDED_LONG_EDGE,
                      DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                      attrs
              );

      log.info(
              "Printer sides support: printer={}, supported={}, oneSidedSupported={}, duplexSupported={}, defaultSides={}",
              printer.getName(),
              supported,
              Boolean.valueOf(oneSidedSupported),
              Boolean.valueOf(duplexSupported),
              printer.getDefaultAttributeValue(Sides.class)
      );
   }
}