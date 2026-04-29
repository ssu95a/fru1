package ru.inversion.fru.parser;

import ru.inversion.utils.Holder;
import ru.inversion.utils.S;
import ru.inversion.utils.parser.ITokenHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/** */
public final class FruEntryProbe {

   private FruEntryProbe() {
   }

   /** */
   public static boolean hasExplicitEntry( Path fruFile, Charset charset ) throws IOException {

      if( fruFile == null )
            return false;

      final Holder<Boolean> hasEntry = new Holder<>(false);

      Predicate<String> finder = new Predicate<String>() {
         @Override
         public boolean test( String s) {

            if( S.isNullOrEmpty(s) )
               return false;

            if( s.charAt(0) == '#')
            {
               if(s.matches("^#entry\\b.*")) {
                  hasEntry.set(true);
                  return true;
               }

               // если нашли другие секции, выходим
               return s.length() > 1 && ITokenHandler.isAlpha(s.charAt(1));

            }
            return false;
         }
      };

      try( BufferedReader br =  Files.newBufferedReader(fruFile, charset) ) {
           br.lines().anyMatch( finder );
      }

      return hasEntry.get();
   }
}