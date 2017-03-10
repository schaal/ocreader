# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

#retrofit
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

-dontnote okhttp3.internal.platform.**
-dontnote io.realm.internal.RealmNotifier
-dontnote io.realm.internal.android.AndroidRealmNotifier
-dontnote io.realm.internal.Collection
-dontnote com.mikepenz.iconics.Iconics
-dontnote com.mikepenz.iconics.view.IconicsImageView
-dontnote com.mikepenz.materialize.view.*
-dontnote com.squareup.moshi.ClassFactory
-dontnote org.apache.http.**
-dontnote android.net.http.**

#project
-keep class email.schaal.ocreader.database.model.Item
-keep class email.schaal.ocreader.view.ArticleWebView$1