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
 * Interrupt parameters to pass to native code
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxInterruptPipeImp extends LinuxPipeImp
{

	/** Constructor */
	public LinuxInterruptPipeImp( UsbPipeAbstraction abstraction ) { super( abstraction ); }

	//*************************************************************************
	// Public methods

	/** Submit a request natively */
	public void submitNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeSubmitInterruptRequest( request, getUsbPipeAbstraction().getEndpointAddress() );
	}

	/** Complete a request natively */
	public void completeNative( LinuxPipeRequest request )
	{
		JavaxUsb.nativeCompleteInterruptRequest( request );
	}

}
