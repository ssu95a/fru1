package ru.inversion.fru.parser.model;


import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.script.FruScript;
import ru.inversion.fru.model.sections.FruSection;
import ru.inversion.fru.model.sections.FruSectionTable;
import ru.inversion.fru.utils.constants.SectionTypeEnum;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class SectionNode extends AbstractSectionNode {

    private final SectionTypeEnum type;

    public SectionNode(SectionTypeEnum t, int orderNum, String h, int b, int e ) {
        super( orderNum, h, b, e);
        this.type = t;
    }

    /** */
    @Override
    public SectionTypeEnum getType() {
        return type;
    }

    /** */
    @Override
    public boolean isEntry() { return false; }


    @Override
    protected void parseContent( FruBuilder fruBuilder, FruSection section )
    {
        if( section == null )
            throw new IllegalArgumentException("'section' is null");

        final List l1 = lines;

        if( l1 != null )
        {
            Function<String, Integer> dataIndex = section instanceof FruSectionTable ? section::getFieldNum : (s -> -1);

            for( Object l : lines )
            {
                if( l instanceof FruScript )
                    section.addLine( new FruLine( Collections.singletonList((FruScript)l)) );
                else
                    section.addLine( FruLine.make( fruBuilder, l.toString(), dataIndex) );
            }
        }
        else
            log.warn( "Секция {} № {} пустая." , getType(), section.getNum() );

        // throw new IllegalStateException( "Секция " + section.getNum() + " пустая" );
        // Пустые секции допустимы

        fruBuilder.addSection( section );
    }
}
