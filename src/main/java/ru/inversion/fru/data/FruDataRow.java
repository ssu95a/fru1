package ru.inversion.fru.data;

import ru.inversion.fru.utils.FruUtils;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

import java.util.*;

public class FruDataRow {

    private final int sectionNum;
    private final List<String> data;
    private Map< Integer, String > splitValues = null;

    /** */
    public FruDataRow( int sectionNum, List<String> data )
    {
        this.sectionNum = sectionNum;
        this.data = data != null ? new ArrayList<>(data) : new ArrayList<>();
    }

    public int getSectionNum() {
        return sectionNum;
    }

    /** */
    public List<String> data() {
        return data;
    }

    /** */
    public String getValue( int valIndex, int length )
    {
        String value;

        if( splitValues == null || !splitValues.containsKey(valIndex) )
        {
            value = getValue(valIndex);

            if( !S.isNullOrEmpty(value) ) {

                Pair<String, String> p = FruUtils.splitString(value, length);
                value = p.first;

                if( splitValues == null )
                    splitValues = new TreeMap<>();

                splitValues.put( valIndex, p.second );
            }
        }
        else
        {
            Pair<String, String> p = FruUtils.splitString( splitValues.get( valIndex ), length );
            value = p.first;
            splitValues.put( valIndex, p.second );
        }
        return value;
    }

    /** */
    public String getValue( int valIndex )
    {

        if( splitValues == null || !splitValues.containsKey( valIndex ) )
            return valIndex < data.size() ? data.get(valIndex) : null;

        try {
            return splitValues.get( valIndex );
        }
        finally {
            splitValues.remove(valIndex);
        }
    }

    /** */
    @Override
    public String toString() {
        return String.format("FruDataRow[section=%d, data=%s, splits=%s]", sectionNum, data, splitValues );
    }
}