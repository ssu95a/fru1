package ru.inversion.fru.parser.model;

import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.sections.*;
import ru.inversion.fru.parser.exceptions.FruBadHeaderFormatException;
import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.inversion.fru.utils.constants.SectionTypeEnum.TABLE;


public abstract class AbstractSectionNode {

    public static final Pattern NAMED_GROUP_PATTERN = Pattern.compile (
        "^#?\\w+\\s*'\\s*(?<num>\\d+)\\s*(?:\\(\\s*(?<fields>[^)]*)\\s*\\))?\\s*;?$"
    );

    public static final Pattern SCRIPT_PATTERN = Pattern.compile (
        "^script\\s+(on|off)\\s*"
    );

    /** Номер по порядку загрузки */
    final private int orderNum;

    /** Скрипт */
    protected FruScript script;

    /** */
    private int begLine, endLine;

    /** */
    protected List<String> lines;

    /** */
    protected String header;

    /** */
    protected AbstractSectionNode(int orderNum, String h, int b, int e ) {
        this.orderNum = orderNum;
        this.header = h;
        this.begLine= b;
        this.endLine= e;
    }

    public int begLine() {
        return begLine;
    }

    public int endLine() {
        return endLine;
    }

    /** */
    public int getOrderNum() {
        return orderNum;
    }

    /** */
    abstract public SectionTypeEnum getType();

    /** */
    abstract public boolean isEntry();

    /** */
    @Override
    public String toString() {
        return String.format("%s: %d [строки %d-%d]\n---", getType(), orderNum,  begLine, endLine );
    }

    /** */
    public void setBilScript( FruScript sc )
    {
        if( this.script!= null )
            throw new IllegalStateException("script already init");

        if( lines == null )
            lines = new LinkedList<>();

        lines.add(null);

        this.script = sc;
    }

    /** */
    public void addLine( String line )
    {
        if( lines == null )
            lines = new LinkedList<>();

        lines.add(line);
    }

    /** */
    public void addParameter( String name, String value )
    { }

    /** */
    public void addFormat( String name, String format )
    { }

    /** */
    public void addString( String name, String text )
    { }

    /** */
    protected FruSection parseHeader(FruBuilder fruBuilder )
    {
        final Matcher matcher = NAMED_GROUP_PATTERN.matcher( header );

        if( !matcher.find() )
            throw new FruBadHeaderFormatException( "Не корректный формат заголовка", header, getOrderNum() );

        String sectionNum = matcher.group("num");  // именованная группа num
        String parameters = matcher.group("fields");

        int num = -1;

        if( !S.isNullOrEmpty(sectionNum) )
        {
            try {
                num = TypeConverter.convert( sectionNum, Integer.class );
            }
            catch( Throwable th ) {
                throw new FruBadHeaderFormatException( "Не корректный номер заголовка", header, getOrderNum() );
            }
        }

        //
        switch( getType() ) {
            case HEAD:
                return new FruSectionHeader( num );
            case TAIL:
                return new FruSectionTail( num );
            case TABLE:
            case TEXT:
            {
                List<String> fieldList;

                if( S.isNullOrEmpty(parameters) )
                    fieldList = Collections.emptyList();
                else
                    fieldList = Arrays.stream(parameters.split(",")).map(String::trim).filter(S::isNotNullOrEmpty).collect(Collectors.toList());

                return getType() == TABLE ? new FruSectionTable( num, fieldList ) : new FruSectionText( num, fieldList );
            }
        }

        return null;
    }

    /** */
    abstract protected void parseContent( FruBuilder fruBuilder, FruSection section );

    /** */
    public void parse( FruBuilder fruBuilder  )
    {
        parseContent( fruBuilder, parseHeader(fruBuilder) );
    }
}
