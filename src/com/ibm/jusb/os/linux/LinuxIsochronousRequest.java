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
 * LinuxRequest for use on isochronous pipes
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxIsochronousRequest extends LinuxPipeRequest implements LinuxRequest
{
	/** Constructor */
	public LinuxIsochronousRequest( LinuxRequestFactory factory ) { super( factory ); }

	//*************************************************************************
	// Public methods

	/** Native submit method */
	public void submitNative() { getLinuxPipeImp().submitNative( this ); }

	/** Native abort method */
	public void abortNative() { JavaxUsb.nativeAbortPipeRequest( this ); }

	/** Native complete method */
	public void completeNative() { getLinuxPipeImp().completeNative( this ); }

	/**
	 * Get specified data buffer.
	 * <p>
	 * The only valid index is 0.
	 * @param index 0 is the only valid index.
	 * @throws javax.usb.IndexOutOfBoundsException if the index is not zero.
	 */
	private byte[] getDataBuffer( int index )
	{
		if (0 != index)
			throw new IndexOutOfBoundsException( "Index must be 0 for UsbIrp" );

		return getUsbIrpImp().getData();
	}

	/**
	 * Set the status at the specified index.
	 * <p>
	 * The only valid index is 0.
	 * @param index the only valid index is zero.
	 * @param status the status.
	 * @throws javax.usb.IndexOutOfBoundsException if the index is not zero.
	 */
	private void setStatus( int index, int status )
	{
		if (0 != index)
			throw new IndexOutOfBoundsException( "Index must be 0 for UsbIrp" );

		if (0 > status) {
			UsbException exception = new UsbException( JavaxUsb.nativeGetErrorMessage( status ), status );
			getUsbIrpImp().setUsbException( exception );
		} else {
			getUsbIrpImp().setDataLength( status );
		}
	}

	/** @return the number of 'packets' */
	public int getNumberOfPackets() { return 1; }

	/** @return the size of the data buffer */
	public int getBufferSize() { return getUsbIrpImp().getData().length; }

}
