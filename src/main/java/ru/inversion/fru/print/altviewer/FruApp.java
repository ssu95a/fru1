package ru.inversion.fru.print.altviewer;

import ru.inversion.fru.api.FruEngineConfig;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.utils.MemoryURL;
import ru.inversion.fru.print.altprint.ALTDoc;
import ru.inversion.fru.print.altprint.ALTLog;

import java.net.URL;
import java.util.Collections;

/** */
public class FruApp extends BaseApp {

    @Override
    public String getAppID() {
        return "XXI.Fru";
    }

    public void init() {

        try {
            URL.setURLStreamHandlerFactory(protocol ->"memory".equals(protocol) ? new MemoryURL.MemoryStreamHandler() : null );
        } catch( Throwable th ) {
            System.out.println("URLStreamHandlerFactory already registered");
        }

        super.init();
    }

    @Override
    protected void showMainWindow( )
    {
        try {
            FruViewController.showViewer( getPrimaryViewContext(), null, Collections.emptyMap() );
        }
        catch( Throwable th ) {
            JInvErrorService.handleException( getPrimaryViewContext(), th );
        }
    }

    /** */
    public static void showReport( ViewContext vc, ALTDoc altDoc, FruEngineConfig config )
    {
        try {
            FruViewController.showViewer( vc, null, Collections.emptyMap() );
        } catch (Exception ex) {
            ALTLog.tech_error("Ошибка при печати файла: " + altDoc.getAltFile(), ex);
        }
    }

    /** */
    public static void main( String[] args)
    {
        launch (args);
    }

}
