package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/**
 * Proxy implementation for Linux's device-based access.
 * @author Dan Streetman
 */
class LinuxDeviceProxy extends LinuxRequestProxy
{
	//*************************************************************************
	// Public methods

	//*************************************************************************
	// Instance variables

	private Runnable proxyRunnable = new Runnable() {
		public void run()
		{ JavaxUsb.nativeDeviceProxy( LinuxDeviceProxy.this ); }
	};

}
