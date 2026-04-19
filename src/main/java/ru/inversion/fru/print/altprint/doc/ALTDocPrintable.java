package ru.inversion.fru.print.altprint.doc;

import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.IAltPrintListener;
import ru.inversion.fru.print.altprint.doc.styled.ALTDocPrintableStyled;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;
import ru.inversion.fru.print.altprint.doc.plain.ALTDocPrintablePlain;
import ru.inversion.fru.print.naltprn.AltSettings;

import javax.print.*;
import java.awt.*;
import java.awt.print.Printable;
import java.io.IOException;

/** */
public abstract class ALTDocPrintable implements Printable, AutoCloseable {

    final protected ALTDoc altDoc;
    final protected AltPrintPageConfig pageConfig;
    protected IAltPrintListener listener;

    /** */
    protected ALTDocPrintable( ALTDoc altDoc, IAltPrintListener listener, AltPrintPageConfig pageConfig )
    {
        this.altDoc     = altDoc;
        this.listener   = listener;
        this.pageConfig = pageConfig;
    }

    public AltPrintPageConfig getPageConfig() {
        return pageConfig;
    }

    /** */
    public abstract void printToMatrix( PrintService printer ) throws PrintException, IOException;

    /** */
    protected void finishPrint() {
        if( listener != null ) {
            listener.onEndPrint();
            listener = null;
        }
    }

    /** */
    public static ALTDocPrintable load( ALTDoc altDoc, IAltPrintListener listener, AltPrintPageConfig pageConfig ) throws IOException {

        if( altDoc.isStyled() )
            return new ALTDocPrintableStyled( altDoc, listener, pageConfig );
        else {

            if( altDoc.getContentMode() == ALTDoc.AltDocContentMode.PLAIN_WITH_HEADER )
            {
                final StyleState baseStyle
                    = PlainHeaderStyleReader
                        .readHeader (
                            altDoc.getAltFile(),
                            altDoc.getCharset(),
                            AltSettings.INSTANCE().commandDict(),
                            altDoc.getContentOffset()
                        );

                return new ALTDocPrintablePlain( altDoc, listener, pageConfig, baseStyle );
            }
            return new ALTDocPrintablePlain( altDoc, listener, pageConfig, AltSettings.INSTANCE().commandDict().getInitCommand().toStyleState() );
        }
    }

}
