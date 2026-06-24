package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;

import java.util.IdentityHashMap;

public final class FruFieldGrpRuntimeRegistry {

   private FruFieldGrpPlan plan;

   private final IdentityHashMap<FruFieldGrp, FruFieldGrpRuntime> runtimes = new IdentityHashMap<FruFieldGrp, FruFieldGrpRuntime>();

   public void setPlan(FruFieldGrpPlan plan) {
      this.plan = plan;
      this.runtimes.clear();
   }

   public boolean hasGroup(FruField field) {
      return plan != null && plan.contains(field);
   }

   public String render(FruContext context, FruField field) {
      if (plan == null) {
         throw new IllegalStateException("FruFieldGrpPlan is not set");
      }

      FruFieldGrp group = plan.groupOf(field);

      if (group == null) {
         throw new IllegalStateException("Field is not part of any FruFieldGrp");
      }

      FruFieldGrpRuntime rt = runtimes.get(group);

      if (rt == null) {
         rt = new FruFieldGrpRuntime(group);
         runtimes.put(group, rt);
      }

      return rt.renderSlot(context, field);
   }

   public void clearRecordLocalState() {
      runtimes.clear();
   }
}