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
 * Linux constants
 * @author Dan Streetman
 * @author E. Michael Maximilien
 */
interface LinuxUsbConst
{

    //*************************************************************************
    // Class constants

	/**
	 * This is the name of the native shared library.
	 * <p>
	 * The full name of this will be 'libJavaxUsb.so'
	 */
    public static final String SHARED_LIBRARY_NAME = "JavaxUsb";

	/**
	 * This is the version number of the API this implements.
	 */
	public static final String API_VERSION = "1.0.1";

	/**
	 * This is the version number of this implementation.
	 */
	public static final String IMP_VERSION = "1.0.0";

    public static final String COULD_NOT_ACCESS_USB_SUBSYSTEM = "Could not access USB subsystem.";
    public static final String NO_USB_DEVICES_FOUND = "No USB devices found.";
    public static final String ERROR_WHILE_LOADING_SHARED_LIBRARY = "Error while loading shared library";
    public static final String EXCEPTION_WHILE_LOADING_SHARED_LIBRARY = "Exception while loading shared library";

	public static final String TAB = "\t";
	public static final String NL = "\n";

	public static final String IMP_DESCRIPTION =
		 TAB+"javax.usb for Java 2 (J2SE 1.3)"
		+NL
		+NL+"Implementation for the Linux kernel (2.4.x).\n"
		+NL
		+NL+"*"
		+NL+"* Copyright (c) 1999 - 2001, International Business Machines Corporation."
		+NL+"* All Rights Reserved."
		+NL+"*"
		+NL+"* This software is provided and licensed under the terms and conditions"
		+NL+"* of the Common Public License:"
		+NL+"* http://oss.software.ibm.com/developerworks/opensource/license-cpl.html"
		+NL
		+NL+"E. M. Maximilien <maxim@us.ibm.com>"
		+NL+"Dan Streetman <ddstreet@us.ibm.com>"
		+NL
		+NL+"http://oss.software.ibm.com/developerworks/projects/javaxusb/"
		+NL+NL
		;

	public static final String VIRTUAL_ROOT_HUB_MANUFACTURER = "IBM Corporation Linux javax.usb implementation version " + IMP_VERSION;
	public static final String VIRTUAL_ROOT_HUB_PRODUCT = "IBM Corporation javax.usb Virtual Root Hub";
	public static final String VIRTUAL_ROOT_HUB_SERIALNUMBER = "19741113";

}
