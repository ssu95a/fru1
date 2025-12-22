package ru.inversion.fru.print.altprint;

import ru.inversion.utils.Pair;
import ru.inversion.utils.U;
import ru.inversion.utils.io.SegmentedBAOS;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.*;
import java.util.List;

/** */
public class ALTDocPrintable implements Printable
{
    public static class PrintParameters
    {
        private String  fontName  = " ";
        private int     fontSize  = 0;
        private int     fontStyle = 0;
        private Font    font;
        private float   spaceAfter = 0.5F;
        private float   leftIndent = 0.0F;
        private boolean underLine = false;

        public PrintParameters( )
        { }

        /** */
        public Font getFont( )
        {
            if( this.font == null )
                this.font = new Font( this.fontName, this.fontStyle, this.fontSize );

            return this.font;
        }

        /** */
        public boolean isBold()
        {
            return (this.fontStyle & 0x1) != 0;
        }

        /** */
        public boolean isItalic()
        {
            return (this.fontStyle & 0x2) != 0;
        }

        public void setBold(boolean b)
        {
            if( ( b && isBold() ) || ( !b && !isBold()) )
                return;

            if( b )
                this.fontStyle |= 0x1;
            else
                this.fontStyle &= 0xFFFFFFFE;

            this.font = null;
        }

        /** */
        public void setItalic(boolean b)
        {
            if( ( b && isItalic() ) || ( !b && !isItalic() ) )
                return;

            if(b) {
                this.fontStyle |= 0x2;
            } else {
                this.fontStyle &= 0xFFFFFFFD;
            }
            this.font = null;
        }

        public void setUnderline(boolean b)
        {
            this.underLine = b;
        }
        public boolean isUnderline()
        {
            return this.underLine;
        }

        public void setLeftIndent(float leftIndent)
        {
            this.leftIndent = leftIndent;
        }
        public float getLeftIndent()
        {
            return this.leftIndent;
        }

        public void setSpaseAfter(float spaceAfter)
        {
            this.spaceAfter = spaceAfter;
        }
        public float getSpaseAfter()
        {
            return this.spaceAfter;
        }

        public void setFontName(String fontName)
        {
            if (!this.fontName.equals(fontName))
            {
                this.fontName = fontName;
                this.font     = null;
            }
        }

        public void setFontSize(int fontSize)
        {
            if( this.fontSize != fontSize )
            {
                this.fontSize = fontSize;
                this.font = null;
            }
        }
        public int getFontSize()
        {
            return this.fontSize;
        }

        public void toTextAttributes(Map<AttributedCharacterIterator.Attribute, Object> attributes)
        {
            Font f = getFont();
            attributes.put( TextAttribute.FONT, f );
            attributes.put( TextAttribute.UNDERLINE, isUnderline() ? TextAttribute.UNDERLINE_ON : -1 );
        }
    }

    /** */
    public static class ALTDocStyle
    {
        private final ALTCommand command;
        private final Object     param;
        /** */
        public ALTDocStyle(ALTCommand command, Object param)
        {
            this.command = command;
            this.param   = param;
        }
        /** */
        public ALTDocStyle(ALTCommand command)
        {
            this(command, null );
        }

        public ALTCommand getCommand()
        {
            return this.command;
        }

        public Object getParam()
        {
            return this.param;
        }
        /** */
        public void toPrintParam(PrintParameters printParam)
        {
            this.command.toPrintParam( printParam, getParam() );
        }
    }

    /** */
    public static class ALTDocElem
    {
        private final String text;
        private ALTDocStyle[] styles;

        public ALTDocElem(String text, ALTDocStyle[] styles)
        {
            this.text   = text;
            this.styles = styles;
        }

        public ALTDocElem(String text)
        {
            this.text = text;
        }

        public String getText()
        {
            return this.text;
        }

        public ALTDocStyle[] getStyles()
        {
            return this.styles;
        }
    }

    /** */
    public static class ALTDocLine
    {
        private List<ALTDocElem> elemList;

        public List<ALTDocElem> getElemList()
        {
            return this.elemList;
        }

        public void add( ALTDocElem elem )
        {
            if( this.elemList == null )
                this.elemList = new ArrayList<>();

            this.elemList.add(elem);
        }
        /** */
        public boolean isEmpty()
        {
            return this.elemList == null;
        }
    }

    final static private ALTDocLine pageBreakLine = new ALTDocLine();

    /** */
    public static class ALTDocPage
    {
        private List<ALTDocLine> lines;

        public List<ALTDocLine> lines()
        {
            return this.lines;
        }

        public void add( ALTDocLine line )
        {
            if( this.lines == null )
                this.lines = new LinkedList<>();

            this.lines.add(line);
        }
    }

    final private List<ALTDocPage> pages           = new ArrayList<>();
    final private PrintParameters  printParameters = new PrintParameters();

    final private ALTDoc altDoc;

    private int linesPerPage = -1;

    private Iterator<ALTDocLine> iterLines;

    private ALTDocPrintable( ALTDoc altDoc) {
        this.altDoc = altDoc;
    }

    /** */
    public List<ALTDocPage> pages()
    {
        return this.pages;
    }

    /** */
    private void initLineIterator( )
    {
        final Iterator<ALTDocLine> il = new Iterator<ALTDocLine>() {

            final private Iterator<ALTDocPage> iterPages = pages().iterator();
            private Iterator<ALTDocLine> currentLineIter;
            private ALTDocLine currentLine;

            {
                while( iterPages.hasNext() && currentLine == null )
                {
                    currentLineIter = iterPages.next().lines().iterator();

                    if( currentLineIter.hasNext() )
                        currentLine = currentLineIter.next();
                }

                if( currentLine == null )
                    currentLineIter = null;
            }

            @Override
            public boolean hasNext() {
                return currentLine != null;
            }

            /** */
            private void nextLine()
            {
                if( currentLineIter == null )
                    return;

                if( currentLineIter.hasNext() ) {
                    currentLine = currentLineIter.next();
                    return;
                }

                if( iterPages.hasNext() )
                {
                    final ALTDocPage nextPage = iterPages.next();

                    currentLineIter = nextPage.lines().iterator();

                    if( currentLineIter.hasNext() )
                    {
                        if( currentLine == pageBreakLine )
                            currentLine = currentLineIter.next();
                        else
                            currentLine = pageBreakLine;
                        return;
                    }
                }

                currentLine     = null;
                currentLineIter = null;
            }

            @Override
            public ALTDocLine next() {
                ALTDocLine cl = currentLine;
                nextLine();
                return cl;
            }
        };

        iterLines = il;
    }

    /** */
    @Override
    public int print( Graphics graphics, PageFormat pageFormat, int pageIndex ) throws PrinterException
    {
        // Рассчитываем ОДИН раз при первом вызове
        if( linesPerPage == -1)
        {
            // totalLinesCount();

            initLineIterator();

            PrintParameters pm = new PrintParameters();
            pm.setFontName( "Monospaced" );
            pm.setFontSize( 10 );

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.setFont( pm.getFont() );
            FontMetrics fm = g2d.getFontMetrics();

            // Высота строки с учетом всех компонентов
            int lineHeight = fm.getHeight();

            // Полезная высота (минус верхнее/нижнее поле)
            double usableHeight = pageFormat.getImageableHeight(); // 50px для полей

            linesPerPage = (int) (usableHeight / lineHeight);
            //totalPages   = (int) Math.ceil((double) totalLines / linesPerPage);
        }


        if( !iterLines.hasNext() )
             return NO_SUCH_PAGE;

        int linesCounter = linesPerPage;

        final Graphics2D g2d = (Graphics2D)graphics;
        final Rectangle  rectangle = new Rectangle( 0, 0, (int)pageFormat.getWidth(), (int)pageFormat.getHeight() );
        g2d.clip(rectangle);
        g2d.translate( pageFormat.getImageableX(), pageFormat.getImageableY() );

        final Map<AttributedCharacterIterator.Attribute, Object> attributes = new HashMap<>();

        float xStep = 0.0F; float yStep = 5.0F;

        StringBuilder sb = null;
        ArrayList<Pair<Integer, Integer>> lengths = new ArrayList<>();

        AttributedString as = null;

        for( ALTDocLine line : U.iterable( iterLines ) )
        {
            if( line.isEmpty() )
                continue;

            if( line == pageBreakLine )
                return PAGE_EXISTS;

            lengths.clear();
            lengths.ensureCapacity( line.getElemList().size() );

            sb = new StringBuilder();
            as = null;

            for( ALTDocElem elem : line.getElemList() )
            {
                lengths.add( Pair.makePair( sb.length(), sb.length() + elem.getText().length() ) );
                sb.append  ( elem.getText() );
            }

            if( sb.length() > 0 )
                as = new AttributedString( sb.toString() );

            int i = 0;

            for( ALTDocElem elem : line.getElemList() )
            {
                if( elem.getStyles() != null )
                {
                    for( ALTDocStyle s : elem.getStyles() )
                         s.toPrintParam( this.printParameters );
                }

                this.printParameters.toTextAttributes( attributes );

                if( as != null )
                    as.addAttributes( attributes, lengths.get(i).first, lengths.get(i).second );

                i++;
            }

            xStep = this.printParameters.getLeftIndent();

            if( as != null )
                g2d.drawString( as.getIterator(), xStep, yStep );

            yStep += this.printParameters.getSpaseAfter();

            if( -- linesCounter == 0 )
                break;
        }

        return iterLines.hasNext() ? PAGE_EXISTS : NO_SUCH_PAGE;
    }


    /** */
    public void printToMatrix( PrintService printer ) throws PrintException
    {
        try {

            final DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;

            final PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet( );
            aset.add( MediaSizeName.ISO_A4 );
            aset.add( this.altDoc.getOrientation() );
            aset.add( this.altDoc.getCopies() );
            //aset.add( ALTSettings.INSTANCE().defSetting().getPrintableArea() );

            final InputStream stream;

            try( SegmentedBAOS baos = new SegmentedBAOS(); OutputStreamWriter w = new OutputStreamWriter( baos, StandardCharsets.UTF_8) )
            {
                for( ALTDocPage page : pages() )
                {
                    for(ALTDocLine line : page.lines())
                    {
                        if( !line.isEmpty() )
                        {
                            for (ALTDocElem elem : line.getElemList())
                            {
                                if (elem.getStyles() != null)
                                {
                                    for (ALTDocStyle s : elem.getStyles())
                                        w.append(s.getCommand().getMatrixData().getPrinterCommand());
                                }

                                w.append( elem.getText() );
                            }
                        }
                    }
                }

                stream = baos.inputStream();
            }

            try( InputStream is = stream ) {

                final Doc doc        = new SimpleDoc( is, flavor, null );
                final DocPrintJob pj = printer.createPrintJob( );

                pj.print( doc, aset );
            }

        } catch (Exception e) {
            throw new PrintException(e);
        }
    }


    /** */
    public static ALTDocPrintable load( ALTDoc altDoc) throws IOException {

        try( BufferedReader br = Files.newBufferedReader( altDoc.getAltFile(), altDoc.getCharset() ) )
        {
            final ALTCommandDict dict = ALTSettings.INSTANCE().commandDict();

            ALTDocPrintable doc = new ALTDocPrintable( altDoc );

            String line = null;

            LinkedList<Pair<String, Boolean>> itemList = new LinkedList<>();
            Pair<String, Boolean> itemLine = null;

            boolean insideCommand = false;

            char ch;

            StringBuilder sb = new StringBuilder();

            int ix = -1;

            while( (line = br.readLine() ) != null )
            {
                ix = line.indexOf('`');

                if( ix != -1 )
                    ix = line.indexOf('`', ix + 1);

                if( ix == -1 )
                {
                    itemList.add( Pair.makePair(line, false) );
                }
                else
                {
                    insideCommand = false;

                    if( sb.length() != 0 )
                        sb = new StringBuilder();

                    for( int i = 0; i < line.length(); i++)
                    {
                        ch = line.charAt(i);

                        if (ch == '`')
                        {
                            if (sb.length() != 0)
                            {
                                String text = sb.toString();

                                itemList.add(Pair.makePair(text, insideCommand ));

                                sb = new StringBuilder();
                            }

                            insideCommand = !insideCommand;

                        }
                        else
                        {
                            sb.append(ch);
                        }
                    }

                    if( sb.length() != 0 )
                        itemList.add( Pair.makePair( sb.toString(), Boolean.FALSE ) );
                }

                itemList.add(Pair.makePair(null, Boolean.TRUE));
            }

            boolean doIt = true;

            final Iterator<Pair<String, Boolean>> iter = itemList.descendingIterator();

            while( iter.hasNext() )
            {
                itemLine = iter.next();

                if( !itemLine.second && itemLine.first != null )
                {
                    for( int i = 0; i < itemLine.first.length(); i++ )
                    {
                        if( !Character.isWhitespace( itemLine.first.charAt(i) ) )
                        {
                            doIt = false;
                            break;
                        }
                    }
                }

                if( !doIt )
                    break;

                iter.remove();
            }

            List<ALTDocPage> pageList = doc.pages;
            ALTDocPage currentPage = new ALTDocPage();
            ALTDocLine currentLine = new ALTDocLine();

            final List<ALTDocStyle> styleList = new ArrayList<>();

            String paramValue  = null;
            String commandName = null;

            pageList.add   ( currentPage );

            currentPage.add( currentLine );

            for( Pair<String, Boolean> pair : itemList )
            {
                if( !pair.second )
                {
                    if( styleList.isEmpty() )
                    {
                        currentLine.add( new ALTDocElem(pair.first) );
                    }
                    else
                    {
                        currentLine.add( new ALTDocElem( pair.first, styleList.toArray( new ALTDocStyle[styleList.size()]) ) );
                        styleList.clear();
                    }
                }
                else if (pair.first == null)
                {
                    currentLine = new ALTDocLine();
                    currentPage.add(currentLine);
                }
                else
                {
                    ix = pair.first.indexOf(',');

                    if(ix == -1)
                    {
                        commandName = pair.first;
                        paramValue = null;
                    }
                    else
                    {
                        commandName = pair.first.substring(0, ix);
                        paramValue  = pair.first.substring(ix + 1);
                    }

                    final ALTCommand cmd = dict.getCommand(commandName);

                    if( cmd != null && cmd.getGraphicData() != null )
                    {
                        doIt = true;
                        ALTParameter<?>[] p = cmd.getGraphicData().getParameters();
                        if( p.length == 1 )
                        {
                            switch (p[0].getType()) {
                                case ORIENTATION:
                                    altDoc.setOrientation(((ALTParameter.PageOrientationParameter) p[0]).getValue());
                                    doIt = false;
                                    break;
                                case COPIES:
                                    altDoc.setCopies(((ALTParameter.CopiesParameter) p[0]).getValue());
                                    doIt = false;
                                    break;
                                case PAGE_END:
                                    currentPage = new ALTDocPage();
                                    pageList.add(currentPage);
                                    break;
                                case LF:
                                    currentLine = new ALTDocLine();
                                    currentPage.add(currentLine);
                                    doIt = false;
                                    break;
                                case LEFT: // `LEFT,5`
                                    altDoc.setPageParameter( p[0], p[0].getValue() );
                                    doIt = false;
                                break;
                            }
                        }

                        if (doIt) {
                            ALTDocStyle style = new ALTDocStyle( cmd, paramValue == null ? null : Float.valueOf(paramValue));
                            styleList.add(style);
                        }
                    } else {
                        ALTLog.warning("Неизвестная команда в тексте документа: " + commandName );
                    }
                }
            }
            return doc;
        }
    }
}
