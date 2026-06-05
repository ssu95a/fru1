package ru.inversion.fru.print.altprint.doc.plain;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.lang.invoke.MethodHandles;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Отрисовка prepared plain-page на Graphics2D.
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - layout страницы
 * - кэш страниц
 * - PrinterJob / PrintService
 */
public final class AltPlainPageRenderer {

   private static final Logger log = getLogger( MethodHandles.lookup().lookupClass() );

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
   ) {
      if (g2d == null)
         throw new IllegalArgumentException("g2d == null");

      if (pf == null)
         throw new IllegalArgumentException("pf == null");

      if (page == null)
         throw new IllegalArgumentException("page == null");

      float scale =
              page.getContentScale();

      if (scale <= 0.0f)
         scale = 1.0f;

      Font scaledFont =
              font.deriveFont(
                      (float) (font.getSize2D() * scale)
              );

      g2d.setFont(scaledFont);
      g2d.setColor(Color.BLACK);

      float x =
              (float) pf.getImageableX()
                      + page.getMarginLeftPt()
                      + baseStyle.leftIndent() * scale;

      float y =
              (float) pf.getImageableY()
                      + page.getMarginTopPt()
                      + page.getAscent() * scale;

      float scaledLineStep =
              page.getLogicalLineStep() * scale;

      float lastBaseline =
              y + Math.max(0, page.getLines().size() - 1) * scaledLineStep;

      log.info(
              "drawPage: lines={}, scale={}, fontSize={} -> {}, ascent={} -> {}, step={} -> {}, x={}, y0={}, lastBaseline={}, imageableBottom={}",
              Integer.valueOf(page.getLines().size()),
              Float.valueOf(scale),
              Float.valueOf(font.getSize2D()),
              Float.valueOf(scaledFont.getSize2D()),
              Float.valueOf(page.getAscent()),
              Float.valueOf(page.getAscent() * scale),
              Float.valueOf(page.getLogicalLineStep()),
              Float.valueOf(scaledLineStep),
              Float.valueOf(x),
              Float.valueOf(y),
              Float.valueOf(lastBaseline),
              Double.valueOf(pf.getImageableY() + pf.getImageableHeight())
      );

      for (String line : page.getLines()) {
         g2d.drawString(line, x, y);
         y += scaledLineStep;
      }
   }

   public Font getFont() {
      return font;
   }
}