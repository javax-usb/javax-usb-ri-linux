
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
 * JavaxUsbInterfaceRequest.c
 *
 * This handles requests to claim/release interfaces
 *
 */
JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSubmitInterfaceRequest
  ( JNIEnv *env, jclass JavaxUsb, jobject linuxInterfaceRequest )
{
	int *interface = NULL, result = 0, fd = -1;

	jobject linuxRequestProxy;
	jclass LinuxRequestProxy, LinuxInterfaceRequest;
	jmethodID getLinuxRequestProxy, getFileDescriptor, getInterfaceNumber, isClaimRequest;
	jmethodID setSubmitCompleted, setSubmissionStatus, setRequestCompleted, setCompletionStatus;
	jmethodID removePendingVector, requestCompleted;
	jboolean claimRequest;

	if (!(interface = malloc(sizeof(*interface)))) {
		dbg( MSG_CRITICAL, "nativeDeviceProxy.handle_interface : Out of memory!\n" );
		return;
	}

	memset(interface, 0, sizeof(*interface));

	LinuxInterfaceRequest = (*env)->GetObjectClass( env, linuxInterfaceRequest );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxInterfaceRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	getInterfaceNumber = (*env)->GetMethodID( env, LinuxInterfaceRequest, "getInterfaceNumber", "()B" );
	isClaimRequest = (*env)->GetMethodID( env, LinuxInterfaceRequest, "isClaimRequest", "()Z" );
	setSubmitCompleted = (*env)->GetMethodID( env, LinuxInterfaceRequest, "setSubmitCompleted", "(Z)V" );
	setSubmissionStatus = (*env)->GetMethodID( env, LinuxInterfaceRequest, "setSubmissionStatus", "(I)V" );
	setRequestCompleted = (*env)->GetMethodID( env, LinuxInterfaceRequest, "setRequestCompleted", "(Z)V" );
	setCompletionStatus = (*env)->GetMethodID( env, LinuxInterfaceRequest, "setCompletionStatus", "(I)V" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxInterfaceRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (int)(*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	requestCompleted = (*env)->GetMethodID( env, LinuxRequestProxy, "requestCompleted", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	claimRequest = (*env)->CallBooleanMethod( env, linuxInterfaceRequest, isClaimRequest );
	*interface = (int)(*env)->CallByteMethod( env, linuxInterfaceRequest, getInterfaceNumber );

	dbg( MSG_DEBUG2, "nativeSubmitInterfaceRequest : %s interface %d\n", claimRequest ? "Claiming" : "Releasing", *interface );

	(*env)->CallVoidMethod( env, linuxInterfaceRequest, setSubmissionStatus, 0 );
	(*env)->CallVoidMethod( env, linuxInterfaceRequest, setSubmitCompleted, JNI_TRUE );

	errno = 0;
	if (ioctl( fd, claimRequest ? USBDEVFS_CLAIMINTERFACE : USBDEVFS_RELEASEINTERFACE, interface ))
		result = -errno;

	if (result) {
		dbg( MSG_ERROR, "nativeSubmitInterfaceRequest : Could not %s interface %d : errno %d\n",
			claimRequest ? "claim" : "release", *interface, result );
	} else {
		dbg( MSG_DEBUG2, "nativeSubmitInterfaceRequest : %s interface %d\n",
			claimRequest ? "Claimed" : "Released", *interface );
	}

	free(interface);

	(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxInterfaceRequest );
	(*env)->CallVoidMethod( env, linuxInterfaceRequest, setCompletionStatus, result );
	(*env)->CallVoidMethod( env, linuxInterfaceRequest, setRequestCompleted, JNI_TRUE );
	(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxInterfaceRequest );
}

