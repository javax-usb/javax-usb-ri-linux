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
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxControlPipeImp extends LinuxPipeImp
{

	/** Constructor */
	public LinuxControlPipeImp( UsbPipeAbstraction abstraction ) { super( abstraction ); }

	//*************************************************************************
	// Public methods

	/** Submit a request natively */
	public void submitNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeSubmitControlRequest( request, getUsbPipeAbstraction().getEndpointAddress() );
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

		/*
		 * The USB Spec is vague on this point; however, this is only important on platforms that
		 * 'claim' interfaces (or endpoints).  So, on Linux this currently will not work
		 * (the kernel will insist that the caller have the destination interface 'claimed').
		 * Roger Lindsjo says he talked to Tom Sailer (the Linux userspace USB interface author),
		 * and Tom agrees with Roger that this should go into the kernel; however it's not in yet.
		 */
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
			config = getUsbPipeAbstraction().getUsbDevice().getActiveUsbConfig();
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
