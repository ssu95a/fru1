package ru.inversion.fru.print.altprint.doc;

import ru.inversion.fru.print.altprint.doc.formatted.StyleState;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.fru.print.naltprn.cmd.AltCommand;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlainHeaderStyleReader {

    private PlainHeaderStyleReader( ) {
    }

    /**
     * Читает заголовок файла (PARAMS_ONLY_AT_START) и формирует итоговый StyleState
     *
     * @param file         файл
     * @param charset      кодировка
     * @param dict         словарь команд
     * @param headerLength длина заголовка в символах
     */
    public static StyleState readHeader(Path file, Charset charset, AltCommandDict dict, int headerLength ) throws IOException {

        try( BufferedReader reader = Files.newBufferedReader( file, charset ) )
        {

            StyleState style = dict.getInitCommand().toStyleState();

            int read = 0;
            int ch;

            while( read < headerLength && (ch = reader.read()) != -1)
            {
                read++;

                if( ch == '`' )
                {
                    String cmdText = readCommand( reader, headerLength - read );
                    read += cmdText.length() + 1; // +1 за закрывающий `

                    AltCommand cmd = dict.getCommand(cmdText);

                    if( cmd != null )
                        style = cmd.applyTo( style, null );

                    continue;
                }

                /* явные управляющие команды */
                if (ch == 'L') {
                    if (tryConsume(reader, "F", headerLength - read)) {
                        read++;
                        continue;
                    }
                }

                if (ch == 'P') {
                    if (tryConsume(reader, "AGE_END", headerLength - read)) {
                        read += "AGE_END".length();
                        continue;
                    }
                }

                /* все остальные символы игнорируем */
            }

            return style;
        }
    }

    /** */
    public static String readCommand( Reader reader, int maxRemaining ) throws IOException
    {

        StringBuilder sb = new StringBuilder();
        int ch;
        int read = 0;

        while( read < maxRemaining && (ch = reader.read()) != -1)
        {
            read++;

            if( ch == '`' )
                break;

            sb.append((char) ch);
        }

        return sb.toString().trim();
    }

    /** */
    private static boolean tryConsume( Reader reader, String expected, int maxRemaining ) throws IOException
    {
        if( expected.length() > maxRemaining )
            return false;

        reader.mark( expected.length() );

        for( int i = 0; i < expected.length(); i++ )
        {
            int ch = reader.read();

            if( ch != expected.charAt(i) ) {
                reader.reset();
                return false;
            }
        }
        return true;
    }
}
