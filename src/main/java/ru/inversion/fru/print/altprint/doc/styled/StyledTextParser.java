package ru.inversion.fru.print.altprint.doc.styled;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;
import ru.inversion.fru.print.naltprn.cmd.AltParameter;
import ru.inversion.fru.print.naltprn.cmd.AltParameterTypeEnum;
import ru.inversion.utils.ReaderScanner;
import ru.inversion.utils.U;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.awt.Font;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fru.print.altprint.doc.styled.IStyledTextParser.TextFlowControl.LINE_FEED;
import static ru.inversion.fru.print.altprint.doc.styled.IStyledTextParser.TextFlowControl.PAGE_FEED;

/** */
public final class StyledTextParser implements IStyledTextParser {

    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    private final AltCommandDict commandDict;

    private final Iterator<ReaderScanner.IContext> scanner;

    private StyleState currentStyle;

    /** */
    public StyledTextParser( Reader reader, AltCommandDict dict )
    {
        this.scanner      = ReaderScanner.newIterable(reader).iterator();
        this.commandDict  = dict;
        this.currentStyle = commandDict.getInitCommand().toStyleState();
    }

    /** */
    @Override
    public boolean hasNext( ) {
        return scanner.hasNext();
    }

    /** */
    @Override
    public ParsedElement next() {

        if( !hasNext() )
            throw new NoSuchElementException();

        StyleState span = currentStyle;

        try {

            final StringBuilder text = new StringBuilder();
            final List<Span>   spans = new ArrayList<>();

            char ch;
            ReaderScanner.IContext ctx;

            while( scanner.hasNext() )
            {
                ctx = scanner.next();
                ch  = ctx.current();

                if( U.inChar( ch, /*'\n',*/ '\f', '`' ) )
                {
                    if( ch == '\f' )
                        return PAGE_FEED;

                    if( ch == '\n' )
                        return LINE_FEED;

                    if( ch == '`' )
                    {
                        final AltParameter<?> altParameter = readCommand( scanner, commandDict );

                        if( altParameter != null )
                            span = altParameter.applyTo( span, null );

                        continue;
                    }
                }

                int pos = text.length();
                text.append(ch);

                if( spans.isEmpty() )
                    spans.add( new Span(0, 1, span ) );
                else
                {
                    Span last = spans.get( spans.size() - 1 );
                    if( last.style() == span )
                        spans.set( spans.size() - 1, new Span( last.start(), pos + 1, last.style() ));
                    else
                        spans.add( new Span(pos, pos + 1, span ) );
                }

                if( U.inChar( ctx.next(), '\f', '\n', '`' ) && !isBlank(text) ) {
                    currentStyle = span;
                    return new StyledTextChunk( text.toString(), spans);
                }
            }

            if( !isBlank(text) )
                return new StyledTextChunk( text.toString(), spans);
            else
                return null;

        } catch (Exception e) {
            throw new ALTPrintException("Ошибка при чтении файла", e);
        }
    }

    /** */
    private static boolean isBlank(StringBuilder sb ) {
        if( sb.length() == 0 )
            return true;
       return sb.length() == 1 && Character.isSpaceChar(sb.charAt(0));
    }

    /** */
    static AltParameter<?> readCommand( Iterator<ReaderScanner.IContext> scanner, AltCommandDict dict )
    {
        final StringBuilder sb = new StringBuilder();

        final ReaderScanner.IContext ctx = scanner.next();

        while( scanner.hasNext() && ctx.current() != '`' )
        {
            sb.append( ctx.current() );
            scanner.next();
        }

        if( sb.length() == 0 )
            return null;

        final String cmdText = sb.toString();

        Optional<AltParameter<?>> altParameter = dict.resolveCommand(cmdText);
        if( altParameter.isPresent() )
        {
            final AltParameter<?> pv = altParameter.get();
            if( U.in( pv.getType(), AltParameterTypeEnum.ORIENTATION, AltParameterTypeEnum.COPIES ) )
                return null;
            return pv;
        }
        else {
            logger.warn("В словаре не найдена команда");
            return null;
        }
    }

    /*
    @Override
    public ParsedElement next() {

        if( eof )
            throw new NoSuchElementException();

        try {

            StringBuilder text = new StringBuilder();
            List<Span>   spans = new ArrayList<>();

            StyleState currentStyle = commandDict.getInitCommand().toStyleState();
            StyleState spanStyle    = currentStyle;

            boolean textStarted = false;

            int ch;
            while( (ch = reader.read()) != -1 )
            {
                if( ch == '\f' )
                {
                    if( text.length() > 0 )
                        return new StyledTextChunk(text.toString(), spans);

                    return PAGE_FEED;
                }

                if( ch == '\n' )
                {
                    if( text.length() > 0 )
                        return new StyledTextChunk(text.toString(), spans);

                    return LINE_FEED;
                }

                if( ch == '`' )
                {
                    String cmdText = readCommand( );

                    if( "PAGE_END".equalsIgnoreCase(cmdText) )
                    {
                        if (text.length() > 0)
                            return new StyledTextChunk(text.toString(), spans);

                        return PAGE_FEED;
                    }

                    if ("LF".equalsIgnoreCase(cmdText)) {

                        if (text.length() > 0)
                            return new StyledTextChunk(text.toString(), spans);

                        return LINE_FEED;
                    }

                    AltCommand cmd = commandDict.getCommand(cmdText);

                    if( cmd != null && !textStarted )
                    {
                        currentStyle = cmd.applyTo(currentStyle, null);
                        spanStyle    = currentStyle;
                    }

                    continue;
                }

                if( !textStarted )
                {
                    textStarted = true;
                    spanStyle = currentStyle;
                }

                int pos = text.length();
                text.append((char) ch);

                if (spans.isEmpty()) {
                    spans.add(new Span(0, 1, spanStyle));
                } else {
                    Span last = spans.get(spans.size() - 1);
                    if (last.style() == spanStyle) {
                        spans.set(spans.size() - 1,
                                new Span(last.start(), pos + 1, last.style()));
                    } else {
                        spans.add(new Span(pos, pos + 1, spanStyle));
                    }
                }
            }

            eof = true;

            if (text.length() > 0)
                return new StyledTextChunk(text.toString(), spans);

            //throw new NoSuchElementException();
            return null;

        } catch (Exception e) {
            throw new ALTPrintException("Ошибка при чтении файла", e);
        }
    }
    */


    private static StyleState defaultStyle() {
        return new StyleState( "Monospaced", 10, Font.PLAIN, false, 0.0f, 0.5f, 0.5f );
    }
}

