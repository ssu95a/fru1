package ru.inversion.fru.print.altviewer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import ru.inversion.fru.print.altprint.ALTDocPrintable;
import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.utils.U;


import javax.print.attribute.standard.OrientationRequested;
import javax.swing.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/** */
public class PrintableTask extends Task<Boolean> {

    final private PrintAwtContext printContext;

    private Dialog<Boolean> progressDialog;
    private final CountDownLatch dialogShownLatch = new CountDownLatch(1);
    private final CountDownLatch completedPrintLatch = new CountDownLatch(1);

    private volatile boolean dialogRun = false;

    /** */
    public PrintableTask( PrintAwtContext printContext ) {
        this.printContext = printContext;
    }

    /** */
    private void showProgressDialog( )
    {
        try {

            progressDialog = new Dialog<>();
            progressDialog.initOwner     ( printContext.getWindow() );
            progressDialog.setTitle      ( "Печать" );
            progressDialog.setHeaderText ( "Выполняется печать документа" );
            progressDialog.setContentText( printContext.getAltDoc().getAltFile().toString() );
            progressDialog.initModality ( Modality.WINDOW_MODAL );

            final ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefWidth(300);
            progressBar.progressProperty().bind(progressProperty());

            final Label messageLabel = new Label();
            messageLabel.textProperty().bind( messageProperty() );

            // Кнопка отмены
            ButtonType cancelButton = new ButtonType( "Отмена", ButtonBar.ButtonData.CANCEL_CLOSE );
            progressDialog.getDialogPane().getButtonTypes().add(cancelButton);

            Button cancelBtn = (Button) progressDialog.getDialogPane().lookupButton(cancelButton);
            cancelBtn.setOnAction(e -> {
                if( isRunning() ) {
                    cancel();
                }
            });

            VBox content = new VBox(10, messageLabel, progressBar );
            content.setStyle("-fx-padding: 20;");

            progressDialog.getDialogPane().setContent( content );

            progressDialog.setOnShown(e -> {
                dialogShownLatch.countDown();
                dialogRun = true;
            });

            progressDialog.setOnCloseRequest(e -> {
                if( isRunning() ) {
                    cancel();
                }
            });

            progressDialog.setResultConverter(buttonType -> false);

            progressDialog.show();
        }
        catch( Exception e ) {
            dialogRun = false;
            dialogShownLatch.countDown();
            throw e;
        }
    }

    private void closeProgressDialog( boolean success )
    {
        if( progressDialog != null && progressDialog.isShowing() )
        {
            progressDialog.setResult(success);
            progressDialog.close();
            progressDialog = null;
        }
        completedPrintLatch.countDown();
    }

    /** */
    private void printMatrix( ) throws Exception {
        final ALTDocPrintable altDocPrintable = printContext.getAltDoc().makePrintable();
        altDocPrintable.printToMatrix( printContext.getAwtPrinter() );
    }

    /** */
    private Void printGraphic( ) throws Exception {

        PrinterJob awtJob = PrinterJob.getPrinterJob();

        PageFormat pageFormat = awtJob.defaultPage();
        if(
            (pageFormat.getOrientation() == 1 && printContext.getAltDoc().getOrientation() == OrientationRequested.PORTRAIT)
            ||
            (pageFormat.getOrientation() == 0 && printContext.getAltDoc().getOrientation() == OrientationRequested.LANDSCAPE)
        )
            ;
        else
            pageFormat.setOrientation( U.decode(printContext.getAltDoc().getOrientation(), OrientationRequested.LANDSCAPE, 0, 1) );


        awtJob.setJobName("ALT: " + printContext.getAltDoc().getAltFile() );

        updateMessage( "Подготовка данных для печати ... " );

        final ALTDocPrintable printable = printContext.getAltDoc().makePrintable();

        updateMessage( "Завершена" );
        updateMessage( "Печать" );

        awtJob.setCopies      ( printContext.getAltDoc().getCopies().getValue() );
        awtJob.setPrintService( printContext.getAwtPrinter() );
        awtJob.setPrintable   ( printable, pageFormat );
        awtJob.print();

        return null;
    }


    /** */
    private boolean print() throws Exception
    {
        try {

            Platform.runLater( this::showProgressDialog );

            dialogShownLatch.await();

            if( !dialogRun )
                 throw new ALTPrintException("Не удалось показать диалог печати");

            if( printContext.isMatrixPrinter() )
                printMatrix( );
            else
                PrintableTask.invokeAndWait( this::printGraphic );

            return true;

        } catch( Exception e ) {
            throw new ALTPrintException( "ошибка при печати документа", e );
        }
        finally
        {
            Platform.runLater( () -> closeProgressDialog( getException() == null ) );
            completedPrintLatch.await();
        }
    }


    /** */
    private static void invokeAndWait( Callable<Void> task) throws Exception {

        final Exception[] exception = new Exception[1];
        final CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            try {
                task.call();
            } catch (Exception e) {
                exception[0] = e;
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        if( exception[0] != null )
            throw exception[0];
    }


    @Override
    protected Boolean call() throws Exception
    {
        return print();
    }

}
