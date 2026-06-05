package ru.inversion.fru.print.altprint.doc.styled;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.utils.Pair;

import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;


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

   private static final Logger log =
           getLogger(MethodHandles.lookup().lookupClass());

   private static final int PAGES_CACHE_SIZE = 4;

   private final ALTDoc altDoc;
   private final Deque<Pair<Integer, AltStyledPreparedPage>> pagesCache = new ArrayDeque<Pair<Integer, AltStyledPreparedPage>>();

   private Float   effectiveScale;
   private boolean effectiveScaleResolved;

   /** */
   public AltStyledPagePlanner(ALTDoc altDoc) {
      this.altDoc = Objects.requireNonNull(altDoc, "'altDoc' is null");
   }

   /**
    * Подготовить страницу для печати.
    * Масштаб вычисляется один раз на весь документ и далее используется
    * одинаково для всех страниц.
    */
   public AltStyledPreparedPage preparePage( Graphics2D g2d, PageFormat pf, int pageIndex ) throws IOException {

      AltStyledPreparedPage cached = findCachedPage(pageIndex);
      if( cached != null )
         return cached;

      float scale = resolveEffectiveScale( g2d, pf );

      AltStyledPreparedPage prepared = layoutPage(g2d, pf, pageIndex, scale);
      if( prepared == null )
          return null;

      cachePage(pageIndex, prepared);

      return prepared;
   }

   /**
    * Один раз вычислить effective scale для всего документа.
    */
   /**
    * Один раз вычислить effective scale для всего документа.
    */
   /**
    * Один раз вычислить effective scale для всего документа.
    */
   private float resolveEffectiveScale(Graphics2D g2d, PageFormat pf) throws IOException {

      if (effectiveScaleResolved)
         return effectiveScale.floatValue();

      final AltPrintPageConfig cfg =
              altDoc.getPageConfig();

      if (!cfg.isShrinkEnabled()) {
         effectiveScale = Float.valueOf(1.0f);
         effectiveScaleResolved = true;
         return 1.0f;
      }

      /*
       * 1. Базовый scale из-за перехода от full-page layout к safe printable area.
       *
       * Раньше Microsoft Print to PDF фактически давал почти всю страницу.
       * Теперь PageFormat честно ограничен imageable area.
       * Чтобы отчёт, который "влезал в PDF", продолжил влезать в safe-area,
       * считаем shrink от физического размера страницы к safe content area.
       */
      float legacyContentWidthPt =
              (float) pf.getWidth()
                      - cfg.getMarginLeftPtOrZero()
                      - cfg.getMarginRightPtOrZero();

      float legacyContentHeightPt =
              (float) pf.getHeight()
                      - cfg.getMarginTopPtOrZero()
                      - cfg.getMarginBottomPtOrZero();

      if (legacyContentWidthPt <= 0.0f) {
         legacyContentWidthPt =
                 cfg.getContentWidthPt(pf);
      }

      if (legacyContentHeightPt <= 0.0f) {
         legacyContentHeightPt =
                 cfg.getContentHeightPt(pf);
      }

      float pageContractScale =
              cfg.resolveShrinkScale(
                      pf,
                      legacyContentWidthPt,
                      legacyContentHeightPt
              );

      /*
       * 2. Дополнительный scale по реальным styled-метрикам.
       * Он нужен, если отдельные строки/куски всё равно шире/выше safe area.
       */
      DocumentMetrics metrics =
              inspectDocumentAtScale(
                      g2d,
                      pf,
                      1.0f
              );

      float metricsScale =
              1.0f;

      if (metrics.hasPages) {
         boolean needShrink =
                 metrics.anyOverflowByHeight
                         || metrics.anyOverflowByWidth
                         || metrics.maxRequiredHeightPt > cfg.getSafeContentHeightPt(pf) + 0.01f
                         || metrics.maxRequiredWidthPt > cfg.getSafeContentWidthPt(pf) + 0.01f;

         if (needShrink) {
            metricsScale =
                    cfg.resolveShrinkScale(
                            pf,
                            metrics.maxRequiredWidthPt,
                            metrics.maxRequiredHeightPt
                    );
         }
      }

      float resolved =
              Math.min(
                      pageContractScale,
                      metricsScale
              );

      if (resolved >= 0.99f)
         resolved = 1.0f;

      effectiveScale =
              Float.valueOf(resolved);

      effectiveScaleResolved =
              true;

      log.info(
              "Styled effective scale: resolved={}, pageContractScale={}, metricsScale={}, " +
                      "pf[w={},h={},ix={},iy={},iw={},ih={}], " +
                      "safeContent={}x{}, legacyContent={}x{}, " +
                      "metricsRequired={}x{}, overflowW={}, overflowH={}, minShrink={}",
              Float.valueOf(resolved),
              Float.valueOf(pageContractScale),
              Float.valueOf(metricsScale),
              Double.valueOf(pf.getWidth()),
              Double.valueOf(pf.getHeight()),
              Double.valueOf(pf.getImageableX()),
              Double.valueOf(pf.getImageableY()),
              Double.valueOf(pf.getImageableWidth()),
              Double.valueOf(pf.getImageableHeight()),
              Float.valueOf(cfg.getSafeContentWidthPt(pf)),
              Float.valueOf(cfg.getSafeContentHeightPt(pf)),
              Float.valueOf(legacyContentWidthPt),
              Float.valueOf(legacyContentHeightPt),
              Float.valueOf(metrics.maxRequiredWidthPt),
              Float.valueOf(metrics.maxRequiredHeightPt),
              Boolean.valueOf(metrics.anyOverflowByWidth),
              Boolean.valueOf(metrics.anyOverflowByHeight),
              Double.valueOf(cfg.getMinShrinkScaleOrDefault())
      );

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

         IStyledTextParser tmpParser = new StyledTextParser(reader, AltSettings.INSTANCE().commandDict());

         PageLayoutEngine tmpEngine = new PageLayoutEngine(tmpParser, g2d, pf, altDoc.getPageConfig(), scale);

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