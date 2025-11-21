###############################
# Atributos que NO deben perderse
###############################
# Genéricos, anotaciones y contexto para reflexión/Gson/Retrofit
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations

###############################
# Retrofit / OkHttp
###############################
# Mantén interfaces y evita warnings innecesarios
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Mantener métodos anotados con @retrofit2.http.*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

###############################
# Gson
###############################
# Mantén anotaciones y stream
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# Si usas @SerializedName, evita que se pierdan nombres de campos
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepnames class * {
    @com.google.gson.annotations.SerializedName *;
}

###############################
# TUS MODELOS / DTOs (Reflexión)
###############################
# ⚠️ CLAVE: Mantener *todos los miembros* (campos y getters/setters) porque accedes por reflexión
-keep class com.farenet.descuentos.API.Actual.DTO.** { *; }
-keep class com.farenet.descuentos.ui.solicitudes.model.** { *; }
-keep class com.farenet.descuentos.domain.model.** { *; }
-keep class com.farenet.descuentos.data.local.realm.entity.** { *; }

# (Opcional) Si tienes otros paquetes de datos, descomenta/ajusta:
# -keep class com.farenet.descuentos.API.**.model.** { *; }
# -keep class com.farenet.descuentos.API.**.dto.** { *; }

# Si usas getters/setters genéricos por reflexión, ayuda a no ofuscar nombres comunes
-keepclassmembers class * {
    *** get*(...);
    void set*(***);
}

###############################
# Realm (Java plugin)
###############################
-keep class io.realm.** { *; }
-keep class io.realm.annotations.RealmModule
-keep @io.realm.annotations.RealmModule class * { *; }
-keep class io.realm.internal.Keep
-keep @io.realm.internal.Keep class * { *; }
# Tus modelos Realm (si extienden RealmObject o usan @RealmClass)
-keep class * extends io.realm.RealmObject { *; }
-keep @io.realm.annotations.RealmClass class * { *; }
-dontwarn javax.**

###############################
# Kotlin / Parcelize / Parcelable (por si aplica)
###############################
-keep class ** implements android.os.Parcelable { *; }
-keepclassmembers class ** implements android.os.Parcelable {
    static ** CREATOR;
}

###############################
# AndroidX / Material (reducir ruido de warnings)
###############################
-dontwarn androidx.**
-dontwarn com.google.android.material.**

###############################
# (Opcional) Logging: Timber u otros
###############################
-dontwarn timber.log.**
