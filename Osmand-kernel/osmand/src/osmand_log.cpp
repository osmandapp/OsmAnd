#ifndef _OSMAND_LOG_CPP
#define _OSMAND_LOG_CPP
#include "osmand_log.h"


#ifdef ANDROID_BUILD
#include <android/log.h>

const char* const LOG_TAG = "net.osmand:native";
void osmand_log_print(int type, const char* msg, ...) {
	va_list args;
	va_start( args, msg);
	if(type == LOG_ERROR) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, msg, args);
	} else if(type == LOG_INFO) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, msg, args);
	} else {
		__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, msg, args);
	}
	va_end(args);
}


#else
#include <stdio.h>
#include <stdarg.h>
const char* const LOG_TAG = "net.osmand:native";
void osmand_log_print(int type, const char* msg, ...) {
	va_list args;
	va_start( args, msg);
	// TODO by type
	printf(msg, args);
	va_end(args);
}


#endif



#endif
