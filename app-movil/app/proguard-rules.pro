# Reglas R8 / ProGuard estrictamente necesarias para FoodJet (AGP 9 / R8 full mode).
# Nota: Retrofit 2.11+, Gson 2.11+, Room 2.6+, Kotlin Coroutines, Compose y DataStore
# incorporan sus propias consumer keep rules en sus artefactos AAR/JAR; por ello no se
# agregan reglas amplias o redundantes que degraden la optimización de R8.

# 1. Puente JavaScript <-> Kotlin del WebView de Google Maps (MapaFoodJet.kt -> PuenteMapa).
# El HTML local (assets/mapa_foodjet.html) invoca dinámicamente Android.listo(),
# Android.keyRechazada(), Android.noDisponible(...) y Android.centro(lat, lng).
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 2. Preservación puntual de campos serializados con Gson en DTOs de FoodJet.
# Permite a R8 renombrar clases (allowobfuscation) y eliminar DTOs no usados,
# conservando únicamente los campos anotados con @SerializedName y firmas genéricas.
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers,allowobfuscation class com.example.foodjeetapp.data.remote.dto.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 3. Room 2.6.1 bajo R8 full mode (AGP 9): Room.databaseBuilder() instancia la
# clase generada FoodJetDatabase_Impl por reflexión usando su constructor vacío.
-keep class com.example.foodjeetapp.data.local.FoodJetDatabase
-keep class com.example.foodjeetapp.data.local.FoodJetDatabase_Impl {
    public <init>();
}
