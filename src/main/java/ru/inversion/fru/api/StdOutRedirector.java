package ru.inversion.fru.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Перенаправить System.out в логгер STDOUT.
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - System.err
 * - конфигурацию logback
 */
public final class StdOutRedirector {

   private StdOutRedirector() {
   }

   public static void install() {

      final Logger logger = LoggerFactory.getLogger("STDOUT");

      OutputStream out = new OutputStream() {
         private final StringBuilder buffer = new StringBuilder();

         @Override
         public void write(int b) {
            if (b == '\n') {
               flushBuffer();
            } else if (b != '\r') {
               buffer.append((char) b);
            }
         }

         @Override
         public void flush() {
            flushBuffer();
         }

         private void flushBuffer() {
            if (buffer.length() > 0) {
               logger.info(buffer.toString());
               buffer.setLength(0);
            }
         }
      };

      System.setOut( new PrintStream(out, true) );
   }
}