
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
 * JavaxUsbDeviceProxy.c
 *
 * This manages I/O between a LinuxDeviceProxy and usbdevfs device node
 *
 */

static void submitRequest( int fd, jobject linuxRequest );
static void cancelRequest( int fd, jobject linuxRequest );

/* These MUST match those defined in com/ibm/jusb/os/linux/LinuxRequest.java */
#define LINUX_PIPE_REQUEST = 1;
#define LINUX_DCP_REQUEST = 2;
#define LINUX_SET_INTERFACE_REQUEST = 3;
#define LINUX_SET_CONFIGURATION_REQUEST = 4;
#define LINUX_CLAIM_INTERFACE_REQUEST = 5;
#define LINUX_IS_CLAIMED_INTERFACE_REQUEST = 6;
#define LINUX_RELEASE_INTERFACE_REQUEST = 7;
#define LINUX_ISOCHRONOUS_REQUEST = 8;

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

	jclass LinuxDeviceProxy = NULL;
	jobject linuxRequest;
	jstring jkey = NULL;
	jmethodID startCompleted, isRequestWaiting, getReadyRequest, getCancelRequest;
	jmethodID getKey;

	LinuxDeviceProxy = (*env)->GetObjectClass( env, linuxDeviceProxy );
	startCompleted = (*env)->GetMethodID( env, LinuxDeviceProxy, "startCompleted", "(I)V" );
	isRequestWaiting = (*env)->GetMethodID( env, LinuxDeviceProxy, "isRequestWaiting", "()Z" );
	getReadyRequest = (*env)->GetMethodID( env, LinuxDeviceProxy, "getReadyRequest", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	getCancelRequest = (*env)->GetMethodID( env, LinuxDeviceProxy, "getCancelRequest", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	getKey = (*env)->GetMethodID( env, LinuxDeviceProxy, "getKey", "()Ljava/lang/String;" );
	jkey = (*env)->CallObjectMethod( env, linuxDeviceProxy, getKey );

	errno = 0;
	if (0 > (fd = open_device( env, jkey, O_RDWR ))) {
		dbg( MSG_ERROR, "nativeDeviceProxy : Could not open node for device!\n" );
		(*env)->CallVoidMethod( env, linuxDeviceProxy, startupCompleted, errno );
		goto DEVICE_PROXY_CLEANUP;
	}

	(*env)->CallVoidMethod( env, linuxDeviceProxy, startupCompleted, 0 );

	/* run forever...? */
	while (1) {

		/* FIXME - stop using polling! */
		if ( loop_count > MAX_LOOP_COUNT ) {
			usleep( 0 );
			loop_count = 0;
		}
		loop_count ++;

		if (JNI_TRUE == (*env)->CallObjectMethod( env, linuxDeviceProxy, isRequestWaiting )) {
			if ((linuxRequest = (*env)->CallObjectMethod( env, linuxDeviceProxy, getReadyRequest ))) {
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Got Request\n" );
				submitRequest( fd, linuxRequest );
				(*env)->DeleteLocalRef( env, linuxRequest );
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Completed Request\n" );
			}

			if ((linuxRequest = (*env)->CallObjectMethod( env, linuxDeviceProxy, getCancelRequest ))) {
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Got Abort Request\n" );
				cancelRequest( fd, linuxRequest );
				(*env)->DeleteLocalRef( env, linuxRequest );
				dbg( MSG_DEBUG1, "nativeDeviceProxy : Completed Abort Request\n" );
			}
		}

		if (!(ioctl( fd, USBDEVFS_REAPURBNDELAY, &urb ))) {
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Got completed URB\n" );
			linuxRequest = urb->usercontext;
			completeRequest( linuxRequest );
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Finished completed URB\n" );
		}
	}

	dbg( MSG_ERROR, "nativeDeviceProxy : Proxy exiting!  ERROR!\n" );

DEVICE_PROXY_CLEANUP:
	if (0 < fd) close( fd );

	if (LinuxDeviceProxy) (*env)->DeleteLocalRef( env, LinuxDeviceProxy );
	if (jkey) (*env)->DeleteLocalRef( env, jkey );
}

static void submitRequest( int fd, jobject linuxRequest )
{
	int type;

	jclass LinuxRequest;
	jmethodID getType;

	LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
	getType = (*env)->GetMethodID( env, LinuxRequest, "getType", "()Ljava/lang/String;" );
	type = (*env)->CallIntMethod( env, linuxRequest, getType );

	switch (type) {
	case LINUX_PIPE_REQUEST:
		break;
	case LINUX_DCP_REQUEST:
		break;
	case LINUX_SET_INTERFACE_REQUEST:
		break;
	case LINUX_SET_CONFIGURATION_REQUEST:
		break;
	case LINUX_CLAIM_INTERFACE_REQUEST:
		break;
	case LINUX_IS_CLAIMED_INTERFACE_REQUEST:
		break;
	case LINUX_RELEASE_INTERFACE_REQUEST:
		break;
	case LINUX_ISOCHRONOUS_REQUEST:
		break;
	default: /* ? */
		break;
	}
}

static void cancelRequest( int fd, jobject linuxRequest )
{
	
}

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

