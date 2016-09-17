# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

#retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-dontnote android.support.**
-dontnote retrofit2.**
-dontnote okio.**
-dontnote okhttp3.**
-dontnote io.realm.**
-dontnote com.mikepenz.**
-dontnote com.google.**
-dontnote com.android.**
-dontnote com.squareup.**
-dontnote org.apache.http.**
-dontnote android.net.http.**

#project
-keep class email.schaal.ocreader.database.model.Item
-keep class email.schaal.ocreader.view.ArticleWebView$1