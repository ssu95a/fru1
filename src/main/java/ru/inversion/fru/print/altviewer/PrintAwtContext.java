package ru.inversion.fru.print.altviewer;

import javafx.stage.Window;
import ru.inversion.fru.print.altprint.doc.ALTDoc;

import javax.print.PrintService;

/** */
public class PrintAwtContext {

    final private PrintService awtPrinter;

    final private boolean matrixPrinter;

    final private ALTDoc altDoc;

    final private Window window;

    public PrintAwtContext( PrintService awtPrinter, boolean matrixPrinter, ALTDoc altDoc, Window window )
    {
        this.awtPrinter    = awtPrinter;
        this.matrixPrinter = matrixPrinter;
        this.altDoc        = altDoc;
        this.window        = window;
    }

    public PrintService getAwtPrinter() {
        return awtPrinter;
    }

    public boolean isMatrixPrinter() {
        return matrixPrinter;
    }

    public ALTDoc getAltDoc() {
        return altDoc;
    }

    public Window getWindow() {
        return window;
    }
}
