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
import java.lang.reflect.*;

import javax.usb.*;
import javax.usb.event.*;

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * Proxy for requests on a pipe
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
abstract class LinuxRequestProxy {

	//*************************************************************************
	// Public methods

	/**
	 * Submit the data on the specified pipe
	 * @param request the LinuxRequest object representing this submission
	 * @exception javax.usb.UsbException if could not submit
	 */
	public void submitRequest( LinuxRequest request ) throws UsbException
	{
		/* First user pays the price... */
		if (!isRunning())
			start();

		checkRequest( request );

		request.setLinuxRequestProxy( this );
		request.getPreTask().execute();

		requestVector.addElement( request );
		notifyProxyThread();
		request.waitUntilSubmitCompleted();

		if (request.isActive() && 0 != request.getSubmissionStatus()) {
			int error = request.getSubmissionStatus();
/* execute post-task? */

			throw new UsbException( "Error in submission : " + JavaxUsb.nativeGetErrorMessage(error), error );
		}
	}

	/**
	 * Cancel a request in progress
	 * @param request the request to cancel
	 */
	public void cancelRequest( LinuxRequest request )
	{
		if (request.isActive() && pendingVector.contains(request) && !cancelVector.contains(request)) {
			cancelVector.add( request );
			notifyProxyThread();
		}
	}

	/** Empty the queue, returning an error on all outstanding */
	public void dequeueAllRequests()
	{
		LinuxRequest request;

		/* Cancel all requests that the proxy hasn't grabbed yet */
		synchronized ( requestVector ) {
			while (!requestVector.isEmpty()) {
				request = (LinuxRequest)requestVector.remove(0);
				request.setSubmissionStatus( RESULT_CANCELED );
				request.setSubmitCompleted( true );
			}
		}

		/* cancel all pending requests */
		while (!pendingVector.isEmpty()) {
			synchronized ( pendingVector ) {
				if (pendingVector.isEmpty()) break;
				else request = (LinuxRequest)pendingVector.elementAt(0);
			}
			if (request.isActive() && !request.isRequestCompleted()) {
				cancelRequest(request);
				request.waitUntilRequestCompleted();
			}
		}

	}

	//*************************************************************************
	// Protected methods

	/**
	 * Start the proxy thread
	 * <p>
	 * This only starts the thread if it's not already alive.
	 * The caller should handle any callback/notification of
	 * sucessful startup.  This just creates and starts the
	 * Thread that runs the provided runnable.  The Thread's
	 * name is set to the provided name, and daemon status is set to true.
	 * The Runnable should monitor isRunning as a flag to exit and die.
	 * @see #getProxyRunnable()
	 * @see #getProxyName()
	 * @see #isRunning
	 */
	protected void start() throws UsbException
	{
		if (null != proxyThread && proxyThread.isAlive())
			return;

		taskManager.start();

		proxyThread = new LinuxProxyThread( getProxyRunnable() );

		proxyThread.setName( getProxyName() );
		proxyThread.setDaemon( true );
		isRunning = true;
		proxyThread.start();
	}

	/**
	 * Tell the proxy thread to stop
	 * <p>
	 * This only stops the Thread if it is alive.
	 * This sets isRunning to false and notifies the Thread.
	 * Any request dequeueing should be done first.
	 * @see #isRunning
	 * @see #dequeueAllRequests()
	 * @see #notifyProxyThread()
	 */
	protected void stop()
	{
		if (null == proxyThread || !proxyThread.isAlive()) return;

		isRunning = false;
		notifyProxyThread();
	}

	/** Notify the proxy thread that there is data enqueued */
	protected void notifyProxyThread()
	{
		JavaxUsb.nativeSignalPID( proxyThread.getPID(), proxyThread.getSignal() );
	}

	/**
	 * @return the Runnable that the proxy thread will do when started
	 * @see #start()
	 * @see #stop()
	 * @see #isRunning
	 */
	protected abstract Runnable getProxyRunnable();

	/** @return the Name for this proxyThread */
	protected abstract String getProxyName();

	/**
	 * Called from native thread when a request is completed
	 * @param request the LinuxRequest that completed
	 * @param requestResult the result of the request (errno or bytes xferred)
	 */
	protected void requestCompleted( LinuxRequest request )
	{
		taskManager.post( request.getPostTask() );
	}

	/**
	 * Proxy Thread flag that notifies the Thread when to exit.
	 * <p>
	 * This is a flag that should be monitored by the provided
	 * Runnable.  The Runnable (Thread) should cleanup and exit
	 * when this is changed to false.
	 */
	protected boolean isRunning() { return isRunning; }

	/** @return a MethodHandlerFactory object */
	protected MethodHandlerFactory getMethodHandlerFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getMethodHandlerFactory();
	}

	//*************************************************************************
	// Private methods

	/** Get the file descriptor */
	private int getFileDescriptor() { return fileDescriptor; }

	/** Set the file descriptor */
	private void setFileDescriptor( int fd ) { fileDescriptor = fd; }

	/** @return a LinuxProxyThread object */
	private Thread getLinuxProxyThread() { return proxyThread; }

	/**
	 * Move the next LinuxRequest from the requestVector to the pendingVector and return it.
	 * @return the 0-th element of the requestVector, or null
	 */
	private LinuxRequest dequeueRequestVector()
	{
		LinuxRequest request;

		/* synchronize so the changeover is atomic */
		synchronized ( requestVector ) {
			synchronized ( pendingVector ) {
				try {
					request = (LinuxRequest)requestVector.remove(0);
				} catch ( ArrayIndexOutOfBoundsException aioobE ) {
					return null;
				}

				pendingVector.add( request );
			}
		}

		return request;		
	}

	/**
	 * Get the next LinuxRequest from the cancelVector
	 * @return the 0-th element of the cancelVector, or null
	 */
	private LinuxRequest dequeueCancelVector()
	{
		try {
			return (LinuxRequest)cancelVector.remove(0);
		} catch ( ArrayIndexOutOfBoundsException aioobE ) {
			return null;
		}
	}

	/**
	 * Remove the specified LinuxRequest from the pendingVector
	 * @param request the LinuxRequest to remove
	 */
	private void removePendingVector( LinuxRequest request )
	{
		pendingVector.remove( request );
	}

	/** Check if the request is ok to submit */
	private void checkRequest( LinuxRequest request ) throws UsbException
	{
		if (requestVector.contains(request))
			throw new UsbException( "Submission already queued" );

		if (pendingVector.contains(request))
			throw new UsbException( "Submission already in progress" );
	}

	//*************************************************************************
	// Instance variables

	private TaskScheduler taskManager = new FifoScheduler();

	private Vector requestVector = new Vector();
	private Vector pendingVector = new Vector();
	private Vector cancelVector = new Vector();

	private LinuxProxyThread proxyThread = null;

	private boolean isRunning = false;

	private int fileDescriptor = -1;

	//*************************************************************************
	// Class constants

	private static final String NOT_RUNNING = "Proxy not running";

//FIXME define and use better errorcode.
	private static final int RESULT_CANCELED = -6900;

	//*************************************************************************
	// Inner classes

	public class LinuxProxyThread extends Thread
	{
		/** Constructor */
		public LinuxProxyThread( Runnable r ) { super( r ); }
		/** @return Process (Thread) ID */
		public int getPID() { return pid; }
		/** @reutrn signal to 'notify' this */
		public int getSignal() { return signal; }
		/** @param Process ID of this thread */
		protected void setPID( int pid ) { this.pid = pid; }
		/** @param signal to use to 'notify' this */
		protected void setSignal( int signal ) { this.signal = signal; }

		private int pid = 0, signal = 0;
	}

}
