
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/**
 * Submit a control pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param usb The usbdevfs_urb.
 * @return The error that occurred, or 0.
 */
int control_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	urb->type = USBDEVFS_URB_TYPE_CONTROL;

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		return -errno;
	else
		return 0;
}

/**
 * Complete a control pipe request.
 * @param env The JNIEnv.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_control_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	jclass LinuxPipeRequest;
	jmethodID setDataLength;

	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	setDataLength = (*env)->GetMethodID( env, LinuxPipeRequest, "setDataLength", "(I)V" );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );

	/* Increase actual length by 8 to account for Setup packet size */
	(*env)->CallVoidMethod( env, linuxPipeRequest, setDataLength, urb->actual_length + 8 );

	return urb->status;
}
