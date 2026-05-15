package ru.inversion.fru.print.altprint.doc.styled;

import org.slf4j.Logger;
import ru.inversion.fru.print.altprint.ALTPrintException;
import ru.inversion.fru.print.naltprn.cmd.AltCommandDict;
import ru.inversion.fru.print.naltprn.cmd.AltParameter;
import ru.inversion.fru.print.naltprn.cmd.AltParameterTypeEnum;
import ru.inversion.utils.ReaderScanner;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.awt.Font;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fru.print.altprint.doc.styled.IStyledTextParser.TextFlowControl.*;

/** */
public final class StyledTextParser implements IStyledTextParser {

    private final static Logger logger = getLogger(MethodHandles.lookup().lookupClass());

    private final AltCommandDict commandDict;
    private final Iterator<ReaderScanner.IContext> scanner;

    private StyleState currentStyle;

    private Deque<ParsedElement> cache;

    /** */
    public StyledTextParser(Reader reader, AltCommandDict dict) {
        this.scanner      = ReaderScanner.newIterable(reader).iterator();
        this.commandDict  = dict;
        this.currentStyle = commandDict.getInitCommand().toStyleState();
    }

    @Override
    public boolean hasNext() {
        return (cache != null && !cache.isEmpty()) || scanner.hasNext();
    }

    @Override
    public ParsedElement next() {

        if( cache != null && !cache.isEmpty() ) {
            ParsedElement el = cache.pollFirst();
            if( cache.isEmpty() )
                cache = null;
            return el;
        }

        if (!scanner.hasNext()) {
            throw new NoSuchElementException();
        }

        try {

            final StringBuilder text = new StringBuilder();
            final List<Span> spans = new ArrayList<Span>();

            StyleState spanStyle = currentStyle;

            while (scanner.hasNext()) {

                ReaderScanner.IContext ctx = scanner.next();
                char ch = ctx.current();

                /*
                 * Physical newline:
                 * - если строка уже содержит текст, возвращаем chunk;
                 * - если строка пустая, возвращаем LINE_FEED, чтобы не потерять blank line.
                 *
                 * ВАЖНО:
                 * Для непустой строки НЕ возвращаем LINE_FEED следом,
                 * иначе с текущим PageLayoutEngine будет двойной межстрочный шаг.
                 */
                if(ch == '\n')
                {
                    currentStyle = spanStyle;

                    if (!isBlank(text)) {
                        return new StyledTextChunk(text.toString(), spans);
                    }

                    return LINE_FEED;
                }

                /*
                 * Form feed:
                 * если перед ним есть текст — сначала вернуть текст,
                 * а PAGE_FEED положить в cache.
                 */
                if (ch == '\f') {
                    currentStyle = spanStyle;

                    if (!isBlank(text)) {
                        enqueue(PAGE_FEED);
                        return new StyledTextChunk(text.toString(), spans);
                    }

                    return PAGE_FEED;
                }

                /*
                 * Backtick command:
                 * НЕ завершает chunk.
                 * Она только меняет стиль следующих символов
                 * или даёт explicit flow control.
                 */
                if (ch == '`') {
                    CommandReadResult cmd = readCommand(scanner, commandDict);

                    if (cmd == null) {
                        continue;
                    }

                    if (cmd.flowControl != null) {
                        currentStyle = spanStyle;

                        if (!isBlank(text)) {
                            enqueue(cmd.flowControl);
                            return new StyledTextChunk(text.toString(), spans);
                        }

                        return cmd.flowControl;
                    }

                    if (cmd.parameter != null) {
                        spanStyle = cmd.parameter.applyTo(spanStyle, null);
                        currentStyle = spanStyle;
                    }

                    continue;
                }

                int pos = text.length();
                text.append(ch);

                if (spans.isEmpty()) {
                    spans.add(new Span(0, 1, spanStyle));
                }
                else {
                    Span last = spans.get(spans.size() - 1);

                    if (last.style() == spanStyle) {
                        spans.set(
                                spans.size() - 1,
                                new Span(last.start(), pos + 1, last.style())
                        );
                    }
                    else {
                        spans.add(new Span(pos, pos + 1, spanStyle));
                    }
                }
            }

            currentStyle = spanStyle;

            if (!isBlank(text)) {
                return new StyledTextChunk(text.toString(), spans);
            }

            return null;
        }
        catch (Exception e) {
            throw new ALTPrintException("Ошибка при чтении файла", e);
        }
    }

    private void enqueue(ParsedElement el) {
        if (el == null) {
            return;
        }

        if (cache == null) {
            cache = new ArrayDeque<ParsedElement>();
        }

        cache.addLast(el);
    }

    private static boolean isBlank(StringBuilder sb) {
        if (sb.length() == 0) {
            return true;
        }

        /*
         * В формах пробелы могут быть значимыми.
         * Поэтому НЕ надо считать строку из многих пробелов blank.
         * Старое поведение оставляем максимально близко:
         * blank только пустая строка или один whitespace-char.
         */
        return sb.length() == 1 && Character.isSpaceChar(sb.charAt(0));
    }

    private static final class CommandReadResult {
        private final AltParameter<?> parameter;
        private final TextFlowControl flowControl;

        private CommandReadResult(
                AltParameter<?> parameter,
                TextFlowControl flowControl
        ) {
            this.parameter = parameter;
            this.flowControl = flowControl;
        }

        static CommandReadResult parameter(AltParameter<?> parameter) {
            return new CommandReadResult(parameter, null);
        }

        static CommandReadResult flow(TextFlowControl flowControl) {
            return new CommandReadResult(null, flowControl);
        }
    }

    static CommandReadResult readCommand(
            Iterator<ReaderScanner.IContext> scanner,
            AltCommandDict dict
    ) {
        final StringBuilder sb = new StringBuilder();

        while (scanner.hasNext()) {
            ReaderScanner.IContext ctx = scanner.next();

            if (ctx.current() == '`') {
                break;
            }

            sb.append(ctx.current());
        }

        if (sb.length() == 0) {
            return null;
        }

        final String cmdText = sb.toString();

        if( "PAGE_END".equalsIgnoreCase(cmdText) || "FF".equalsIgnoreCase(cmdText) )
        {
            return CommandReadResult.flow(PAGE_FEED);
        }

        if ("LF".equalsIgnoreCase(cmdText)) {
            return CommandReadResult.flow(LINE_FEED);
        }

        Optional<AltParameter<?>> altParameter = dict.resolveCommand(cmdText);

        if( altParameter.isPresent() )
        {
            final AltParameter<?> pv = altParameter.get();

            if (U.in(
                    pv.getType(),
                    AltParameterTypeEnum.ORIENTATION,
                    AltParameterTypeEnum.COPIES
            )) {
                return null;
            }

            return CommandReadResult.parameter(pv);
        }

        logger.warn("В словаре не найдена команда: " + cmdText);
        return null;
    }
}