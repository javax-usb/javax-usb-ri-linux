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

/**
 * Linux implementation of UsbPipeImpFactory
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxPipeImpFactory implements UsbPipeImpFactory {

	public LinuxPipeImpFactory() { }

	/**
	 * @param abstraction the UsbPipeAbstraction to use
	 * @return a new UsbPipeImp
	 */
	public UsbPipeImp createUsbPipeImp( UsbPipeAbstraction abstraction )
	{
		switch ( abstraction.getType() ) {
			case UsbInfoConst.ENDPOINT_TYPE_CONTROL:
				return new LinuxControlPipeImp( abstraction );
			case UsbInfoConst.ENDPOINT_TYPE_INT:
				return new LinuxInterruptPipeImp( abstraction );
			case UsbInfoConst.ENDPOINT_TYPE_ISOC:
				return new LinuxIsochronousPipeImp( abstraction );
			case UsbInfoConst.ENDPOINT_TYPE_BULK:
				return new LinuxBulkPipeImp( abstraction );
			default:
				throw new UsbRuntimeException("Cannot create pipe for type " + abstraction.getType() );
		}
	}

}
