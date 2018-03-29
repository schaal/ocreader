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

#Realm
-dontnote io.realm.Sort
-dontnote io.realm.internal.**
-dontwarn org.conscrypt.*
-dontnote io.reactivex.Flowable

#project
-keep class email.schaal.ocreader.database.model.Item
-keep class email.schaal.ocreader.view.ArticleWebView$1

#Guava
-dontwarn com.google.errorprone.annotations.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.j2objc.annotations.*
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontnote com.google.common.util.concurrent.MoreExecutors
-dontnote com.google.common.cache.Striped64
-dontnote com.google.common.cache.Striped64$Cell
-dontnote com.google.common.util.concurrent.AbstractFuture$UnsafeAtomicHelper

#ACRA
-dontnote org.acra.config.CoreConfiguration
-dontnote org.acra.data.CrashReportData
-dontnote org.acra.collections.ImmutableSet
-dontnote org.acra.builder.ReportBuilder
-dontnote org.acra.config.CoreConfigurationBuilder
-dontnote org.acra.log.ACRALog
