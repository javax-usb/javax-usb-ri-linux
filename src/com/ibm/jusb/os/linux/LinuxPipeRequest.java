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

	/** @param c If this is completed or not */
	public void setCompleted(boolean c)
	{
		if (c) {
			getLinuxPipeOsImp().linuxPipeRequestCompleted(this);
			getUsbIrpImp().complete();
		}

		super.setCompleted(c);
	}

	//****************************************************************************
	// Private methods

	/** @return The type of pipe */
	private int getPipeType()
	{
		switch (getLinuxPipeOsImp().getUsbPipeImp().getUsbEndpoint().getType()) {
		case UsbInfoConst.ENDPOINT_TYPE_CONTROL: return PIPE_CONTROL;
		case UsbInfoConst.ENDPOINT_TYPE_BULK: return PIPE_BULK;
		case UsbInfoConst.ENDPOINT_TYPE_INT: return PIPE_INTERRUPT;
		case UsbInfoConst.ENDPOINT_TYPE_ISOC: return PIPE_ISOCHRONOUS;
		default: /* log */ return 0;
		}
	}

	/** @return the endpoint address */
	private byte getEndpointAddress() { return getLinuxPipeOsImp().getUsbPipeImp().getUsbEndpoint().getEndpointAddress(); }

	//*************************************************************************
	// Instance variables

	private UsbIrpImp usbIrpImp = null;

	private LinuxPipeOsImp linuxPipeImp = null;

	private int urbAddress = 0;

	/* These MUST match those defined in jni/linux/JavaxUsbRequest.c */
	private static final int PIPE_CONTROL = 1;
	private static final int PIPE_BULK = 2;
	private static final int PIPE_INTERRUPT = 3;
	private static final int PIPE_ISOCHRONOUS = 4;
}
