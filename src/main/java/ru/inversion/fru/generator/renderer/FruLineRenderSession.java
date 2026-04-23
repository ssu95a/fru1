package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.utils.S;

import java.util.IdentityHashMap;
import java.util.Map;

public class FruLineRenderSession {

   private final Map<FruField, String> pendingValues = new IdentityHashMap<>();
   private boolean continuationPass;

   public String resolveValue(FruContext context, FruField field) {
      if (continuationPass && field.hasFieldSplit() && hasPendingFor(field)) {
         return pendingValues.remove(field);
      }

      return field.getValue(context);
   }

   public boolean hasPendingFor(FruField field) {
      return pendingValues.containsKey(field);
   }

   public boolean hasPending() {
      return !pendingValues.isEmpty();
   }

   public void storeRemainder(FruField field, String remainder) {
      if (S.isNotNullOrEmpty(remainder)) {
         pendingValues.put(field, remainder);
      }
   }

   public void nextPass() {
      continuationPass = true;
   }

   public boolean isContinuationPass() {
      return continuationPass;
   }
}