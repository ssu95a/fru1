package ru.inversion.fru.print.altviewer;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.inversion.fru.print.altprint.AltPrinter;
import ru.inversion.utils.MemoryURL;
import ru.inversion.fru.print.altprint.doc.ALTDoc;

import java.net.URL;
import java.util.Objects;

/** */
public class FruApp extends Application {

    private static ALTDoc altDoc;
    private static AltPrinter altPrinter;

    @Override
    public void init() {

        try {
            URL.setURLStreamHandlerFactory(protocol ->"memory".equals(protocol) ? new MemoryURL.MemoryStreamHandler() : null );
        } catch( Throwable th ) {
            System.out.println("URLStreamHandlerFactory already registered");
        }
    }

    /** */
    @Override
    public void start( Stage stage )
    {
        try {
            FruViewController.showViewer( stage, altPrinter, altDoc );
        }
        catch( Throwable th ) {
            th.printStackTrace();
        }
    }


    /** */
    public static void run( AltPrinter altPrinter, ALTDoc altDoc )
    {
        Objects.requireNonNull( altDoc, "'altDoc' is null" );
        Objects.requireNonNull( altPrinter, "'altPrinter' is null" );

        new Thread( () -> {
            FruApp.altDoc = altDoc;
            FruApp.altPrinter = altPrinter;
            launch();
        }).start();
    }

}
