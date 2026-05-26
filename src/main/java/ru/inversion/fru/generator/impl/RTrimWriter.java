package ru.inversion.fru.generator.impl;

import ru.inversion.utils.S;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/** Обрезает все пробелы справа до \n при выводе текста */
public final class RTrimWriter extends FilterWriter {

   /** */
   public interface WrittenCharListener {
      void written(char ch) throws IOException;
   }

   private final StringBuilder lineBuffer = new StringBuilder(256);

   private int physicalLine      = 1;
   private int currentCharInLine = 1;
   private int totalChars        = 0;

   private final WrittenCharListener listener;

   public RTrimWriter( Writer out, WrittenCharListener listener ) {
      super(out);
      this.listener = listener;
   }

   /**
    * Физическая строка output-а после rtrim.
    * Не page-local.
    */
   public int physicalLine() {
      return physicalLine;
   }

   public int currentCharInLine() {
      return currentCharInLine;
   }

   public int totalChars() {
      return totalChars;
   }

   @Override
   public void write(int c) throws IOException {
      writeChar((char) c, true);
   }

   @Override
   public void write(char[] cbuf, int off, int len) throws IOException {

      if( cbuf == null )
          throw new NullPointerException("cbuf is null");

      if (off < 0 || len < 0 || off + len > cbuf.length)
         throw new IndexOutOfBoundsException();

      for (int i = off; i < off + len; i++)
         writeChar(cbuf[i], true);
   }

   @Override
   public void write(String str, int off, int len) throws IOException {

      if (str == null)
         throw new NullPointerException("str is null");

      if( off < 0 || len < 0 || off + len > str.length())
         throw new IndexOutOfBoundsException();

      for( int i = off; i < off + len; i++ )
           writeChar(str.charAt(i), true);
   }

   /**
    * Запись через rtrim-фильтр, но без статистики и listener-а.
    */
   public void writeForced(String value) throws IOException {

      if( S.isNullOrEmpty(value) )
         return;

      for( int i = 0; i < value.length(); i++ )
           writeChar( value.charAt(i), false );
   }

   /** */
   private void writeChar( char ch, boolean count) throws IOException {

      if( ch == '\n' )
      {
         flushLine(count);
         writeDirect('\n', count);
         return;
      }

      if( ch == '\r')
      {
         flushLine(count);
         writeDirect('\r', count);
         return;
      }

      lineBuffer.append(ch);
   }

   /** */
   private void flushLine(boolean count) throws IOException {

      int end = rTrimmedLength( lineBuffer );

      for( int i = 0; i < end; i++)
           writeDirect( lineBuffer.charAt(i), count );

      lineBuffer.setLength(0);
   }

   /**
    * Сбросить текущую строку без rtrim.
    * Нужно перед переключением writer-а на буфер.
    */
   public void flushLineRaw(boolean count) throws IOException {

      for( int i = 0; i < lineBuffer.length(); i++)
           writeDirect(lineBuffer.charAt(i), count);

      lineBuffer.setLength(0);
   }

   /**
    * EOF считается концом строки.
    */
   public void finish() throws IOException {
      flushLine(true);
   }

   private void writeDirect(char ch, boolean count) throws IOException {

      out.write(ch);

      if( !count )
          return;

      updateStats(ch);

      if( listener != null )
         listener.written(ch);
   }

   /** */
   private void updateStats(char ch) {

      totalChars++;

      if (ch == '\n') {
         physicalLine++;
         currentCharInLine = 1;
      }
      else if (ch == '\r') {
         /*
          * FruWriter штатно пишет '\n'.
          * CR поддерживаем как line-break для rtrim,
          * но строковую статистику двигает LF.
          */
      }
      else {
         currentCharInLine++;
      }
   }

   /** */
   private static int rTrimmedLength( CharSequence value )
   {

      int end = value.length();

      while (end > 0) {
         char ch = value.charAt(end - 1);

         if (ch != ' ' && ch != '\t')
            break;

         end--;
      }

      return end;
   }

   @Override
   public void flush() throws IOException {
      /*
       * flush не конец строки.
       * Незавершённая строка остаётся в lineBuffer.
       */
      out.flush();
   }

   @Override
   public void close() throws IOException {
      finish();
      super.close();
   }
}