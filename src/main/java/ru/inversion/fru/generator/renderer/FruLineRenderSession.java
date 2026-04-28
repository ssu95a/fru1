package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.utils.S;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class FruLineRenderSession {

   private final Map<Object, String> pendingValues = new HashMap<Object, String>();

   private boolean continuationPass;

   /** */
   public String resolveValue(FruContext context, FruField field)
   {
      if (continuationPass && hasPendingFor(field)) {
         return pendingValues.remove(keyOf(field));
      }

      return field.getValue(context);
   }


   public boolean hasPendingFor(FruField field) {
      return pendingValues.containsKey(keyOf(field));
   }

   public void storeRemainder(FruField field, String remainder) {
      if (S.isNotNullOrEmpty(remainder)) {
         pendingValues.put(keyOf(field), remainder);
      }
   }

   public boolean hasPending() {
      return !pendingValues.isEmpty();
   }

   public void nextPass() {
      continuationPass = true;
   }

   public boolean isContinuationPass() {
      return continuationPass;
   }

   private Object keyOf(FruField field) {
      if (field instanceof FruFieldVal) {
         FruFieldVal fv = (FruFieldVal) field;
         return "VAL:" + fv.getValIndex();
      }

      return "FIELD:" + field.getType() + ":" + field.getName();
   }

   public String debugPending() {
      StringBuilder sb = new StringBuilder();

      for (Map.Entry<Object, String> e : pendingValues.entrySet()) {
         String value = e.getValue();

         if (sb.length() > 0) {
            sb.append(", ");
         }

         sb.append(e.getKey())
                 .append(": len=")
                 .append(value == null ? 0 : value.length())
                 .append(" value=[")
                 .append(value)
                 .append("]");
      }

      return sb.toString();
   }
}