# R8 obfuscates everything except what is looked up by name at runtime.
# Every release/nightly build writes its own mapping.txt; keep it with the build to retrace crashes.
-repackageclasses
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- JNI: core-legacy (libosmand.so) finds these classes, fields and methods by name ---
-keepclasseswithmembernames,includedescriptorclasses class * {
	native <methods>;
}
-keep class net.osmand.NativeLibrary { *; }
-keep class net.osmand.NativeLibrary$* { *; }
-keep class net.osmand.plus.render.NativeOsmandLibrary { *; }
-keep class net.osmand.RenderingContext { *; }
-keep class net.osmand.Reshaper { *; }
-keep class net.osmand.binary.RouteDataObject { *; }
-keep class net.osmand.binary.BinaryMapRouteReaderAdapter$RouteRegion { *; }
-keep class net.osmand.binary.BinaryMapRouteReaderAdapter$RouteSubregion { *; }
-keep class net.osmand.render.RenderingRule { *; }
-keep class net.osmand.render.RenderingRuleProperty { *; }
-keep class net.osmand.render.RenderingRuleSearchRequest { *; }
-keep class net.osmand.render.RenderingRulesStorage { *; }
-keep class net.osmand.render.RenderingRuleStorageProperties { *; }
-keep class net.osmand.router.GeneralRouter { *; }
-keep class net.osmand.router.GeneralRouter$* { *; }
-keep class net.osmand.router.HHRouteDataStructure$HHRoutingConfig { *; }
-keep class net.osmand.router.NativeTransport* { *; }
-keep class net.osmand.router.PrecalculatedRouteDirection { *; }
-keep class net.osmand.router.RouteCalculationProgress { *; }
-keep class net.osmand.router.RoutePlannerFrontEnd$RouteCalculationMode { *; }
-keep class net.osmand.router.RouteSegmentResult { *; }
-keep class net.osmand.router.RoutingConfiguration { *; }
-keep class net.osmand.router.RoutingContext { *; }
-keep class net.osmand.router.TransportRoutingConfiguration { *; }
-keep class net.osmand.router.TurnType { *; }
-keep class net.osmand.util.TransliterationHelper { *; }
# Passed to searchNativeObjectsForRendering as "objectWithInterruptedField".
-keepclassmembers class net.osmand.plus.render.MapRenderRepositories {
	boolean interrupted;
	int renderedState;
}

# --- JNI: OsmAndCore SWIG bindings (director upcalls) and Qt ---
-keep class net.osmand.core.jni.** { *; }
-keep class net.osmand.core.android.** { *; }
-keep class org.qtproject.qt5.android.** { *; }

# --- Public AIDL API: Parcelable class names travel to third-party apps ---
-keep class net.osmand.aidl.** { *; }
-keep class net.osmand.aidlapi.** { *; }

# --- Protobuf (vendored runtime + generated OBF/vector-tile messages use reflection) ---
-keep class com.google.protobuf.** { *; }
-keep class net.osmand.binary.OsmandOdb** { *; }
-keep class net.osmand.binary.VectorTile** { *; }

# --- Gson models (field names are the JSON keys; Gson 2.8 reads enum constants by field name) ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class net.osmand.plus.settings.backend.ApplicationModeBean { <init>(); <fields>; }
-keep class net.osmand.plus.download.DownloadOsmandIndexesHelper$AssetEntry { <fields>; }
-keep class net.osmand.plus.resources.ResourceManager$AssetEntryList { <init>(); <fields>; }
-keep class net.osmand.plus.plugins.externalsensors.DevicesSettingsCollection$DeviceSettings { <fields>; }
-keep class net.osmand.plus.liveupdates.Protocol$* { <init>(); <fields>; }
-keep class net.osmand.shared.data.BTDeviceInfo { <fields>; }
-keepclassmembers enum net.osmand.plus.profiles.ProfileIconColors { <fields>; }
-keepclassmembers enum net.osmand.plus.routing.RouteService { <fields>; }
-keepclassmembers enum net.osmand.plus.plugins.externalsensors.DeviceType { <fields>; }
-keepclassmembers enum net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty { <fields>; }
-keep class org.openplacereviews.** { *; }

# --- Reflection on R: poi_*/translation strings and h_*/mm_* rendering icons are read by field name ---
-keepclassmembers class net.osmand.plus.R$string { public static <fields>; }
-keepclassmembers class net.osmand.plus.R$drawable { public static <fields>; }

# --- Constructors called through Class.newInstance()/getConstructor() (full mode drops them otherwise) ---
-keepclassmembers,allowobfuscation class * extends net.osmand.plus.quickaction.QuickAction {
	<init>();
	<init>(net.osmand.plus.quickaction.QuickAction);
}
-keepclassmembers,allowobfuscation class * extends net.osmand.plus.dashboard.DashBaseFragment { <init>(); }
-keepclassmembers,allowobfuscation class * extends androidx.fragment.app.Fragment { public <init>(); }
-keepclassmembers,allowobfuscation class net.osmand.router.HHRouteDataStructure$NetworkDBPoint* { <init>(); }

# --- Class-relative getResourceAsStream("x.xml"): the class must stay in its package ---
-keep,allowshrinking,allowoptimization class net.osmand.osm.MapPoiTypes
-keep,allowshrinking,allowoptimization class net.osmand.osm.MapRenderingTypes
-keep,allowshrinking,allowoptimization class net.osmand.map.OsmandRegions
# ICU4J (Reshaper: Bidi, ArabicShaping) reads data/icudt49b/*.icu relative to its own classes.
-keep,allowshrinking,allowoptimization class com.ibm.icu.**

# --- Class names used as strings ---
-keep class net.osmand.plus.base.EnableStrictMode { <init>(); }
# app:fragment in res/xml preference screens (aapt does not generate rules for them).
-keep class * extends androidx.preference.PreferenceFragmentCompat { <init>(); }
# app:layout_behavior in layouts.
-keep class * extends androidx.coordinatorlayout.widget.CoordinatorLayout$Behavior {
	<init>(android.content.Context, android.util.AttributeSet);
}

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

# Optional dependency surfaces referenced by bundled libraries but not available on Android.
-dontwarn java.beans.**
-dontwarn javax.ws.rs.**
-dontwarn org.immutables.value.**
-dontwarn org.kxml2.io.**
