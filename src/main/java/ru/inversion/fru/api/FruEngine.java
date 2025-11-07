package ru.inversion.fru.api;

import ru.inversion.fru.api.exceptions.FruCommandLineException;
import ru.inversion.fru.api.exceptions.FruException;
import ru.inversion.fru.data.FruDataFile;
import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.generator.FruWriter;
import ru.inversion.fru.model.Fru;
import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.parser.FruParser;

import javax.swing.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Основной движок для генерации отчетов FRU
 * парсинг форм, обработка данных и рендеринг
 */
public class FruEngine {

    static public final Charset csWin1251 = Charset.forName("CP1251");
    static public final Charset csDos866  = Charset.forName("CP866" );

    /** */
    public FruEngine() {
    }

    /**
     * Основной метод генерации отчета из готовых объектов
     */
    public void generate( Fru fru, FruDataFile dataFile, Writer output )
    {
        try ( FruWriter fruWriter = new FruWriter(output);
              FruContext context = new FruContext(fru, fruWriter, dataFile)) {

            // Запускаем процесс генерации
            while(!context.data().eof()) {
                   context.data().next(); // Это вызовет рендеринг через FruContext.setCurrentRow()
            }
        } catch( Exception e ) {
            throw new FruException("Ошибка генерации отчета", e);
        }
    }


    /** Парсит FRU форму из файла */
    private static Fru parseFru( Path fruFile, Charset charset ) throws Exception {

        try( Reader reader = Files.newBufferedReader( fruFile, charset) )
        {
            FruBuilder fruBuilder = new FruBuilder( fruFile, charset );
            FruParser.parseFru( reader, fruBuilder );
            return fruBuilder.build();
        }
    }

    /** */
    public static void print( String[] args ) throws Exception
    {
        final FruEngineConfig config = FruEngineConfig.fromCommandLine(args);

        final FruEngine engine = new FruEngine();
        final Fru fru = parseFru( config.getFruFile(), config.getCharset() );

        try( FruDataFile datFile = new FruDataFile( config.getDatFile(), config.getCharset() );
        )
        {
            engine.generate( fru, datFile, Files.newBufferedWriter( config.getOutFile(), Charset.forName("Cp866")) );
        }

        if( config.getGenerateMode() != FruEngineConfig.GenerateModeEnum.File )
            FruLaunchViewer.showReport(config);

        //System.out.println("Файл с отчетом сформирован: " + config.getOutFile().toString() );
    }


    /**
     * Основной метод для запуска из командной строки
     */
    public static void main( String[] args )
    {
        try {

            print( args );

        }
        catch( FruCommandLineException fcle ) {
            System.err.println( "Ошибка при запуске: " + fcle.getMessage() );
            printUsage();
        }
        catch( Exception e ) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage()
    {
        System.out.println("Использование: [options] <report file with cursor data> <fru-form-file> <output-file>");
        System.out.println( );
        System.out.println("Параметры:");
        System.out.println("   fru-file    - FRU файл с описанием формы отчета");
        System.out.println("   data-file   - файл с данными для отчета");
        System.out.println("   output-file - файл для сохранения результата");
        System.out.println( );
        System.out.println("[options]");
        System.out.println("-C<num> Copies");
        System.out.println("-E Allow editing");
        System.out.println("-G[D|F|P] Generate to Display, File or Printer");
        System.out.println("-L Light view");
        System.out.println("-O OEM encoding");
        System.out.println("-P<idx> Printer index");
        System.out.println("-S Silent mode");
        System.out.println( );
        System.out.println("Пример:");
        System.out.println("   FruEngine -O report.fru data.dat result.txt");
    }
}