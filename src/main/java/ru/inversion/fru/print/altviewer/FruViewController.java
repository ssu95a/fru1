package ru.inversion.fru.print.altviewer;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.print.PageOrientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import org.controlsfx.control.StatusBar;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.FileChooserDialog;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.*;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.MemoryURL;
import ru.inversion.utils.S;
import ru.inversion.fru.print.altprint.ALTDoc;
import ru.inversion.fru.print.altprint.ALTSettings;

import javax.print.attribute.standard.Copies;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/** */
public class FruViewController extends JInvFXFormController<Void> {

    private enum StateEnum {
        Start,
        LoadFile,
        ApplyFormatting
    }

    /** */
    private enum ViewModeEnum {
        Plain, Formatted
    }

    /** Доступные кодировки */
    private static final Charset[] charsets = {
            Charset.forName("windows-1251"),
            Charset.forName("IBM866"),
            StandardCharsets.UTF_8
    };

    /** Уровни масштабирования */
    private static final String[] zoomLevels = {"50%", "75%", "100%", "125%", "150%", "200%"};


    @FXML
    private JInvToolBar toolBar;

    @FXML
    private StatusBar statusBar;

    @FXML
    private VBox rootBox;

    @FXML
    private JInvLabel stub;

    private boolean isApplyingFormatting = false;

    private CodeArea fruArea;

    private ComboBox<String> fontComboBox;
    private ComboBox<String> zoomComboBox;

    private JInvLongField    fontSizeField;

    final private Scale      currentScale = new Scale(1.0, 1.0);;

    /** */
    private JInvComboBox<Charset,Charset> encodingComboBox;

    /** */
    private ALTDoc altDoc;

    /** */
    final private ObjectProperty<ViewModeEnum> viewModeProperty = new SimpleObjectProperty<>( this, "viewModeProperty", ViewModeEnum.Plain );

    /** */
    final private ObjectProperty<StateEnum> stateProperty = new SimpleObjectProperty<>( this, "stateProperty", StateEnum.Start );

    /** */
    private boolean isFormattedMode()
    {
        return viewModeProperty.get() == ViewModeEnum.Formatted;
    }

    /** */
    private void initFonts()
    {
        final List<String> fontNames = FontMan.getMonospaceFontNames();

        fontComboBox.getItems().clear();
        fontComboBox.getItems().addAll(fontNames);

        String defaultFontName = FontMan.getDefaultMonospaceFont(fontNames);
        fontComboBox.setValue( defaultFontName );

        onFontName();
    }

    /** */
    private void onFontName( ) {
        String font = fontComboBox.getValue();
        String size = fontSizeField.getText();
        if( font != null )
            fruArea.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + size + "px;" );
    }

    /** */
    private void onFontSize( ) {
        try {
            int size = fontSizeField.getValue().intValue();
            if (size > 0 && size <= 72)
                onFontName();
        } catch (NumberFormatException ignore ) {
            // ignore
        }
    }

    /** */
    private void onZoom( )
    {
        final String zoomValue = zoomComboBox.getValue( );

        if( zoomValue != null )
        {
            try {
                double zoomLevel = Double.parseDouble(zoomValue.replace("%", "")) / 100.0;
                currentScale.setX(zoomLevel);
                currentScale.setY(zoomLevel);
                fruArea.requestLayout();
            } catch( NumberFormatException e ) {
                System.err.println("Invalid zoom value: " + zoomValue);
            }
        }
    }

    /** */
    private void initToggleMode( )
    {
        // Режим просмотра
        final ToggleGroup viewModeGroup    = new ToggleGroup();
        final ToggleButton formattedToggle = new ToggleButton("G"); formattedToggle.setUserData(ViewModeEnum.Formatted);
        final ToggleButton plainTextToggle = new ToggleButton("M"); plainTextToggle.setUserData(ViewModeEnum.Plain    );
        formattedToggle.setToggleGroup(viewModeGroup);
        plainTextToggle.setToggleGroup(viewModeGroup);
        plainTextToggle.setSelected(true);

        final AtomicBoolean inToggle = new AtomicBoolean(false);

        viewModeGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {

                if( inToggle.get() )
                    return;

                inToggle.set(true);

                try {
                    viewModeProperty.setValue((ViewModeEnum) newValue.getUserData());
                }
                finally {
                    inToggle.set(false);
                }
            }
        });

        viewModeProperty.addListener(new ChangeListener<ViewModeEnum>() {
            @Override
            public void changed(ObservableValue<? extends ViewModeEnum> observable, ViewModeEnum oldValue, ViewModeEnum newValue) {

                if( !inToggle.get() )
                    return;

                inToggle.set(false);

                try {
                    if( newValue == ViewModeEnum.Formatted )
                        applyFormattingMode();
                    else
                        applyPlainMode();
                }
                finally {
                    inToggle.set(true);
                }
            }
        });
        toolBar.getItems().addAll( new Label("Режим:"), formattedToggle, plainTextToggle );
    }

    /** */
    private void initToolbar( )
    {
        initToggleMode( );

        // Выбор шрифта
        fontComboBox = new ComboBox<>();
        fontComboBox.setPrefWidth(200);
        fontComboBox.setOnAction(e -> onFontName());

        // Размер шрифта
        fontSizeField = new JInvLongField(12L);
        fontSizeField.setPrefWidth(35);
        fontSizeField.valueProperty().addListener(new ChangeListener<Long>() {
            @Override
            public void changed(ObservableValue<? extends Long> observable, Long oldValue, Long newValue) {
                onFontSize();
            }
        });

        // Кодировка
        encodingComboBox = new JInvComboBox<>();
        encodingComboBox.getItems().addAll(charsets);
        encodingComboBox.setValue(charsets[0]);
        encodingComboBox.setPrefWidth(120);
        //encodingComboBox.setOnAction(e -> updateEncoding());

        // Масштаб
        zoomComboBox = new ComboBox<>();
        zoomComboBox.getItems().addAll(zoomLevels);
        zoomComboBox.setValue("100%");
        zoomComboBox.setPrefWidth(80);
        zoomComboBox.setOnAction(e -> onZoom());

        // Кнопки
        Button loadButton = new Button("Загрузить файл");
        loadButton.setOnAction(e -> onLoadDocument());

        Button printButton = new Button("Печать");
        printButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        printButton.setOnAction(e -> printDocument());

        toolBar.getItems().addAll (
            printButton, loadButton,
            new Label("Шрифт:"), fontComboBox,
            new Label("Размер:"), fontSizeField,
            new Label("Кодировка:"), encodingComboBox,
            new Label("Масштаб:"), zoomComboBox
        );
    }

    /** */
    private Function<String,Object> getPrintSettings()
    {
        return new Function<String, Object>() {
            @Override
            public Object apply(String s) {
                switch (s.toLowerCase()) {
                    case "orientation":
                    case "pageorientation":
                        return PageOrientation.PORTRAIT;
                    case "copies":
                        return new Copies(1);
                    case "printjobname":
                        return "ALT: " + altDoc.getAltFile();
                }
                return null;
            }
        };
    }

    /** */
    private void printDocument( )
    {
        try {
//            AltPrinter altPrinter = new AltPrinter( getPrintSettings() );
//            altPrinter.print( getViewContext(), altDoc );
        } catch ( Throwable th ) {
            handleException(th);
        }
    }

    /** */
    private void initStyles( Scene scene ) {

        try {

            scene.getStylesheets().add( getClass().getResource("res/base.css").toExternalForm() );

            final String cssStyles = ALTSettings.INSTANCE().commandDict().getCSSStylesList();

            if( cssStyles != null)
            {
                final URL cssUrl = MemoryURL.create("/styles/altprnt5ini.css", cssStyles );
                scene.getStylesheets().add( cssUrl.toExternalForm() );
            }
            else {
                System.err.println("No CSS styles found in command dictionary");
            }

        } catch (Exception e) {
            System.err.println("Error setting up styles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** */
    private void initStatusBar() {

        statusBar.textProperty().bind( stateProperty.asString() );

        JInvLabel fontLabel     = new JInvLabel( S.EMPTY_STRING ); fontLabel.textProperty().bind( fontComboBox.valueProperty().asString() );
        JInvLabel encodingLabel = new JInvLabel( S.EMPTY_STRING ); encodingLabel.textProperty().bind( encodingComboBox.valueProperty().asString() );
        JInvLabel modeLabel     = new JInvLabel( S.EMPTY_STRING ); modeLabel.textProperty().bind( viewModeProperty.asString() );
        JInvLabel zoomLabel     = new JInvLabel( S.EMPTY_STRING ); zoomLabel.textProperty().bind( zoomComboBox.valueProperty().asString() );

        statusBar.getRightItems().addAll (
            new JInvLabel("Режим: "),     modeLabel, new Separator(Orientation.VERTICAL),
            new JInvLabel("Шрифт: "),     fontLabel, new Separator(Orientation.VERTICAL),
            new JInvLabel("Кодировка: "), encodingLabel, new Separator(Orientation.VERTICAL),
            new JInvLabel("Масштаб: "),   zoomLabel
        );

        JInvLabel fileLabel     = new JInvLabel( S.EMPTY_STRING ); fontLabel.textProperty().bind( fontComboBox.valueProperty().asString() );

        //statusBar.getLeftItems().addAll( new JInvLabel("Файл: "), fileLabel );
    }

    /** */
    private void initEditor( )
    {
        fruArea = new CodeArea();
        fruArea.getStyleClass().add("code-area");
        fruArea.getTransforms().add(currentScale);
        fruArea.setEditable(false);
        fruArea.setWrapText(false);
        VirtualizedScrollPane<CodeArea> v = new VirtualizedScrollPane<>(fruArea);
        VBox.setVgrow( v, Priority.ALWAYS );
        rootBox.getChildren().set( 1, v   );
    }

    /** */
    private void applyFormattingMode() {

        if( altDoc == null )
            return;

        isApplyingFormatting = true;

        try {

            TagProcessor.ParseResult result = TagProcessor.parseForFormattedMode( altDoc.readFile() );

            Platform.runLater(() -> {
                try {

                    fruArea.replaceText  (    result.text );
                    fruArea.setStyleSpans( 0, result.styleSpans );

                    viewModeProperty.setValue( ViewModeEnum.Formatted );

                    //updateStatusBar();
                    //statusBar.setText("Форматирование применено");

                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    //statusBar.setText("Ошибка применения стилей");
                    showTextWithoutStyles(result.text);
                } finally {
                }
            });

        } catch (Exception e) {
            //System.err.println("Parsing error: " + e.getMessage());
            e.printStackTrace();
            //statusBar.setText("Ошибка парсинга");
            //showTextWithoutStyles(originalText);
        } finally {
            isApplyingFormatting = false;
        }
    }

    private void applyPlainMode() {

        if( altDoc == null )
            return;

        isApplyingFormatting = true;

        try {

            //statusBar.setText("Переключение в Plain Text режим...");

            TagProcessor.ParseResult result = TagProcessor.parseForPlainTextMode( altDoc.readFile() );

            Platform.runLater(() -> {
                try {
                    fruArea.replaceText(result.text);
                    fruArea.setStyleSpans(0, result.styleSpans);

                    viewModeProperty.setValue( ViewModeEnum.Plain );

                    //updateStatusBar();
                    //statusBar.setText("Plain Text режим");

                } catch (Exception e) {
                    System.err.println("Error in plain text mode: " + e.getMessage());
                    //statusBar.setText("Ошибка переключения режима");
                }
            });

        } catch (Exception e) {
            System.err.println("Error showing plain text: " + e.getMessage());
            //statusBar.setText("Ошибка: " + e.getMessage());
        } finally {
            isApplyingFormatting = false;
        }
    }


    /**
     * Показывает текст без стилей (аварийный режим)
     */
    private void showTextWithoutStyles(String text) {
        try {
            fruArea.clear();
            fruArea.appendText(text);
            System.out.println("Text shown without styles (fallback mode)");
        } catch (Exception e) {
            System.err.println("Even fallback mode failed: " + e.getMessage());
        }
    }

    /** */
    private void loadFile( File file)
    {
        // statusBar.setText("Загрузка файла...");

        ALTDoc ad = ALTDoc.loadFile( file.toPath(), encodingComboBox.getValue() );

        final TagProcessor.ParseResult result;

        if( viewModeProperty.get() == ViewModeEnum.Formatted )
            result = TagProcessor.parseForFormattedMode( ad.readFile() );
        else
            result = TagProcessor.parseForPlainTextMode( ad.readFile() );

        this.altDoc = ad;

        Platform.runLater(() -> {

            try {

                fruArea.replaceText  ( result.text );
                fruArea.setStyleSpans( 0, result.styleSpans );

                setTitle( "Предварительный просмотр - " + file.getName() );

//                statusBar.setText("Файл загружен: " + file.getName());

            } catch( Exception e ) {
                handleException(e);
                //statusBar.setText("Ошибка обновления интерфейса");
            }
        });
    }

    /** */
    private void onLoadDocument() {

        try {

            final File file = new FileChooserDialog("fru.viewer.file").title("Выберите файл для просмотра").showOpenDialog(getViewContext().getWindow());

            if( file != null )
                loadFile(file);
        }
        catch ( Throwable th ) {
            handleException(th);
        }
    }

    @Override
    protected void beforeInit() throws Exception {
        initStyles( rootBox.getScene() );
    }

    /** */
    @Override
    protected void init()
    {
        initToolbar();

        initEditor();

        initFonts();

        initStatusBar();
    }


    /** */
    public static void showViewer( ViewContext vc, TaskContext tc, Map<String,Object> parameters )
    {
        new FXFormLauncher<>( tc, vc, FruViewController.class, BaseApp.APP().getCommonResourceBundle() ).show ();
    }

}
