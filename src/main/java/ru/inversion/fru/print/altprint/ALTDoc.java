package ru.inversion.fru.print.altprint;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/** */
public class ALTDoc {

    final private Path altFile;

    final private Charset charset;

    private OrientationRequested orientation;

    private Copies copies;

    /** */
    private ALTDoc( Path altFile, Charset charset, OrientationRequested orientation, Copies copies) {
        this.altFile     = altFile;
        this.charset     = charset;
        this.orientation = orientation;
        this.copies      = copies;
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
        return orientation;
    }
    public void setOrientation(OrientationRequested orientation) {
        this.orientation = orientation;
    }

    /** */
    public Copies getCopies() {
        return copies;
    }

    /** */
    public void setCopies(Copies copies) {
        this.copies = copies;
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
    public Printable makePrintable() throws IOException {
        return ALTDocPrintable.load(this);
    }

    /** */
    public static ALTDoc loadFile( Path file, Charset charset )
    {
        try
        {
            ALTInitCommand initCommand = ALTSettings.INSTANCE().commandDict().getInitCommand();

            return new ALTDoc( file, charset, initCommand.getOrientation(), initCommand.getCopies() );
        }
        catch (Exception ex)
        {
            throw new ALTException("Ошибка при загрузке файла с отчетом " + file, ex);
        }
    }

}
