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
 * Interface for Linux Requests
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
interface LinuxRequest extends Recyclable
{
	//*************************************************************************
	// Public methods

	/**
	 * Native submit method.
	 */
	public void submitNative();

	/**
	 * Native abort method.
	 */
	public void abortNative();

	/**
	 * Native complete method.
	 */
	public void completeNative();

	/**
	 * Get the pre-execution Task object.
	 */
	public Task getPreTask();

	/**
	 * Get the post-execution Task object.
	 */
	public Task getPostTask();

	/**
	 * Wait until the request is submitted.
	 * <p>
	 * This returns when isSubmitCompleted() becomes true
	 * or isActive() becomes false.
	 */
	public void waitUntilSubmitCompleted();

	/**
	 * Wait until the request is completed.
	 * <p>
	 * This returns when isRequestCompleted() becomes true
	 * or isActive() becomes false.
	 */
	public void waitUntilRequestCompleted();

	//*************************************************************************
	// Getters

	/**
	 * If this request is active
	 * @return if this is active
	 */
	public boolean isActive();

	/**
	 * If this request has been submitted (or submission failed).
	 * @return if this request has been submitted
	 */
	public boolean isSubmitCompleted();

	/**
	 * Get the submission status of this request
	 * <p>
	 * This is only valid if isSubmitCompleted() is true.
	 * @return the submission status
	 */
	public int getSubmissionStatus();

	/**
	 * If the request is completed.
	 * @return if this request has completed
	 */
	public boolean isRequestCompleted();

	/**
	 * Get the completion status of this request
	 * <p>
	 * This is only valid if isRequestCompleted() is true.
	 * @return the competion status
	 */
	public int getCompletionStatus();

	/**
	 * Get the LinuxRequestProxy this request is active on.
	 * @return this request's LinuxRequestProxy.
	 */
	public LinuxRequestProxy getLinuxRequestProxy();

	//*************************************************************************
	// Setters

	/**
	 * Set this request active.
	 * @param active if this request is active
	 */
	public void setActive( boolean active );

	/**
	 * Set if this request has been submitted (or submission failed).
	 * @param completed if this request has been submitted
	 */
	public void setSubmitCompleted( boolean completed );

	/**
	 * Set the submission status of this request
	 * <p>
	 * This should be set before setSubmitCompleted( true ).
	 * @param status the submission status
	 */
	public void setSubmissionStatus( int status );

	/**
	 * Set if the request is completed.
	 * @param completed if this request has completed
	 */
	public void setRequestCompleted( boolean completed );

	/**
	 * Set the completion status of this request
	 * <p>
	 * This should be set before setRequestCompleted( true ).
	 * @param status the competion status
	 */
	public void setCompletionStatus( int status );

	/**
	 * Set the LinuxRequestProxy this request is active on.
	 * @param proxy this request's LinuxequestProxy.
	 */
	public void setLinuxRequestProxy( LinuxRequestProxy proxy );

}
