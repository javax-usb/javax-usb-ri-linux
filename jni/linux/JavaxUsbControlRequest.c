
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
 * JavaxUsbPipeRequest.c
 *
 * This handles I/O on a (non-Isochronous) pipe
 * For Isochronous pipes, see JavaxUsbIsochronousRequest.c
 *
 */

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSubmitControlRequest
  ( JNIEnv *env, jclass JavaxUsb, jobject localLinuxPipeRequest, jbyte epAddress )
{
	struct usbdevfs_urb *urb;
	int result = 0, fd = -1;

	jclass LinuxRequestProxy, LinuxPipeRequest;
	jobject linuxPipeRequest, linuxRequestProxy;
	jmethodID setSubmissionStatus, setSubmitCompleted, removePendingVector, setUrbAddress;
	jmethodID getData, getAcceptShortPacket, getLinuxRequestProxy, getFileDescriptor;
	jboolean acceptShortPacket;
	jbyteArray data;

	linuxPipeRequest = (*env)->NewGlobalRef( env, localLinuxPipeRequest );
	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	setUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "setUrbAddress", "(I)V" );
	setSubmissionStatus = (*env)->GetMethodID( env, LinuxPipeRequest, "setSubmissionStatus", "(I)V" );
	setSubmitCompleted = (*env)->GetMethodID( env, LinuxPipeRequest, "setSubmitCompleted", "(Z)V" );
	getData = (*env)->GetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	data = (*env)->CallObjectMethod( env, linuxPipeRequest, getData );
	getAcceptShortPacket = (*env)->GetMethodID( env, LinuxPipeRequest, "getAcceptShortPacket", "()Z" );
	acceptShortPacket = (*env)->CallBooleanMethod( env, linuxPipeRequest, getAcceptShortPacket );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxPipeRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxPipeRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (int)(*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );

	if (!(urb = malloc(sizeof(*urb)))) {
		dbg( MSG_CRITICAL, "nativeSubmitControlRequest : Out of memory! (%d needed)\n", sizeof(*urb) );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxPipeRequest );
		(*env)->CallVoidMethod( env, linuxPipeRequest, setSubmissionStatus, -ENOMEM );
		(*env)->CallVoidMethod( env, linuxPipeRequest, setSubmitCompleted, JNI_TRUE );
		return;
	}

	memset(urb, 0, sizeof(*urb));

	dbg( MSG_DEBUG2, "nativeSubmitControlRequest : Submitting URB\n" );

	urb->type = USBDEVFS_URB_TYPE_CONTROL;
	urb->buffer = (*env)->GetByteArrayElements( env, data, NULL );
	urb->buffer_length = (*env)->GetArrayLength( env, data );
#ifdef SIGSUSPEND_WORKS
	urb->signr = URB_NOTIFY_SIGNAL;
#endif /* SIGSUSPEND_WORKS */
	urb->usercontext = linuxPipeRequest;
	urb->endpoint = (unsigned char)epAddress;
	if (JNI_FALSE == acceptShortPacket)
		urb->flags |= USBDEVFS_URB_DISABLE_SPD;

	debug_urb( "nativeSubmitControlRequest", urb );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		result = -errno;

	if (result) {
		dbg( MSG_ERROR, "nativeSubmitControlRequest : Could not submit URB (errno %d)\n", result );
		(*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxPipeRequest );
		free(urb);
	} else {
		dbg( MSG_DEBUG2, "nativeSubmitControlRequest : Submitted URB\n" );
		(*env)->CallVoidMethod( env, linuxPipeRequest, setUrbAddress, urb );
	}

	(*env)->CallVoidMethod( env, linuxPipeRequest, setSubmissionStatus, result );
	(*env)->CallVoidMethod( env, linuxPipeRequest, setSubmitCompleted, JNI_TRUE );

	(*env)->DeleteLocalRef( env, LinuxRequestProxy );
	(*env)->DeleteLocalRef( env, linuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );
	(*env)->DeleteLocalRef( env, data );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeCompleteControlRequest
  ( JNIEnv *env, jclass JavaxUsb, jobject linuxPipeRequest )
{
	struct usbdevfs_urb *urb;
	int result;

	jclass LinuxRequestProxy, LinuxPipeRequest;
	jobject linuxRequestProxy;
	jmethodID setRequestCompleted, setCompletionStatus, getLinuxRequestProxy;
	jmethodID requestCompleted, removePendingVector, getData, getUrbAddress, setUrbAddress;
	jbyteArray data;

	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	getData = (*env)->GetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	setRequestCompleted = (*env)->GetMethodID( env, LinuxPipeRequest, "setRequestCompleted", "(Z)V" );
	setCompletionStatus = (*env)->GetMethodID( env, LinuxPipeRequest, "setCompletionStatus", "(I)V" );
	getUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "getUrbAddress", "()I" );
	setUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "setUrbAddress", "(I)V" );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxPipeRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxPipeRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	requestCompleted = (*env)->GetMethodID( env, LinuxRequestProxy, "requestCompleted", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );

	dbg( MSG_DEBUG2, "nativeCompleteControlRequest : Completing URB\n" );

	if (!(urb = (struct usbdevfs_urb*)(*env)->CallIntMethod( env, linuxPipeRequest, getUrbAddress ))) {
		dbg( MSG_ERROR, "nativeCompleteControlRequest : No URB to complete\n" );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxPipeRequest );
		(*env)->CallVoidMethod( env, linuxPipeRequest, setCompletionStatus, -ENODATA );
		(*env)->CallVoidMethod( env, linuxPipeRequest, setRequestCompleted, JNI_TRUE );
		(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxPipeRequest );
		return;
	}

	debug_urb( "nativeCompleteControlRequest", urb );

	/* Increase actual length by 8 to account for Setup packet size */
	result = ( urb->status ? urb->status : urb->actual_length + 8 );

	data = (*env)->CallObjectMethod( env, linuxPipeRequest, getData );
	(*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );

	(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxPipeRequest );
	(*env)->CallVoidMethod( env, linuxPipeRequest, setCompletionStatus, result );
	(*env)->CallVoidMethod( env, linuxPipeRequest, setRequestCompleted, JNI_TRUE );
	(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxPipeRequest );
	(*env)->CallVoidMethod( env, linuxPipeRequest, setUrbAddress, 0 );

	free(urb);

	dbg( MSG_DEBUG2, "nativeCompleteControlRequest : Completed URB\n" );

	(*env)->DeleteLocalRef( env, LinuxPipeRequest );
	(*env)->DeleteLocalRef( env, linuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxRequestProxy );
}
