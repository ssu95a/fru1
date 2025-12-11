package ru.inversion.fru.print.altviewer;

import javafx.scene.text.Font;
import ru.inversion.utils.S;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class FontMan {

    /** */
    private static final String RESOURCE_PATH = "res/monospaced_fonts.txt";

    /** */
    private static Set<String> monospacedFamilies;

    static {
        monospacedFamilies = Collections.unmodifiableSet(loadMonospaced());
    }

    private static Set<String> getDefaultFamilies() {
        return new HashSet<>( Arrays.asList(
            "mono", "monospace", "typewriter", "console",
            "terminal", "fixed", "courier", "consolas", "menlo", "source code",
            "dejavu", "liberation", "ubuntu mono", "roboto mono", "fira code", "hack",
            "droid sans", "noto sans", "sf mono", "cascadia", "jetbrains")
        );
    }

    /** */
    private static Set<String> loadMonospaced( )
    {
        final Set<String> familes;

        try( InputStream inputStream = FontMan.class.getResourceAsStream(RESOURCE_PATH) )
        {
            if( inputStream == null ) {
                System.err.println("Resource not found: " + RESOURCE_PATH );
                return getDefaultFamilies();
            }

            try( BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8) ) ) {

                familes = reader.lines().map(String::trim)
                            .filter(S::isNotNullOrEmpty).filter(s->s.charAt(0) != '#')
                                .map(String::toLowerCase).collect(Collectors.toSet());

            }

            System.out.println("Loaded " + familes.size() + " monospaced from resources");

            return familes;

        } catch (IOException e) {
            System.err.println("Error loading monospaced from resources: " + e.getMessage() );
            return getDefaultFamilies();
        }
   }

    /** */
    public static List<String> getMonospaceFontNames() {

        List<String> allFonts = Font.getFamilies();

        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for( String fontFamily : allFonts )
        {
            if( isMonospacedFont(fontFamily) ) {
                result.add(fontFamily);
            }
        }

        return new ArrayList<>(result);
    }

    /** */
    private static boolean isMonospacedByWidth(String fontFamily) {
        try {

            Font testFont = Font.font(fontFamily, 12);

            // Для Java 8 используем javafx.scene.text.Text для измерения ширины
            javafx.scene.text.Text textW = new javafx.scene.text.Text("W");
            textW.setFont(testFont);

            javafx.scene.text.Text textI = new javafx.scene.text.Text("i");
            textI.setFont(testFont);

            javafx.scene.text.Text textDot = new javafx.scene.text.Text(".");
            textDot.setFont(testFont);

            javafx.scene.text.Text textSpace = new javafx.scene.text.Text(" ");
            textSpace.setFont(testFont);

            // В моноширинном шрифте все символы имеют одинаковую ширину
            // Допускаем небольшую погрешность
            double tolerance = 0.1;
            return Math.abs(textW.getLayoutBounds().getWidth() - textI.getLayoutBounds().getWidth()) < tolerance &&
                    Math.abs(textW.getLayoutBounds().getWidth() - textDot.getLayoutBounds().getWidth()) < tolerance &&
                    Math.abs(textW.getLayoutBounds().getWidth() - textSpace.getLayoutBounds().getWidth()) < tolerance;

        } catch (Exception e) {
            // Если не удалось проверить алгоритмически, полагаемся на список
            return false;
        }
    }

    public static boolean isMonospacedFont(String fontFamily) {

        String lowerFont = fontFamily.toLowerCase();

        //boolean hasMonospacedIndicator = false;

        for(String f : monospacedFamilies )
        {
            if( lowerFont.contains(f) ) {
                return true;
            }
        }

        // Дополнительная проверка по ширине символов
        return isMonospacedByWidth(fontFamily);
    }

    /**
     * Возвращает список моноширинных шрифтовых семейств (без дубликатов)

    public static List<String> getMonospaceFontNames() {
        // Получаем все шрифтовые семейства (а не отдельные начертания)
        Set<String> allFontFamilies = new HashSet<>(Font.getFamilies());
        List<String> monospaceFonts = new ArrayList<>();

        for (String fontFamily : allFontFamilies) {
            if (isLikelyMonospace(fontFamily)) {
                monospaceFonts.add(fontFamily);
            }
        }

        // Сортируем по алфавиту
        Collections.sort(monospaceFonts);

        return monospaceFonts;
    }
     */

    /**
     * Возвращает рекомендуемый шрифт по умолчанию
     */
    public static String getDefaultMonospaceFont(List<String> availableFonts) {
        return "Monospaced";
    }


    /**
     * Получает все доступные шрифтовые семейства (для отладки)
     */
    public static List<String> getAllFontFamilies() {
        List<String> allFamilies = new ArrayList<>(Font.getFamilies());
        Collections.sort(allFamilies);
        return allFamilies;
    }

    /**
     * Получает все доступные шрифты с начертаниями (для отладки)
     */
    public static List<String> getAllFontNames() {
        List<String> allFonts = new ArrayList<>(Font.getFontNames());
        Collections.sort(allFonts);
        return allFonts;
    }
}