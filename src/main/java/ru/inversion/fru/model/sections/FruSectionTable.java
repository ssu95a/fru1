package ru.inversion.fru.model.sections;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.utils.U;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    private Map<Integer,List<Integer>> fieldLengths = null;

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

    @Override
    public SectionTypeEnum getType() {
        return SectionTypeEnum.TABLE;
    }

    /** */
    @Override
    public void beforeUse(FruContext context) {
    }

    /** */
    @Override
    public void afterUse(FruContext context) {
    }

    /** */
    @Override
    public int getFieldNum( String name )
    {
        return fieldMap.getOrDefault( name, -1 );
    }

    /* */
    public String getFieldValue( FruContext context, String name )
    {
        int index = getFieldNum(name);

        if( index == -1 )
            return null;

        return context.data().currentRow().data().get( index );
    }

    /** */
    public void linkFields( ) {

        final Map<Integer,List<Integer>> fl = new TreeMap<>();

        lines.forEach(l-> l.collectFieldLengths(fl));

        if(!fl.isEmpty() )
            fieldLengths = fl;
    }

    public Map<Integer, List<Integer>> getFieldLengths() {
        return fieldLengths;
    }
}
