
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
 * Submit a control pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxControlRequest The LinuxControlRequest.
 * @param usb The usbdevfs_urb.
 * @return The error that occurred, or 0.
 */
int control_pipe_request( JNIEnv *env, int fd, jobject linuxControlRequest, struct usbdevfs_urb *urb )
{
	int offset = 0;
	int ret = 0;

	jclass LinuxControlRequest = (*env)->GetObjectClass( env, linuxControlRequest );
	jmethodID getSetupPacket = (*env)->GetMethodID( env, LinuxControlRequest, "getSetupPacket", "()[B" );
	jmethodID getData = (*env)->GetMethodID( env, LinuxControlRequest, "getData", "()[B" );
	jmethodID getOffset = (*env)->GetMethodID( env, LinuxControlRequest, "getOffset", "()I" );
	jmethodID getLength = (*env)->GetMethodID( env, LinuxControlRequest, "getLength", "()I" );
	jbyteArray setupPacket = (*env)->CallObjectMethod( env, linuxControlRequest, getSetupPacket );
	jbyteArray data = (*env)->CallObjectMethod( env, linuxControlRequest, getData );
	(*env)->DeleteLocalRef( env, LinuxControlRequest );

	offset = (unsigned int)(*env)->CallIntMethod( env, linuxControlRequest, getOffset );
	urb->buffer_length = (unsigned int)(*env)->CallIntMethod( env, linuxControlRequest, getLength );

	if (!(urb->buffer = malloc(urb->buffer_length + 8))) {
		dbg( MSG_CRITICAL, "control_pipe_request : Out of memory!\n" );
		ret = -ENOMEM;
		goto END_SUBMIT;
	}

	(*env)->GetByteArrayRegion( env, setupPacket, 0, 8, urb->buffer );
	(*env)->GetByteArrayRegion( env, data, offset, urb->buffer_length, urb->buffer + 8 );

	urb->type = USBDEVFS_URB_TYPE_CONTROL;

	errno = 0;
	if (ioctl( fd, USBDEVFS_SUBMITURB, urb ))
		ret = -errno;

END_SUBMIT:
	if (ret)
		if (urb->buffer) free(urb->buffer);

	if (setupPacket) (*env)->DeleteLocalRef( env, setupPacket );
	if (data) (*env)->DeleteLocalRef( env, data );

	return ret;
}

/**
 * Complete a control pipe request.
 * @param env The JNIEnv.
 * @param linuxControlRequest The LinuxControlRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_control_pipe_request( JNIEnv *env, jobject linuxControlRequest, struct usbdevfs_urb *urb )
{
	jclass LinuxControlRequest = (*env)->GetObjectClass( env, linuxControlRequest );
	jmethodID setActualLength = (*env)->GetMethodID( env, LinuxControlRequest, "setActualLength", "(I)V" );
	jmethodID getData = (*env)->GetMethodID( env, LinuxControlRequest, "getData", "()[B" );
	jmethodID getOffset = (*env)->GetMethodID( env, LinuxControlRequest, "getOffset", "()I" );
	jmethodID getLength = (*env)->GetMethodID( env, LinuxControlRequest, "getLength", "()I" );
	jbyteArray data = (*env)->CallObjectMethod( env, linuxControlRequest, getData );
	unsigned int offset = (unsigned int)(*env)->CallIntMethod( env, linuxControlRequest, getOffset );
	unsigned int length = (unsigned int)(*env)->CallIntMethod( env, linuxControlRequest, getLength );
	(*env)->DeleteLocalRef( env, LinuxControlRequest );

	if (length < urb->actual_length) {
		dbg( MSG_ERROR, "complete_control_pipe_request : Actual length %d greater than requested length %d\n", urb->actual_length, length );
		urb->actual_length = length;
	}

	(*env)->SetByteArrayRegion( env, data, offset, urb->actual_length, urb->buffer + 8 );

	(*env)->CallVoidMethod( env, linuxPipeRequest, setActualLength, urb->actual_length );

	if (data) (*env)->DeleteLocalRef( env, data );
	if (urb->buffer) free(urb->buffer);

	return urb->status;
}

/**
 * Set a configuration.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxSetConfigurationRequest The LinuxSetConfigurationRequest.
 * @return The error, or 0.
 */
int set_configuration( JNIEnv *env, int fd, jobject linuxSetConfigurationRequest )
{
	unsigned int *configuration = NULL;
	int ret = 0;

	jclass LinuxSetConfigurationRequest;
	jmethodID getConfiguration;

	LinuxSetConfigurationRequest = (*env)->GetObjectClass( env, linuxSetConfigurationRequest );
	getConfiguration = (*env)->GetMethodID( env, LinuxSetConfigurationRequest, "getConfiguration", "()I" );
	(*env)->DeleteLocalRef( env, LinuxSetConfigurationRequest );

	if (!(configuration = malloc(sizeof(*configuration)))) {
		dbg( MSG_CRITICAL, "set_configuration : Out of memory!\n" );
		return -ENOMEM;
	}

	*configuration = (unsigned int)(*env)->CallIntMethod( env, linuxSetConfigurationRequest, getConfiguration );

	dbg( MSG_DEBUG2, "set_configuration : Setting configuration to %d\n", *configuration );

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
 * @param linuxSetInterfaceRequest The LinuxSetInterfaceRequest.
 * @return The error, or 0.
 */
int set_interface( JNIEnv *env, int fd, jobject linuxSetInterfaceRequest )
{
	struct usbdevfs_setinterface *interface = NULL;
	int ret = 0;

	jclass LinuxSetInterfaceRequest;
	jmethodID getInterface, getSetting;

	LinuxSetInterfaceRequest = (*env)->GetObjectClass( env, linuxSetInterfaceRequest );
	getInterface = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "getInterface", "()I" );
	getSetting = (*env)->GetMethodID( env, LinuxSetInterfaceRequest, "getSetting", "()I" );
	(*env)->DeleteLocalRef( env, LinuxSetInterfaceRequest );

	if (!(interface = malloc(sizeof(*interface)))) {
		dbg( MSG_CRITICAL, "set_interface : Out of memory!\n" );
		return -ENOMEM;
	}

	interface->interface = (unsigned int)(*env)->CallIntMethod( env, linuxSetInterfaceRequest, getInterface );
	interface->altsetting = (unsigned int)(*env)->CallIntMethod( env, linuxSetInterfaceRequest, getSetting );

	dbg( MSG_DEBUG2, "set_interface : Setting interface %d to setting %d\n", interface->interface, interface->altsetting );

	errno = 0;
	if (ioctl( fd, USBDEVFS_SETINTERFACE, interface ))
		ret = -errno;

	if (ret)
		dbg( MSG_ERROR, "set_interface : Could not set interface (errno %d)\n", ret );
	else
		dbg( MSG_DEBUG2, "set_interface : Set interface\n" );

	free(interface);

	return ret;
}
