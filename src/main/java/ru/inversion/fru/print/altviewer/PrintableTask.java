package ru.inversion.fru.print.altviewer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import ru.inversion.fru.print.altprint.doc.ALTDocPrintable;
import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.fru.print.altprint.IAltPrintListener;
import ru.inversion.utils.Holder;


import javax.swing.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/** */
public class PrintableTask extends Task<Boolean> implements IAltPrintListener {

    final private PrintAwtContext printContext;

    private Dialog<Boolean> progressDialog;

    /** */
    public PrintableTask( PrintAwtContext printContext ) {

        this.printContext = printContext;

        setOnSucceeded(e -> closeProgressDialog(true) );
        setOnFailed   (e -> handleException());
        setOnCancelled(e -> closeProgressDialog(false));
    }

    /** */
    private void handleException() {
        FruViewController.handleException(printContext.getWindow(), getException() );
        closeProgressDialog(false);
    }

    /** */
    private void showProgressDialog( )
    {
        try {

            progressDialog = new Dialog<>();
            progressDialog.initOwner     ( printContext.getWindow() );
            progressDialog.setTitle      ( "Выполняется печать документа" );
            progressDialog.setHeaderText ( printContext.getAltDoc().getAltFile().toString() );
            progressDialog.initModality  ( Modality.WINDOW_MODAL );

            final ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefWidth(500);
            progressBar.progressProperty().bind(progressProperty());

            final Label messageLabel = new Label();
            messageLabel.textProperty().bind( messageProperty() );

            // Кнопка отмены
            ButtonType cancelButton = new ButtonType( "Отмена", ButtonBar.ButtonData.CANCEL_CLOSE );
            progressDialog.getDialogPane().getButtonTypes().add(cancelButton);

            Button cancelBtn = (Button) progressDialog.getDialogPane().lookupButton(cancelButton);
            cancelBtn.setOnAction(e ->  cancel() );

            VBox content = new VBox(10, messageLabel, progressBar );
            content.setStyle("-fx-padding: 10;");

            progressDialog.getDialogPane().setContent( content );

            progressDialog.setOnCloseRequest(e -> cancel() );
            progressDialog.setResultConverter(buttonType -> false);

            progressDialog.show();
        }
        catch( Exception e ) {
            throw e;
        }
    }
    /** */
    private void closeProgressDialog( boolean success )
    {
        final Dialog<Boolean> dlg = progressDialog;
        progressDialog = null;
        if (dlg != null && dlg.isShowing()) {
            dlg.setResult(success);
            dlg.close();
        }
    }

    /** */
    private void printMatrix( ) throws Exception {
        try( ALTDocPrintable altDocPrintable = printContext.getAltDoc().makePrintable(this, printContext.getPageConfig() ) ) {
             altDocPrintable.printToMatrix(printContext.getAwtPrinter());
        }
    }

    /** */
    private Void printGraphic( ) throws Exception {


        try( ALTDocPrintable printable = printContext.getAltDoc().makePrintable( this, printContext.getPageConfig()) ) {

            final PrinterJob awtJob = PrinterJob.getPrinterJob();

            PageFormat pf = awtJob.defaultPage();
            //PageFormat pf = printContext.getPageConfig().merge(pageFormat);

            awtJob.setJobName("ALT: " + printContext.getAltDoc().getAltFile());

            updateMessage("Подготовка данных для печати ... ");

            awtJob.setCopies(printContext.getAltDoc().getCopies().getValue());
            awtJob.setPrintService(printContext.getAwtPrinter());
            awtJob.setPrintable(printable, pf);

            updateMessage("Передача страниц на печать ...");
            awtJob.print();

            return null;
        }
    }


    /** */
    private static void invokeAndWait( Callable<Void> task) throws Exception {

        final Exception[] exception = new Exception[1];
        final CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
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

    /** */
    private boolean print() throws Exception
    {
        Platform.runLater( this::showProgressDialog );

        if( printContext.isMatrixPrinter() )
            printMatrix( );
        else
            printGraphic();
        //PrintableTask.invokeAndWait( this::printGraphic );

        return true;
    }


    @Override
    protected Boolean call() throws Exception {

        Holder<Boolean> success = new Holder<>(false);

        try {

            print();

            success.set(true);

            return true;

        }
        catch ( Exception e ) {
            throw new ALTPrintException( "Ошибка при печати документа", e ) {
                @Override
                public String getDetailedMessage() {
                    return printContext.getAltDoc().getAltFile().toString();
                }
            };
        }
//        finally
//        {
//            onFinalPrint(success.get());
//            Platform.runLater(() -> closeProgressDialog(success.get()));
//        }
    }

    @Override
    public void onBeginPrint() {
        updateMessage("Начало печати");
    }

    @Override
    public void onEndPrint() {
        updateMessage("Ожидание завершения печати принтером!");
    }

    @Override
    public void onPagePrinted(int pageIndex) {
        updateMessage("Передача страницы " + (pageIndex + 1));
    }

    @Override
    public void onFinalPrint( Exception ex ) {

        Platform.runLater(() -> {
            final Alert alert;
            if(ex == null) {
                alert = new Alert(
                        Alert.AlertType.INFORMATION,
                        printContext.getAltDoc().getAltFile().toString(),
                        ButtonType.OK
                );
                alert.setHeaderText("Документ отправлен на печать");
                alert.setTitle("Сообщение");
            } else {
                alert = new Alert(
                        Alert.AlertType.ERROR,
                        ex.getLocalizedMessage(),
                        ButtonType.OK
                );
                alert.setHeaderText("Ошибка при печати документа");
                alert.setTitle("Ошибка");
            }
            alert.initOwner( printContext.getWindow());
            alert.showAndWait();
        });
    }
}
