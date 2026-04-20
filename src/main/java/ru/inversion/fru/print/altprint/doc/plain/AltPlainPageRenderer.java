package ru.inversion.fru.print.altprint.doc.plain;

import ru.inversion.fru.print.altprint.doc.styled.StyleState;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;

/**
 * Отрисовка prepared plain-page на Graphics2D.
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - layout страницы
 * - кэш страниц
 * - PrinterJob / PrintService
 */
public final class AltPlainPageRenderer {

   private final StyleState baseStyle;
   private final Font font;

   public AltPlainPageRenderer(StyleState baseStyle) {

      if( baseStyle == null )
          throw new IllegalArgumentException("baseStyle == null");

      this.baseStyle = baseStyle;
      this.font      = baseStyle.font();
   }

   public void drawPage(
           Graphics2D g2d,
           PageFormat pf,
           AltPlainPreparedPage page
   )
   {
      if( g2d == null )
         throw new IllegalArgumentException("g2d == null");
      if( pf == null )
         throw new IllegalArgumentException("pf == null");
      if( page == null )
         throw new IllegalArgumentException("page == null");

      g2d.setFont(font);
      g2d.setColor(Color.BLACK);

      float x = (float)(pf.getImageableX() + baseStyle.leftIndent());
      float y = (float) pf.getImageableY() + page.getAscent();

      for( String line : page.getLines() )
      {
           g2d.drawString(line, x, y);
           y += page.getLogicalLineStep();
      }
   }

   public Font getFont() {
      return font;
   }
}