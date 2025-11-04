package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.parser.nprsr.NewToken;
import ru.inversion.parser.nprsr.Tokenizer;
import ru.inversion.parser.nprsr.state.AbstractTokenHandler;

import static ru.inversion.parser.nprsr.NewToken.TypeEnum;
/**
 *
 * @author ssu @
 */
public class FruLineHandler extends AbstractTokenHandler<String> {

	public FruLineHandler( ) {
	}

    @Override
    public NewToken<String> apply( Tokenizer.IContext t ) {

        char ch = t.current();

        if( ch == '\n' )
        {
            t.shift();
            return NewToken.Nls;
        }

        final StringBuilder text = new StringBuilder();

        do {

            ch =  t.current();

            text.append( ch );

            if( ch == '\n' ) {
                t.shift();
                break;
            }

        } while( t.shift() );

        return new NewToken<>( TypeEnum.FRU_LINE, text.toString() );
    }

    /** */
    @Override
    public String getName() {
        return "FruLineHandler";
    }

    /** */
    @Override
    public boolean matches( Tokenizer.IContext ctx ) {
        return ctx.current() != '#';
    }
}
