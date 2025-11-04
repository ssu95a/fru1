package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.parser.nprsr.NewToken;
import ru.inversion.parser.nprsr.Tokenizer;
import ru.inversion.parser.nprsr.state.AbstractTokenHandler;

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
    public NewToken<String> apply( Tokenizer.IContext t ) {

        final StringBuilder text = new StringBuilder();

        while( t.shift() && t.current() != '@' ) {
               text.append( t.current() );
        }

        t.shift();

        return new NewToken<>( NewToken.TypeEnum.FRU_PARAMETER, text.toString() );
    }
}
