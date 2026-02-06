package ru.inversion.fru.print.altprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.doc.ALTDocPrintable;
import ru.inversion.fru.print.naltprn.AltSettings;

import javax.print.*;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.Optional;

/** */
public class AltPrinter {

    private static final Logger log = LoggerFactory.getLogger(AltPrinter.class);

    /** */
    public AltPrinter( ) {
    }

    /** */
    public void print( ALTDoc doc, IAltPrintListener listener ) throws Exception {

        final PrintService awtPrinter =
            findAWTPrinterByIndex( FruEngineConfig.instance().getPrinterIndex() )
                .orElseThrow(
                    ()->new ALTPrintException("Невозможно определить принтер по переданному индексу: " + FruEngineConfig.instance().getPrinterIndex() )
                );

        final boolean matrix = isMatrix(awtPrinter);

        ALTDocPrintable printable = doc.makePrintable( listener, null );

        log.info( "Печать на принтер: {}, матричный: {}", awtPrinter.getName(), ( matrix ? " Y" : "N" ));

        if( matrix ) {
            printable.printToMatrix( awtPrinter );
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();

        PageFormat pf = job.defaultPage();
        pf.setOrientation( decodeOrientation( doc.getOrientation()) );

        job.setJobName( "ALT: " + doc.getAltFile() );
        job.setCopies ( doc.getCopies().getValue() );
        job.setPrintService(awtPrinter);
        job.setPrintable(printable, pf);

        job.print();
    }

    /** */
    private static int decodeOrientation(OrientationRequested o) {
        return o == OrientationRequested.LANDSCAPE ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT;
    }

    /** */
    public static boolean isMatrix( PrintService awtPrinter )
    {
        return AltSettings.INSTANCE().isMatrixPrinter( awtPrinter.getName() );
    }

    /**
     *
     */
    public static Optional<PrintService> findAWTPrinterByName(String fxPrinterName)
    {
        final PrintService[] services = PrintServiceLookup.lookupPrintServices( null, null );

        for (PrintService service : services) {
            if( service.getName().equals(fxPrinterName) ) {
                return Optional.of(service);
            }
        }
        return Optional.empty();
    }

    /** */
    public static Optional<PrintService> findAWTPrinterByIndex(int index ) {

        final PrintService[] services = PrintServiceLookup.lookupPrintServices( null, null );

        if( index < 0 || index >= services.length )
        {
            ALTLog.error( String.format("Ошибочное значение заданного индекса принтера - %d, всего доступно принтеров %d", index, services.length ) );
            ALTLog.warning("Будет использован принтер по умолчанию");

            return Optional.ofNullable(PrintServiceLookup.lookupDefaultPrintService());
        }

        return Optional.of(services[index]);
    }

}