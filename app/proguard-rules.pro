# Apache POI ProGuard/R8 Rules
# These rules ignore missing references to Java SE classes not available on Android
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.xmlbeans.**
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn javax.xml.stream.**
-dontwarn java.awt.**
-dontwarn com.sun.javadoc.**
-dontwarn com.sun.tools.javadoc.**
-dontwarn org.w3c.dom.ElementTraversal
-dontwarn org.checkerframework.**
-dontwarn org.apache.jcp.xml.dsig.internal.dom.**

# Log4j and OSGi (often brought in by Apache POI dependencies)
-dontwarn org.apache.logging.log4j.**
-dontwarn org.osgi.framework.**

# Keep Apache POI classes and their members
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.apache.xmlbeans.** { *; }

# Gson rules (if needed, as you have Gson in your dependencies)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter
-keep class com.google.gson.stream.JsonReader
-keep class com.google.gson.stream.JsonWriter
