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
 * Interface for configuration-changing Requests.
 * @author Dan Streetman
 */
public class LinuxSetConfigurationRequest extends LinuxRequest
{
	//*************************************************************************
	// Public methods

	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_SET_CONFIGURATION_REQUEST; }

	/** @return The configuration number */
	public int getConfiguration() { return configuration; }

	/** @param config The configuration number */
	public void setConfiguration( byte config ) { configuration = UsbUtil.unsignedInt(config); }

	/** @return The error that occured, or 0 if none occurred. */
	public int getError() { return errorNumber; }

	/** @param error The number of the error that occurred. */
	public void setError(int error) { errorNumber = error; }

	/** @return The RequestImp */
	public RequestImp getRequestImp() { return requestImp; }

	/** @param request The RequestImp. */
	public void setRequestImp(RequestImp request) { requestImp = request; }

	/** @param c If this is completed. */
	public void setCompleted(boolean c)
	{
		if (c)
			getRequestImp().complete();

		super.setCompleted(c);
	}		

	//*************************************************************************
	// Instance variables

	private RequestImp requestImp = null;

	private int configuration;

	private int errorNumber = 0;
}
