
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/* These MUST match those defined in com/ibm/jusb/os/linux/LinuxPipeRequest.java */
#define PIPE_CONTROL 1
#define PIPE_BULK 2
#define PIPE_INTERRUPT 3
#define PIPE_ISOCHRONOUS 4

/**
 * Submit a pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 * @return The error, or 0.
 */
int pipe_request( JNIEnv *env, int fd, jobject linuxRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0, type, urbsize;

	jclass LinuxPipeRequest, linuxPipeRequest = NULL;
	jmethodID setUrbAddress, getData, getAcceptShortPacket, getEndpointAddress, getPipeType;
	jboolean acceptShortPacket;
	jbyteArray data = NULL;

	linuxPipeRequest = (*env)->NewGlobalRef( env, linuxRequest );
	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	getEndpointAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "getEndpointAddress", "()B" );
	getPipeType = (*env)->GetMethodID( env, LinuxPipeRequest, "getPipeType", "()I" );
	type = (*env)->CallIntMethod( env, linuxPipeRequest, getPipeType );
	setUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "setUrbAddress", "(I)V" );
	getData = (*env)->GetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	data = (*env)->CallObjectMethod( env, linuxPipeRequest, getData );
	getAcceptShortPacket = (*env)->GetMethodID( env, LinuxPipeRequest, "getAcceptShortPacket", "()Z" );
	acceptShortPacket = (*env)->CallBooleanMethod( env, linuxPipeRequest, getAcceptShortPacket );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );

	urbsize = sizeof(*urb);
	if (PIPE_ISOCHRONOUS == type)
		urbsize += sizeof(struct usbdevfs_iso_packet_desc);

	if (!(urb = malloc(urbsize))) {
		dbg( MSG_CRITICAL, "pipe_request : Out of memory!\n" );
		ret = -ENOMEM;
		goto end;
	}

	memset(urb, 0, sizeof(*urb));

	urb->buffer = (*env)->GetByteArrayElements( env, data, NULL );
	urb->buffer_length = (*env)->GetArrayLength( env, data );
	urb->endpoint = (unsigned char)(*env)->CallByteMethod( env, linuxPipeRequest, getEndpointAddress );
	urb->usercontext = linuxPipeRequest;
	if (JNI_FALSE == acceptShortPacket)
		urb->flags |= NO_ACCEPT_SHORT_PACKET;

	dbg( MSG_DEBUG2, "pipe_request : Submitting URB\n" );
	debug_urb( "pipe_request", urb );

	switch (type) {
	case PIPE_CONTROL: ret = control_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	case PIPE_BULK: ret = bulk_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	case PIPE_INTERRUPT: ret = interrupt_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	case PIPE_ISOCHRONOUS: ret = isochronous_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	default: dbg(MSG_ERROR, "pipe_request : Unknown pipe type %d\n", type ); ret = -EINVAL; break;
	}

	if (ret) {
		dbg( MSG_ERROR, "pipe_request : Could not submit URB (errno %d)\n", ret );
	} else {
		dbg( MSG_DEBUG2, "pipe_request : Submitted URB\n" );
		(*env)->CallVoidMethod( env, linuxPipeRequest, setUrbAddress, urb );
	}

end:
	if (ret) {
			if (linuxPipeRequest) (*env)->DeleteGlobalRef( env, linuxPipeRequest );
			if (data && urb && urb->buffer) (*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );
			if (urb) free(urb);
	}
	if (data) (*env)->DeleteLocalRef( env, data );

	return ret;
}

/**
 * Complete a pipe request.
 * @param env The JNIEnv.
 * @param linuxRequest The LinuxRequest.
 * @return The error or 0.
 */
int complete_pipe_request( JNIEnv *env, jobject linuxPipeRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0, type;

	jclass LinuxPipeRequest;
	jmethodID getData, getPipeType, getUrbAddress;
	jbyteArray data;

	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	getData = (*env)->GetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	getPipeType = (*env)->GetMethodID( env, LinuxPipeRequest, "getPipeType", "()I" );
	getUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "getUrbAddress", "()I" );
	type = (*env)->CallIntMethod( env, linuxPipeRequest, getPipeType );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );

	if (!(urb = (struct usbdevfs_urb*)(*env)->CallIntMethod( env, linuxPipeRequest, getUrbAddress ))) {
		dbg( MSG_ERROR, "complete_pipe_request : No URB to complete\n" );
		return -EINVAL;
	}

	dbg( MSG_DEBUG2, "complete_pipe_request : Completing URB\n" );
	debug_urb( "complete_pipe_request", urb );

	switch (type) {
	case PIPE_CONTROL: ret = complete_control_pipe_request( env, linuxPipeRequest, urb ); break;
	case PIPE_BULK: ret = complete_bulk_pipe_request( env, linuxPipeRequest, urb ); break;
	case PIPE_INTERRUPT: ret = complete_interrupt_pipe_request( env, linuxPipeRequest, urb ); break;
	case PIPE_ISOCHRONOUS: ret = complete_isochronous_pipe_request( env, linuxPipeRequest, urb ); break;
	default: dbg(MSG_ERROR, "complete_pipe_request : Unknown pipe type %d\n", type); ret = -EINVAL; break;
	}

	data = (*env)->CallObjectMethod( env, linuxPipeRequest, getData );
	(*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );
	(*env)->DeleteLocalRef( env, data );

	free(urb);

	dbg( MSG_DEBUG2, "complete_pipe_request : Completed URB\n" );

	return ret;
}

/**
 * Abort a pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxPipeRequest The LinuxPipeRequest.
 */
void cancel_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest )
{
	struct usbdevfs_urb *urb;

	jclass LinuxPipeRequest;
	jmethodID getUrbAddress;

	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	getUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "getUrbAddress", "()I" );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );

	dbg( MSG_DEBUG2, "cancel_pipe_request : Canceling URB\n" );

	urb = (struct usbdevfs_urb *)(*env)->CallIntMethod( env, linuxPipeRequest, getUrbAddress );

	if (!urb) {
		dbg( MSG_INFO, "cancel_pipe_request : No URB to cancel\n" );
		return;
	}

	errno = 0;
	if (ioctl( fd, USBDEVFS_DISCARDURB, urb ))
		dbg( MSG_DEBUG2, "cancel_pipe_request : Could not unlink urb %#x (error %d)\n", (unsigned int)urb, -errno );
}
