package ru.inversion.fru.print.altviewer;

import javafx.print.JobSettings;
import javafx.print.PageRange;
import javafx.print.PrintSides;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

public final class JavaFxPrintAttributes {

   private JavaFxPrintAttributes() {
   }

   public static PrintRequestAttributeSet toAwt(JobSettings settings) {

      PrintRequestAttributeSet attrs =
              new HashPrintRequestAttributeSet();

      if (settings == null)
         return attrs;

      addCopies(attrs, settings);
      addSides(attrs, settings);
      addCollation(attrs, settings);
      addQuality(attrs, settings);
      addOrientation(attrs, settings);
      addResolution(attrs, settings);
      addPageRanges(attrs, settings);

      return attrs;
   }

   private static void addCopies(
           PrintRequestAttributeSet attrs,
           JobSettings settings
   ) {
      int copies = settings.getCopies();

      if (copies > 0)
         attrs.add(new Copies(copies));
   }

   private static void addSides(
           PrintRequestAttributeSet attrs,
           JobSettings settings
   ) {
      PrintSides sides = settings.getPrintSides();

      if (sides == null)
         return;

      switch (sides) {
         case ONE_SIDED:
            attrs.add(Sides.ONE_SIDED);
            break;

         case DUPLEX:
            attrs.add(Sides.TWO_SIDED_LONG_EDGE);
            break;

         case TUMBLE:
            attrs.add(Sides.TWO_SIDED_SHORT_EDGE);
            break;

         default:
            break;
      }
   }

   private static void addCollation(
           PrintRequestAttributeSet attrs,
           JobSettings settings
   ) {
      if (settings.getCollation() == null)
         return;

      switch (settings.getCollation()) {
         case COLLATED:
            attrs.add(SheetCollate.COLLATED);
            break;

         case UNCOLLATED:
            attrs.add(SheetCollate.UNCOLLATED);
            break;

         default:
            break;
      }
   }

   private static void addQuality(
           PrintRequestAttributeSet attrs,
           JobSettings settings
   ) {
      if (settings.getPrintQuality() == null)
         return;

      switch (settings.getPrintQuality()) {
         case DRAFT:
         case LOW:
            attrs.add(javax.print.attribute.standard.PrintQuality.DRAFT);
            break;

         case HIGH:
            attrs.add(javax.print.attribute.standard.PrintQuality.HIGH);
            break;

         case NORMAL:
            attrs.add(javax.print.attribute.standard.PrintQuality.NORMAL);
            break;

         default:
            break;
      }
   }

   private static void addOrientation(
           PrintRequestAttributeSet attrs,
           JobSettings settings
   ) {
      if (settings.getPageLayout() == null)
         return;

      if (settings.getPageLayout().getPageOrientation() == null)
         return;

      switch (settings.getPageLayout().getPageOrientation()) {
         case PORTRAIT:
            attrs.add(OrientationRequested.PORTRAIT);
            break;

         case LANDSCAPE:
            attrs.add(OrientationRequested.LANDSCAPE);
            break;

         case REVERSE_PORTRAIT:
            attrs.add(OrientationRequested.REVERSE_PORTRAIT);
            break;

         case REVERSE_LANDSCAPE:
            attrs.add(OrientationRequested.REVERSE_LANDSCAPE);
            break;

         default:
            break;
      }
   }

   private static void addResolution(
           PrintRequestAttributeSet attrs,
           JobSettings settings
   ) {
      if (settings.getPrintResolution() == null)
         return;

      int crossFeed =
              settings.getPrintResolution().getCrossFeedResolution();

      int feed =
              settings.getPrintResolution().getFeedResolution();

      if (crossFeed > 0 && feed > 0) {
         attrs.add(
                 new PrinterResolution(
                         crossFeed,
                         feed,
                         PrinterResolution.DPI
                 )
         );
      }
   }

   private static void addPageRanges(
           PrintRequestAttributeSet attrs,
           JobSettings settings
   ) {
      PageRange[] ranges = settings.getPageRanges();

      if (ranges == null || ranges.length == 0)
         return;

      int[][] members =
              new int[ranges.length][2];

      for (int i = 0; i < ranges.length; i++) {
         members[i][0] = ranges[i].getStartPage();
         members[i][1] = ranges[i].getEndPage();
      }

      attrs.add(new PageRanges(members));
   }
}