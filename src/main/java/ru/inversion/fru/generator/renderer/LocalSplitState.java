package ru.inversion.fru.generator.renderer;

public final class LocalSplitState {

   private boolean active;
   private boolean consumed;
   private String  pending;

   public boolean isActive() {
      return active;
   }

   public void setActive(boolean active) {
      this.active = active;
   }

   public boolean isConsumed() {
      return consumed;
   }

   public void setConsumed(boolean consumed) {
      this.consumed = consumed;
   }

   public String getPending() {
      return pending;
   }

   public void setPending(String pending) {
      this.pending = pending;
   }

   public void reset() {
      active = false;
      consumed = false;
      pending = null;
   }
}