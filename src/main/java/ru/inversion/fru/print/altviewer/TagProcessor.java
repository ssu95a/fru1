package ru.inversion.fru.print.altviewer;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import ru.inversion.fru.print.naltprn.AltSettings;
import ru.inversion.fru.print.naltprn.cmd.AltCommand;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;
import ru.inversion.utils.S;

import java.util.*;

/**
 * Парсер AltPrinter-тегов вида `TAG`, `TAG+`, `TAG-`
 *
 * Формат:
 *  - текст может содержать последовательности, начинающиеся и заканчивающиеся '`'
 *  - внутри: [\\w,]+[+-]? иначе считается обычным текстом
 *
 * Возвращает:
 *  - cleanText: исходный текст без тегов
 *  - styleSpans: spans для RichTextFX по cleanText
 */
public class TagProcessor {

    private static final String PAGE_END_TAG = "PAGE_END";

    private static final String PAGE_SEPARATOR_STYLE = "page-separator";
    private static final String PAGE_SEPARATOR_TITLE = " Разделитель страниц ";
    private static final String PAGE_SEPARATOR_FALLBACK = "====== Разделитель страниц ======";

    private static final AltCommandDict DICT = AltSettings.INSTANCE().commandDict();

    private static final char TAG_CHAR = '`';

    // cssStyleName -> bit (выделяется динамически по мере встречаемости)
    private static final Map<String, Integer> CSS_TO_BIT = new HashMap<>();
    private static int nextBit = 1;
    private static final int MAX_BITS = 30;

    // mask -> immutable styles (RichTextFX)
    private static final Map<Integer, Collection<String>> MASK_TO_STYLES = new HashMap<>();
    static {
        MASK_TO_STYLES.put( 0, Collections.emptyList() );
    }

    public static class ParseResult {

        public final String text;
        public final StyleSpans<Collection<String>> styleSpans;

        public ParseResult(String text, StyleSpans<Collection<String>> styleSpans) {
            this.text       = text;
            this.styleSpans = styleSpans;
        }
    }

    /** */
    private static boolean isPageEndTag(String tag) {
        return PAGE_END_TAG.equalsIgnoreCase(tag) || "FF".equalsIgnoreCase(tag);
    }

    /**
     * Вычисляет ширину страницы для preview по самой длинной видимой строке ALT.
     *
     * Важно:
     * - ALT tags между `...` не считаются видимым текстом;
     * - PAGE_END / FF не считаются строкой контента;
     * - \r\n считается одним переводом строки;
     * - если ширину определить не удалось, возвращаем -1.
     */
    private static int guessVisiblePageWidth(CharSequence text) {

        if( text == null || text.length() == 0 )
            return -1;

        int max     = 0;
        int current = 0;

        final int n = text.length();

        for( int i = 0; i < n; i++ )
        {
            char ch = text.charAt(i);

            if( ch == TAG_CHAR )
            {
                int close = indexOf( text, TAG_CHAR, i + 1, n );

                if( close > i )
                {
                    String tag = text.subSequence(i + 1, close).toString();

                    /*
                     * PAGE_END является page marker, а не частью строки.
                     * Остальные валидные ALT tags тоже не считаем видимыми символами.
                     */
                    if( isValidTagBody(text, i + 1, close) ) {
                        i = close;
                        continue;
                    }
                }

                /*
                 * Некорректный одинокий ` считаем обычным символом.
                 */
                current++;
                continue;
            }

            if( ch == '\r' )
            {
                if( current > max )
                    max = current;

                current = 0;

                if( i + 1 < n && text.charAt(i + 1) == '\n' )
                    i++;

                continue;
            }

            if( ch == '\n' || ch == '\f' )
            {
                if( current > max )
                    max = current;

                current = 0;
                continue;
            }

            current++;
        }

        if( current > max )
            max = current;

        return max > 0 ? max : -1;
    }

    /** */
    private static void appendPageSeparator(
            StringBuilder out,
            StyleSpansBuilder<Collection<String>> spans,
            int pageWidth
    ) {
        if( out.length() > 0 && !endsWithLineBreak(out) ) {
            out.append('\n');
            spans.add(Collections.<String>emptyList(), 1);
        }

        String line = makePageSeparatorLine(pageWidth);

        out.append(line);
        spans.add(Collections.singleton(PAGE_SEPARATOR_STYLE), line.length());

        out.append('\n');
        spans.add(Collections.<String>emptyList(), 1);
    }

    private static String makePageSeparatorLine(int pageWidth) {

        if( pageWidth <= 0 )
            return PAGE_SEPARATOR_FALLBACK;

        String title = PAGE_SEPARATOR_TITLE;

        if( pageWidth <= title.length() )
            return title.trim();

        int fill = pageWidth - title.length();

        int left = fill / 2;
        int right = fill - left;

        StringBuilder sb = new StringBuilder(pageWidth);

        appendChars(sb, '=', left);
        sb.append(title);
        appendChars(sb, '=', right);

        return sb.toString();
    }

    private static void appendChars(StringBuilder sb, char ch, int count) {
        for( int i = 0; i < count; i++ )
            sb.append(ch);
    }

    private static boolean endsWithLineBreak(StringBuilder sb) {

        if( sb.length() == 0 )
            return true;

        char ch = sb.charAt(sb.length() - 1);
        return ch == '\n' || ch == '\r';
    }

    private static int skipSingleLineBreak(CharSequence text, int index, int n) {

        if( index >= n )
            return index;

        char ch = text.charAt(index);

        if( ch == '\r' ) {
            if( index + 1 < n && text.charAt(index + 1) == '\n' )
                return index + 2;

            return index + 1;
        }

        if( ch == '\n' )
            return index + 1;

        return index;
    }

    /** */
    public static ParseResult parseForFormattedMode(CharSequence text) {
        int width = guessVisiblePageWidth(text);
        return parseForFormattedMode(text, width);
    }

    /** */
    public static ParseResult parseForPlainTextMode(CharSequence text) {
        int width = guessVisiblePageWidth(text);
        return parseForPlainTextMode(text, width);
    }

    /** Форматированный режим: теги убираются, стили применяются к cleanText */
    public static ParseResult parseForFormattedMode( CharSequence text, int pageWidth )
    {
        if( text == null || text.length() == 0 )
            return new ParseResult( S.EMPTY_STRING, StyleSpans.singleton( Collections.emptyList(), 0));

        final int n = text.length();
        final StringBuilder clean = new StringBuilder(n);
        final StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();

        int mask = 0;
        ArrayList<String> currentCss = new ArrayList<>(4);

        int i = 0;
        int lastTextStart = 0;

        while( i < n )
        {
            char ch = text.charAt(i);

            if( ch != TAG_CHAR ) {
                i++;
                continue;
            }

            // встретили '`' -> попробуем распарсить тег
            int tagStart = i;
            int close = indexOf( text, TAG_CHAR, i + 1, n );
            if( close < 0 ) {
                // нет закрывающей '`' -> дальше обычный текст
                i++;
                continue;
            }

            // сначала сбрасываем текст до '`' как обычный сегмент
            if( tagStart > lastTextStart )
                appendSegment( text, lastTextStart, tagStart, clean, spans, mask, currentCss );

            // кандидаты на тег: между backtick’ами
            int innerStart = tagStart + 1;
            int innerEnd   = close; // exclusive
            int innerLen   = innerEnd - innerStart;

            if( innerLen <= 0)
            {
                // `` -> считаем как обычный текст (оба backtick’а)
                appendSegment(text, tagStart, close + 1, clean, spans, mask, currentCss);
                i = close + 1;

                lastTextStart = i;

                continue;
            }

            // проверим, что это "валидный тег" как в regex: [\w,]+[+-]?
            // иначе считаем это обычным текстом целиком (включая backtick’и)

            if( !isValidTagBody(text, innerStart, innerEnd) )
            {
                appendSegment(text, tagStart, close + 1, clean, spans, mask, currentCss);

                i = close + 1;
                lastTextStart = i;
                continue;
            }

            // применяем тег
            String tag = text.subSequence(innerStart, innerEnd).toString();

            if( isPageEndTag(tag) )
            {
                appendPageSeparator(clean, spans, pageWidth);

                i = skipSingleLineBreak(text, close + 1, n);
                lastTextStart = i;

                continue;
            }

            mask = applyTagToMaskAndList(tag, mask, currentCss);

            i = close + 1;
            lastTextStart = i;
        }

        // хвост текста
        if (lastTextStart < n) {
            appendSegment(text, lastTextStart, n, clean, spans, mask, currentCss);
        }

        String resultText = clean.toString();
        StyleSpans<Collection<String>> styleSpans = spans.create();

        if (styleSpans.length() != resultText.length()) {
            // fallback: возвращаем исходный текст без форматирования (без stderr)
            StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
            b.add(Collections.emptyList(), text.length());
            return new ParseResult(text.toString(), b.create());
        }

        return new ParseResult(resultText, styleSpans);
    }


    /** Plain text режим: подсвечиваем сами теги как "tag" */
    public static ParseResult parseForPlainTextMode(CharSequence text, int pageWidth) {

        if( text == null || text.length() == 0 )
            return new ParseResult( S.EMPTY_STRING, StyleSpans.singleton(Collections.emptyList(), 0));

        final int n = text.length();
        final StringBuilder out = new StringBuilder(n);
        final StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();

        int i = 0;
        int lastTextStart = 0;

        while( i < n )
        {
            char ch = text.charAt(i);

            if( ch != TAG_CHAR ) {
                i++;
                continue;
            }

            int tagStart = i;
            int close = indexOf(text, TAG_CHAR, i + 1, n);

            if( close < 0 ) {
                i++;
                continue;
            }

            if( tagStart > lastTextStart ) {
                appendPlainSegment(
                        text,
                        lastTextStart,
                        tagStart,
                        out,
                        spans,
                        Collections.<String>emptyList()
                );
            }

            int innerStart = tagStart + 1;
            int innerEnd = close;

            boolean valid = (innerEnd > innerStart) && isValidTagBody(text, innerStart, innerEnd);

            appendPlainSegment(
                    text,
                    tagStart,
                    close + 1,
                    out,
                    spans,
                    valid ? Collections.singleton("tag") : Collections.<String>emptyList()
            );

            if( valid )
            {
                String tag = text.subSequence(innerStart, innerEnd).toString();

                if( isPageEndTag(tag) )
                {
                    appendPageSeparator(out, spans, pageWidth);

                    i = skipSingleLineBreak(text, close + 1, n);
                    lastTextStart = i;

                    continue;
                }
            }

            i = close + 1;
            lastTextStart = i;
        }

        if( lastTextStart < n ) {
            appendPlainSegment(
                    text,
                    lastTextStart,
                    n,
                    out,
                    spans,
                    Collections.<String>emptyList()
            );
        }

        String resultText = out.toString();
        StyleSpans<Collection<String>> styleSpans = spans.create();

        if( styleSpans.length() != resultText.length() ) {
            throw new IllegalStateException(
                    "StyleSpans length (" + styleSpans.length()
                            + ") doesn't match text length (" + resultText.length() + ")."
            );
        }

        return new ParseResult(resultText, styleSpans);
    }

    private static void appendPlainSegment(
            CharSequence src,
            int start,
            int end,
            StringBuilder out,
            StyleSpansBuilder<Collection<String>> spans,
            Collection<String> style
    ) {
        int len = end - start;

        if( len <= 0 )
            return;

        out.append(src, start, end);
        spans.add(style, len);
    }

    /** Plain text режим: подсвечиваем сами теги как "tag"
    public static ParseResult parseForPlainTextMode(CharSequence text) {

        if( text == null || text.length() == 0 )
            return new ParseResult( S.EMPTY_STRING, StyleSpans.singleton(Collections.emptyList(), 0));

        final int n = text.length();
        final StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();

        int i = 0;
        int lastTextStart = 0;

        while( i < n )
        {
            char ch = text.charAt(i);

            if( ch != TAG_CHAR ) {
                i++;
                continue;
            }

            int tagStart = i;
            int close = indexOf( text, TAG_CHAR, i + 1, n);
            if (close < 0) {
                i++;
                continue;
            }

            // обычный текст до '`'
            if( tagStart > lastTextStart ) {
                spans.add( Collections.emptyList(), tagStart - lastTextStart );
            }

            int innerStart = tagStart + 1;
            int innerEnd   = close;

            boolean valid = (innerEnd > innerStart) && isValidTagBody(text, innerStart, innerEnd);

            int segLen = (close + 1) - tagStart;
            spans.add( valid ? Collections.singleton("tag") : Collections.emptyList(), segLen );

            i = close + 1;
            lastTextStart = i;
        }

        if (lastTextStart < n) {
            spans.add(Collections.emptyList(), n - lastTextStart);
        }

        StyleSpans<Collection<String>> styleSpans = spans.create();
        if (styleSpans.length() != n) {
            throw new IllegalStateException("StyleSpans length (" + styleSpans.length() + ") doesn't match text length (" + n + ").");
        }

        return new ParseResult(text.toString(), styleSpans);
    }
    */

    /** */
    private static void appendSegment ( CharSequence src, int start, int end, StringBuilder clean, StyleSpansBuilder<Collection<String>> spans, int mask, List<String> currentCss )
    {
        int len = end - start;

        if( len <= 0 )
            return;

        clean.append( src, start, end );
        spans.add   ( stylesForMask( mask, currentCss ), len );
    }

    /** */
    private static int indexOf( CharSequence s, char c, int from, int to ) {

        for (int i = from; i < to; i++)
        {
            if( s.charAt(i) == c )
                return i;
        }
        return -1;
    }

    /**
     * Валидность тела тега по regex:
     *   [\w,]+[+-]?
     * Здесь проверяем:
     *  - все символы: буквы/цифры/'_' или ',' кроме последнего, где может быть '+'/'-'
     *  - минимум 1 символ
     */
    private static boolean isValidTagBody( CharSequence s, int start, int end )
    {
        int len = end - start;

        if( len <= 0 )
            return false;

        for( int i = start; i < end; i++)
        {
            char ch = s.charAt(i);

            boolean last = (i == end - 1);

            if( last && (ch == '+' || ch == '-'))
            {
                // знак допускается только в конце
                if( len == 1 )
                    return false; // "+" или "-" не считается тегом

                continue;
            }

            // Для совместимости разрешим Character.isLetterOrDigit + '_' + ','
            if( ch == '_' || ch == ',' || ch == '=')
                continue;

            if( Character.isLetterOrDigit(ch) )
                continue;

            return false;
        }

        return true;
    }

    /** */
    private static Collection<String> stylesForMask( int mask, List<String> currentCss )
    {

        Collection<String> cached = MASK_TO_STYLES.get(mask);
        if( cached != null )
            return cached;

        if( mask == 0 )
            return Collections.emptyList();

        Collection<String> res = Collections.unmodifiableList(new ArrayList<>(currentCss) );

        MASK_TO_STYLES.put( mask, res);

        return res;
    }

    /** */
    private static int bitForCss(String css)
    {
        Integer bit = CSS_TO_BIT.get(css);
        if( bit != null )
            return bit;

        int used  = CSS_TO_BIT.size();
        if (used >= MAX_BITS)
            return 0;

        int newBit = nextBit;
        nextBit <<= 1;

        CSS_TO_BIT.put(css, newBit);

        return newBit;
    }

    /** */
    private static int applyTagToMaskAndList( String tag, int mask, ArrayList<String> currentCss )
    {
        if( tag == null || tag.isEmpty() )
            return mask;

        char lastCh = S.lastChar(tag);

        boolean hasSign = (lastCh == '+' || lastCh == '-');
        boolean remove  = (lastCh == '-');

        if( !hasSign && "NORMAL".equalsIgnoreCase(tag) ) {
            currentCss.clear();
            return 0;
        }

        String baseTag  = tag;
        String lookupTag= tag;

        if( hasSign )
        {
            baseTag   = tag.substring(0, tag.length() - 1);
            lookupTag = baseTag + "+";
        }

        AltCommand cmd = DICT.getCommand(lookupTag);

        if( cmd == null && hasSign )
            cmd = DICT.getCommand(baseTag);

        if( cmd == null && !hasSign )
            cmd = DICT.getCommand(tag);

        if( cmd == null )
            return mask;

        String css = getCorrectStyleName( cmd.getCssStyleName() );

        if (css == null || css.isEmpty())
            return mask;

        int bit = bitForCss(css);
        if (bit == 0) return mask;

        boolean isSet = (mask & bit) != 0;

        if (hasSign) {
            if (remove) {
                if (isSet) {
                    mask &= ~bit;
                    removeFromList(currentCss, css);
                }
            } else {
                if (!isSet) {
                    mask |= bit;
                    currentCss.add(css);
                }
            }
        } else {
            if (!isSet) {
                mask |= bit;
                currentCss.add(css);
            }
        }
        return mask;
    }

    private static void removeFromList(ArrayList<String> list, String css) {
        for (int i = 0; i < list.size(); i++) {
            if (css.equals(list.get(i))) {
                list.remove(i);
                return;
            }
        }
    }

    private static String getCorrectStyleName(String tagName) {
        return tagName;
    }
}
