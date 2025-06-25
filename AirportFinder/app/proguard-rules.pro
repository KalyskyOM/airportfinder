# Add project specific ProGuard rules here.
-keep class com.airportfinder.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}