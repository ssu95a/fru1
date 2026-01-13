package ru.inversion.fru.model.script;

import ru.inversion.fru.model.items.FruItem;

import java.util.List;

public class FruScript extends FruItem {

    final private String body;

    final private List<String> importArgs;
    final private List<String> exportArgs;

    public FruScript( String body, List< String > importArgs, List< String > exportArgs ) {
        this.body = body;
        this.importArgs = importArgs;
        this.exportArgs = exportArgs;
    }

    /** */
    public String getBody() {
        return body;
    }

}
