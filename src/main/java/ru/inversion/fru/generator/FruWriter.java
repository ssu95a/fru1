package ru.inversion.fru.generator;

import ru.inversion.fru.generator.impl.RTrimWriter;
import ru.inversion.fru.model.items.FruPaging;
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

    private final RTrimWriter out;

    private Writer current;
    private Writer buffer;   // единственный буфер (null если выключен)

    private int currentPage     = 1;
    private int currentPageLine = 1;

    private final FruPaging paging;

    private final BiConsumer<Integer, FruPaging> pageConsumer;

    /** */
    public FruWriter(Writer out) {

        this.paging       = null;
        this.pageConsumer = null;

        this.out     = new RTrimWriter( Objects.requireNonNull(out, "'out' is null"), this::onWrittenChar );

        this.current = this.out;
    }

    /** */
    public FruWriter(Writer out, FruPaging paging, BiConsumer<Integer, FruPaging> pc) {
        this.paging       = paging;
        this.pageConsumer = pc;

        this.out = new RTrimWriter(
            Objects.requireNonNull(out, "'out' is null"),
            this::onWrittenChar
        );

        this.current = this.out;
    }

   /**
    * Строка внутри текущей FRU-страницы.
    */
    public int currentLine() {
        return currentPageLine;
    }

   /**
    * Позиция в физической output-строке после rtrim.
    */
    public int currentCharInLine() {
        return out.currentCharInLine();
    }

    public int getCurrentCharInLine() {
        return out.currentCharInLine();
    }

    public int getTotalChars() {
        return out.totalChars();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Для диагностики: физическая строка output-а после rtrim,
     * без сброса на page break.
     */
    public int physicalLine() {
        return out.physicalLine();
    }

    /** Использовать внешний Writer как буфер */
    public void startBuffer( Writer externalBuffer )
    {
        Objects.requireNonNull( externalBuffer, "externalBuffer" );

        if( buffer != null )
            throw new IllegalStateException("Buffer already started");

        try {
            /*
             * Не оставляем часть строки внутри RTrimWriter
             * перед переключением current на другой writer.
             */
            out.flushLineRaw(true);
        }
        catch (IOException e) {
            throw new RuntimeException("Error on start buffer", e);
        }

        buffer  = externalBuffer;
        current = buffer;
    }

    /** Включить буферизацию */
    public void startBuffer() {
        startBuffer(new CharArrayWriter(1024));
    }

    /** Слить буфер в out */
    public void stopBuffer() {

        if (buffer == null)
            return;

        try {
            final Writer finished = buffer;

            buffer = null;
            current = out;

            if (finished instanceof CharArrayWriter) {
                final CharArrayWriter cw = (CharArrayWriter) finished;

                if (cw.size() > 0)
                    cw.writeTo(out);
            }
            else {
                /*
                 * Внешний writer сам отвечает за слив.
                 * Статистика RTrimWriter по нему не обновляется.
                 * Это соответствует старой "externalBuffer" семантике.
                 */
                finished.flush();
            }

            out.flush();
        }
        catch (IOException ex) {
            throw new RuntimeException("Error on switch to 'out' writer", ex);
        }
    }

    /** Выкинуть буфер без слива */
    public void discardBuffer() {
        buffer = null;
        current = out;
    }

    /** */
    public boolean isBufferEnabled() {
        return buffer != null;
    }

    /** */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        current.write(cbuf, off, len);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        current.write(str, off, len);
    }

    @Override
    public void write(int c) throws IOException {
        current.write(c);
    }

    @Override
    public void flush() throws IOException {
        current.flush();
    }

    @Override
    public void close() throws IOException {

        if (buffer != null) {
            stopBuffer();
        }

        out.close();
    }

    /**
     * Вызывается только для реально записанных символов после rtrim.
     */
    private void onWrittenChar(char c) throws IOException {

        if (c != '\n')
            return;

        incrementPageLine();

        updatePagingAfterNewLine();
    }

    private void incrementPageLine() {
        currentPageLine++;
    }

    private void resetPageLine() {
        currentPageLine = 1;
    }

    private void updatePagingAfterNewLine() throws IOException {

        if (paging == null)
            return;

        if (paging.getLines() == currentPageLine) {

            if (paging.isBottom() && pageConsumer != null)
                this.pageConsumer.accept(currentPage, paging);

            currentPage++;
            resetPageLine();

            if (paging.isTop() && pageConsumer != null)
                this.pageConsumer.accept(currentPage, paging);
        }
    }

    /** */
    public FruWriter print(String value) {
        return print(value, false);
    }

    /** */
    public FruWriter print(String value, boolean force) {
        try {
            final String v = value == null ? S.EMPTY_STRING : value;

            if (force) {
                if (current instanceof RTrimWriter) {
                    ((RTrimWriter) current).writeForced(v);
                }
                else {
                    /*
                     * при активном external buffer пишем напрямую,
                     * без статистики и paging.
                     */
                    current.write(v);
                }
            }
            else {
                write(v);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Error on write value: " + value, e);
        }

        return this;
    }

    public String marker = S.EMPTY_STRING;

    /** */
    public FruWriter newLine() {
        try {
            //write(marker);
            write('\n');
            //marker = S.EMPTY_STRING;
        }
        catch (IOException e) {
            throw new RuntimeException("Error on write nl value", e);
        }

        return this;
    }
}