#ifndef _OSMAND_LOG_H
#define _OSMAND_LOG_H

typedef enum osmand_LogPriority {
	LOG_ERROR = 1,
	LOG_WARN,
	LOG_DEBUG,
	LOG_INFO
} osmand_LogPriority;
//#define _ANDROID_BUILD
void osmand_log_print(int type, const char* msg, ...);


#endif
