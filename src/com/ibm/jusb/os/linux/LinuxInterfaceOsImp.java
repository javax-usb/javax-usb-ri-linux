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

	/** Claim this interface. */
	public void claim() throws UsbException
	{
		LinuxInterfaceRequest request = new LinuxInterfaceRequest.LinuxClaimInterfaceRequest();
		request.setInterfaceNumber(getInterfaceNumber());
		submit(request);

		request.waitUntilCompleted();

		if (0 != request.getError())
			throw new UsbException("Could not claim interface : " + JavaxUsb.nativeGetErrorMessage(request.getError()));
	}

	/** Release this interface. */
	public void release()
	{
		LinuxInterfaceRequest request = new LinuxInterfaceRequest.LinuxReleaseInterfaceRequest();
		request.setInterfaceNumber(getInterfaceNumber());

		try {
			submit(request);
		} catch ( UsbException uE ) {
//FIXME - log this
			return;
		}

		request.waitUntilCompleted();
	}

	/** @return if this interface is claimed. */
	public boolean isClaimed()
	{
		LinuxInterfaceRequest request = new LinuxInterfaceRequest.LinuxIsClaimedInterfaceRequest();
		request.setInterfaceNumber(getInterfaceNumber());

		try {
			submit(request);
		} catch ( UsbException uE ) {
//FIXME - log this
			return false;
		}

		request.waitUntilCompleted();

		if (0 != request.getError()) {
//FIXME - log
				return false;
		}

		return request.isClaimed();
	}

	public byte getInterfaceNumber() { return getUsbInterfaceImp().getInterfaceNumber(); }

	//**************************************************************************
	// Package methods

	/**
	 * Submit a Request.
	 * @param request The LinuxRequest.
	 */
	void submit(LinuxRequest request) throws UsbException { getLinuxDeviceOsImp().submit(request); }

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
