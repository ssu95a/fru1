package ru.inversion.fru.api;

import ru.inversion.utils.S;
import ru.inversion.utils.U;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class BuildInfo {

   public static final String UNKNOWN = "unknown";

   private BuildInfo() {
   }

   public static String readLastBuildTime( ) {

      ClassLoader cl = Thread.currentThread().getContextClassLoader();

      if( cl == null )
          cl = BuildInfo.class.getClassLoader();

      try {

         Enumeration<URL> resources = cl.getResources("META-INF/MANIFEST.MF");

         while (resources.hasMoreElements())
         {
            URL url = resources.nextElement();

            String fileName = S.EMPTY_STRING;

            if( "file".equals(url.getProtocol()) ) {
               Path path = Paths.get(url.toURI());
               if (Files.isRegularFile(path)) {
                  if (path.getFileName().toString().toLowerCase().endsWith(".jar"))
                     fileName = ", " + path;
               }
            }
            try( InputStream in = url.openStream() )
            {
               Manifest manifest = new Manifest(in);
               Attributes attrs = manifest.getMainAttributes();

               String value = attrs.getValue("LastBuildTime");

               if( value != null && !value.trim().isEmpty() )
                   return "LastBuildTime: " + value.trim() + fileName;
            }
         }
      }
      catch( Exception e ) {
         return UNKNOWN;
      }

      return UNKNOWN;
   }

   /** */

   /** */
   private static String readForeVersion( ) {

      try {

         final URL location = S.class.getProtectionDomain().getCodeSource().getLocation();

         if( location != null) {

             File file = new File(location.toURI());

             if( file.isFile())
             {
                try( JarFile jar = new JarFile(file)) {

                     StringBuilder sb = new StringBuilder("JInvFore.jar : ");

                     Manifest   manifest = jar.getManifest();
                     Attributes maniAttr = manifest.getMainAttributes();
                     sb.append( U.callIfNotNull( maniAttr.getValue("LastBuildTime"), String::trim ) )
                       .append(", ")
                       .append(U.callIfNotNull( maniAttr.getValue("Implementation-Version"), String::trim ) )
                       .append(", ")
                       .append(file.getAbsoluteFile());

                     return sb.toString();
                }
             }
         }
      }
      catch( Exception e ) {
         return e.getLocalizedMessage();
      }
      return UNKNOWN;
   }

   /** */
   private static String readBilVersion( ) {

      try {

         ScriptEngineManager manager = new ScriptEngineManager();
         for( ScriptEngineFactory factory : manager.getEngineFactories() ) {

            boolean isBil =
                    factory.getEngineName().toLowerCase().contains("bil")
                            || factory.getNames().stream().anyMatch(n -> n.equalsIgnoreCase("bil"))
                            || factory.getClass().getName().toLowerCase().contains("bil");

            if( !isBil )
               continue;

            Class<?> factoryClass = factory.getClass();

            URL location = factoryClass.getProtectionDomain().getCodeSource().getLocation();

            if(!"file".equals(location.getProtocol())) {
                continue;
            }

            Path path = Paths.get(location.toURI());

            if(!path.toString().endsWith(".jar"))
                continue;

            try( JarFile jar = new JarFile(path.toFile()) )
            {
               Manifest   manifest = jar.getManifest();

               //manifest not found
               if( manifest == null )
                   continue;
               StringBuilder sb = new StringBuilder("Bil.jar      : ");

               Attributes maniAttr = manifest.getMainAttributes();
               sb.append( U.callIfNotNull( maniAttr.getValue("LastBuildTime"), String::trim ) )
                 .append(", ")
                 .append(U.callIfNotNull( maniAttr.getValue("Implementation-Version"), String::trim ) )
                 .append(", ")
                 .append(path);

               return sb.toString();
            }
         }
      }
      catch( Exception e ) {
         return e.getLocalizedMessage();
      }
      return UNKNOWN;
   }

   /** */
   public static void installSystemProperties() {
      System.setProperty("app.lastBuildTime", readLastBuildTime());
      System.setProperty("app.foreInfo", readForeVersion());
      System.setProperty("app.bilInfo", readBilVersion() );
   }
}