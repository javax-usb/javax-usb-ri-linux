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
	public byte getInterfaceNumber() { return interfaceNumber; }

	/** @param number The interface number */
	public void setInterfaceNumber( byte number ) { interfaceNumber = number; }

	/** @return The type of interface request */
	public int getClaimType() { return claimType; }

	/** @param type The type of claim */
	public void setClaimType(int type) { claimType = type; }

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

	private byte interfaceNumber;

	private int claimType = 0;

	private int errorNumber = 0;

	private boolean claimed = false;

	public static final int INTERFACE_CLAIM = 1;
	public static final int INTERFACE_RELEASE = 2;
	public static final int INTERFACE_IS_CLAIMED = 3;
}
