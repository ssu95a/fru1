package ru.inversion.fru.parser;

import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.parser.model.AbstractSectionNode;
import ru.inversion.fru.parser.model.EntrySectionNode;
import ru.inversion.fru.parser.model.SectionNode;
import ru.inversion.fru.parser.tokenizer.tokens.BilScriptToken;
import ru.inversion.fru.parser.tokenizer.tokens.SectionHeaderToken;
import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.utils.parser.Token;
import ru.inversion.utils.S;


import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.inversion.utils.parser.Token.*;
import static ru.inversion.utils.parser.Token.TypeEnum.*;

/** */
public class FruParser implements Iterator<AbstractSectionNode> {

    /** */
    final protected Iterator<Token<?>> base;

    /** */
    final protected Token<?>[] buffer = new Token[5];

    /** */
    private AbstractSectionNode currentSection;

    /** */
    private int orderNum = 0;

    /** */
    private boolean insideEntry = false;

    /** */
    public FruParser( Iterator<Token<?>> base ) {

        this.base = Objects.requireNonNull( base, "'base' is null" );

        for( int i = 0; i < buffer.length; i++ )
             buffer[i] = base.hasNext() ? base.next() : null;

        while( !eof() ) {

            if( buffer[0] instanceof SectionHeaderToken)
                break;

            shift(1);
        }

        if( eof() )
            throw new IllegalStateException("buffer[0] instanceof HeaderSectionToken - false");
    }

    /** */
    private boolean eof( )
    {
        return buffer[0] == null || buffer[0] == End;
    }

    /** */
    protected boolean shift( int count ) {
        // Сдвигаем буфер
        for( int i = 0; i < buffer.length - count; i++ )
            buffer[i] = buffer[ i + count ];
        //
        for( int i = buffer.length - count; i < buffer.length; i++ ) {
            buffer[i] = base.hasNext() ? base.next() : null;
        }

        return !eof();
    }

    /** */
    private AbstractSectionNode trySectionHeader( )
    {
        if( buffer[0] instanceof SectionHeaderToken)
        {
            SectionHeaderToken hst = (SectionHeaderToken)buffer[0];

            shift(1);

            if( hst.getSectionType() == SectionTypeEnum.ENTRY )
                return ( currentSection = new EntrySectionNode( hst.getValue(), 0, 0) );;

            return ( currentSection = new SectionNode( hst.getSectionType(), orderNum++, hst.getValue(), 0, 0) );
        }
        return null;
    }

    /** */
    private boolean tryFormatOrString()
    {
        if( ( buffer[0] == Format || buffer[0] == Token.Str ) && buffer[1].isExpression() && buffer[2] == Equals && buffer[3].getType() == TEXT_IN_QUOTES )
        {
              if( buffer[0] == Format )
                  currentSection.addFormat( buffer[1].getValue().toString(), buffer[3].getValue().toString() );
              else
                  currentSection.addString( buffer[1].getValue().toString(), buffer[3].getValue().toString() );

              shift(5);

              return true;
        }

        return false;
    }

    /** */
    private boolean tryEntryParam( )
    {
        if( buffer[0].isExpression() )
        {
            String name = (String)buffer[0].getValue();

            if (
                S.in( name, "width", "lines", "paging", "first" )
            )
            {
                shift(1);

                final StringBuilder sb = new StringBuilder();

                while( shift(1) && buffer[0] != Token.Semicolon )
                {
                    sb.append( (buffer[0].getValue()).toString() );
                }

                shift(1);

                currentSection.addParameter( name, sb.toString() );

                return true;
            }
        }
        return false;
    }

    /** */
    private boolean tryLine( ) {
        currentSection.addLine( buffer[0].getValue().toString() );
        return true;
    }

    /** */
    private boolean tryScript( ) {

        if( buffer[0].getType() == BIL )
        {
            currentSection.setBilScript( ((BilScriptToken)buffer[0] ).toBilScript() );

            shift(1);

            return true;
        }

        return false;
    }

    /** */
    protected AbstractSectionNode nextSection( ) {

        if( !hasNext() )
            throw new NoSuchElementException();

        do {

            AbstractSectionNode result = trySectionHeader();

            if( result != null )
                return result;

            if( tryFormatOrString() )
                continue;

            if( insideEntry && tryEntryParam( ) )
                continue;

            if( tryScript() )
                continue;

            if( buffer[0].getType() == COMMENT )
                ;
            else
                if( !insideEntry && tryLine() )
                    ;

            shift(1);

        } while( !eof() );

        return null;
    }

    /** */
    @Override
    public boolean hasNext() {
        return !eof();
    }

    /** */
    private AbstractSectionNode preNext(AbstractSectionNode section )
    {
        if( section != null )
            insideEntry = section.isEntry();
        return section;
    }

    /** */
    public AbstractSectionNode next() {
        return preNext( nextSection() );
    }

    /** */
    public static void parseFru( Reader r, FruBuilder fruBuilder )
    {
        final Iterator<AbstractSectionNode> sectionIter = new FruParser( FruTokenizer.parse(r).iterator() );

        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool( 3);

        AbstractSectionNode last = null;

        try {

             do {

                 AbstractSectionNode next = sectionIter.next();

                 if( last != null )
                {
                    final AbstractSectionNode asyncSection = last;

                    if( asyncSection.isEntry() )
                    {
                        asyncSection.parse(fruBuilder);
                    }
                    else
                    {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> asyncSection.parse(fruBuilder), executor)
                                .exceptionally(th -> {
                                    th.printStackTrace();
                                    return null;
                                });
                        futures.add(future);
                    }
                }

                last = next;

            } while( last != null );

//             if( entryFuture != null )
//                 entryFuture.thenRun( ()->CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
//             else

                CompletableFuture.allOf( futures.toArray( new CompletableFuture[0] ) ).join();
        }
        finally {
            executor.shutdown();
            try {
                executor.awaitTermination( 1L, TimeUnit.SECONDS );
            } catch (InterruptedException ignored) {
            }
        }
    }
}
