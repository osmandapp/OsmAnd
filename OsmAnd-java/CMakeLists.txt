project(OsmAndJNI)

set(target_specific_private_includes "")
set(target_specific_public_libraries "")
if(CMAKE_TARGET_OS STREQUAL "windows")
	set(target_specific_private_includes ${target_specific_private_includes}
		"$ENV{JAVA_HOME}/include/win32"
	)
elseif(CMAKE_TARGET_OS STREQUAL "darwin")
	set(target_specific_private_includes ${target_specific_private_includes}
		"$ENV{JAVA_HOME}/include/darwin"
	)
elseif(CMAKE_TARGET_OS STREQUAL "linux")
	set(target_specific_private_includes ${target_specific_private_includes}
		"$ENV{JAVA_HOME}/include/linux"
	)
endif()

file(GLOB_RECURSE core_includes "${OSMAND_ROOT}/core/include/*.h*")
file(GLOB_RECURSE core_headers "${OSMAND_ROOT}/core/src/*.h*")
file(GLOB_RECURSE core_sources "${OSMAND_ROOT}/core/src/*.c*")
file(GLOB_RECURSE core_headers_client "${OSMAND_ROOT}/core/client/*.h*")
file(GLOB_RECURSE core_sources_client  "${OSMAND_ROOT}/core/client/*.c*")
file(GLOB core_swig "${OSMAND_ROOT}/core.swig")
file(GLOB_RECURSE core_swig_subfiles "${OSMAND_ROOT}/swig/*")
if(CMAKE_HOST_WIN32 AND NOT CYGWIN)
	add_custom_target(OsmAndJNI_generate_swig
		ALL
		bash --login "${CMAKE_CURRENT_LIST_DIR}/generate-swig.sh"
		DEPENDS
			${core_includes}
			${core_headers}
			${core_sources}
			${core_headers_client}
			${core_sources_client}
			${core_swig}
			${core_swig_subfiles}
		COMMENT "Generating sources...")
else()
	add_custom_target(OsmAndJNI_generate_swig
		ALL
		"${CMAKE_CURRENT_LIST_DIR}/generate-swig.sh"
		DEPENDS
			${core_includes}
			${core_headers}
			${core_sources}
			${core_headers_client}
			${core_sources_client}
			${core_swig}
			${core_swig_subfiles}
		COMMENT "Generating sources...")
endif()
set_source_files_properties("${CMAKE_CURRENT_LIST_DIR}/c-src/swig.cpp"
	PROPERTIES
		GENERATED TRUE
)

if(CMAKE_SHARED_LIBS_ALLOWED_ON_TARGET)
	add_library(OsmAndJNI SHARED
		"c-src/swig.cpp"
	)
	target_compile_definitions(OsmAndJNI
		PRIVATE
			-DOSMAND_CORE_EXPORTS
	)
	target_include_directories(OsmAndJNI
		PRIVATE
			"$ENV{JAVA_HOME}/include"
			${target_specific_private_includes}
	)
	add_dependencies(OsmAndJNI
		OsmAndJNI_generate_swig
	)

	if(CMAKE_STATIC_LIBS_ALLOWED_ON_TARGET)
		add_dependencies(OsmAndJNI
			OsmAndCore_static
			OsmAndCoreUtils_static
		)
		target_link_libraries(OsmAndJNI
			LINK_PUBLIC
				OsmAndCore_static
				OsmAndCoreUtils_static
				${target_specific_public_libraries}
		)
	else()
		add_dependencies(OsmAndJNI
			OsmAndCore_shared
			OsmAndCoreUtils_shared
		)
		target_link_libraries(OsmAndJNI
			LINK_PUBLIC
				OsmAndCore_shared
				OsmAndCoreUtils_shared
				${target_specific_public_libraries}
		)
	endif()
endif()
