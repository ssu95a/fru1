package ru.inversion.fru.print.altprint.doc.styled;

import java.awt.*;
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

   private static final Stroke UNDERLINE_STROKE = new BasicStroke(0.35f);

   public void drawPage(Graphics2D g2d, AltStyledPreparedPage page)
   {
      if (g2d == null)
         throw new IllegalArgumentException("g2d == null");
      if (page == null)
         throw new IllegalArgumentException("page == null");

      Color oldColor   = g2d.getColor();
      Stroke oldStroke = g2d.getStroke();

      try {

         g2d.setColor( Color.BLACK );

         for( PageLine line : page.getLines() )
         {
            line.layout().draw( g2d, line.x(), line.baselineY() );

            if( line.style().underline() ) {
                drawUnderline(g2d, line);
            }
         }
      }
      finally {
         g2d.setColor(oldColor);
         g2d.setStroke(oldStroke);
      }
   }

   private void drawUnderline(Graphics2D g2d, PageLine line)
   {
      TextLayout layout = line.layout();

      float x1 = line.x();
      float x2 = x1 + layout.getAdvance();

      float y = line.baselineY() + Math.max(0.75f, layout.getDescent() * 0.35f);

      Stroke oldStroke = g2d.getStroke();

      try {
         g2d.setStroke(UNDERLINE_STROKE);
         g2d.draw(new java.awt.geom.Line2D.Float(x1, y, x2, y));
      }
      finally {
         g2d.setStroke(oldStroke);
      }
   }
}