# OsmAnd JNI
add_subdirectory("${OSMAND_ROOT}/android/OsmAnd-java" "jni")
add_dependencies(OsmAndJNI OsmAndCore)
