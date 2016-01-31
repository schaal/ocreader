# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

#picasso
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**

#retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

#realm
-keep class io.realm.annotations.RealmModule
-keep @io.realm.annotations.RealmModule class *
-keep class io.realm.internal.Keep
-keep @io.realm.internal.Keep class * { *; }
-dontwarn javax.**
-dontwarn io.realm.**
