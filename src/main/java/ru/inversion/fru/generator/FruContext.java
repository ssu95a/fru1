package ru.inversion.fru.generator;

import ru.inversion.fru.data.FruData;
import ru.inversion.fru.data.FruDataFile;
import ru.inversion.fru.data.FruDataRow;
import ru.inversion.fru.generator.renderer.Renderers;
import ru.inversion.fru.model.Fru;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.script.FruScriptContext;
import ru.inversion.fru.model.sections.FruSection;
import ru.inversion.property.IProperty;
import ru.inversion.utils.S;


import javax.script.*;

import java.util.Set;
import java.util.function.Function;

/** */
public class FruContext implements AutoCloseable {

    private final Renderers renderers = new Renderers();

    /** */
    final private ScriptEngine scriptEngine;

    /** */
    final private Fru fru;

    /** */
    final private FruWriter writer;

    /** */
    final private FruScriptContext globalScriptContext;

    /** */
    final private FruData data;

    /** */
    private FruSection currentSection;

    /** */
    public FruContext( Fru fru, FruWriter writer, FruDataFile dataFile ) {

        this.fru    = fru;
        this.data   = new FruData(dataFile);
        this.writer = writer;

        this.data.rowProperty().addListener(new IProperty.ChangeListener<FruDataRow>() {
            @Override
            public void changed(IProperty<? extends FruDataRow> property, FruDataRow oldValue, FruDataRow newValue) {
                setCurrentRow( newValue );
            }
        });

        final ScriptEngineManager engineManager = new ScriptEngineManager();
        scriptEngine = engineManager.getEngineByName("bil");

        globalScriptContext = new FruScriptContext();
        globalScriptContext.setWriter( this.writer );

        scriptEngine.setContext( globalScriptContext );
    }

    /** */
    public Renderers renderers() { return renderers; }

    /** */
    public ScriptContext globalScriptContext() {
        return globalScriptContext;
    }

    /** */
    private void runEntry(FruDataRow row) {

        if( row.getSectionNum() == 0 ) {
            int i = 0;

            for( String s : row.data() )
                 setArgumentValue( i++, s );
        }

        FruScript script = fru.initScript( );

        if( script == null )
            return;

        executeScript(script);
    }

    /** */
    private int rowCount = 0;

    /** */
    private void setCurrentRow( FruDataRow row )
    {

        if( row.getSectionNum() == 0 )
        {
            runEntry(row);
        }
        else
        {
            if( rowCount == 0 )
                runEntry(row);

            if( currentSection == null || currentSection.getNum() != row.getSectionNum() )
            {

                if( currentSection != null )
                    currentSection.afterUse( this );

                currentSection = fru.sections()
                                        .stream().filter(t -> t.getNum() == row.getSectionNum())
                                            .findFirst().orElseThrow(() -> new RuntimeException( "Не найдена секция с номером " + row.getSectionNum()) );

                currentSection.beforeUse( this );
            }

            if( currentSection != null )
                renderers.render( this, currentSection );

            rowCount++;

            if( rowCount == fru.getLines() )
            {
                printPageNum();
            }
        }
    }

    int pageNum = 0;

    /** */
    private void printPageNum( )
    {
        pageNum ++;

    }

    /** */
    public int getCurrentPageNum( )
    {
        return pageNum;
    }

    /** */
    public FruWriter writer() {
        return (FruWriter) globalScriptContext().getWriter();
    }

    /** */
    public FruFormat getFormat( String name ) {
        return fru.formats().get(name);
    }

    /** */
    public String getString(String name) {
        return fru.strings().get(name);
    }

    /** */
    public String getArgument(String name) {
        return globalScriptContext.getAttribute(name, ScriptContext.GLOBAL_SCOPE).toString();
    }

    /** */
    public int getWidth() {
        return fru.getWidth();
    }

    /** */
    public FruData data() {
        return data;
    }

    /** */
    public void setArgumentValue( int num, String value) {
        final String arg = fru.arguments().get(num);
        if (!S.isNullOrEmpty(arg))
            globalScriptContext.setAttribute( arg, value, ScriptContext.GLOBAL_SCOPE );
    }

    /** */
    public Set<Character> excludeSymbols() {
        return fru.excludeSymbols();
    }

    /** */
    public FruSection currentSection() {
        return currentSection;
    }

    /** */
    @Override
    public void close() throws Exception {
        writer.close();
    }

    /** */
    public int getCurrentPosition() {
        return writer.getCurrentCharInLine();
    }

    /** */
    public void executeScript(FruScript script) {

        try {

            if( currentSection != null )
            {
                globalScriptContext.setValuesSupplier(new Function<String, Object>() {
                    @Override
                    public Object apply(String name) {
                        return currentSection.getFieldValue(FruContext.this, name);
                    }
                });
            }

            scriptEngine.eval( script.getBody() );

        } catch( ScriptException e ) {
            throw new RuntimeException(e);
        }
        finally {
            if( globalScriptContext != null )
                globalScriptContext.clearValuesSupplier();
        }
    }
}

