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
	public LinuxUsbServices()
	{
		topologyUpdateManager.setMaxSize(Long.MAX_VALUE);
	}

    //*************************************************************************
    // Public methods

    /** @return The virtual USB root hub */
    public synchronized UsbRootHub getUsbRootHub() throws UsbException
	{
		JavaxUsb.loadLibrary(); 

		if (!isListening()) {
			synchronized (topologyLock) {
				startTopologyListener();

				try {
					topologyLock.wait();
				} catch ( InterruptedException iE ) {
					throw new UsbException("Interrupted while enumerating USB devices, try again");
				}
			}
		}

        if ( 0 > totalDevices ) throw new UsbException( COULD_NOT_ACCESS_USB_SUBSYSTEM );

        return JavaxUsb.getRootHub();
	}

	/** @return The minimum API version this supports. */
	public String getApiVersion() { return LINUX_API_VERSION; }

	/** @return The version number of this implementation. */
	public String getImpVersion() { return LINUX_IMP_VERSION; }

	/** @return Get a description of this UsbServices implementation. */
	public String getImpDescription() { return LINUX_IMP_DESCRIPTION; }

    //*************************************************************************
    // Private methods

	/** @return If the topology listener is listening */
	private boolean isListening()
	{
		try { return topologyListener.isAlive(); }
		catch ( NullPointerException npE ) { return false; }
	}

	/** Start Topology Change Listener Thread */
	private void startTopologyListener()
	{
		Runnable r = new Runnable() {
				public void run()
				{ topologyListenerExit(JavaxUsb.nativeTopologyListener(LinuxUsbServices.this)); }
			};

		topologyListener = new Thread(r);

		topologyListener.setDaemon(true);
		topologyListener.setName("javax.usb Linux implementation Topology Listener");
		topologyListener.start();
	}

	/**
	 * Called when the topology listener exits.
	 * @param error The return code of the topology listener.
	 */
	private void topologyListenerExit(int error)
	{
//FIXME - disconnet all devices

		synchronized (topologyLock) {
			topologyLock.notifyAll();
		}
	}

	/** Enqueue an update topology request */
	private void topologyChange()
	{
		Runnable r = new Runnable() {
				public void run()
				{ updateTopology(); }
			};

		topologyUpdateManager.add(r);
	}

	/** Update the topology and fire connect/disconnect events */
	private void updateTopology()
	{
		totalDevices = JavaxUsb.nativeTopologyUpdater( topologyUpdater );

			Enumeration oldKeys = JavaxUsb.getUsbDeviceKeyEnumeration();

			while (oldKeys.hasMoreElements()) {
				String key = (String)oldKeys.nextElement();

				if (allDeviceKeys.contains(key)) continue;

				UsbDeviceImp device = JavaxUsb.getUsbDeviceImp( key );
				JavaxUsb.removeUsbDeviceImp( device );
				JavaxUsb.disconnectUsbDeviceImp( device );
				disconnectedDevices.addUsbInfo( device );
			}

			allDeviceKeys.removeAllElements();
		if ( !connectedDevices.isEmpty() ) {
			UsbInfoList usbInfoList = new DefaultUsbInfoList();
			for (int i=0; i<connectedDevices.size(); i++)
				usbInfoList.addUsbInfo((UsbInfo)connectedDevices.get(i));
			connectedDevices.clear();
			fireUsbDeviceAttachedEvent( usbInfoList );
		}

		if ( !disconnectedDevices.isEmpty() ) {
			UsbInfoList usbInfoList = new DefaultUsbInfoList();
			for (int i=0; i<disconnectedDevices.size(); i++)
				usbInfoList.addUsbInfo((UsbInfo)disconnectedDevices.get(i));
			disconnectedDevices.clear();
			fireUsbDeviceDetachedEvent( usbInfoList );
		}

		synchronized (topologyLock) {
			topologyLock.notifyAll();
		}
	}

	/**
	 * Check if a UsbDeviceImp already exists.
	 * @param device The UsbDeviceImp to check.
	 * @return The new UsbDeviceImp or existing UsbDeviceImp.
	 */
	private UsbDeviceImp checkUsbDeviceImp(UsbDeviceImp device)
	{
		allDeviceKeys.addElement( key );
		UsbDeviceImp oldDevice = JavaxUsb.getUsbDeviceImp( key );
		if ( null != oldDevice) return oldDevice;

		connectedDevices.addUsbInfo( device );
		JavaxUsb.addUsbDeviceImp( device, key );
		return device;		
	}

    /**
     * Fires UsbServicesEvent to all listeners on getTopologyHelper()
	 * @param usbDevices the attached devices
     */
    private void fireUsbDeviceAttachedEvent( UsbInfoList usbDevices )
	{
		UsbServicesEvent event = new UsbServicesEvent( this, usbDevices );
        fireDeviceAttachedEvent( event );
	}

    /**
     * Fires UsbServicesEvent to all listeners on getTopologyHelper()
	 * @param usbDevices the detached devices
     */
    private void fireUsbDeviceDetachedEvent( UsbInfoList usbDevices )
	{
		UsbServicesEvent event = new UsbServicesEvent( this, usbDevices );
        fireDeviceDetachedEvent( event );
	}

    //*************************************************************************
    // Instance variables

	private RunnableManager topologyUpdateManager = new RunnableManager();

	private List connectedDevices = new ArrayList();
	private List disconnectedDevices = new ArrayList();

	private Thread topologyListener = null;
	private Object topologyLock = new Object();

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
		private UsbDeviceImp addUsbDeviceImp( UsbDeviceImp device, String key )
		{
		}

		/**
		 * Update the topology by comparing the device table to the newly created vector and
		 * removing any disconnected devices
		 */
		private void updateTopology()
		{
		}

		private Vector allDeviceKeys = new Vector();

	}

}


