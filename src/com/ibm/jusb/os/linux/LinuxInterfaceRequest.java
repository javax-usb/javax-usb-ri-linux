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
abstract class LinuxInterfaceRequest extends LinuxRequest
{
	//*************************************************************************
	// Public methods

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

	//*************************************************************************
	// Inner classes

	public static class LinuxClaimInterfaceRequest extends LinuxInterfaceRequest
	{ public int getType() { return LinuxRequest.LINUX_CLAIM_INTERFACE_REQUEST; } }

	public static class LinuxIsClaimedInterfaceRequest extends LinuxInterfaceRequest
	{ public int getType() { return LinuxRequest.LINUX_IS_CLAIMED_INTERFACE_REQUEST; } }

	public static class LinuxReleaseInterfaceRequest extends LinuxInterfaceRequest
	{ public int getType() { return LinuxRequest.LINUX_RELEASE_INTERFACE_REQUEST; } }

}
