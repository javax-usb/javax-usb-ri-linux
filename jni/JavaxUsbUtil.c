
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

int msg_level = MSG_MIN;

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetMsgLevel
	(JNIEnv *env, jclass JavaxUsb, jint level)
{
	if ( MSG_MIN <= level && level <= MSG_MAX )
		msg_level = level;
}
