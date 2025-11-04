package ru.inversion.fru.model.formats;

import ru.inversion.utils.U;

import java.util.NoSuchElementException;

/** */
public enum CaseModeEnum {

    Upper('b'), Lower('s'), FirstU('f');

    final private char ch;

    /** */
    CaseModeEnum(char ch) {
        this.ch = ch;
    }

    /** */
    public static CaseModeEnum of( char ch )
    {
        switch (ch) {
            case 'b':
                return Upper;
            case 's':
                return Lower;
            case 'f':
                return FirstU;
        }
        throw new NoSuchElementException("'" + ch + "' не существует элемент CaseModeEnum" );
    }

    /** */
    public static boolean isCaseMode( char ch )
    {
        return U.inChar( ch, 'b', 's', 'f' );
    }

    /*
    /b - преобразование всех букв в верний регистр
    /s - преобразование всех букв в нижний регистр
    /f - преобразование к виду с первой заглавной буквой
    */
}
