package ru.inversion.fru.model.items;


import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormat;

import java.util.List;

public class FruFormatCall extends FruItem {

    final private FruFormat format;
    final private List< FruField > fields;

    /** */
    public FruFormatCall( FruFormat format, List<FruField> fields) {
        this.format = format;
        this.fields = fields;
    }

    public FruFormat getFormat() {
        return format;
    }

    public List<FruField> getFields() {
        return fields;
    }
}
