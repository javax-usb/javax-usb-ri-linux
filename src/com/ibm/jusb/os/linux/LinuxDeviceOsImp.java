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
class LinuxDeviceOsImp extends AbstractUsbDeviceOsImp implements UsbDeviceOsImp
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

	/**
	 * Get the LinuxDeviceProxy.
	 * <p>
	 * This will start up the LinuxDeviceProxy if not running.
	 * @return The LinuxDeviceProxy.
	 * @exception UsbException If an UsbException occurred while starting the LinuxDeviceProxy.
	 */
	public LinuxDeviceProxy getLinuxDeviceProxy() throws UsbException
	{
		if (!linuxDeviceProxy.isRunning()) {
			synchronized(linuxDeviceProxy) { linuxDeviceProxy.start(); }
		}

		return linuxDeviceProxy;
	}

	/** @param proxy The LinuxDeviceProxy */
	public void setLinuxDeviceProxy(LinuxDeviceProxy proxy) { linuxDeviceProxy = proxy; }

	/** AsyncSubmit a ControlUsbIrpImp */
	public void asyncSubmit( ControlUsbIrpImp controlUsbIrpImp ) throws UsbException
	{
		if (controlUsbIrpImp.isSetConfiguration())
			submit(controlUsbIrpImpToLinuxSetConfigurationRequest(controlUsbIrpImp));
		else if (controlUsbIrpImp.isSetInterface())
			submit(controlUsbIrpImpToLinuxSetInterfaceRequest(controlUsbIrpImp));
		else
			submit(controlUsbIrpImpToLinuxDcpRequest(controlUsbIrpImp));
	}

	//**************************************************************************
	// Package methods

	/** Submit a Request. */
	void submit(LinuxRequest request) throws UsbException { getLinuxDeviceProxy().submit(request); }

	/** Cancel a Request. */
	void cancel(LinuxRequest request)
	{
		/* Ignore proxy-starting exception, it should already be started */
		try { getLinuxDeviceProxy().cancel(request); }
		catch ( UsbException uE ) { }
	}

	//**************************************************************************
	// Protected methods

	/** Convert a ControlUsbIrpImp to a LinuxSetConfigurationRequest */
	protected LinuxSetConfigurationRequest controlUsbIrpImpToLinuxSetConfigurationRequest(ControlUsbIrpImp controlUsbIrpImp)
	{
		LinuxSetConfigurationRequest configRequest = new LinuxSetConfigurationRequest();
		configRequest.setConfiguration((byte)controlUsbIrpImp.wValue()); /* use only lower byte */
		configRequest.setControlUsbIrpImp(controlUsbIrpImp);
		return configRequest;
	}

	/** Convert a ControlUsbIrpImp to a LinuxSetInterfaceRequest */
	protected LinuxSetInterfaceRequest controlUsbIrpImpToLinuxSetInterfaceRequest(ControlUsbIrpImp controlUsbIrpImp)
	{
		LinuxSetInterfaceRequest interfaceRequest = new LinuxSetInterfaceRequest();
		interfaceRequest.setInterface(controlUsbIrpImp.wIndex());
		interfaceRequest.setSetting(controlUsbIrpImp.wValue());
		interfaceRequest.setControlUsbIrpImp(controlUsbIrpImp);
		return interfaceRequest;
	}

	/** Convert a ControlUsbIrpImp to a LinuxDcpRequest */
	protected LinuxDcpRequest controlUsbIrpImpToLinuxDcpRequest(ControlUsbIrpImp controlUsbIrpImp)
	{
		LinuxDcpRequest dcpRequest = new LinuxDcpRequest();
		dcpRequest.setData(controlUsbIrpImp.getData());
		dcpRequest.setControlUsbIrpImp(controlUsbIrpImp);
		return dcpRequest;
	}

	//**************************************************************************
	// Instance variables

	private UsbDeviceImp usbDeviceImp = null;

	private LinuxDeviceProxy linuxDeviceProxy = null;

}
