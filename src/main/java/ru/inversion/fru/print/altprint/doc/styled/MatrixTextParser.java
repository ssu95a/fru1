package ru.inversion.fru.print.altprint.doc.styled;

import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.fru.print.altprint.doc.MatrixRawWriter;
import ru.inversion.fru.print.naltprn.cmd.AltCommand;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;
import ru.inversion.utils.ReaderScanner;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;


/** */
public class MatrixTextParser implements IStyledTextParser {
   /** */
   private static class MatrixCommand implements ParsedElement {

      final private AltCommand command;
      /** */
      private MatrixCommand( AltCommand c ) {
         this.command = c;
      }

      /** */
      public byte[] toBytea()
      {
         if( command == null || command.getMatrixData() == null )
             return new byte[0];
         return command.getMatrixData().getPrinterCommand();
      }

      /** */
      @Override
      public void matrixWrite( MatrixRawWriter w ) throws IOException {
         w.command( toBytea() );
      }
   }

   private final Iterator<ReaderScanner.IContext> scanner;
   private final AltCommandDict commandDict;

   public MatrixTextParser( Reader reader, AltCommandDict commandDict ) {
      this.scanner     = ReaderScanner.newIterable(reader).iterator();
      this.commandDict = commandDict;
   }

   @Override
   public boolean hasNext() {
      return scanner.hasNext();
   }

   public ParsedElement next()
   {
      if( !hasNext() )
         throw new NoSuchElementException();

      try {

         StringBuilder text = new StringBuilder();

         char ch;
         ReaderScanner.IContext ctx;
         while ( scanner.hasNext() )
         {
            ctx = scanner.next();
            ch  = ctx.current();

            if( U.inChar( ch, '\f', '`' ) )
            {

               if( ch == '\f' )
                  return new MatrixCommand( commandDict.getCommand("FF") );

               if( ch == '\n' )
                  return new MatrixCommand( commandDict.getCommand("LF") );

               if( ch == '`' )
               {
                  final StringBuilder sb = new StringBuilder();

                  do
                  {
                     scanner.next();

                     if( ctx.current() == '`' )
                        break;

                     sb.append( ctx.current() );

                  }while( scanner.hasNext() );

                  if( sb.length() == 0 )
                      continue;

                  final String cmdText = sb.toString();

                  if( "PAGE_END".equalsIgnoreCase(cmdText) )
                      return new MatrixCommand( commandDict.getCommand("FF") );

                  if( "LF".equalsIgnoreCase(cmdText) )
                      return new MatrixCommand( commandDict.getCommand("LF") ); // ;LINE_FEED;

                  AltCommand cmd = commandDict.getCommand(cmdText);

                  if( cmd != null )
                     return new MatrixCommand(cmd);
               }
            }
            else
                 text.append(ch);

            if( U.inChar( ctx.next(), '\f', '\n', '`' ) )
                return new MatrixTextChunk( text );

         }//end for

         if( text.length() > 0 )
             return new MatrixTextChunk( text );

         return null;

      } catch( Exception e ) {
         throw new ALTPrintException("Ошибка при разборе файла для печати на матричном принтере", e );
      }
   }



   /*
   @Override
   public ParsedElement next()
   {
      if( eof )
         throw new NoSuchElementException();

      try {

         StringBuilder text = new StringBuilder();

         int ch;

         while( (ch = reader.read()) != -1 )
         {
            if( ch == '\f' )
            {
               if( text.length() > 0 )
                   return new MatrixTextChunk(text);

               return new MatrixCommand( commandDict.getCommand("FF") );
            }

            if( ch == '\n' )
            {
               if( text.length() > 0 )
                   return new MatrixTextChunk(text);

               return new MatrixCommand( commandDict.getCommand("LF") );
            }

            if( ch == '`' )
            {
               String cmdText = readCommand( reader, Integer.MAX_VALUE );

               if( "PAGE_END".equalsIgnoreCase(cmdText) )
               {
                  if( text.length() > 0 )
                      return new MatrixTextChunk(text);

                  //return PAGE_FEED;
                  return new MatrixCommand( commandDict.getCommand("FF") );
               }

               if ("LF".equalsIgnoreCase(cmdText)) {

                  if( text.length() > 0 )
                      return new MatrixTextChunk( text );

                  return new MatrixCommand( commandDict.getCommand("LF") ); // ;LINE_FEED;
               }

               AltCommand cmd = commandDict.getCommand(cmdText);

               if( cmd != null )
                   return new MatrixCommand(cmd);

               text.setLength(0);

               continue;
            }

            text.append( (char)ch );

         }//end while

         eof = true;

         if( text.length() > 0 )
             return new MatrixTextChunk( text);

         return null;

      } catch( Exception e ) {
         throw new ALTPrintException("Ошибка при разборе файла для печати на матричном принтере", e );
      }
   }*/
}
