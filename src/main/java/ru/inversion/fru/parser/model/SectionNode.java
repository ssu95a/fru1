package ru.inversion.fru.parser.model;


import ru.inversion.fru.model.FruBuilder;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.sections.FruSection;
import ru.inversion.fru.model.sections.FruSectionTable;
import ru.inversion.fru.utils.constants.SectionTypeEnum;


import java.util.Collections;
import java.util.LinkedList;
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

        final LinkedList<String> l1 = (LinkedList<String>)lines;

        if( l1 != null ) {

//            while (!l1.isEmpty() && l1.getLast() != null && l1.getLast().length() == 1) {
//                l1.removeLast();
//            }

            Function<String, Integer> dataIndex = section instanceof FruSectionTable ? section::getFieldNum : (s -> -1);

            for( String line : lines )
            {
                if( line == null )
                {
                    if (script != null)
                        section.addLine( new FruLine(Collections.singletonList(script)) );
                } else
                    section.addLine(FruLine.make(fruBuilder, line, dataIndex));
            }
        }
        else
            log.warn( "Секция {} № {} пустая." , getType(), section.getNum() );
        // throw new IllegalStateException( "Секция " + section.getNum() + " пустая" );
        // Пустые секции допустимы
        fruBuilder.addSection( section );
    }
}
