
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

#define JAVAXUSB_CLASSNAME "com/ibm/jusb/os/linux/JavaxUsb"

static int fatalLogError = 0;

jboolean tracing = JNI_TRUE;
jboolean trace_default = JNI_TRUE;
jboolean trace_hotplug = JNI_TRUE;
jboolean trace_xfer = JNI_TRUE;
jboolean trace_urb = JNI_FALSE;
int trace_level = LOG_ERROR;

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetTraceData
(JNIEnv *env, jclass JavaxUsb, jboolean enable)
{
	tracing = enable;
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetTraceType
(JNIEnv *env, jclass JavaxUsb, jboolean setting, jstring jname)
{
	const char *name = (*env)->GetStringUTFChars( env, jname, NULL );
	if (!strcmp("default", name))
		trace_default = setting;
	else if (!strcmp("hotplug", name))
		trace_hotplug = setting;
	else if (!strcmp("xfer", name))
		trace_xfer = setting;
	else if (!strcmp("urb", name))
		trace_urb = setting;
	else
		log( LOG_ERROR, "No match for log type %s", name );
	(*env)->ReleaseStringUTFChars( env, jname, name );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetTraceLevel
(JNIEnv *env, jclass JavaxUsb, jint level)
{
	if (LOG_LEVEL_MIN > level || LOG_LEVEL_MAX < level)
		log( LOG_ERROR, "Invalid trace level %d", level );
	else
		trace_level = level;
}

static inline void log_fatal(char *msg)
{
	if (!fatalLogError) {
		fatalLogError = 1;
		fprintf(stderr, "Unable to log : %s\n", msg);
	}
}

void stderr_log(char *logname, int level, char *file, char *func, int line, char *msg)
{
	fprintf(stderr, "[%s](%d) %s.%s[%d] %s\n",logname,level,file,func,line,msg);
}

