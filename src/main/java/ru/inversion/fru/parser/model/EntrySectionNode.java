package ru.inversion.fru.parser.model;

import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.sections.FruSection;
import ru.inversion.fru.parser.exceptions.FruBadHeaderFormatException;
import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.utils.S;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntrySectionNode extends AbstractSectionNode {

    /** entry */
    protected static final Pattern ENTRY_PATTERN = Pattern.compile ("#?entry\\(\\s*(?<fields>.*?)\\s*\\);?" );

    /** */
    public Map<String,String> formats;

    /** */
    private Map<String,String> strings;

    /** */
    private Map<String,String> parameters;

    /** */
    public EntrySectionNode( String h, int b, int e  ) {
        super( 0, h, b, e );
    }

    /** */
    @Override
    public SectionTypeEnum getType( ) {
        return SectionTypeEnum.ENTRY;
    }

    /** */
    @Override
    public boolean isEntry( ) { return true; }

    /** */
    public void addParameter( String name, String value )
    {
        if( parameters == null )
            parameters = new HashMap<>();

        parameters.put( name, value );
    }

    /** */
    public void addFormat( String name, String format )
    {
        if( formats == null )
            formats = new LinkedHashMap<>();

        formats.put( name, format );
    }

    /** */
    public void addString( String name, String text )
    {
        if( strings == null )
            strings = new LinkedHashMap<>();

        strings.put( name, text );
    }

    /** */
    @Override
    protected FruSection parseHeader( FruBuilder fruBuilder )
    {
        if( S.isNullOrEmpty(header) )
            return null;

        final Matcher matcher = ENTRY_PATTERN.matcher( header );

        if( !matcher.find() )
            throw new FruBadHeaderFormatException( "Не корректный формат заголовка #entry", header, getOrderNum() );

        String as = matcher.group("fields");

        final List<String> fieldList = Arrays.stream(as.split(",")).map(String::trim).filter(S::isNotNullOrEmpty).collect(Collectors.toList());

        if(!fieldList.isEmpty() )
            fruBuilder.initArgumentList(fieldList);

        if( formats != null )
            formats.forEach( (k,v)->fruBuilder.addFormat( k, FruFormat.make(v) ) );

        if( strings != null )
            strings.forEach( fruBuilder::addString );

        if( parameters != null )
            parameters.forEach( fruBuilder::addParameter );

        if( lines != null )
            lines.stream().filter(sc->sc instanceof FruScript).findFirst().ifPresent(scr->fruBuilder.initScript( (FruScript)scr ) );

        return null;
    }

    /** */
    @Override
    protected void parseContent( FruBuilder fruBuilder, FruSection section ) { }
}
