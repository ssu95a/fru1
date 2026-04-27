package ru.inversion.fru.print.naltprn.cmd;

import java.io.ByteArrayOutputStream;

/** */
public final class MatrixCommandBytes {

   private MatrixCommandBytes() {
   }

   public static byte[] compile( String source ) {

      if( source == null || source.isEmpty() )
          return new byte[0];

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      for( int i = 0; i < source.length(); )
      {
          char ch = source.charAt(i);

         if (ch != '\\') {
            writeByte( out, ch, source );
            i++;
            continue;
         }

         if( i + 1 >= source.length() )
            throw new IllegalArgumentException("Незавершенная esc команда: " + source );

         char kind = source.charAt(i + 1);

         if (kind == 'd')
         {
            int start = i + 2;
            int end   = start;

            while (end < source.length() && Character.isDigit(source.charAt(end))) {
               end++;
            }

            if( end == start )
               throw new IllegalArgumentException("Пропущенный 'decimal byte' после \\d в: " + source);

            int value = Integer.parseInt(source.substring(start, end));
            writeByte(out, value, source);
            i = end;
            continue;
         }

         if( kind == 'x')
         {
            int start = i + 2;
            int end   = start + 2;

            if( end > source.length() )
               throw new IllegalArgumentException( "Пропущенный 'two hex digits' после \\x в: " + source);

            int value = Integer.parseInt(source.substring(start, end), 16);
            writeByte(out, value, source);
            i = end;
            continue;
         }

         if (kind == 'c')
         {
            int pos = i + 2;

            if( pos >= source.length() )
               throw new IllegalArgumentException("Пропущен символ после \\c в: " + source);

            writeByte(out, source.charAt(pos), source);
            i = pos + 1;
            continue;
         }

         throw new IllegalArgumentException("Неизвестная esc команда \\" + kind + " в строке command: " + source);
      }

      return out.toByteArray();
   }

   /** */
   private static void writeByte( ByteArrayOutputStream out, int value, String source )
   {
      if( value < 0 || value > 255 )
         throw new IllegalArgumentException( "Byte value out of range 0..255: " + value + " in command: " + source );

      out.write(value);
   }
}