package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.UsbException;
import javax.usb.util.*;

import com.ibm.jusb.*;

/**
 * Isochronous parameters to pass to native code
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxIsochronousPipeImp extends LinuxPipeImp
{

	/** Constructor */
    public LinuxIsochronousPipeImp( UsbPipeAbstraction abstraction ) { super( abstraction ); }

	//*************************************************************************
	// Public methods

	/** Submit a request natively */
	public void submitNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeSubmitIsochronousRequest( (LinuxIsochronousRequest)request, getUsbPipeAbstraction().getEndpointAddress() );
	}

	/** Complete a request natively */
	public void completeNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeCompleteIsochronousRequest( (LinuxIsochronousRequest)request );
	}

	/**
	 * Synchronous submission using a UsbCompositeIrpImp.
	 * @param irp the UsbCompositeIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void syncSubmit( UsbCompositeIrpImp irp ) throws UsbException
	{
		irp.setPreSubmissionTask( compositePreTaskList );
		irp.setPostSubmissionTask( compositeSyncPostTaskList );

		setupInCompositeTasks( irp );

		internalAsyncSubmit( irp );

		irp.waitUntilCompleted();

		if (irp.isInUsbException())
			throw irp.getUsbException();
	}

	/**
	 * Asynchronous submission using a UsbCompositeIrpImp.
	 * @param irp the UsbCompositeIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void asyncSubmit( UsbCompositeIrpImp irp ) throws UsbException
	{
		/* Should this be done higher up? */
		if (0 == irp.getUsbIrps().size())
			throw new UsbException( "Cannot handle empty UsbCompositeIrp" );

		irp.setPreSubmissionTask( compositePreTaskList );
		irp.setPostSubmissionTask( compositeAsyncPostTaskList );

		setupInCompositeTasks( irp );

		internalAsyncSubmit( irp );
	}

	/**
	 * Stop a composite submission in progress
	 * @param the UsbCompositeIrpImp to stop
	 */
	public void abortSubmission( UsbCompositeIrpImp irp )
	{
		abortSubmission( (UsbIrpImp)irp );
	}

	//*************************************************************************
	// Protected methods

	/**
	 * Async-submit the specified UsbIrpImp; the pre and post Tasks should already
	 * be set up.
	 * @param irp the UsbIrpImp to submit.
	 */
	protected void internalAsyncSubmit( UsbCompositeIrpImp irp ) throws UsbException
	{
		LinuxRequestFactory compositeFactory = LinuxUsbServices.getLinuxInstance().getLinuxHelper().getLinuxIsochronousCompositeRequestFactory();
		LinuxPipeRequest request = (LinuxPipeRequest)compositeFactory.takeLinuxRequest();

		request.setLinuxPipeImp( this );
		request.setUsbIrpImp( irp );

		try {
			enqueueLinuxRequest( request );
		} catch ( UsbException uE ) {
			request.recycle();

			throw uE;
		}
	}

	/**
	 * Setup the Tasks for the individual UsbIrps in the specified composite.
	 * @param composite the composite to set up.
	 */
	protected void setupInCompositeTasks( UsbCompositeIrpImp composite )
	{
		UsbIrpListIterator iterator = composite.getUsbIrps().usbIrpListIterator();

		while (iterator.hasNext()) {
			UsbIrpImp irp = (UsbIrpImp)iterator.nextUsbIrp();

			irp.setPreSubmissionTask( preTaskList );
			irp.setPostSubmissionTask( inCompositePostTaskList );
		}
	}

	/** @return a factory for LinuxIsochronousRequests */
	protected LinuxRequestFactory getLinuxRequestFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getLinuxIsochronousRequestFactory();
	}

	//*************************************************************************
	// Instance variables

	private UsbIrpImp.TaskList compositePreTaskList = new UsbIrpImp.TaskList();
	private UsbIrpImp.TaskList compositeSyncPostTaskList = new UsbIrpImp.TaskList();
	private UsbIrpImp.TaskList compositeAsyncPostTaskList = new UsbIrpImp.TaskList();

	//*************************************************************************
	// Inner classes

	public class ExecutePreTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			UsbIrpIterator iterator = ((UsbCompositeIrpImp)usbIrpImp).getUsbIrps().usbIrpIterator();

			while (iterator.hasNext()) {
				UsbIrpImp irp = (UsbIrpImp)iterator.nextUsbIrp();

				irp.getPreSubmissionTask().execute( irp );
			}
		}
	}

	public class ExecutePostTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			UsbCompositeIrpImp composite = (UsbCompositeIrpImp)usbIrpImp;
			UsbIrpIterator iterator = composite.getUsbIrps().usbIrpIterator();

			while ( iterator.hasNext() ) {
				UsbIrpImp irp = (UsbIrpImp)iterator.nextUsbIrp();

				irp.getPostSubmissionTask().execute( irp );

				/* This retains ONLY the last UsbException - the JavaDoc may need clarification! */
				if (irp.isInUsbException()) {
					if (composite.getCompositeErrorCommand().continueSubmissions( composite, irp ))
						composite.setUsbException( irp.getUsbException() );
					else
						composite.setUsbException( null );
				}
			}
		}
	}

}

