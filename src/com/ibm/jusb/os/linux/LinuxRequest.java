package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/**
 * Abstract class for Linux requests.
 * @author Dan Streetman
 */
abstract class LinuxRequest
{
	/**
	 * Get the type of this request.
	 * @return The type of this request.
	 */
	public abstract int getType();

	/** 
	 * Set the error that occurred.
	 * @param The error that occcured.
	 */
	public abstract void setError(int error);

	/** Wait until completed. */
	public void waitUntilCompleted()
	{
		synchronized ( waitLock ) {
			waitCount++;
			while (!isCompleted()) {
				try { waitLock.wait(); }
				catch ( InterruptedException iE ) { }
			}
			waitCount--;
		}
	}

	/** @return If this is completed. */
	public boolean isCompleted() { return completed; }

	/**
	 * Set completed.
	 * @param c If this is completed or not.
	 */
	public void setCompleted(boolean c)
	{
		completed = c;

		if (completed)
			notifyCompleted();
	}

	/** Notify waiteers of completion. */
	public void notifyCompleted()
	{
		if (0 < waitCount) {
			synchronized ( waitLock ) {
				waitLock.notifyAll();
			}
		}		
	}

	private LinuxRequestProxy linuxRequestProxy = null;

	private Object waitLock = new Object();
	private int waitCount = 0;
	private boolean completed = false;

	/* These MUST be the same as those defined in jni/linux/JavaxUsbDeviceProxy.c */
	public static final int LINUX_PIPE_REQUEST = 1;
	public static final int LINUX_DCP_REQUEST = 2;
	public static final int LINUX_SET_INTERFACE_REQUEST = 3;
	public static final int LINUX_SET_CONFIGURATION_REQUEST = 4;
	public static final int LINUX_CLAIM_INTERFACE_REQUEST = 5;
	public static final int LINUX_IS_CLAIMED_INTERFACE_REQUEST = 6;
	public static final int LINUX_RELEASE_INTERFACE_REQUEST = 7;
	public static final int LINUX_ISOCHRONOUS_REQUEST = 8;
}
