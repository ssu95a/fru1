package ru.inversion.fru.model.sections;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.utils.constants.SectionTypeEnum;

import java.util.LinkedList;
import java.util.List;

/** */
public abstract class FruSection extends FruItem {

    /** */
    final private int num;

    /** Содержимое */
    final protected List<FruLine> lines = new LinkedList<>();

    /** */
    private int useCount = 0;

    /** */
    protected FruSection( int num ) {
        this.num = num;
    }

    /** */
    public void addLine( FruLine line )
    {
        lines.add( line );
    }

    /** */
    public int getNum( ) {
        return num;
    }

    /** */
    public List<FruLine> getLines() {
        return lines;
    }

    /** */
    public abstract SectionTypeEnum getType();

    /** */
    public int getUseCount() {
        return useCount;
    }

    /** */
    public void incrementUse() {
        useCount++;
    }

    /** */
    public int getFieldNum(String name ) { return -1; }

    /** */
    public void beforeUse(FruContext context)
    { }

    /** */
    public void afterUse(FruContext context)
    { }

    /** */
    public Object getFieldValue(FruContext fruContext, String name) {
        return null;
    }
}
