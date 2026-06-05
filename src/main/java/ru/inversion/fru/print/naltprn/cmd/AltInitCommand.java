package ru.inversion.fru.print.naltprn.cmd;

import ru.inversion.fru.print.altprint.AltPrintPageConfig;
import ru.inversion.fru.print.altprint.doc.styled.StyleState;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.*;
import ru.inversion.utils.Pair;
import ru.inversion.utils.U;

public class AltInitCommand extends AltCommand {

    /** */
    public AltInitCommand(String name, String note) {
        super(name, note);
    }

    /** */
    public OrientationRequested getOrientation() {
        for (AltParameter<?> p : getGraphicData().getParameters()) {
            if (p.getType() == AltParameterTypeEnum.ORIENTATION) {
                return ((AltParameter.PageOrientationParameter) p).getValue();
            }
        }
        return null;
    }

    /** */
    public Copies getCopies() {
        for (AltParameter<?> p : getGraphicData().getParameters()) {
            if (p.getType() == AltParameterTypeEnum.COPIES) {
                return ((AltParameter.CopiesParameter) p).getValue();
            }
        }
        return null;
    }

    /** */
    @Override
    void toCSStyle(StringBuilder sb, Object paramObject) {
        super.toCSStyle(sb, paramObject);
    }

    /** */
    public <T> T getParameter( AltParameterTypeEnum type )
    {
        for( AltParameter<?> p : getGraphicData().getParameters() ) {
             if( p == null )
                 continue;
             if( type == p.getType() )
                 return (T)p.getValue();
        }
        return null;
    }

    /** */
    public void initPrintPageConfig(final AltPrintPageConfig.Builder b )
    {
        U.callIfNotNull( getOrientation(), b::orientation );

        if( getGraphicData() == null || getGraphicData().getParameters() == null )
            return;

        for( AltParameter<?> p : getGraphicData().getParameters() ) {

            if( p == null || p.getType() == null )
                continue;

            switch( p.getType() ) {
                case FONT_NAME:
                    b.fontName(p.getValue().toString());
                    break;
                case FONT_SIZE:
                    b.fontSize((Integer) p.getValue());
                    break;
                case BOLD:
                    b.bold(Boolean.TRUE.equals(p.getValue()));
                    break;
                case ITALIC:
                    b.italic(Boolean.TRUE.equals(p.getValue()));
                    break;
//                case UNDERLINE:
//                    b.underline(Boolean.TRUE.equals(p.getValue()));
//                    break;
                case LEFT:
                    // В INI это left indent (layout), не printableArea
                    b.marginLeftMm( (Float) p.getValue());
                    break;
                case UP:
                    // В INI это upper indent (layout), не printableArea
                    b.marginTopMm((Float) p.getValue());
                    break;
                default:
                    // ORIENTATION/COPIES/Cmd/LF/PAGE_END — не часть StyleState (job-level или runtime-level)
                    break;
            }
        }
    }

    public StyleState toStyleState() {

        // Базовые дефолты (совместимы с altprint defaultPlainStyle / PlainHeaderStyleReader)
        StyleState style = new StyleState("Monospaced", 10, Font.PLAIN, false, 0.0f, 0.5f, 0.5f);

        StyleState.Builder b = style.toBuilder();

        if( getGraphicData() == null || getGraphicData().getParameters() == null ) {
            return b.build();
        }

        for (AltParameter<?> p : getGraphicData().getParameters()) {
            if (p == null || p.getType() == null)
                continue;

            AltParameterTypeEnum type = p.getType();

            if( type == AltParameterTypeEnum.FRU_COMMAND ) {
                b = p.applyTo( b.build(), null ).toBuilder();
            }

            switch (p.getType()) {
                case FONT_NAME:
                    b.fontName((String) p.getValue());
                    break;
                case FONT_SIZE:
                    b.fontSize((Integer) p.getValue());
                    break;
                case BOLD:
                    b.bold(Boolean.TRUE.equals(p.getValue()));
                    break;
                case ITALIC:
                    b.italic(Boolean.TRUE.equals(p.getValue()));
                    break;
                case UNDERLINE:
                    b.underline(Boolean.TRUE.equals(p.getValue()));
                    break;
                case LEFT:
                    // В INI это left indent (layout), не printableArea
                    b.leftIndent((Float) p.getValue());
                    break;
                case UP:
                    // В INI это upper indent (layout), не printableArea
                    b.upperIndent((Float) p.getValue());
                    break;
                case SPACE_AFTER: {
                    // Вертикальный шаг в формате p1/p2 дюйма -> points: p1 * 72 / p2
                    // Для INIT обычно "1/6" => 12pt.
                    @SuppressWarnings("unchecked")
                    Pair<Float, Float> frac = (Pair<Float, Float>) p.getValue();
                    if (frac != null && frac.first != null && frac.second != null && frac.second != 0f) {
                        float pt = frac.first * 72.0f / frac.second;
                        b.verticalMovePt(pt);
                    }
                    // если p1 == null (n/72), без параметра вычислить нельзя -> оставляем дефолт
                    break;
                }

                default:
                    // ORIENTATION/COPIES/Cmd/LF/PAGE_END — не часть StyleState (job-level или runtime-level)
                    break;
            }
        }

        return b.build();
    }
}
