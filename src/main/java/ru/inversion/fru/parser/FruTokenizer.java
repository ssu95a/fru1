package ru.inversion.fru.parser;

import ru.inversion.fru.parser.tokenizer.states.FruCommentHandler;
import ru.inversion.fru.parser.tokenizer.states.FruLineHandler;
import ru.inversion.fru.parser.tokenizer.states.FruOnStartHandle;
import ru.inversion.fru.parser.tokenizer.states.FruSectionHeaderHandler;
import ru.inversion.fru.parser.tokenizer.tokens.SectionHeaderToken;
import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.utils.parser.ITokenHandler;
import ru.inversion.utils.parser.Token;
import ru.inversion.utils.parser.Tokenizer;
import ru.inversion.utils.parser.state.ExpressionHandler;
import ru.inversion.utils.parser.state.OperatorHandler;
import ru.inversion.utils.parser.state.QuotesHandler;
import ru.inversion.utils.parser.state.SyntaxSymbolHandler;
import ru.inversion.property.BooleanProperty;
import ru.inversion.property.IProperty;
import ru.inversion.utils.U;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** */
public class FruTokenizer extends Tokenizer {

    /** */
    private FruTokenizer( Reader reader ) {
        super( reader );
    }

    /** */
    @Override
    public void close() throws Exception { }

    /** */
    @Override
    protected List<ITokenHandler<?>> prepareStates( ) {
        return new ArrayList<>( Arrays.asList (
            new FruSectionHeaderHandler(),
            new FruCommentHandler(),
            new QuotesHandler(),
            new OperatorHandler(),
            new SyntaxSymbolHandler(),
            new ExpressionHandler()
        ));
    }

    /** */
    private static Iterable<Token<?>> tokenize(Reader r )
    {
        final FruTokenizer ep = new FruTokenizer(r);
        final IContext ctx = ep.context();

        final Iterator<Token<?>> iter = new Iterator<Token<?>>() {

            final private BooleanProperty entryOn = new BooleanProperty(false);

            {
                entryOn.addListener( new IProperty.ChangeListener<Boolean>() {
                    @Override
                    public void changed( IProperty<? extends Boolean> property, Boolean oldValue, Boolean newValue ) {

                        if( !newValue ) {

                            ITokenHandler<?> eh = ep.states.get( ep.states.size() - 1 );

                            ep.states.removeIf (
                                th -> U.in( th.getClass(), QuotesHandler.class, OperatorHandler.class, SyntaxSymbolHandler.class, ExpressionHandler.class )
                            );
                            ep.states.addAll( Arrays.asList( new FruLineHandler(), eh ) );

                            entryOn.removeListener(this);
                        }
                    }
                });

                new FruOnStartHandle().apply(ctx);
            }

            /** */
            @Override
            public boolean hasNext() {
                return !ctx.eof();
            }

            /** */
            @Override
            public Token<?> next() {

                final Token<?> newToken = nextToken();

                if( newToken instanceof SectionHeaderToken)
                    entryOn.setValue( ((SectionHeaderToken)newToken).getSectionType() == SectionTypeEnum.ENTRY );

                if( newToken == null )
                    System.out.println( "NULL" );

                return newToken;
            }

            /** */
            private Token<?> nextToken() {

                try {

                    if( entryOn.get() )
                        while( ITokenHandler.isSpace( ctx.current() ) && ctx.shift() ) { }

                    if( ctx.eof() )
                        return Token.End;

                    for( ITokenHandler<?> state : ep.states() )
                    {
                        if( state.matches(ctx) )
                        {
//                          state.incrementUse();
//
                            return state.apply( ctx );
                        }
                    }

                    return ep.states().get( ep.states().size() - 1 ).apply(ctx);
                }
                catch( Throwable th ) {
                    throw new RuntimeException(th);
                }
            }
        };

        return U.iterable( iter );
    }

    /** */
    public static Iterable<Token<?>> parse(Reader r )
    {
        return U.iterable ( tokenize(r).iterator() );
    }

    /** */
    public static Iterable<Token<?>> parse(String s )
    {
        return parse( new StringReader(s) );
    }
}
