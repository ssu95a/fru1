package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.parser.nprsr.ITokenHandler;
import ru.inversion.parser.nprsr.NewToken;
import ru.inversion.parser.nprsr.Tokenizer;
import ru.inversion.parser.nprsr.state.AbstractTokenHandler;

/** */
public class FruOnStartHandle extends AbstractTokenHandler<String> {

    @Override
    public String getName() {
        return "FruOnStart";
    }

    /** */
    @Override
    public boolean matches(Tokenizer.IContext ctx) {
        return true;
    }

    /** */
    @Override
    public NewToken<String> apply( Tokenizer.IContext ctx ) {

        do {
            if( ctx.current() == '#' && !ITokenHandler.isSpace( ctx.next() ) )
                return null;
        }
        while( ctx.shift() );

        return null;
    }
}
