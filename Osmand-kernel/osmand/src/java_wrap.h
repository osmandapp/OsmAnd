#ifndef _JAVA_WRAP_H
#define _JAVA_WRAP_H

#include "jni.h"
#include "binaryRead.h"


struct ResultJNIPublisher : ResultPublisher {
	JNIEnv* env;
	jobject o;
	jfieldID interruptedField;
	ResultJNIPublisher(jobject o, jfieldID interruptedField, JNIEnv* env) :
		o(o), interruptedField(interruptedField), env(env){
	}

	bool isCancelled() {
		if (env != NULL) {
			return env->GetBooleanField(o, interruptedField);
		}
		return false;
	}
};

#endif
