# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

#retrofit
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

-dontwarn okhttp3.internal.**
-dontnote okhttp3.internal.**

-dontnote retrofit2.**
-dontnote io.realm.internal.RealmNotifier
-dontnote com.mikepenz.**
-dontnote com.squareup.moshi.*
-dontnote org.apache.http.**
-dontnote org.apache.commons.codec.**
-dontnote android.net.http.**

#project
-keep class email.schaal.ocreader.database.model.Item
-keep class email.schaal.ocreader.view.ArticleWebView$1