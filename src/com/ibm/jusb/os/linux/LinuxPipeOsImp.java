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
 * <li>The {@link #getLinuxDeviceProxy() LinuxDeviceProxy} must be set
 *     either in the constructor or by its {@link #setLinuxDeviceProxy(LinuxDeviceProxy) setter}.</li>
 * </ul>
 * @author Dan Streetman
 */
public abstract class LinuxPipeOsImp implements UsbPipeOsImp
{
	/** Constructor */
	public LinuxPipeOsImp( UsbPipeImp pipe, LinuxDeviceProxy proxy )
	{
		setUsbPipeImp(pipe);
		setLinuxDeviceProxy(proxy);
	}

    //*************************************************************************
    // public methods

	/** @return The UsbPipeImp for this */
	public UsbPipeImp getUsbPipeImp() { return usbPipeImp; }

	/** @param usbPipeImp The UsbPipeImp for this */
	public void setUsbPipeImp( UsbPipeImp pipe ) { usbPipeImp = pipe; }

	/** @return The LinuxDeviceProxy in use */
	public LinuxDeviceProxy getLinuxDeviceProxy() { return linuxDeviceProxy; }

	/** @param proxy The LinuxDeviceProxy to use */
	public void setLinuxDeviceProxy( LinuxDeviceProxy proxy ) { linuxDeviceProxy = proxy; }

	/**
	 * Open this pipe
	 * @exception javax.usb.UsbException if the pipe could not be opened
	 */
	public void open() throws UsbException
	{
/* Check for pipe in error state */
	}

	/**
	 * Close this pipe
	 */
	public void close() { }

	/**
	 * Synchonously submits this byte[] array to the UsbPipe
	 * @param data the byte[] data
	 * @return The result of the submission
	 * @exception javax.usb.UsbException if error occurs
	 */
	public int syncSubmit( byte[] data ) throws UsbException
	{
		UsbIrpImp irp = usbIrpImpFactory.createUsbIrpImp();
		irp.setData( data );

		syncSubmit( irp );

		int result = irp.getDataLength();

		irp.recycle();

		return result;
	}

	/**
	 * Synchronous submission using a UsbIrpImp.
	 * @param irp the UsbIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void syncSubmit( UsbIrpImp irp ) throws UsbException
	{
		internalAsyncSubmit( irp );

		irp.waitUntilCompleted();

		if (irp.isInUsbException())
			throw irp.getUsbException();
	}

	/**
	 * Asynchronous submission using a UsbIrpImp.
	 * @param irp the UsbIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
	public void asyncSubmit( UsbIrpImp irp ) throws UsbException
	{
		internalAsyncSubmit( irp );
	}

	/**
	 * Synchronous submission using a UsbCompositeIrpImp.
	 * @param irp the UsbCompositeIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void syncSubmit( List list ) throws UsbException
	{
throw new UsbException("STUB");
	}

	/**
	 * Asynchronous submission using a UsbCompositeIrpImp.
	 * @param irp the UsbCompositeIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void asyncSubmit( List list ) throws UsbException
	{
throw new UsbException("STUB");
	}

	/**
	 * Stop a submission in progress
	 * @param the UsbIrpImp to stop
	 */
	public void abortSubmission( UsbIrpImp irp )
	{
//FIXME - implement
	}

	/**
	 * Stop all submissions in progress
	 */
	public void abortAllSubmissions()
	{
//FIXME - implement
	}

	/** Submit a request natively */
	public abstract void submitNative( LinuxPipeRequest request );

	/** Abort a request in progress */
	public void abortNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeAbortPipeRequest( request );
	}

	/** Complete a request natively */
	public abstract void completeNative( LinuxPipeRequest request );

    //*************************************************************************
    // Protected methods

	/**
	 * Async-submit the specified UsbIrpImp; the pre and post Tasks should already
	 * be set up.
	 * @param irp the UsbIrpImp to submit.
	 */
	protected void internalAsyncSubmit( UsbIrpImp irp ) throws UsbException
	{
		LinuxPipeRequest request = createLinuxPipeRequest( irp );

		try {
			enqueueLinuxRequest( request );
		} catch ( UsbException uE ) {
			request.recycle();

			throw uE;
		}
	}

	/**
	 * Create a LinuxPipeRequest for the specified UsbIrpImp.
	 * @param irp the UsbIrpImp to create a LinuxPipeRequest for.
	 * @return a LinuxPipeRequest for the specified UsbIrpImp.
	 */
	protected LinuxPipeRequest createLinuxPipeRequest( UsbIrpImp irp )
	{
//FIXME - implament

		//request.setLinuxPipeOsImp( this );
		//request.setUsbIrpImp( irp );
		//request.setData( irp.getData() );

return null;
	}

	/**
	 * Enqueue the specified LinuxRequest to be submitted.
	 * @param request the LinuxRequest to enqueue.
	 * @throws javax.usb.UsbException if the request could not be enqueued or submitted.
	 */
	protected void enqueueLinuxRequest( LinuxPipeRequest request ) throws UsbException
	{
		getLinuxDeviceProxy().submitRequest( request );
		if ( request.isActive() ) {
//getLinuxRequestTable().put( request.getUsbIrpImp(), request );
		}
	}

    //*************************************************************************
    // Instance variables

	private UsbPipeImp usbPipeImp = null;
	private LinuxDeviceProxy linuxDeviceProxy = null;
	private UsbIrpImpFactory usbIrpImpFactory = new UsbIrpImpFactory();
}
