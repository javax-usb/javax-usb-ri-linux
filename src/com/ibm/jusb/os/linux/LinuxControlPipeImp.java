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
import javax.usb.util.*;

import com.ibm.jusb.*;

/**
 * Control parameters to pass to native code
 * <p>
 * This must be set up before use.  See {@link com.ibm.jusb.os.linux.LinuxPipeImp LinuxPipeImp} for details.
 * @author Dan Streetman
 */
class LinuxControlPipeImp extends LinuxPipeImp
{
	/** Constructor */
	public LinuxControlPipeImp( UsbPipeImp pipe, LinuxDeviceProxy proxy ) { super(pipe,proxy); }

	//*************************************************************************
	// Public methods

	/** Submit a request natively */
	public void submitNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeSubmitControlRequest( request, getUsbPipeImp().getEndpointAddress() );
	}

	/** Complete a request natively */
	public void completeNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeCompleteControlRequest( request );
	}

	/**
	 * Asynchronous submission using a UsbIrpImp.
	 * @param irp the UsbIrpImp to use for this submission
     * @exception javax.usb.UsbException if error occurs
	 */
	public void asyncSubmit( UsbIrpImp irp ) throws UsbException
	{
		checkControlHeader( irp.getData() );

		super.asyncSubmit( irp );
	}

	//*************************************************************************
	// Private methods

	/**
	 * Check the header of a Standard (Spec-defined) Control Message
	 * @param data the data containing the header to check
	 * @throws UsbException if the header is not ok
	 */
	private void checkControlHeader( byte[] data ) throws UsbException
	{
		if (data.length < 8)
			throw new UsbException( "Control pipe submission header too short" );

		/** Vendor requests have no format limitations */
		if (RequestConst.REQUESTTYPE_TYPE_VENDOR == (data[0] & RequestConst.REQUESTTYPE_TYPE_MASK))
			return;

		/*
		 * See linux/drivers/usb/devio.c in the kernel for the corresponding 'claim' check in the kernel.
		 */
		if (RequestConst.REQUESTTYPE_RECIPIENT_INTERFACE == (data[0] & RequestConst.REQUESTTYPE_RECIPIENT_MASK)) {
			if (!getLinuxDeviceProxy().isInterfaceClaimed( data[4] ))
				throw new UsbException( "Interface not claimed" );
		} else if (RequestConst.REQUESTTYPE_RECIPIENT_ENDPOINT == (data[0] & RequestConst.REQUESTTYPE_RECIPIENT_MASK)) {
			if (!getLinuxDeviceProxy().isInterfaceClaimed( getInterfaceNumberForEndpointAddress( data[4] ) ))
				throw new UsbException( "Interface not claimed" );
		}
	}

	/** Get the number of the interface that 'owns' the specified endpoint */
	private byte getInterfaceNumberForEndpointAddress( byte epAddr ) throws UsbException
	{
		UsbConfig config;

		try {
			config = getUsbPipeImp().getUsbDevice().getActiveUsbConfig();
		} catch ( UsbRuntimeException urE ) {
			throw new UsbException( "Device is not configured.", UsbInfoConst.USB_INFO_ERR_NOT_CONFIGURED );
		}

		UsbInfoListIterator ifaces = config.getUsbInterfaces();

		while (ifaces.hasNext()) {
			UsbInterface iface = (UsbInterface)ifaces.nextUsbInfo();

			if (iface.containsUsbEndpoint( epAddr ))
				return iface.getInterfaceNumber();
		}

		throw new UsbException( "Invalid endpoint address 0x" + UsbUtil.toHexString( epAddr ) );
	}

}
