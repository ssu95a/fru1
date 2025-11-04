package ru.inversion.fru.parser.tokenizer.tokens;

import ru.inversion.fru.model.script.FruScript;
import ru.inversion.parser.nprsr.NewToken;


import java.util.List;

/** */
public class BilScriptToken extends NewToken<String> {

    private final List<String> expList;
    private final List<String> impList;

    /** */
    public BilScriptToken( String script, List<String> impList, List<String> expList  ) {
        super( TypeEnum.BIL, script );
        this.expList = expList;
        this.impList = impList;
    }

    /** */
    public List<String> getExpList() {
        return expList;
    }

    /** */
    public List<String> getImpList() {
        return impList;
    }

    /** */
    public FruScript toBilScript() {
        return new FruScript( getValue(), impList, expList );
    }
}
