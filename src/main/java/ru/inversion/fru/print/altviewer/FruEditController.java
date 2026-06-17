package ru.inversion.fru.print.altviewer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.fru.print.naltprn.cmd.AltCommand;

import javax.swing.text.DefaultEditorKit;
import java.io.BufferedWriter;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ru.inversion.fru.print.altviewer.FruViewController.handleException;

public class FruEditController extends FruControllerBase {

   final private GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

   @FXML
   private TextArea editor;
   @FXML
   private ToolBar toolBar;

   @FXML
   private Button btSaveAndClose;
   @FXML
   private Button btCancelAndClose;

   @FXML
   private Button btSave;
   @FXML
   private Button btSaveAs;
   @FXML
   private Button btCut;
   @FXML
   private Button btCopy;
   @FXML
   private Button btPaste;
   @FXML
   private Button btDelete;
   @FXML
   private ChoiceBox<String> cbCommands;
   @FXML
   private Button btUndo;
   @FXML
   private Button btRedo;

   private ALTDoc altDoc;
   private AtomicBoolean retValue;

   /** */
   @Override
   public void initialize(URL location, ResourceBundle resources)
   {
      initToolBar( );
      initEditor ( );
      Platform.runLater( ()->
         setTitle( "Редактирование - " + altDoc.getAltFile().toString() )
      );

      // loadALTDoc(altDoc);
   }

   /** */
   protected Stage getStage()
   {
      return toolBar == null ? null : (Stage)toolBar.getScene().getWindow();
   }

   /** */
   private void initToolBar( )
   {

      btSaveAndClose.setGraphic( fontAwesome.create( FontAwesome.Glyph.CHECK).color(Color.WHITE) );
      btSaveAndClose.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
      btSaveAndClose.setOnAction( (e)->this.saveAndClose() );

      btCancelAndClose.setGraphic( fontAwesome.create( FontAwesome.Glyph.BAN).color(Color.WHITE) );
      btCancelAndClose.setStyle("-fx-background-color: #8c1e0a; -fx-text-fill: white;");
      btCancelAndClose.setOnAction( e->cancelAndClose() );

      btUndo.setGraphic   ( fontAwesome.create( FontAwesome.Glyph.UNDO ) );
      btUndo.setOnAction  ( (e)->editor.undo() );
      btUndo.disableProperty().bind( editor.undoableProperty().not() );

      btRedo.setGraphic   ( fontAwesome.create( FontAwesome.Glyph.ROTATE_RIGHT ) );
      btRedo.setOnAction  ( (e)->editor.redo() );
      btRedo.disableProperty().bind( editor.redoableProperty().not() );

      btSave.setGraphic   ( fontAwesome.create( FontAwesome.Glyph.SAVE  ) );
      btSave.setOnAction  ( e->saveFile(null) );
      btSaveAs.setGraphic ( fontAwesome.create( FontAwesome.Glyph.FLOPPY_ALT  ) );
      btSaveAs.setOnAction( this::saveFileAs );
      btCut.setGraphic    ( fontAwesome.create( FontAwesome.Glyph.CUT   ) );
      btCut.setOnAction   ( (e)->editor.cut() );
      btCopy.setGraphic   ( fontAwesome.create( FontAwesome.Glyph.COPY  ) );
      btCopy.setOnAction  ( (e)->editor.copy() );
      btPaste.setGraphic  ( fontAwesome.create( FontAwesome.Glyph.PASTE ) );
      btPaste.setOnAction ( (e)->editor.paste() );
      btDelete.setGraphic ( fontAwesome.create( FontAwesome.Glyph.REMOVE) );
      btDelete.setOnAction( this::onDelete );

      cbCommands.getItems().addAll (
         AltSettings.INSTANCE().commandDict()
            .getCommandList().stream().map(AltCommand::getName).collect( Collectors.toList() )
      );

      cbCommands.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
         @Override
         public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            String t = "`" + newValue + "`";
            insertText(t);
         }
      });
   }

   /** */
   private void insertText( String text ) {

      IndexRange selection = editor.getSelection();

      if( selection.getStart() == selection.getEnd() )
         editor.insertText( selection.getStart(), text );
      else
         editor.replaceText( selection, text );

   }

   /** */
   private void onDelete(ActionEvent e) {
      editor.deleteText(editor.getSelection());
   }

   transient private File lastDirectory = null;

   /** */
   private void saveFileAs( ActionEvent e )
   {
      try {

         final FileChooser fc = new FileChooser();
         fc.setTitle("Выберите имя файла для сохранения");

         if( lastDirectory != null )
            fc.setInitialDirectory(lastDirectory);

         fc.setInitialFileName( altDoc.getAltFile().toString() );

         final File file = fc.showSaveDialog(getStage());

         if( file != null )
         {
            saveFile( file.toPath() );
            lastDirectory = file.getParentFile();
         }
      }
      catch ( Throwable th ) {
         handleException( getStage(), th );
      }

   }

   /** */
   private void saveFile( Path fileTo )
   {
      try {

         Path altFile    = fileTo == null ? altDoc.getAltFile() : fileTo;
         Charset charset = altDoc.getCharset();

         try( BufferedWriter writer = Files.newBufferedWriter( altFile, charset ) )
         {
            writer.write(editor.getText());
         }
      }
      catch( Exception ex ) {
         handleException( getStage(), ex );
      }
   }

   /** */
   private void initEditor ( )
   {
      editor.setText( altDoc.readFile().toString() );
   }


   /** */
   private void cancelAndClose() {

      if( yesNo("Отменить и закрыть редактор?") )
      {
         saveFile(null);
         retValue.set(false);
         getStage().close();
      }
   }


   /** */
   private void saveAndClose() {
      if( yesNo("Сохранить и закрыть редактор?") )
      {
         saveFile(null);
         retValue.set(true);
         getStage().close();
      }
   }


   /** */
   public static boolean showEditor( Stage primaryStage, ALTDoc altDoc )
   {
      try {

         final AtomicBoolean retValue = new AtomicBoolean(false);

         Stage editStage = new Stage();
         editStage.initModality(Modality.WINDOW_MODAL);
         editStage.initOwner   (primaryStage);

         FXMLLoader loader = new FXMLLoader();
         loader.setLocation ( FruApp.class.getResource("fxml/FruEdit.fxml"));
         loader.setResources( ResourceBundle.getBundle("ru.inversion.fru.print.altviewer.res.gui"));
         loader.setControllerFactory(new Callback<Class<?>, Object>() {
            @Override
            public Object call(Class<?> type) {
               try {
                  FruEditController controller = (FruEditController) type.newInstance();
                  controller.retValue = retValue;
                  controller.altDoc   = altDoc;
                  return controller;
               }
               catch ( Throwable th ) {
                  throw new RuntimeException(th);
               }
            }
         });

         Parent root = loader.load();
         Scene scene = new Scene(root);
         editStage.setScene(scene);
         editStage.showAndWait();

         return retValue.get();

      } catch( Exception e ) {
         handleException( primaryStage, e );
      }

      return false;
   }


}
