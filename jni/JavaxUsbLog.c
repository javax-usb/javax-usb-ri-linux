
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

int show_urb_data = 0;

static inline void log_fatal(char *msg)
{
	if (!fatalLogError) {
		fatalLogError = 1;
		fprintf(stderr, "Unable to log : %s\n", msg);
	}
}

void java_log(JNIEnv *env, char *logname, int level, char *file, char *func, int line, char *msg)
{
	jclass JavaxUsb = NULL;
	jstring jlogname = NULL, jfile = NULL, jfunc = NULL, jmsg = NULL;
	jmethodID log;
	jboolean existingException;

	if (fatalLogError)
		return;

	if (JNI_TRUE == (existingException = (*env)->ExceptionCheck(env)))
		(*env)->ExceptionClear(env);

	JavaxUsb = (*env)->FindClass( env, JAVAXUSB_CLASSNAME );

	if (!JavaxUsb || (JNI_TRUE == (*env)->ExceptionCheck(env))) {
		log_fatal("Could not find JavaxUsb class.");
		return;
	}

	log = (*env)->GetStaticMethodID( env, JavaxUsb, "log", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;ILjava/lang/String;)V" );

	if (JNI_TRUE == (*env)->ExceptionCheck(env)) {
		(*env)->ExceptionClear(env);
		log_fatal("JavaxUsb class has no log method.");
		(*env)->DeleteLocalRef( env, JavaxUsb );
		return;
	}

//FIXME - check for OOM error?
	jlogname = (*env)->NewStringUTF( env, logname );
	jfile = (*env)->NewStringUTF( env, file );
	jfunc = (*env)->NewStringUTF( env, func );
	jmsg = (*env)->NewStringUTF( env, msg );

	(*env)->CallStaticVoidMethod( env, JavaxUsb, log, jlogname, level, jfile, jfunc, line, jmsg );

	if (JavaxUsb) (*env)->DeleteLocalRef( env, JavaxUsb );
	if (jlogname) (*env)->DeleteLocalRef( env, jlogname );
	if (jfile) (*env)->DeleteLocalRef( env, jfile );
	if (jfunc) (*env)->DeleteLocalRef( env, jfunc );
	if (jmsg) (*env)->DeleteLocalRef( env, jmsg );
}
