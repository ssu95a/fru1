package ru.inversion.fru.parser.tokenizer.states;

import ru.inversion.fru.parser.tokenizer.tokens.BilScriptToken;
import ru.inversion.fru.parser.tokenizer.tokens.SectionHeaderToken;
import ru.inversion.fru.utils.constants.SectionTypeEnum;
import ru.inversion.utils.parser.ITokenHandler;
import ru.inversion.utils.parser.Token;
import ru.inversion.utils.parser.Tokenizer;
import ru.inversion.utils.parser.state.AbstractTokenHandler;
import ru.inversion.utils.io.SegmentedCAW;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static ru.inversion.fru.parser.model.AbstractSectionNode.SCRIPT_PATTERN;
import static ru.inversion.fru.utils.constants.SectionTypeEnum.END;
import static ru.inversion.fru.utils.constants.SectionTypeEnum.ENTRY;
import static ru.inversion.utils.parser.Token.End;

public class FruSectionHeaderHandler extends AbstractTokenHandler<String> {

    /** */
    @Override
    public String getName() {
        return "FruSection";
    }

    /** */
    @Override
    public boolean matches( Tokenizer.IContext ctx ) {
        return ctx.current() == '#' && !ITokenHandler.isSpace( ctx.next() );
    }

    /** */
    @Override
    public Token<String> apply(Tokenizer.IContext t)
    {
        final StringBuilder text = new StringBuilder();

        while( t.shift() && t.current() != ';' && t.current() != '\n' ) {
               text.append( t.current() );
        }

        String s = text.toString();
        final SectionTypeEnum sectionType = SectionTypeEnum.getType(s);

        if( sectionType != null ) {

            if( sectionType == END )
                return End;

            while( t.current() != ';' && t.shift()  ) {
               if( !ITokenHandler.isSpace( t.current() ) )
                    text.append( t.current() );
            }

            s = text.toString();

            if( sectionType == ENTRY )
                while( t.shift( ) && ITokenHandler.isSpace( t.current() ) )
                { }
            else
                while( t.shift( ) && t.current() != ' ' && ITokenHandler.isSpace( t.current() ) )
                { }

            return new SectionHeaderToken(s, sectionType);
        }

        final Matcher matcher = SCRIPT_PATTERN.matcher(s);

        if( matcher.find() )
            return parseScript( s, t );

        return null;
    }

    /** */
    private String parseLine( Tokenizer.IContext t )
    {
        StringBuilder sb = new StringBuilder();

        while( t.shift() && t.current() != ';' && t.current() != '\n' ) {
               sb.append( t.current() );
        }

        String line = sb.toString();

        final Matcher matcher = SCRIPT_PATTERN.matcher(line);

        if( matcher.find() ) {
            //script off
            while( ITokenHandler.isSpace( t.current() ) && t.shift() ) { }
            return null;
        }

        if( "start".equals(line) ) {
            return "start";
        }

        while( t.current() != ';' && t.shift()  ) {
            sb.append( t.current() );
        }

        return sb.toString();
    }

    /** */
    protected BilScriptToken parseScript(String header, Tokenizer.IContext t ) {

        try( Writer caw = new SegmentedCAW() ) {

            //caw.write(header);
            caw.write("//script on");

            String line;
            List<String> impList = null, expList = null;

            do
            {
                if ( t.current() == '#' )
                {
                    if (ITokenHandler.isSpace(t.next()))
                    {
                        Token<String> cmntr = new FruCommentHandler().apply(t);
                        caw.write("//");
                        caw.write(cmntr.getValue());
                        caw.write('\n');
                        continue;
                    }

                    line = parseLine(t);

                    if( line == null )
                    {
                        caw.append("//script end");
                        break;
                    }

                    if( line.startsWith("import") ) {
                        impList = Arrays.stream(line.replaceFirst( "^import\\s*", "" ).replaceAll(";\\s*$", "").trim().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                    }
                    else if( line.startsWith("export") )
                    {
                        expList = Arrays.stream(line.replaceFirst( "^export\\s*", "" ).replaceAll(";\\s*$", "").trim().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                    }
                    else if( line.startsWith("start") )
                        ;//caw.append();
                }
                else
                    caw.write( t.current() );

            } while( t.shift() );

            return new BilScriptToken( caw.toString(), impList, expList );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
