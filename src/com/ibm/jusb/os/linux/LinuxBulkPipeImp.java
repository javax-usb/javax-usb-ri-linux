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
 * Bulk parameters to pass to native code
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxBulkPipeImp extends LinuxPipeImp
{

	/** Constructor */
    public LinuxBulkPipeImp( UsbPipeAbstraction abstraction ) { super( abstraction ); }

	//*************************************************************************
	// Public methods

	/** Submit a request natively */
	public void submitNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeSubmitBulkRequest( request, getUsbPipeAbstraction().getEndpointAddress() );
	}

	/** Complete a request natively */
	public void completeNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeCompleteBulkRequest( request );
	}

}
