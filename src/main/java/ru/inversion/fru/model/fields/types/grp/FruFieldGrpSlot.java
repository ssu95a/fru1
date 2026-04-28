package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.model.fields.types.FruFieldVal;

public final class FruFieldGrpSlot {

   private final FruFieldVal field;
   private final int lineIndex;
   private final int fieldIndex;

   private final boolean splitSlot;
   private final boolean tailSlot;

   public FruFieldGrpSlot(
           FruFieldVal field,
           int lineIndex,
           int fieldIndex,
           boolean splitSlot,
           boolean tailSlot
   ) {
      if (field == null) {
         throw new IllegalArgumentException("field == null");
      }

      this.field = field;
      this.lineIndex = lineIndex;
      this.fieldIndex = fieldIndex;
      this.splitSlot = splitSlot;
      this.tailSlot = tailSlot;
   }

   public FruFieldVal getField() {
      return field;
   }

   public int getLineIndex() {
      return lineIndex;
   }

   public int getFieldIndex() {
      return fieldIndex;
   }

   public boolean isSplitSlot() {
      return splitSlot;
   }

   public boolean isTailSlot() {
      return tailSlot;
   }
}