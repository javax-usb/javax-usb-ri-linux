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
 * Request to claim or release an interface.
 * @author Dan Streetman
 */
class LinuxInterfaceRequest extends LinuxRequest
{
	//*************************************************************************
	// Public methods

	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_INTERFACE_REQUEST; }

	/** @return The interface number */
	public int getInterfaceNumber() { return interfaceNumber; }

	/** @param number The interface number */
	public void setInterfaceNumber( int number ) { interfaceNumber = number; }

	/** @param error The number of the error that occurred. */
	public void setError(int error) { errorNumber = error; }

	/** @return The error number, or 0 if no error occurred. */
	public int getError() { return errorNumber; }

	/** @return If the interface is claimed */
	public boolean isClaimed() { return claimed; }

	/** @param c If the interface is claimed */
	public void setClaimed(boolean c) { claimed = c; }

	//*************************************************************************
	// Instance variables

	private int interfaceNumber;

	private int errorNumber = 0;

	private boolean claimed = false;
}
