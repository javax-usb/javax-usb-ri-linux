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
import com.ibm.jusb.os.*;

/**
 * UsbInterfaceOsImp implementation for Linux platform.
 * <p>
 * This must be set up before use.
 * <ul>
 * <li>The {@link #getUsbInterfaceImp() UsbInterfaceImp} must be set
 *     either in the constructor or by its {@link #setUsbInterfaceImp(UsbInterfaceImp) setter}.</li>
 * <li>The {@link #getLinuxDeviceOsImp() LinuxDeviceOsImp} must be set
 *     either in the constructor or by its {@link #setLinuxDeviceOsImp(LinuxDeviceOsImp) setter}.</li>
 * </ul>
 * @author Dan Streetman
 */
class LinuxInterfaceOsImp implements UsbInterfaceOsImp
{
	/** Constructor */
	public LinuxInterfaceOsImp( UsbInterfaceImp iface, LinuxDeviceOsImp device )
	{
		setUsbInterfaceImp(iface);
		setLinuxDeviceOsImp(device);
	}

	//*************************************************************************
	// Public methods

	/** @return The UsbInterfaceImp for this */
	public UsbInterfaceImp getUsbInterfaceImp() { return usbInterfaceImp; }

	/** @param iface The UsbInterfaceImp for this */
	public void setUsbInterfaceImp( UsbInterfaceImp iface ) { usbInterfaceImp = iface; }

	/** @return The LinuxDeviceOsImp for this */
	public LinuxDeviceOsImp getLinuxDeviceOsImp() { return linuxDeviceOsImp; }

	/** @param device The LinuxDeviceOsImp for this */
	public void setLinuxDeviceOsImp( LinuxDeviceOsImp device ) { linuxDeviceOsImp = device; }

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
	public void release()
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

	public LinuxDeviceProxy getLinuxDeviceProxy() { return getLinuxDeviceOsImp().getLinuxDeviceProxy(); }

	public byte getInterfaceNumber() { return getUsbInterfaceImp().getInterfaceNumber(); }

	//**************************************************************************
	// Package methods

	/**
	 * Submit a Request.
	 * @param request The LinuxRequest.
	 */
	void submit(LinuxRequest request) { getLinuxDeviceOsImp().submit(request); }

	/**
	 * Cancel a Request.
	 * @param request The LinuxRequest.
	 */
	void cancel(LinuxRequest request) { getLinuxDeviceOsImp().cancel(request); }

	//*************************************************************************
	// Instance variables

	public UsbInterfaceImp usbInterfaceImp = null;
	public LinuxDeviceOsImp linuxDeviceOsImp = null;
}
