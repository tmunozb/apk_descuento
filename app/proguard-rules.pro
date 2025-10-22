# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
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
######## Atributos que NO deben perderse (clave para Retrofit/Gson) ########
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations

######## Retrofit / OkHttp ########
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Mantén métodos anotados con @retrofit2.http.* (para que no se los lleve el shrinker)
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

######## Gson ########
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# Modelos que Gson/Retrofit serializan/deserializan (ajusta a tus paquetes reales)
-keep class com.farenet.descuentos.API.Actual.DTO.** { *; }
-keep class com.farenet.descuentos.domain.model.** { *; }
-keep class com.farenet.descuentos.data.local.realm.entity.** { *; }

######## Realm ########
-keep class io.realm.annotations.RealmModule
-keep @io.realm.annotations.RealmModule class * { *; }
-keep class io.realm.internal.Keep
-keep @io.realm.internal.Keep class * { *; }
-keep class io.realm.** { *; }
-keep class com.farenet.descuentos.** extends io.realm.RealmObject { *; }
-dontwarn javax.**

######## AndroidX/Material (ruido) ########
-dontwarn androidx.**
-dontwarn com.google.android.material.**

######## Kotlin coroutines (si usas) ########
-dontwarn kotlinx.coroutines.**
