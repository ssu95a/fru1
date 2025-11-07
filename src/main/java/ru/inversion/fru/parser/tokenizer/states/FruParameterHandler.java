package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.utils.parser.Token;
import ru.inversion.utils.parser.Tokenizer;
import ru.inversion.utils.parser.state.AbstractTokenHandler;

public class FruParameterHandler extends AbstractTokenHandler<String> {
    @Override
    public String getName() {
        return "FruParameterHandler";
    }

    @Override
    public boolean matches( Tokenizer.IContext ctx ) {
        return ctx.current() == '@' && ctx.next() != '@';
    }

    @Override
    public Token<String> apply(Tokenizer.IContext t ) {

        final StringBuilder text = new StringBuilder();

        while( t.shift() && t.current() != '@' ) {
               text.append( t.current() );
        }

        t.shift();

        return new Token<>( Token.TypeEnum.FRU_PARAMETER, text.toString() );
    }
}
