package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.model.fields.FruField;

/** */
public final class FruFieldGrpSlot {

   private final FruField field;
   private final int      lineIndex;
   private final int      fieldIndex;

   private final boolean  splitSlot;
   private final boolean  tailSlot;

   public FruFieldGrpSlot(
           FruField field,
           int lineIndex,
           int fieldIndex,
           boolean splitSlot,
           boolean tailSlot
   )
   {
      if( field == null )
         throw new IllegalArgumentException("field == null");

      this.field = field;
      this.lineIndex = lineIndex;
      this.fieldIndex = fieldIndex;
      this.splitSlot = splitSlot;
      this.tailSlot = tailSlot;
   }

   public FruField getField() {
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