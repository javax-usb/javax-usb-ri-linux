package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import com.ibm.jusb.*;

/**
 * Linux implementation of UsbPipeImpFactory
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxInfoImpFactory implements UsbInfoImpFactory
{
	/**
	 * Create a UsbDeviceImp object.
	 * @param abstraction the UsbDeviceAbstraction to associate the implementation with.
	 */
	public UsbDeviceImp createUsbDeviceImp( UsbDeviceAbstraction abstraction )
	{
		return new LinuxDeviceImp( abstraction );
	}

	/**
	 * Create a UsbInterfaceImp object.
	 * @param abstraction the UsbInterfaceAbstraction to associate the implementation with.
	 * @param device the 'parent' UsbDeviceImp.
	 */
	public UsbInterfaceImp createUsbInterfaceImp( UsbInterfaceAbstraction abstraction, UsbDeviceImp device )
	{
		return new LinuxInterfaceImp( abstraction, (LinuxDeviceImp)device );
	}
}
