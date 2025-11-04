package ru.inversion.fru.data;

import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/** */
public class FruDataFile implements Iterator<Pair<Integer,List<String>>>, AutoCloseable {

    public static final char RESET_CHAR = 'T';
    public static final char RESUME_CHAR = 'E';
    public static final char FIELD_TERMINATOR = 0x12;
    public static final char CURSOR_TERMINATOR = 0x0c;

    private BufferedReader reader;

    private int currentNum  = 0;
    private List<String> currentLine;

    /** */
    public FruDataFile( Path file, Charset charset ) throws IOException {

        reader = Files.newBufferedReader( file, charset );
        String line = reader.readLine();

        if( S.isNullOrEmpty(line) || line.length() != 2 || line.charAt(0) != FIELD_TERMINATOR || line.charAt(1) != CURSOR_TERMINATOR )
            throw new IllegalStateException("Файл не является форматом FRU/UFS с данными");

        currentLine = nextLine();
    }

    /** */
    private String normalize( String s )
    {
        s = s.trim();

        if( S.lastChar(s) == FIELD_TERMINATOR )
            s = s.substring( 0, s.length() - 1 );

        if( S.isNullOrEmpty(s) )
            return null;

        return s;
    }

    /** */
    @Override
    public boolean hasNext() {
        return currentLine != null || reader != null;
    }

    /** */
    private List<String> nextLine( ) throws IOException {

        if( reader == null )
            return null;

        final List<String> currentRec = new ArrayList<>();

        String line;

        while( ( line = reader.readLine() ) != null ) {

            //ncount ++;

            if( line.length() == 1 && line.charAt(0) == CURSOR_TERMINATOR )
            {
                break;
            }

            if( line.startsWith("CURSOR") ) {
                try {
                    currentNum = Integer.parseInt(line.substring("CURSOR".length()));
                } catch (NumberFormatException n) {
                    throw new IllegalStateException("Ошибочная строка '" + line + "' CURSOR - не корректный номер курсора");
                }
                continue;
            }

            if( S.lastChar(line) == FIELD_TERMINATOR )
            {
                currentRec.add( normalize( line ) );
            }
            else
            {
                final StringBuilder sb = new StringBuilder(line);
                while( (line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                    if( S.lastChar(line) == FIELD_TERMINATOR )
                        break;
                }//
                currentRec.add( normalize( sb.toString() ) );
            }
        }// end while

        if( line == null )
        {
            reader.close();
            reader = null;
        }

        return currentRec.isEmpty() ? null : currentRec;
    }

    /** */
    @Override
    public Pair<Integer, List<String>> next() {
        try {
            List<String> a = currentLine;
            int cn         = currentNum;
            currentLine = nextLine();
            return Pair.makePair( cn,  a );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** */
    @Override
    public void close() throws Exception {
        if( reader != null )
            reader.close();
        reader = null;
    }
}
