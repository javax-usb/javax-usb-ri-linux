package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/**
 * Abstract class for Linux requests.
 * @author Dan Streetman
 */
class LinuxRequest
{
	/**
	 * Get the type of this request.
	 * @return The type of this request.
	 */
	public abstract int getType();

	/**
	 * Get the LinuxRequestProxy.
	 * @return The LinuxRequestProxy.
	 */
	public LinuxRequestProxy getLinuxRequestProxy() { return linuxRequestProxy; }

	/**
	 * Set the LinuxRequestProxy.
	 * @param proxy The LinuxRequestProxy.
	 */
	public void setLinuxRequestProxy(LinuxRequestProxy proxy) { linuxRequestProxy = proxy; }

	private LinuxRequestProxy linuxRequestProxy = null;

	public static final int LINUX_PIPE_REQUEST = 1;
	public static final int LINUX_DCP_REQUEST = 2;
	public static final int LINUX_SET_INTERFACE_REQUEST = 3;
	public static final int LINUX_SET_CONFIGURATION_REQUEST = 4;
	public static final int LINUX_INTERFACE_REQUEST = 5;
	public static final int LINUX_ISOCHRONOUS_REQUEST = 6;
}
