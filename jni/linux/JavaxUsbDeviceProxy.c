
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/* These MUST match those defined in com/ibm/jusb/os/linux/LinuxRequest.java */
#define LINUX_PIPE_REQUEST 1
#define LINUX_DCP_REQUEST 2
#define LINUX_SET_INTERFACE_REQUEST 3
#define LINUX_SET_CONFIGURATION_REQUEST 4
#define LINUX_CLAIM_INTERFACE_REQUEST 5
#define LINUX_IS_CLAIMED_INTERFACE_REQUEST 6
#define LINUX_RELEASE_INTERFACE_REQUEST 7
#define LINUX_ISOCHRONOUS_REQUEST 8

static void submitRequest( JNIEnv *env, int fd, jobject linuxRequest );
static void cancelRequest( JNIEnv *env, int fd, jobject linuxRequest );
static void completeRequest( JNIEnv *env, jobject linuxRequest );

/*
 * Proxy for all I/O with a device
 * @author Dan Streetman
 */
JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeDeviceProxy
  ( JNIEnv *env, jclass JavaxUsb, jobject linuxDeviceProxy )
{
	int fd = 0;
	struct usbdevfs_urb *urb;
	int loop_count = 0;

	jclass LinuxDeviceProxy;
	jobject linuxRequest;
	jstring jkey;
	jmethodID startCompleted, isRequestWaiting, getReadyRequest, getCancelRequest;
	jmethodID getKey;

	LinuxDeviceProxy = (*env)->GetObjectClass( env, linuxDeviceProxy );
	startCompleted = (*env)->GetMethodID( env, LinuxDeviceProxy, "startCompleted", "(I)V" );
	isRequestWaiting = (*env)->GetMethodID( env, LinuxDeviceProxy, "isRequestWaiting", "()Z" );
	getReadyRequest = (*env)->GetMethodID( env, LinuxDeviceProxy, "getReadyRequest", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	getCancelRequest = (*env)->GetMethodID( env, LinuxDeviceProxy, "getCancelRequest", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	getKey = (*env)->GetMethodID( env, LinuxDeviceProxy, "getKey", "()Ljava/lang/String;" );
	jkey = (*env)->CallObjectMethod( env, linuxDeviceProxy, getKey );
	(*env)->DeleteLocalRef( env, LinuxDeviceProxy );

	errno = 0;
	fd = open_device( env, jkey, O_RDWR );
	(*env)->DeleteLocalRef( env, jkey );

	if (0 > fd) {
		dbg( MSG_ERROR, "nativeDeviceProxy : Could not open node for device!\n" );
		(*env)->CallVoidMethod( env, linuxDeviceProxy, startCompleted, errno );
		return;
	}

	(*env)->CallVoidMethod( env, linuxDeviceProxy, startCompleted, 0 );

	/* run forever...? */
	while (1) {
		/* FIXME - stop using polling! */
		if ( loop_count > 20 ) {
			usleep( 0 );
			loop_count = 0;
		}
		loop_count ++;

		if (JNI_TRUE == (*env)->CallBooleanMethod( env, linuxDeviceProxy, isRequestWaiting )) {
			if ((linuxRequest = (*env)->CallObjectMethod( env, linuxDeviceProxy, getReadyRequest ))) {
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Got Request\n" );
				submitRequest( env, fd, linuxRequest );
				(*env)->DeleteLocalRef( env, linuxRequest );
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Completed Request\n" );
			}

			if ((linuxRequest = (*env)->CallObjectMethod( env, linuxDeviceProxy, getCancelRequest ))) {
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Got Abort Request\n" );
				cancelRequest( env, fd, linuxRequest );
				(*env)->DeleteLocalRef( env, linuxRequest );
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Completed Abort Request\n" );
			}
		}

		if (!(ioctl( fd, USBDEVFS_REAPURBNDELAY, &urb ))) {
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Got completed URB\n" );
			linuxRequest = urb->usercontext;
			completeRequest( env, linuxRequest );
			(*env)->DeleteGlobalRef( env, linuxRequest );
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Finished completed URB\n" );
		}
	}

	dbg( MSG_ERROR, "nativeDeviceProxy : Proxy exiting!  ERROR!\n" );

	close( fd );
}

/**
 * Submit a LinuxRequest.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 */
static void submitRequest( JNIEnv *env, int fd, jobject linuxRequest )
{
	int type, err, sync = 0;

	jclass LinuxRequest;
	jmethodID getType, setError, setCompleted;

	LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
	getType = (*env)->GetMethodID( env, LinuxRequest, "getType", "()I" );
	setCompleted = (*env)->GetMethodID( env, LinuxRequest, "setCompleted", "(Z)V" );
	setError = (*env)->GetMethodID( env, LinuxRequest, "setError", "(I)V" );
	(*env)->DeleteLocalRef( env, LinuxRequest );

	type = (*env)->CallIntMethod( env, linuxRequest, getType );

	switch (type) {
	case LINUX_PIPE_REQUEST:
		err = pipe_request( env, fd, linuxRequest );
		break;
	case LINUX_DCP_REQUEST:
		err = dcp_request( env, fd, linuxRequest );
		break;
	case LINUX_SET_INTERFACE_REQUEST:
		err = set_interface( env, fd, linuxRequest );
		sync = 1;
		break;
	case LINUX_SET_CONFIGURATION_REQUEST:
		err = set_configuration( env, fd, linuxRequest );
		sync = 1;
		break;
	case LINUX_CLAIM_INTERFACE_REQUEST:
		err = claim_interface( env, fd, 1, linuxRequest );
		sync = 1;
		break;
	case LINUX_RELEASE_INTERFACE_REQUEST:
		err = claim_interface( env, fd, 0, linuxRequest );
		sync = 1;
		break;
	case LINUX_IS_CLAIMED_INTERFACE_REQUEST:
		err = is_claimed( env, fd, linuxRequest );
		sync = 1;
		break;
	case LINUX_ISOCHRONOUS_REQUEST:
		err = isochronous_request( env, fd, linuxRequest );
		break;
	default: /* ? */
		dbg( MSG_ERROR, "submitRequest : Unknown Request type %d\n", type );
		err = -EINVAL;
		break;
	}

	if (err)
		(*env)->CallVoidMethod( env, linuxRequest, setError, err );

	if (sync || err)
		(*env)->CallVoidMethod( env, linuxRequest, setCompleted, JNI_TRUE );
}

/**
 * Cancel a LinuxRequest.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 */
static void cancelRequest( JNIEnv *env, int fd, jobject linuxRequest )
{
	int type;

	jclass LinuxRequest;
	jmethodID getType;

	LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
	getType = (*env)->GetMethodID( env, LinuxRequest, "getType", "()I" );
	(*env)->DeleteLocalRef( env, LinuxRequest );

	type = (*env)->CallIntMethod( env, linuxRequest, getType );

	switch (type) {
	case LINUX_PIPE_REQUEST:
		cancel_pipe_request( env, fd, linuxRequest );
		break;
	case LINUX_DCP_REQUEST:
		cancel_dcp_request( env, fd, linuxRequest );
		break;
	case LINUX_SET_INTERFACE_REQUEST:
	case LINUX_SET_CONFIGURATION_REQUEST:
	case LINUX_CLAIM_INTERFACE_REQUEST:
	case LINUX_IS_CLAIMED_INTERFACE_REQUEST:
	case LINUX_RELEASE_INTERFACE_REQUEST:
		/* cannot abort these synchronous requests */
		break;
	case LINUX_ISOCHRONOUS_REQUEST:
		cancel_isochronous_request( env, fd, linuxRequest );
		break;
	default: /* ? */
		dbg( MSG_ERROR, "cancelRequest : Unknown Request type %d\n", type );
		break;
	}	
}

/**
 * Complete a LinuxRequest.
 * @param env The JNIEnv.
 * @param linuxRequest The LinuxRequest.
 */
static void completeRequest( JNIEnv *env, jobject linuxRequest )
{
	int type, err;

	jclass LinuxRequest;
	jmethodID getType, setError, setCompleted;

	LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
	getType = (*env)->GetMethodID( env, LinuxRequest, "getType", "()I" );
	setCompleted = (*env)->GetMethodID( env, LinuxRequest, "setCompleted", "(Z)V" );
	setError = (*env)->GetMethodID( env, LinuxRequest, "setError", "(I)V" );
	(*env)->DeleteLocalRef( env, LinuxRequest );

	type = (*env)->CallIntMethod( env, linuxRequest, getType );

	switch (type) {
	case LINUX_PIPE_REQUEST:
		err = complete_pipe_request( env, linuxRequest );
		break;
	case LINUX_DCP_REQUEST:
		err = complete_dcp_request( env, linuxRequest );
		break;
	case LINUX_SET_INTERFACE_REQUEST:
	case LINUX_SET_CONFIGURATION_REQUEST:
	case LINUX_CLAIM_INTERFACE_REQUEST:
	case LINUX_IS_CLAIMED_INTERFACE_REQUEST:
	case LINUX_RELEASE_INTERFACE_REQUEST:
		/* these are synchronous, completion happens during submit */
		break;
	case LINUX_ISOCHRONOUS_REQUEST:
		err = complete_isochronous_request( env, linuxRequest );
		break;
	default: /* ? */
		dbg( MSG_ERROR, "completeRequest : Unknown Request type %d\n", type );
		err = -EINVAL;
		break;
	}

	if (err)
		(*env)->CallVoidMethod( env, linuxRequest, setError, err );

	(*env)->CallVoidMethod( env, linuxRequest, setCompleted, JNI_TRUE );
}

/**
 * Debug a URB.
 * @param calling_method The name of the calling method.
 * @param urb The usbdevfs_urb.
 */
inline void debug_urb( char *calling_method, struct usbdevfs_urb *urb )
{
	int i;

	dbg( MSG_DEBUG3, "%s : URB endpoint = %x\n", calling_method, urb->endpoint );
	dbg( MSG_DEBUG3, "%s : URB status = %d\n", calling_method, urb->status );
	dbg( MSG_DEBUG3, "%s : URB signal = %d\n", calling_method, urb->signr );
	dbg( MSG_DEBUG3, "%s : URB buffer length = %d\n", calling_method, urb->buffer_length );
	dbg( MSG_DEBUG3, "%s : URB actual length = %d\n", calling_method, urb->actual_length );
	if (urb->buffer) {
		dbg( MSG_DEBUG3, "%s : URB data = ", calling_method );
		for (i=0; i<urb->buffer_length; i++) dbg( MSG_DEBUG3, "%2.2x ", ((unsigned char *)urb->buffer)[i] );
		dbg( MSG_DEBUG3, "\n" );
	} else {
		dbg( MSG_DEBUG3, "%s : URB data empty!\n", calling_method );
	}

}

