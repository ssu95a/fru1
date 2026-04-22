package ru.inversion.fru.print.altprint;

import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.fru.print.altprint.doc.ALTDoc;
import ru.inversion.fru.print.naltprn.AltSettings;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.Optional;

/**
 * ЗОНА ОТВЕТСТВЕННОСТИ:
 * Фасад уровня приложения для печати ALT-документа.
 *
 * Отвечает за:
 * - поиск принтера
 * - вызов общего execution service
 *
 * НЕ ОТВЕЧАЕТ ЗА:
 * - фактическое исполнение печати
 * - PageFormat / print-attributes
 * - layout документа
 * - shrink логики страницы
 */
public class AltPrinter {

    private final AltPrintExecutionService executionService = new AltPrintExecutionService();

    public AltPrinter() {
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Напечатать документ на принтер по индексу из конфигурации.
     */
    public void print(ALTDoc doc, IAltPrintListener listener) throws Exception {
        PrintService printer =
                findAWTPrinterByIndex(FruEngineConfig.instance().getPrinterIndex())
                        .orElseThrow(() ->
                                new ALTPrintException(
                                        "Невозможно определить принтер по переданному индексу: "
                                                + FruEngineConfig.instance().getPrinterIndex()
                                )
                        );

        executionService.print(doc, printer, listener);
    }

    /**
     * ЗОНА ОТВЕТСТВЕННОСТИ:
     * Напечатать документ на уже выбранный принтер.
     */
    public void print(ALTDoc doc, IAltPrintListener listener, PrintService printer) throws Exception {
        if (printer == null) {
            throw new ALTPrintException("Принтер не задан");
        }

        executionService.print(doc, printer, listener);
    }

    /**
     * Проверка, относится ли принтер к matrix backend.
     */
    public static boolean isMatrix(PrintService awtPrinter) {

        if( awtPrinter == null)
            return false;

        return AltSettings.INSTANCE().isMatrixPrinter(awtPrinter.getName());
    }

    /**
     * Найти AWT-принтер по имени.
     */
    public static Optional<PrintService> findAWTPrinterByName(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService service : services) {
            if (service.getName().equals(printerName)) {
                return Optional.of(service);
            }
        }

        return Optional.empty();
    }

    /**
     * Найти AWT-принтер по индексу.
     */
    public static Optional<PrintService> findAWTPrinterByIndex(int index) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        if (index < 0 || index >= services.length) {
            ALTLog.error(String.format(
                    "Ошибочное значение индекса принтера: %d, всего доступно принтеров: %d",
                    index,
                    services.length
            ));
            ALTLog.warning("Будет использован принтер по умолчанию");

            return Optional.ofNullable(PrintServiceLookup.lookupDefaultPrintService());
        }

        return Optional.of(services[index]);
    }
}