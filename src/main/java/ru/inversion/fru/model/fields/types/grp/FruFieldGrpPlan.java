package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

public final class FruFieldGrpPlan {

   private final List<FruFieldGrp> groups;
   private final IdentityHashMap<FruField, FruFieldGrp> byField;
   private final IdentityHashMap<FruField, FruFieldGrpSlot> bySlot;

   public FruFieldGrpPlan(List<FruFieldGrp> groups) {
      this.groups = groups == null
              ? Collections.<FruFieldGrp>emptyList()
              : Collections.unmodifiableList(new ArrayList<FruFieldGrp>(groups));

      this.byField = new IdentityHashMap<>();
      this.bySlot  = new IdentityHashMap<>();

      for( FruFieldGrp group : this.groups )
      {
         for( FruFieldGrpSlot slot : group.getSlots() )
         {
            FruField field = slot.getField();

            if( byField.containsKey(field))
                throw new IllegalStateException( "FruField is assigned to multiple groups: " + field );


            byField.put(field, group);
            bySlot.put(field, slot);
         }
      }
   }

   public boolean contains(FruField field) {
      return byField.containsKey(field);
   }

   public FruFieldGrp groupOf(FruField field) {
      return byField.get(field);
   }

   public FruFieldGrpSlot slotOf(FruField field) {
      return bySlot.get(field);
   }

   public List<FruFieldGrp> getGroups() {
      return groups;
   }

   public boolean isEmpty() {
      return groups.isEmpty();
   }
}