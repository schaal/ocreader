# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# AboutLibraries
-keep class .R
-keep class **.R$* {
    <fields>;
}