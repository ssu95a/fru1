package ru.inversion.fru.print.altviewer;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import ru.inversion.utils.S;
import ru.inversion.fru.print.altprint.ALTCommand;
import ru.inversion.fru.print.altprint.ALTCommandDict;
import ru.inversion.fru.print.altprint.ALTSettings;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagProcessor {

    /** */
    public static class ParseResult {

        public final String text;
        public final StyleSpans<Collection<String>> styleSpans;

        public ParseResult( String text, StyleSpans<Collection<String>> styleSpans) {
            this.text       = text;
            this.styleSpans = styleSpans;
        }
    }

    /** */
    private static final char tagChar = '`';

    /** */
    private static final Pattern TAG_PATTERN = Pattern.compile("(`\\w+[+|-]?`)|([^`]+)");


    /**
     * Парсер для форматированного режима
     */
    public static ParseResult parseForFormattedMode( CharSequence text )
    {
        if( text == null || text.length() == 0 )
            return new ParseResult( S.EMPTY_STRING, StyleSpans.singleton( Collections.emptyList(), 0) );

        final StringBuilder   cleanText = new StringBuilder();
        final Set<String> currentStyles = new HashSet<>();

        final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        Matcher matcher = TAG_PATTERN.matcher(text);

        int segmentCount = 0;

        while( matcher.find() )
        {
            segmentCount++;

            String segment = matcher.group();

            if( segment.isEmpty() )
                continue;

            if( segment.charAt(0) == tagChar && S.lastChar(segment) == tagChar )
            {
                String tag = segment.substring(1, segment.length() - 1);
                applyTag( tag, currentStyles );
            }
            else
            {
                cleanText.append(segment);
                Collection<String> styles = new ArrayList<>(currentStyles);
                spansBuilder.add(styles, segment.length());
            }
        }

        String resultText = cleanText.toString();
        StyleSpans<Collection<String>> styleSpans = spansBuilder.create();

        if( styleSpans.length() != resultText.length()) {

            System.err.println("ERROR: Length mismatch! Text: " + resultText.length() + ", Spans: " + styleSpans.length());

            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            builder.add(Collections.emptyList(), text.length());
            return new ParseResult( text.toString(), builder.create() );
        }

        return new ParseResult( resultText, styleSpans );
    }

    /**
     * Парсит CharSequence для plain text режима
     */
    public static ParseResult parseForPlainTextMode( CharSequence text )
    {
        if( text == null || text.length() == 0 )
            return new ParseResult(S.EMPTY_STRING, StyleSpans.singleton(Collections.emptyList(),0) );

        final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        final Matcher matcher = TAG_PATTERN.matcher( text );

        while( matcher.find() )
        {
            final String segment = matcher.group();

            if( segment.isEmpty( ))
                continue;

            if( segment.charAt(0) == tagChar && S.lastChar( segment ) == tagChar )
                spansBuilder.add( Collections.singleton("tag"), segment.length() );
            else
                spansBuilder.add( Collections.emptyList(), segment.length() );
        }

        final StyleSpans<Collection<String>> styleSpans = spansBuilder.create();

        if( styleSpans.length() != text.length())
            throw new IllegalStateException( "StyleSpans length (" + styleSpans.length() + ") doesn't match text length (" + text.length() + "). " + "This indicates a bug in the parser." );

        return new ParseResult( text.toString(), styleSpans);
    }

    /** */
    private static void applyTag( String tag, Set<String> currentStyles )
    {
        try {

            ALTCommandDict altCommandDict = ALTSettings.INSTANCE().commandDict();

            char lastCh = S.lastChar(tag);

            boolean remove = lastCh == '-';

            if( remove )
                tag = tag.substring(0, tag.length() - 1) + '+';

            if( lastCh == '+' || remove )
            {
                ALTCommand command = altCommandDict.getCommand(tag);

                if( command == null )
                {
                    String tagName = tag.substring( 0, tag.length() - 1 );

                    command = altCommandDict.getCommand(tagName);

                    if( command == null )
                    {
                        System.out.println("Tag: '" + tag + "' -> COMMAND Not Found");
                        return;
                    }

                    if( remove )
                        currentStyles.remove( getCorrectStyleName(command.getCssStyleName()) );
                    else
                        currentStyles.add( getCorrectStyleName(command.getCssStyleName()));
                }
                else
                {
                    if( remove )
                        currentStyles.remove( getCorrectStyleName(command.getCssStyleName()) );
                    else
                        currentStyles.add( getCorrectStyleName(command.getCssStyleName()));
                }
            }
            else
            {
                if( "NORMAL".equalsIgnoreCase(tag) )
                    currentStyles.clear();
                else {
                    ALTCommand command = altCommandDict.getCommand(tag);
                    if( command != null )
                        currentStyles.add(getCorrectStyleName(command.getCssStyleName()));
                    else
                        System.out.println("Tag: '" + tag + "' -> COMMAND Not Found");
                }
            }
        }
        catch ( Throwable th ) {
            throw new RuntimeException("Error on applyTag: " + tag, th );
        }
    }

    private static String getCorrectStyleName(String tagName) {
        return tagName;
        //return "under".equals(tagName) ? "underline" : tagName;
        //return "underp-text".equals(tagName) ? "underline" : tagName;
    }
}