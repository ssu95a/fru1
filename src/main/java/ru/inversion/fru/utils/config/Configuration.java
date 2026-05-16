package ru.inversion.fru.utils.config;

import ru.inversion.utils.U;

import java.util.Collections;
import java.util.Map;

public interface Configuration {

   /** */
   String getString(String key, boolean required );

   /** */
   <T> T get( String key, Class<T> typeClass, boolean require );

   /** */
   default String getString(String key, String defValue )
   {
      return U.nvl( getString(key,false), defValue );
   }

   /** */
   default <T> T get( String key, Class<T> typeClass, T defValue )
   {
      return U.nvl( get(key, typeClass, false), defValue );
   }

   /** */
   default String getString(String key )
   {
      return getString(key,false);
   }

   /** */
   default Map<String,Object> snapshot(String prefixFor) {
      return Collections.emptyMap();
   }
}
