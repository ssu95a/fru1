package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldScr;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

import java.util.IdentityHashMap;

public final class FruFieldGrpRuntime {

   private final FruFieldGrp group;

   private boolean initialized;
   private String pending;

   private final IdentityHashMap<FruField, String> rendered = new IdentityHashMap<>();


   public FruFieldGrpRuntime( FruFieldGrp group ) {
      if( group == null )
          throw new IllegalArgumentException("group == null");
      this.group = group;
   }

   /** */
   public String renderSlot( FruContext context, FruField field) {

      if( field == null )
         throw new IllegalArgumentException("field == null");

      FruFieldGrpSlot slot = group.slotOf(field);

      if( slot == null )
          throw new IllegalStateException("Field is not part of group");

      String cached = rendered.get(field);
      if( cached != null )
          return cached;

      if(!initialized)
      {
         initialized = true;

         String raw = null;

         if( field instanceof FruFieldVal ) {
             raw = context.data().currentRow().getValue( ((FruFieldVal)field).getValIndex() );
         }
         else if( field instanceof FruFieldScr) {
            raw = context.globalScriptContext().getAttribute(field.getName()).toString();
         }
         pending = raw == null ? S.EMPTY_STRING : raw;
      }

      String result;

      if (S.isNullOrEmpty(pending)) {
         /*
          * ВАЖНО:
          * Даже пустой grouped slot должен пройти через formatter,
          * чтобы сохранить ширину поля.
          *
          * Иначе строки вида:
          *   @S24/85/lz@
          * где данных уже нет, превращаются в реально пустые строки.
          */
         result = formatEmptySlot(context, field);
         pending = S.EMPTY_STRING;
      }
      else
      {
         Pair<String, String> pv = formatSlot(context, field, pending);
         result = pv.first == null ? S.EMPTY_STRING : pv.first;
         pending = S.isNotNullOrEmpty(pv.second) ? pv.second : S.EMPTY_STRING;
      }

      if( slot.isTailSlot() )
      {
         /*
          * Tail slot закрывает группу после вывода своего значения.
          * Но сам tail slot всё равно должен быть отформатирован,
          * включая пустой padding.
          */
         pending = S.EMPTY_STRING;
      }

      rendered.put( field, result );

      return result;
   }

   private String formatEmptySlot(FruContext context, FruField field) {
      if (field.getFormatter() == null) {
         return S.EMPTY_STRING;
      }

      Pair<String, String> pv =
              field.getFormatter().format(context, S.EMPTY_STRING, field);

      return pv == null || pv.first == null
              ? S.EMPTY_STRING
              : pv.first;
   }

   private Pair<String, String> formatSlot(
           FruContext context,
           FruField field,
           String value
   ) {
      if (field.getFormatter() == null) {
         return new Pair<String, String>(
                 value == null ? S.EMPTY_STRING : value,
                 S.EMPTY_STRING
         );
      }

      Pair<String, String> pv =
              field.getFormatter().format(context, value, field);

      if (pv == null) {
         return new Pair<String, String>(
                 S.EMPTY_STRING,
                 S.EMPTY_STRING
         );
      }

      return pv;
   }

   private String splitForSlot(
           FruContext context,
           FruFieldVal field,
           String value
   ) {
      FruFormatter formatter = field.getFormatter();

      if (formatter == null) {
         pending = "";
         return value;
      }

      /*
       * ВАЖНО:
       * Здесь надо использовать тот же механизм форматирования/разрезания,
       * который сейчас используется для /z.
       *
       * Ниже — адаптационный слой. Подставь фактическую сигнатуру
       * FruFormatter.format(...) из проекта.
       */
      FruFieldGrpSplitResult r =
              FruFieldGrpSplitSupport.formatAndSplit(context, field, value);

      pending = r.getRemainder();

      if (pending == null) {
         pending = "";
      }

      return r.getVisible();
   }
}