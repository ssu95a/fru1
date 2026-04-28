package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.utils.parser.ITokenHandler;
import ru.inversion.utils.parser.Token;
import ru.inversion.utils.parser.Tokenizer;
import ru.inversion.utils.parser.state.AbstractTokenHandler;

/**
 *
 * @author ssu @
 */
public class FruCommentHandler extends AbstractTokenHandler<String>
{
	/** */
	private static boolean isCommentBegin( char ch1, char ch2 ) {
		return ch1 == '#' && ITokenHandler.isSpace(ch2);
	}

	public FruCommentHandler( ) {
	}

    @Override
    public Token<String> apply(Tokenizer.IContext t ) {

		final StringBuilder commentText = new StringBuilder();

		while( t.shift( ) && t.current() != '\n' )
		{
			commentText.append( t.current() );
		}

        t.shift( );

		if( commentText.length() == 0 )
			return Token.EmptyComment;

		final String comment = commentText.toString().trim();

		if( comment.isEmpty() )
			return Token.EmptyComment;

        return new Token<>( Token.TypeEnum.COMMENT, comment );
	}

	@Override
	public String getName() {
		return "CommentState";
	}

	/** */
	@Override
	public boolean matches( Tokenizer.IContext ctx ) {
//		if( ctx.current() == '#' && ctx.next() == '#' )
//			return false;
		return ctx.previous() == '\n' && ctx.current() == '#' && ITokenHandler.isSpace(ctx.next());
	}
}
