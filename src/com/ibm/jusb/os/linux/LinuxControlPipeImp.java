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
import com.ibm.jusb.os.*;

/**
 * Control parameters to pass to native code
 * <p>
 * This must be set up before use.  See {@link com.ibm.jusb.os.linux.LinuxPipeOsImp LinuxPipeOsImp} for details.
 * @author Dan Streetman
 */
class LinuxControlPipeImp extends LinuxPipeOsImp
{
	/** Constructor */
	public LinuxControlPipeImp( UsbPipeImp pipe, LinuxInterfaceOsImp iface ) { super(pipe,iface); }

	/**
	 * Asynchronous submission using a UsbControlIrpImp.
	 * @param irp the UsbControlIrpImp to use for this submission
     * @exception UsbException If any error occurrs.
	 */
	public void asyncSubmit( UsbControlIrpImp irp ) throws UsbException
	{
//FIXME - a check could be added here to verify claimed interface, if appropriate, as the kernel errors are generic

		super.asyncSubmit( irp );
	}

}
