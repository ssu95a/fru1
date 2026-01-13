package ru.inversion.fru.print.altprint;

/** */
public interface IAltPrintListener {

    void onBeginPrint( );

    void onEndPrint( );

    void onPagePrinted( int pageIndex );

    boolean isCancelled();

    void onFinalPrint(Exception ex);
}
