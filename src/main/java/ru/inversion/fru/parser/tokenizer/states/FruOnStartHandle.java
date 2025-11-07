package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.utils.parser.ITokenHandler;
import ru.inversion.utils.parser.Token;
import ru.inversion.utils.parser.Tokenizer;
import ru.inversion.utils.parser.state.AbstractTokenHandler;

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
    public Token<String> apply(Tokenizer.IContext ctx ) {

        do {
            if( ctx.current() == '#' && !ITokenHandler.isSpace( ctx.next() ) )
                return null;
        }
        while( ctx.shift() );

        return null;
    }
}
