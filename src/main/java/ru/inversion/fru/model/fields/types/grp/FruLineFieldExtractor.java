package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.items.FruLine;

import java.util.List;

public interface FruLineFieldExtractor {

   List<FruField> extract(FruLine line);
}