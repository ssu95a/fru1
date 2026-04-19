package ru.inversion.fru.print.altprint.doc.styled;

import java.util.List;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Immutable-результат подготовки одной styled-страницы к печати.
 *
 * Отвечает за:
 * - хранение lines страницы
 * - хранение требуемых width/height
 * - хранение флагов overflow
 * - хранение итогового scale
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - layout страницы
 * - draw страницы
 * - работу с PrinterJob / PrintService
 */
public final class AltStyledPreparedPage {

   private final List<PageLine> lines;

   private final float requiredHeightPt;
   private final boolean overflowByHeight;

   private final float requiredWidthPt;
   private final boolean overflowByWidth;

   private final float scale;

   public AltStyledPreparedPage(
           List<PageLine> lines,
           float requiredHeightPt,
           boolean overflowByHeight,
           float requiredWidthPt,
           boolean overflowByWidth,
           float scale
   ) {
      this.lines = lines;
      this.requiredHeightPt = requiredHeightPt;
      this.overflowByHeight = overflowByHeight;
      this.requiredWidthPt = requiredWidthPt;
      this.overflowByWidth = overflowByWidth;
      this.scale = scale;
   }

   public List<PageLine> getLines() {
      return lines;
   }

   public float getRequiredHeightPt() {
      return requiredHeightPt;
   }

   public boolean isOverflowByHeight() {
      return overflowByHeight;
   }

   public float getRequiredWidthPt() {
      return requiredWidthPt;
   }

   public boolean isOverflowByWidth() {
      return overflowByWidth;
   }

   public float getScale() {
      return scale;
   }
}