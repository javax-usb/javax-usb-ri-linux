package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import com.ibm.jusb.util.*;

/**
 * Factory to manage LinuxRequests.
 * @author Dan Streetman
 */
abstract class LinuxRequestFactory
{
	/** Constructor */
	public LinuxRequestFactory() { }

	//*************************************************************************
	// Public methods

	/** @return a 'clean' LinuxRequest ready for use */
	public LinuxRequest takeLinuxRequest()
	{
return null;
	}

	/** @param the LinuxRequest that is done with (which will be recycled) */
	public void returnLinuxRequest( LinuxRequest request )
	{
//FIXME
	}

}
