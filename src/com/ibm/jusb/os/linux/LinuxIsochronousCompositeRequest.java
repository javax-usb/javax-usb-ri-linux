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
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * LinuxRequest for use (with UsbCompositeIrps) on isochronous pipes.
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxIsochronousCompositeRequest extends LinuxIsochronousRequest implements LinuxRequest
{
	/** Constructor */
	public LinuxIsochronousCompositeRequest( LinuxRequestFactory factory ) { super( factory ); }

	//*************************************************************************
	// Public methods

	/**
	 * Get specified data buffer.
	 * @param index the index of the data buffer to get.
	 * @throws javax.usb.IndexOutOfBoundsException if the index is invalid.
	 */
	private byte[] getDataBuffer( int index )
	{
		return ((UsbCompositeIrpImp)getUsbIrpImp()).getUsbIrps().getUsbIrp( index ).getData();
	}

	/**
	 * Set the status at the specified index..
	 * @param index the index to set the status at.
	 * @param status the status.
	 * @throws javax.usb.IndexOutOfBoundsException if the index is invalid.
	 */
	private void setStatus( int index, int status )
	{
		if (0 > status) {
			UsbException exception = new UsbException( JavaxUsb.nativeGetErrorMessage( status ), status );
			((UsbIrpImp)((UsbCompositeIrpImp)getUsbIrpImp()).getUsbIrps().getUsbIrp( index )).setUsbException( exception );
		} else {
			((UsbIrpImp)((UsbCompositeIrpImp)getUsbIrpImp()).getUsbIrps().getUsbIrp( index )).setDataLength( status );
		}
	}

	/** @return the number of 'packets' (individual UsbIrps) */
	public int getNumberOfPackets() { return ((UsbCompositeIrpImp)getUsbIrpImp()).getUsbIrps().size(); }

	/** @return the total size of all data buffers */
	public int getBufferSize()
	{
		UsbIrpIterator iterator = ((UsbCompositeIrpImp)getUsbIrpImp()).getUsbIrps().usbIrpIterator();
		int size = 0;

		while (iterator.hasNext())
			size += iterator.nextUsbIrp().getData().length;

		return size;
	}

}
