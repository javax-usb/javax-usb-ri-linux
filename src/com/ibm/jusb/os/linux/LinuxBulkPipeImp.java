package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import com.ibm.jusb.*;

/**
 * LinuxPipeImp implementation for Bulk pipe.
 * <p>
 * This must be set up before use.  See {@link com.ibm.jusb.os.linux.LinuxPipeImp LinuxPipeImp} for details.
 * @author Dan Streetman
 */
class LinuxBulkPipeImp extends LinuxPipeImp
{
	/** Constructor */
    public LinuxBulkPipeImp( UsbPipeImp pipe, LinuxDeviceProxy proxy ) { super(pipe,proxy); }

	//*************************************************************************
	// Public methods

	/** Submit a request natively */
	public void submitNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeSubmitBulkRequest( request, getUsbPipeImp().getEndpointAddress() );
	}

	/** Complete a request natively */
	public void completeNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeCompleteBulkRequest( request );
	}

}
