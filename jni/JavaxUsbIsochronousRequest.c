
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
		dbg( MSG_CRITICAL, "isochronous_pipe_request : Out of memory!\n" );
		ret = -ENOMEM;
		goto END_SUBMIT;
	}

	(*env)->GetByteArrayRegion( env, data, offset, urb->buffer_length, urb->buffer );

	urb->type = USBDEVFS_URB_TYPE_ISO;
	urb->flags |= USBDEVFS_URB_ISO_ASAP;
	urb->number_of_packets = 1;
	urb->iso_frame_desc[0].length = urb->buffer_length;

	debug_urb( "isochronous_pipe_request", urb );

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
 * Complete a simple isochronous pipe request.
 * @param env The JNIEnv.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_isochronous_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
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

	(*env)->CallVoidMethod( env, linuxPipeRequest, setActualLength, urb->iso_frame_desc[0].actual_length );

	if (data) (*env)->DeleteLocalRef( env, data );
	if (urb->buffer) free(urb->buffer);

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
	jmethodID getAcceptShortPacket, getTotalLength, size, setUrbAddress, getEndpointAddress;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getAcceptShortPacket = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getAcceptShortPacket", "()Z" );
	getTotalLength = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getTotalLength", "()I" );
	size = (*env)->GetMethodID( env, LinuxIsochronousRequest, "size", "()I" );
	setUrbAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setUrbAddress", "(I)V" );
	getEndpointAddress = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getEndpointAddress", "()B" );
	npackets = (unsigned int)(*env)->CallIntMethod( env, linuxIsochronousRequest, size );
	bufsize = (unsigned int)(*env)->CallIntMethod( env, linuxIsochronousRequest, getTotalLength );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	urbsize = sizeof(*urb) + (npackets * sizeof(struct usbdevfs_iso_packet_desc));

	if (!(urb = malloc(urbsize))) {
		dbg( MSG_CRITICAL, "isochronous_request : Out of memory! (%d bytes needed)\n", urbsize );
		ret = -ENOMEM;
		goto ISOCHRONOUS_REQUEST_END;
	}

	memset(urb, 0, urbsize);

	urb->number_of_packets = npackets;
	urb->buffer_length = bufsize;

	if (!(urb->buffer = malloc(urb->buffer_length))) {
		dbg( MSG_CRITICAL, "isochronous_request : Out of memory! (%d needed)\n", urb->buffer_length );
		ret = -ENOMEM;
		goto ISOCHRONOUS_REQUEST_END;
	}

	memset(urb->buffer, 0, urb->buffer_length);

	if ((ret = create_iso_buffer( env, linuxIsochronousRequest, urb )))
		goto ISOCHRONOUS_REQUEST_END;

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
		dbg( MSG_ERROR, "isochronous_request : Could not submit URB (errno %d)\n", ret );
	} else {
		dbg( MSG_DEBUG2, "isochronous_request : Submitted URB\n" );
		(*env)->CallVoidMethod( env, linuxIsochronousRequest, setUrbAddress, urb );
	}

ISOCHRONOUS_REQUEST_END:
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
		dbg( MSG_DEBUG2, "cancel_isochronous_request : Could not unlink urb %p (error %d)\n", urb, -errno );
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
	jmethodID getUrbAddress;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
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
	int i, offset = 0, buffer_offset = 0;

	jclass LinuxIsochronousRequest;
	jmethodID getDirection, getData, getOffset, getLength;
	jbyteArray jbuf;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getDirection = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getDirection", "()B" );
	getData = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getData", "(I)[B" );
	getOffset = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getOffset", "(I)I" );
	getLength = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getLength", "(I)I" );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	/* Copy buffer out ONLY if direction is host->device */
	if (USB_DIR_OUT == (unsigned char)(*env)->CallByteMethod( env, linuxIsochronousRequest, getDirection )) {
		for (i=0; i<urb->number_of_packets; i++) {
			if (!(jbuf = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getData, i ))) {
				dbg( MSG_ERROR, "create_iso_buffer : Could not access data at index %d\n", i );
				return -EINVAL;
			}

			offset = (*env)->CallIntMethod( env, linuxIsochronousRequest, getOffset, i );
			urb->iso_frame_desc[i].length = (*env)->CallIntMethod( env, linuxIsochronousRequest, getLength, i );
			(*env)->GetByteArrayRegion( env, jbuf, offset, urb->iso_frame_desc[i].length, urb->buffer + buffer_offset );
			buffer_offset += urb->iso_frame_desc[i].length;

			(*env)->DeleteLocalRef( env, jbuf );
		}
	}

	return 0;
}

/**
 * Destroy the multi-packet ISO buffer and iso_frame_desc's.
 */
static inline int destroy_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb )
{
	int i, offset = 0, buffer_offset = 0, actual_length = 0;

	jclass LinuxIsochronousRequest;
	jmethodID getDirection, getData, getOffset, setActualLength, setError;
	jbyteArray jbuf;

	LinuxIsochronousRequest = (*env)->GetObjectClass( env, linuxIsochronousRequest );
	getDirection = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getDirection", "()B" );
	getData = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getData", "(I)[B" );
	getOffset = (*env)->GetMethodID( env, LinuxIsochronousRequest, "getOffset", "(I)I" );
	setActualLength = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setActualLength", "(II)V" );
	setError = (*env)->GetMethodID( env, LinuxIsochronousRequest, "setError", "(II)V" );
	(*env)->DeleteLocalRef( env, LinuxIsochronousRequest );

	/* Copy buffer in ONLY if direction is device->host */
	if (USB_DIR_IN == (unsigned char)(*env)->CallByteMethod( env, linuxIsochronousRequest, getDirection )) {
		for (i=0; i<urb->number_of_packets; i++) {
			if (!(jbuf = (*env)->CallObjectMethod( env, linuxIsochronousRequest, getData, i ))) {
				dbg( MSG_ERROR, "destory_iso_buffer : Could not access data buffer at index %d\n", i );
				return -EINVAL;
			}

			offset = (*env)->CallIntMethod( env, linuxIsochronousRequest, getOffset, i );
			actual_length = urb->iso_frame_desc[i].actual_length;
			if ((offset + actual_length) > (*env)->GetArrayLength( env, jbuf )) {
				dbg( MSG_WARNING, "destroy_iso_buffer : WARNING!  Data buffer %d too small, data truncated!\n", i );
				actual_length = (*env)->GetArrayLength( env, jbuf ) - offset;
			}
			(*env)->SetByteArrayRegion( env, jbuf, offset, actual_length, urb->buffer + buffer_offset );
			(*env)->CallVoidMethod( env, linuxIsochronousRequest, setActualLength, i, actual_length );
			if (0 > urb->iso_frame_desc[i].status)
				(*env)->CallVoidMethod( env, linuxIsochronousRequest, setError, i, urb->iso_frame_desc[i].status );
			buffer_offset += urb->iso_frame_desc[i].length;

			(*env)->DeleteLocalRef( env, jbuf );
		}
	}
			
//FIXME - what should we return here, this or something based on each packet's status?
	return urb->status;
}

