package ru.inversion.fru.model;

import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.sections.FruSection;
import ru.inversion.fru.model.sections.FruSectionHeader;
import ru.inversion.fru.model.sections.FruSectionTable;
import ru.inversion.fru.model.sections.FruSectionTail;

import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static  ru.inversion.fru.utils.constants.SectionTypeEnum.*;

/** */
public class FruBuilder {

    final private Path fruFile;
    final private Charset fruCharset;

    final public Map<String, FruFormat> formats = new HashMap<>();
    final public Map<String, String>    strings = new HashMap<>();

    final public Map<String,Object>     parameters = new HashMap<>();

    public List<String> argumentList = Collections.emptyList();

    /** */
    private FruScript initScript;

    /** */
    final private LinkedList<FruSection> sections = new LinkedList<>();


    /** */
    public FruBuilder( Path f, Charset charset )
    {
        this.fruFile = f;
        this.fruCharset = charset;
    }


    /** */
    public Path fruFile( ) {
        return fruFile;
    }


    /** */
    public FruBuilder initArgumentList( List<String> l ) {
        argumentList = l;
        return this;
    }


    /** */
    public FruBuilder initScript( FruScript script ) {
        initScript = script;
        return this;
    }


    /** */
    public FruBuilder addFormat( String name, FruFormat value )
    {
        formats.put( name, value );
        return this;
    }


    /** */
    public FruBuilder addString( String name, String value )
    {
        if( !S.isNullOrEmpty(name) )
            strings.put( name, value );
        return this;
    }


    /** */
    public FruBuilder addParameter( String name, String value )
    {
        if( !S.isNullOrEmpty(name) )
            parameters.put( name, value );

        return this;
    }


    /** */
    public FruBuilder addSection( FruSection section )
    {
        if( section == null )
            throw new NullPointerException("addSection: 'section' is null");

        sections.add(section);

        return this;
    }


    /** */
    public Fru build() throws Exception
    {
        //sections.sort( Comparator.comparingInt(FruSection::getNum) );
        sections.sort(new Comparator<FruSection>() {
            @Override
            public int compare( FruSection o1, FruSection o2 ) {
                try {
                    return Integer.compare(o1.getNum(), o2.getNum());
                }
                catch ( Throwable th ) {
                    throw new RuntimeException("Error on sort sections. o1 = " + o1  + ", o2 = " + o2, th );
                }
            }
        });

        List<FruSectionTable> tables = sections.stream()
                .filter( t->t.getType() == TABLE )
                    .map(t->(FruSectionTable)t).collect( Collectors.toList() );

        for( FruSectionTable t : tables ) {

             sections
                .stream()
                    .filter( s->s.getType() == TAIL && s.getNum() == t.getNum() )
                        .findFirst().ifPresent( tail->t.setTail((FruSectionTail)tail) );
             sections
                .stream()
                    .filter( s->s.getType() == HEAD && s.getNum() == t.getNum() )
                        .findFirst().ifPresent( head->t.setHeader((FruSectionHeader)head) );
        }

        sections.removeIf( t-> U.in( t.getType(), HEAD, TAIL ) );

        //sections.forEach( f->{if( f instanceof FruSectionTable ) ((FruSectionTable) f).linkFields();} );

        final Fru fru = new Fru (
            fruFile,
            formats, strings, parameters, sections, U.nvl( argumentList, Collections.emptyList() ), initScript
        );

        return fru;
    }

}
