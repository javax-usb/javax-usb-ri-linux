package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import java.util.*;

import javax.usb.*;
import javax.usb.event.*;
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;
import com.ibm.jusb.util.*;

/**
 * UsbPipeOsImp implementation for Linux platform.
 * <p>
 * This must be set up before use.
 * <ul>
 * <li>The {@link #getUsbPipeImp() UsbPipeImp} must be set
 *     either in the constructor or by its {@link #setUsbPipeImp(UsbPipeImp) setter}.</li>
 * <li>The {@link #getLinuxInterfaceOsImp() LinuxInterfaceOsImp} must be set
 *     either in the constructor or by its {@link #setLinuxInterfaceOsImp(LinuxInterfaceOsImp) setter}.</li>
 * </ul>
 * @author Dan Streetman
 */
public class LinuxPipeOsImp extends AbstractUsbPipeOsImp implements UsbPipeOsImp,LinuxRequest.Completion
{
	/** Constructor */
	public LinuxPipeOsImp( UsbPipeImp pipe, LinuxInterfaceOsImp iface )
	{
		setUsbPipeImp(pipe);
		setLinuxInterfaceOsImp(iface);
	}

	/** @return The UsbPipeImp for this */
	public UsbPipeImp getUsbPipeImp() { return usbPipeImp; }

	/** @param usbPipeImp The UsbPipeImp for this */
	public void setUsbPipeImp( UsbPipeImp pipe )
	{
		usbPipeImp = pipe;

		try {
			pipeType = pipe.getUsbEndpointImp().getType();
			endpointAddress = pipe.getUsbEndpointImp().getEndpointDescriptor().bEndpointAddress();
		} catch ( NullPointerException npE ) {
			/* wait 'til the UsbPipeImp is non-null */
		}
	}

	/** @return The LinuxInterfaceOsImp */
	public LinuxInterfaceOsImp getLinuxInterfaceOsImp() { return linuxInterfaceOsImp; }

	/** @param iface The LinuxInterfaceOsImp */
	public void setLinuxInterfaceOsImp(LinuxInterfaceOsImp iface) { linuxInterfaceOsImp = iface; }

	/**
	 * Open this pipe
	 * @exception javax.usb.UsbException if the pipe could not be opened
	 */
	public void open() throws UsbException
	{
		if (!getLinuxInterfaceOsImp().isClaimed())
			throw new UsbException("Interface must be claimed before opening pipe");

//FIXME - use open/closed states?
	}

	/**
	 * Asynchronous submission using a UsbIrpImp.
	 * @param irp the UsbIrpImp to use for this submission
	 * @exception javax.usb.UsbException if error occurs
	 */
	public void asyncSubmit( UsbIrpImp irp ) throws UsbException
	{
		LinuxPipeRequest request = usbIrpImpToLinuxPipeRequest(irp);

		getLinuxInterfaceOsImp().submit(request);

		synchronized(inProgressList) {
			inProgressList.add(request);
		}
	}

	/**
	 * Stop all submissions in progress
	 */
	public void abortAllSubmissions()
	{
		Object[] requests = null;

		synchronized(inProgressList) {
			requests = inProgressList.toArray();
			inProgressList.clear();
		}

		for (int i=0; i<requests.length; i++)
			getLinuxInterfaceOsImp().cancel((LinuxPipeRequest)requests[i]);

		for (int i=0; i<requests.length; i++)
				((LinuxPipeRequest)requests[i]).waitUntilCompleted();
	}

	/** @param request The LinuxRequest that completed. */
	public void linuxRequestComplete(LinuxRequest request)
	{
		synchronized (inProgressList) {
			inProgressList.remove(request);
		}
	}

	/**
	 * Create a LinuxPipeRequest to wrap a UsbIrpImp.
	 * @param usbIrpImp The UsbIrpImp.
	 * @return A LinuxPipeRequest for a UsbIrpImp.
	 */
	protected LinuxPipeRequest usbIrpImpToLinuxPipeRequest(UsbIrpImp usbIrpImp)
	{
		LinuxPipeRequest request = new LinuxPipeRequest(pipeType,endpointAddress);
		request.setUsbIrpImp(usbIrpImp);
		request.setCompletion(this);
		return request;
	}

	private UsbPipeImp usbPipeImp = null;
	private LinuxInterfaceOsImp linuxInterfaceOsImp = null;

	private byte pipeType = 0;
	private byte endpointAddress = 0;

	private List inProgressList = new LinkedList();
}
