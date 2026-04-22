package ru.inversion.fru.print.altprint;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.ALTDocPrintable;
import ru.inversion.fru.print.naltprn.AltSettings;

import javax.print.PrintService;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.lang.invoke.MethodHandles;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Единая точка исполнения печати ALT-документа.
 *
 * Отвечает за:
 * - выбор backend-а печати (matrix / graphic)
 * - запуск AWT/WYSIWYG-печати
 * - запуск matrix/raw-печати
 * - использование AltPrintPageResolver для AWT-пути
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - UI / JavaFX dialog
 * - layout документа
 * - shrink логики страницы
 * - поиск принтера по индексу/имени
 */
public final class AltPrintExecutionService {

   private static final Logger log = getLogger( MethodHandles.lookup().lookupClass() );

   private final AltPrintPageResolver pageResolver = new AltPrintPageResolver();

   /**
    * Исполнить печать документа на уже выбранный принтер.
    */
   public void print( ALTDoc doc, PrintService printer, IAltPrintListener listener ) throws Exception {

      if( doc == null )
         throw new IllegalArgumentException("doc == null");
      if( printer == null )
         throw new IllegalArgumentException("printer == null");

      boolean matrix = isMatrixPrinter(printer);

      log.info( "Печать на принтер: {}, матричный: {}", printer.getName(), matrix ? "Y" : "N" );

      try (ALTDocPrintable printable = doc.makePrintable(listener))
      {
         if( matrix )
            printMatrix(printable, printer);
         else
            printGraphic(doc, printable, printer);
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
   private void printGraphic(ALTDoc doc, ALTDocPrintable printable, PrintService printer) throws Exception {

      final PrinterJob job = PrinterJob.getPrinterJob();
      job.setPrintService(printer);
      job.setJobName( "ALT: " + doc.getAltFile() );

      AltPrintPageResolver.ResolvedPageSetup setup = pageResolver.resolve( job, printer, doc.getPageConfig(), doc.getCopies().getValue());
      /*
         log.info("Resolved printable area: {}", pageResolver.printableAreaToString(setup.getPrintableArea()));
         log.info("PF orientation={}", setup.getPageFormat().getOrientation());
         log.info("PF imageableX={}, imageableY={}", setup.getPageFormat().getImageableX(), setup.getPageFormat().getImageableY());
         log.info("PF imageableW={}, imageableH={}", setup.getPageFormat().getImageableWidth(), setup.getPageFormat().getImageableHeight());
         log.info("PF pageW={}, pageH={}", setup.getPageFormat().getWidth(), setup.getPageFormat().getHeight());
      */

      job.setPrintable( printable, setup.getPageFormat() );
      job.print( setup.getAttributes() );
   }
}