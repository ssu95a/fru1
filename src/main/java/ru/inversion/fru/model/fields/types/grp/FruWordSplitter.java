package ru.inversion.fru.model.fields.types.grp;

import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

public final class FruWordSplitter {

   private FruWordSplitter() {
   }

   public static Pair<String, String> split(String value, int maxChars)
   {

      if( value == null )
          value = S.EMPTY_STRING;

      if( maxChars <= 0 )
         return Pair.makePair( S.EMPTY_STRING, value );

      value = value.trim();

      if( value.length() <= maxChars )
          return Pair.makePair( value, S.EMPTY_STRING );

      int splitIndex = -1;

      int limit = Math.min(maxChars, value.length() - 1);

      for( int i = 0; i <= limit; i++ )
      {
         char ch = value.charAt(i);

         if (ch == '\n' || ch == '\r') {
            splitIndex = i;
            break;
         }

         if( Character.isWhitespace(ch) )
             splitIndex = i;
      }

      String head;
      String tail;

      if( splitIndex < 0 )
      {
         head = value.substring(0, maxChars);
         tail = value.substring(maxChars   );
      }
      else
      {
         head = value.substring( 0, splitIndex );

         int tailStart = splitIndex + 1;

         while (tailStart < value.length()
                 && Character.isWhitespace(value.charAt(tailStart))) {
            tailStart++;
         }

         tail = tailStart < value.length() ? value.substring(tailStart) : S.EMPTY_STRING;
      }

      return Pair.makePair(head, tail);
   }
}