package ru.inversion.fru.model.sections;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.utils.constants.SectionTypeEnum;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** */
public class FruSectionTable extends FruSection {

    /** */
    final private Map<String,Integer> fieldMap;

    /** */
    private FruSectionHeader header;

    /** */
    private FruSectionTail tail;

    /** */
    private FruSectionLine tableLine;

    /** */
    public FruSectionTable(int num, List<String> fieldList) {
        super(num);
        AtomicInteger di = new AtomicInteger(0);
        this.fieldMap = fieldList.stream().collect( Collectors.toMap(s -> s, s -> di.getAndIncrement())  );
    }

    public FruSectionHeader getHeader() {
        return header;
    }
    public void setHeader(FruSectionHeader header) {
        this.header = header;
    }

    public FruSectionTail getTail() {
        return tail;
    }
    public void setTail(FruSectionTail tail) {
        this.tail = tail;
    }

    public FruSectionLine getLine() {
        return tableLine;
    }
    public void setLine(FruSectionLine tableLine) {
        this.tableLine = tableLine;
    }


    @Override
    public SectionTypeEnum getType() {
        return SectionTypeEnum.TABLE;
    }

    /** */
    @Override
    public void beforeUse( FruContext context ) {

        if( getHeader() != null )
            context.renderers().get( FruSectionHeader.class ).render( context, getHeader() );
    }

    /** */
    @Override
    public void afterUse( FruContext context ) {

        if( getTail() != null )
            context.renderers().get( FruSectionTail.class ).render( context, getTail() );
    }

    /** */
    @Override
    public int getFieldNum( String name )
    {
        return fieldMap.getOrDefault( name, -1 );
    }

    /** */
    public String getFieldValue( FruContext context, String name )
    {
        int index = getFieldNum(name);

        if( index == -1 )
            return null;

        return context.data().currentRow().data().get( index );
    }

    static final public String PLACEHOLDER_VALUE = String.valueOf(' ');

    /** */
    public List<String> makePlaceholderRow ()
    {
        List<String> placeholderRow = new ArrayList<>(fieldMap.size());
        for( int i = 0; i < fieldMap.size(); i++ )
             placeholderRow.add(PLACEHOLDER_VALUE);

        return placeholderRow;
    }

}
