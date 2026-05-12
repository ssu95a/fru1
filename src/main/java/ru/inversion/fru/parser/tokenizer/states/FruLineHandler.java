package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.utils.parser.Token;
import ru.inversion.utils.parser.Tokenizer;
import ru.inversion.utils.parser.state.AbstractTokenHandler;

import static ru.inversion.utils.parser.Token.TypeEnum;
/**
 *
 * @author ssu @
 */
public class FruLineHandler extends AbstractTokenHandler<String> {

	public FruLineHandler( ) {
	}

    @Override
    public Token<String> apply(Tokenizer.IContext t ) {

        char ch = t.current();

        if( ch == '\n' )
        {
            t.shift();
            return Token.Nls;
        }

        final StringBuilder text = new StringBuilder();

        boolean firstChar = true;

        while( true )
        {
            ch = t.current();

            if( firstChar && ch == '#' && t.next() == '#' )
            {
                text.append('#');
                firstChar = false;

                if( !t.shift() )
                    break;

                if( !t.shift() )
                    break;

                continue;
            }

            text.append(ch);
            firstChar = false;

            if( ch == '\n' )
            {
                t.shift();
                break;
            }

            if( !t.shift() )
                break;
        }
        return new Token<>( TypeEnum.FRU_LINE, text.toString() );
    }
    
    /** */
    @Override
    public String getName() {
        return "FruLineHandler";
    }

    /** */
    @Override
    public boolean matches( Tokenizer.IContext ctx ) {
        return true; //ctx.current() != '#';
    }
}
