package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import com.ibm.jusb.util.*;

/**
 * Linux Request Object
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
abstract class DefaultLinuxRequest implements LinuxRequest
{
	/** Constructor */
	public DefaultLinuxRequest( LinuxRequestFactory factory ) { linuxRequestFactory = factory; }

	//*************************************************************************
	// Public methods

	/**
	 * Get the pre-execution Task object.
	 */
	public Task getPreTask() { return nullTask; }

	/**
	 * Get the post-execution Task object.
	 */
	public Task getPostTask() { return nullTask; }

	/**
	 * Wait until the request is submitted.
	 * <p>
	 * This returns when isSubmitCompleted() becomes true
	 * or isActive() becomes false.
	 */
	public void waitUntilSubmitCompleted()
	{
		if (isActive() && !isSubmitCompleted()) {
			synchronized ( submissionLock ) {
				submissionWaiters++;

				while (isActive() && !isSubmitCompleted()) {
					try { submissionLock.wait(); }
					catch ( InterruptedException iE ) { }
				}

				submissionWaiters--;
			}
		}
	}

	/**
	 * Wait until the request is completed.
	 * <p>
	 * This returns when isRequestCompleted() becomes true
	 * or isActive() becomes false.
	 */
	public void waitUntilRequestCompleted()
	{
		if (isActive() && !isRequestCompleted()) {
			synchronized ( completionLock ) {
				completionWaiters++;

				while (isActive() && !isRequestCompleted()) {
					try { completionLock.wait(); }
					catch ( InterruptedException iE ) { }
				}

				completionWaiters--;
			}
		}
	}

	//*************************************************************************
	// Recyclable methods

	/** Clean this object */
	public void clean()
	{
		setSubmitCompleted( false );
		setRequestCompleted( false );
	}

	/** Recycle */
	public void recycle()
	{
		linuxRequestFactory.returnLinuxRequest( this );
	}

	//*************************************************************************
	// Getters

	/**
	 * If this request is active
	 * @return if this is active
	 */
	public boolean isActive() { return requestActive; }

	/**
	 * If this request has been submitted (or submission failed).
	 * @return if this request has been submitted
	 */
	public boolean isSubmitCompleted() { return submitCompleted; }

	/**
	 * Get the submission status of this request
	 * <p>
	 * This is only valid if isSubmitCompleted() is true.
	 * @return the submission status
	 */
	public int getSubmissionStatus() { return submissionStatus; }

	/**
	 * If the request is completed.
	 * @return if this request has completed
	 */
	public boolean isRequestCompleted() { return requestCompleted; }

	/**
	 * Get the completion status of this request
	 * <p>
	 * This is only valid if isRequestCompleted() is true.
	 * @return the competion status
	 */
	public int getCompletionStatus() { return completionStatus; }

	/**
	 * Get the LinuxRequestProxy this request is active on.
	 * @return this request's LinuxRequestProxy.
	 */
	public LinuxRequestProxy getLinuxRequestProxy() { return linuxRequestProxy; }

	//*************************************************************************
	// Setters

	/**
	 * Set this request active.
	 * @param active if this request is active
	 */
	public void setActive( boolean active )
	{
		requestActive = active;

		if (!requestActive) {
			notifySubmissionWaiters();
			notifyCompletionWaiters();
		}
	}

	/**
	 * Set if this request has been submitted (or submission failed).
	 * @param completed if this request has been submitted
	 */
	public void setSubmitCompleted( boolean completed )
	{
		submitCompleted = completed;

		if (submitCompleted)
			notifySubmissionWaiters();
	}

	/**
	 * Set the submission status of this request
	 * <p>
	 * This should be set before setSubmitCompleted( true ).
	 * @param status the submission status
	 */
	public void setSubmissionStatus( int status ) { submissionStatus = status; }

	/**
	 * Set if the request is completed.
	 * @param completed if this request has completed
	 */
	public void setRequestCompleted( boolean completed )
	{
		requestCompleted = completed;

		if (requestCompleted)
			notifyCompletionWaiters();
	}

	/**
	 * Set the completion status of this request
	 * <p>
	 * This should be set before setRequestCompleted( true ).
	 * @param status the competion status
	 */
	public void setCompletionStatus( int status ) { completionStatus = status; }

	/**
	 * Set the LinuxRequestProxy this request is active on.
	 * @param proxy this request's LinuxRequestProxy.
	 */
	public void setLinuxRequestProxy( LinuxRequestProxy proxy ) { linuxRequestProxy = proxy; }

	//*************************************************************************
	// Protected methods

	/** Notify submission waiters */
	protected void notifySubmissionWaiters()
	{
		if (0 < submissionWaiters) {
			synchronized( submissionLock ) {
				submissionLock.notifyAll();
			}
		}
	}

	/** Notify completion waiters */
	protected void notifyCompletionWaiters()
	{
		if (0 < completionWaiters) {
			synchronized( completionLock ) {
				completionLock.notifyAll();
			}
		}
	}

	//*************************************************************************
	// Instance variables

	private LinuxRequestFactory linuxRequestFactory = null;

	private LinuxRequestProxy linuxRequestProxy = null;

	private boolean requestActive = false;
	private boolean submitCompleted = false;
	private boolean requestCompleted = false;

	private int submissionStatus = -1;
	private int completionStatus = -1;

	private Object submissionLock = new Object();
	private Object completionLock = new Object();

	private int submissionWaiters = 0;
	private int completionWaiters = 0;

	protected Task nullTask = new Task() {
		public void execute() { }
	};

	protected Task recycleTask = new Task() {
		public void execute() { DefaultLinuxRequest.this.recycle(); }
	};
}
