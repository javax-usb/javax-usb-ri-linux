
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
 * JavaxUsbIsochronousRequest.c
 *
 * This handles I/O on an Isochronous pipe
 *
 */

static inline int create_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb );
static inline int destroy_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb );

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSubmitIsochronousRequest
  ( JNIEnv *env, jclass JavaxUsb, jobject localLinuxIsochronousRequest, jbyte epAddress)
{
	struct usbdevfs_urb *urb;
	int result = 0, fd = -1, npackets, bufsize, urbsize;

	jclass LinuxRequestProxy, LinuxIsochronousRequest;
	jobject linuxIsochronousRequest, linuxRequestProxy;
	jmethodID setSubmissionStatus, setSubmitCompleted, removePendingVector, setUrbAddress;
	jmethodID getData, getAcceptShortPacket, getLinuxRequestProxy, getFileDescriptor;
	jmethodID getNumberOfPackets, getBufferSize;
	jboolean acceptShortPacket;
	jbyteArray data;

	linuxIsochronousRequest = (*env)->NewGlobalRef( env, localLinuxIsochronousRequest );
	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	setUrbAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setUrbAddress", "(I)V" );
	setSubmissionStatus = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setSubmissionStatus", "(I)V" );
	setSubmitCompleted = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setSubmitCompleted", "(Z)V" );
	getNumberOfPackets = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getNumberOfPackets", "()I" );
	getBufferSize = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getBufferSize", "()I" );
	getData = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getData", "()[B" );
	data = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getData );
	getAcceptShortPacket = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getAcceptShortPacket", "()Z" );
	acceptShortPacket = (*env)->CallBooleanMethod( env, linuxIsochronousRequest, getAcceptShortPacket );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (int)(*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );
	npackets = (int)(*env)->CallIntMethod( env, linuxIsochronousRequest, getNumberOfPackets );
	bufsize = (int)(*env)->CallIntMethod( env, linuxIsochronousRequest, getBufferSize );

	urbsize = sizeof(*urb) + (npackets * sizeof(struct usbdevfs_iso_packet_desc));

	if (!(urb = malloc(urbsize))) {
		dbg( MSG_CRITICAL, "nativeSubmitIsochronousRequest : Out of memory! (%d needed)\n", urbsize );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxIsochronousRequest );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setSubmissionStatus, -ENOMEM );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setSubmitCompleted, JNI_TRUE );
		return;
	}

	memset(urb, 0, urbsize);

	urb->number_of_packets = npackets;
	urb->buffer_length = bufsize;

	if ((result = create_iso_buffer( env, linuxIsochronousRequest, urb ))) {
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxIsochronousRequest );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setSubmissionStatus, result );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setSubmitCompleted, JNI_TRUE );
		free(urb);
		return;
	}

	dbg( MSG_DEBUG2, "nativeSubmitIsochronousRequest : Submitting URB\n" );

	urb->type = USBDEVFS_URB_TYPE_ISO;
#ifdef SIGSUSPEND_WORKS
	urb->signr = URB_NOTIFY_SIGNAL;
#endif /* SIGSUSPEND_WORKS */
	urb->usercontext = linuxIsochronousRequest;
	urb->endpoint = (unsigned char)epAddress;
	urb->flags |= USBDEVFS_URB_ISO_ASAP;
	if (JNI_FALSE == acceptShortPacket)
		urb->flags |= USBDEVFS_URB_DISABLE_SPD;

	debug_urb( "nativeSubmitIsochronousRequest", urb );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		result = -errno;

	if (result) {
		dbg( MSG_ERROR, "nativeSubmitIsochronousRequest : Could not submit URB (errno %d)\n", result );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxIsochronousRequest );
		free(urb->buffer);
		free(urb);
	} else {
		dbg( MSG_DEBUG2, "nativeSubmitIsochronousRequest : Submitted URB\n" );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setUrbAddress, urb );
	}

	(*env)->CallVoidMethod( env, linuxIsochronousRequest, setSubmissionStatus, result );
	(*env)->CallVoidMethod( env, linuxIsochronousRequest, setSubmitCompleted, JNI_TRUE );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeCompleteIsochronousRequest
  ( JNIEnv *env, jclass JavaxUsb, jobject linuxIsochronousRequest )
{
	struct usbdevfs_urb *urb;
	int result;

	jclass LinuxRequestProxy, LinuxIsochronousRequest;
	jobject linuxRequestProxy;
	jmethodID setRequestCompleted, setCompletionStatus, getLinuxRequestProxy;
	jmethodID requestCompleted, removePendingVector, getData, getUrbAddress, setUrbAddress;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getData = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getData", "()[B" );
	setRequestCompleted = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setRequestCompleted", "(Z)V" );
	setCompletionStatus = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setCompletionStatus", "(I)V" );
	getUrbAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getUrbAddress", "()I" );
	setUrbAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setUrbAddress", "(I)V" );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	requestCompleted = (*env)->GetMethodID( env, LinuxRequestProxy, "requestCompleted", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );

	dbg( MSG_DEBUG2, "nativeCompleteIsochronousRequest : Completing URB\n" );

	if (!(urb = (struct usbdevfs_urb*)(*env)->CallIntMethod( env, linuxIsochronousRequest, getUrbAddress ))) {
		dbg( MSG_ERROR, "nativeCompleteIsochronousRequest : No URB to complete\n" );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxIsochronousRequest );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setCompletionStatus, -ENODATA );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setRequestCompleted, JNI_TRUE );
		(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxIsochronousRequest );
		return;
	}

	debug_urb( "nativeCompleteIsochronousRequest", urb );

	result = destroy_iso_buffer( env, linuxIsochronousRequest, urb );

	(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxIsochronousRequest );
	(*env)->CallVoidMethod( env, linuxIsochronousRequest, setCompletionStatus, result );
	(*env)->CallVoidMethod( env, linuxIsochronousRequest, setRequestCompleted, JNI_TRUE );
	(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxIsochronousRequest );
	(*env)->CallVoidMethod( env, linuxIsochronousRequest, setUrbAddress, 0 );

	free(urb);

	dbg( MSG_DEBUG2, "nativeCompleteIsochronousRequest : Completed URB\n" );
}

static inline int create_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb )
{
	int i, offset = 0;

	jclass LinuxIsochronousRequest;
	jmethodID getDataBuffer;
	jbyteArray jbuf;

	if (!(urb->buffer = malloc(urb->buffer_length))) {
		dbg( MSG_CRITICAL, "nativeSubmitIsochronousRequest : Out of memory! (%d needed)\n", urb->buffer_length );
		return -ENOMEM;
	}

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getDataBuffer = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getDataBuffer", "(I)[B" );

	for (i=0; i<urb->number_of_packets; i++) {
		if (!(jbuf = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getDataBuffer, i ))) {
			dbg( MSG_ERROR, "nativeSubmitIsochronousRequest : Could not access data buffer at index %d\n", i );
			(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );
			free( urb->buffer );
			return -EINVAL;
		}

		urb->iso_frame_desc[i].length = (*env)->GetArrayLength( env, jbuf );
		(*env)->GetByteArrayRegion( env, jbuf, 0, urb->iso_frame_desc[i].length, urb->buffer + offset );
		offset += urb->iso_frame_desc[i].length;

		(*env)->DeleteLocalRef( env, jbuf );
	}
			
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	return 0;
}

static inline int destroy_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb )
{
	int i, offset = 0;

	jclass LinuxIsochronousRequest;
	jmethodID setStatus, getDataBuffer;
	jbyteArray jbuf;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	setStatus = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setStatus", "(II)V" );
	getDataBuffer = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getDataBuffer", "(I)[B" );

	for (i=0; i<urb->number_of_packets; i++) {
		if (!(jbuf = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getDataBuffer, i ))) {
			dbg( MSG_ERROR, "nativeSubmitIsochronousRequest : Could not access data buffer at index %d\n", i );
			(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );
			free(urb->buffer);
			return -EINVAL;
		}

		if (urb->iso_frame_desc[i].actual_length > (*env)->GetArrayLength( env, jbuf )) {
			dbg( MSG_WARNING, "nativeSubmitIsochronousRequest : WARNING!  Data buffer too small, data truncated!\n" );
			urb->iso_frame_desc[i].actual_length = (*env)->GetArrayLength( env, jbuf );
		}
		(*env)->SetByteArrayRegion( env, jbuf, 0, urb->iso_frame_desc[i].actual_length, urb->buffer + offset );
		if (0 > urb->iso_frame_desc[i].status)
			(*env)->CallVoidMethod( env, linuxIsochronousRequest, setStatus, 0, urb->iso_frame_desc[i].status );
		else
			(*env)->CallVoidMethod( env, linuxIsochronousRequest, setStatus, 0, urb->iso_frame_desc[i].actual_length );
		offset += urb->iso_frame_desc[i].length;

		(*env)->DeleteLocalRef( env, jbuf );
	}
			
	free(urb->buffer);

	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	return urb->status;
}

