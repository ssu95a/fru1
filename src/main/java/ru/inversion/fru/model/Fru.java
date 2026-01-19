package ru.inversion.fru.model;

import ru.inversion.fru.model.formats.AlignEnum;
import ru.inversion.fru.model.formats.FruFormat;
import ru.inversion.fru.model.formats.FruFormatter;
import ru.inversion.fru.model.items.FruPaging;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.sections.FruSection;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** */
public class Fru {

    final private Path fruFile;

    final private Map<String, FruFormat> formats;
    final private Map<String,String>     strings;

    final private Map<String,Object>     parameters;

    final private Map<Integer,String>    arguments;

    /** string FILTER = "Исключаемые символы"
        определяет набор символов, исключаемых из полей с ключем /v */

    final private Set<Character> excludeSymbols;

    /** */
    final private Map<Integer,FruSection> sections;

    /** */
    private FruScript initScript;

    /** */
    public Set<Character> excludeSymbols() { return excludeSymbols; }

    /** */
    final private int lines;

    /** */
    final private int width;

    /** */
    final FruPaging paging;

    public Fru (
        Path fruFile,
        Map<String, FruFormat> formats,
        Map<String, String> strings,
        Map<String, Object> parameters,
        List<FruSection> sectionsList,
        List<String> argumetsList,
        FruScript initScript
    )
    {
        this.fruFile    = fruFile;

        this.formats    = U.nvl( formats,    Collections.emptyMap() );
        this.strings    = U.nvl( strings,    Collections.emptyMap() );
        this.parameters = U.nvl( parameters, Collections.emptyMap() );
        this.initScript = initScript;

        {
            String excludeStr = (String) parameters.get("FILTER");
            if (!S.isNullOrEmpty(excludeStr)) {
                excludeSymbols = new HashSet<>();
                excludeStr.chars().forEach(i -> excludeSymbols.add((char) i));
            }
            else
                excludeSymbols = Collections.emptySet();
        }

        {
            final AtomicInteger index = new AtomicInteger(0);
            this.arguments = argumetsList.stream().collect( Collectors.toMap(s-> index.getAndIncrement(), s->s ) );
        }

        {
            lines = !parameters.containsKey("lines") ? - 1 : Integer.parseInt( (String) parameters.get("lines") );
            width = !parameters.containsKey("width") ? - 1 : Integer.parseInt( (String) parameters.get("width") );
        }

        if( lines > 0 )
        {
            final String paging_s = (String)parameters.get("paging");
            if( S.isNullOrEmpty(paging_s) )
                paging = null;
            else
            {
                final String pageEnd = (String) parameters.get("PAGE_END");
                final String pageLine = U.nvl((String) parameters.get("PAGELINE"), "- @ /0 @- ");

                final String first_s = (String) parameters.get("first");
                boolean first = !S.isNullOrEmpty(first_s) && !"off".equals(first_s);

                Pair<Boolean, AlignEnum> p = FruPaging.parsePaging(paging_s);

                paging = new FruPaging( p.second, p.first, first, pageEnd, FruFormatter.make(pageLine), lines );
            }
        }
        else
            paging = null;

        this.sections = new HashMap<>();
        sectionsList.forEach( (fs)->sections.put( fs.getNum(), fs ) );
    }

    /** */
    public FruPaging getPaging() {
        return paging;
    }

    /** */
    public int getLines() {
        return lines;
    }

    /** */
    public boolean hasLines() {
        return getLines() != -1;
    }

    /** */
    public boolean hasWidth( )
    {
        return getWidth() != -1;
    }

    /** */
    public int getWidth() {
        return width;
    }

    /** */
    public Map< String, FruFormat > formats()
    {
        return formats;
    }

    /** */
    public Map<String, String> strings()
    {
        return strings;
    }

    /** */
    public Map<Integer,FruSection> sections()
    {
        return sections;
    }

    /** */
    public Map<String, Object> parameters()
    {
        return parameters;
    }

    /** */
    public Map<Integer, String > arguments() { return arguments; }

    /** */
    public FruScript initScript() { return initScript; }
}


