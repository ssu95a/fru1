package ru.inversion.fru.model.formats;


import ru.inversion.utils.U;

import java.util.NoSuchElementException;

/** */
public enum AlignEnum {

    None('0'), Left('l'), Center('c'), Right('r');

    final private char ch;

    /** */
    AlignEnum(char ch) {
        this.ch = ch;
    }

    /** */
    public static AlignEnum of(char ch )
    {
        switch (ch) {
            case 'l':
                return Left;
            case 'r':
                return Right;
            case 'c':
                return Center;
            case '0':
                return None;
        }
        throw new NoSuchElementException("'" + ch + "' не существует элемент FruAlignEnum" );
    }

    /** */
    public static boolean isAlign( char ch )
    {
        return U.inChar( ch, 'l', 'r', 'c' );
    }

}
