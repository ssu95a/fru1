package ru.inversion.fru.parser;

import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.parser.exceptions.FruParseException;
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
import java.util.concurrent.*;

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
    private final boolean entryImplicit;

    /** */
    private boolean implicitEntryReturned = false;

    /** */
    public FruParser( Iterator<Token<?>> base, boolean entryImplicit ) {

        this.base = Objects.requireNonNull( base, "'base' is null" );
        this.entryImplicit = entryImplicit;

        for( int i = 0; i < buffer.length; i++ )
             buffer[i] = base.hasNext() ? base.next() : null;

        if( !entryImplicit )
        {
            skipUntilFirstSectionHeader();

            if( eof() )
                throw new IllegalStateException("buffer[0] instanceof HeaderSectionToken - false");
        }
        else
            if( !eof() && !(buffer[0] instanceof SectionHeaderToken) )
            {
            /*
             * FRU has no explicit #entry. Everything before the first real
             * section is treated as implicit entry-level content.
             */
            currentSection = new EntrySectionNode("#entry();", 0, 0);
            insideEntry = true;
        }
    }

    /** */
    private void skipUntilFirstSectionHeader() {
        while( !eof() ) {
            if( buffer[0] instanceof SectionHeaderToken )
                break;

            shift(1);
        }
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
    private void ensureEntrySection() {

        if( currentSection == null ) {
            currentSection = new EntrySectionNode("#entry();", 0, 0);
            insideEntry = true;
            implicitEntryReturned = true;
        }
    }

    /** */
    private boolean tryFormatOrString()
    {
        if( buffer[0] != Format && buffer[0] != Token.Str )
            return false;

        if( buffer[1] == null || buffer[2] == null || buffer[3] == null )
            return false;

        if( buffer[1].isExpression() && buffer[2] == Equals && buffer[3].getType() == TEXT_IN_QUOTES )
        {
            ensureEntrySection();

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
        if( buffer[0] == null || !buffer[0].isExpression() )
            return false;

        String name = String.valueOf(buffer[0].getValue());

        if( !S.in( name, "width", "lines", "paging", "first" ) )
            return false;

        ensureEntrySection();

        shift(1);

        if( buffer[0] == Equals )
            shift(1);

        final StringBuilder sb = new StringBuilder();

        while( !eof() && buffer[0] != Token.Semicolon )
        {
            if( buffer[0] != null && buffer[0].getValue() != null )
                sb.append( buffer[0].getValue().toString() );

            shift(1);
        }

        if( buffer[0] == Token.Semicolon )
            shift(1);

        currentSection.addParameter( name, sb.toString() );

        return true;
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
            currentSection.addBilScript( ((BilScriptToken)buffer[0] ).toBilScript() );

            shift(1);

            return true;
        }

        return false;
    }

    /** */
    protected AbstractSectionNode nextSection( ) {

        if( !hasNext() )
            throw new NoSuchElementException();

        if( entryImplicit
                && currentSection != null
                && currentSection.isEntry()
                && !implicitEntryReturned
                && !(buffer[0] instanceof SectionHeaderToken) )
        {
            implicitEntryReturned = true;
            insideEntry = true;
            return currentSection;
        }

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
    private static void submitSection( AbstractSectionNode section, FruBuilder fruBuilder)
    {
        if( section.isEntry())
            // синхронно - исключение летит сразу
            section.parse(fruBuilder);
        else
            section.parse(fruBuilder);
    }

    /** */
    public static void parseFru(
            Reader r,
            FruBuilder fruBuilder,
            boolean entryImplicit
    ) {
        final Iterator<AbstractSectionNode> sectionIter =
                new FruParser(
                        FruTokenizer.parse(r, entryImplicit).iterator(),
                        entryImplicit
                );

        AbstractSectionNode previous = null;

        try {
            while (sectionIter.hasNext()) {
                AbstractSectionNode current = sectionIter.next();

                if (previous != null) {
                    submitSection(previous, fruBuilder);
                }

                previous = current;
            }

            if (previous != null) {
                submitSection(previous, fruBuilder);
            }
        }
        catch (RuntimeException e) {
            throw new FruParseException(
                    fruBuilder.fruFile(),
                    "Ошибка при разборе FRU файла",
                    e
            );
        }
    }
}
