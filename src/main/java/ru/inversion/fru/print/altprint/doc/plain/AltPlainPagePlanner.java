package ru.inversion.fru.print.altprint.doc.plain;

import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;
import ru.inversion.utils.Pair;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

   private static final int PAGES_CACHE_SIZE = 3;

   private final ALTDoc altDoc;
   private final StyleState baseStyle;
   private final Font font;

   private final Deque<Pair<Integer, AltPlainPreparedPage>> pagesCache =
           new ArrayDeque<Pair<Integer, AltPlainPreparedPage>>();

   private int linesPerPage = -1;
   private int lineHeight = -1;
   private int ascent = 0;

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

   /**
    * ЗОНА ОТВЕТСТВЕННОСТИ:
    * Рассчитать метрики plain-layout для текущего шрифта.
    */
   private void initPageLayout(Graphics2D g2d, PageFormat pf) {

      if( linesPerPage > 0 )
         return;

      g2d.setFont(font);

      FontMetrics fm = g2d.getFontMetrics(font);

      lineHeight = fm.getHeight();
      ascent     = fm.getAscent();

      int logicalLineStep = (int)( lineHeight + baseStyle.spaceAfter() );
      if( logicalLineStep <= 0 )
          throw new IllegalStateException("Invalid line step for plain page");


      linesPerPage = (int) (pf.getImageableHeight() / logicalLineStep);

      if (linesPerPage <= 0) {
         throw new IllegalStateException("Invalid page layout: linesPerPage <= 0");
      }
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

      if (pageLines.isEmpty()) {
         return null;
      }

      return new AltPlainPreparedPage(pageLines);
   }

   public int getLineHeight() {
      return lineHeight;
   }

   public int getAscent() {
      return ascent;
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