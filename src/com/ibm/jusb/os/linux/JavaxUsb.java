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
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;
import com.ibm.jusb.util.*;

/**
 * Provides package visible native methods
 * Provides private static methods for native code to call
 * @author Dan Streetman
 * @version 0.0.1 (JDK 1.1.x)
 */
class JavaxUsb {

	//*************************************************************************
	// Public methods

	/** Load native library */
	public static void loadLibrary() throws UsbException
	{
		if ( libraryLoaded ) return;
		try { System.loadLibrary( LIBRARY_NAME ); }
		catch ( Exception e ) { throw new UsbException( EXCEPTION_WHILE_LOADING_SHARED_LIBRARY + " " + System.mapLibraryName( LIBRARY_NAME ) + " : " + e.getMessage() ); }
		catch ( Error e ) { throw new UsbException( ERROR_WHILE_LOADING_SHARED_LIBRARY + " " + System.mapLibraryName( LIBRARY_NAME ) + " : " + e.getMessage() ); }

//FIXME - change to real tracing
		msgLevelTable.put( MSG_CRITICAL, new Integer(0) );
		msgLevelTable.put( MSG_ERROR, new Integer(1) );
		msgLevelTable.put( MSG_WARNING, new Integer(2) );
		msgLevelTable.put( MSG_NOTICE, new Integer(3) );
		msgLevelTable.put( MSG_INFO, new Integer(4) );
		msgLevelTable.put( MSG_DEBUG1, new Integer(5) );
		msgLevelTable.put( MSG_DEBUG2, new Integer(6) );
		msgLevelTable.put( MSG_DEBUG3, new Integer(7) );

		UsbProperties usbProperties = UsbHostManager.getInstance().getUsbProperties();
		if ( usbProperties.isPropertyDefined( MSG_ENV_NAME ) )
			setMsgLevel( usbProperties.getPropertyString( MSG_ENV_NAME ) );
	}

	/**
	 * @param level the message level to use
	 */
	public static void setMsgLevel( String level ) throws UsbException
	{
		if ( null != level && null != msgLevelTable.get( level ) )
			nativeSetMsgLevel( ((Integer)msgLevelTable.get( level )).intValue() );
		else
			throw new UsbException( INVALID_MSG_LEVEL + " : " + level );
	}

	/** Get the virtual root hub */
	public static UsbRootHub getRootHub() { return rootHub; }

	//*************************************************************************
	// Native methods

	/**
	 * @param level the new msg level to use
	 */
	private static native void nativeSetMsgLevel( int level );

		//*********************************
		// JavaxUsbTopologyUpdater methods

	/**
	 * Call the native function that updates the topology
	 * @param updater A LinuxUsbServices.LinuxTopologyUpdater object
	 * @return The number of devices connected (positive) / disconnected (negative)
	 */
	static synchronized native int nativeTopologyUpdater( LinuxUsbServices.LinuxTopologyUpdater updater );

		//*********************************
		// JavaxUsbTopologyListener methods

	/**
	 * Call the native function that listens for topology changes
	 * @param listener A topology listener whose methods will be called on events
	 */
	static native void nativeTopologyListener( LinuxUsbServices.LinuxTopologyListener listener );

		//*********************************
		// JavaxUsbDeviceProxy methods

	/**
	 * Start a LinuxDeviceProxy
	 * @param io A LinuxInterfaceIO object
	 */
	static native void nativeDeviceProxy( LinuxDeviceProxy proxy );

	/**
	 * @param pid the process ID to signal
	 * @param sig the signal to use
	 */
	static native void nativeSignalPID( int pid, int sig );

		//*********************************
		// JavaxUsbError methods

	/**
	 * @param error the error number
	 * @return if the specified error is serious (continued error condition)
	 */
	static native boolean nativeIsErrorSerious( int error );

	/**
	 * @param error the error number
	 * @return the message associated with the specified error number
	 */
	static native String nativeGetErrorMessage( int error );

		//*********************************
		// JavaxUsbInterfaceRequest methods

	/**
	 * Claim or release an interface.
	 * @param request the LinuxInterfaceRequest
	 */
	static native void nativeSubmitInterfaceRequest( LinuxInterfaceRequest request );

		//*********************************
		// JavaxUsbDcpRequest methods

	/**
	 * Submit to a DCP.
	 * @param request the LinuxDcpRequest.
	 */
	static native void nativeSubmitDcpRequest( LinuxDcpRequest request );

	/**
	 * Abort a request in progress on a DCP.
	 * @param request the LinuxDcpRequest.
	 */
	static native void nativeAbortDcpRequest( LinuxDcpRequest request );

	/**
	 * Complete a request on a DCP.
	 * @param request the LinuxDcpRequest.
	 */
	static native void nativeCompleteDcpRequest( LinuxDcpRequest request );

	/**
	 * Set a configuration.
	 * @param request the LinuxSetConfigurationRequest.
	 */
	static native void nativeSetConfiguration( LinuxSetConfigurationRequest request );

	/**
	 * Set an interface.
	 * @param request the LinuxSetInterfaceRequest.
	 */
	static native void nativeSetInterface( LinuxSetInterfaceRequest request );

		//*********************************
		// JavaxUsbPipeRequest methods

	/**
	 * Abort a request in progress on pipe.
	 * @param request the LinuxPipeRequest
	 */
	static native void nativeAbortPipeRequest( LinuxPipeRequest request );

		//*********************************
		// JavaxUsbControlRequest methods

	/**
	 * Submit data on a control pipe.
	 * @param request the LinuxPipeRequest
	 * @param epAddress the address of the target endpoint
	 */
	static native void nativeSubmitControlRequest( LinuxPipeRequest request, byte epAddress );

	/**
	 * Handle a completed URB from a control pipe
	 * @param request the LinuxPipeRequest
	 */
	static native void nativeCompleteControlRequest( LinuxPipeRequest request );

		//*********************************
		// JavaxUsbBulkRequest methods

	/**
	 * Submit data on a bulk pipe.
	 * @param request the LinuxPipeRequest
	 * @param epAddress the address of the target endpoint
	 */
	static native void nativeSubmitBulkRequest( LinuxPipeRequest request, byte epAddress );

	/**
	 * Handle a completed URB from a bulk pipe
	 * @param request the LinuxPipeRequest
	 */
	static native void nativeCompleteBulkRequest( LinuxPipeRequest request );

		//*********************************
		// JavaxUsbInterruptRequest methods

	/**
	 * Submit data on an interrupt pipe.
	 * @param request the LinuxPipeRequest
	 * @param epAddress the address of the target endpoint
	 */
	static native void nativeSubmitInterruptRequest( LinuxPipeRequest request, byte epAddress );

	/**
	 * Handle a completed URB from an interrupt pipe
	 * @param request the LinuxPipeRequest
	 */
	static native void nativeCompleteInterruptRequest( LinuxPipeRequest request );

		//*********************************
		// JavaxUsbIsochronousRequest methods

	/**
	 * Submit data on a isochronous pipe.
	 * @param request the LinuxIsochronousRequest
	 * @param epAddress the address of the target endpoint
	 */
	static native void nativeSubmitIsochronousRequest( LinuxIsochronousRequest request, byte epAddress );

	/**
	 * Handle a completed URB from an isochronous pipe
	 * @param request the LinuxIsochronousRequest
	 */
	static native void nativeCompleteIsochronousRequest( LinuxIsochronousRequest request );

	//*************************************************************************
	// Device key methods

	/**
	 * Add UsbDevice to device table/map
	 * @param device The UsbDevice to add
	 * @param key The key to associate the UsbDevice with
	 */
	static void addUsbDevice( UsbDevice device, String key )
	{
		usbDeviceTable.put( key, device );
		usbDeviceKeyTable.put( device, key );
	}

	/**
	 * Remove UsbDevice to device table/map
	 * @param device The UsbDevice to remove
	 */
	static void removeUsbDevice( UsbDevice device )
	{
		if (!usbDeviceKeyTable.containsKey( device )) return;

		usbDeviceTable.remove( (String)usbDeviceKeyTable.get( device ) );
		usbDeviceKeyTable.remove( device );
	}

	/**
	 * Get a UsbDevice from the table
	 * @param key The key of the UsbDevice to get
	 * @return The UsbDevice associated with the key, or null if not in table
	 */
	static UsbDevice getUsbDevice( String key )
	{
		return (UsbDevice)usbDeviceTable.get( key );
	}

	/**
	 * Get a key for a UsbDevice from the table
	 * @param usbDevice The UsbDevice for the key to get
	 * @return The key associated with the UsbDevice, or null if not in table
	 */
	static String getUsbDeviceKey( UsbDevice usbDevice )
	{
		return (String)usbDeviceKeyTable.get( usbDevice );
	}

	/** @return an Enumeration of all device keys */
	static Enumeration getUsbDeviceKeyEnumeration()
	{
		return usbDeviceTable.keys();
	}

	//*************************************************************************
	// Creation methods

	/**
	 * Create a new unconfigured UsbRootHub
	 * @return A new unconfigured UsbRootHub
	 */
	private static UsbRootHub createUsbRootHub()
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbRootHub
	 * @param maxPorts The number of ports this UsbRootHub has
	 * @return A new unconfigured UsbRootHub
	 */
	private static UsbRootHub createUsbRootHub( int maxPorts )
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbHub
	 * @param maxPorts The number of ports this UsbHub has
	 * @return A new unconfigured UsbHub
	 */
	private static UsbHub createUsbHub( int maxPorts )
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbHub and hook it to its parent UsbHub
	 * @param parentHub The parent UsbHub
	 * @param parentPort The port (on the parent UsbHub) this UsbHub is connected to
	 * @param maxPorts The number of ports this UsbHub has
	 * @return A new unconfigured UsbHub
	 */
	private static UsbHub createUsbHub( UsbHub parentHub, byte parentPort, int maxPorts )
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbDevice
	 * @return A new unconfigured UsbDevice
	 */
	private static UsbDevice createUsbDevice( )
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbDevice and hook it to its parent UsbHub
	 * @param parentHub The parent UsbHub
	 * @param parentPort The port (on the parent UsbHub) this UsbDevice is connected to
	 * @return A new unconfigured UsbDevice
	 */
	private static UsbDevice createUsbDevice( UsbHub parentHub, byte parentPort )
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbConfig and connect it to its UsbDevice
	 * @param device The UsbDevice this UsbConfig belongs to
	 * @return A new unconfigured UsbConfig
	 */
	private static UsbConfig createUsbConfig( UsbDevice device )
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbInterface and connect it to its UsbConfig
	 * @param config The UsbConfig this UsbInterface belongs to
	 * @return A new unconfigured UsbInterface
	 */
	private static UsbInterface createUsbInterface( UsbConfig config )
	{
return null;
	}

	/**
	 * Create a new unconfigured UsbEndpoint and connect it to its UsbInterface
	 * @param iface The UsbInterface this UsbEndpoint belongs to
	 * @return A new unconfigred UsbEndpoint
	 */
	private static UsbEndpoint createUsbEndpoint( UsbInterface iface )
	{
return null;
	}

	//*************************************************************************
	// Setup methods

	private static void configureUsbDevice( UsbDeviceImp targetDevice,
		byte length, byte type,
		byte deviceClass, byte deviceSubClass, byte deviceProtocol, byte maxDefaultEndpointSize,
		byte manufacturerIndex, byte productIndex, byte serialNumberIndex, byte numConfigs, short vendorId,
		short productId, short bcdDevice, short bcdUsb, String speedString )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		deviceClass += 0;
		deviceSubClass += 0;
		deviceProtocol += 0;
		maxDefaultEndpointSize += 0;
		manufacturerIndex += 0;
		productIndex += 0;
		serialNumberIndex += 0;
		numConfigs += 0;
		vendorId += 0;
		productId += 0;
		bcdDevice += 0;
		bcdUsb += 0;

		speedString = speedString.trim();

		DeviceDescriptorImp desc = new DeviceDescriptorImp( length, type,
			deviceClass, deviceSubClass, deviceProtocol, maxDefaultEndpointSize, manufacturerIndex,
			productIndex, serialNumberIndex, numConfigs, vendorId, productId, bcdDevice, bcdUsb );

		targetDevice.setDeviceDescriptor(desc);
		targetDevice.setSpeedString(speedString);
	}

	private static void configureUsbConfig( UsbConfigImp targetConfig,
		byte length, byte type,
		byte numInterfaces, byte configValue, byte configIndex, byte attributes,
		byte maxPowerNeeded, boolean active )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		numInterfaces += 0;
		configValue += 0;
		configIndex += 0;
		attributes += 0;
		maxPowerNeeded += 0;

		ConfigDescriptorImp desc = new ConfigDescriptorImp( length, type,
/* FIXME - get total length! */ (short)0,
			numInterfaces, configValue, configIndex, attributes, maxPowerNeeded );

		targetConfig.setConfigDescriptor(desc);

		if (active)
			targetConfig.getUsbDeviceImp().setActiveUsbConfigNumber(configValue);
	}

	private static void configureUsbInterface( UsbInterfaceImp targetInterface,
		byte length, byte type,
		byte interfaceNumber, byte alternateNumber, byte numEndpoints,
		byte interfaceClass, byte interfaceSubClass, byte interfaceProtocol, byte interfaceIndex )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		interfaceNumber += 0;
		alternateNumber += 0;
		numEndpoints += 0;
		interfaceClass += 0;
		interfaceSubClass += 0;
		interfaceProtocol += 0;
		interfaceIndex += 0;

		InterfaceDescriptorImp desc = new InterfaceDescriptorImp( length, type,
			interfaceNumber, alternateNumber, numEndpoints, interfaceClass, interfaceSubClass,
			interfaceProtocol, interfaceIndex );

		targetInterface.setInterfaceDescriptor(desc);
	}

	private static void configureUsbEndpoint( UsbEndpointImp targetEndpoint,
		byte length, byte type,
		byte endpointAddress, byte attributes, byte interval, short maxPacketSize )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		endpointAddress += 0;
		attributes += 0;
		interval += 0;
		maxPacketSize += 0;

		EndpointDescriptorImp desc = new EndpointDescriptorImp( length, type,
			endpointAddress, attributes, interval, maxPacketSize );

		targetEndpoint.setEndpointDescriptor(desc);
	}

	private static void configureStringDescriptor( UsbDeviceImp device,
		byte length, byte type,
		byte index, String newString )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		index += 0;

		StringDescriptorImp desc = new StringDescriptorImp( length, type, newString );

		device.setStringDescriptor(index,desc);
	}

	//*************************************************************************
	// Topology updating methods

	/**
	 * Connect a UsbDeviceImp to its parent UsbHubImp.
	 * @param device the UsbDevice to connect
	 * @param hub the parent UsbHub
	 * @param port the port the UsbDevice is connected on
	 */
	static void connectUsbDevice( UsbDeviceImp device, UsbHubImp hub, byte port )
	{
		try {
			device.connect( hub, port );
		} catch ( UsbException uE ) {
//FIXME - add error handling!
throw new UsbRuntimeException("Could not attach UsbDeviceImp to parent UsbHubImp : " + uE.getMessage());
		}
	}

	/**
	 * Disconnect a UsbDeviceImp.
	 * @param device the UsbDevice to disconnect
	 */
	static void disconnectUsbDevice( UsbDeviceImp device )
	{
		device.disconnect();
	}

	//*************************************************************************
	// Private methods

	/** Set the virtual root hub */
	private static void setRootHub( UsbRootHub hub ) { rootHub = hub; }

	/**
	 * Get a virtual root hub
	 */
	private static UsbRootHub getVirtualRootHub()
	{
		return virtualRootHub;
	}

	//*************************************************************************
	// Class variables

	private static UsbRootHub rootHub = null;
	private static UsbRootHub virtualRootHub = new VirtualUsbRootHubImp();

	private static Hashtable usbDeviceTable = new Hashtable();
	private static Hashtable usbDeviceKeyTable = new Hashtable();

	private static boolean libraryLoaded = false;

	private static Hashtable msgLevelTable = new Hashtable();

	//*************************************************************************
	// Class constants

	public static final String LIBRARY_NAME = "JavaxUsb";

    public static final String ERROR_WHILE_LOADING_SHARED_LIBRARY = "Error while loading shared library";
    public static final String EXCEPTION_WHILE_LOADING_SHARED_LIBRARY = "Exception while loading shared library";

	static final String MSG_ENV_NAME = "JAVAX_USB_MSG_LEVEL";

	static final String MSG_CRITICAL = "CRITICAL";
	static final String MSG_ERROR = "ERROR";
	static final String MSG_WARNING = "WARNING";
	static final String MSG_NOTICE = "NOTICE";
	static final String MSG_INFO = "INFO";
	static final String MSG_DEBUG1 = "DEBUG1";
	static final String MSG_DEBUG2 = "DEBUG2";
	static final String MSG_DEBUG3 = "DEBUG3";

	private static final String INVALID_MSG_LEVEL = "Invalid message level";
}
