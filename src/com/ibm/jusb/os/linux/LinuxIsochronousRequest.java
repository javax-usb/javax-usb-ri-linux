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

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * Special request for use on Isochronous pipes.
 * @author Dan Streetman
 */
class LinuxIsochronousRequest extends LinuxRequest
{
	//*************************************************************************
	// Public methods

	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_ISOCHRONOUS_REQUEST; }

	/** @param The error */
	public void setError(int error)
	{
		for (int i=0; i<getNumberOfPackets(); i++)
			setError( i, error );
	}

	/**
	 * Get the data buffer at the specified index.
	 * @return The data buffer for the specified index.
	 */
	public byte[] getData( int index ) { return getUsbIrpImp(index).getData(); }   

	/**
	 * Get the total size of all buffers in the List.
	 * @return The total size of all buffers in the List.
	 */
	public int getBufferSize()
	{
		int totalSize = 0;
		for (int i=0; i<getNumberOfPackets(); i++)
			totalSize += getData(i).length;
		return totalSize;
	}

	/**
	 * Set the data length of the data at the specified index.
	 * @param index The index of the data.
	 * @param len The data length of the specified indexed data.
	 */
	public void setStatus( int index, int len ) { getUsbIrpImp(index).setDataLength(len); }

	/**
	 * Set the error of the data at the specified index.
	 * @param index The index of the data.
	 * @param err The number of the error that occurred.
	 */
	public void setError( int index, int error )
	{
//FIXME - improve message and/or set correct error number?
		getUsbIrpImp(index).setUsbException(new UsbException(JavaxUsb.nativeGetErrorMessage(error),error));
	}

	/** @return The number of 'packets' */
	public int getNumberOfPackets() { return usbIrpImps.size(); }

	/** @return The UsbIrpImp of */
	public UsbIrpImp getUsbIrpImp( int index ) { return (UsbIrpImp)usbIrpImps.get(index); }

	/** @param list The List of UsbIrpImps */
	public void setUsbIrpImps( List list ) { usbIrpImps = list; }

	/** @return the assocaited LinuxPipeOsImp */
	public LinuxPipeOsImp getLinuxPipeOsImp() { return linuxPipeImp; }

	/** @param pipe the assocaited LinuxPipeOsImp */
	public void setLinuxPipeOsImp( LinuxPipeOsImp pipe ) { linuxPipeImp = pipe; }

	/** @return the address of the assocaited URB */
	public int getUrbAddress() { return urbAddress; }

	/** @param address the address of the assocaited URB */
	public void setUrbAddress( int address ) { urbAddress = address; }

	//*************************************************************************
	// Private methods

	/** @return the endpoint address */
	private byte getEndpointAddress() { return getLinuxPipeOsImp().getUsbPipeImp().getUsbEndpoint().getEndpointAddress(); }

	//*************************************************************************
	// Instance variables

	private List usbIrpImps = null;

	private LinuxPipeOsImp linuxPipeImp = null;

	private int urbAddress = 0;
}
