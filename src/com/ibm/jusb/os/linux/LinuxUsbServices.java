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
import javax.usb.util.UsbInfoList;
import javax.usb.util.DefaultUsbInfoList;

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * Basic set of Linux native services
 * @author E. Michael Maximilien
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
public class LinuxUsbServices extends AbstractUsbServices implements UsbServices, UsbTopologyServices
{

    public LinuxUsbServices()
	{
		linuxUsbServices = this;
	}

    //*************************************************************************
    // Public methods

    /**
     * Accepts a DescriptorVisitor objects
     * @param visitor the OSServicesVisitor object
     */
    public void accept( UsbServicesVisitor visitor ) { visitor.visitLinuxUsbServices( this ); }

	/** @return the AbstractUsbServices.AbstractHelper object */
	public AbstractHelper getHelper() { return helper; }

	/** @return the LinuxUsbServices.LinuxHelper object */
	public LinuxHelper getLinuxHelper() { return helper; }

	/** @deprecated use getUsbRootHub() */
    public synchronized UsbHub getRootUsbHub() throws UsbException { return getUsbRootHub(); }

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

        if ( 0 > totalDevices ) throw new UsbException( LinuxUsbConst.COULD_NOT_ACCESS_USB_SUBSYSTEM );
        if ( 0 == totalDevices ) throw new UsbException( LinuxUsbConst.NO_USB_DEVICES_FOUND );

        return JavaxUsb.getRootHub();
	}

	/**
	 * Get the (minimum) version number of the javax.usb API
	 * that this UsbServices implements.
	 * <p>
	 * This should correspond to the output of (some version of) the
	 * {@link javax.usb.Version#getApiVersion() javax.usb.Version}.
	 */
	public String getApiVersion() { return LinuxUsbConst.API_VERSION; }

	/**
	 * Get the version number of the UsbServices implementation.
	 * <p>
	 * The format should be <major>.<minor>.<revision>
	 */
	public String getImpVersion() { return LinuxUsbConst.IMP_VERSION; }

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
	public String getImpDescription() { return LinuxUsbConst.IMP_DESCRIPTION; }

    //*************************************************************************
    // Package methods

    /**
     * Get current instance of this
     * @return The current UsbServices instance
     */
    static LinuxUsbServices getLinuxInstance()
	{
        return linuxUsbServices;
	}

    //*************************************************************************
    // Private methods

	/** Start Topology Change Listener Thread */
	private void startTopologyListener()
	{
		topologyListener.thread = new Thread( topologyListenerRunnable ); 

		topologyChangeScheduler.start();

		topologyListener.thread.setDaemon( true );
		topologyListener.thread.setName( "javax.usb Topology Listener Thread" );
		topologyListener.thread.start();
	}

	/** Enqueue an update topology request */
	private void topologyChange()
	{
		Task update = new Task() {
			public void execute()
			{ LinuxUsbServices.this.updateTopology(); }
		};

		topologyChangeScheduler.start();
		topologyChangeScheduler.resume();
		topologyChangeScheduler.post( update );
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

	private TaskScheduler topologyChangeScheduler = new FifoScheduler();

    private int totalDevices = 0;

    private UsbPipeImpFactory usbPipeImpFactory = new LinuxPipeImpFactory();
	private UsbInfoImpFactory usbInfoImpFactory = new LinuxInfoImpFactory();

	private LinuxRequestFactory linuxPipeRequestFactory = new LinuxRequestFactory() {
		public Recyclable createRecyclable() { return new LinuxPipeRequest( LinuxUsbServices.this.linuxPipeRequestFactory ); }
	};
	private LinuxRequestFactory linuxIsochronousRequestFactory = new LinuxRequestFactory() {
		public Recyclable createRecyclable() { return new LinuxIsochronousRequest( LinuxUsbServices.this.linuxIsochronousRequestFactory ); }
	};
	private LinuxRequestFactory linuxIsochronousCompositeRequestFactory = new LinuxRequestFactory() {
		public Recyclable createRecyclable() { return new LinuxIsochronousCompositeRequest( LinuxUsbServices.this.linuxIsochronousCompositeRequestFactory ); }
	};
	private LinuxRequestFactory linuxInterfaceRequestFactory = new LinuxRequestFactory() {
		public Recyclable createRecyclable() { return new LinuxInterfaceRequest( LinuxUsbServices.this.linuxInterfaceRequestFactory ); }
	};
	private LinuxRequestFactory linuxDcpRequestFactory = new LinuxRequestFactory() {
		public Recyclable createRecyclable() { return new LinuxDcpRequest( LinuxUsbServices.this.linuxDcpRequestFactory ); }
	};
	private LinuxRequestFactory linuxSetConfigurationRequestFactory = new LinuxRequestFactory() {
		public Recyclable createRecyclable() { return new LinuxSetConfigurationRequest( LinuxUsbServices.this.linuxSetConfigurationRequestFactory ); }
	};
	private LinuxRequestFactory linuxSetInterfaceRequestFactory = new LinuxRequestFactory() {
		public Recyclable createRecyclable() { return new LinuxSetInterfaceRequest( LinuxUsbServices.this.linuxSetInterfaceRequestFactory ); }
	};

	private MethodHandlerFactory methodHandlerFactory = new MethodHandlerFactory();

    private LinuxHelper helper = new LinuxHelper();
            
    //*************************************************************************
    // Class variables

    private static LinuxUsbServices linuxUsbServices = null;

	//*************************************************************************
	// Inner interfaces

	/**
	 * Helper class
	 */
	public class LinuxHelper extends AbstractHelper
	{
		/** @return a UsbPipeImpFactory instance */
		public UsbPipeImpFactory getUsbPipeImpFactory() { return LinuxUsbServices.this.usbPipeImpFactory; }

		/** @return a UsbInfoImpFactory instance */
		public UsbInfoImpFactory getUsbInfoImpFactory() { return LinuxUsbServices.this.usbInfoImpFactory; }

		/** @return a LinuxRequestFactory instance for LinuxPipeRequests */
		public LinuxRequestFactory getLinuxPipeRequestFactory() { return LinuxUsbServices.this.linuxPipeRequestFactory; }

		/** @return a LinuxRequestFactory instance for LinuxIsochronousRequests */
		public LinuxRequestFactory getLinuxIsochronousRequestFactory() { return LinuxUsbServices.this.linuxIsochronousRequestFactory; }

		/** @return a LinuxRequestFactory instance for LinuxIsochronousCompositeRequests */
		public LinuxRequestFactory getLinuxIsochronousCompositeRequestFactory() { return LinuxUsbServices.this.linuxIsochronousCompositeRequestFactory; }

		/** @return a LinuxRequestFactory instance for LinuxInterfaceRequests */
		public LinuxRequestFactory getLinuxInterfaceRequestFactory() { return LinuxUsbServices.this.linuxInterfaceRequestFactory; }

		/** @return a LinuxRequestFactory instance for LinuxDcpRequests */
		public LinuxRequestFactory getLinuxDcpRequestFactory() { return LinuxUsbServices.this.linuxDcpRequestFactory; }

		/** @return a LinuxRequestFactory instance for LinuxSetConfigurationRequests */
		public LinuxRequestFactory getLinuxSetConfigurationRequestFactory() { return LinuxUsbServices.this.linuxSetConfigurationRequestFactory; }

		/** @return a LinuxRequestFactory instance for LinuxSetInterfaceRequests */
		public LinuxRequestFactory getLinuxSetInterfaceRequestFactory() { return LinuxUsbServices.this.linuxSetInterfaceRequestFactory; }

		/** @return a MethodHandlerFactory instance */
		public MethodHandlerFactory getMethodHandlerFactory() { return LinuxUsbServices.this.methodHandlerFactory; }
	}

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
			allDeviceKeys.addElement( key );
			UsbDevice oldDevice = JavaxUsb.getUsbDevice( key );
			if ( null != oldDevice) return oldDevice;

			connectedDevices.addUsbInfo( device );
			JavaxUsb.addUsbDevice( device, key );
			return device;
		}

		/**
		 * Update the topology by comparing the device table to the newly created vector and
		 * removing any disconnected devices
		 */
		private void updateTopology()
		{
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
		}

		public UsbInfoList getConnectedDevices() { return connectedDevices; }
		public UsbInfoList getDisconnectedDevices() { return disconnectedDevices; }

		private Vector allDeviceKeys = new Vector();
		private UsbInfoList connectedDevices = new DefaultUsbInfoList();
		private UsbInfoList disconnectedDevices = new DefaultUsbInfoList();

	}

}


