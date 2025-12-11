package ru.inversion.fru.print.altprint;

import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.utils.U;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;

/** */
public class AltPrinter {

    final private FruEngineConfig engineConfig;

    /** */
    public AltPrinter( FruEngineConfig ec ) {
        engineConfig = ec;
    }

    /** */
    private PrintService findAWTPrinterByName(String fxPrinterName)
    {
        final PrintService[] services = PrintServiceLookup.lookupPrintServices( null, null );

        for (PrintService service : services) {
            if( service.getName().equals(fxPrinterName) ) {
                return service; //PrinterJob.createPrinterJob();
            }
        }
        return null;
    }

    /** */
    private PrintService findAWTPrinterByIndex(int index ) {

        final PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        if( index < 0 || index >= services.length )
        {
            ALTLog.error( String.format("Ошибочное значение заданного индекса принтера - %d, всего доступно принтеров %d", index, services.length ) );
            ALTLog.warning("Будет использован принтер по умолчанию");

            return PrintServiceLookup.lookupDefaultPrintService();
        }

        return services[index];
    }

    /** */
    public void print( ALTDoc altDoc ) {

        try {

            PrintService awtPrinter = findAWTPrinterByIndex( engineConfig.getPrinterIndex() );

            if( awtPrinter == null )
                throw new IllegalStateException("Невозможно определить принтер для печати");

            PrinterJob awtJob = PrinterJob.getPrinterJob( );

            PageFormat pageFormat = awtJob.defaultPage();
            if(
                ( pageFormat.getOrientation() == 1 && altDoc.getOrientation() == OrientationRequested.PORTRAIT )
                ||
                ( pageFormat.getOrientation() == 0 && altDoc.getOrientation() == OrientationRequested.LANDSCAPE)
            )
                ;
            else
                pageFormat.setOrientation( U.decode( altDoc.getOrientation(), OrientationRequested.LANDSCAPE, 0, 1) );

            awtJob.setJobName( "ALT: " + altDoc.getAltFile() );
            awtJob.setCopies ( altDoc.getCopies().getValue() );
            awtJob.setPrintService( awtPrinter );
            awtJob.setPrintable   ( altDoc.makePrintable(), pageFormat );

            awtJob.print( );
        }
        catch( Throwable th ) {
            throw new ALTPrintException("Ошибка при печати отчета", th );
        }
    }

    /** */
    public void saveTo( ) {
    }
}