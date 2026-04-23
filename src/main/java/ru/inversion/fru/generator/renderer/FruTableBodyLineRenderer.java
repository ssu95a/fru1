package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.fields.FruField;
import ru.inversion.fru.model.items.FruFormatCall;
import ru.inversion.fru.model.items.FruItem;
import ru.inversion.fru.model.items.FruLine;
import ru.inversion.fru.model.items.FruText;
import ru.inversion.utils.S;

import java.util.ArrayList;
import java.util.List;

public class FruTableBodyLineRenderer implements IRenderer<FruLine> {

   private static final int MAX_CONTINUATION_PASSES = 1000;

   @Override
   public void render(FruContext context, FruLine line) {
      final FruLineRenderSession oldSession = context.getLineRenderSession();
      final FruLineRenderSession session = new FruLineRenderSession();
      final List<Integer> itemWidths = new ArrayList<>(line.getItems().size());

      context.setLineRenderSession(session);

      try {
         renderMainPass(context, line, itemWidths);

         int guard = 0;

         while (session.hasPending()) {
            if (++guard > MAX_CONTINUATION_PASSES) {
               throw new IllegalStateException("Infinite split continuation detected for line: " + line);
            }

            context.writer().newLine();
            session.nextPass();
            renderContinuationPass(context, line, itemWidths, session);
         }
      }
      finally {
         context.setLineRenderSession(oldSession);
      }
   }

   private void renderMainPass(FruContext context, FruLine line, List<Integer> itemWidths) {
      for (FruItem item : line.getItems()) {
         final int startPos = context.getCurrentPosition();
         context.renderers().render(context, item);
         final int written = Math.max(0, context.getCurrentPosition() - startPos);
         itemWidths.add(written);
      }
   }

   private void renderContinuationPass(
           FruContext context,
           FruLine line,
           List<Integer> itemWidths,
           FruLineRenderSession session
   ) {
      final List<? extends FruItem> items = line.getItems();

      for (int i = 0; i < items.size(); i++) {
         final FruItem item = items.get(i);
         final int width = i < itemWidths.size() ? itemWidths.get(i) : 0;

         if (item instanceof FruField) {
            renderFieldContinuation(context, (FruField) item, width, session);
         }
         else if (item instanceof FruFormatCall) {
            renderFormatCallContinuation(context, (FruFormatCall) item, width, session);
         }
         else if (item instanceof FruText) {
            context.writer().print(maskFrameText(((FruText) item).getText(), width));
         }
         else if (width > 0) {
            context.writer().print(S.space(width, ' '));
         }
      }
   }

   private void renderFieldContinuation(
           FruContext context,
           FruField field,
           int width,
           FruLineRenderSession session
   ) {
      if (field.hasFieldSplit() && session.hasPendingFor(field)) {
         renderAndPad(context, field, width);
      }
      else if (width > 0) {
         context.writer().print(S.space(width, ' '));
      }
   }

   private void renderFormatCallContinuation(
           FruContext context,
           FruFormatCall formatCall,
           int width,
           FruLineRenderSession session
   ) {
      if (hasPendingInFormatCall(formatCall, session)) {
         renderAndPad(context, formatCall, width);
      }
      else if (width > 0) {
         context.writer().print(S.space(width, ' '));
      }
   }

   private boolean hasPendingInFormatCall(FruFormatCall formatCall, FruLineRenderSession session) {
      if (formatCall == null || formatCall.getFields() == null) {
         return false;
      }

      for (FruField field : formatCall.getFields()) {
         if (field != null && field.hasFieldSplit() && session.hasPendingFor(field)) {
            return true;
         }
      }

      return false;
   }

   private void renderAndPad(FruContext context, FruItem item, int width) {
      final int startPos = context.getCurrentPosition();

      context.renderers().render(context, item);

      final int written = Math.max(0, context.getCurrentPosition() - startPos);

      if (written < width) {
         context.writer().print(S.space(width - written, ' '));
      }
   }

   private String maskFrameText(String text, int width) {
      if (S.isNullOrEmpty(text)) {
         return width > 0 ? S.space(width, ' ') : S.EMPTY_STRING;
      }

      final StringBuilder sb = new StringBuilder(Math.max(text.length(), width));

      for (int i = 0; i < text.length(); i++) {
         final char ch = text.charAt(i);

         if (Character.isWhitespace(ch) || isFrameChar(ch)) {
            sb.append(ch);
         }
         else {
            sb.append(' ');
         }
      }

      if (sb.length() < width) {
         sb.append(S.space(width - sb.length(), ' '));
      }

      return sb.toString();
   }

   private boolean isFrameChar(char ch) {
      return ch == '|' || ch == '│' || ch == '║' || ch == '¦';
   }
}