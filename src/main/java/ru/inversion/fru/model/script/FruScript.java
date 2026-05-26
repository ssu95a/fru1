package ru.inversion.fru.model.script;

import ru.inversion.fru.model.items.FruItem;
import ru.inversion.utils.S;

import java.util.*;

public class FruScript extends FruItem {

    final private String body;

    final private Set<String> importArgs;
    final private Set<String> exportArgs;

    private int executeCount = 0;

    public FruScript( String body, List< String > importArgs, List< String > exportArgs ) {

        this.body = body;

        if( importArgs != null && !importArgs.isEmpty() )
            this.importArgs = new HashSet<>(importArgs);
        else
            this.importArgs = Collections.emptySet();

        if( exportArgs != null && !exportArgs.isEmpty() )
            this.exportArgs = new HashSet<>(exportArgs);
        else
            this.exportArgs = Collections.emptySet();
    }

    /** */
    public String getBody() {
        return body;
    }

    /** */
    public boolean hasExportArg( String name )
    {
        return !S.isNullOrEmpty(name) && exportArgs.contains(name);
    }
    /** */
    public boolean hasImportArg( String name )
    {
        return !S.isNullOrEmpty(name) && importArgs.contains(name);
    }

    /** */
    public Collection<String> getImportArgs()
    {
        return importArgs;
    }

    /** */
    public void beforeExecute()
    { }

    /** */
    public void afterExecute()
    {
        executeCount++;
    }

    /** */
    public int getExecuteCount() {
        return executeCount;
    }
}
