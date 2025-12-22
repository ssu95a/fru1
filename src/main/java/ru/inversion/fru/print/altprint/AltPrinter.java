package ru.inversion.fru.print.altprint;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.stage.Window;
import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.fru.print.altviewer.FruViewController;
import ru.inversion.fru.print.altviewer.PrintAwtContext;
import ru.inversion.fru.print.altviewer.PrintableTask;

import javax.print.*;

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
                return service;
            }
        }
        return null;
    }

    /** */
    private PrintService findAWTPrinterByIndex(int index ) {

        final PrintService[] services = PrintServiceLookup.lookupPrintServices( null, null );

        if( index < 0 || index >= services.length )
        {
            ALTLog.error( String.format("Ошибочное значение заданного индекса принтера - %d, всего доступно принтеров %d", index, services.length ) );
            ALTLog.warning("Будет использован принтер по умолчанию");

            return PrintServiceLookup.lookupDefaultPrintService();
        }

        return services[index];
    }


    /** */
    public void print( Window window, ALTDoc altDoc )
    {
        try {

            javafx.print.PrinterJob fxJob = javafx.print.PrinterJob.createPrinterJob();
            if( fxJob == null )
                throw new IllegalStateException("Не удалось инициализировать печать");

            fxJob.getJobSettings().setJobName( altDoc.getAltFile().toString() );

            if( !fxJob.showPrintDialog(window) ) {
                return;
            }

            PrintService awtPrinter = findAWTPrinterByName( fxJob.getPrinter().getName() );

            if( awtPrinter == null ) {

                awtPrinter = findAWTPrinterByIndex(engineConfig.getPrinterIndex());

                if( awtPrinter == null )
                    throw new IllegalStateException("Невозможно определить принтер для печати");
            }

            final PrintAwtContext context = new PrintAwtContext( awtPrinter, isMatrix(awtPrinter), altDoc, window );

            final PrintableTask printTask = new PrintableTask( context );

            printTask.setOnFailed( new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle( WorkerStateEvent event ) {
                    FruViewController.handleException( window, printTask.getException() );
                }
            });

            Thread thread = new Thread(printTask);
            thread.setDaemon(true);
            thread.start();
        }
        catch( Throwable th ) {
            throw new ALTPrintException( "Ошибка при печати отчета", th );
        }
    }

    /** */
    private boolean isMatrix( PrintService awtPrinter )
    {
        return ALTSettings.INSTANCE().isMatrixPrinter(awtPrinter.getName());
    }

}