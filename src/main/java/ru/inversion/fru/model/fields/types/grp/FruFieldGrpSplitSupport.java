package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.utils.Pair;

public final class FruFieldGrpSplitSupport {

   private FruFieldGrpSplitSupport() {
   }

   public static FruFieldGrpSplitResult formatAndSplit(
           FruContext context,
           FruFieldVal field,
           String value
   ) {
      if (value == null) {
         value = "";
      }

      FruFormatter formatter = field.getFormatter();

      if (formatter == null) {
         return new FruFieldGrpSplitResult(value, "");
      }

      /*
       * ВАРИАНТ А:
       * если в проекте уже есть holder remainderHolder:
       *
       * Holder<String> remainderHolder = new Holder<String>();
       * String visible = formatter.format(context, value, remainderHolder);
       * return new FruFieldGrpSplitResult(visible, remainderHolder.get());
       */

      /*
       * ВАРИАНТ B:
       * если remainder сейчас получается внутри Renderers.fieldRenderer,
       * вынеси этот кусок в общий helper и вызови его отсюда.
       */

      return formatAndSplitUsingExistingFormatter(context, field, value);
   }

   private static FruFieldGrpSplitResult formatAndSplitUsingExistingFormatter(
           FruContext context,
           FruFieldVal field,
           String value
   ) {
      /*
       * TODO: заменить на фактическую сигнатуру formatter-а.
       *
       * Это единственное место, которое должно знать детали старого
       * FruFormatter. Не размазывай split logic по FruFieldGrpRuntime.
       */

      Pair<String,String> sval = field.getFormatter().format( context, value,  field );

      return new FruFieldGrpSplitResult(
              sval.first,
              sval.second
      );
   }
}