
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

/**
 * Claim or release a specified interface.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param claim Whether to claim or release.
 * @param linuxRequest The request.
 * @return error.
 */
int claim_interface( JNIEnv *env, int fd, int claim, jobject linuxRequest )
{
	int ret = 0, *interface = NULL;

	jclass LinuxRequest = NULL;
	jmethodID getInterfaceNumber;

	LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
	getInterfaceNumber = (*env)->GetMethodID( env, LinuxRequest, "getInterfaceNumber", "()I" );
	(*env)->DeleteLocalRef( env, LinuxRequest );

	if (!(interface = malloc(sizeof(*interface)))) {
		dbg( MSG_CRITICAL, "claim_interface : Out of memory!\n" );
		return -ENOMEM;
	}

	*interface = (*env)->CallIntMethod( env, linuxRequest, getInterfaceNumber );

	dbg( MSG_DEBUG2, "claim_interface : %s interface %d\n", claim ? "Claiming" : "Releasing", *interface );

	errno = 0;
	if (ioctl( fd, claim ? USBDEVFS_CLAIMINTERFACE : USBDEVFS_RELEASEINTERFACE, interface ))
		ret = -errno;

	if (ret)
		dbg( MSG_ERROR, "claim_interface : Could not %s interface %d : errno %d\n", claim ? "claim" : "release", *interface, ret );
	else
		dbg( MSG_DEBUG2, "claim_interface : %s interface %d\n", claim ? "Claimed" : "Released", *interface );

	free(interface);

	return ret;
}

/**
 * Check if an interface is claimed.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 */
int is_claimed( JNIEnv *env, int fd, jobject linuxRequest )
{
	struct usbdevfs_getdriver *gd;
	int ret = 0;

	jclass LinuxRequest;
	jmethodID getInterfaceNumber, setClaimed;

	LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
	getInterfaceNumber = (*env)->GetMethodID( env, LinuxRequest, "getInterfaceNumber", "()I" );
	setClaimed = (*env)->GetMethodID( env, LinuxRequest, "setClaimed", "(Z)V" );
	(*env)->DeleteLocalRef( env, LinuxRequest );

	if (!(gd = malloc(sizeof(*gd)))) {
		dbg(MSG_CRITICAL, "is_claimed : Out of memory!\n");
		return -ENOMEM;
	}

	memset(gd, 0, sizeof(*gd));

	gd->interface = (*env)->CallIntMethod( env, linuxRequest, getInterfaceNumber );

	errno = 0;
	if (ioctl( fd, USBDEVFS_GETDRIVER, gd )) {
		ret = -errno;

		if (-ENODATA == ret)
			dbg( MSG_DEBUG3, "is_claimed : Interface %d is not claimed\n", gd->interface );
		else
			dbg( MSG_ERROR, "is_claimed : Could not determine if interface %d is claimed\n", gd->interface );
	} else {
		dbg( MSG_DEBUG3, "is_claimed : Interface %d is claimed by driver %s\n", gd->interface, gd->driver );
	}

	(*env)->CallVoidMethod( env, linuxRequest, setClaimed, (ret ? JNI_FALSE : JNI_TRUE) );

	free(gd);

	return (-ENODATA == ret ? 0 : ret);
}

