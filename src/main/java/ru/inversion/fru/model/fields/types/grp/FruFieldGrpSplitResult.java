package ru.inversion.fru.model.fields.types.grp;

public final class FruFieldGrpSplitResult {

   private final String visible;
   private final String remainder;

   public FruFieldGrpSplitResult(String visible, String remainder) {
      this.visible = visible == null ? "" : visible;
      this.remainder = remainder == null ? "" : remainder;
   }

   public String getVisible() {
      return visible;
   }

   public String getRemainder() {
      return remainder;
   }
}