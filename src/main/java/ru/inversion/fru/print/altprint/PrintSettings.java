package ru.inversion.fru.print.altprint;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;

public class PrintSettings
{
    private Copies copies;
    private OrientationRequested orientation;
    private MediaPrintableArea printableArea;

    private final PrintSettings defaultSettings;

    public PrintSettings( ) {
        this( null );
        copies        = new Copies(1);
        orientation   = OrientationRequested.PORTRAIT;
        printableArea = new MediaPrintableArea(10,10,10,10,1000);
    }

    public PrintSettings( PrintSettings defaultSettings) {
        this.defaultSettings = defaultSettings;
    }

    public void setOrientation( OrientationRequested orientation)
    {
        this.orientation = orientation;
    }
    public OrientationRequested getOrientation()
    {
        return this.orientation == null ? defaultSettings.orientation : orientation;
    }

    public void setCopies(int copies)
    {
        this.copies = new Copies(copies);
    }
    public void setCopies(Copies copies)
    {
        this.copies = copies;
    }

    public Copies getCopies()
    {
        return this.copies == null ? defaultSettings.copies : copies;
    }

    public MediaPrintableArea getPrintableArea()
    {
        return this.printableArea == null ? defaultSettings.printableArea : this.printableArea;
    }
    public void setPrintableArea( MediaPrintableArea a )
    {
        this.printableArea = a;
    }
}
