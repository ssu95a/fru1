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

   private final float contentScale;
   private final float marginLeftPt;
   private final float marginTopPt;

   public AltPlainPreparedPage(
           List<String> lines,
           float ascent,
           float logicalLineStep,
           float contentScale,
           float marginLeftPt,
           float marginTopPt
   ) {
      this.lines = lines;
      this.ascent = ascent;
      this.logicalLineStep = logicalLineStep;
      this.contentScale = contentScale;
      this.marginLeftPt = marginLeftPt;
      this.marginTopPt = marginTopPt;

      if (logicalLineStep < ascent) {
         Logger log = getLogger(MethodHandles.lookup().lookupClass());
         log.warn(
                 "Suspicious plain page metrics: ascent={}, logicalLineStep={}",
                 Float.valueOf(ascent),
                 Float.valueOf(logicalLineStep)
         );
      }

      if (contentScale <= 0.0f || contentScale > 1.0f) {
         Logger log = getLogger(MethodHandles.lookup().lookupClass());
         log.warn(
                 "Suspicious plain page scale: contentScale={}",
                 Float.valueOf(contentScale)
         );
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

   public float getContentScale() {
      return contentScale;
   }

   public float getMarginLeftPt() {
      return marginLeftPt;
   }

   public float getMarginTopPt() {
      return marginTopPt;
   }

   public boolean isEmpty() {
      return lines == null || lines.isEmpty();
   }
}