package ru.inversion.fru.model.items;


import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.formats.FruFormatter;

import java.util.List;

public class FruFormatCall extends FruItem {

    final private FruFormat      format;
    final private List<FruField> fields;
    final private FruFormatter containing;

    /** */
    public FruFormatCall( FruFormat format, List<FruField> fields, FruFormatter additional ) {
        this.format = format;
        this.fields = fields;
        this.containing = additional;
    }

    /** */
    public FruFormat getFormat() {
        return format;
    }

    /** */
    public List<FruField> getFields() {
        return fields;
    }

    /** */
    public FruFormatter getContaining() {
        return containing;
    }
}
