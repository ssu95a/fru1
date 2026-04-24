package ru.inversion.fru.print.altviewer;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.util.prefs.Preferences;

/**
 * Сохраняет состояние окна: размеры, положение, максимизацию
 */
public class WindowSizeSaver {

    private final Stage stage;
    private final Preferences prefs;

    public WindowSizeSaver( Stage stage, String configName) {
        this.stage = stage;
        this.prefs = Preferences.userRoot().node(configName);
        // Обработчик на закрытие
        stage.setOnCloseRequest(this::onWindowClosing);
    }

    // Восстановление состояния
    public void load() {

        // Значения по умолчанию
        double defaultWidth  = 900;
        double defaultHeight = 600;

        double x = prefs.getDouble("x", -1);
        double y = prefs.getDouble("y", -1);

        double width = prefs.getDouble("width", defaultWidth);
        double height = prefs.getDouble("height", defaultHeight);

        boolean maximized = prefs.getBoolean("maximized", false);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        // Если координаты не сохранены или некорректны
        if (x < 0 || y < 0 ||
                x > screen.getWidth() - 100 ||
                y > screen.getHeight() - 100) {
            // Центрируем
            x = screen.getWidth() / 2 - width / 2;
            y = screen.getHeight() / 2 - height / 2;
        }

        // Устанавливаем
        if( maximized )
            stage.setMaximized(true);
        else {
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(width);
            stage.setHeight(height);
        }
    }

    // Обработчик закрытия окна
    private void onWindowClosing(WindowEvent event) {
        save();
    }

    public void save() {

        prefs.putBoolean("maximized", stage.isMaximized());

        if( !stage.isMaximized() )
        {
            prefs.putDouble("x", stage.getX());
            prefs.putDouble("y", stage.getY());
            prefs.putDouble("width", stage.getWidth());
            prefs.putDouble("height", stage.getHeight());
        }

        try {
            prefs.flush();
            //System.out.println("Сохранено состояние окна: " + String.format("%.0fx%.0f @ (%.0f,%.0f)", stage.getWidth(), stage.getHeight(), stage.getX(), stage.getY()));
        } catch (Exception e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    public void resetToDefaults() {
        try {
            prefs.clear();
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Ошибка сброса: " + e.getMessage());
        }
    }
}