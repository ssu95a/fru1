package ru.inversion.fru.utils.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesSource implements ConfigSource {

   private static final Logger log = LoggerFactory.getLogger(PreferencesSource.class);


   final private boolean userRoot;

   public PreferencesSource(boolean userRoot) {
      this.userRoot = userRoot;
   }

   @Override
   public String name() {
      return userRoot ? "Preferences.userRoot" : "Preferences.systemRoot";
   }

   /** */
   @Override
   public Object get(String key) {

      Preferences preferences = userRoot ? Preferences.userRoot() : Preferences.systemRoot();

      try {

         if( preferences.nodeExists("ALTPRINT") ) {
             return preferences.node("ALTPRINT").get("PATH_ALTPRINT", null);
         }

      } catch( BackingStoreException e ) {
         log.warn( "Error on try get value '" + key + "' from registry", e);
      }
      return null;
   }
}
