package ru.inversion.fru.print.altprint.doc.formatted;

import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.fru.print.naltprn.cmd.AltCommand;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;

import java.io.*;
import java.util.*;
import java.awt.Font;

import static ru.inversion.fru.print.altprint.doc.formatted.IStyledTextParser.TextFlowControl.LINE_FEED;
import static ru.inversion.fru.print.altprint.doc.formatted.IStyledTextParser.TextFlowControl.PAGE_FEED;

/** */
public final class StyledTextParser implements IStyledTextParser {

    private final BufferedReader reader;
    private final AltCommandDict commandDict;

    private boolean eof;

    /** */
    public StyledTextParser( Reader reader, AltCommandDict dict )
    {
        this.reader      = new BufferedReader(reader);
        this.commandDict = dict;
    }

    /** */
    @Override
    public boolean hasNext( ) {
        return !eof;
    }

    /** */
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
                        return new TextChunk(text.toString(), spans);

                    return PAGE_FEED;
                }

                if( ch == '\n' )
                {
                    if( text.length() > 0 )
                        return new TextChunk(text.toString(), spans);

                    return LINE_FEED;
                }

                if( ch == '`' )
                {
                    String cmdText = readCommand( );

                    if( "PAGE_END".equalsIgnoreCase(cmdText) )
                    {
                        if (text.length() > 0)
                            return new TextChunk(text.toString(), spans);

                        return PAGE_FEED;
                    }

                    if ("LF".equalsIgnoreCase(cmdText)) {

                        if (text.length() > 0)
                            return new TextChunk(text.toString(), spans);

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
                return new TextChunk(text.toString(), spans);

            //throw new NoSuchElementException();
            return null;

        } catch (Exception e) {
            throw new ALTPrintException("Ошибка при чтении файла", e);
        }
    }


    /** */
    private String readCommand() throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int ch;
        while( (ch = reader.read()) != -1)
        {
            if( ch == '`' )
                break;

            sb.append( (char) ch );
        }
        return sb.toString().trim();
    }

    private static StyleState defaultStyle() {
        return new StyleState( "Monospaced", 10, Font.PLAIN, false, 0.0f, 0.5f, 0.5f );
    }
}

