# Add project specific ProGuard rules here.
-keep class com.jiliu.launcher.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
