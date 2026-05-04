# Add project specific ProGuard rules here.

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.ads.mediation.** { *; }
-keep class com.google.android.gms.ads.nativead.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep native ad classes
-keep class * extends com.google.android.gms.ads.nativead.NativeAd {
    *;
}
-keep class * extends com.google.android.gms.ads.nativead.NativeAdView {
    *;
}