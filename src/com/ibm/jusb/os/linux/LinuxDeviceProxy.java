package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.*;

/**
 * Proxy implementation for Linux's device-based access.
 * @author Dan Streetman
 */
class LinuxDeviceProxy extends LinuxRequestProxy
{
	/**
	 * Constructor.
	 * @param k The native key.  The key cannot be changed.
	 */
	public LinuxDeviceProxy(String k)
	{
		super();
		key = k;
	}

	//*************************************************************************
	// Public methods

	/** If this is running */
	public boolean isRunning()
	{
		try { return thread.isAlive(); }
		catch ( NullPointerException npE ) { return false; }
	}

	/** Start this proxy. */
	public void start() throws UsbException
	{
		Thread t = new Thread(proxyRunnable);

		t.setDaemon(true);

		synchronized (startLock) {
			t.start();

			try { startLock.wait(); }
			catch ( InterruptedException iE ) { }
		}

		if (0 != startError)
			throw new UsbException("Could not connect to USB device : " + JavaxUsb.nativeGetErrorMessage(startError));
		else
			thread = t;
	}

	//*************************************************************************
	// JNI methods

	/** @return The native device key. */
	private String getKey() { return key; }

	/**
	 * Signal startup completed.
	 * @param error The error number if startup failed, or 0 if startup succeeded.
	 */
	private void startCompleted( int error )
	{
		synchronized (startLock) {
			startError = error;

			startLock.notifyAll();
		}
	}

	//*************************************************************************
	// Instance variables

	private Thread thread = null;
	private String key = null;

	private Runnable proxyRunnable = new Runnable() {
		public void run()
		{ JavaxUsb.nativeDeviceProxy( LinuxDeviceProxy.this ); }
	};

	private Object startLock = new Object();
	private int startError = -1;

}
