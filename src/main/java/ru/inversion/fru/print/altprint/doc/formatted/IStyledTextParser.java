package ru.inversion.fru.print.altprint.doc.formatted;


import java.util.Iterator;
import java.util.List;

public interface IStyledTextParser extends Iterator<IStyledTextParser.ParsedElement> {

    /** */
    final class Span {

        final private int start; //[
        final private int end; //)
        final private StyleState style;

        public Span( int start, int end, StyleState style) {
            this.start = start;
            this.end   = end;
            this.style = style;
        }
        /** */
        public int start() {
            return start;
        }
        /** */
        public int end() {
            return end;
        }
        /** */
        public StyleState style() {
            return style;
        }
    }

    /** */
    interface ParsedElement { }

    /** */
    enum TextFlowControl implements ParsedElement {
        LINE_FEED,
        PAGE_FEED
    }

    /** */
    final class TextChunk implements ParsedElement {

        private final String text;
        private final List<Span> spans;

        public TextChunk( String text, List<Span> spans) {
            this.text = text;
            this.spans= spans;
        }

        /** */
        public String text() {
            return text;
        }

        /** */
        public List<Span> spans() {
            return spans;
        }
    }
}
