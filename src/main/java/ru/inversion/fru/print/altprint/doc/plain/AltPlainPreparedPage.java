package ru.inversion.fru.print.altprint.doc.plain;

import org.slf4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

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

      if( logicalLineStep < ascent ) {
         Logger log = getLogger( MethodHandles.lookup().lookupClass() );
         log.warn( "Suspicious plain page metrics: ascent={}, logicalLineStep={}", ascent, logicalLineStep );
      }
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