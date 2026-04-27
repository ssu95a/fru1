package ru.inversion.fru.print.altprint.doc;

import ru.inversion.utils.S;
import ru.inversion.utils.io.RawBAOS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Для печати на матричный принтер
 * разделен вывод текста и вывод управляющих команд
 */
public final class MatrixRawWriter implements AutoCloseable {

   private final RawBAOS out;
   private final Charset textCharset;

   /** */
   public MatrixRawWriter(RawBAOS out, Charset textCharset ) {
      this.out = out;
      this.textCharset = textCharset;
   }

   /** */
   public void text(String text) throws IOException {
      if(!S.isNullOrEmpty(text) )
          out.write(text.getBytes(textCharset));
   }

   /** */
   public void command( byte[] command) throws IOException {
      if( command != null && command.length > 0)
          out.write(command);
   }

   @Override
   public void close() throws IOException {
      out.close();
   }

   /** */
   public OutputStream out() {
      return out;
   }

   /** */
   public byte[] bytea() {
      return out.buf();
   }

   /** */
   public InputStream is() {
      return out.inputStream();
   }
}