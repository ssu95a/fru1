package ru.inversion.fru.model.items;

import ru.inversion.fru.model.formats.AlignEnum;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.exceptions.FruItemBadValueException;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Простановка номеров страниц  */
public class FruPaging extends FruItem {

    final private AlignEnum    align;
    final private boolean      top;
    final private boolean      firstOff;
    final private String       pageEnd;
    final private FruFormatter formatter;
    final private int          lines;

    /** */
    public FruPaging( AlignEnum align, boolean top, boolean firstOff, String pageEnd, FruFormatter formatter, int lines)
    {
        this.align    = align;
        this.top      = top;
        this.firstOff = firstOff;
        this.pageEnd  = pageEnd;
        this.formatter= formatter;
        this.lines    = lines;
    }

    /** */
    public int getLines() {
        return lines;
    }

    /** */
    public AlignEnum getAlign() {
        return align;
    }

    /** */
    public boolean isTop() {
        return top;
    }

    /** */
    public boolean isBottom() {
        return !top;
    }

    /** */
    public boolean isFirstOff() {
        return firstOff;
    }

    /** */
    public String getPageEnd( ) {
        return pageEnd;
    }

    /** */
    public FruFormatter getFormatter( ) {
        return formatter;
    }

    /** */
    public static Pair<Boolean,AlignEnum> parsePaging( String pagingStr )
    {
        if( S.isNullOrEmpty(pagingStr) )
            return null;

        final List<String> items = Arrays.stream( pagingStr.split("/") ).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        boolean top;

        switch( items.get(0) ) {
            case "up"  : top = true;  break;
            case "down": top = false; break;
            default:
                throw new FruItemBadValueException("Ошибочное значение параметра 'paging': " + items.get(0) );
        }

        AlignEnum align;

        if( items.size() > 1 && !items.get(1).isEmpty() )
            align = AlignEnum.of(items.get(1).charAt(0) );
        else
            align = AlignEnum.Center;

        return Pair.makePair( top, align );
    }
}
