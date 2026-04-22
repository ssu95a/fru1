package ru.inversion.fru.data;

import ru.inversion.property.Property;
import ru.inversion.utils.Pair;

import java.util.*;

import java.util.stream.IntStream;

/** */
public class FruData {

    final private FruDataFile dataFile;

    final private Property<FruDataRow> rowProperty = new Property<>( new FruDataRow( -1, Collections.emptyList()) );

    private List<String> cacheRow;

    /** */
    public FruData( FruDataFile dataFile ) {
        this.dataFile = dataFile;
    }

    /** */
    public Property<FruDataRow> rowProperty() {
        return rowProperty;
    }

    /** */
    public int currentSectionNum( )
    {
        return rowProperty.getValue().getSectionNum();
    }

    /** */
    public FruDataRow currentRow( )
    {
        return rowProperty.getValue();
    }

    /** */
    public void next()
    {
        if( cacheRow != null )
        {
            List<String> cacheTmp = cacheRow;
            cacheRow = null;
            rowProperty.setValue( new FruDataRow( currentSectionNum(), cacheTmp ) );
        }
        else
        {
            Pair<Integer, List<String>> p = dataFile.next();
            rowProperty.setValue( new FruDataRow( p.first, p.second ) );
        }
    }

    /** */
    public void put2CacheRow( String value, int valIndex )
    {
        if( cacheRow == null ) {
            cacheRow = new ArrayList<>();
            IntStream.range( 0, rowProperty.getValue().data().size() ).forEach(i->cacheRow.add(null) );
        }
        cacheRow.set( valIndex, value );
    }

    /** */
    public boolean isCacheFilled()
    {
        return cacheRow != null;
    }

    /** */
    public boolean eof()
    {
        return /*cacheRow == null && */ !dataFile.hasNext( );
    }
}
