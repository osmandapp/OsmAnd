-dontobfuscate

# First R8 rollout: keep OsmAnd-owned bytecode intact and shrink dependency code.
# OsmAnd still has many reflection and dynamic resource lookup paths.
-keep class net.osmand.** { *; }

# Keep repo-source/vendor packages outside net.osmand as project code too.
-keep class btools.routingapp.** { *; }
-keep class com.example.android.common.view.** { *; }
-keep class com.github.ksoichiro.android.observablescrollview.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class com.jwetherell.openmap.common.** { *; }
-keep class com.wdtinc.mapbox_vector_tile.** { *; }
-keep class io.github.cosinekitty.astronomy.** { *; }
-keep class net.sf.marineapi.** { *; }
-keep class org.openplacereviews.** { *; }

# Gson relies on generic signatures for fields like List<AssetEntry>.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Rhino loads several runtime classes by class name while initializing voice scripts.
-keep class org.mozilla.javascript.VMBridge { *; }
-keep class org.mozilla.javascript.Interpreter { *; }
-keep class org.mozilla.javascript.Interpreter$* { *; }
-keep class org.mozilla.javascript.NativeContinuation { *; }
-keep class org.mozilla.javascript.JavaAdapter { *; }
-keep class org.mozilla.javascript.jdk15.** { *; }
-keep class org.mozilla.javascript.jdk18.** { *; }
-keep class org.mozilla.javascript.regexp.** { *; }
-keep class org.mozilla.javascript.typedarrays.** { *; }
-keep class org.mozilla.javascript.xmlimpl.** { *; }

# Java classes called from Qt/OsmAndCore native code must keep their JNI-visible names and members.
-keep class org.qtproject.qt5.android.** { *; }

# Optional dependency surfaces referenced by bundled libraries but not available on Android.
-dontwarn java.beans.**
-dontwarn javax.ws.rs.**
-dontwarn org.immutables.value.**
-dontwarn org.kxml2.io.**
