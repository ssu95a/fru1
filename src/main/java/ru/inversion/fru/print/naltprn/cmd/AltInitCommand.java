package ru.inversion.fru.print.naltprn.cmd;

import ru.inversion.fru.print.altprint.PrintSettings;
import ru.inversion.fru.print.altprint.doc.formatted.StyleState;

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.*;
import ru.inversion.utils.Pair;

public class AltInitCommand extends AltCommand {
    public AltInitCommand(String name, String note) {
        super(name, note);
    }

    /**
     *
     */
    public OrientationRequested getOrientation() {
        for (AltParameter<?> p : getGraphicData().getParameters()) {
            if (p.getType() == AltParameterTypeEnum.ORIENTATION) {
                return ((AltParameter.PageOrientationParameter) p).getValue();
            }
        }
        return null;
    }

    /**
     *
     */
    public Copies getCopies() {
        for (AltParameter<?> p : getGraphicData().getParameters()) {
            if (p.getType() == AltParameterTypeEnum.COPIES) {
                return ((AltParameter.CopiesParameter) p).getValue();
            }
        }
        return null;
    }

    @Override
    void toCSStyle(StringBuilder sb, Object paramObject) {
        super.toCSStyle(sb, paramObject);
    }

    public StyleState toStyleState() {
        // Базовые дефолты (совместимы с altprint defaultPlainStyle / PlainHeaderStyleReader)
        StyleState style = new StyleState("Monospaced", 10, Font.PLAIN, false, 0.0f, 0.5f, 0.5f);

        StyleState.Builder b = style.toBuilder();

        if (getGraphicData() == null || getGraphicData().getParameters() == null) {
            return b.build();
        }

        for (AltParameter<?> p : getGraphicData().getParameters()) {
            if (p == null || p.getType() == null)
                continue;

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
                    // В INI это left indent (layout), не printableArea
                    b.upperIndent((Float) p.getValue());
                    break;
                case SPACE_AFTER: {
                    // Вертикальный шаг в формате p1/p2 дюйма -> points: p1 * 72 / p2
                    // Для INIT обычно "1/6" => 12pt.
                    @SuppressWarnings("unchecked")
                    Pair<Float, Float> frac = (Pair<Float, Float>) p.getValue();
                    if (frac != null && frac.first != null && frac.second != null && frac.second != 0f) {
                        float pt = frac.first * 72.0f / frac.second;
                        b.spaceAfter(pt);
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
