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

import com.ibm.jusb.util.*;

/**
 * LinuxRequest for use on the Default Control Pipe.
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxDcpRequest extends DefaultLinuxRequest implements LinuxRequest
{
	/** Constructor */
	public LinuxDcpRequest( LinuxRequestFactory factory ) { super( factory ); }

	//*************************************************************************
	// Public methods

	/** Native submit method */
	public void submitNative() { JavaxUsb.nativeSubmitDcpRequest( this ); }

	/** Native abort method */
	public void abortNative() { JavaxUsb.nativeAbortDcpRequest( this ); }

	/** Native complete method */
	public void completeNative() { JavaxUsb.nativeCompleteDcpRequest( this ); }

	/** @return this request's data buffer */
	public byte[] getData() { return dataBuffer; }

	/** @param data the data buffer to use */
	public void setData( byte[] data ) { dataBuffer = data; }

	/** @return the address of the assocaited URB */
	public int getUrbAddress() { return urbAddress; }

	/** @param address the address of the assocaited URB */
	public void setUrbAddress( int address ) { urbAddress = address; }

	//*************************************************************************
	// Recyclable methods

	/** Clean this object */
	public void clean()
	{
		super.clean();
		dataBuffer = null;
		urbAddress = 0;
	}

	//*************************************************************************
	// Instance variables

	private byte[] dataBuffer = null;

	private int urbAddress = 0;

}
