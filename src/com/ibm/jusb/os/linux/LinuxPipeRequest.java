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
 * LinuxRequest for use on pipes.
 * @author Dan Streetman
 */
class LinuxPipeRequest extends LinuxRequest
{
	//*************************************************************************
	// Public methods

	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_PIPE_REQUEST; }

	/** @return this request's data buffer */
	public byte[] getData() { return getUsbIrpImp().getData(); }

	/** @return if Short Packet Detection should be enabled */
	public boolean getAcceptShortPacket() { return getUsbIrpImp().getAcceptShortPacket(); }

	/** @param len The data's length. */
	public void setDataLength(int len) { getUsbIrpImp().setDataLength(len); }

	/** @param error The number of the error that occurred. */
	public void setError(int error)
	{
//FIXME - improve error number handling
		getUsbIrpImp().setUsbException(new UsbException("Error during submission : " + JavaxUsb.nativeGetErrorMessage(error),error));
	}

	/** @return the assocaited UsbIrpImp */
	public UsbIrpImp getUsbIrpImp() { return usbIrpImp; }

	/** @param irp the assocaited UsbIrpImp */
	public void setUsbIrpImp( UsbIrpImp irp ) { usbIrpImp = irp; }

	/** @return the assocaited LinuxPipeOsImp */
	public LinuxPipeOsImp getLinuxPipeOsImp() { return linuxPipeImp; }

	/** @param pipe the assocaited LinuxPipeOsImp */
	public void setLinuxPipeOsImp( LinuxPipeOsImp pipe ) { linuxPipeImp = pipe; }

	/** @return the address of the assocaited URB */
	public int getUrbAddress() { return urbAddress; }

	/** @param address the address of the assocaited URB */
	public void setUrbAddress( int address ) { urbAddress = address; }

	//*************************************************************************
	// Instance variables

	private UsbIrpImp usbIrpImp = null;

	private LinuxPipeOsImp linuxPipeImp = null;

	private int urbAddress = 0;

}
