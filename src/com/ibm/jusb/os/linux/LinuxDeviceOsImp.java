package com.ibm.jusb.os.linux;

/*
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import java.util.*;

import javax.usb.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;

/**
 * UsbDeviceOsImp implemenation for Linux platform.
 * <p>
 * This must be set up before use.
 * <ul>
 * <li>The {@link #getUsbDeviceImp() UsbDeviceImp} must be set
 *     either in the constructor or by its {@link #setUsbDeviceImp(UsbDeviceImp) setter}.</li>
 * <li>The {@link #getLinuxDeviceProxy() LinuxDeviceProxy} must be set
 *     either in the constructor or by its {@link #setLinuxDeviceProxy(LinuxDeviceProxy) setter}.</li>
 * </ul>
 * @author Dan Streetman
 */
class LinuxDeviceOsImp implements UsbDeviceOsImp
{
	/** Constructor */
	public LinuxDeviceOsImp( UsbDeviceImp device, LinuxDeviceProxy proxy )
	{
		setUsbDeviceImp(device);
		setLinuxDeviceProxy(proxy);
	}

	/** @return The UsbDeviceImp for this */
	public UsbDeviceImp getUsbDeviceImp() { return usbDeviceImp; }

	/** @param device The UsbDeviceImp for this */
	public void setUsbDeviceImp( UsbDeviceImp device ) { usbDeviceImp = device; }

	/** @return The LinuxDeviceProxy for this */
	public LinuxDeviceProxy getLinuxDeviceProxy() { return linuxDeviceProxy; }

	/** @param proxy The LinuxDeviceProxy */
	public void setLinuxDeviceProxy(LinuxDeviceProxy proxy) { linuxDeviceProxy = proxy; }

	/** SyncSubmit a RequestImp */
	public void syncSubmit( RequestImp request ) throws UsbException
	{
throw new UsbException("STUB");
	}

	/** SyncSubmit a List */
	public void syncSubmit( List list ) throws UsbException
	{
throw new UsbException("STUB");
	}

	/** AsyncSubmit a RquestImp */
	public void asyncSubmit( RequestImp request ) throws UsbException
	{
throw new UsbException("STUB");
	}

	//**************************************************************************
	// Instance variables

	private UsbDeviceImp usbDeviceImp = null;

	private LinuxDeviceProxy linuxDeviceProxy = null;
}
