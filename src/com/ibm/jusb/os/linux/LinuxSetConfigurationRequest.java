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

import com.ibm.jusb.util.*;

/**
 * Interface for configuration-changing Requests.
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
public class LinuxSetConfigurationRequest extends DefaultLinuxRequest implements LinuxRequest
{
	/** Constructor */
	public LinuxSetConfigurationRequest( LinuxRequestFactory factory ) { super(factory); }

	//*************************************************************************
	// Public methods

	/**
	 * Native submit method.
	 */
	public void submitNative() { JavaxUsb.nativeSetConfiguration( this ); }

	/**
	 * Native abort method.
	 */
	public void abortNative() { /* Submission is synchronous, cannot abort */ }

	/**
	 * Native complete method.
	 */
	public void completeNative() { /* Submission is synchronous, completion handled there */ }

	/**
	 * Get the configuration to change to.
	 * <p>
	 * Note that this returns an unsigned integer if the configuration has been set;
	 * if the configuration has not been {@link #setConfiguration(byte) set},
	 * this returns -1.
	 * @return the configuration number to change to (as an unsigned int).
	 */
	public int getConfiguration() { return configuration; }

	/**
	 * Set the configuration to change to.
	 * <p>
	 * The byte-type number is converted to an unsigned integer.
	 * @param config the configuration number to change to.
	 */
	public void setConfiguration( byte config ) { configuration = UsbUtil.unsignedInt(config); }

	//*************************************************************************
	// Recyclable methods

	/** Clean this object */
	public void clean()
	{
		super.clean();
		configuration = -1;
	}

	//*************************************************************************
	// Instance variables

	private int configuration = -1;

}
