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
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * Handles maintainence of RequestProxy
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxDeviceProxy extends LinuxRequestProxy
{

	/** Constructor */
	public LinuxDeviceProxy( UsbDevice device ) { usbDevice = device; }

	//*************************************************************************
	// Public methods

	/** @return the UsbDevice associated with this proxy */
	public UsbDevice getUsbDevice() { return usbDevice; }

	/** @return if the specified interface is claimed by this proxy */
	public boolean isInterfaceClaimed( byte interfaceNumber )
	{
		return interfaceTable.containsKey( new Byte( interfaceNumber ) );
	}

	/**
	 * Claim an interface
	 * @param interfaceNumber the number of the interface to claim
	 * @throws javax.usb.UsbException if the interface could not be claimed
	 */
	public void claimInterface( byte interfaceNumber ) throws UsbException
	{
		synchronized ( interfaceTable ) {
			if ( isInterfaceClaimed( interfaceNumber ) )
				return;

			LinuxInterfaceRequest request = (LinuxInterfaceRequest)getLinuxInterfaceRequestFactory().takeLinuxRequest();

			request.setInterfaceNumber( interfaceNumber );
			request.setClaimRequest( true );

			try {
				submitRequest( request );
			} catch ( UsbException uE ) {
				request.recycle();

				throw uE;
			}

			request.waitUntilRequestCompleted();

			if ( 0 == request.getCompletionStatus() ) {
				interfaceTable.put( new Byte( interfaceNumber ), request );
			} else {
				int errorNumber = request.getCompletionStatus();
				String errorMessage = JavaxUsb.nativeGetErrorMessage( errorNumber );

				request.recycle();

				throw new UsbException( "Could not claim interface : " + errorMessage, errorNumber );
			}
		}
	}

	/**
	 * Release an interface
	 * @param intefaceNumber the number of the interface to release
	 */
	public void releaseInterface( byte interfaceNumber ) throws UsbException
	{
		synchronized ( interfaceTable ) {
			if ( !isInterfaceClaimed( interfaceNumber ) )
				return;

			LinuxInterfaceRequest request = (LinuxInterfaceRequest)interfaceTable.get( new Byte( interfaceNumber ) );

			request.setClaimRequest( false );

			try {
				submitRequest( request );
			} catch ( UsbException uE ) {
				throw uE;
			}

			request.recycle();

			interfaceTable.remove( new Byte( interfaceNumber ) );
		}
	}

	/** Start this proxy */
	public void start() throws UsbException
	{
		synchronized ( startLock ) {
			super.start();

			while ( !doneStarting ) {
				try { startLock.wait(); }
				catch ( InterruptedException iE ) { }
			}
		}

		if (!isRunning()) {
			String errorMessage = JavaxUsb.nativeGetErrorMessage( startupErrno );

			throw new UsbException( "Could not start device proxy : " + errorMessage, startupErrno );
		}
	}

	//*************************************************************************
	// Protected overridden methods

	/** @return the Runnable proxy */
	protected Runnable getProxyRunnable() { return proxyRunnable; }

	/** @return the Name for this proxyThread */
	protected String getProxyName() { return DEVICE_PROXY_NAME + " - " + getUsbDevice().getName(); }

	//*************************************************************************
	// Private methods

	/** Called from native thread when startup is completed */
	private void startupCompleted()
	{
		synchronized ( startLock ) {
			doneStarting = true;
			startLock.notifyAll();
		}
	}

	/** Get a LinuxInterfaceRequestFactory */
	private LinuxRequestFactory getLinuxInterfaceRequestFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getLinuxInterfaceRequestFactory();
	}

	//*************************************************************************
	// Protected methods

	/**
	 * Notify that an interface claim is done.
	 * @param interfaceRequest the LinuxInterfaceRequest to notify
	 */
	protected void notifyInterface( Object request )
	{
//FIXME - do this in JNI
//Remove the need to (cast)
		((LinuxInterfaceRequest)request).setRequestCompleted( true );
//FIXME
	}

	//*************************************************************************
	// Instance variables

	private UsbDevice usbDevice = null;

	private Vector interfaceRequestVector = new Vector();
	private Hashtable interfaceTable = new Hashtable();

	private Object startLock = new Object();
	private boolean doneStarting = false;
	private int startupErrno = 0;

	private Runnable proxyRunnable = new Runnable() {
		public void run()
		{ JavaxUsb.nativeDeviceProxy( LinuxDeviceProxy.this ); }
	};

	//*************************************************************************
	// Class constants

	private static final String DEVICE_PROXY_NAME = "Device Proxy";

}
