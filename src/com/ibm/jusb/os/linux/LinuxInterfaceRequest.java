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
 * LinuxRequest for use in claiming/releasing interfaces.
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class LinuxInterfaceRequest extends DefaultLinuxRequest implements LinuxRequest
{
	/** Constructor */
	public LinuxInterfaceRequest( LinuxRequestFactory factory ) { super( factory ); }

	//*************************************************************************
	// Public methods

	/** Native submit method */
	public void submitNative() { JavaxUsb.nativeSubmitInterfaceRequest( this ); }

	/** Native abort method */
	public void abortNative() { }

	/** Native complete method */
	public void completeNative() { }

	/**
	 * Get the interface number
	 * @return the interface number
	 */
	public byte getInterfaceNumber() { return interfaceNumber; }

	/**
	 * Set the interface number
	 * @param number the interface number
	 */
	public void setInterfaceNumber( byte number ) { interfaceNumber = number; }

	/**
	 * If this request is to claim the interface
	 * @return true if this request is to claim the interface, false if the request is to release
	 */
	public boolean isClaimRequest() { return claimRequest; }

	/**
	 * Set if this request is to claim the interface
	 * @param claim if this request is to claim the interface
	 */
	public void setClaimRequest( boolean claim ) { claimRequest = claim; }

	//*************************************************************************
	// Instance variables

	/** This will be unsigned in JNI, so -1 is 255 */
	private byte interfaceNumber = -1;

	private boolean claimRequest = false;

}
