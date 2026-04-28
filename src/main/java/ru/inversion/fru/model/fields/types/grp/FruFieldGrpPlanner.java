package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

public final class FruFieldGrpPlanner {

   private static final int MAX_GROUP_SCAN_LINES = 12;
   private static final int MAX_GROUP_SLOTS = 12;

   private final FruLineFieldExtractor fieldExtractor;

   public FruFieldGrpPlanner(FruLineFieldExtractor fieldExtractor) {
      if (fieldExtractor == null) {
         throw new IllegalArgumentException("fieldExtractor == null");
      }

      this.fieldExtractor = fieldExtractor;
   }

   public FruFieldGrpPlan plan(FruSection section) {
      if (section == null || section.getLines() == null || section.getLines().isEmpty()) {
         return new FruFieldGrpPlan(Collections.<FruFieldGrp>emptyList());
      }

      List<FruLine> lines = section.getLines();

      List<List<FruFieldVal>> fieldsByLine =
              new ArrayList<List<FruFieldVal>>(lines.size());

      for (FruLine line : lines) {
         List<FruFieldVal> fields = fieldExtractor.extract(line);

         if (fields == null) {
            fields = Collections.emptyList();
         }

         fieldsByLine.add(fields);
      }

      List<FruFieldGrp> groups = new ArrayList<FruFieldGrp>();
      IdentityHashMap<FruFieldVal, Boolean> assigned =
              new IdentityHashMap<FruFieldVal, Boolean>();

      for (int lineIndex = 0; lineIndex < fieldsByLine.size(); lineIndex++) {
         List<FruFieldVal> fields = fieldsByLine.get(lineIndex);

         for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
            FruFieldVal field = fields.get(fieldIndex);

            if (field == null) {
               continue;
            }

            if (assigned.containsKey(field)) {
               continue;
            }

            if (!isLocalSplitField(field)) {
               continue;
            }

            FruFieldGrp group = tryBuildGroup(
                    section.getNum(),
                    field,
                    lineIndex,
                    fieldIndex,
                    fieldsByLine,
                    assigned
            );

            if (group == null) {
               continue;
            }
debugGroup(group);
            groups.add(group);

            for (FruFieldGrpSlot slot : group.getSlots()) {
               assigned.put(slot.getField(), Boolean.TRUE);
            }
         }
      }

      return new FruFieldGrpPlan(groups);
   }

   private void debugGroup(FruFieldGrp group) {
      System.out.println(
              "FruFieldGrp CREATED section=" + group.getSectionNum()
                      + " valIndex=" + group.getValIndex()
                      + " slots=" + group.getSlots().size()
      );

      for (FruFieldGrpSlot slot : group.getSlots()) {
         FruFieldVal f = slot.getField();

         System.out.println(
                 "  slot fieldId=" + System.identityHashCode(f)
                         + " valIndex=" + f.getValIndex()
                         + " splitMode=" + f.getFormatter().getSplitMode()
                         + " line=" + slot.getLineIndex()
                         + " field=" + slot.getFieldIndex()
                         + " splitSlot=" + slot.isSplitSlot()
                         + " tailSlot=" + slot.isTailSlot()
         );
      }
   }

   private FruFieldGrp tryBuildGroup(
           int sectionNum,
           FruFieldVal firstField,
           int startLineIndex,
           int startFieldIndex,
           List<List<FruFieldVal>> fieldsByLine,
           IdentityHashMap<FruFieldVal, Boolean> assigned
   ) {
      int valIndex = firstField.getValIndex();

      List<FruFieldGrpSlot> slots = new ArrayList<FruFieldGrpSlot>();

      slots.add(new FruFieldGrpSlot(
              firstField,
              startLineIndex,
              startFieldIndex,
              true,
              false
      ));

      boolean closedByTail = false;

      int maxLineIndex = Math.min(
              fieldsByLine.size() - 1,
              startLineIndex + MAX_GROUP_SCAN_LINES
      );

      for (int lineIndex = startLineIndex; lineIndex <= maxLineIndex; lineIndex++) {
         List<FruFieldVal> fields = fieldsByLine.get(lineIndex);

         int fromFieldIndex = lineIndex == startLineIndex
                 ? startFieldIndex + 1
                 : 0;

         boolean lineHasSameValIndex = false;
         boolean lineAddedSlot = false;

         for (int fieldIndex = fromFieldIndex; fieldIndex < fields.size(); fieldIndex++) {
            FruFieldVal candidate = fields.get(fieldIndex);

            if (candidate == null) {
               continue;
            }

            if (candidate == firstField) {
               continue;
            }

            if (assigned.containsKey(candidate)) {
               continue;
            }

            if (candidate.getValIndex() != valIndex) {
               continue;
            }

            lineHasSameValIndex = true;

            if (slots.size() >= MAX_GROUP_SLOTS) {
               closedByTail = true;
               break;
            }

            if (isLocalSplitField(candidate)) {
               slots.add(new FruFieldGrpSlot(
                       candidate,
                       lineIndex,
                       fieldIndex,
                       true,
                       false
               ));

               lineAddedSlot = true;
               continue;
            }

            if (isPlainTailCandidate(candidate)) {
               slots.add(new FruFieldGrpSlot(
                       candidate,
                       lineIndex,
                       fieldIndex,
                       false,
                       true
               ));

               lineAddedSlot = true;
               closedByTail = true;
               break;
            }

            /*
             * Same valIndex, но формат не /z и не plain tail.
             * Например /x. Не включаем в group.
             */
         }

         if (closedByTail) {
            break;
         }

         /*
          * Начиная со следующей строки после старта:
          * если строка уже не содержит этот valIndex — считаем,
          * что локальная flow-группа закончилась.
          *
          * Это защищает от случайного захвата такого же S24 ниже по секции.
          */
         if (lineIndex > startLineIndex && !lineHasSameValIndex) {
            break;
         }

         /*
          * Если в строке был same valIndex, но слот не добавился,
          * лучше остановиться: там какой-то нестандартный формат.
          */
         if (lineIndex > startLineIndex && lineHasSameValIndex && !lineAddedSlot) {
            break;
         }
      }

      /*
       * Одинокий /z не считаем группой.
       */
      if (slots.size() < 2) {
         return null;
      }

      return new FruFieldGrp(sectionNum, valIndex, slots);
   }

   private boolean isLocalSplitField(FruFieldVal field) {
      return field != null
              && field.getFormatter() != null
              && field.getFormatter().getSplitMode() == 1;
   }

   private boolean isPlainTailCandidate(FruFieldVal field) {
      return field != null
              && field.getFormatter() != null
              && field.getFormatter().getSplitMode() == 0;
   }
}