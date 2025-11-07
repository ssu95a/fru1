package ru.inversion.fru.api;

import ru.inversion.fru.altprint.ALTRunner;

import javax.swing.*;

/** */
public class FruLaunchViewer {

    public static void showReport( FruEngineConfig config )
    {
        SwingUtilities.invokeLater(() -> {

            ALTRunner altRunner = new ALTRunner( null ); // args любой массив String

            if( config.getGenerateMode() == FruEngineConfig.GenerateModeEnum.Display )
                altRunner.printReport( config.getOutFile().toString(), config.isOem(), config.getPrinterIndex(), 1, System.getProperty("java.io.tmpdir"), config.getCopies() );
            else if( config.getGenerateMode() == FruEngineConfig.GenerateModeEnum.Printer )
                altRunner.printReport( config.getOutFile().toString(), config.isOem(), config.getPrinterIndex(), 0, System.getProperty("java.io.tmpdir"), config.getCopies() );


//            int  altRunner.printReport(
//                    String fileName, // фал который печатать
//            boolean isOEM, // в ДОС кодировке иль нет
//            int printerID,     // какимпринтером (см. ниже)
//            int previewMode, // предварительно казать иль нет, 0 – нет, 1,2 да (разные режимы)
//            String tempPath, // путь до темп (хз зачем)
//            int copies ); // скока копий
        });

    }

}
