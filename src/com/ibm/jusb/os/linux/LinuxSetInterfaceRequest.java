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
public class LinuxSetInterfaceRequest extends LinuxRequest
{
	//*************************************************************************
	// Public methods

	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_SET_INTERFACE_REQUEST; }

	/** @return The interface number */
	public int getInterface() { return interfaceNumber; }

	/** @return The interface setting */
	public int getSetting() { return interfaceSetting; }

	/** @param number The interface number */
	public void setInterface( short number ) { interfaceNumber = UsbUtil.unsignedInt(number); }

	/** @param setting The interface setting */
	public void setSetting( short setting ) { interfaceSetting = UsbUtil.unsignedInt(setting); }

	/** @return The error that occured, or 0 if none occurred. */
	public int getError() { return errorNumber; }

	/** @param error The number of the error that occurred. */
	public void setError(int error) { errorNumber = error; }

	/** @return The ControlUsbIrpImp */
	public UsbIrpImp.ControlUsbIrpImp getControlUsbIrpImp() { return controlUsbIrpImp; }

	/** @param irp The ControlUsbIrpImp. */
	public void setControlUsbIrpImp(UsbIrpImp.ControlUsbIrpImp irp) { controlUsbIrpImp = irp; }

	/** @param c If this is completed. */
	public void setCompleted(boolean c)
	{
		if (c)
			getControlUsbIrpImp().complete();

		super.setCompleted(c);
	}		

	//*************************************************************************
	// Instance variables

	private UsbIrpImp.ControlUsbIrpImp controlUsbIrpImp = null;

	private int interfaceNumber;
	private int interfaceSetting;

	private int errorNumber = 0;

}
