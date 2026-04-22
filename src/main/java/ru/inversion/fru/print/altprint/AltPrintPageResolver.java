package ru.inversion.fru.print.altprint;

import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Resolver параметров страницы для AWT-печати.
 *
 * Отвечает за:
 * - сбор PrintRequestAttributeSet
 * - подбор MediaPrintableArea
 * - получение PageFormat через PrinterJob
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - layout документа
 * - shrink контента
 * - рисование страницы
 * - matrix/raw-печать
 */
public final class AltPrintPageResolver {

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Готовый набор параметров страницы для одной операции печати.
    */
   public static final class ResolvedPageSetup {
      private final PrintRequestAttributeSet attributes;
      private final PageFormat pageFormat;
      private final MediaPrintableArea printableArea;

      public ResolvedPageSetup(
              PrintRequestAttributeSet attributes,
              PageFormat pageFormat,
              MediaPrintableArea printableArea
      ) {
         this.attributes = attributes;
         this.pageFormat = pageFormat;
         this.printableArea = printableArea;
      }

      public PrintRequestAttributeSet getAttributes() {
         return attributes;
      }

      public PageFormat getPageFormat() {
         return pageFormat;
      }

      public MediaPrintableArea getPrintableArea() {
         return printableArea;
      }
   }

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Построить полный setup страницы для конкретного принтера.
    */
   public ResolvedPageSetup resolve(
           PrinterJob job,
           PrintService service,
           AltPrintPageConfig cfg,
           int copies
   )
   {
      if (job == null) { throw new IllegalArgumentException("job == null"); }
      if (service == null) { throw new IllegalArgumentException("service == null");}
      if (cfg == null) { throw new IllegalArgumentException("cfg == null"); }

      PrintRequestAttributeSet attrs = buildAttributes(cfg, copies);

      MediaPrintableArea printableArea = resolvePrintableArea(service, cfg, attrs);
      if( printableArea != null )
         attrs.add(printableArea);

      PageFormat pf = job.getPageFormat(attrs);
      pf = job.validatePage(pf);

      return new ResolvedPageSetup( attrs, pf, printableArea );
   }

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Собрать print-attributes задания без layout-логики.
    */
   public PrintRequestAttributeSet buildAttributes(AltPrintPageConfig cfg, int copies) {
      HashPrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();

      if (copies > 0) {
         attrs.add(new Copies(copies));
      }

      if (cfg.getMedia() != null) {
         attrs.add(cfg.getMedia());
      }

      if (cfg.getOrientation() != null) {
         attrs.add(cfg.getOrientation());
      }

      return attrs;
   }

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Подобрать printable area, максимально близкую к печати на весь лист,
    * но только в рамках того, что реально поддерживает PrintService.
    */
   public MediaPrintableArea resolvePrintableArea(
           PrintService service,
           AltPrintPageConfig cfg,
           AttributeSet contextAttrs
   ) {
      if (service == null) {
         return null;
      }
      if (cfg == null) {
         return null;
      }

      MediaPrintableArea fullArea = buildFullMediaArea(cfg);
      if (fullArea != null) {
         boolean supported = service.isAttributeValueSupported(
                 fullArea,
                 DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                 contextAttrs
         );

         if (supported) {
            return fullArea;
         }
      }

      Object raw = service.getSupportedAttributeValues(
              MediaPrintableArea.class,
              DocFlavor.SERVICE_FORMATTED.PRINTABLE,
              contextAttrs
      );

      if (raw instanceof MediaPrintableArea) {
         return (MediaPrintableArea) raw;
      }

      if (raw instanceof MediaPrintableArea[]) {
         return chooseLargestArea((MediaPrintableArea[]) raw);
      }

      Object def = service.getDefaultAttributeValue(MediaPrintableArea.class);
      if (def instanceof MediaPrintableArea) {
         return (MediaPrintableArea) def;
      }

      return null;
   }

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Построить идеальную printable area на весь лист для media из config.
    */
   private MediaPrintableArea buildFullMediaArea(AltPrintPageConfig cfg) {
      MediaSizeName media = cfg.getMedia();
      if (media == null) {
         return null;
      }

      MediaSize ms = MediaSize.getMediaSizeForName(media);
      if (ms == null) {
         throw new IllegalStateException("Unsupported media: " + media);
      }

      float wMm = ms.getX(MediaSize.MM);
      float hMm = ms.getY(MediaSize.MM);

      return new MediaPrintableArea(0f, 0f, wMm, hMm, MediaPrintableArea.MM);
   }

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Выбрать самую большую printable area из набора поддержанных.
    */
   private MediaPrintableArea chooseLargestArea( MediaPrintableArea[] areas) {

      if( areas == null || areas.length == 0 )
         return null;


      MediaPrintableArea best = areas[0];
      float bestSquare = areaSquareMm(best);

      for (int i = 1; i < areas.length; i++) {
         float current = areaSquareMm(areas[i]);
         if (current > bestSquare) {
            best = areas[i];
            bestSquare = current;
         }
      }

      return best;
   }

   /**
    *  Расчёт площади printable area.
    */
   private float areaSquareMm(MediaPrintableArea area) {
      return area.getWidth(MediaPrintableArea.MM) * area.getHeight(MediaPrintableArea.MM);
   }

   /**
    * Строковое представление printable area для логов.
    */
   public String printableAreaToString( MediaPrintableArea area )
   {
      if( area == null)
         return "<null>";

      return String.format(
              "x=%.3fmm, y=%.3fmm, w=%.3fmm, h=%.3fmm",
              area.getX(MediaPrintableArea.MM),
              area.getY(MediaPrintableArea.MM),
              area.getWidth(MediaPrintableArea.MM),
              area.getHeight(MediaPrintableArea.MM)
      );
   }
}