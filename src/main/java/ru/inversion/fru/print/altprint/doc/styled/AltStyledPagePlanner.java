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

   private final Deque<Pair<Integer, AltStyledPreparedPage>> pagesCache = new ArrayDeque<Pair<Integer, AltStyledPreparedPage>>();

   /** */
   public AltStyledPagePlanner(ALTDoc altDoc) {
      //Checks.Require.object(altDoc,"altDoc");
      this.altDoc = Objects.requireNonNull(altDoc, "'altDoc' is null");
   }

   /**
    * Подготовить страницу для печати.
    * Сначала строит страницу без shrink, затем при необходимости пересчитывает её с уменьшением масштаба.
    */
   public AltStyledPreparedPage preparePage( Graphics2D g2d, PageFormat pf, int pageIndex) throws IOException {

      AltStyledPreparedPage cached = findCachedPage(pageIndex);
      if( cached != null )
          return cached;

      final AltPrintPageConfig cfg = altDoc.getPageConfig();

      AltStyledPreparedPage normal = layoutPage( g2d, pf, pageIndex, 1.0f );
      //если нет больше страниц, то выходим
      if( normal == null )
          return null;

      // если масштабирование не включено
      if( !cfg.isShrinkEnabled() ) {
         cachePage( pageIndex, normal );
         return normal;
      }

      // надо ли масштабировать
      boolean needShrink =
              normal.isOverflowByHeight()
                      || normal.isOverflowByWidth()
                      || normal.getRequiredHeightPt() > cfg.getSafeContentHeightPt(pf) + 0.01f
                      || normal.getRequiredWidthPt()  > cfg.getSafeContentWidthPt(pf)  + 0.01f;

      if( !needShrink ) {
         cachePage(pageIndex, normal);
         return normal;
      }

      // попытались
      float resolvedScale = cfg.resolveShrinkScale( pf, normal.getRequiredWidthPt(), normal.getRequiredHeightPt() );
      // нет необходимости не делаем
      if( resolvedScale >= 0.99f )
      {
         cachePage(pageIndex, normal);
         return normal;
      }

      AltStyledPreparedPage shrunk = layoutPage( g2d, pf, pageIndex, resolvedScale );
      AltStyledPreparedPage result = (shrunk != null) ? shrunk : normal;

      cachePage(pageIndex, result);

      return result;
   }

   /**
    * Построить указанную страницу с заданным scale.
    * Stateless: каждый вызов перечитывает документ с начала.
    */
   private AltStyledPreparedPage layoutPage( Graphics2D g2d, PageFormat pf, int targetPage, float scale) throws IOException
   {
      try( BufferedReader reader = Files.newBufferedReader( altDoc.getAltFile(), altDoc.getCharset() ) )
      {
         IStyledTextParser tmpParser = new StyledTextParser( reader, AltSettings.INSTANCE().commandDict() );

         PageLayoutEngine tmpEngine = new PageLayoutEngine( tmpParser, g2d, pf, altDoc.getPageConfig(), scale );

         int currentPage = 0;
         PageLayoutEngine.LaidOutPage laidOut;

         while( (laidOut = tmpEngine.nextPreparedPage()) != null )
         {
            if( currentPage == targetPage )
                return new AltStyledPreparedPage( laidOut.getLines(), laidOut.getRequiredHeightPt(), laidOut.isOverflowByHeight(), laidOut.getRequiredWidthPt(), laidOut.isOverflowByWidth(), scale );

            currentPage++;
         }
      }
      return null;
   }

   /**
    * Очистка кэша prepared pages.
    */
   public void clearCache() {
      pagesCache.clear();
   }

   /** */
   private AltStyledPreparedPage findCachedPage( int pageIndex )
   {
      for (Pair<Integer, AltStyledPreparedPage> p : pagesCache)
      {
         if( p.first == pageIndex )
            return p.second;
      }
      return null;
   }

   /** */
   private void cachePage( int pageIndex, AltStyledPreparedPage page ) {

      while( pagesCache.size() >= PAGES_CACHE_SIZE )
      {
         pagesCache.removeLast();
      }

      pagesCache.addFirst( Pair.makePair(pageIndex, page) );
   }
}