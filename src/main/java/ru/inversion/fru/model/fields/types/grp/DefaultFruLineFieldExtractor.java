package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultFruLineFieldExtractor implements FruLineFieldExtractor {

   @Override
   public List<FruFieldVal> extract(FruLine line) {
      if (line == null || line.getItems() == null) {
         return Collections.emptyList();
      }

      List<FruFieldVal> result = new ArrayList<FruFieldVal>();

      for( FruItem field : line.getItems()) {
         if (field instanceof FruFieldVal) {
            result.add((FruFieldVal) field);
         }
      }

      return result;
   }
}