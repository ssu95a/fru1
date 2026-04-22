package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.utils.S;

import java.util.IdentityHashMap;
import java.util.Map;

public class FruLineRenderSession {

   private final Map<FruField, String> pendingValues = new IdentityHashMap<>();

   private boolean continuationPass = false;

  /**
   * На основном проходе берём обычное значение поля.
   * На continuation-проходе для split-поля берём сохранённый хвост.
   */
   public String resolveValue( FruContext context, FruField field )
   {
      if( continuationPass && field.hasFieldSplit() /*&& hasPendingFor(field)*/ )
          return pendingValues.remove( field );

      return field.getValue( context );
   }

   /** Есть ли хвост именно у этого поля. */
   public boolean hasPendingFor(FruField field) {
      return pendingValues.containsKey(field);
   }

   /** Сохранить хвост поля для следующего continuation-прохода. */
   public void storeRemainder(FruField field, String remainder) {
      if( S.isNotNullOrEmpty(remainder) )
          pendingValues.put(field, remainder);
   }

   /** Есть ли ещё хвосты split-полей в этой строке. */
   public boolean hasPending() {
     return !pendingValues.isEmpty();
 }

   /** Переключение в continuation-проход. */
   public void nextPass() {
     continuationPass = true;
 }

}
