
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

	LinuxRequest = CheckedGetObjectClass( env, linuxRequest );
	getInterfaceNumber = CheckedGetMethodID( env, LinuxRequest, "getInterfaceNumber", "()I" );
	CheckedDeleteLocalRef( env, LinuxRequest );

	if (!(interface = malloc(sizeof(*interface)))) {
		log( LOG_CRITICAL, "Out of memory!" );
		return -ENOMEM;
	}

	*interface = CheckedCallIntMethod( env, linuxRequest, getInterfaceNumber );

	log( LOG_FUNC, "%s interface %d", claim ? "Claiming" : "Releasing", *interface );

	errno = 0;
	if (ioctl( fd, claim ? USBDEVFS_CLAIMINTERFACE : USBDEVFS_RELEASEINTERFACE, interface ))
		ret = -errno;

	if (ret)
		log( LOG_ERROR, "Could not %s interface %d : errno %d", claim ? "claim" : "release", *interface, ret );
	else
		log( LOG_FUNC, "%s interface %d", claim ? "Claimed" : "Released", *interface );

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

	LinuxRequest = CheckedGetObjectClass( env, linuxRequest );
	getInterfaceNumber = CheckedGetMethodID( env, LinuxRequest, "getInterfaceNumber", "()I" );
	setClaimed = CheckedGetMethodID( env, LinuxRequest, "setClaimed", "(Z)V" );
	CheckedDeleteLocalRef( env, LinuxRequest );

	if (!(gd = malloc(sizeof(*gd)))) {
		log( LOG_CRITICAL, "Out of memory!");
		return -ENOMEM;
	}

	memset(gd, 0, sizeof(*gd));

	gd->interface = CheckedCallIntMethod( env, linuxRequest, getInterfaceNumber );

	errno = 0;
	if (ioctl( fd, USBDEVFS_GETDRIVER, gd )) {
		ret = -errno;

		if (-ENODATA == ret)
			log( LOG_INFO, "Interface %d is not claimed.", gd->interface );
		else
			log( LOG_ERROR, "Could not determine if interface %d is claimed.", gd->interface );
	} else {
		log( LOG_INFO, "Interface %d is claimed by driver %s.", gd->interface, gd->driver );
	}

	CheckedCallVoidMethod( env, linuxRequest, setClaimed, (ret ? JNI_FALSE : JNI_TRUE) );

	free(gd);

	return (-ENODATA == ret ? 0 : ret);
}

