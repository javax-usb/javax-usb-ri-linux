package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import java.util.*;

import javax.usb.*;
import javax.usb.os.*;
import javax.usb.event.*;
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;
import com.ibm.jusb.util.*;

/**
 * UsbServices implementation for Linux platform.
 * @author Dan Streetman
 */
public class LinuxUsbServices extends AbstractUsbServices implements UsbServices
{
    //*************************************************************************
    // Public methods

    /**
	 * Get the root hub
	 * <p>
     * The root hub contains all the correct USB devices currently connected
     * the the hub including all USB devices and other hubs.
     * @return the root USB hub on this system
     * @exception javax.usb.UsbException in case something goes wrong trying to get the root hub
     */
    public synchronized UsbRootHub getUsbRootHub() throws UsbException
	{
		JavaxUsb.loadLibrary(); 

		if (!topologyListener.isListening())
			startTopologyListener();

//HACK - WAIT FOR LISTENER TO STARTUP
try { Thread.currentThread().sleep( 1000 ); }
catch ( InterruptedException iE ) { }

		if (0 >= totalDevices)
			totalDevices = JavaxUsb.nativeTopologyUpdater( topologyUpdater );

        if ( 0 > totalDevices ) throw new UsbException( COULD_NOT_ACCESS_USB_SUBSYSTEM );
        if ( 0 == totalDevices ) throw new UsbException( NO_USB_DEVICES_FOUND );

        return JavaxUsb.getRootHub();
	}

	/**
	 * Get the (minimum) version number of the javax.usb API
	 * that this UsbServices implements.
	 * <p>
	 * This should correspond to the output of (some version of) the
	 * {@link javax.usb.Version#getApiVersion() javax.usb.Version}.
	 */
	public String getApiVersion() { return LINUX_API_VERSION; }

	/**
	 * Get the version number of the UsbServices implementation.
	 * <p>
	 * The format should be <major>.<minor>.<revision>
	 */
	public String getImpVersion() { return LINUX_IMP_VERSION; }

	/**
	 * Get a description of this UsbServices implementation.
	 * <p>
	 * The format is implementation-specific, but should include at least
	 * the following:
	 * <ul>
	 * <li>The company or individual author(s).</li>
	 * <li>The license, or license header.</li>
	 * <li>Contact information.</li>
	 * <li>The minimum or expected version of Java.</li>
	 * <li>The Operating System(s) supported (usually one per implementation).</li>
	 * <li>Any other useful information.</li>
	 * </ul>
	 */
	public String getImpDescription() { return LINUX_IMP_DESCRIPTION; }

    //*************************************************************************
    // Private methods

	/** Start Topology Change Listener Thread */
	private void startTopologyListener()
	{
		topologyListener.thread = new Thread( topologyListenerRunnable ); 

		topologyListener.thread.setDaemon( true );
		topologyListener.thread.setName( "javax.usb Topology Listener Thread" );
		topologyListener.thread.start();
	}

	/** Enqueue an update topology request */
	private void topologyChange()
	{
		Runnable r = new Runnable() {
			public void run()
			{ LinuxUsbServices.this.updateTopology(); }
		};

		Thread t = new Thread(r);

		t.setDaemon(true);
		t.start();
	}

	/** Update the topology and fire connect/disconnect events */
	private void updateTopology()
	{
		totalDevices = JavaxUsb.nativeTopologyUpdater( topologyUpdater );

		UsbInfoList connectedDevices = topologyUpdater.getConnectedDevices();
		UsbInfoList disconnectedDevices = topologyUpdater.getDisconnectedDevices();

		if ( !connectedDevices.isEmpty() ) {
			fireUsbDeviceAttachedEvent( connectedDevices.copy() );
			connectedDevices.clear();
		}

		if ( !disconnectedDevices.isEmpty() ) {
			fireUsbDeviceDetachedEvent( disconnectedDevices.copy() );
			disconnectedDevices.clear();
		}
	}

    /**
     * Fires UsbServicesEvent to all listeners on getTopologyHelper()
	 * @param usbDevices the attached devices
     */
    private void fireUsbDeviceAttachedEvent( UsbInfoList usbDevices )
	{
		UsbServicesEvent event = new UsbServicesEvent( this, usbDevices );
        getTopologyHelper().fireUsbDeviceAttachedEvent( event );
	}

    /**
     * Fires UsbServicesEvent to all listeners on getTopologyHelper()
	 * @param usbDevices the detached devices
     */
    private void fireUsbDeviceDetachedEvent( UsbInfoList usbDevices )
	{
		UsbServicesEvent event = new UsbServicesEvent( this, usbDevices );
        getTopologyHelper().fireUsbDeviceDetachedEvent( event );
	}

    //*************************************************************************
    // Instance variables

	private LinuxTopologyListener topologyListener = new LinuxTopologyListener() {
			public void topologyChange() { LinuxUsbServices.this.topologyChange(); }
		};


	private LinuxTopologyUpdater topologyUpdater = new LinuxTopologyUpdater();

    private Runnable topologyListenerRunnable = new Runnable() {
        public void run() { JavaxUsb.nativeTopologyListener( LinuxUsbServices.this.topologyListener ); }
	};

    private int totalDevices = 0;

	//*************************************************************************
	// Class constants

    public static final String COULD_NOT_ACCESS_USB_SUBSYSTEM = "Could not access USB subsystem.";
    public static final String NO_USB_DEVICES_FOUND = "No USB devices found.";

	public static final String LINUX_API_VERSION = "0.9.1";
	public static final String LINUX_IMP_VERSION = "0.9.1";
	public static final String LINUX_IMP_DESCRIPTION =
		 "\t"+"JSR80 : javax.usb"
		+"\n"
		+"\n"+"Implementation for the Linux kernel (2.4.x).\n"
		+"\n"
		+"\n"+"*"
		+"\n"+"* Copyright (c) 1999 - 2001, International Business Machines Corporation."
		+"\n"+"* All Rights Reserved."
		+"\n"+"*"
		+"\n"+"* This software is provided and licensed under the terms and conditions"
		+"\n"+"* of the Common Public License:"
		+"\n"+"* http://oss.software.ibm.com/developerworks/opensource/license-cpl.html"
		+"\n"
		+"\n"+"http://javax-usb.org/"
		+"\n"+"\n"
		;


	//*************************************************************************
	// Inner interfaces

	/**
	 * Linux topology listener, should be passed to native code which will callback the methods
	 * @author Dan Streetman
	 * @vesion 0.0.1 (JDK 1.1.x)
	 */
	public class LinuxTopologyListener
	{

		/** A topology change has occured */
		public void topologyChange() { }

		/** Check if this listener is listening */
        public boolean isListening() { return (listening && thread.isAlive()); }

		/** Set if this is listening */
		public void setListening( boolean listen ) { listening = listen; }

		public Thread thread = null;
		private boolean listening = false;
	}

	/**
	 * Linux topology updater
	 * @author Dan Streetman
	 * @version 0.0.1 (JDK 1.1.x)
	 */
	public class LinuxTopologyUpdater
	{

		/**
		 * Try to add a UsbDevice
		 * @param device the UsbDevice to all
		 * @param key the device's key
		 * @return the device already in the topology, or the newly added device (param 1)
		 */
		private UsbDevice addUsbDevice( UsbDevice device, String key )
		{
/*
			allDeviceKeys.addElement( key );
			UsbDevice oldDevice = JavaxUsb.getUsbDevice( key );
			if ( null != oldDevice) return oldDevice;

			connectedDevices.addUsbInfo( device );
			JavaxUsb.addUsbDevice( device, key );
			return device;
*/return null;
		}

		/**
		 * Update the topology by comparing the device table to the newly created vector and
		 * removing any disconnected devices
		 */
		private void updateTopology()
		{
/*
			Enumeration oldKeys = JavaxUsb.getUsbDeviceKeyEnumeration();

			while (oldKeys.hasMoreElements()) {
				String key = (String)oldKeys.nextElement();

				if (allDeviceKeys.contains(key)) continue;

				UsbDevice device = JavaxUsb.getUsbDevice( key );
				JavaxUsb.removeUsbDevice( device );
				JavaxUsb.disconnectUsbDevice( device );
				disconnectedDevices.addUsbInfo( device );
			}

			allDeviceKeys.removeAllElements();
*/
		}

		public UsbInfoList getConnectedDevices() { return connectedDevices; }
		public UsbInfoList getDisconnectedDevices() { return disconnectedDevices; }

		private Vector allDeviceKeys = new Vector();
		private UsbInfoList connectedDevices = new DefaultUsbInfoList();
		private UsbInfoList disconnectedDevices = new DefaultUsbInfoList();

	}

}


