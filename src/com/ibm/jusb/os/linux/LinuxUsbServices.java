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

		checkProperties();
	}

    //*************************************************************************
    // Public methods

	/** Main method to print out version information */
	public static void main(String[] argv)
	{
		System.out.println("javax.usb Linux implementation version " + LINUX_IMP_VERSION);
		System.out.println("javax.usb Required Platform-Implementation version " + LINUX_API_VERSION + " (or later)");
		System.out.println(LINUX_IMP_DESCRIPTION);
	}

    /** @return The virtual USB root hub */
    public synchronized UsbHub getRootUsbHub() throws UsbException
	{
		JavaxUsb.loadLibrary(); 

		synchronized (topologyLock) {
			if (!isListening()) {
				startTopologyListener();

				try {
					topologyLock.wait();
				} catch ( InterruptedException iE ) {
					throw new UsbException("Interrupted while enumerating USB devices, try again");
				}
			}
		}

        if ( 0 != topologyListenerError ) throw new UsbException( COULD_NOT_ACCESS_USB_SUBSYSTEM + " : " + topologyListenerError );

		if ( 0 != topologyUpdateResult ) throw new UsbException( COULD_NOT_ACCESS_USB_SUBSYSTEM + " : " + topologyUpdateResult );

		return getRootUsbHubImp();
	}

	/** @return The minimum API version this supports. */
	public String getApiVersion() { return LINUX_API_VERSION; }

	/** @return The version number of this implementation. */
	public String getImpVersion() { return LINUX_IMP_VERSION; }

	/** @return Get a description of this UsbServices implementation. */
	public String getImpDescription() { return LINUX_IMP_DESCRIPTION; }

    //*************************************************************************
    // Private methods

	/** Set variables from user-specified properties */
	private void checkProperties()
	{
		Properties p = null;

		try { p = UsbHostManager.getProperties(); } catch ( Exception e ) { return; }

		try {
			if (p.containsKey(TOPOLOGY_UPDATE_DELAY_KEY))
				topologyUpdateDelay = Integer.decode(p.getProperty(TOPOLOGY_UPDATE_DELAY_KEY)).intValue();
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TOPOLOGY_UPDATE_USE_POLLING_KEY))
				topologyUpdateUsePolling = Boolean.valueOf(p.getProperty(TOPOLOGY_UPDATE_USE_POLLING_KEY)).booleanValue();
		} catch ( Exception e ) { }
	}

	/** @return If the topology listener is listening */
	private boolean isListening()
	{
		try { return topologyListener.isAlive(); }
		catch ( NullPointerException npE ) { return false; }
	}

	/** Start Topology Change Listener Thread */
	private void startTopologyListener()
	{
		Runnable r = null;
		String threadName = null;

		if (topologyUpdateUsePolling) {
			threadName = "javax.usb Linux implementation Topology Poller";
			r = new Runnable() {
					public void run()
					{
						while (true) {
							try { Thread.sleep(topologyUpdateDelay); } catch ( InterruptedException iE ) { }
							updateTopology();
						}
					}
				};
		} else {
			threadName = "javax.usb Linux implementation Topology Listener";
			r = new Runnable() {
					public void run()
					{ topologyListenerExit(JavaxUsb.nativeTopologyListener(LinuxUsbServices.this)); }
				};
		}

		topologyListener = new Thread(r);

		topologyListener.setDaemon(true);
		topologyListener.setName(threadName);

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
		try { Thread.sleep(topologyUpdateDelay); } catch ( InterruptedException iE ) { }

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

//FIXME - Iterators can throw ConcurrentModificationException!
			Iterator iterator = hub.getAttachedUsbDevices().iterator();
			while (iterator.hasNext())
				fillDeviceList( (UsbDeviceImp)iterator.next(), list );
		}
	}

	/** Update the topology and fire connect/disconnect events */
	private void updateTopology()
	{
		List connectedDevices = new ArrayList();
		List disconnectedDevices = new ArrayList();

		fillDeviceList(getRootUsbHubImp(), disconnectedDevices);
		disconnectedDevices.remove(getRootUsbHubImp());

		topologyUpdateResult = JavaxUsb.nativeTopologyUpdater( this, connectedDevices, disconnectedDevices );

		for (int i=0; i<disconnectedDevices.size(); i++)
			((UsbDeviceImp)disconnectedDevices.get(i)).disconnect();

		for (int i=0; i<connectedDevices.size(); i++) {
			UsbDeviceImp device = (UsbDeviceImp)connectedDevices.get(i);
			device.getParentUsbPortImp().attachUsbDeviceImp(device);
		}

		for (int i=0; i<disconnectedDevices.size(); i++)
			listenerImp.usbDeviceDetached(new UsbServicesEvent(this, (UsbDevice)disconnectedDevices.get(i)));

		for (int i=0; i<connectedDevices.size(); i++)
			listenerImp.usbDeviceAttached(new UsbServicesEvent(this, (UsbDevice)connectedDevices.get(i)));

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
		byte port = (byte)p;
		UsbPortImp usbPortImp = hub.getUsbPortImp(port);

		if (null == usbPortImp) {
			hub.resize(port);
			usbPortImp = hub.getUsbPortImp(port);
		}

		if (!usbPortImp.isUsbDeviceAttached()) {
			connected.add(device);
			device.setParentUsbPortImp(usbPortImp);
			return device;
		}

		UsbDeviceImp existingDevice = usbPortImp.getUsbDeviceImp();

		if (isUsbDevicesEqual(existingDevice, device)) {
			disconnected.remove(existingDevice);
			return existingDevice;
		} else {
			connected.add(device);
			device.setParentUsbPortImp(usbPortImp);
			return device;
		}
	}

	/**
	 * Return if the specified devices appear to be equal.
	 * <p>
	 * If either of the device's descriptors are null, this returns false.
	 * @param dev1 The first device.
	 * @param dev2 The second device.
	 * @return If the devices appear to be equal.
	 */
	protected boolean isUsbDevicesEqual(UsbDeviceImp dev1, UsbDeviceImp dev2)
	{
		try {
			UsbDeviceDescriptor desc1 = dev1.getUsbDeviceDescriptor();
			UsbDeviceDescriptor desc2 = dev2.getUsbDeviceDescriptor();

			return
				(dev1.isUsbHub() == dev1.isUsbHub()) &&
				dev1.getSpeed() == dev2.getSpeed() &&
				desc1.equals(desc2);
		} catch ( NullPointerException npE ) {
			return false;
		}
	}

    //*************************************************************************
    // Instance variables

	private RunnableManager topologyUpdateManager = new RunnableManager();

	private Thread topologyListener = null;
	private Object topologyLock = new Object();

    private int topologyListenerError = 0;
	private int topologyUpdateResult = 0;

	protected boolean topologyUpdateUsePolling = TOPOLOGY_UPDATE_USE_POLLING;
	protected int topologyUpdateDelay = TOPOLOGY_UPDATE_DELAY;

	//*************************************************************************
	// Class constants

	/* If not polling, this is the delay in ms after getting a connect/disconnect notification
	 * before checking for device updates.  If polling, this is the number of ms between polls.
	 */
	public static final int TOPOLOGY_UPDATE_DELAY = 1000; /* 1 second */
	public static final String TOPOLOGY_UPDATE_DELAY_KEY = "com.ibm.jusb.os.linux.LinuxUsbServices.topologyUpdateDelay";

	/* Whether to use polling to wait for connect/disconnect notification */
	public static final boolean TOPOLOGY_UPDATE_USE_POLLING = false;
	public static final String TOPOLOGY_UPDATE_USE_POLLING_KEY = "com.ibm.jusb.os.linux.LinuxUsbServices.topologyUpdateUsePolling";

    public static final String COULD_NOT_ACCESS_USB_SUBSYSTEM = "Could not access USB subsystem.";

	public static final String LINUX_API_VERSION = "0.10.1";
	public static final String LINUX_IMP_VERSION = "0.10.4-CVS";
	public static final String LINUX_IMP_DESCRIPTION =
		 "JSR80 : javax.usb"
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
		;

}
