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

        if ( 0 != topologyListenerError ) throw new UsbException( COULD_NOT_ACCESS_USB_SUBSYSTEM, topologyListenerError );

		if ( 0 != topologyUpdateResult ) throw new UsbException( COULD_NOT_ACCESS_USB_SUBSYSTEM, topologyUpdateResult );

		return getUsbRootHubImp();
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

		topologyListenerError = 0;
		topologyListener.start();
	}

	/**
	 * Called when the topology listener exits.
	 * @param error The return code of the topology listener.
	 */
	private void topologyListenerExit(int error)
	{
//FIXME - disconnet all devices

		topologyListenerError = error;

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

	/**
	 * Fill the List with all devices.
	 * @param device The device to add.
	 * @param list The list to add to.
	 */
	private void fillDeviceList( UsbDeviceImp device, List list )
	{
		list.add(device);

		if (device.isUsbHub()) {
			UsbHubImp hub = (UsbHubImp)device;

			UsbInfoListIterator iterator = hub.getAttachedUsbDevices();
			while (iterator.hasNext())
				fillDeviceList( (UsbDeviceImp)iterator.nextUsbInfo(), list );
		}
	}

	/** Update the topology and fire connect/disconnect events */
	private void updateTopology()
	{
		List connectedDevices = new ArrayList();
		List disconnectedDevices = new ArrayList();

		fillDeviceList(getUsbRootHubImp(), disconnectedDevices);
		disconnectedDevices.remove(getUsbRootHubImp());

		topologyUpdateResult = JavaxUsb.nativeTopologyUpdater( this, connectedDevices, disconnectedDevices );

		for (int i=0; i<disconnectedDevices.size(); i++)
			((UsbDeviceImp)disconnectedDevices.get(i)).disconnect();

		for (int i=0; i<connectedDevices.size(); i++) {
			UsbDeviceImp device = (UsbDeviceImp)connectedDevices.get(i);
			device.getUsbPortImp().attachUsbDeviceImp(device);
		}

		if ( !disconnectedDevices.isEmpty() ) {
			UsbInfoList usbInfoList = new DefaultUsbInfoList();
			for (int i=0; i<disconnectedDevices.size(); i++)
				usbInfoList.addUsbInfo((UsbInfo)disconnectedDevices.get(i));
			fireUsbDeviceDetachedEvent( usbInfoList );
		}

		if ( !connectedDevices.isEmpty() ) {
			UsbInfoList usbInfoList = new DefaultUsbInfoList();
			for (int i=0; i<connectedDevices.size(); i++)
				usbInfoList.addUsbInfo((UsbInfo)connectedDevices.get(i));
			fireUsbDeviceAttachedEvent( usbInfoList );
		}

		synchronized (topologyLock) {
			topologyLock.notifyAll();
		}
	}

	/**
	 * Check a device.
	 * <p>
	 * If the device exists, the existing device is removed from the disconnected list and returned.
	 * If the device is new, it is added to the connected list and returned.  If the new device replaces
	 * an existing device, the old device is retained in the disconnected list, and the new device is returned.
	 * @param hub The parent UsbHubImp.
	 * @param p The parent port number.
	 * @param device The UsbDeviceImp to add.
	 * @param disconnected The List of disconnected devices.
	 * @param connected The List of connected devices.
	 * @return The new UsbDeviceImp or existing UsbDeviceImp.
	 */
	private UsbDeviceImp checkUsbDeviceImp( UsbHubImp hub, int p, UsbDeviceImp device, List connected, List disconnected )
	{
		UsbPortImp usbPortImp = null;
		byte port = (byte)p;

		try {
			usbPortImp = hub.getUsbPortImp(port);
		} catch ( UsbRuntimeException urE ) {
			hub.resize(port);
			usbPortImp = hub.getUsbPortImp(port);
		}

		if (!usbPortImp.isUsbDeviceAttached()) {
			connected.add(device);
			device.setUsbPortImp(usbPortImp);
			return device;
		}

		UsbDeviceImp existingDevice = usbPortImp.getUsbDeviceImp();

		if (existingDevice.equals(device)) {
			disconnected.remove(existingDevice);
			return existingDevice;
		} else {
			connected.add(device);
			device.setUsbPortImp(usbPortImp);
			return device;
		}
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

	private Thread topologyListener = null;
	private Object topologyLock = new Object();

    private int topologyListenerError = 0;
	private int topologyUpdateResult = 0;

	//*************************************************************************
	// Class constants

    public static final String COULD_NOT_ACCESS_USB_SUBSYSTEM = "Could not access USB subsystem.";

	public static final String LINUX_API_VERSION = com.ibm.jusb.Version.getApiVersion();
	public static final String LINUX_IMP_VERSION = "0.9.3";
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

}
