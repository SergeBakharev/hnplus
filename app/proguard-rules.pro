# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep GeckoView and related classes
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# Keep SnakeYAML classes that are causing issues
-keep class org.yaml.snakeyaml.** { *; }
-dontwarn org.yaml.snakeyaml.**

# Keep Java beans classes that are referenced but not available on Android
-dontwarn java.beans.**
-keep class java.beans.** { *; }

# Keep HTTP client classes
-keep class cz.msebera.android.httpclient.** { *; }
-dontwarn cz.msebera.android.httpclient.**

# Keep your app's main classes
-keep class com.sergebakharev.hnplus.** { *; }

# Keep Android framework classes
-keep class android.** { *; }
-keep interface android.** { *; }

# Keep Kotlin standard library
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep AndroidX classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep AsyncTask for now (deprecated but still used)
-keep class android.os.AsyncTask { *; }

# Keep Handler
-keep class android.os.Handler { *; }

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R classes
-keep class **.R$* {
    public static <fields>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep generic signatures
-keepattributes Signature

# Keep exceptions
-keepattributes Exceptions

# Keep annotations
-keepattributes *Annotation*

# Keep source file names for debugging
-keepattributes SourceFile,LineNumberTable 