package ru.inversion.fru.print.altviewer;

import javafx.stage.Window;
import ru.inversion.fru.print.altprint.doc.ALTDoc;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

/** */
public class PrintAwtContext {

    final private PrintService awtPrinter;

    final private boolean matrixPrinter;

    final private ALTDoc altDoc;

    final private Window window;

    private final PrintRequestAttributeSet attributes;

    public PrintAwtContext( PrintService awtPrinter, boolean matrixPrinter, ALTDoc altDoc, Window window, PrintRequestAttributeSet attributes )
    {
        this.awtPrinter    = awtPrinter;
        this.matrixPrinter = matrixPrinter;
        this.altDoc        = altDoc;
        this.window        = window;
        this.attributes    =
                           attributes == null
                                ? new HashPrintRequestAttributeSet()
                                : new HashPrintRequestAttributeSet(attributes);
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

    /** */
    public PrintRequestAttributeSet getAttributes() {
        return attributes;
    }
}
