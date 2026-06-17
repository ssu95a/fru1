package ru.inversion.fru.print.altviewer;

import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class FruControllerBase implements Initializable {

   protected static final Logger log = getLogger( MethodHandles.lookup().lookupClass() );

   /** Доступные кодировки */
   public static final Charset[] charsets = {
           Charset.forName("windows-1251"),
           Charset.forName("IBM866"),
           StandardCharsets.UTF_8
   };

   final protected GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

   /** */
   static public void handleException(Window window, Throwable th )
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
   protected abstract Stage getStage();

   /** */
   protected void setTitle( String title )
   {
      getStage().setTitle( title );
   }

   /** */
   protected boolean yesNo( String text )
   {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.initOwner( getStage() );
      alert.initModality(Modality.APPLICATION_MODAL);
      alert.setHeaderText(text);

      return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
   }
}
