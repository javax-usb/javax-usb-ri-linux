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
public abstract class LinuxPipeOsImp implements UsbPipeOsImp
{
	/** Constructor */
	public LinuxPipeOsImp( UsbPipeImp pipe, LinuxInterfaceOsImp iface )
	{
		setUsbPipeImp(pipe);
		setLinuxInterfaceOsImp(iface);
	}

    //*************************************************************************
    // public methods

	/** @return The UsbPipeImp for this */
	public UsbPipeImp getUsbPipeImp() { return usbPipeImp; }

	/** @param usbPipeImp The UsbPipeImp for this */
	public void setUsbPipeImp( UsbPipeImp pipe ) { usbPipeImp = pipe; }

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
	 * Close this pipe
	 */
	public void close() { }

	/** @return Not to accept byte[]s */
	public boolean passBytes() { return false; }

	/** @return Not to accept Lists */
	public boolean passLists() { return false; }

	/**
	 * Synchonously submits this byte[] array to the UsbPipe
	 * @param data the byte[] data
	 * @return The result of the submission
	 * @exception javax.usb.UsbException if error occurs
	 */
	public int syncSubmit( byte[] data ) throws UsbException
	{
//FIXME - do not implement
throw new UsbException("Not implemented");
	}

	/**
	 * Synchronous submission using a UsbIrpImp.
	 * @param irp the UsbIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void syncSubmit( UsbIrpImp irp ) throws UsbException
	{
//FIXME - implement in UsbPipeImp
throw new UsbException("Not implemented");
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
	 * Synchronous submission using a UsbCompositeIrpImp.
	 * @param irp the UsbCompositeIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void syncSubmit( List list ) throws UsbException
	{
		for (int i=0; i<list.size(); i++)
			syncSubmit((UsbIrpImp)list.get(i));
	}

	/**
	 * Asynchronous submission using a UsbCompositeIrpImp.
	 * @param irp the UsbCompositeIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void asyncSubmit( List list ) throws UsbException
	{
//FIXME - this needs work...may die in middle?
		for (int i=0; i<list.size(); i++)
			asyncSubmit((UsbIrpImp)list.get(i));
	}

	/**
	 * Stop all submissions in progress
	 */
	public void abortAllSubmissions()
	{
		LinuxPipeRequest[] requests = null;

		synchronized(inProgressList) {
			requests = (LinuxPipeRequest[])inProgressList.toArray();
			inProgressList.clear();
		}

		for (int i=0; i<requests.length; i++)
			getLinuxInterfaceOsImp().cancel(requests[i]);

		for (int i=0; i<requests.length; i++)
			requests[i].getUsbIrpImp().waitUntilCompleted();
	}

    //*************************************************************************
    // Protected methods

	/**
	 * Create and submit a LinuxRequest.
	 * @param irp the UsbIrpImp to submit.
	 */
	protected void internalAsyncSubmit( UsbIrpImp irp ) throws UsbException
	{
		getLinuxInterfaceOsImp().submit( usbIrpImpToLinuxPipeRequest(irp) );
	}

	/**
	 * Create a LinuxPipeRequest to wrap a UsbIrpImp.
	 * @param usbIrpImp The UsbIrpImp.
	 * @return A LinuxPipeRequest for a UsbIrpImp.
	 */
	protected LinuxPipeRequest usbIrpImpToLinuxPipeRequest(UsbIrpImp usbIrpImp)
	{
		LinuxPipeRequest request = new LinuxPipeRequest();
		request.setUsbIrpImp(usbIrpImp);
		return request;
	}

    //*************************************************************************
    // Instance variables

	private UsbPipeImp usbPipeImp = null;
	private LinuxInterfaceOsImp linuxInterfaceOsImp = null;

	private List inProgressList = new LinkedList();
}
