package ru.inversion.fru.print.altprint.doc;

import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.fru.print.altprint.*;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.fru.print.naltprn.cmd.AltCommand;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;
import ru.inversion.utils.U;
import ru.inversion.utils.io.RawCAW;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.inversion.fru.print.altprint.doc.PlainHeaderStyleReader.readCommand;

/** Документ альтернативной печати */
public class ALTDoc {

    /** Путь до */
    final private Path altFile;

    /** Кодировка */
    final private Charset charset;

    /** Настройки печати */
    final private PrintSettings printSettings = new PrintSettings(  );

    /** */
    final private int contentState;

    /** */
    private ALTDoc( Path altFile, Charset charset, int contentState )
    {
        this.altFile      = altFile;
        this.charset      = charset;
        this.contentState = contentState;
    }

    public int getContentState( ) {
        return contentState;
    }

    /** */
    public Path getAltFile( ) {
        return altFile;
    }

    /** */
    public Charset getCharset( ) {
        return charset;
    }

    /** */
    public OrientationRequested getOrientation() {
        return printSettings.resolveOrientation(OrientationRequested.PORTRAIT);
    }
    public void setOrientation(OrientationRequested orientation) {
        this.printSettings.setOrientation(orientation );
    }

    /** */
    public Copies getCopies() {
        return U.nvl( FruEngineConfig.instance().getCopies(), printSettings.resolveCopies(new Copies(1)) );
    }
    public void setCopies(Copies copies) {
        this.printSettings.setCopies( copies );
    }

    /** */
    public boolean isLoaded( )
    {
        return altFile != null;
    }

    /** */
    public CharSequence readFile()
    {
        try( BufferedReader reader = Files.newBufferedReader( altFile, charset ) )
        {
            final RawCAW r = new RawCAW( (int)Files.size(altFile) );
            r.write( reader );
            return r;
        }
        catch( IOException e) {
            throw new ALTException( "Ошибка при чтении файла " + altFile, e );
        }
    }

    /** */
    public ALTDocPrintable makePrintable( IAltPrintListener listener, AltPrintPageConfig pageConfig ) throws IOException {
        return ALTDocPrintable.load( this, listener, pageConfig );
    }


    /**
     * Определяет тип содержимого сверстанного файла с данными.
     * 0 - только текст, NO_PARAMS
     * > 0 - текст, но с начала идут инструкции в параметрах, PARAMS_ONLY_AT_START
     * -1 - файл с командами форматирования, TEXT_WITH_PARAMS
     */
    private static int getContentState( Path file, Charset charset, AltCommandDict dict ) throws IOException {

        try( Reader br = Files.newBufferedReader(file, charset) )
        {
            int ch;
            int offset = 0;

            boolean sawStyleCommand = false;
            boolean textStarted    = false;

            while ((ch = br.read()) != -1) {
                offset++;

                // допустимые управляющие
                if (ch == '\n' || ch == '\f') {
                    continue;
                }

                // команда
                if (ch == '`')
                {
                    String cmdText = readCommand( br, Integer.MAX_VALUE );
                    offset += cmdText.length() + 1;

                    int ix = cmdText.indexOf(',');

                    if( ix > 0 )
                        cmdText = cmdText.substring(0,ix);

                    AltCommand cmd = dict.getCommand(cmdText);
                    if (cmd == null)
                        continue;

                    if( cmd.isStyleChanging() )
                    {
                        if (textStarted) {
                            //  style-команда ПОСЛЕ текста
                            return -1; // TEXT_WITH_PARAMS
                        }
                        sawStyleCommand = true;
                    }

                    continue;
                }

                // любой непробельный символ → начался текст
                if (!Character.isWhitespace(ch)) {
                    textStarted = true;
                    continue; //
                }
            }

            if (!sawStyleCommand)
                return 0;           // NO_PARAMS

            return offset;          // PARAMS_ONLY_AT_START
        }
    }




    /** */
    public static ALTDoc loadFile( Path file, Charset charset )
    {
        try {

            int contentState = getContentState( file, charset, AltSettings.INSTANCE().commandDict() );

            return new ALTDoc( file, charset, contentState );
        }
        catch (Exception ex) {
            throw new ALTException( "Ошибка при загрузке файла с отчетом " + file, ex );
        }
    }
}
