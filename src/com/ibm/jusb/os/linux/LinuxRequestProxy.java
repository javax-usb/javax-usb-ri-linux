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

/**
 * Abstract proxy which manages requests to JNI.
 * @author Dan Streetman
 */
abstract class LinuxRequestProxy
{
	//**************************************************************************
	// Public methods

	/**
	 * Submit the Request.
	 * <p>
	 * No checking of the Request is done.
	 * @param request The LinuxRequest.
	 */
	public void submit( LinuxRequest request )
	{
		synchronized(readyList) {
			readyList.add(request);
		}
	}

	/**
	 * Cancel the Request.
	 * @param request The LinuxRequest.
	 */
	public void cancel( LinuxRequest request )
	{
		synchronized(readyList) {
			if (readyList.contains(request)) {
				readyList.remove(request);
				request.setError(-1);
				request.setCompleted(true);
				return;
			}
		}

		synchronized(cancelList) {
			cancelList.add(request);
		}
	}

	//**************************************************************************
	// JNI methods

	/**
	 * If there are any requests waiting.
	 * @return If there are any requests waiting.
	 */
	private boolean isRequestWaiting()
	{
		return !readyList.isEmpty() || !cancelList.isEmpty();
	}

	/**
	 * Get the next ready Request.
	 * @return The next ready Request.
	 */
	private LinuxRequest getReadyRequest()
	{
		LinuxRequest request = null;

		synchronized(readyList) {
			try { request = (LinuxRequest)readyList.remove(0); }
			catch ( IndexOutOfBoundsException ioobE ) { return null; }
	
			inProgressList.add(request);
		}

		return request;
	}

	/**
	 * Get the next cancel Request.
	 * @return The next cancel Request.
	 */
	private LinuxRequest getCancelRequest()
	{
		synchronized(cancelList) {
			try { return (LinuxRequest)cancelList.remove(0); }
			catch ( IndexOutOfBoundsException ioobE ) { return null; }
		}
	}

	/**
	 * Complete a Request.
	 * @param request The LinuxRequest.
	 */
	private void completeRequest(LinuxRequest request)
	{
		inProgressList.remove(request);
	}

	//**************************************************************************
	// Instance variables

	private List readyList = new LinkedList();
	private List cancelList = new LinkedList();
	private List inProgressList = new LinkedList(); /* only proxy Thread touches, no sync required */
}
