
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
 * Listener for connect/disconnect events
 * @author Dan Streetman
 */
JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeTopologyListener
			( JNIEnv *env, jclass JavaxUsb, jobject linuxTopologyListener ) {
	struct pollfd devpoll;
	int poll_timeout = -1;
	int descriptor = 0;
	unsigned int pollingError = 0;

	jclass LinuxTopologyListener = (*env)->GetObjectClass( env, linuxTopologyListener );

	jmethodID setListening = (*env)->GetMethodID( env, LinuxTopologyListener, "setListening", "(Z)V" );
	jmethodID isListening = (*env)->GetMethodID( env, LinuxTopologyListener, "isListening", "()Z" );
	jmethodID topologyChange = (*env)->GetMethodID( env, LinuxTopologyListener, "topologyChange", "()V" );

	descriptor = open( USBDEVFS_DEVICES, O_RDONLY, 0 );
	if ( 0 >= descriptor ) {
		dbg( MSG_ERROR, "TopologyListener : Could not open %s\n", USBDEVFS_DEVICES );
		goto TOPOLOGY_LISTENER_CLEANUP;
	}

	devpoll.fd = descriptor;
	devpoll.events = POLLIN;

	(*env)->CallVoidMethod( env, linuxTopologyListener, setListening, JNI_TRUE );

	while ( JNI_TRUE == (*env)->CallBooleanMethod( env, linuxTopologyListener, isListening ) ) {
		poll(&devpoll, 1, poll_timeout);

		// Skip empty wake-ups
		if ( 0x0 == devpoll.revents ) continue;

		// Polling Error...strange...
		if ( devpoll.revents & POLLERR ) {
			dbg( MSG_ERROR, "TopologyListener : Topology Polling error.\n" );
			if (MAX_POLLING_ERRORS < ++pollingError) {
				dbg( MSG_CRITICAL, "TopologyListener : %d polling errors; aborting!\n", pollingError );
				break;
			} else continue;
		}

		// Connect/Disconnect event...
		if ( devpoll.revents & POLLIN ) {
			dbg( MSG_DEBUG3, "TopologyListener : Got topology change event\n" );
			(*env)->CallVoidMethod( env, linuxTopologyListener, topologyChange );
			continue;
		}

		// Freak event...
		dbg( MSG_DEBUG3, "TopologyListener : Unknown event received = 0x%x\n", devpoll.revents );
	}

	// Clean up
	(*env)->CallVoidMethod( env, linuxTopologyListener, setListening, JNI_FALSE );
	dbg( MSG_DEBUG1, "TopologyListener : Exiting.\n" );
	close( descriptor );

TOPOLOGY_LISTENER_CLEANUP:
	(*env)->DeleteLocalRef( env, LinuxTopologyListener );
}

