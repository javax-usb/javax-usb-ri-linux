
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
 * Submit a interrupt pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param usb The usbdevfs_urb.
 * @return The error that occurred, or 0.
 */
int interrupt_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	int offset = 0;
	int ret = 0;

	jclass LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	jmethodID getData = (*env)->GetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	jmethodID getOffset = (*env)->GetMethodID( env, LinuxPipeRequest, "getOffset", "()I" );
	jmethodID getLength = (*env)->GetMethodID( env, LinuxPipeRequest, "getLength", "()I" );
	jbyteArray data = (*env)->CallObjectMethod( env, linuxPipeRequest, getData );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );

	offset = (unsigned int)(*env)->CallIntMethod( env, linuxPipeRequest, getOffset );
	urb->buffer_length = (unsigned int)(*env)->CallIntMethod( env, linuxPipeRequest, getLength );

	if (!(urb->buffer = malloc(urb->buffer_length))) {
		dbg( MSG_CRITICAL, "interrupt_pipe_request : Out of memory!\n" );
		ret = -ENOMEM;
		goto END_SUBMIT;
	}

	(*env)->GetByteArrayRegion( env, data, offset, urb->buffer_length, urb->buffer );

#ifdef INTERRUPT_USES_BULK
	urb->type = USBDEVFS_URB_TYPE_BULK;
#ifdef QUEUE_BULK
	urb->flags |= QUEUE_BULK;
#endif
#else
	urb->type = USBDEVFS_URB_TYPE_INTERRUPT;
#endif

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		ret = -errno;

END_SUBMIT:
	if (ret)
		if (urb->buffer) free(urb->buffer);

	if (data) (*env)->DeleteLocalRef( env, data );

	return ret;
}

/**
 * Complete a interrupt pipe request.
 * @param env The JNIEnv.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_interrupt_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	jclass LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	jmethodID setActualLength = (*env)->GetMethodID( env, LinuxPipeRequest, "setActualLength", "(I)V" );
	jmethodID getData = (*env)->GetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	jmethodID getOffset = (*env)->GetMethodID( env, LinuxPipeRequest, "getOffset", "()I" );
	jmethodID getLength = (*env)->GetMethodID( env, LinuxPipeRequest, "getLength", "()I" );
	jbyteArray data = (*env)->CallObjectMethod( env, linuxPipeRequest, getData );
	unsigned int offset = (unsigned int)(*env)->CallIntMethod( env, linuxPipeRequest, getOffset );
	unsigned int length = (unsigned int)(*env)->CallIntMethod( env, linuxPipeRequest, getLength );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );

	if (length < urb->actual_length) {
		dbg( MSG_ERROR, "complete_interrupt_pipe_request : Actual length %d greater than requested length %d\n", urb->actual_length, length );
		urb->actual_length = length;
	}

	(*env)->SetByteArrayRegion( env, data, offset, urb->actual_length, urb->buffer );

	(*env)->CallVoidMethod( env, linuxPipeRequest, setActualLength, urb->actual_length );

	if (data) (*env)->DeleteLocalRef( env, data );
	if (urb->buffer) free(urb->buffer);

	return urb->status;
}
