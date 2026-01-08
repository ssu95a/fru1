package ru.inversion.fru.print.altprint.doc;

import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.IAltPrintListener;
import ru.inversion.fru.print.altprint.doc.formatted.ALTDocPrintableStyled;
import ru.inversion.fru.print.altprint.doc.formatted.StyleState;
import ru.inversion.fru.print.naltprn.AltSettings;

import javax.print.PrintException;
import javax.print.PrintService;
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

    /** */
    public abstract void printToMatrix( PrintService printer ) throws PrintException;

    /** */
    protected void finishPrint() {
        if( listener != null ) {
            listener.onEndPrint();
            listener = null;
        }
    }

    /** */
    public static ALTDocPrintable load( ALTDoc altDoc, IAltPrintListener listener, AltPrintPageConfig pageConfig ) throws IOException {

        if( altDoc.getContentState() == -1 )
            return new ALTDocPrintableStyled( altDoc, listener, pageConfig );
        else {

            if( altDoc.getContentState() > 0)
            {
                StyleState baseStyle = PlainHeaderStyleReader.readHeader( altDoc.getAltFile(), altDoc.getCharset(), AltSettings.INSTANCE().commandDict(), altDoc.getContentState() );
                return new ALTDocPrintablePlain( altDoc, listener, pageConfig, baseStyle );
            }

            return new ALTDocPrintablePlain(altDoc, listener, pageConfig, AltSettings.INSTANCE().commandDict().getInitCommand().toStyleState() );
        }
    }

    //INIT=Name Font=Courier New;Size Font=10;Italic=No;Bold=No;Under=No;Orientation=Portrait;Left=0;Set Copies=1;Cmd=`INTERVAL_6`;

    /** */
    private static StyleState defaultPlainStyle() {

        return new StyleState( "Monospaced", 10, Font.PLAIN, false, 0.0f, 0.5f );
    }

}
