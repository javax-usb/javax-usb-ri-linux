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

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * LinuxRequest for use on pipes.
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxPipeRequest extends DefaultLinuxRequest implements LinuxRequest
{
	/** Constructor */
	public LinuxPipeRequest( LinuxRequestFactory factory ) { super( factory ); }

	//*************************************************************************
	// Public methods

	/** Native submit method */
	public void submitNative() { getLinuxPipeImp().submitNative( this ); }

	/** Native abort method */
	public void abortNative() { JavaxUsb.nativeAbortPipeRequest( this ); }

	/** Native complete method */
	public void completeNative() { getLinuxPipeImp().completeNative( this ); }

	/** @return this request's data buffer */
	public byte[] getData() { return dataBuffer; }

	/** @param data the data buffer to use */
	public void setData( byte[] data ) { dataBuffer = data; }

	/** @return if Short Packet Detection should be enabled */
	public boolean getAcceptShortPacket() { return getUsbIrpImp().getAcceptShortPacket(); }

	/** @return the assocaited UsbIrpImp */
	public UsbIrpImp getUsbIrpImp() { return usbIrpImp; }

	/** @param irp the assocaited UsbIrpImp */
	public void setUsbIrpImp( UsbIrpImp irp ) { usbIrpImp = irp; }

	/** @return the assocaited LinuxPipeImp */
	public LinuxPipeImp getLinuxPipeImp() { return linuxPipeImp; }

	/** @param pipe the assocaited LinuxPipeImp */
	public void setLinuxPipeImp( LinuxPipeImp pipe ) { linuxPipeImp = pipe; }

	/** @return the address of the assocaited URB */
	public int getUrbAddress() { return urbAddress; }

	/** @param address the address of the assocaited URB */
	public void setUrbAddress( int address ) { urbAddress = address; }

	//*************************************************************************
	// Public overridden methods

	/**
	 * Get the pre-execution Task object.
	 */
	public Task getPreTask() { return preTask; }

	/**
	 * Get the post-execution Task object.
	 */
	public Task getPostTask() { return postTask; }

	//*************************************************************************
	// Recyclable methods

	/** Clean this object */
	public void clean()
	{
		super.clean();
		dataBuffer = null;
		usbIrpImp = null;
		linuxPipeImp = null;
		urbAddress = 0;
	}

	//*************************************************************************
	// Protected methods

	protected void preTaskMethod()
	{
		getUsbIrpImp().getPreSubmissionTask().execute( getUsbIrpImp() );
	}

	protected void postTaskMethod()
	{
		if ( getUsbIrpImp().getDataLength() < 0 ) {
			getUsbIrpImp().setDataLength( getCompletionStatus() );
		}

		if (0 > getCompletionStatus()) {
			String errorMessage = JavaxUsb.nativeGetErrorMessage( getCompletionStatus() );

			UsbException uE = new UsbException( errorMessage, getCompletionStatus() );

			getUsbIrpImp().setUsbException( uE );
		}

		getLinuxPipeImp().removeUsbIrpImpKey( getUsbIrpImp() );

		UsbIrpImp irp = getUsbIrpImp();

		recycle();

		irp.getPostSubmissionTask().execute( irp );
	}

	//*************************************************************************
	// Instance variables

	private byte[] dataBuffer = null;

	private UsbIrpImp usbIrpImp = null;

	private LinuxPipeImp linuxPipeImp = null;

	private int urbAddress = 0;

	private Task preTask = new Task() {
		public void execute() { LinuxPipeRequest.this.preTaskMethod(); }
	};

	private Task postTask = new Task() {
		public void execute() { LinuxPipeRequest.this.postTaskMethod(); }
	};

}
