
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
 * JavaxUsbDcpRequest.c
 *
 * This handles I/O using the Default Constrol Pipe.
 *
 */

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSubmitDcpRequest
  (JNIEnv *env, jclass JavaxUsb, jobject localLinuxDcpRequest)
{
	struct usbdevfs_urb *urb;
	int result = 0, fd = -1;

	jclass LinuxRequestProxy, LinuxDcpRequest;
	jobject linuxDcpRequest, linuxRequestProxy;
	jmethodID setSubmissionStatus, setSubmitCompleted, removePendingVector, setUrbAddress;
	jmethodID getData, getLinuxRequestProxy, getFileDescriptor;
	jbyteArray data;

	linuxDcpRequest = (*env)->NewGlobalRef( env, localLinuxDcpRequest );
	LinuxDcpRequest = (*env)->GetObjectClass( env, linuxDcpRequest );
	setUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "setUrbAddress", "(I)V" );
	setSubmissionStatus = (*env)->GetMethodID( env, LinuxDcpRequest, "setSubmissionStatus", "(I)V" );
	setSubmitCompleted = (*env)->GetMethodID( env, LinuxDcpRequest, "setSubmitCompleted", "(Z)V" );
	getData = (*env)->GetMethodID( env, LinuxDcpRequest, "getData", "()[B" );
	data = (*env)->CallObjectMethod( env, linuxDcpRequest, getData );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxDcpRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxDcpRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (int)(*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );

	if (!(urb = malloc(sizeof(*urb)))) {
		dbg( MSG_CRITICAL, "nativeSubmitDcpRequest : Out of memory! (%d needed)\n", sizeof(*urb) );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxDcpRequest );
		(*env)->CallVoidMethod( env, linuxDcpRequest, setSubmissionStatus, -ENOMEM );
		(*env)->CallVoidMethod( env, linuxDcpRequest, setSubmitCompleted, JNI_TRUE );
		goto DCP_SUBMIT_CLEANUP;
	}

	memset(urb, 0, sizeof(*urb));

	dbg( MSG_DEBUG2, "nativeSubmitDcpRequest : Submitting URB\n" );

	urb->type = USBDEVFS_URB_TYPE_CONTROL;
	urb->buffer = (*env)->GetByteArrayElements( env, data, NULL );
	urb->buffer_length = (*env)->GetArrayLength( env, data );
#ifdef SIGSUSPEND_WORKS
	urb->signr = URB_NOTIFY_SIGNAL;
#endif /* SIGSUSPEND_WORKS */
	urb->usercontext = linuxDcpRequest;
	urb->endpoint = 0;

	debug_urb( "nativeSubmitDcpRequest", urb );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		result = -errno;

	if (result) {
		dbg( MSG_ERROR, "nativeSubmitDcpRequest : Could not submit URB (errno %d)\n", result );
		(*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxDcpRequest );
		free(urb);
	} else {
		dbg( MSG_DEBUG2, "nativeSubmitDcpRequest : Submitted URB\n" );
		(*env)->CallVoidMethod( env, linuxDcpRequest, setUrbAddress, urb );
	}

	(*env)->CallVoidMethod( env, linuxDcpRequest, setSubmissionStatus, result );
	(*env)->CallVoidMethod( env, linuxDcpRequest, setSubmitCompleted, JNI_TRUE );

DCP_SUBMIT_CLEANUP:
	(*env)->DeleteLocalRef( env, LinuxRequestProxy );
	(*env)->DeleteLocalRef( env, linuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxDcpRequest );
	(*env)->DeleteLocalRef( env, data );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeAbortDcpRequest
  (JNIEnv *env, jclass JavaxUsb, jobject linuxDcpRequest)
{
	struct usbdevfs_urb *urb;
	int fd;

	jclass LinuxDcpRequest, LinuxRequestProxy;
	jobject linuxRequestProxy;
	jmethodID getLinuxRequestProxy, getFileDescriptor, getUrbAddress, setUrbAddress;

	LinuxDcpRequest = (*env)->GetObjectClass( env, linuxDcpRequest );
	getUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "getUrbAddress", "()I" );
	setUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "setUrbAddress", "(I)V" );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxDcpRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxDcpRequest, getLinuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxDcpRequest );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxRequestProxy );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );
	(*env)->DeleteLocalRef( env, linuxRequestProxy );

	dbg( MSG_DEBUG2, "nativeAbortDcpRequest : Canceling URB\n" );

	urb = (struct usbdevfs_urb *)(*env)->CallIntMethod( env, linuxDcpRequest, getUrbAddress );

	if (!urb) {
		dbg( MSG_INFO, "nativeAbortDcpRequest : No URB to cancel\n" );
		return;
	}

	errno = 0;
	if (ioctl( fd, USBDEVFS_DISCARDURB, urb ))
		dbg( MSG_DEBUG2, "nativeAbortDcpRequest : Could not unlink urb %#lx (error %d)\n", (unsigned long)urb, -errno );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeCompleteDcpRequest
  (JNIEnv *env, jclass JavaxUsb, jobject linuxDcpRequest)
{
	struct usbdevfs_urb *urb;
	int result;

	jclass LinuxRequestProxy, LinuxDcpRequest;
	jobject linuxRequestProxy;
	jmethodID setRequestCompleted, setCompletionStatus, getLinuxRequestProxy;
	jmethodID requestCompleted, removePendingVector, getData, getUrbAddress, setUrbAddress;
	jbyteArray data;

	LinuxDcpRequest = (*env)->GetObjectClass( env, linuxDcpRequest );
	getData = (*env)->GetMethodID( env, LinuxDcpRequest, "getData", "()[B" );
	setRequestCompleted = (*env)->GetMethodID( env, LinuxDcpRequest, "setRequestCompleted", "(Z)V" );
	setCompletionStatus = (*env)->GetMethodID( env, LinuxDcpRequest, "setCompletionStatus", "(I)V" );
	getUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "getUrbAddress", "()I" );
	setUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "setUrbAddress", "(I)V" );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxDcpRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxDcpRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	requestCompleted = (*env)->GetMethodID( env, LinuxRequestProxy, "requestCompleted", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );

	dbg( MSG_DEBUG2, "nativeCompleteDcpRequest : Completing URB\n" );

	if (!(urb = (struct usbdevfs_urb*)(*env)->CallIntMethod( env, linuxDcpRequest, getUrbAddress ))) {
		dbg( MSG_ERROR, "nativeCompleteDcpRequest : No URB to complete\n" );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxDcpRequest );
		(*env)->CallVoidMethod( env, linuxDcpRequest, setCompletionStatus, -ENODATA );
		(*env)->CallVoidMethod( env, linuxDcpRequest, setRequestCompleted, JNI_TRUE );
		(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxDcpRequest );
		goto DCP_COMPLETE_CLEANUP;
	}

	debug_urb( "nativeCompleteDcpRequest", urb );

	/* Increase actual length by 8 to account for Setup packet size */
	result = ( urb->status ? urb->status : urb->actual_length + 8 );

	data = (*env)->CallObjectMethod( env, linuxDcpRequest, getData );
	(*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );

	(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxDcpRequest );
	(*env)->CallVoidMethod( env, linuxDcpRequest, setCompletionStatus, result );
	(*env)->CallVoidMethod( env, linuxDcpRequest, setRequestCompleted, JNI_TRUE );
	(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxDcpRequest );
	(*env)->CallVoidMethod( env, linuxDcpRequest, setUrbAddress, 0 );

	free(urb);

	dbg( MSG_DEBUG2, "nativeCompleteDcpRequest : Completed URB\n" );

DCP_COMPLETE_CLEANUP:
	(*env)->DeleteLocalRef( env, LinuxDcpRequest );
	(*env)->DeleteLocalRef( env, linuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxRequestProxy );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetConfiguration
  (JNIEnv *env, jclass JavaxUsb, jobject linuxSetConfigurationRequest)
{
	unsigned int *configuration = NULL;
	int result = 0, fd = -1;

	jclass LinuxRequestProxy, LinuxSetConfigurationRequest;
	jobject linuxRequestProxy;
	jmethodID setSubmissionStatus, setSubmitCompleted, removePendingVector, requestCompleted;
	jmethodID setCompletionStatus, setRequestCompleted;
	jmethodID getConfiguration, getLinuxRequestProxy, getFileDescriptor;

	LinuxSetConfigurationRequest = (*env)->GetObjectClass( env, linuxSetConfigurationRequest );
	setSubmissionStatus = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "setSubmissionStatus", "(I)V" );
	setSubmitCompleted = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "setSubmitCompleted", "(Z)V" );
	setCompletionStatus = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "setCompletionStatus", "(I)V" );
	setRequestCompleted = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "setRequestCompleted", "(Z)V" );
	getConfiguration = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "getConfiguration", "()I" );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxSetConfigurationRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	requestCompleted = (*env)->GetMethodID( env, LinuxRequestProxy, "requestCompleted", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (int)(*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );

	if (!(configuration = malloc(sizeof(*configuration)))) {
		dbg( MSG_CRITICAL, "nativeSetConfigurationRequest : Out of memory! (%d needed)\n", sizeof(*configuration) );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxSetConfigurationRequest );
		(*env)->CallVoidMethod( env, linuxSetConfigurationRequest, setSubmissionStatus, -ENOMEM );
		(*env)->CallVoidMethod( env, linuxSetConfigurationRequest, setSubmitCompleted, JNI_TRUE );
		goto DCP_CONFIGURATION_CLEANUP;
	}

	*configuration = (unsigned int)(*env)->CallIntMethod( env, linuxSetConfigurationRequest, getConfiguration );

	/*
	 * Finish 'submission' before actual ioctl, since ioctl is blocking.
	 */
	(*env)->CallVoidMethod( env, linuxSetConfigurationRequest, setSubmissionStatus, result );
	(*env)->CallVoidMethod( env, linuxSetConfigurationRequest, setSubmitCompleted, JNI_TRUE );

	dbg( MSG_DEBUG2, "nativeSetConfigurationRequest : Setting configuration\n" );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SETCONFIGURATION, configuration ))
		result = -errno;

	if (result) {
		dbg( MSG_ERROR, "nativeSetConfigurationRequest : Could not set configuration (errno %d)\n", result );
	} else {
		dbg( MSG_DEBUG2, "nativeSetConfigurationRequest : Set configuration\n" );
	}

	(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxSetConfigurationRequest );
	(*env)->CallVoidMethod( env, linuxSetConfigurationRequest, setCompletionStatus, result );
	(*env)->CallVoidMethod( env, linuxSetConfigurationRequest, setRequestCompleted, JNI_TRUE );
	(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxSetConfigurationRequest );

	free(configuration);

DCP_CONFIGURATION_CLEANUP:
	(*env)->DeleteLocalRef( env, LinuxRequestProxy );
	(*env)->DeleteLocalRef( env, linuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxSetConfigurationRequest );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetInterface
  (JNIEnv *env, jclass JavaxUsb, jobject linuxSetInterfaceRequest)
{
	struct usbdevfs_setinterface *interface = NULL;
	int result = 0, fd = -1;

	jclass LinuxRequestProxy, LinuxSetInterfaceRequest;
	jobject linuxRequestProxy;
	jmethodID setSubmissionStatus, setSubmitCompleted, removePendingVector, requestCompleted;
	jmethodID setCompletionStatus, setRequestCompleted;
	jmethodID getInterface, getSetting, getLinuxRequestProxy, getFileDescriptor;

	LinuxSetInterfaceRequest = (*env)->GetObjectClass( env, linuxSetInterfaceRequest );
	setSubmissionStatus = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "setSubmissionStatus", "(I)V" );
	setSubmitCompleted = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "setSubmitCompleted", "(Z)V" );
	setCompletionStatus = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "setCompletionStatus", "(I)V" );
	setRequestCompleted = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "setRequestCompleted", "(Z)V" );
	getInterface = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "getInterface", "()I" );
	getSetting = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "getSetting", "()I" );
	getLinuxRequestProxy = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "getLinuxRequestProxy", "()Lcom/ibm/jusb/os/linux/LinuxRequestProxy;" );
	linuxRequestProxy = (*env)->CallObjectMethod( env, linuxSetInterfaceRequest, getLinuxRequestProxy );
	LinuxRequestProxy = (*env)->GetObjectClass( env, linuxRequestProxy );
	removePendingVector = (*env)->GetMethodID( env, LinuxRequestProxy, "removePendingVector", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	requestCompleted = (*env)->GetMethodID( env, LinuxRequestProxy, "requestCompleted", "(Lcom/ibm/jusb/os/linux/LinuxRequest;)V" );
	getFileDescriptor = (*env)->GetMethodID( env, LinuxRequestProxy, "getFileDescriptor", "()I" );
	fd = (int)(*env)->CallIntMethod( env, linuxRequestProxy, getFileDescriptor );

	if (!(interface = malloc(sizeof(*interface)))) {
		dbg( MSG_CRITICAL, "nativeSetInterfaceRequest : Out of memory! (%d needed)\n", sizeof(*interface) );
		(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxSetInterfaceRequest );
		(*env)->CallVoidMethod( env, linuxSetInterfaceRequest, setSubmissionStatus, -ENOMEM );
		(*env)->CallVoidMethod( env, linuxSetInterfaceRequest, setSubmitCompleted, JNI_TRUE );
		goto DCP_INTERFACE_CLEANUP;
	}

	interface->interface = (unsigned int)(*env)->CallIntMethod( env, linuxSetInterfaceRequest, getInterface );
	interface->altsetting = (unsigned int)(*env)->CallIntMethod( env, linuxSetInterfaceRequest, getSetting );

	/*
	 * Finish 'submission' before actual ioctl, since ioctl is blocking.
	 */
	(*env)->CallVoidMethod( env, linuxSetInterfaceRequest, setSubmissionStatus, result );
	(*env)->CallVoidMethod( env, linuxSetInterfaceRequest, setSubmitCompleted, JNI_TRUE );

	dbg( MSG_DEBUG2, "nativeSetInterfaceRequest : Setting interface\n" );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SETINTERFACE, interface ))
		result = -errno;

	if (result) {
		dbg( MSG_ERROR, "nativeSetInterfaceRequest : Could not set interface (errno %d)\n", result );
	} else {
		dbg( MSG_DEBUG2, "nativeSetInterfaceRequest : Set interface\n" );
	}

	(*env)->CallVoidMethod( env, linuxRequestProxy, removePendingVector, linuxSetInterfaceRequest );
	(*env)->CallVoidMethod( env, linuxSetInterfaceRequest, setCompletionStatus, result );
	(*env)->CallVoidMethod( env, linuxSetInterfaceRequest, setRequestCompleted, JNI_TRUE );
	(*env)->CallVoidMethod( env, linuxRequestProxy, requestCompleted, linuxSetInterfaceRequest );

	free(interface);

DCP_INTERFACE_CLEANUP:
	(*env)->DeleteLocalRef( env, LinuxRequestProxy );
	(*env)->DeleteLocalRef( env, linuxRequestProxy );
	(*env)->DeleteLocalRef( env, LinuxSetInterfaceRequest );
}

