
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/* simple isochronous functions */

/**
 * Submit a simple isochronous pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param usb The usbdevfs_urb.
 * @return The error that occurred, or 0.
 */
int isochronous_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	urb->type = USBDEVFS_URB_TYPE_ISO;
	urb->flags |= USBDEVFS_URB_ISO_ASAP;
	urb->number_of_packets = 1;
	urb->iso_frame_desc[0].length = urb->buffer_length;

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		return -errno;
	else
		return 0;
}

/**
 * Complete a simple isochronous pipe request.
 * @param env The JNIEnv.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_isochronous_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	jclass LinuxPipeRequest;
	jmethodID setDataLength;

	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	setDataLength = (*env)->GetMethodID( env, LinuxPipeRequest, "setDataLength", "(I)V" );
	(*env)->DeleteLocalRef( env, LinuxPipeRequest );

	(*env)->CallVoidMethod( env, linuxPipeRequest, setDataLength, urb->iso_frame_desc[0].actual_length );

	return urb->iso_frame_desc[0].status;
}

/* Complex isochronous functions */

static inline int create_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb );
static inline int destroy_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb );

/**
 * Submit a complex isochronous pipe request.
 * Note that this does not support _disabling_ short packets.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxIsochronousRequest The LinuxIsochronousRequest.
 * @return The error that occurred, or 0.
 */
int isochronous_request( JNIEnv *env, int fd, jobject linuxIsochronousRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0, npackets, bufsize, urbsize;

	jclass LinuxIsochronousRequest;
	jmethodID setUrbAddress, setStatus, setError, getNumberOfPackets, getBufferSize, getEndpointAddress;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	setUrbAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setUrbAddress", "(I)V" );
	setStatus = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setStatus", "(II)V" );
	setError = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setError", "(II)V" );
	getNumberOfPackets = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getNumberOfPackets", "()I" );
	getBufferSize = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getBufferSize", "()I" );
	getEndpointAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getEndpointAddress", "()B" );
	npackets = (int)(*env)->CallIntMethod( env, linuxIsochronousRequest, getNumberOfPackets );
	bufsize = (int)(*env)->CallIntMethod( env, linuxIsochronousRequest, getBufferSize );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	urbsize = sizeof(*urb) + (npackets * sizeof(struct usbdevfs_iso_packet_desc));

	if (!(urb = malloc(urbsize))) {
		dbg( MSG_CRITICAL, "isochronous_request : Out of memory! (%d bytes needed)\n", urbsize );
		goto end;
	}

	memset(urb, 0, urbsize);

	urb->number_of_packets = npackets;
	urb->buffer_length = bufsize;

	if (!(urb->buffer = malloc(urb->buffer_length))) {
		dbg( MSG_CRITICAL, "isochronous_request : Out of memory! (%d needed)\n", urb->buffer_length );
		goto end;
	}

	memset(urb->buffer, 0, urb->buffer_length);

	if ((ret = create_iso_buffer( env, linuxIsochronousRequest, urb )))
		goto end;

	urb->type = USBDEVFS_URB_TYPE_ISO;
	urb->usercontext = (*env)->NewGlobalRef( env, linuxIsochronousRequest );
	urb->endpoint = (unsigned char)(*env)->CallByteMethod( env, linuxIsochronousRequest, getEndpointAddress );
	urb->flags |= USBDEVFS_URB_ISO_ASAP;

	dbg( MSG_DEBUG2, "isochronous_request : Submitting URB\n" );
	debug_urb( "isochronous_request", urb );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		ret = -errno;

	if (ret) {
		dbg( MSG_ERROR, "submit_isochronous_request : Could not submit URB (errno %d)\n", ret );
	} else {
		dbg( MSG_DEBUG2, "submit_isochronous_request : Submitted URB\n" );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setUrbAddress, urb );
	}

end:
	if (ret) {
		if (urb) {
			if (urb->usercontext) (*env)->DeleteGlobalRef( env, urb->usercontext);
			if (urb->buffer) free(urb->buffer);
			free(urb);
		}
	}

	return ret;
}

/**
 * Cancel a complex isochronous request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxIsochronousRequest The LinuxIsochronousRequest.
 */
void cancel_isochronous_request( JNIEnv *env, int fd, jobject linuxIsochronousRequest )
{
	struct usbdevfs_urb *urb;

	jclass LinuxIsochronousRequest;
	jmethodID getUrbAddress;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getUrbAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getUrbAddress", "()I" );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	dbg( MSG_DEBUG2, "cancel_isochronous_request : Canceling URB\n" );

	urb = (struct usbdevfs_urb *)(*env)->CallIntMethod( env, linuxIsochronousRequest, getUrbAddress );

	if (!urb) {
		dbg( MSG_INFO, "cancel_isochronous_request : No URB to cancel\n" );
		return;
	}

	errno = 0;
	if (ioctl( fd, USBDEVFS_DISCARDURB, urb ))
		dbg( MSG_DEBUG2, "cancel_isochronous_request : Could not unlink urb %#x (error %d)\n", (unsigned int)urb, -errno );
}

/**
 * Complete a complex isochronous pipe request.
 * @param env The JNIEnv.
 * @param linuxIsochronousRequest The LinuxIsochronousRequest.
 * @return The error that occurred, or 0.
 */
int complete_isochronous_request( JNIEnv *env, jobject linuxIsochronousRequest )
{
	struct usbdevfs_urb *urb;
	int ret;

	jclass LinuxIsochronousRequest;
	jmethodID getData, getUrbAddress;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getData = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getData", "()[B" );
	getUrbAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getUrbAddress", "()I" );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	if (!(urb = (struct usbdevfs_urb*)(*env)->CallIntMethod( env, linuxIsochronousRequest, getUrbAddress ))) {
		dbg( MSG_ERROR, "complete_isochronous_request : No URB to complete!\n" );
		return -EINVAL;
	}

	dbg( MSG_DEBUG2, "complete_isochronous_request : Completing URB\n" );
	debug_urb( "complete_isochronous_request", urb );

	ret = destroy_iso_buffer( env, linuxIsochronousRequest, urb );

	free(urb->buffer);
	free(urb);

	dbg( MSG_DEBUG2, "complete_isochronous_request : Completed URB\n" );

	return ret;
}

/**
 * Create the multi-packet ISO buffer and iso_frame_desc's.
 */
static inline int create_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb )
{
	int i, offset = 0;

	jclass LinuxIsochronousRequest;
	jmethodID getData;
	jbyteArray jbuf;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getData = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getData", "(I)[B" );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	for (i=0; i<urb->number_of_packets; i++) {
		if (!(jbuf = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getData, i ))) {
			dbg( MSG_ERROR, "create_iso_buffer : Could not access data at index %d\n", i );
//FIXME - need to ReleaseByteArrayRegion for data up to this i?
			return -EINVAL;
		}

		urb->iso_frame_desc[i].length = (*env)->GetArrayLength( env, jbuf );
		(*env)->GetByteArrayRegion( env, jbuf, 0, urb->iso_frame_desc[i].length, urb->buffer + offset );
		offset += urb->iso_frame_desc[i].length;

		(*env)->DeleteLocalRef( env, jbuf );
	}

	return 0;
}

/**
 * Destroy the multi-packet ISO buffer and iso_frame_desc's.
 */
static inline int destroy_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb )
{
	int i, offset = 0;

	jclass LinuxIsochronousRequest;
	jmethodID setStatus, getData;
	jbyteArray jbuf;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	setStatus = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setStatus", "(II)V" );
	getData = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getData", "(I)[B" );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	for (i=0; i<urb->number_of_packets; i++) {
		if (!(jbuf = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getData, i ))) {
			dbg( MSG_ERROR, "destory_iso_buffer : Could not access data buffer at index %d\n", i );
//FIXME - release all buffers?
			return -EINVAL;
		}

		if (urb->iso_frame_desc[i].actual_length > (*env)->GetArrayLength( env, jbuf )) {
			dbg( MSG_WARNING, "destroy_iso_buffer : WARNING!  Data buffer %d too small, data truncated!\n", i );
			urb->iso_frame_desc[i].actual_length = (*env)->GetArrayLength( env, jbuf );
		}
		(*env)->SetByteArrayRegion( env, jbuf, 0, urb->iso_frame_desc[i].actual_length, urb->buffer + offset );
		if (0 > urb->iso_frame_desc[i].status)
			(*env)->CallVoidMethod( env, linuxIsochronousRequest, setStatus, i, urb->iso_frame_desc[i].status );
		else
			(*env)->CallVoidMethod( env, linuxIsochronousRequest, setStatus, i, urb->iso_frame_desc[i].actual_length );
		offset += urb->iso_frame_desc[i].length;

		(*env)->DeleteLocalRef( env, jbuf );
	}
			
//FIXME - if not all packets completed (e.g. if URB canceled) then we need to the uncompleted packets to this
	return urb->status;
}

