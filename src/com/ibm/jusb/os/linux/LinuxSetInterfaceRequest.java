package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.util.UsbUtil;

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * Interface for interface-changing Requests.
 * @author Dan Streetman
 */
public class LinuxSetInterfaceRequest extends LinuxControlRequest
{
	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_SET_INTERFACE_REQUEST; }

	/** @return The interface number */
	public int getInterface() { return interfaceNumber; }

	/** @return The interface setting */
	public int getSetting() { return interfaceSetting; }

	/** @param irp The UsbControlIrpImp */
	public void setUsbIrpImp(UsbControlIrpImp irp)
	{
		super.setUsbIrpImp(irp);
		interfaceNumber = UsbUtil.unsignedInt(irp.wIndex());
		interfaceSetting = UsbUtil.unsignedInt(irp.wValue());
	}

	private int interfaceNumber = 0;
	private int interfaceSetting = 0;
}
