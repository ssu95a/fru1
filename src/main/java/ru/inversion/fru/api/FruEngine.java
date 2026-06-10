package ru.inversion.fru.api;

import org.slf4j.bridge.SLF4JBridgeHandler;
import ru.inversion.fru.api.exceptions.FruCommandLineException;
import ru.inversion.fru.api.exceptions.FruException;
import ru.inversion.fru.data.FruDataFile;
import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.Fru;
import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.parser.FruEntryProbe;
import ru.inversion.fru.parser.FruParser;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.altprint.AltPrinter;
import ru.inversion.fru.print.altviewer.FruApp;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

/**
 * Основной движок для генерации отчетов FRU
 * парсинг форм, обработка данных и рендеринг на принтер
 */
public class FruEngine {

    private static final Logger log = LoggerFactory.getLogger(FruEngine.class);

    static public final Charset csWin1251 = Charset.forName("CP1251");
    static public final Charset csDos866  = Charset.forName("CP866" );

    /** */
    public FruEngine() {
    }

    /** Основной метод генерации отчета из готовых объектов */
    private void generate( Fru fru, FruDataFile dataFile, Writer output )
    {
        try( FruContext context  = new FruContext( fru, output, dataFile ) ) {
            // Запускаем процесс генерации
            while(!context.data().eof()) {
                   context.data().next(); // зовет рендеринг через FruContext.setCurrentRow()
            }
        } catch( Exception e ) {
            throw new FruException( "Ошибка генерации отчета", e );
        }
    }

    /** Парсит FRU форму из файла */
    private static Fru parseFru(Path fruFile, Charset charset) {

        boolean repeat = false;

        do {

            try {

                final boolean hasExplicitEntry = FruEntryProbe.hasExplicitEntry( fruFile, charset );
                final boolean entryImplicit    = !hasExplicitEntry;

                if( entryImplicit )
                    log.debug("FRU {}: explicit #entry not found, entryImplicit=true", fruFile );

                try (Reader reader = Files.newBufferedReader(fruFile, charset)) {
                    FruBuilder fruBuilder = new FruBuilder(fruFile, charset);

                    FruParser.parseFru(reader, fruBuilder, entryImplicit);

                    return fruBuilder.build();
                }

            }
            catch (Throwable th) {

                if (!repeat)
                {
                    Throwable th1 = th;

                    while (th1 != null)
                    {
                        if( th1 instanceof UnmappableCharacterException) {
                            repeat = true;
                            break;
                        }

                        if (th1 instanceof java.nio.charset.MalformedInputException) {
                            repeat = true;
                            break;
                        }

                        th1 = th1.getCause();
                    }

                    if (repeat) {
                        charset = charset == csWin1251 ? csDos866 : csWin1251;
                        log.warn("Смена кодировки файла формы на {}", charset);
                        continue;
                    }
                }
                throw new FruException( "Ошибка разбора тела формы из файла " + fruFile + ", в кодировке " + charset, th );
            }
        }
        while (repeat);

        return null;
    }

    /** */
    public static void print( String[] args ) throws Exception
    {
        final FruEngineConfig config = FruEngineConfig.parseCommandLine(args);

        final FruEngine engine = new FruEngine();

        if( config.useFru() )
        {
            final Fru fru = parseFru( config.getFruFile(), config.getCharset() );

            try( FruDataFile datFile = new FruDataFile( config.getDatFile(), config.getCharset() ) ) {
                 engine.generate(fru, datFile, Files.newBufferedWriter( config.getOutFile(), config.getCharset())) ;
            }

            // Копируем файл данных, если вывод не задан явно
            config.normalizeOutFile();
        }

        if( config.getGenerateMode() == FruEngineConfig.GenerateModeEnum.File )
            ;//altPrinter.saveTo();
        else
        {
            final AltPrinter altPrinter = new AltPrinter( );

            final ALTDoc altDoc
                = ALTDoc.loadFile (
                    config.getOutFile(),
                    resolveCharset( config.getOutFile(), config.getCharset() )
                );

            if( config.getGenerateMode() == FruEngineConfig.GenerateModeEnum.Printer )
                altPrinter.print( altDoc, null);

            else if( config.getGenerateMode() == FruEngineConfig.GenerateModeEnum.Display )
                FruApp.run( altPrinter, altDoc );
        }
    }

    /**
        Уточнить кодировку файла, если XML то он мб в UTF
     */
    private static Charset resolveCharset( Path file, Charset defCharset )
    {
        final String fileName = file.toString();

        if( fileName.toLowerCase().endsWith(".xml") )
        {
            log.info("Проверка кодировки XML файла {}", fileName );

            try( InputStream fis = Files.newInputStream(file) )
            {
                final String encoding;

                final XMLInputFactory factory = XMLInputFactory.newInstance();
                final XMLStreamReader reader  = factory.createXMLStreamReader(fis);

                encoding = reader.getEncoding();
                reader.close();

                if( encoding == null )
                    log.info("кодировка: не возможно определить. null" );
                else
                {
                    log.info("кодировка: {}", encoding);
                    return Charset.forName(encoding);
                }
            } catch (Exception e ) {
                log.error("кодировка: ошибка при определении кодировки: {}", e.getLocalizedMessage() );
            }
        }
        return defCharset;
    }

    /**
     * Основной метод для запуска из командной строки
     */
    public static void main( String[] args )
    {
        try {

            BuildInfo.installSystemProperties();

            final URL logbackUrl = FruEngine.class.getResource("/logback.xml");

            if( logbackUrl != null )
                System.setProperty( ch.qos.logback.classic.util.ContextInitializer.CONFIG_FILE_PROPERTY, logbackUrl.toExternalForm() );
            else
                System.err.println( "Не найден /logback.xml внутри jar" );

            LogManager.getLogManager().reset();

            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            StdOutRedirector.install();
            System.setErr( new PrintStream( System.err, true, "CP866" ) );

            log.info( System.getProperty( "app.lastBuildTime", BuildInfo.UNKNOWN) );
            log.info( System.getProperty( "app.foreInfo",      BuildInfo.UNKNOWN) );
            log.info( System.getProperty( "app.bilInfo",       BuildInfo.UNKNOWN) );

            log.info( "FRU started, args={}", Arrays.toString(args) );
            print( args );
            log.info("FRU finished successfully");
        }
        catch( FruCommandLineException fcle ) {
            System.out.println( "Ошибка при запуске: " + fcle.getMessage() );
            System.out.println( "Не корректные входные параметры." );
            printUsage();
            System.exit(1);
        }
        catch( Exception e ) {
            log.error( "Unhandled error", e );
            System.exit(1);
        }
    }

    /** */
    private static void printUsage()
    {
        System.out.println( );
        System.out.println("Использование: [options] <report file with cursor data> <fru-form-file> <output-file>");
        System.out.println( );
        System.out.println("Параметры:");
        System.out.println("      fru-file - FRU файл с описанием формы отчета");
        System.out.println("     data-file - файл с данными для отчета");
        System.out.println("   output-file - файл для сохранения результата");
        System.out.println( );
        System.out.println("[options]");
        System.out.println("-C<num> Copies");
        System.out.println("-E Allow editing");
        System.out.println("-G[D|F|P] Generate to Display, File or Printer");
        System.out.println("-L Light view");
        System.out.println("-O OEM encoding | -U utf-8 encoding");
        System.out.println("-P<idx> Printer index");
        System.out.println("-S Silent mode");
        System.out.println( );
        System.out.println("Пример: ");
        System.out.println("   FruEngine -O report.fru data.dat result.txt");
    }

    /** Метод генерации отчета из готовых объектов */
    static public void generate( Path fruPath, Reader reader, Writer output )
    {
        final Fru fru;

        try {
             fru = parseFru( fruPath, csWin1251 );
        }
        catch ( Throwable th ) {
            throw new FruException( "Ошибка разбора файла FRU отчета", th );
        }

        try ( FruContext context  = new FruContext( fru, output, new FruDataFile(reader) ) ) {
            // Запускаем процесс генерации
            while(!context.data().eof()) {
                   context.data().next(); // зовет рендеринг через FruContext.setCurrentRow()
            }
        } catch( Exception e ) {
            throw new FruException( "Ошибка генерации отчета", e );
        }
    }
}