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
 * Factory to generate LinuxRequests
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
abstract class LinuxRequestFactory extends RecycleFactory
{
	/** Constructor */
	public LinuxRequestFactory() { }

	//*************************************************************************
	// Public methods

	/** @return a Recyclable */
	public Recyclable take() { return takeLinuxRequest(); }

	/** @return a 'clean' LinuxRequest ready for use */
	public LinuxRequest takeLinuxRequest()
	{
		LinuxRequest request = (LinuxRequest)super.take();

		request.setActive( true );

		return request;
	}

	/** @param the LinuxRequest that is done with (which will be recycled) */
	public void returnLinuxRequest( LinuxRequest request )
	{
		request.setActive( false );

		super.recycle( request );
	}

}
