package ru.inversion.fru.print.altprint.doc.styled;

import ru.inversion.fru.print.altprint.doc.MatrixRawWriter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public interface IStyledTextParser extends Iterator<IStyledTextParser.ParsedElement> {

    /** */
    final class Span {

        final private int start; //[
        final private int end; //)
        final private StyleState style;

        public Span( int start, int end, StyleState style )
        {
            this.start = start;
            this.end   = end;
            this.style = style;
        }
        /** */
        public int start() {
            return start;
        }
        /** */
        public int end( ) {
            return end;
        }
        /** */
        public StyleState style() {
            return style;
        }
    }

    /** */
    interface ParsedElement {
        void matrixWrite( MatrixRawWriter w ) throws IOException;
    }

    /** */
    enum TextFlowControl implements ParsedElement {

        LINE_FEED,
        PAGE_FEED;

        /** */
        public void matrixWrite( MatrixRawWriter w ) throws IOException
        { }
    }

    /** */
    final class StyledTextChunk implements ParsedElement {

        private final String     text;
        private final List<Span> spans;

        public StyledTextChunk(String text, List<Span> spans) {
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

        /** */
        public void matrixWrite( MatrixRawWriter w ) throws IOException
        { }
    }

    /** */
    final class MatrixTextChunk implements ParsedElement {

        /** */
        final private String text;

        /** */
        public MatrixTextChunk( StringBuilder sb) {
            this.text = sb.toString();
            sb.setLength(0);
        }

        /** */
        public MatrixTextChunk( String s) {
            this.text = s;
        }

        /** */
        @Override
        public String toString() {
            return text;
        }

        /** */
        public void matrixWrite( MatrixRawWriter w ) throws IOException
        {
            w.text( text );
        }
    }
}
