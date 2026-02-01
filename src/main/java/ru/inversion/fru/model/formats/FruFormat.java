package ru.inversion.fru.model.formats;

import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruText;
import ru.inversion.utils.S;

import java.util.LinkedList;
import java.util.List;

public class FruFormat extends FruItem {

    /** */
    final private List<FruItem> items;

    /** */
    public FruFormat( List<FruItem> items ) {
        this.items = items;
    }

    /** */
    public List<FruItem> getItems() {
        return items;
    }

    /** */
    public FruFormatter getFormatter( int num )
    {
        int j = 0;

        for( FruItem item : items )
        {
            if (item instanceof FruFormatter)
                if (num == j)
                    return (FruFormatter) item;
                else
                    j++;
        }

        return null;
    }

    /** */
    public static FruFormat make( String s )
    {
        final LinkedList<FruItem> result = new LinkedList<>();
        final StringBuilder current = new StringBuilder();
        boolean insideAt = false;

        for( int i = 0; i < s.length(); i++ )
        {
            char ch = s.charAt(i);

            if( ch == '@' )
            {
                if( current.length() > 0 )
                {
                    if( insideAt )
                        result.add( FruFormatter.make( current.toString() ) );
                    else
                        result.add( new FruText( current.toString() ) );

                    current.setLength(0);
                }

                insideAt = !insideAt;
            }
            else
                current.append(ch);
        }

        // Добавляем последний элемент
        if( current.length() > 0 ) {
            if( insideAt )
                result.add( FruFormatter.make( current.toString() ) );
            else
                result.add( new FruText( current.toString() ) );
        }

        final FruItem last = result.getLast();

        if( last instanceof FruText )
        {
            FruText t = (FruText)last;

            if(S.isNullOrEmpty( t.getText() ) || t.getText().length() == 1 && S.lastChar(t.getText()) == '\n' )
                result.removeLast();
        }

        return new FruFormat( result );
    }
}
