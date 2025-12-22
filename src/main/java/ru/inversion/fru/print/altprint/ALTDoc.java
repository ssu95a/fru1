package ru.inversion.fru.print.altprint;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/** Документ альтернативной печати */
public class ALTDoc {

    /** Путь до */
    final private Path altFile;

    /** Кодировка */
    final private Charset charset;

    /** Настройки печати */
    final private PrintSettings printSettings = new PrintSettings( ALTSettings.INSTANCE().defSetting() );

    /** */
    private ALTDoc( Path altFile, Charset charset ) {
        this.altFile     = altFile;
        this.charset     = charset;
    }

    /** */
    public Path getAltFile() {
        return altFile;
    }

    /** */
    public Charset getCharset() {
        return charset;
    }

    /** */
    public OrientationRequested getOrientation() {
        return printSettings.getOrientation();
    }
    public void setOrientation(OrientationRequested orientation) {
        this.printSettings.setOrientation(orientation );
    }

    /** */
    public Copies getCopies() {
        return printSettings.getCopies();
    }
    public void setCopies(Copies copies) {
        this.printSettings.setCopies( copies );
    }

    /** */
    public void setPageParameter( ALTParameter<?> parameter, Object value ) {

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
            StringBuilder sb = new StringBuilder( (int)Files.size(altFile) );
            //
            String line;
            while(( line = reader.readLine() ) != null ) {
                    sb.append(line).append('\n');
            }
            return sb;
        }
        catch( IOException e) {
            throw new ALTException( "Ошибка при чтении файла " + altFile, e );
        }
    }

    /** */
    public ALTDocPrintable makePrintable() throws IOException {
        return ALTDocPrintable.load( this );
    }

    /** */
    public static ALTDoc loadFile( Path file, Charset charset )
    {
        try
        {
            return new ALTDoc( file, charset );
        }
        catch (Exception ex) {
            throw new ALTException( "Ошибка при загрузке файла с отчетом " + file, ex );
        }
    }
}
