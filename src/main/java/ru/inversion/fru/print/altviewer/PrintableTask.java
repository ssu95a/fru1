package ru.inversion.fru.print.altviewer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.fru.print.altprint.AltPrintExecutionService;
import ru.inversion.fru.print.altprint.IAltPrintListener;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * JavaFX background-task для запуска печати документа из viewer.
 *
 * Отвечает за:
 * - показ/закрытие progress dialog
 * - запуск печати в фоне
 * - проброс сообщений прогресса в UI
 * - обработку финального результата/ошибки
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - выбор backend-а печати (matrix / graphic)
 * - расчёт PageFormat и print-attributes
 * - layout документа
 * - shrink логики страницы
 */
public class PrintableTask extends Task<Boolean> implements IAltPrintListener {

    private final PrintAwtContext printContext;
    private final AltPrintExecutionService executionService = new AltPrintExecutionService();

    private Dialog<Boolean> progressDialog;

    public PrintableTask(PrintAwtContext printContext) {
        this.printContext = printContext;

        setOnSucceeded(e -> closeProgressDialog(true));
        setOnFailed(e -> handleException());
        setOnCancelled(e -> closeProgressDialog(false));
    }

    private void handleException() {
        FruViewController.handleException(printContext.getWindow(), getException());
        closeProgressDialog(false);
    }

    private void showProgressDialog() {
        progressDialog = new Dialog<Boolean>();
        progressDialog.initOwner(printContext.getWindow());
        progressDialog.setTitle("Выполняется печать документа");
        progressDialog.setHeaderText(printContext.getAltDoc().getAltFile().toString());
        progressDialog.initModality(Modality.WINDOW_MODAL);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(500);
        progressBar.progressProperty().bind(progressProperty());

        Label messageLabel = new Label();
        messageLabel.textProperty().bind(messageProperty());

        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        progressDialog.getDialogPane().getButtonTypes().add(cancelButton);

        Button cancelBtn = (Button) progressDialog.getDialogPane().lookupButton(cancelButton);
        cancelBtn.setOnAction(e -> cancel());

        VBox content = new VBox(10, messageLabel, progressBar);
        content.setStyle("-fx-padding: 10;");

        progressDialog.getDialogPane().setContent(content);
        progressDialog.setOnCloseRequest(e -> cancel());
        progressDialog.setResultConverter(buttonType -> Boolean.FALSE);

        progressDialog.show();
    }

    private void closeProgressDialog(boolean success) {
        Dialog<Boolean> dlg = progressDialog;
        progressDialog = null;

        if (dlg != null && dlg.isShowing()) {
            dlg.setResult(Boolean.valueOf(success));
            dlg.close();
        }
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Запустить печать документа через общий execution service.
     */
    private void doPrint() throws Exception {
        Platform.runLater(this::showProgressDialog);

        executionService.print(
                printContext.getAltDoc(),
                printContext.getAwtPrinter(),
                this
        );
    }

    @Override
    protected Boolean call() throws Exception {
        try {
            doPrint();
            return Boolean.TRUE;
        } catch (Exception e) {
            throw new ALTPrintException("Ошибка при печати документа", e) {
                @Override
                public String getDetailedMessage() {
                    return printContext.getAltDoc().getAltFile().toString();
                }
            };
        }
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
    public void onFinalPrint(Exception ex) {
        Platform.runLater(() -> {
            Alert alert;
            if (ex == null) {
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
            alert.initOwner(printContext.getWindow());
            alert.showAndWait();
        });
    }
}