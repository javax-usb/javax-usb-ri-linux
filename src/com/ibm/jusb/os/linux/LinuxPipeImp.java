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
import com.ibm.jusb.util.*;

/**
 * Linux implementation of UsbPipeImp
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
abstract class LinuxPipeImp implements UsbPipeImp
{
	/** Constructor */
	public LinuxPipeImp( UsbPipeAbstraction abstraction )
	{
		this.abstraction = abstraction;

		preTaskList.add( initializeTask );
		preTaskList.add( startTask );

		syncPostTaskList.add( fireEventTask );
		syncPostTaskList.add( syncResubmitTask );
		syncResubmitTask.add( stopTask );
		syncResubmitTask.add( notifyThreadsTask );

		asyncPostTaskList.add( fireEventTask );
		asyncPostTaskList.add( asyncResubmitTask );
		asyncResubmitTask.add( stopTask );
		asyncResubmitTask.add( notifyUsbPipeTask );
		asyncResubmitTask.add( notifyThreadsTask );

		inCompositePostTaskList.add( fireEventTask );
		inCompositePostTaskList.add( stopTask );
		inCompositePostTaskList.add( notifyThreadsTask );

		
	}

    //*************************************************************************
    // public methods

	/** @return the UsbPipeAbstraction object for this implementation */
	public UsbPipeAbstraction getUsbPipeAbstraction() { return abstraction; }

	/**
	 * Get the associated UsbInterfaceImp
	 * @return the associated UsbInterfaceImp
	 */
	public UsbInterfaceImp getUsbInterfaceImp() { return usbInterfaceImp; }

	/**
	 * Set the associated UsbInterfaceImp
	 * @param usbInterfaceImp the associated UsbIntefaceImp
	 */
	public void setUsbInterfaceImp( UsbInterfaceImp usbInterfaceImp ) { this.usbInterfaceImp = usbInterfaceImp; }

	/**
	 * Return the current sequence number.
	 * @return the current sequence number.
	 */
	public long getSequenceNumber() { return sequenceNumber; }

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
		UsbIrpImp irp = (UsbIrpImp)getUsbIrpImpFactory().take();
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
		irp.setPreSubmissionTask( preTaskList );
		irp.setPostSubmissionTask( syncPostTaskList );

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
		irp.setPreSubmissionTask( preTaskList );
		irp.setPostSubmissionTask( asyncPostTaskList );

		internalAsyncSubmit( irp );
	}

	/**
	 * Synchronous submission using a UsbCompositeIrpImp.
	 * @param irp the UsbCompositeIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
    public void syncSubmit( UsbCompositeIrpImp irp ) throws UsbException
	{
		irp.setPreSubmissionTask( preTaskList );
		irp.setPostSubmissionTask( syncPostTaskList );

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
    public void asyncSubmit( UsbCompositeIrpImp composite ) throws UsbException
	{
		/* Should this be done higher up? */
		if (0 == composite.getUsbIrps().size())
			throw new UsbException( "Cannot handle empty UsbCompositeIrp" );

		composite.setPreSubmissionTask( preTaskList );
		composite.setPostSubmissionTask( asyncPostTaskList );

		setupInCompositeTasks( composite );

		internalAsyncSubmit( composite );
	}

	/**
	 * Stop a submission in progress
	 * @param the UsbIrpImp to stop
	 */
	public void abortSubmission( UsbIrpImp irp )
	{
		if (!irpRequests.containsKey( irp ))
			return;

		getLinuxDeviceProxy().cancelRequest( (LinuxRequest)irpRequests.get( irp ) );
	}

	/**
	 * Stop a composite submission in progress
	 * @param the UsbCompositeIrpImp to stop
	 */
	public void abortSubmission( UsbCompositeIrpImp irp )
	{
		UsbIrpIterator iterator = irp.getUsbIrps().usbIrpIterator();

		while (iterator.hasNext())
			abortSubmission( (UsbIrpImp)iterator.nextUsbIrp() );
	}

	/**
	 * Stop all submissions in progress
	 */
	public void abortAllSubmissions()
	{
		Enumeration e = getLinuxRequestTable().elements();

		while (e.hasMoreElements()) {
			LinuxPipeRequest request = (LinuxPipeRequest)e.nextElement();
			UsbIrpImp irp = request.getUsbIrpImp();

			getLinuxDeviceProxy().cancelRequest( request );

			if (null != irp)
				irp.waitUntilCompleted();
		}
	}

	/**
	 * Remove the pair with the specified UsbIrpImp key from the LinuxRequest table.
	 * @param irp the UsbIrpImp key to remove
	 */
	public void removeUsbIrpImpKey( UsbIrpImp irp )
	{
		getLinuxRequestTable().remove( irp );
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
    // Package methods

	/** @return the associated LinuxInterfaceImp */
	LinuxInterfaceImp getLinuxInterfaceImp() { return (LinuxInterfaceImp)usbInterfaceImp; }

	/** @return the LinuxDeviceProxy in use */
	LinuxDeviceProxy getLinuxDeviceProxy() { return getLinuxInterfaceImp().getLinuxDeviceProxy(); }

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
	 * Async-submit the specified UsbCompositeIrpImp; the pre and post Tasks should already
	 * be set up.
	 * @param composite the UsbCompositeIrpImp to submit.
	 */
	protected void internalAsyncSubmit( UsbCompositeIrpImp composite ) throws UsbException
	{
		UsbIrpListIterator iterator = composite.getUsbIrps().usbIrpListIterator();

		while (iterator.hasNext()) {
			UsbIrpImp irp = (UsbIrpImp)iterator.nextUsbIrp();

			try {
				internalAsyncSubmit( irp );
			} catch ( UsbException uE ) {
				irp.setUsbException( uE );

				if (composite.getCompositeErrorCommand().continueSubmissions( composite, irp )) {
					abortSubmission( composite );
/* execute composite post-task? */

					throw uE;
				}
					
			}
		}
	}

	/**
	 * Setup the Tasks for the individual UsbIrps in the specified composite.
	 * @param composite the composite to set up.
	 */
	protected void setupInCompositeTasks( UsbCompositeIrpImp composite )
	{
		UsbIrpImp.TaskList firstUsbIrpPreTaskList = new LinuxPipeImp.FirstUsbIrpPreTaskList( composite );
		firstUsbIrpPreTaskList.add( preTaskList );

		UsbIrpImp.TaskList lastUsbIrpPostTaskList = new LinuxPipeImp.LastUsbIrpPostTaskList( composite );
		lastUsbIrpPostTaskList.add( inCompositePostTaskList );

		UsbIrpListIterator iterator = composite.getUsbIrps().usbIrpListIterator();

		while (iterator.hasNext()) {
			boolean first = !iterator.hasPrevious();
			UsbIrpImp irp = (UsbIrpImp)iterator.nextUsbIrp();
			boolean last = !iterator.hasNext();

			if (first)
				irp.setPreSubmissionTask( firstUsbIrpPreTaskList );
			else
				irp.setPreSubmissionTask( preTaskList );

			if (last)
				irp.setPostSubmissionTask( lastUsbIrpPostTaskList );
			else
				irp.setPostSubmissionTask( inCompositePostTaskList );
		}
	}

	/** @return the UsbIrpImp / LinuxRequest hashtable */
	protected Hashtable getLinuxRequestTable() { return irpRequests; }

	/** @return a LinuxIrpImpFactory object */
	protected RecycleFactory getUsbIrpImpFactory()
	{ return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getUsbIrpImpFactory(); }

	/** @return a LinuxRequestFactory object */
	protected LinuxRequestFactory getLinuxRequestFactory()
	{ return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getLinuxPipeRequestFactory(); }

	/**
	 * Assign a sequence number, and allocate the current sequence number.
	 * @return a sequence number.
	 */
	protected long assignSequenceNumber() { return sequenceNumber++; }

	/**
	 * Create a LinuxPipeRequest for the specified UsbIrpImp.
	 * @param irp the UsbIrpImp to create a LinuxPipeRequest for.
	 * @return a LinuxPipeRequest for the specified UsbIrpImp.
	 */
	protected LinuxPipeRequest createLinuxPipeRequest( UsbIrpImp irp )
	{
		LinuxPipeRequest request = (LinuxPipeRequest)getLinuxRequestFactory().take();

		request.setLinuxPipeImp( this );
		request.setUsbIrpImp( irp );
		request.setData( irp.getData() );

		return request;
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
			getLinuxRequestTable().put( request.getUsbIrpImp(), request );
		}
	}

    //*************************************************************************
    // Instance variables

	private long sequenceNumber = 1;

	private Hashtable irpRequests = new Hashtable();

	private UsbPipeAbstraction abstraction = null;

	private UsbInterfaceImp usbInterfaceImp = null;

	protected UsbIrpImp.Task initializeTask = new LinuxPipeImp.InitializeTask();
	protected UsbIrpImp.Task startTask = new LinuxPipeImp.StartTask();
	protected UsbIrpImp.Task stopTask = new LinuxPipeImp.StopTask();
	protected UsbIrpImp.Task fireEventTask = new LinuxPipeImp.FireEventTask();
	protected UsbIrpImp.Task notifyUsbPipeTask = new LinuxPipeImp.NotifyUsbPipeTask();
	protected UsbIrpImp.Task notifyThreadsTask = new LinuxPipeImp.NotifyThreadsTask();

	protected UsbIrpImp.TaskList preTaskList = new UsbIrpImp.TaskList();

	protected UsbIrpImp.TaskList asyncResubmitTask = new LinuxPipeImp.ResubmitTask();
	protected UsbIrpImp.TaskList syncResubmitTask = new LinuxPipeImp.ResubmitTask();

	protected UsbIrpImp.TaskList asyncPostTaskList = new UsbIrpImp.TaskList();
	protected UsbIrpImp.TaskList syncPostTaskList = new UsbIrpImp.TaskList();

	protected UsbIrpImp.TaskList inCompositePostTaskList = new UsbIrpImp.TaskList();

	//*************************************************************************
	// Inner classes

	protected class InitializeTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			usbIrpImp.setUsbException( null );
			usbIrpImp.setUsbPipe( LinuxPipeImp.this.getUsbPipeAbstraction() );
			usbIrpImp.setSequenceNumber( LinuxPipeImp.this.assignSequenceNumber() );

			/* Set the 'start' task as the only task */
			usbIrpImp.setPreSubmissionTask( LinuxPipeImp.this.startTask );
		}
	}

	protected class StartTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			usbIrpImp.setActive( true );
			usbIrpImp.setCompleted( false );
		}
	}

	protected class StopTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			usbIrpImp.setActive( false );
			usbIrpImp.setCompleted( true );
		}
	}

	protected class NotifyUsbPipeTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			LinuxPipeImp.this.getUsbPipeAbstraction().UsbIrpImpCompleted( usbIrpImp );
		}
	}

	protected class NotifyThreadsTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			usbIrpImp.notifyCompleted();
		}
	}

	protected class ResubmitTask extends UsbIrpImp.TaskList implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp usbIrpImp )
		{
			if (!usbIrpImp.getResubmit()) {
				super.execute( usbIrpImp );
				return;
			}

			if (usbIrpImp.isInUsbException()) {
				try {
					if (usbIrpImp.getResubmitErrorCommand().continueResubmission( usbIrpImp )) {
						usbIrpImp.setUsbException( null );
					} else {
						usbIrpImp.setResubmit( false );
						super.execute( usbIrpImp );
						return;
					}
				} catch ( Exception e ) {
/* Handle exception in ResubmitErrorCommand! */
					usbIrpImp.setResubmit( false );
					super.execute( usbIrpImp );
					return;
				}
			} else {
				try {
					usbIrpImp.setData( usbIrpImp.getResubmitDataCommand().getResubmitData( usbIrpImp ) );
				} catch ( Exception e ) {
/* Handle exception in ResubmitDataCommand! */
					usbIrpImp.setResubmit( false );
					super.execute( usbIrpImp );
					return;
				}
			}

			try {
				/* a Visitor would be better but is not reentrant */
				if ( usbIrpImp instanceof UsbCompositeIrpImp )
					LinuxPipeImp.this.internalAsyncSubmit( (UsbCompositeIrpImp)usbIrpImp );
				else
					LinuxPipeImp.this.internalAsyncSubmit( usbIrpImp );
			} catch ( UsbException uE ) {
/* Handle exception in resubmission! */
				usbIrpImp.setResubmit( false );
				super.execute( usbIrpImp );
				return;
			}
		}
	}

	protected class FireEventTask implements UsbIrpImp.Task
	{
		public void execute( UsbIrpImp irp )
		{
			if (irp.getEventCommand().shouldFireEvent( irp )) {
				UsbPipeAbstraction pipe = LinuxPipeImp.this.getUsbPipeAbstraction();
				UsbException uE = irp.getUsbException();

				if (irp.isInUsbException()) {
					UsbPipeErrorEvent event = new UsbPipeErrorEvent( pipe, irp.getSequenceNumber(), uE.getErrorCode(), uE );
					pipe.getUsbPipeEventHelper().fireUsbPipeErrorEvent( event );
				} else {
					UsbPipeDataEvent event = new UsbPipeDataEvent( pipe, irp.getSequenceNumber(), irp.getData(), irp.getDataLength() );
					pipe.getUsbPipeEventHelper().fireUsbPipeDataEvent( event );
				}
			}
		}
	}

	protected class FirstUsbIrpPreTaskList extends UsbIrpImp.TaskList implements UsbIrpImp.Task
	{
		public FirstUsbIrpPreTaskList( UsbCompositeIrpImp composite ) { usbCompositeIrpImp = composite; }
		private UsbCompositeIrpImp usbCompositeIrpImp = null;
		public void execute( UsbIrpImp usbIrpImp )
		{
			usbCompositeIrpImp.getPreSubmissionTask().execute( usbCompositeIrpImp );
			super.execute( usbIrpImp );
		}
	}

	protected class LastUsbIrpPostTaskList extends UsbIrpImp.TaskList implements UsbIrpImp.Task
	{
		public LastUsbIrpPostTaskList( UsbCompositeIrpImp composite ) { usbCompositeIrpImp = composite; }
		private UsbCompositeIrpImp usbCompositeIrpImp = null;
		public void execute( UsbIrpImp usbIrpImp )
		{
			super.execute( usbIrpImp );
			usbCompositeIrpImp.getPostSubmissionTask().execute( usbCompositeIrpImp );
		}
	}

}
