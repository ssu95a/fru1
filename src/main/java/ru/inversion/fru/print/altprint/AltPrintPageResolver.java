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

import java.awt.print.Paper;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Разрешение print request attributes + PageFormat для AWT/WYSIWYG печати.
 *
 * Важно:
 * - Не просить драйвер печатать "на весь лист" автоматически.
 * - PageFormat после validatePage() является layout contract для Printable.
 * - Если драйвер/PDF printer вернул imageableX/Y = 0, применяем safe fallback.
 */
public final class AltPrintPageResolver {

   private static final double PT_PER_MM = 72.0 / 25.4;

   /*
    * Safe fallback для драйверов, которые возвращают full-page imageable area:
    *   imageableX=0
    *   imageableY=0
    *
    * 5 мм — компромиссный legacy-safe отступ.
    * Если нужно — позже вынести в AltPrintPageConfig / settings.
    */
   private static final double SAFE_FALLBACK_MARGIN_MM = 5.0;
   private static final double SAFE_FALLBACK_MARGIN_PT =
           SAFE_FALLBACK_MARGIN_MM * PT_PER_MM;

   private static final double EPS_PT = 0.10;
   private static final float  EPS_MM = 0.10f;

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
      if (job == null) {
         throw new IllegalArgumentException("job == null");
      }
      if (service == null) {
         throw new IllegalArgumentException("service == null");
      }
      if (cfg == null) {
         throw new IllegalArgumentException("cfg == null");
      }

      PrintRequestAttributeSet attrs = buildAttributes(cfg, copies);

      /*
       * Не добавляем full-page printable area.
       * Если драйвер даёт нормальную область — используем.
       * Если нет — ниже подстрахуем PageFormat.
       */
      MediaPrintableArea printableArea = resolvePrintableArea(service, cfg, attrs);

      if (printableArea != null) {
         attrs.add(printableArea);
      }

      PageFormat pf = job.getPageFormat(attrs);
      pf = job.validatePage(pf);

      /*
       * Критичный fallback:
       * некоторые драйверы возвращают imageableX/Y = 0 и full-page area.
       * Для legacy форм это означает печать от самого края.
       *
       * Не трогаем драйвер attrs, а корректируем PageFormat/Paper как
       * layout contract для Printable.
       */
      pf = applySafePageFormatFallbackIfNeeded(pf);

      return new ResolvedPageSetup(attrs, pf, printableArea);
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
    * Подобрать printable area без требования печати "на весь лист".
    *
    * Важно:
    * - Не строим MediaPrintableArea(0,0,w,h) автоматически.
    * - Не выбираем largest area, потому что она часто означает full-page.
    * - Если нормальной области нет, пробуем safe area 5 мм по MediaSize.
    * - Если драйвер не принимает safe area, возвращаем null.
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

      /*
       * 1. Если когда-нибудь появится явная printable area в cfg,
       * её надо строить здесь.
       *
       * Сейчас в AltPrintPageConfig таких полей нет, поэтому null.
       * Document margins НЕ являются MediaPrintableArea.
       */
      MediaPrintableArea explicitArea = buildExplicitPrintableArea(cfg);

      if (explicitArea != null && isSupported(service, explicitArea, contextAttrs)) {
         return explicitArea;
      }

      /*
       * 2. Default area драйвера.
       * Берём только если у неё есть ненулевой left/top origin.
       */
      Object def = service.getDefaultAttributeValue(MediaPrintableArea.class);

      if (def instanceof MediaPrintableArea) {
         MediaPrintableArea area = (MediaPrintableArea) def;

         if (hasNonZeroLeftTop(area) && isSupported(service, area, contextAttrs)) {
            return area;
         }
      }

      /*
       * 3. Supported values.
       * Не выбираем "самую большую" область.
       * Выбираем самую большую из тех, где left/top ненулевые.
       */
      Object raw = service.getSupportedAttributeValues(
              MediaPrintableArea.class,
              DocFlavor.SERVICE_FORMATTED.PRINTABLE,
              contextAttrs
      );

      if (raw instanceof MediaPrintableArea) {
         MediaPrintableArea area = (MediaPrintableArea) raw;

         if (hasNonZeroLeftTop(area) && isSupported(service, area, contextAttrs)) {
            return area;
         }
      }

      if (raw instanceof MediaPrintableArea[]) {
         MediaPrintableArea area = chooseLargestAreaWithNonZeroLeftTop(
                 (MediaPrintableArea[]) raw
         );

         if (area != null && isSupported(service, area, contextAttrs)) {
            return area;
         }
      }

      /*
       * 4. Safe request к драйверу.
       * Если cfg.getMedia() задан, можно построить safe printable area.
       *
       * Если драйвер её не примет — вернём null, но потом сработает
       * PageFormat fallback после validatePage().
       */
      MediaPrintableArea safeArea = buildSafeMediaAreaFromConfig(cfg);

      if (safeArea != null && isSupported(service, safeArea, contextAttrs)) {
         return safeArea;
      }

      return null;
   }

   /**
    * Сейчас явной printable area в AltPrintPageConfig нет.
    *
    * Document margins не использовать здесь:
    * они являются layout margins внутри imageable area,
    * а не request к драйверу.
    */
   private MediaPrintableArea buildExplicitPrintableArea(AltPrintPageConfig cfg) {
      return null;
   }

   /**
    * Safe MediaPrintableArea по cfg.getMedia().
    *
    * Не использует getPaperWidthMm/getPaperHeightMm.
    */
   private MediaPrintableArea buildSafeMediaAreaFromConfig(AltPrintPageConfig cfg) {
      if (cfg == null) {
         return null;
      }

      MediaSizeName media = cfg.getMedia();

      if (media == null) {
         return null;
      }

      MediaSize ms = MediaSize.getMediaSizeForName(media);

      if (ms == null) {
         return null;
      }

      float paperW = ms.getX(MediaSize.MM);
      float paperH = ms.getY(MediaSize.MM);

      return buildSafeMediaArea(paperW, paperH, (float) SAFE_FALLBACK_MARGIN_MM);
   }

   private MediaPrintableArea buildSafeMediaArea(
           float paperWmm,
           float paperHmm,
           float marginMm
   ) {
      if (paperWmm <= 0f || paperHmm <= 0f) {
         return null;
      }

      float x = marginMm;
      float y = marginMm;
      float w = paperWmm - marginMm - marginMm;
      float h = paperHmm - marginMm - marginMm;

      if (w <= 0f || h <= 0f) {
         return null;
      }

      return new MediaPrintableArea(
              x,
              y,
              w,
              h,
              MediaPrintableArea.MM
      );
   }

   private boolean isSupported(
           PrintService service,
           MediaPrintableArea area,
           AttributeSet attrs
   ) {
      if (service == null || area == null) {
         return false;
      }

      if (!service.isAttributeCategorySupported(MediaPrintableArea.class)) {
         return false;
      }

      return service.isAttributeValueSupported(
              area,
              DocFlavor.SERVICE_FORMATTED.PRINTABLE,
              attrs
      );
   }

   /**
    * Для текущей проблемы важны именно left/top.
    * Если x=0/y=0, форма физически уезжает к краю.
    */
   private boolean hasNonZeroLeftTop(MediaPrintableArea area) {
      if (area == null) {
         return false;
      }

      float x = area.getX(MediaPrintableArea.MM);
      float y = area.getY(MediaPrintableArea.MM);

      return x > EPS_MM && y > EPS_MM;
   }

   /**
    * Выбираем максимально большую область, но только из тех,
    * у которых left/top не равны нулю.
    */
   private MediaPrintableArea chooseLargestAreaWithNonZeroLeftTop(
           MediaPrintableArea[] areas
   ) {
      if (areas == null || areas.length == 0) {
         return null;
      }

      MediaPrintableArea best = null;
      float bestSquare = -1f;

      for (int i = 0; i < areas.length; i++) {
         MediaPrintableArea area = areas[i];

         if (area == null) {
            continue;
         }

         if (!hasNonZeroLeftTop(area)) {
            continue;
         }

         float current = areaSquareMm(area);

         if (current > bestSquare) {
            best = area;
            bestSquare = current;
         }
      }

      return best;
   }

   /**
    * Критичный fallback по PageFormat.
    *
    * Срабатывает, если validatePage() всё равно вернул:
    *   imageableX == 0
    *   imageableY == 0
    *
    * Не зависит от cfg.getMedia().
    * Использует реальные Paper width/height из PageFormat.
    */
   private PageFormat applySafePageFormatFallbackIfNeeded(PageFormat pf) {
      if (pf == null) {
         return null;
      }

      if (!needsSafePageFormatFallback(pf)) {
         return pf;
      }

      Paper oldPaper = pf.getPaper();

      if (oldPaper == null) {
         return pf;
      }

      double paperW = oldPaper.getWidth();
      double paperH = oldPaper.getHeight();

      if (paperW <= 0.0 || paperH <= 0.0) {
         return pf;
      }

      double left = SAFE_FALLBACK_MARGIN_PT;
      double top = SAFE_FALLBACK_MARGIN_PT;
      double right = SAFE_FALLBACK_MARGIN_PT;
      double bottom = SAFE_FALLBACK_MARGIN_PT;

      double imageableW = paperW - left - right;
      double imageableH = paperH - top - bottom;

      if (imageableW <= 0.0 || imageableH <= 0.0) {
         return pf;
      }

      Paper newPaper = new Paper();
      newPaper.setSize(paperW, paperH);
      newPaper.setImageableArea(
              left,
              top,
              imageableW,
              imageableH
      );

      PageFormat copy = (PageFormat) pf.clone();
      copy.setPaper(newPaper);

      return copy;
   }

   /**
    * Считаем full-page подозрительным, если left/top == 0.
    *
    * Даже если imageableW/H чуть отличаются от pageW/H из-за float-округления,
    * отсутствие left/top offset уже достаточно опасно для legacy forms.
    */
   private boolean needsSafePageFormatFallback(PageFormat pf) {
      if (pf == null) {
         return false;
      }

      return pf.getImageableX() <= EPS_PT
              || pf.getImageableY() <= EPS_PT;
   }

   /**
    * Расчёт площади printable area.
    */
   private float areaSquareMm(MediaPrintableArea area) {
      if (area == null) {
         return -1f;
      }

      return area.getWidth(MediaPrintableArea.MM)
              * area.getHeight(MediaPrintableArea.MM);
   }

   /**
    * Строковое представление printable area для логов.
    */
   public String printableAreaToString(MediaPrintableArea area)
   {
      if (area == null) {
         return "<null>";
      }

      return String.format(
              "x=%.3fmm, y=%.3fmm, w=%.3fmm, h=%.3fmm",
              area.getX(MediaPrintableArea.MM),
              area.getY(MediaPrintableArea.MM),
              area.getWidth(MediaPrintableArea.MM),
              area.getHeight(MediaPrintableArea.MM)
      );
   }
}