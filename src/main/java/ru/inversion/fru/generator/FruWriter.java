package ru.inversion.fru.generator;

import ru.inversion.fru.model.items.FruPaging;
import ru.inversion.property.Property;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.lstn.IListenerManConsumer;
import ru.inversion.utils.lstn.ListenerManFactory;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/** */
public class FruWriter extends Writer {

    final private Writer out;

    private Writer current;
    private Writer buffer;   // единственный буфер (null если выключен)

    private int currentPage       = 1;
    private int currentLine       = 1;
    private int currentCharInLine = 1;
    private int totalChars        = 0;

    final private FruPaging paging;

    final private BiConsumer<Integer, FruPaging> pageConsumer;

    /** */
    private IListenerManConsumer<Consumer<Pair<Integer,Integer>>> nlListeners;

    /** */
    public FruWriter( Writer out ) {
        this.out     = Objects.requireNonNull(out, "'out' is null");
        this.current = out;
        this.paging  = null;
        this.pageConsumer = null;
    }

    /** */
    public FruWriter( Writer out, FruPaging paging, BiConsumer<Integer, FruPaging> pc ) {
        this.out     = Objects.requireNonNull( out, "'out' is null" );
        this.current = out;
        this.paging  = paging;
        this.pageConsumer = pc;
    }

    public int currentLine() {
        return currentLine;
    }

    public int currentCharInLine() {
        return currentCharInLine;
    }

    /** */
    public void addNLListener( Consumer<Pair<Integer,Integer>> nlListener )
    {
        if( nlListeners == null )
            nlListeners = ListenerManFactory.createListenerManConsumer();
        nlListeners.addListener( nlListener );
    }

    /** */
    public void removeNLListener( Consumer<Pair<Integer,Integer>> nlListener )
    {
        if( nlListeners != null ) {
            nlListeners.removeListener(nlListener);
        }
    }

    /** */
    private void fireNLListener()
    {
        if( nlListeners != null ) {
            final Pair<Integer,Integer> pn = Pair.makePair(currentPage,currentLine);
            nlListeners.fire((l) -> l.accept(pn));
        }
    }

    /** Использовать внешний Writer как буфер */
    public void startBuffer( Writer externalBuffer )
    {
        Objects.requireNonNull( externalBuffer, "externalBuffer");

        if( buffer != null )
            throw new IllegalStateException("Buffer already started");

        buffer  = externalBuffer;
        current = buffer;
    }

    /** Включить буферизацию */
    public void startBuffer()
    {
        startBuffer( new CharArrayWriter(1024) );
    }

    /** Слить буфер out */
    public void stopBuffer() {

        if( buffer == null )
            return;

        try {

            final Writer finished = buffer;

            buffer  = null;
            current = out;

            if( finished instanceof CharArrayWriter )
            {
                final CharArrayWriter cw = (CharArrayWriter) finished;
                if( cw.size() > 0 )
                    cw.writeTo(out);
            }
            else
                finished.flush(); // ответственность за слив на внешнем writer

            out.flush();
        }
        catch ( IOException ex ) {
            throw new RuntimeException( "Error on switch to 'out' writer", ex );
        }
    }

    /** Выкинуть буфер без слива */
    public void discardBuffer() {
        buffer = null;
        current= out;
    }

    /** */
    public boolean isBufferEnabled() {
        return buffer != null;
    }

    /** */
    @Override
    public void write( char[] cbuf, int off, int len ) throws IOException {

        if( len > 0 )
            for( int i = off; i < off + len; i++ )
                 updatePosition( cbuf[i] );

        current.write( cbuf, off, len );
    }

    @Override
    public void write( String str, int off, int len ) throws IOException {

        if( len > 0 )
            for( int i = off; i < off + len; i++ )
                updatePosition( str.charAt(i) );

        current.write( str, off, len );
    }

    @Override
    public void write(int c) throws IOException {
        updatePosition((char) c);
        current.write(c);
    }

    @Override
    public void flush() throws IOException {
        current.flush();
    }

    @Override
    public void close() throws IOException {
        current.close();
    }

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
    private void incrementLine()
    {
        currentLine++;
        //fireNLListener();
    }

    /** */
    private void resetLine()
    {
        currentLine = 1;
        //fireNLListener();
    }

    /** */
    private void updatePosition( char c ) throws IOException {

        if( c == '\n' )
        {
            //write(" marker:" );write(marker);

            currentCharInLine=1;
            incrementLine( );
        }
        else
            currentCharInLine++;

        totalChars++;

        if( paging != null )
        {
            if( paging.getLines() == currentLine )
            {
                if( paging.isBottom() && pageConsumer != null )
                    this.pageConsumer.accept( currentPage, paging );

                currentPage ++;
                resetLine( );

                if( paging.isTop() && pageConsumer != null )
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
                current.write(value);
            else
                write( value == null ? S.EMPTY_STRING : value );

        } catch ( IOException e) {
            throw new RuntimeException( "Error on write value: " + value, e );
        }

        return this;
    }

    public String marker = S.EMPTY_STRING;

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
