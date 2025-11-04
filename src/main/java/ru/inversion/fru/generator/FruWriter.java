package ru.inversion.fru.generator;

import ru.inversion.utils.S;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

// TrackPosition
/** */
public class FruWriter extends Writer {

    final private Writer out;

    private int currentLine       = 1;
    private int currentCharInLine = 1;
    private int totalChars        = 0;

    /** */
    public FruWriter(Writer out ) {
        this.out = Objects.requireNonNull(out, "'out' is null");
    }

    @Override
    public void write( char[] cbuf, int off, int len ) throws IOException {
        if( len > 0 ) {
            for( int i = off; i < off + len; i++ ) updatePosition(cbuf[i]);}
        out.write(cbuf, off, len);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        if (len > 0) {
            for( int i = off; i < off + len; i++ ) updatePosition(str.charAt(i));
        }
        out.write(str, off, len);
    }

    @Override
    public void write(int c) throws IOException {
        updatePosition((char) c);
        out.write(c);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getCurrentCharInLine() {
        return currentCharInLine;
    }

    public int getTotalChars() {
        return totalChars;
    }

    /** */
    private void updatePosition( char c )
    {
        if( c == '\n' )
        {
            currentLine++;
            currentCharInLine=1;
        }
        else
        {
            currentCharInLine++;
        }
        totalChars++;
    }

    /** */
    public void appendEmptyLine( ) throws IOException {
        String spaces = S.space( currentCharInLine, ' ' );
        write("\n");
        write(spaces);
    }

    /** */
    public FruWriter print( String value )
    {
        try {
            write(value == null ? S.EMPTY_STRING : value);
        } catch (IOException e) {
            throw new RuntimeException("Error on write value: " + value, e );
        }
        return this;
    }

    /** */
    public FruWriter newLine(  )
    {
        try {
            write('\n');
        } catch (IOException e) {
            throw new RuntimeException("Error on write nl value", e );
        }
        return this;
    }

}
