package ru.inversion.fru.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fru.data.FruData;
import ru.inversion.fru.data.FruDataFile;
import ru.inversion.fru.data.FruDataRow;
import ru.inversion.fru.generator.exceptions.FruScriptException;
import ru.inversion.fru.generator.renderer.FruLineRenderSession;
import ru.inversion.fru.generator.renderer.LocalSplitState;
import ru.inversion.fru.generator.renderer.Renderers;
import ru.inversion.fru.model.Fru;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.fields.functions.BuiltinFunctionEnum;
import ru.inversion.fru.model.fields.types.FruFieldVal;
import ru.inversion.fru.model.fields.types.grp.DefaultFruLineFieldExtractor;
import ru.inversion.fru.model.fields.types.grp.FruFieldGrpPlan;
import ru.inversion.fru.model.fields.types.grp.FruFieldGrpPlanner;
import ru.inversion.fru.model.fields.types.grp.FruFieldGrpRuntimeRegistry;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.items.FruPaging;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.script.FruScriptContext;
import ru.inversion.fru.model.sections.FruSection;
import ru.inversion.property.IProperty;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

import javax.script.*;

import java.io.Writer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static javax.script.ScriptContext.GLOBAL_SCOPE;

/** */
public class FruContext implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FruContext.class);

    private final Renderers renderers = new Renderers();

    /** */
    final private ScriptEngine scriptEngine;

    /** */
    final private Fru fru;

    /** */
    private final FruWriter writer;

    /** */
    final private FruScriptContext globalScriptContext;

    /** */
    final private FruData data;

    /** */
    private FruSection currentSection;

    private FruLineRenderSession lineRenderSession;

    /** */
    private final Map<String, LocalSplitState> localSplitStates = new HashMap<>();

    private final FruFieldGrpRuntimeRegistry fieldGrpRuntimeRegistry = new FruFieldGrpRuntimeRegistry();


    public void setFieldGrpPlan(FruFieldGrpPlan plan) {
        fieldGrpRuntimeRegistry.setPlan(plan);
    }

    public boolean hasFieldGroup(FruField field) {
        return fieldGrpRuntimeRegistry.hasGroup(field);
    }

    public String renderFieldGroupSlot(FruField field) {
        return fieldGrpRuntimeRegistry.render(this, field);
    }

    //private List<Integer> numSectionsUse;

    /** */
    public FruContext( Fru fru, Writer output, FruDataFile dataFile ) {

        this.fru  = fru;
        this.data = new FruData(dataFile);

//        numSectionsUse = new ArrayList<>( this.fru.sections().size() );
//        fru.sections().values().forEach( s-> numSectionsUse.add(s.getNum()) );

        this.data.rowProperty().addListener(new IProperty.ChangeListener<FruDataRow>() {
            @Override
            public void changed(IProperty<? extends FruDataRow> property, FruDataRow oldValue, FruDataRow newValue) {
                // renderMissingSectionsBefore(newValue);
                setCurrentRow( newValue );
            }
        });

        final ScriptEngineManager engineManager = new ScriptEngineManager();
        scriptEngine = engineManager.getEngineByName("bil");

        final FruPaging paging = fru.getPaging();

        FruWriter fruWriter;

        if( paging != null )
        {
            fruWriter = new FruWriter(output, paging, new BiConsumer<Integer, FruPaging>() {
                @Override
                public void accept( Integer pageNum, FruPaging paging ) {
                    renderers.render( FruContext.this, paging );
                }
            });
        }
        else
            fruWriter = new FruWriter(output);

        this.writer = fruWriter;

        globalScriptContext = new FruScriptContext();
        globalScriptContext.setWriter( this.writer );

        scriptEngine.setContext( globalScriptContext );
    }

    public void clearLocalSplitState() {
        localSplitStates.clear();
        fieldGrpRuntimeRegistry.clearRecordLocalState();

    }

    public LocalSplitState findLocalSplitState(FruField f) {
        return localSplitStates.get(f.getKey());
    }

    /** */
    public LocalSplitState getOrCreateLocalSplitState(FruField f) {
       //System.out.println( "createLocalSplitState: " + f );
       return localSplitStates.computeIfAbsent(f.getKey(), k -> new LocalSplitState());
    }


    /** */
    public FruLineRenderSession getLineRenderSession() {
        return lineRenderSession;
    }

    public void setLineRenderSession(FruLineRenderSession lineRenderSession) {
        this.lineRenderSession = lineRenderSession;
    }

    /** */
    public Renderers renderers() { return renderers; }

    /** */
    public ScriptContext globalScriptContext() {
        return globalScriptContext;
    }

    /** */
    private void runEntry( FruDataRow row )
    {
        entryExecuted = true;

        if( row != null)
        {
            int i = 0;

            for( String s : row.data() )
                 setArgumentValue( i++, s );
        }

        final FruScript script = fru.initScript();

        if( script != null )
            executeScript( script );
    }

    /** */
    private int rowCount = 0;

    /** */
    private boolean entryExecuted = false;

    /** */
    final private Set<Integer> missingSectionsSet = new TreeSet<>();


    private final FruFieldGrpPlanner fieldGrpPlanner = new FruFieldGrpPlanner(new DefaultFruLineFieldExtractor());

    /** */
    private void setCurrentRow( FruDataRow row )
    {
        if( !entryExecuted )
        {
            if( row.getSectionNum() == -1 )
            {
                runEntry( row );
                return; // entry не рендерим
            }

            // entry отсутствует — всё равно считаем его завершённым
            runEntry( null );
        }

        clearLocalSplitState();

        if( currentSection == null || currentSection.getNum() != row.getSectionNum() )
        {
            if( currentSection != null )
                currentSection.afterUse( this );

            currentSection = fru.sections().get( row.getSectionNum() );

            if( currentSection != null ) {
                FruFieldGrpPlan grpPlan = fieldGrpPlanner.plan(currentSection);
                setFieldGrpPlan(grpPlan);
                currentSection.beforeUse(this);
            }
            else
            {
                if( missingSectionsSet.add(row.getSectionNum() ) )
                    log.warn("В файле формы не найдена секция с номером {}", row.getSectionNum() );
            }
        }

        if( currentSection != null )
            renderers.render( this, currentSection );

        rowCount++;
    }

    /** */
    public int getCurrentPageNum( )
    {
        return writer.getCurrentPage();
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
    public String getArgument( String name )
    {
        Object v = globalScriptContext.getAttribute( name, GLOBAL_SCOPE );
        return v == null ? null : String.valueOf(v);
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
    public void setArgumentValue( int num, String value ) {

        final String arg = fru.arguments().get(num);

        if (!S.isNullOrEmpty(arg) )
            globalScriptContext.setAttribute( arg, value, GLOBAL_SCOPE );
    }

    /** */
    public Set<Character> excludeSymbols() {
        return fru.excludeSymbols();
    }

    /** */
    public FruSection currentSection() {
        return currentSection;
    }
    /*
    private void renderRemainingMissingSections() {
        while (!numSectionsUse.isEmpty()) {
            int sectionNum = numSectionsUse.remove(0);

            setCurrentRow(new FruDataRow(
                    sectionNum,
                    fru.getSectionPlaceholderRow(sectionNum)
            ));
        }
    }
    */

    /** */
    @Override
    public void close() throws Exception {

        try {

            // renderRemainingMissingSections();

            if( currentSection != null )
            {
                currentSection.afterUse(this);
                currentSection = null;
            }
        }
        finally {
            writer.close();
        }
    }

    /** */
    public int getCurrentPosition() {
        return writer.getCurrentCharInLine();
    }

    /** */
    public void executeScript( FruScript script )
    {
        try {

            if( currentSection != null ) {
                globalScriptContext.setValuesSupplier(new Function<String, Pair<Object, Boolean>>() {
                    @Override
                    public Pair<Object, Boolean> apply(String name) {

                        Object fieldValue = currentSection.getFieldValue(FruContext.this, name );

                        if( fieldValue == null && currentSection.getFieldNum(name) == -1 )
                        {
                            BuiltinFunctionEnum f = BuiltinFunctionEnum.find(name);
                            if( f != null )
                                fieldValue = f.execute( FruContext.this, null );
                        }

                        return Pair.makePair( fieldValue, script.hasImportArg(name) );
                    }
                });
            }
writer.marker = "scriptEngine";

                script.beforeExecute();

                try {
                    scriptEngine.eval( script.getBody());
                }
                finally {
                    script.afterExecute();
                }

writer.marker = S.EMPTY_STRING;

        } catch( ScriptException e ) {
            throw new FruScriptException( "Ошибка при выполнении скрипта", e, script.getBody() );
        }
        finally {

            if( globalScriptContext != null )
                globalScriptContext.clearValuesSupplier();

        }
    }
}

