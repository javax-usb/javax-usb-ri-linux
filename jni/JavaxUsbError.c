
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/*
 * Get String message for specified error number
 * @author Dan Streetman
 */
JNIEXPORT jstring JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeGetErrorMessage
  (JNIEnv *env, jclass JavaxUsb, jint error) {
	if (0 < error) error = -error;

	switch (error) {
		case -EPERM			: return (*env)->NewStringUTF( env, "Operation not permitted" );
		case -ENOENT		: return (*env)->NewStringUTF( env, "Submission aborted" );
		case -EINTR			: return (*env)->NewStringUTF( env, "Interrupted system call" );
		case -EIO			: return (*env)->NewStringUTF( env, "I/O error" );
		case -ENXIO			: return (*env)->NewStringUTF( env, "Cannot queue certain submissions on Universal Host Controller (unsupported in Linux driver)" );
		case -EAGAIN		: return (*env)->NewStringUTF( env, "Temporarily busy, try again" );
		case -ENOMEM		: return (*env)->NewStringUTF( env, "Out of memory" );
		case -EACCES		: return (*env)->NewStringUTF( env, "Permission denied" );
		case -EBUSY			: return (*env)->NewStringUTF( env, "Device or resource busy" );
		case -ENODEV		: return (*env)->NewStringUTF( env, "Device removed (or no such device)" );
		case -EINVAL		: return (*env)->NewStringUTF( env, "Invalid" );
		case -EPIPE			: return (*env)->NewStringUTF( env, "Broken or stalled pipe" );
		case -ENOSYS		: return (*env)->NewStringUTF( env, "Function not implemented" );
		case -ENODATA		: return (*env)->NewStringUTF( env, "No data available" );
		case -EPROTO		: return (*env)->NewStringUTF( env, "Protocol error" );
		case -EILSEQ		: return (*env)->NewStringUTF( env, "Illegal byte sequence" );
		case -ERESTART		: return (*env)->NewStringUTF( env, "Interrupted system call should be restarted" );
		case -EOPNOTSUPP	: return (*env)->NewStringUTF( env, "Operation not supported on transport endpoint" );
		case -ECONNRESET	: return (*env)->NewStringUTF( env, "Connection reset by peer" );
		case -ENOBUFS 		: return (*env)->NewStringUTF( env, "No buffer space available" );
		case -ETIMEDOUT		: return (*env)->NewStringUTF( env, "Timed out" );
		case -ECONNREFUSED	: return (*env)->NewStringUTF( env, "Connection refused" );
		case -EALREADY		: return (*env)->NewStringUTF( env, "Operation already in progress" );
		case -EINPROGRESS	: return (*env)->NewStringUTF( env, "Operation now in progress" );
		default				: {
			char err[32];
			sprintf(err, "Error %d", (int)error);
			return (*env)->NewStringUTF( env, err );
		}
	}
}

/*
 * Check if specified error is serious (continued error condition)
 * @author Dan Streetman
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeIsErrorSerious
  (JNIEnv *env, jclass JavaxUsb, jint error) {
	if (0 < error) error = -error;

	switch (error) {
		case -ENODEV :
		case -EPIPE :
			return JNI_TRUE;
		default :
			return JNI_FALSE;
	}
}
