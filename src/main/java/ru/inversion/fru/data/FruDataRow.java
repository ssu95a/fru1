package ru.inversion.fru.data;

import java.util.*;
import java.util.function.Consumer;

import static ru.inversion.fru.model.sections.FruSectionTable.PLACEHOLDER_VALUE;

public class FruDataRow {

    private final int sectionNum;

    private final List<String> data;

    /** */
    public FruDataRow( int sectionNum, List<String> data )
    {
        this.sectionNum = sectionNum;

        if( data != null && !data.isEmpty() )
        {
            if( data.get(0) == PLACEHOLDER_VALUE )
                this.data = data;
            else
                if( data.stream().allMatch(s->s.indexOf('\n') < 0 ) )
                    this.data = new ArrayList<>(data);
                else
                {
                    this.data = new ArrayList<>();

                    data.forEach( new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            if( s.indexOf('\n') >=0 )
                                FruDataRow.this.data.add( s.replace( '\n', ' ' ) );
                            else
                                FruDataRow.this.data.add( s );
                        }
                    });
                }
        }
        else
            this.data = new ArrayList<>();
    }

    /** Номер секции из fru файла */
    public int getSectionNum() {
        return sectionNum;
    }

    /** Данные одной записи - для секции */
    public List<String> data() {
        return data;
    }

    /** Сырое значение без split-состояния текущей строки. */
    public String getValue( int valIndex )
    {
        return valIndex < data.size() ? data.get(valIndex) : null;
    }

    /** */
    @Override
    public String toString() {
        return String.format(" FruDataRow[ section=%d, data=%s ]", sectionNum, data );
    }
}