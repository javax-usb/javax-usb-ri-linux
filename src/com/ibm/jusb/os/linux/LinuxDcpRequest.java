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

	/** @return this request's data buffer */
	public byte[] getData() { return dataBuffer; }

	/** @param data the data buffer to use */
	public void setData( byte[] data ) { dataBuffer = data; }

	/** @return The RequestImp */
	public RequestImp getRequestImp() { return requestImp; }

	/** @param request The RequestImp. */
	public void setRequestImp(RequestImp request) { requestImp = request; }

	/** @return the address of the assocaited URB */
	public int getUrbAddress() { return urbAddress; }

	/** @param address the address of the assocaited URB */
	public void setUrbAddress( int address ) { urbAddress = address; }

	//*************************************************************************
	// Instance variables

	private byte[] dataBuffer = null;

	private RequestImp requestImp = null;

	private int urbAddress = 0;

}
