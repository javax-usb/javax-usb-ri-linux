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

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * LinuxRequest for use on the Default Control Pipe.
 * @author Dan Streetman
 */
class LinuxDcpRequest extends LinuxRequest
{
	//*************************************************************************
	// Public methods

	/** @return The type of this request */
	public int getType() { return LinuxRequest.LINUX_DCP_REQUEST; }

	/** @param error The error. */
	public void setError(int error)
	{
		requestImp.setUsbException( new UsbException("Error during submission : " + JavaxUsb.nativeGetErrorMessage(error), error ) );
	}

	/** @return this request's data buffer */
	public byte[] getData() { return dataBuffer; }

	/** @param data the data buffer to use */
	public void setData( byte[] data ) { dataBuffer = data; }

	/** @return this request's data buffer valid length */
	public int getDataLength() { return getRequestImp().getDataLength(); }

	/** @param len the data buffer valid length (minus 8 for setup packet) */
	public void setDataLength( int len ) { getRequestImp().setDataLength(len - 8); }

	/** @return The RequestImp */
	public RequestImp getRequestImp() { return requestImp; }

	/** @param request The RequestImp. */
	public void setRequestImp(RequestImp request) { requestImp = request; }

	/** @return the address of the assocaited URB */
	public int getUrbAddress() { return urbAddress; }

	/** @param address the address of the assocaited URB */
	public void setUrbAddress( int address ) { urbAddress = address; }

	/** @param c If this is completed. */
	public void setCompleted(boolean c)
	{
		if (c) {
			System.arraycopy(dataBuffer, 8, getRequestImp().getData(), 0, getDataLength());
			getRequestImp().complete();
		}

		super.setCompleted(c);
	}		

	//*************************************************************************
	// Instance variables

	private byte[] dataBuffer = null;

	private RequestImp requestImp = null;

	private int urbAddress = 0;

}
