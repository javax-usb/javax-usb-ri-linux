
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/**
 * Submit a dcp request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 * @return The error, or 0.
 */
int dcp_request( JNIEnv *env, int fd, jobject linuxRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0;

	jclass LinuxPipeRequest, linuxPipeRequest = NULL;
	jmethodID setUrbAddress, getData;
	jbyteArray data = NULL;

	linuxDcpRequest = (*env)->NewGlobalRef( env, linuxRequest );
	LinuxDcpRequest = (*env)->GetObjectClass( env, linuxDcpRequest );
	setUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "setUrbAddress", "(I)V" );
	getData = (*env)->GetMethodID( env, LinuxDcpRequest, "getData", "()[B" );
	data = (*env)->CallObjectMethod( env, linuxDcpRequest, getData );
	(*env)->DeleteLocalRef( env, LinuxDcpRequest );

	if (!(urb = malloc(sizeof(*urb))) {
		dbg( MSG_CRITICAL, "dcp_request : Out of memory!\n" );
		ret = -ENOMEM;
		goto end;
	}

	memset(urb, 0, sizeof(*urb));

	urb->buffer = (*env)->GetByteArrayElements( env, data, NULL );
	urb->buffer_length = (*env)->GetArrayLength( env, data );
	urb->endpoint = 0; /* Default Control Pipe is endpoint 0 */
	urb->usercontext = linuxPipeRequest;
	urb->flags |= USBDEVFS_URB_DISABLE_SPD;

	dbg( MSG_DEBUG2, "dcp_request : Submitting URB\n" );
	debug_urb( "dcp_request", urb );

	ret = control_pipe_request( env, fd, linuxPipeRequest, urb );

	if (ret) {
		dbg( MSG_ERROR, "dcp_request : Could not submit URB (errno %d)\n", ret );
	} else {
		dbg( MSG_DEBUG2, "dcp_request : Submitted URB\n" );
		(*env)->CallVoidMethod( env, linuxDcpRequest, setUrbAddress, urb );
	}

end:
	if (ret) {
			if (linuxDcpRequest) (*env)->DeleteGlobalRef( env, linuxDcpRequest );
			if (data && urb && urb->buffer) (*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );
			if (urb) free(urb);
	}
	if (data) (*env)->DeleteLocalRef( env, data );

	return ret;
}

/**
 * Complete a dcp request.
 * @param env The JNIEnv.
 * @param linuxRequest The LinuxRequest.
 * @return The error or 0.
 */
int complete_dcp_request( JNIEnv *env, jobject linuxRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0, type;

	jclass LinuxDcpRequest;
	jmethodID getData, getUrbAddress;
	jbyteArray data;

	LinuxDcpRequest = (*env)->GetObjectClass( env, linuxDcpRequest );
	getData = (*env)->GetMethodID( env, LinuxDcpRequest, "getData", "()[B" );
	getUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "getUrbAddress", "()I" );
	setDataLength = (*env)->GetMethodID( env, LinuxDcpRequest, "setDataLength", "(I)V" );
	(*env)->DeleteLocalRef( env, LinuxDcpRequest );

	if (!(urb = (struct usbdevfs_urb*)(*env)->CallIntMethod( env, linuxDcpRequest, getUrbAddress ))) {
		dbg( MSG_ERROR, "complete_dcp_request : No URB to complete\n" );
		return -EINVAL;
	}

	dbg( MSG_DEBUG2, "complete_dcp_request : Completing URB\n" );
	debug_urb( "complete_dcp_request", urb );

	/* Increase actual length by 8 to account for Setup packet size */
	(*env)->CallVoidMethod( env, linuxDcpRequest, setDataLength, urb->actual_length + 8 );

	ret = urb->status;

	data = (*env)->CallObjectMethod( env, linuxDcpRequest, getData );
	(*env)->ReleaseByteArrayElements( env, data, urb->buffer, 0 );
	(*env)->DeleteLocalRef( env, data );

	free(urb);

	dbg( MSG_DEBUG2, "complete_dcp_request : Completed URB\n" );

	return ret;
}

/**
 * Abort a dcp request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxDcpRequest The LinuxDcpRequest.
 */
void cancel_dcp_request( JNIEnv *env, int fd, jobject linuxDcpRequest )
{
	struct usbdevfs_urb *urb;
	int fd;

	jclass LinuxDcpRequest;
	jmethodID getUrbAddress;

	LinuxDcpRequest = (*env)->GetObjectClass( env, linuxDcpRequest );
	getUrbAddress = (*env)->GetMethodID( env, LinuxDcpRequest, "getUrbAddress", "()I" );
	(*env)->DeleteLocalRef( env, LinuxDcpRequest );

	dbg( MSG_DEBUG2, "cancel_dcp_request : Canceling URB\n" );

	urb = (struct usbdevfs_urb *)(*env)->CallIntMethod( env, linuxDcpRequest, getUrbAddress );

	if (!urb) {
		dbg( MSG_INFO, "cancel_dcp_request : No URB to cancel\n" );
		return;
	}

	errno = 0;
	if (ioctl( fd, USBDEVFS_DISCARDURB, urb ))
		dbg( MSG_DEBUG2, "cancel_dcp_request : Could not unlink urb %#x (error %d)\n", (unsigned int)urb, -errno );
}

/**
 * Set a configuration.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 * @return The error, or 0.
 */
int set_configuration( JNIEnv *env, int fd, jobject linuxRequest )
{
	unsigned int *configuration = NULL;
	int ret = 0;

	jclass LinuxSetConfigurationRequest;
	jmethodID getConfiguration;

	LinuxSetConfigurationRequest = (*env)->GetObjectClass( env, linuxSetConfigurationRequest );
	getConfiguration = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "getConfiguration", "()I" );
	(*env)->DeleteLocalRef( env, LinuxSetConfigurationRequest )

	if (!(configuration = malloc(sizeof(*configuration)))) {
		dbg( MSG_CRITICAL, "set_configuration : Out of memory!\n" );
		return -ENOMEM;
	}

	*configuration = (unsigned int)(*env)->CallIntMethod( env, linuxRequest, getConfiguration );

	dbg( MSG_DEBUG2, "set_configuration : Setting configuration to %d\n", *configuraton );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SETCONFIGURATION, configuration ))
		ret = -errno;

	if (ret)
		dbg( MSG_ERROR, "set_configuration : Could not set configuration (errno %d)\n", ret );
	else
		dbg( MSG_DEBUG2, "set_configuration : Set configuration\n" );

	free(configuration);

	return ret;
}

/**
 * Set a interface setting.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 * @return The error, or 0.
 */
int set_interface( JNIEnv *env, int fd, jobject linuxRequest )
{
	struct usbdevfs_setinterface *interface = NULL;
	int ret = 0;

	jclass LinuxSetInterfaceRequest;
	jmethodID getInterface, getSetting;

	LinuxSetInterfaceRequest = (*env)->GetObjectClass( env, linuxRequest );
	getInterface = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "getInterface", "()I" );
	getSetting = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "getSetting", "()I" );
	(*env)->DeleteLocalRef( env, LinuxSetInterfaceRequest );

	if (!(interface = malloc(sizeof(*interface)))) {
		dbg( MSG_CRITICAL, "set_interface : Out of memory!\n" );
		return -ENOMEM;
	}

	interface->interface = (unsigned int)(*env)->CallIntMethod( env, linuxRequest, getInterface );
	interface->altsetting = (unsigned int)(*env)->CallIntMethod( env, linuxRequest, getSetting );

	dbg( MSG_DEBUG2, "set_interface : Setting interface %d to setting %d\n", interface->interface, interface->altsetting );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SETINTERFACE, interface ))
		ret = -errno;

	if (ret)
		dbg( MSG_ERROR, "set_interface : Could not set interface (errno %d)\n", result );
	else
		dbg( MSG_DEBUG2, "set_interface : Set interface\n" );

	free(interface);

	return ret;
}
