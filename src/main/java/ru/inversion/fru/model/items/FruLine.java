package ru.inversion.fru.model.items;

import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.utils.S;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** */
public class FruLine extends FruItem {

    /** */
    final private List<? extends FruItem> items;

    /** */
    public FruLine( List<FruItem> items ) {
        this.items = items;
    }

    /** */
    public List<? extends FruItem> getItems( ) {
        return items;
    }

    /*
    public void collectFieldLengths(Map<Integer,List<Integer>> fieldLengths )
    {
        items.stream()
            .filter(fruItem -> fruItem instanceof FruFieldVal && ((FruFieldVal)fruItem).hasFieldSplit() )
                .map( i->(FruFieldVal)i )
                    .forEach(new Consumer<FruFieldVal>() {
                        @Override
                        public void accept( FruFieldVal fruField ) {
                            fieldLengths.computeIfAbsent( fruField.getValIndex(), ArrayList::new ).add( fruField.getWidth() );
                        }
                    });
    }
    */

    /** */
    public static final Pattern FORMAT_CALL_PATTERN = Pattern.compile ("^(?<name>\\w+)\\s*\\(\\s*(?<fields>\\w+(?:\\s*,\\s*\\w+)*\\s*)?\\)\\s*(?<flags>(?:/\\w+)*)$");

    /** */
    private static FruItem makeItem( FruBuilder fruBuilder, String fieldStr, Function<String,Integer> dataIndex )
    {
        final Matcher matcher = FORMAT_CALL_PATTERN.matcher( fieldStr );

        if( matcher.matches() )
        {
            String name   = matcher.group("name"  );
            String fields = matcher.group("fields");
            String flags  = matcher.group("flags" );

            final FruFormat fruFormat = fruBuilder.formats.get( name );

            if( fruFormat != null )
            {
                final List<String> fl = Arrays.stream(fields.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                final List<FruField> fieldList = new ArrayList<>( fl.size() );

                final AtomicInteger fldIndex = new AtomicInteger(0);
                fl.forEach( fs->fieldList.add( FruField.make( fruBuilder, fs, fruFormat.getFormatter(fldIndex.getAndIncrement()), dataIndex.apply(fs) ) ) );

                return new FruFormatCall( fruFormat, fieldList, S.isNullOrEmpty(flags) ? null : FruFormatter.make(flags) );
            }

            throw new IllegalStateException( "Формат с именем '" + name + "' не найден в форме!" );
        }
        else
        {
            int index = fieldStr.indexOf('/');
            if( index == -1 )
                return FruField.make( fruBuilder, fieldStr.trim(), null, dataIndex.apply(fieldStr.trim()) );

            return FruField.make( fruBuilder, fieldStr.substring( 0, index ).trim(), FruFormatter.make( fieldStr.substring(index) ), dataIndex.apply( fieldStr.substring( 0, index ).trim() ) );
        }
    }


    /** */
    public static FruLine make( FruBuilder fruBuilder, String line, Function<String,Integer> dataIndex )
    {
        final LinkedList<FruItem> result = new LinkedList<>();
        StringBuilder sb = new StringBuilder();
        boolean insideAt = false;


        for( int i = 0; i < line.length(); i++ )
        {
            char ch = line.charAt(i);

            if( ch == '@' )
            {
                if( sb.length() > 0 )
                {
                    if( insideAt ) {
                        String fieldStr = sb.toString();
                        result.add( makeItem( fruBuilder, fieldStr, dataIndex ) );
                    }
                    else
                        result.add( new FruText(sb.toString()) );

                    sb.setLength(0);
                }

                insideAt = !insideAt;
            }
            else
            if( ch != '\n' )
                sb.append(ch);
        }

        // Добавляем последний элемент
        if( sb.length() > 0 )
        {
            if( insideAt ) {
                String fieldStr = sb.toString();
                result.add( makeItem( fruBuilder, fieldStr, dataIndex ) );
            }
            else
                result.add( new FruText( sb.toString() ) );
        }

        return new FruLine( result );
    }
}
