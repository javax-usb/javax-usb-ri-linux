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
 * Interface for interface-changing Requests.
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
public class LinuxSetInterfaceRequest extends DefaultLinuxRequest implements LinuxRequest
{
	/** Constructor */
	public LinuxSetInterfaceRequest( LinuxRequestFactory factory ) { super(factory); }

	//*************************************************************************
	// Public methods

	/**
	 * Native submit method.
	 */
	public void submitNative() { JavaxUsb.nativeSetInterface( this ); }

	/**
	 * Native abort method.
	 */
	public void abortNative() { /* Submission is synchronous, cannot abort */ }

	/**
	 * Native complete method.
	 */
	public void completeNative() { /* Submission is synchronous, completion handled there */ }

	/**
	 * Get the interface number to change the setting of.
	 * <p>
	 * Note that this returns an unsigned int if the number has been set;
	 * if it has not been set this returns -1.
	 * @return the interface number to change the setting of.
	 */
	public int getInterface() { return interfaceNumber; }

	/**
	 * Get the interface setting to change to.
	 * <p>
	 * Note that this returns an unsigned int if the setting has been set;
	 * if it has not been set this returns -1.
	 * @return the interface setting to change to.
	 */
	public int getSetting() { return interfaceSetting; }

	/**
	 * Set the interface number to change the setting of.
	 * <p>
	 * This is converted to an unsigned int.
	 * @param number the interface number to change the setting of.
	 */
	public void setInterface( byte number ) { interfaceNumber = UsbUtil.unsignedInt(number); }

	/**
	 * Set the interface setting to change to.
	 * <p>
	 * This is converted to an unsigned int.
	 * @param setting the interface setting to change to.
	 */
	public void setSetting( byte setting ) { interfaceSetting = UsbUtil.unsignedInt(setting); }

	//*************************************************************************
	// Recyclable methods

	/** Clean this object */
	public void clean()
	{
		super.clean();
		interfaceNumber = -1;
		interfaceSetting = -1;
	}

	//*************************************************************************
	// Instance variables

	private int interfaceNumber = -1;
	private int interfaceSetting = -1;

}
