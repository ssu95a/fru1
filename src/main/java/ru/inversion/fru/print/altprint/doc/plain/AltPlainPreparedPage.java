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

   public AltPlainPreparedPage(List<String> lines) {
      this.lines = lines;
   }

   public List<String> getLines() {
      return lines;
   }

   public boolean isEmpty() {
      return lines == null || lines.isEmpty();
   }
}