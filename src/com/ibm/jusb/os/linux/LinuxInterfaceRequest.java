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

	/** @return If this is a claim request (true) or release request (false) */
	public boolean isClaimRequest() { return claimRequest; }

	/** @param claim If this is a claim request (true) or release request (false) */
	public void setClaimRequest( boolean claim ) { claimRequest = claim; }

	//*************************************************************************
	// Instance variables

	private byte interfaceNumber;

	private boolean claimRequest;
}
