package com.ibm.jusb.os.linux;

/*
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.*;

import com.ibm.jusb.*;

/**
 * Linux implementation of the UsbDevice interface
 * @author Dan Streetman
 * @version 1.0.0
 */
class LinuxDeviceImp implements UsbDeviceImp
{
	//-------------------------------------------------------------------------
	// Ctor
	//

	/**
	 * Constructor
	 * @param abstraction the UsbDeviceAbstraction to use.
	 */
	public LinuxDeviceImp( UsbDeviceAbstraction abstraction ) { usbDeviceAbstraction = abstraction; }

	//-------------------------------------------------------------------------
	// UsbDeviceImp methods
	//

	/**
	 * Get the UsbDeviceAbstraction associated with this implementation.
	 * @return the associated UsbDeviceAbstraction.
	 */
	public UsbDeviceAbstraction getUsbDeviceAbstraction() { return usbDeviceAbstraction; }

	//-------------------------------------------------------------------------
	// Package methods
	//

	/** @return the LinuxDeviceProxy for this UsbDeviceImp */
	LinuxDeviceProxy getLinuxDeviceProxy()
	{
		if ( null == linuxDeviceProxy )
			linuxDeviceProxy = new LinuxDeviceProxy( getUsbDeviceAbstraction() );

		return linuxDeviceProxy;
	}

	//-------------------------------------------------------------------------
	// Instance variables
	//

	private UsbDeviceAbstraction usbDeviceAbstraction = null;

	private LinuxDeviceProxy linuxDeviceProxy = null;
}
