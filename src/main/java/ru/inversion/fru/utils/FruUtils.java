package ru.inversion.fru.utils;

import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** */
public class FruUtils {


    /** */
    public static Deque<String> splitString( String text, List<Integer> lengths )
    {
        final Deque<String> result = new ArrayDeque<>();

        if( text == null || lengths == null || lengths.isEmpty() )
            return result;

        if( text.length() <= lengths.get(0) )
        {
            result.add(text);
            return result;
        }

        final String[] words = text.split("\\s+");
        int wordIndex = 0;

        for( int targetLength : lengths )
        {
            if( wordIndex >= words.length )
                break;

            StringBuilder currentLine = new StringBuilder();

            // Добавляем слова пока не достигнем целевой длины
            while( wordIndex < words.length )
            {
                String word = words[wordIndex];

                // Проверяем, поместится ли слово в текущую строку
                if (currentLine.length() == 0) {
                    // Первое слово в строке
                    if (word.length() <= targetLength)
                    {
                        currentLine.append(word);
                        wordIndex++;
                    }
                    else
                    {
                        // Слово длиннее целевой длины - разбиваем принудительно
                        currentLine.append( word, 0, targetLength );
                        words[wordIndex] =  word.substring(targetLength);
                        break;
                    }
                }
                else
                {
                    // Не первое слово - проверяем с учетом пробела
                    if( currentLine.length() + 1 + word.length() <= targetLength ) {
                        currentLine.append(" ").append(word);
                        wordIndex++;
                    } else {
                        // Слово не помещается - оставляем для следующей строки
                        break;
                    }
                }

                // Если достигли точной длины - выходим
                if( currentLine.length() >= targetLength) {
                    break;
                }
            }

            result.add( currentLine.toString() );
        }

        // Добавляем оставшиеся слова
        if( wordIndex < words.length )
        {
            StringBuilder remainder = new StringBuilder();
            for (int i = wordIndex; i < words.length; i++) {
                if (remainder.length() > 0) remainder.append(" ");
                remainder.append(words[i]);
            }
            result.add(remainder.toString());
        }
        return result;
    }

    /** */
    public static Pair< String, String > splitString( String text, int length )
    {
        if( S.isNullOrEmpty(text) || length >= text.length() )
            return Pair.makePair( text, null );

        int lastWSIndex = 0;

        for( int i = 0; i < length; i++ )
        {
            if( text.charAt(i) == ' ' )
                lastWSIndex = i;
        }

        if( lastWSIndex == 0 )
            lastWSIndex = length - 1;

        return Pair.makePair( text.substring( 0 ,lastWSIndex ), text.substring( lastWSIndex + 1 ) );
    }
}
