package ru.inversion.fru.print.altprint;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;

public class DefSettings
{
    private Copies copies = new Copies(1);

    private OrientationRequested orientation = OrientationRequested.PORTRAIT;
    private MediaPrintableArea printableArea = new MediaPrintableArea(10, 10, 10, 10, 1000);

    public void setOrientation(OrientationRequested orientation)
    {
        this.orientation = orientation;
    }
    public OrientationRequested getOrientation()
    {
        return this.orientation;
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
        return this.copies;
    }

    public MediaPrintableArea getPrintableArea()
    {
        return this.printableArea;
    }
}
