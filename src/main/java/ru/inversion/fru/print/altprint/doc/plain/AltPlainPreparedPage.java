package ru.inversion.fru.print.altprint.doc.plain;

import java.util.List;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Immutable-результат подготовки одной plain-страницы.
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - layout
 * - draw
 * - PrinterJob / PrintService
 */
public final class AltPlainPreparedPage {

   private final List<String> lines;
   private final float ascent;
   private final float logicalLineStep;
   public AltPlainPreparedPage(List<String> lines, float ascent, float logicalLineStep) {
      this.lines = lines;
      this.ascent = ascent;
      this.logicalLineStep = logicalLineStep;
   }

   public List<String> getLines() {
      return lines;
   }

   public float getAscent() {
      return ascent;
   }

   public float getLogicalLineStep() {
      return logicalLineStep;
   }

   public boolean isEmpty() {
      return lines == null || lines.isEmpty();
   }
}