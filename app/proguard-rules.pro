# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

#picasso
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**

#retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-dontwarn android.support.**
-dontnote android.support.**
-dontnote retrofit2.**
-dontnote okio.**
-dontnote okhttp3.**
-dontnote io.realm.**
-dontnote com.mikepenz.**
-dontnote com.google.**
-dontnote com.android.**
-dontnote com.squareup.**

-keepclassmembers class android.support.v4.widget.DrawerLayout {
    private android.support.v4.widget.ViewDragHelper mLeftDragger;
    private android.support.v4.widget.ViewDragHelper mRightDragger;
}

-keepclassmembers class android.support.v4.widget.ViewDragHelper {
    private int mEdgeSize;
}