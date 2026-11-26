# Preserva nomes de arquivo e números de linha para stack traces legíveis
-keepattributes SourceFile,LineNumberTable

# Modelos e serializers gerados pelo kotlinx.serialization (backup/restauração JSON)
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.gymslut.app.data.model.**$$serializer { *; }
-keepclassmembers class com.gymslut.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.gymslut.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Enums persistidos no Room / DataStore e serializados no backup
-keepclassmembers enum com.gymslut.app.data.local.entity.EquipmentCategory {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.gymslut.app.data.local.entity.MuscleGroup {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.gymslut.app.core.preferences.ThemeMode {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
