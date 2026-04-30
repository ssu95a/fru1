package ru.inversion.fru.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public final class BuildInfo {

   public static final String UNKNOWN = "unknown";

   private BuildInfo() {
   }

   public static String readLastBuildTime() {

      ClassLoader cl = Thread.currentThread().getContextClassLoader();

      if(cl == null)
         cl = BuildInfo.class.getClassLoader();


      try {

         Enumeration<URL> resources = cl.getResources("META-INF/MANIFEST.MF");

         while (resources.hasMoreElements())
         {
            URL url = resources.nextElement();

            try (InputStream in = url.openStream()) {
               Manifest manifest = new Manifest(in);
               Attributes attrs = manifest.getMainAttributes();

               String value = attrs.getValue("LastBuildTime");

               if (value != null && !value.trim().isEmpty()) {
                  return value.trim();
               }
            }
         }
      }
      catch (IOException e) {
         return UNKNOWN;
      }

      return UNKNOWN;
   }

   public static void installSystemProperties() {
      System.setProperty("app.lastBuildTime", readLastBuildTime());
   }
}