# OsmAnd JNI
add_subdirectory("${OSMAND_ROOT}/android/OsmAnd-java/jni" "jni")
add_dependencies(OsmAndJNI OsmAndCore)
