package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.parser.nprsr.ITokenHandler;
import ru.inversion.parser.nprsr.NewToken;
import ru.inversion.parser.nprsr.Tokenizer;
import ru.inversion.parser.nprsr.state.AbstractTokenHandler;

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
    public NewToken<String> apply( Tokenizer.IContext t ) {

		final StringBuilder commentText = new StringBuilder();

		while( t.shift( ) && t.current() != '\n' )
		{
			commentText.append( t.current() );
		}

        t.shift( );

		if( commentText.length() == 0 )
			return NewToken.EmptyComment;

		final String comment = commentText.toString().trim();

		if( comment.isEmpty() )
			return NewToken.EmptyComment;

        return new NewToken<>( NewToken.TypeEnum.COMMENT, comment );
	}

	@Override
	public String getName() {
		return "CommentState";
	}

	/** */
	@Override
	public boolean matches( Tokenizer.IContext ctx ) {
		return isCommentBegin( ctx.current(), ctx.next() );
	}
}
