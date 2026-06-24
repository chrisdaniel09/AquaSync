# Keep the entire data model class and its properties completely intact
-keep class dc.aquasync.PumpState { *; }

# Keep Firebase Database classes and reflection helper methods
-keep class com.google.firebase.database.** { *; }
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
}

# Keep Firebase Auth and Anonymous sign-in internals
-keep class com.google.firebase.auth.** { *; }

# Keep Jetpack Compose and Kotlin serialization components
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Suppress warnings for missing Firebase KTX classes
-dontwarn com.google.firebase.ktx.**
