
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
 * This handles I/O on a pipe.
 *
 */

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeAbortPipeRequest
  ( JNIEnv *env, jclass JavaxUsb, jobject linuxPipeRequest )
{
	struct usbdevfs_urb *urb;
	int fd;

	jclass LinuxPipeRequest, LinuxRequestProxy;
	jobject linuxRequestProxy;
	jmethodID getLinuxRequestProxy, getFileDescriptor, getUrbAddress, setUrbAddress;

	LinuxPipeRequest = (*env)->GetObjectClass( env, linuxPipeRequest );
	getUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "getUrbAddress", "()I" );
	setUrbAddress = (*env)->GetMethodID( env, LinuxPipeRequest, "setUrbAddress", "(I)V" );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxPipeRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxPipeRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );

	dbg( MSG_DEBUG2, "nativeAbortPipeRequest : Canceling URB\n" );

	urb = (struct usbdevfs_urb *)(*env)->CallIntMethod( env, linuxPipeRequest, getUrbAddress );

	if (!urb) {
		dbg( MSG_INFO, "nativeAbortPipeRequest : No URB to cancel\n" );
		return;
	}

	errno = 0;
	if (ioctl( fd, USBDEVFS_DISCARDURB, urb ))
		dbg( MSG_DEBUG2, "nativeAbortPipeRequest : Could not unlink urb %#lx (error %d)\n", (unsigned long)urb, -errno );
}

