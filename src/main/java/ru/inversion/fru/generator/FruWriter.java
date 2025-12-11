package ru.inversion.fru.generator;

import ru.inversion.fru.generator.renderer.Renderers;
import ru.inversion.fru.model.items.FruPaging;
import ru.inversion.utils.S;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

// TrackPosition
/** */
public class FruWriter extends Writer {

    final private Writer out;

    private int currentPage       = 1;
    private int currentLine       = 1;
    private int currentCharInLine = 1;
    private int totalChars        = 0;

    final private FruPaging paging;

    final private BiConsumer<Integer, FruPaging> pageConsumer;

    /** */
    public FruWriter( Writer out ) {
        this.out = Objects.requireNonNull(out, "'out' is null");
        this.paging = null;
        this.pageConsumer = null;
    }

    /** */
    public FruWriter( Writer out, FruPaging paging, BiConsumer<Integer, FruPaging> pc ) {
        this.out    = Objects.requireNonNull( out, "'out' is null" );
        this.paging = paging;
        this.pageConsumer = pc;
    }

    @Override
    public void write( char[] cbuf, int off, int len ) throws IOException {

        if( len > 0 )
            for( int i = off; i < off + len; i++ )
                updatePosition( cbuf[i] );

        out.write( cbuf, off, len );
    }

    @Override
    public void write( String str, int off, int len ) throws IOException {

        if( len > 0 )
            for( int i = off; i < off + len; i++ )
                updatePosition( str.charAt(i) );

        out.write( str, off, len );
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

    //public int getCurrentLine() {
    //    return currentLine;
    //}

    public int getCurrentCharInLine() {
        return currentCharInLine;
    }
    public int getTotalChars() {
        return totalChars;
    }
    public int getCurrentPage() {
        return currentPage;
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
            currentCharInLine++;

        totalChars++;

        if( paging != null )
        {
            if( paging.getLines() == currentLine )
            {
                if( paging.isBottom() )
                    this.pageConsumer.accept( currentPage, paging );

                currentPage ++;
                currentLine = 1;

                if( paging.isTop() )
                    this.pageConsumer.accept( currentPage, paging );
            }
        }
    }

    /** */
    public FruWriter print( String value )
    {
        return print(value,false);
    }

    /** */
    public FruWriter print( String value, boolean force )
    {
        try {
            if( force )
                out.write(value);
            else
                write( value == null ? S.EMPTY_STRING : value );
        } catch ( IOException e) {
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
