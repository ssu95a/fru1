package ru.inversion.fru.print.altprint.doc.styled;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Отрисовка prepared styled-page на Graphics2D.
 *
 * Отвечает за:
 * - draw текста
 * - draw underline
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - layout страницы
 * - shrink decision
 * - работу с PrinterJob / PrintService
 */
public final class AltStyledPageRenderer {

   public void drawPage( Graphics2D g2d, AltStyledPreparedPage page )
   {
      if( g2d == null )
         throw new IllegalArgumentException("g2d == null");
      if( page == null )
         throw new IllegalArgumentException("page == null");

      g2d.setColor( Color.BLACK );

      for( PageLine line : page.getLines() )
      {
         line.layout().draw( g2d, line.x(), line.baselineY() );

         if( line.style().underline() )
             drawUnderline( g2d, line );

      }
   }

   private void drawUnderline( Graphics2D g2d, PageLine line ) {

      TextLayout layout = line.layout();

      float x1 = line.x();
      float x2 = x1 + layout.getAdvance();
      float y  = line.baselineY() + 1.0f;

      g2d.draw( new java.awt.geom.Line2D.Float(x1, y, x2, y) );
   }
}