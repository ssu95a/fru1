package ru.inversion.fru.print.altprint.doc.plain;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;
import ru.inversion.utils.Pair;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import ru.inversion.fru.print.altprint.AltPrintPageConfig;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Подготовка plain-страниц к печати:
 * - расчёт page layout
 * - чтение строк документа по страницам
 * - page cache
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - draw страницы
 * - lifecycle печати
 * - PrinterJob / PrintService
 */
public final class AltPlainPagePlanner {

   private static final Logger log = getLogger( MethodHandles.lookup().lookupClass() );

   private static final int PAGES_CACHE_SIZE = 3;

   private final ALTDoc altDoc;
   private final StyleState baseStyle;
   private final Font font;

   private final Deque<Pair<Integer, AltPlainPreparedPage>> pagesCache = new ArrayDeque<Pair<Integer, AltPlainPreparedPage>>();

   private int linesPerPage = -1;

   private float ascent = Float.NaN;
   private float logicalLineStep = Float.NaN;

   private float contentScale = 1.0f;


   private double layoutPageWidth = -1.0d;
   private double layoutPageHeight = -1.0d;
   private double layoutImageableX = -1.0d;
   private double layoutImageableY = -1.0d;
   private double layoutImageableWidth = -1.0d;
   private double layoutImageableHeight = -1.0d;
   private int layoutOrientation = Integer.MIN_VALUE;
   private int layoutFontHash = 0;

   public AltPlainPagePlanner(ALTDoc altDoc, StyleState baseStyle) {
      if (altDoc == null) {
         throw new IllegalArgumentException("altDoc == null");
      }
      if (baseStyle == null) {
         throw new IllegalArgumentException("baseStyle == null");
      }

      this.altDoc = altDoc;
      this.baseStyle = baseStyle;
      this.font = baseStyle.font();
   }

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Подготовить plain-страницу по pageIndex.
    * Stateless: каждый раз перечитывает файл с начала.
    */
   public AltPlainPreparedPage preparePage(Graphics2D g2d, PageFormat pf, int pageIndex) throws IOException {
      AltPlainPreparedPage cached = findCachedPage(pageIndex);
      if (cached != null) {
         return cached;
      }

      initPageLayout(g2d, pf);

      AltPlainPreparedPage page = readPage(pageIndex);
      if (page != null) {
         cachePage(pageIndex, page);
      }

      return page;
   }

   private boolean sameLayout(PageFormat pf) {

      if (linesPerPage <= 0)
         return false;

      int fontHash =
              font == null ? 0 : font.hashCode();

      return layoutPageWidth == pf.getWidth()
              && layoutPageHeight == pf.getHeight()
              && layoutImageableX == pf.getImageableX()
              && layoutImageableY == pf.getImageableY()
              && layoutImageableWidth == pf.getImageableWidth()
              && layoutImageableHeight == pf.getImageableHeight()
              && layoutOrientation == pf.getOrientation()
              && layoutFontHash == fontHash;
   }

   /**
    * Рассчитать метрики plain-layout для текущего шрифта и PageFormat.
    */
   private void initPageLayout(Graphics2D g2d, PageFormat pf) {

      if (g2d == null)
         throw new IllegalArgumentException("g2d == null");

      if (pf == null)
         throw new IllegalArgumentException("pf == null");

      if (sameLayout(pf))
         return;

      g2d.setFont(font);

      FontMetrics fm =
              g2d.getFontMetrics(font);

      ascent =
              fm.getAscent();

      float configuredLineStep =
              baseStyle.verticalMovePt();

      float effectiveLineStep =
              configuredLineStep > 0.01f
                      ? configuredLineStep
                      : (float) font.getSize2D() * 1.2f;

      logicalLineStep =
              Math.max(1, Math.round(effectiveLineStep));

      AltPrintPageConfig cfg =
              altDoc.getPageConfig();

      float safeContentHeight =
              cfg.getSafeContentHeightPt(pf);

      if (safeContentHeight <= 0.0f) {
         throw new IllegalStateException(
                 "Invalid page layout: safeContentHeight <= 0"
         );
      }

      /*
       * Legacy target:
       * раньше plain-layout фактически ориентировался на высоту страницы.
       * Поэтому пытаемся сохранить примерно то же количество строк,
       * но втиснуть его в безопасную printable/content area.
       */
      int legacyLinesPerPage =
              (int) Math.floor(
                      pf.getHeight() / logicalLineStep
              );

      if (legacyLinesPerPage <= 0)
         legacyLinesPerPage = 1;

      float requiredLegacyHeight =
              legacyLinesPerPage * logicalLineStep;

      contentScale =
              1.0f;

      if (cfg.isShrinkEnabled()
              && requiredLegacyHeight > safeContentHeight) {

         double requestedScale =
                 safeContentHeight / requiredLegacyHeight;

         double minScale =
                 cfg.getMinShrinkScaleOrDefault();

         double scale =
                 Math.max(
                         requestedScale,
                         minScale
                 );

         scale =
                 Math.min(
                         scale,
                         1.0d
                 );

         contentScale =
                 (float) scale;

         if (requestedScale < minScale) {
            log.warn(
                    "Plain layout requires scale below minimum: requestedScale={}, minScale={}, safeContentHeight={}, requiredLegacyHeight={}, legacyLinesPerPage={}, logicalLineStep={}",
                    Double.valueOf(requestedScale),
                    Double.valueOf(minScale),
                    Float.valueOf(safeContentHeight),
                    Float.valueOf(requiredLegacyHeight),
                    Integer.valueOf(legacyLinesPerPage),
                    Float.valueOf(logicalLineStep)
            );
         }
      }

      float scaledLineStep =
              logicalLineStep * contentScale;

      if (scaledLineStep <= 0.0f) {
         throw new IllegalStateException(
                 "Invalid page layout: scaledLineStep <= 0"
         );
      }

      linesPerPage =
              (int) Math.floor(
                      safeContentHeight / scaledLineStep
              );

      if (linesPerPage <= 0) {
         throw new IllegalStateException(
                 "Invalid page layout: linesPerPage <= 0"
         );
      }

      layoutPageWidth =
              pf.getWidth();

      layoutPageHeight =
              pf.getHeight();

      layoutImageableX =
              pf.getImageableX();

      layoutImageableY =
              pf.getImageableY();

      layoutImageableWidth =
              pf.getImageableWidth();

      layoutImageableHeight =
              pf.getImageableHeight();

      layoutOrientation =
              pf.getOrientation();

      layoutFontHash =
              font == null ? 0 : font.hashCode();

      log.info(
              "initPageLayout: font={}, fmHeight={}, fmAscent={}, configuredStep={}, effectiveStep={}, logicalStep={}, contentScale={}, scaledLineStep={}, " +
                      "pf[w={},h={},ix={},iy={},iw={},ih={},orient={}], safeContentHeight={}, legacyLinesPerPage={}, linesPerPage={}, usedHeight={}, freeHeight={}",
              font,
              Integer.valueOf(fm.getHeight()),
              Integer.valueOf(fm.getAscent()),
              Float.valueOf(configuredLineStep),
              Float.valueOf(effectiveLineStep),
              Float.valueOf(logicalLineStep),
              Float.valueOf(contentScale),
              Float.valueOf(scaledLineStep),
              Double.valueOf(pf.getWidth()),
              Double.valueOf(pf.getHeight()),
              Double.valueOf(pf.getImageableX()),
              Double.valueOf(pf.getImageableY()),
              Double.valueOf(pf.getImageableWidth()),
              Double.valueOf(pf.getImageableHeight()),
              Integer.valueOf(pf.getOrientation()),
              Float.valueOf(safeContentHeight),
              Integer.valueOf(legacyLinesPerPage),
              Integer.valueOf(linesPerPage),
              Float.valueOf(linesPerPage * scaledLineStep),
              Float.valueOf(safeContentHeight - linesPerPage * scaledLineStep)
      );
   }

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Прочитать нужную страницу из документа.
    */
   private AltPlainPreparedPage readPage(int targetPageIndex) throws IOException {

      List<String> pageLines = new ArrayList<String>(linesPerPage);

      try (BufferedReader reader = Files.newBufferedReader(altDoc.getAltFile(), altDoc.getCharset())) {

         if( altDoc.getContentMode() == ALTDoc.AltDocContentMode.PLAIN_WITH_HEADER )
             reader.skip( altDoc.getContentOffset() );

         int currentPage = 0;

         while (currentPage < targetPageIndex) {
            for (int i = 0; i < linesPerPage; i++) {
               if (reader.readLine() == null) {
                  return null;
               }
            }
            currentPage++;
         }

         for (int i = 0; i < linesPerPage; i++) {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            pageLines.add(line);
         }
      }

      if( pageLines.isEmpty() )
          return null;

      AltPrintPageConfig cfg =
              altDoc.getPageConfig();



      return new AltPlainPreparedPage(
              pageLines,
              ascent,
              logicalLineStep,
              contentScale,
              cfg.getMarginLeftPtOrZero(),
              cfg.getMarginTopPtOrZero()
      );
   }

   public void clearCache() {
      pagesCache.clear();
   }

   private AltPlainPreparedPage findCachedPage(int pageIndex) {
      for (Pair<Integer, AltPlainPreparedPage> p : pagesCache) {
         if (p.first == pageIndex) {
            return p.second;
         }
      }
      return null;
   }

   private void cachePage(int pageIndex, AltPlainPreparedPage page) {
      while (pagesCache.size() >= PAGES_CACHE_SIZE) {
         pagesCache.removeLast();
      }

      pagesCache.addFirst(Pair.makePair(pageIndex, page));
   }
}