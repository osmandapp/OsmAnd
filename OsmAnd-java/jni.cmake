# OsmAnd JNI
add_subdirectory("${OSMAND_ROOT}/android/OsmAnd-java" "android/OsmAnd-java")
add_dependencies(OsmAndJNI OsmAndCore)
