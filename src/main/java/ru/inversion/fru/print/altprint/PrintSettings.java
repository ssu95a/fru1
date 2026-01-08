package ru.inversion.fru.print.altprint;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;

public class PrintSettings
{
    private Copies               copies;
    private OrientationRequested orientation;
    private MediaPrintableArea   printableArea;

    public PrintSettings( ) {
    }

    public void setOrientation(OrientationRequested o) { this.orientation = o; }
    public void setCopies(int c) { this.copies = new Copies(c); }
    public void setCopies(Copies c) { this.copies = c; }
    public void setPrintableArea(MediaPrintableArea a) { this.printableArea = a; }

    public Copies resolveCopies( Copies def) {
        return copies != null ? copies : def;
    }
    public OrientationRequested resolveOrientation(OrientationRequested def) {
        return orientation != null ? orientation : def;
    }
    public MediaPrintableArea resolvePrintableArea(MediaPrintableArea def) {
        return printableArea != null ? printableArea : def;
    }

    public Copies getCopies() {
        return copies;
    }

    public OrientationRequested getOrientation() {
        return orientation;
    }

    public MediaPrintableArea getPrintableArea() {
        return printableArea;
    }
}
