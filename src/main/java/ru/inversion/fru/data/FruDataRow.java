package ru.inversion.fru.data;

import java.util.*;

public class FruDataRow {

    private final int sectionNum;

    private final List<String> data;

    /** */
    public FruDataRow( int sectionNum, List<String> data )
    {
        this.sectionNum = sectionNum;
        this.data = data != null ? new ArrayList<>(data) : new ArrayList<>();
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
        return String.format("FruDataRow[section=%d, data=%s]", sectionNum, data );
    }
}