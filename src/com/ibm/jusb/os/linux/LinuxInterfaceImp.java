package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.*;
import javax.usb.util.*;

import com.ibm.jusb.*;

/**
 * Linux implementation of the UsbInterface interface
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxInterfaceImp implements UsbInterfaceImp
{

	/**
	 * Constructor.
	 * @param abstraction the associated UsbInterfaceAbstraction.
	 * @param device the 'parent' LinuxDeviceImp.
	 */
	public LinuxInterfaceImp( UsbInterfaceAbstraction abstraction, LinuxDeviceImp device )
	{
		usbInterfaceAbstraction = abstraction;
		linuxDeviceImp = device;
	}

	//*************************************************************************
	// Public methods

	/**
	 * Get the associatd UsbInterfaceAbstraction
	 * @return this implementation's interface abstraction
	 */
	public UsbInterfaceAbstraction getUsbInterfaceAbstraction() { return usbInterfaceAbstraction; }

	/**
	 * Get the associated LinuxDeviceImp
	 * @return the associated LinuxDeviceImp
	 */
	public LinuxDeviceImp getLinuxDeviceImp() { return linuxDeviceImp; }

	/**
	 * Claim this interface.
	 * @exception UsbException if the interface could not be claimed.
	 */
	public void claim() throws UsbException
	{
		getLinuxDeviceProxy().claimInterface( getInterfaceNumber() );
	}

	/**
	 * Release this interface.
	 * @exception UsbException if the interface could not be released.
	 */
	public void release() throws UsbException
	{
		getLinuxDeviceProxy().releaseInterface( getInterfaceNumber() );
	}

	/**
	 * @return if this interface is claimed (in Java).
	 */
	public boolean isClaimed()
	{
		return getLinuxDeviceProxy().isInterfaceClaimed( getInterfaceNumber() );
	}

	//*************************************************************************
	// Package methods

	/** @return the LinuxDeviceProxy object to use */
	LinuxDeviceProxy getLinuxDeviceProxy() { return getLinuxDeviceImp().getLinuxDeviceProxy(); }

	//*************************************************************************
	// Private methods

	private byte getInterfaceNumber() { return getUsbInterfaceAbstraction().getInterfaceNumber(); }

	//*************************************************************************
	// Instance variables

	private UsbInterfaceAbstraction usbInterfaceAbstraction = null;

	private LinuxDeviceImp linuxDeviceImp = null;

}
