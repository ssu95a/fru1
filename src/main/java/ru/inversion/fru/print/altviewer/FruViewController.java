package ru.inversion.fru.print.altviewer;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.print.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import org.controlsfx.control.StatusBar;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.fru.print.altprint.AltPrinter;
import ru.inversion.utils.MemoryURL;
import ru.inversion.utils.S;
import ru.inversion.fru.print.altprint.ALTDoc;
import ru.inversion.fru.print.altprint.ALTSettings;
import ru.inversion.utils.U;

import javax.print.attribute.standard.OrientationRequested;
import java.io.File;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

/** */
public class FruViewController implements Initializable {

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

    /** Размер шрифта */
    private static final String[] fontSize = {"8", "9", "10", "12", "14", "16", "20", "22", "24"};

    final private GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

    @FXML
    private ToolBar toolBar;

    @FXML
    private StatusBar statusBar;

    @FXML
    private VBox rootBox;

    private boolean isApplyingFormatting = false;

    private CodeArea fruArea;

    private ComboBox<String> fontComboBox;
    private ComboBox<String> zoomComboBox;
    private ComboBox<String> sizeComboBox;

    /** */
    final private Scale currentScale = new Scale( 1.0, 1.0 );

    /** */
    private ComboBox<Charset> encodingComboBox;

    /** */
    private ALTDoc     altDoc;
    private AltPrinter altPrinter;

    /** */
    final private ObjectProperty<ViewModeEnum> viewModeProperty = new SimpleObjectProperty<>( this, "viewModeProperty", ViewModeEnum.Plain );

    /** */
    //final private ObjectProperty<StateEnum> stateProperty = new SimpleObjectProperty<>( this, "stateProperty", StateEnum.Start );

    /** */
    private boolean isFormattedMode()
    {
        return viewModeProperty.get() == ViewModeEnum.Formatted;
    }

    public ALTDoc getAltDoc() {
        return altDoc;
    }

    public void setAltDoc(ALTDoc altDoc) {
        this.altDoc = altDoc;
    }

    /** */
    private void setAltPrinter(AltPrinter altPrinter) {
        this.altPrinter = altPrinter;
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
        String size = sizeComboBox.getValue();
        if( font != null )
            fruArea.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + size + "px;" );
    }

    /** */
    private void onFontSize( ) {
        try {
            int size = Integer.parseInt( sizeComboBox.getValue() );
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
        final ToggleButton formattedToggle = new ToggleButton(S.EMPTY_STRING, fontAwesome.create( FontAwesome.Glyph.FILE_IMAGE_ALT ) ); formattedToggle.setUserData(ViewModeEnum.Formatted);
        final ToggleButton plainTextToggle = new ToggleButton(S.EMPTY_STRING, fontAwesome.create( FontAwesome.Glyph.FILE_TEXT_ALT  ) ); plainTextToggle.setUserData(ViewModeEnum.Plain    );
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
                    viewModeProperty.setValue( (ViewModeEnum) newValue.getUserData() );
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
        sizeComboBox = new ComboBox<>();
        sizeComboBox.getItems().addAll( fontSize );
        sizeComboBox.setValue( fontSize[3] );
        sizeComboBox.setOnAction( e-> onFontSize() );

        // Кодировка
        encodingComboBox = new ComboBox<>();
        encodingComboBox.getItems().addAll(charsets);
        encodingComboBox.setValue(charsets[0]);
        encodingComboBox.setPrefWidth(120);
        encodingComboBox.setOnAction(this::updateEncoding);

        // Масштаб
        zoomComboBox = new ComboBox<>();
        zoomComboBox.getItems().addAll(zoomLevels);
        zoomComboBox.setValue("100%");
        zoomComboBox.setPrefWidth(80);
        zoomComboBox.setOnAction(e -> onZoom());

        // Кнопки
        Button loadButton = new Button("Загрузить файл");
        loadButton.setOnAction(e -> onLoadDocument());

        Button printButton = new Button("Печать", fontAwesome.create( FontAwesome.Glyph.PRINT).color(Color.WHITE) );
        printButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        printButton.setOnAction(e -> printDocument());

        Button printButton2 = new Button( S.EMPTY_STRING, fontAwesome.create( FontAwesome.Glyph.PRINT).color(Color.BLUEVIOLET) );
        printButton2.setOnAction(e -> printWysiwygDoc());

        toolBar.getItems().addAll (
            printButton, printButton2, new Separator(), loadButton,
            new Label("Шрифт:"),    fontComboBox,
            new Label("Размер:"),   sizeComboBox,
            new Label("Кодировка:"),encodingComboBox,
            new Label("Масштаб:"),  zoomComboBox
        );
    }


    /** */
    private void updateEncoding( ActionEvent e )
    {
        try {
            ALTDoc ad = ALTDoc.loadFile( altDoc.getAltFile(), encodingComboBox.getValue());
            loadFile(ad);
            altDoc = ad;
        } catch (Exception ex) {
          handleException( getStage(), ex);
        }
    }

    /** */
    private Stage getStage()
    {
        return (Stage) toolBar.getScene().getWindow();
    }

    /** */
    private void printDocument( )
    {
        try {
            altPrinter.print( getStage(), altDoc );
        } catch ( Throwable th ) {
            handleException( getStage(), th );
        }
    }

    /** **/
     private Transform configure( CodeArea codeArea, PageLayout pageLayout )
     {
         double contentWidth = codeArea.getTotalWidthEstimate();
         double pageWidth    = pageLayout.getPrintableWidth();

         if( contentWidth <= 0 || contentWidth > 10000 )
             ;//throw new ALTPrintException("Ширина не определена, печать без масштабирования");

         if( Math.abs(contentWidth - pageWidth) >= 0.1d )
         {
            double scale = pageWidth / contentWidth;
            scale = Math.max(scale, 0.3);

            Transform t = null;
            if( !codeArea.getTransforms().isEmpty())
                t = codeArea.getTransforms().get(0);

            final Scale scaleTransform = new Scale(scale, scale);
            codeArea.getTransforms().add(scaleTransform);

            codeArea.applyCss();
            codeArea.layout();

            return t;
         }
         return null;
     }


    /** */
    private void printWysiwygDoc( )
    {
        Transform t = null;

        try {

            final PrinterJob job = PrinterJob.createPrinterJob();
            if( job == null )
                throw new ALTPrintException("Невозможно проинициализировать службу печати");

            final JobSettings jobSettings = job.getJobSettings( );
            jobSettings.setJobName( altDoc.getAltFile().toString() );
            jobSettings.setCopies ( altDoc.getCopies().getValue()  );

            if( !job.showPrintDialog( fruArea.getScene().getWindow()) )
                return;

            Printer printer              = job.getPrinter();
            PageLayout defaultPageLayout = printer.getDefaultPageLayout();
            PageLayout pageLayout        = printer.createPageLayout (
                defaultPageLayout.getPaper(),
                U.decode( altDoc.getOrientation(), OrientationRequested.LANDSCAPE, PageOrientation.LANDSCAPE, PageOrientation.PORTRAIT ),
                Printer.MarginType.DEFAULT
            );

            t = configure( fruArea, pageLayout );

            // Печатаем
            boolean printResult = job.printPage(pageLayout, fruArea);
            if( printResult )
                job.endJob();

        } catch ( Throwable  th ) {
            handleException( getStage(), th );
        }
        finally {
            fruArea.getTransforms().clear();
            if( t != null ) {
                fruArea.getTransforms().add(t);
                fruArea.applyCss();
                fruArea.layout();
            }
        }
    }


    /** */
    private void initStatusBar() {

        //statusBar.textProperty().bind( stateProperty.asString() );

        Label fontLabel     = new Label( S.EMPTY_STRING ); fontLabel.textProperty().bind( fontComboBox.valueProperty().asString() );
        Label encodingLabel = new Label( S.EMPTY_STRING ); encodingLabel.textProperty().bind( encodingComboBox.valueProperty().asString() );
        Label modeLabel     = new Label( S.EMPTY_STRING ); modeLabel.textProperty().bind( viewModeProperty.asString() );
        Label zoomLabel     = new Label( S.EMPTY_STRING ); zoomLabel.textProperty().bind( zoomComboBox.valueProperty().asString() );

        statusBar.getRightItems().addAll (
            new Label("Режим: "),     modeLabel,     new Separator(Orientation.VERTICAL),
            new Label("Шрифт: "),     fontLabel,     new Separator(Orientation.VERTICAL),
            new Label("Кодировка: "), encodingLabel, new Separator(Orientation.VERTICAL),
            new Label("Масштаб: "),   zoomLabel
        );

        Label iniLabel = new Label( ALTSettings.INSTANCE().getINIFileName().toString() );
        //fontLabel.textProperty().bind( fontComboBox.valueProperty().asString() );

        statusBar.getLeftItems().addAll( new Label(ALTSettings.INI_FILE_NAME + ": " ),  iniLabel );
    }

    /** */
    private void initEditor( )
    {
        fruArea = new CodeArea();
        fruArea.getStyleClass().add("code-area");
        fruArea.getTransforms().add(currentScale);
        fruArea.setEditable(false);
        fruArea.setWrapText(false);
        final VirtualizedScrollPane<CodeArea> v = new VirtualizedScrollPane<>(fruArea);
        VBox.setVgrow( v, Priority.ALWAYS );
        fruArea.setPadding(new Insets(5.0) );
        rootBox.getChildren().set( 1, v );
    }

    /** */
    private void applyFormattingMode() {

        if( altDoc == null )
            return;

        try {

            TagProcessor.ParseResult result = TagProcessor.parseForFormattedMode( altDoc.readFile() );

            fruArea.replaceText  (    result.text );
            fruArea.setStyleSpans( 0, result.styleSpans );

            viewModeProperty.setValue( ViewModeEnum.Formatted );

            //updateStatusBar();
            //statusBar.setText("Форматирование применено");
        } catch( Throwable th ) {
            handleException( getStage(), th );
        }
    }

    /* */
    private void applyPlainMode() {

        if( altDoc == null )
            return;


        try {

            //statusBar.setText("Переключение в Plain Text режим...");

            TagProcessor.ParseResult result = TagProcessor.parseForPlainTextMode( altDoc.readFile() );

            fruArea.replaceText  (result.text);
            fruArea.setStyleSpans(0, result.styleSpans);

            viewModeProperty.setValue( ViewModeEnum.Plain );

            //updateStatusBar();
            //statusBar.setText("Plain Text режим");


        } catch( Throwable th ) {
            handleException( getStage(), th );
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
    private void setTitle( String title )
    {
        ((Stage)toolBar.getScene().getWindow()).setTitle( title );
    }

    /** */
    private ALTDoc loadFile( Path file)
    {
        return ALTDoc.loadFile( file, encodingComboBox.getValue() );
    }

    /** */
    private void loadFile( ALTDoc ad )
    {
        // statusBar.setText("Загрузка файла...");

        if( viewModeProperty.getValue() == ViewModeEnum.Formatted )
            applyFormattingMode();
        else
            applyPlainMode();

        Platform.runLater( ()->
            setTitle( "Предварительный просмотр - " + altDoc.getAltFile().toString() )
        );
    }

    /** */
    static public void handleException( Window window, Throwable th )
    {
        th.printStackTrace();

        ExceptionDialog dialog = new ExceptionDialog(th);
        if( window != null )
            dialog.initOwner( window );
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Ошибка");
        dialog.showAndWait();
    }

    /** */
    transient private File lastDirectory;

    /** */
    private void onLoadDocument() {

        try {

            final FileChooser fc = new FileChooser();
            fc.setTitle("Выберите файл для просмотра");

            if( lastDirectory != null )
                fc.setInitialDirectory(lastDirectory);

            final File file = fc.showOpenDialog(getStage());

            if( file != null ) {
                loadFile( file.toPath() );
                lastDirectory = file.getParentFile();
            }
        }
        catch ( Throwable th ) {
            handleException( getStage(), th );
        }
    }

    /** */
    @Override
    public void initialize( URL location, ResourceBundle resources )
    {
        initToolbar();

        initEditor();

        initFonts();

        initStatusBar();

        loadFile(altDoc);
    }


    /** */
    private static void initStyles( Scene scene ) throws Exception {

        scene.getStylesheets().add( FruViewController.class.getResource("res/base.css").toExternalForm() );

        final String cssStyles = ALTSettings.INSTANCE().commandDict().getCSSStylesList();

        if( cssStyles != null)
        {
            final URL cssUrl = MemoryURL.create("/styles/altprnt5ini.css", cssStyles );
            scene.getStylesheets().add( cssUrl.toExternalForm() );
        }
        else {
            System.err.println("No CSS styles found in command dictionary");
        }
    }


    /** */
    public static void showViewer(Stage primaryStage, AltPrinter altPrinter, ALTDoc altDoc )
    {
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation( FruApp.class.getResource("fxml/FruView.fxml"));
            loader.setControllerFactory(new Callback<Class<?>, Object>() {
                @Override
                public Object call(Class<?> type) {
                    try {
                        FruViewController controller = (FruViewController) type.newInstance();
                        controller.setAltDoc(altDoc);
                        controller.setAltPrinter(altPrinter);
                        return controller;
                    }
                    catch ( Throwable th ) {
                        throw new RuntimeException(th);
                    }
                }
            });

            Parent root = loader.load();
            Scene scene = new Scene(root);
            initStyles(scene);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
