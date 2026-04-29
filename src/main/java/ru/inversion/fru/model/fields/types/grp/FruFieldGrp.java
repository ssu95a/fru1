package ru.inversion.fru.model.fields.types.grp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fru.model.fields.types.FruFieldVal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** */
public final class FruFieldGrp {

   private static final Logger log = LoggerFactory.getLogger(FruFieldGrp.class);

   private final int sectionNum;
   private final int valIndex;

   private final List<FruFieldGrpSlot> slots;

   public FruFieldGrp (
        int sectionNum,
        int valIndex,
        List<FruFieldGrpSlot> slots
   )
   {
      if( slots == null || slots.isEmpty() )
          throw new IllegalArgumentException("slots is empty");

      this.sectionNum = sectionNum;
      this.valIndex   = valIndex;
      this.slots      = Collections.unmodifiableList( new ArrayList<>(slots) );

      // log.debug( "FruFieldGrp created: section={}, valIndex={}, slots={}", sectionNum, valIndex, slots.size() );
   }

   public int getSectionNum() {
      return sectionNum;
   }

   public int getValIndex() {
      return valIndex;
   }

   public List<FruFieldGrpSlot> getSlots() {
      return slots;
   }

   public boolean contains( FruFieldVal field) {

      for( FruFieldGrpSlot slot : slots )
      {
         if (slot.getField() == field)
            return true;
      }
      return false;
   }

   public FruFieldGrpSlot slotOf(FruFieldVal field) {
      for (FruFieldGrpSlot slot : slots) {
         if (slot.getField() == field) {
            return slot;
         }
      }

      return null;
   }
}