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

if(CMAKE_HOST_WIN32 AND NOT CYGWIN)
	add_custom_command(OUTPUT "${CMAKE_CURRENT_LIST_DIR}/c-src/swig.cpp"
		COMMAND bash ARGS --login "${CMAKE_CURRENT_LIST_DIR}/generate-swig.sh"
		COMMENT "Generating sources...")
else()
	add_custom_command(OUTPUT "${CMAKE_CURRENT_LIST_DIR}/c-src/swig.cpp"
		COMMAND "${CMAKE_CURRENT_LIST_DIR}/generate-swig.sh"
		COMMENT "Generating sources...")
endif()
add_custom_target(OsmAndJNI_swig
	DEPENDS "${CMAKE_CURRENT_LIST_DIR}/c-src/swig.cpp"
)
set_source_files_properties("${CMAKE_CURRENT_LIST_DIR}/c-src/swig.cpp" PROPERTIES GENERATED TRUE)

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
		OsmAndJNI_swig
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
