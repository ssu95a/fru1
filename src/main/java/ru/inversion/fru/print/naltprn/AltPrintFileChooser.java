package ru.inversion.fru.print.naltprn;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;

import static ru.inversion.fru.print.naltprn.AltSettings.INI_FILE_NAME;

public final class AltPrintFileChooser {

    private AltPrintFileChooser() {}

    public static File chooseAltPrint5ini( )
    {
        // если headless (сервер/CI) — не падаем
        if( GraphicsEnvironment.isHeadless() ) {
            System.err.println( "Конфигурационный файл не найден и GUI недоступен (headless). Пропишите требуемый путь для " + INI_FILE_NAME + " согласно документации!" );
            return null;
        }

        Frame owner = new Frame();
        owner.setUndecorated(true);      // не показываем окно
        owner.setAlwaysOnTop(true);      // чтобы диалог не терялся
        owner.setLocationRelativeTo(null);

        final FileDialog dialog = new FileDialog( owner, "Выберите файл с параметрами ALTPRNT5.INI", FileDialog.LOAD);
        dialog.setFilenameFilter((dir,name) -> { int dot = name.lastIndexOf('.'); return dot >= 0 && "ini".equalsIgnoreCase( name.substring(dot + 1) );} );
        dialog.setFile(INI_FILE_NAME);

        try {

            dialog.setVisible(true);

            String dir = dialog.getDirectory();
            String file = dialog.getFile();

            if (dir == null || file == null)
                return null; // on cancel

            return new File(dir, file);
        }
        finally {
            dialog.dispose();
            owner.dispose();
        }
    }
}
