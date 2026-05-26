package ru.inversion.fru.print.altprint.doc;

import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.fru.print.altprint.*;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;
import ru.inversion.fru.print.naltprn.cmd.AltParameter;
import ru.inversion.utils.Pair;
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
import java.util.Optional;

import static ru.inversion.fru.print.altprint.doc.PlainHeaderStyleReader.readCommand;
import static ru.inversion.fru.print.naltprn.cmd.AltParameterTypeEnum.*;

/** Документ альтернативной печати */
public class ALTDoc {

    public enum AltDocContentMode {
        PLAIN,
        PLAIN_WITH_HEADER,
        STYLED
    }

    /** Путь до */
    final private Path altFile;

    /** Кодировка */
    final private Charset charset;

    /** Настройки печати */
    final private AltPrintPageConfig pageConfig;

    /** */
    private final AltDocContentMode contentMode;
    private final int contentOffset;

    /** */
    private ALTDoc( Path altFile, Charset charset, AltPrintPageConfig pc, AltDocContentMode contentMode, int contentOffset )
    {
        this.altFile       = altFile;
        this.charset       = charset;
        this.contentMode   = contentMode;
        this.contentOffset = contentOffset;
        this.pageConfig    = pc;
    }

    /** */
    public AltPrintPageConfig getPageConfig() {
        return pageConfig;
    }

    public AltDocContentMode getContentMode() {
        return contentMode;
    }

    /** */
    public int getContentOffset() {
        return contentOffset;
    }

    /** */
    public boolean isStyled() {
        return contentMode == AltDocContentMode.STYLED;
    }

    /** */
    public boolean hasHeaderCommands( ) {
        return contentMode == AltDocContentMode.PLAIN_WITH_HEADER;
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
    public Copies getCopies() {
        return U.nvl( FruEngineConfig.instance().getCopies(), getPageConfig().getCopies() );
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
            if( contentOffset > 0 )
                skipHeader( reader, contentOffset );

            final RawCAW r = new RawCAW( (int)Files.size(altFile) );
            r.write( reader );
            return r;
        }
        catch( IOException e) {
            throw new ALTException( "Ошибка при чтении файла " + altFile, e );
        }
    }

    /** */
    public ALTDocPrintable makePrintable( IAltPrintListener listener ) throws IOException {
        return ALTDocPrintable.load( this, listener, pageConfig );
    }


    /** */
    private static Pair<AltDocContentMode,Integer> getContentState( Path file, Charset charset, AltCommandDict dict, AltPrintPageConfig.Builder b ) throws IOException {

        try( Reader br = Files.newBufferedReader( file, charset ) )
        {
            int ch;
            int offset = 0;
            int realOffset = 0;

            boolean sawStyleCommand = false;
            boolean textStarted     = false;

            while((ch = br.read()) != -1) {

                offset++;

                // допустимые управляющие
                if( ch == '\n' || ch == '\f' )
                    continue;

                // команда
                if (ch == '`' )
                {
                    String cmdText = readCommand( br, Integer.MAX_VALUE );
                    offset += cmdText.length() + 1;

                    if( !textStarted )
                         realOffset = offset;

                    Optional<AltParameter<?>> altParameter = dict.resolveCommand(cmdText);
                    if(!altParameter.isPresent() )
                        continue;

                    if( altParameter.get().getType() == ORIENTATION )
                        b.orientation( (OrientationRequested)altParameter.get().getValue() );

                    if( altParameter.get().getType() == COPIES )
                        b.copies( (Integer) altParameter.get().getValue() );

                    if( U.notIn( altParameter.get().getType(), PAGE_END, LF, ORIENTATION, COPIES ) )
                    {
                        if( textStarted ) {
                            //  style-команда ПОСЛЕ текста
                            return Pair.makePair( AltDocContentMode.STYLED, 0 ); // TEXT_WITH_PARAMS
                        }

                        sawStyleCommand = true;
                    }

                    continue;
                }

                // любой непробельный символ → начался текст
                if(!Character.isWhitespace(ch)) {
                    textStarted = true;
                    continue; //
                }
            }

            if(!sawStyleCommand)
                return Pair.makePair( AltDocContentMode.PLAIN, 0 );

            return Pair.makePair( AltDocContentMode.PLAIN_WITH_HEADER, realOffset );
        }
    }


    /** */
    public static ALTDoc loadFile( Path file, Charset charset )
    {
        try {

            AltPrintPageConfig.Builder b = AltSettings.INSTANCE().commandDict().getInitAltPrintPageConfig();

            Pair<AltDocContentMode,Integer> contentInfo = getContentState( file, charset, AltSettings.INSTANCE().commandDict(), b );
            return new ALTDoc( file, charset, b.build(), contentInfo.first, contentInfo.second );
        }
        catch (Exception ex) {
            throw new ALTException( "Ошибка при загрузке файла с отчетом " + file, ex );
        }
    }

    /** */
    public OrientationRequested getOrientation() {
        return pageConfig.getOrientation();
    }

    private static void skipHeader(Reader reader, long count) throws IOException {

        long remaining = count;

        while (remaining > 0) {
            long skipped = reader.skip(remaining);
            if (skipped <= 0) {
                if (reader.read() == -1) {
                    break;
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }
}
