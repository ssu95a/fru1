package ru.inversion.fru.data;

import ru.inversion.fru.data.exceptions.FruDataException;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** */
public class FruDataFile implements Iterator<Pair<Integer,List<String>>>, AutoCloseable {

    public static final char FIELD_TERMINATOR  = 0x12;
    public static final char CURSOR_TERMINATOR = 0x0c;

    private BufferedReader reader;

    private int currentNum  = -1;
    private List<String> currentLine;
    boolean entryEmitted = false;

    private final List<String> entryFields = new ArrayList<>();

    private FruDataFile( BufferedReader reader ) throws IOException {

        this.reader = Objects.requireNonNull(reader,"'reader' is null");

        String line = reader.readLine();

        if( S.isNullOrEmpty(line) || line.charAt(0) != FIELD_TERMINATOR || line.charAt(1) != CURSOR_TERMINATOR )
            throw new IllegalStateException("Не валидный формат заголовка. Ожидается: header[0] - '0x12', header[1] - '0x0c'");

        readEntry( );
    }

    /** */
    public FruDataFile( Path file, Charset charset ) throws IOException {
        this( Files.newBufferedReader(file, charset) );
        // throw new FruDataException(file, "Файл " + file + " не является форматом FRU/UFS с данными");
    }

    /** */
    public FruDataFile( Reader reader ) throws IOException {
        this( reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader( reader ) );
        // throw new FruDataException(file, "Файл " + file + " не является форматом FRU/UFS с данными");
    }

    /** */
    private String readField( String line ) throws IOException {

        if( S.lastChar(line) == FIELD_TERMINATOR )
            return line;

        final StringBuilder sb = new StringBuilder(line);
        String l;
        while( (l = reader.readLine()) != null )
        {
            sb.append('\n').append(l);

            if( S.lastChar(l) == FIELD_TERMINATOR )
                break;

        }

        return sb.toString();
    }

    /** */
    private List<String> readRowSingle( ) throws IOException
    {
        if( reader == null )
            return null;

        final List<String> currentRec = new ArrayList<>();
        String line;

        int warnLeft = 10;

        while ((line = reader.readLine()) != null)
        {
            if( line.length() == 1 && line.charAt(0) == CURSOR_TERMINATOR ) {
                break; // конец блока
            }

            if( line.startsWith("CURSOR") ) {
                currentNum = parseCursorNum(line);
                continue;
            }

            // Мусор/анонимные строки после entry: пропускаем до первого CURSOR<n>
            if (currentNum < 1)
            {
                if( warnLeft > 0 ) {
                    warnLeft--;
                    //log.warn("Skip garbage before first CURSOR: '{}'", safeSnippet(line));
                    System.err.println( "Skip garbage before first CURSOR: " + line );
                }
                continue;
            }

            String value = normalize( readField(line) );
            currentRec.add( U.nvl( value, S.EMPTY_STRING) );
        }

        if( line == null )
            close();

        return currentRec;
    }

    /** */
    private List<String> readRow() throws IOException {

        List<String> rec;

        do {

            rec = readRowSingle();

        } while( reader != null && rec != null && rec.isEmpty() );

        return( rec == null || rec.isEmpty() ) ? null : rec;
    }

    /** */
    private void readEntry( ) throws IOException
    {
        String line;

        int warnLeft = 20;

        while( (line = reader.readLine()) != null )
        {
            // конец блока
            if( line.length() == 1 && line.charAt(0) == CURSOR_TERMINATOR)
            {
                // завершили преамбулу; теперь читаем первый курсорный блок (если будет)
                currentLine = readRow();
                return;
            }

            if( line.startsWith("CURSOR") )
            {
                currentNum  = parseCursorNum(line);
                currentLine = readRow();
                return;
            }

            String value = normalize( readField(line) );
            entryFields.add(U.nvl( value, S.EMPTY_STRING ));
        }

        // EOF: курсоров нет
        close();

        currentLine = null;
    }

    /** */
    @Override
    public boolean hasNext() {
        return !entryEmitted || currentLine != null;
    }


    /** */
    private String normalize( String s )
    {
        if( S.isNullOrEmpty(s) )
            return null;

        if( S.lastChar(s) == FIELD_TERMINATOR )
            s = s.substring( 0, s.length() - 1 );

        return S.isNullOrEmpty(s) ? null : s;
    }

    /** */
    private int parseCursorNum( String line ) {

        if( line.equals("CURSOR0") )
            throw new IllegalStateException("CURSOR0 запрещён форматом");

        try {
            return Integer.parseInt( line.substring("CURSOR".length()) );
        } catch (NumberFormatException n) {
            throw new IllegalStateException("Ошибочная строка '" + line + "': CURSOR - не корректный номер курсора");
        }
    }

    /** */
    @Override
    public Pair<Integer, List<String>> next( )
    {
        if( !hasNext() )
            throw new java.util.NoSuchElementException();

        try {

            if(!entryEmitted )
            {
                entryEmitted = true;
                return Pair.makePair( -1, entryFields );
            }

            List<String> a = currentLine;
            int cn         = currentNum;

            currentLine    = readRow();

            return Pair.makePair( cn,  a );

        } catch( IOException e ) {
            throw new RuntimeException(e);
        }
    }

    /** */
    @Override
    public void close() {

        try {

            if( reader != null )
                reader.close();
        }
        catch( IOException ignored ) {
        }
        finally {
            reader = null;
        }
    }
}
