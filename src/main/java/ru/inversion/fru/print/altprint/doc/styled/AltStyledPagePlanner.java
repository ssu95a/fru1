package ru.inversion.fru.print.altprint.doc.styled;

import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.utils.Pair;

import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Подготовка styled-страниц к печати:
 * - постраничный layout
 * - shrink decision
 * - page cache
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - draw страницы
 * - lifecycle печати
 * - отправку задания в PrinterJob
 * - matrix/raw-печать
 */
public final class AltStyledPagePlanner {

   private static final int PAGES_CACHE_SIZE = 3;

   private final ALTDoc altDoc;
   private final Deque<Pair<Integer, AltStyledPreparedPage>> pagesCache =
           new ArrayDeque<Pair<Integer, AltStyledPreparedPage>>();

   private Float effectiveScale;
   private boolean effectiveScaleResolved;

   public AltStyledPagePlanner(ALTDoc altDoc) {
      this.altDoc = Objects.requireNonNull(altDoc, "'altDoc' is null");
   }

   /**
    * Подготовить страницу для печати.
    * Масштаб вычисляется один раз на весь документ и далее используется
    * одинаково для всех страниц.
    */
   public AltStyledPreparedPage preparePage(Graphics2D g2d, PageFormat pf, int pageIndex) throws IOException {

      AltStyledPreparedPage cached = findCachedPage(pageIndex);
      if (cached != null)
         return cached;

      float scale = resolveEffectiveScale(g2d, pf);

      AltStyledPreparedPage prepared = layoutPage(g2d, pf, pageIndex, scale);
      if (prepared == null)
         return null;

      cachePage(pageIndex, prepared);
      return prepared;
   }

   /**
    * Один раз вычислить effective scale для всего документа.
    */
   private float resolveEffectiveScale(Graphics2D g2d, PageFormat pf) throws IOException {

      if (effectiveScaleResolved)
         return effectiveScale.floatValue();

      final AltPrintPageConfig cfg = altDoc.getPageConfig();

      if (!cfg.isShrinkEnabled()) {
         effectiveScale = Float.valueOf(1.0f);
         effectiveScaleResolved = true;
         return 1.0f;
      }

      DocumentMetrics metrics = inspectDocumentAtScale(g2d, pf, 1.0f);

      if (!metrics.hasPages) {
         effectiveScale = Float.valueOf(1.0f);
         effectiveScaleResolved = true;
         return 1.0f;
      }

      boolean needShrink =
              metrics.anyOverflowByHeight
                      || metrics.anyOverflowByWidth
                      || metrics.maxRequiredHeightPt > cfg.getSafeContentHeightPt(pf) + 0.01f
                      || metrics.maxRequiredWidthPt > cfg.getSafeContentWidthPt(pf) + 0.01f;

      float resolved = 1.0f;

      if (needShrink) {
         resolved = cfg.resolveShrinkScale(
                 pf,
                 metrics.maxRequiredWidthPt,
                 metrics.maxRequiredHeightPt
         );

         if (resolved >= 0.99f)
            resolved = 1.0f;
      }

      effectiveScale = Float.valueOf(resolved);
      effectiveScaleResolved = true;

      return resolved;
   }

   /**
    * Один полный прогон документа на заданном scale, чтобы собрать
    * максимальные требования по ширине/высоте для всех страниц.
    */
   private DocumentMetrics inspectDocumentAtScale(Graphics2D g2d, PageFormat pf, float scale) throws IOException {

      try (BufferedReader reader = Files.newBufferedReader(altDoc.getAltFile(), altDoc.getCharset())) {

         IStyledTextParser tmpParser =
                 new StyledTextParser(reader, AltSettings.INSTANCE().commandDict());

         PageLayoutEngine tmpEngine =
                 new PageLayoutEngine(tmpParser, g2d, pf, altDoc.getPageConfig(), scale);

         DocumentMetrics metrics = new DocumentMetrics();

         PageLayoutEngine.LaidOutPage laidOut;
         while ((laidOut = tmpEngine.nextPreparedPage()) != null) {
            metrics.hasPages = true;
            metrics.maxRequiredHeightPt =
                    Math.max(metrics.maxRequiredHeightPt, laidOut.getRequiredHeightPt());
            metrics.maxRequiredWidthPt =
                    Math.max(metrics.maxRequiredWidthPt, laidOut.getRequiredWidthPt());
            metrics.anyOverflowByHeight |= laidOut.isOverflowByHeight();
            metrics.anyOverflowByWidth  |= laidOut.isOverflowByWidth();
         }

         return metrics;
      }
   }

   /**
    * Построить указанную страницу с уже выбранным общим scale.
    * Stateless: каждый вызов перечитывает документ с начала,
    * но scale у всех страниц один и тот же.
    */
   private AltStyledPreparedPage layoutPage(Graphics2D g2d, PageFormat pf, int targetPage, float scale) throws IOException
   {
      try (BufferedReader reader = Files.newBufferedReader(altDoc.getAltFile(), altDoc.getCharset())) {

         IStyledTextParser tmpParser =
                 new StyledTextParser(reader, AltSettings.INSTANCE().commandDict());

         PageLayoutEngine tmpEngine =
                 new PageLayoutEngine(tmpParser, g2d, pf, altDoc.getPageConfig(), scale);

         int currentPage = 0;
         PageLayoutEngine.LaidOutPage laidOut;

         while ((laidOut = tmpEngine.nextPreparedPage()) != null) {
            if (currentPage == targetPage) {
               return new AltStyledPreparedPage(
                       laidOut.getLines(),
                       laidOut.getRequiredHeightPt(),
                       laidOut.isOverflowByHeight(),
                       laidOut.getRequiredWidthPt(),
                       laidOut.isOverflowByWidth(),
                       scale
               );
            }

            currentPage++;
         }
      }

      return null;
   }

   public void clearCache() {
      pagesCache.clear();
      effectiveScale = null;
      effectiveScaleResolved = false;
   }

   private AltStyledPreparedPage findCachedPage(int pageIndex)
   {
      for (Pair<Integer, AltStyledPreparedPage> p : pagesCache) {
         if (p.first == pageIndex)
            return p.second;
      }
      return null;
   }

   private void cachePage(int pageIndex, AltStyledPreparedPage page) {

      while (pagesCache.size() >= PAGES_CACHE_SIZE) {
         pagesCache.removeLast();
      }

      pagesCache.addFirst(Pair.makePair(pageIndex, page));
   }

   private static final class DocumentMetrics {
      private boolean hasPages;
      private float maxRequiredHeightPt;
      private float maxRequiredWidthPt;
      private boolean anyOverflowByHeight;
      private boolean anyOverflowByWidth;
   }
}