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
import com.ibm.jusb.*;
import com.ibm.jusb.os.*;
import javax.usb.util.*;

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
		try { System.loadLibrary( LinuxUsbConst.SHARED_LIBRARY_NAME ); }
		catch ( Exception e ) { throw new UsbException( LinuxUsbConst.EXCEPTION_WHILE_LOADING_SHARED_LIBRARY + " " + System.mapLibraryName( LinuxUsbConst.SHARED_LIBRARY_NAME ) + " : " + e.getMessage() ); }
		catch ( Error e ) { throw new UsbException( LinuxUsbConst.ERROR_WHILE_LOADING_SHARED_LIBRARY + " " + System.mapLibraryName( LinuxUsbConst.SHARED_LIBRARY_NAME ) + " : " + e.getMessage() ); }

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
		return getUsbInfoFactory().createUsbRootHub();
	}

	/**
	 * Create a new unconfigured UsbRootHub
	 * @param maxPorts The number of ports this UsbRootHub has
	 * @return A new unconfigured UsbRootHub
	 */
	private static UsbRootHub createUsbRootHub( int maxPorts )
	{
		return getUsbInfoFactory().createUsbRootHub( maxPorts );
	}

	/**
	 * Create a new unconfigured UsbHub
	 * @param maxPorts The number of ports this UsbHub has
	 * @return A new unconfigured UsbHub
	 */
	private static UsbHub createUsbHub( int maxPorts )
	{
		return getUsbInfoFactory().createUsbHub( maxPorts );
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
		try {
			return getUsbInfoFactory().createUsbHub( parentHub, parentPort, maxPorts );
		} catch ( UsbException uE ) {
			System.err.println( "Could not create hub : " + uE.getMessage() );
			return null;
		}
	}

	/**
	 * Create a new unconfigured UsbDevice
	 * @return A new unconfigured UsbDevice
	 */
	private static UsbDevice createUsbDevice( )
	{
		return getUsbInfoFactory().createUsbDevice( );
	}

	/**
	 * Create a new unconfigured UsbDevice and hook it to its parent UsbHub
	 * @param parentHub The parent UsbHub
	 * @param parentPort The port (on the parent UsbHub) this UsbDevice is connected to
	 * @return A new unconfigured UsbDevice
	 */
	private static UsbDevice createUsbDevice( UsbHub parentHub, byte parentPort )
	{
		try {
			return getUsbInfoFactory().createUsbDevice( parentHub, parentPort );
		} catch ( UsbException uE ) {
			System.err.println( "Could not create device : " + uE.getMessage() );
			return null;
		}
	}

	/**
	 * Create a new unconfigured UsbConfig and connect it to its UsbDevice
	 * @param device The UsbDevice this UsbConfig belongs to
	 * @return A new unconfigured UsbConfig
	 */
	private static UsbConfig createUsbConfig( UsbDevice device )
	{
		return getUsbInfoFactory().createUsbConfig( device );
	}

	/**
	 * Create a new unconfigured UsbInterface and connect it to its UsbConfig
	 * @param config The UsbConfig this UsbInterface belongs to
	 * @return A new unconfigured UsbInterface
	 */
	private static UsbInterface createUsbInterface( UsbConfig config )
	{
		return getUsbInfoFactory().createUsbInterface( config );
	}

	/**
	 * Create a new unconfigured UsbEndpoint and connect it to its UsbInterface
	 * @param iface The UsbInterface this UsbEndpoint belongs to
	 * @return A new unconfigred UsbEndpoint
	 */
	private static UsbEndpoint createUsbEndpoint( UsbInterface iface )
	{
		return getUsbInfoFactory().createUsbEndpoint( iface );
	}

	//*************************************************************************
	// Setup methods

	private static void configureUsbDevice( UsbDevice targetDevice,
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

		speedString = ( null == speedString ? "" : speedString.trim() );

		DeviceDescriptor desc = getDescriptorFactory().createDeviceDescriptor( length, type,
			deviceClass, deviceSubClass, deviceProtocol, maxDefaultEndpointSize, manufacturerIndex,
			productIndex, serialNumberIndex, numConfigs, vendorId, productId, bcdDevice, bcdUsb );

		targetDevice.accept( getInitUsbInfoV() );
		getInitUsbInfoV().setUsbDeviceInfo( desc, speedString );
	}

	private static void configureUsbConfig( UsbConfig targetConfig,
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

		ConfigDescriptor desc = getDescriptorFactory().createConfigDescriptor( length, type,
			numInterfaces, configValue, configIndex, attributes, maxPowerNeeded );

		targetConfig.accept( getInitUsbInfoV() );
		getInitUsbInfoV().setUsbConfigInfo( desc );

		if (active) {
			targetConfig.getUsbDevice().accept( getInitUsbInfoV() );
			getInitUsbInfoV().setActiveUsbConfigNumber( configValue );
		}
	}

	private static void configureUsbInterface( UsbInterface targetInterface,
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

		InterfaceDescriptor desc = getDescriptorFactory().createInterfaceDescriptor( length, type,
			interfaceNumber, alternateNumber, numEndpoints, interfaceClass, interfaceSubClass,
			interfaceProtocol, interfaceIndex );

		targetInterface.accept( getInitUsbInfoV() );
		getInitUsbInfoV().setUsbInterfaceInfo( desc );
	}

	private static void configureUsbEndpoint( UsbEndpoint targetEndpoint,
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

		EndpointDescriptor desc = getDescriptorFactory().createEndpointDescriptor( length, type,
			endpointAddress, attributes, interval, maxPacketSize );

		targetEndpoint.accept( getInitUsbInfoV() );
		getInitUsbInfoV().setUsbEndpointInfo( desc );
	}

	private static void configureStringDescriptor( UsbDevice device,
		byte length, byte type,
		byte index, String newString )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		index += 0;

		StringDescriptor desc = getDescriptorFactory().createStringDescriptor( length, type, newString );

		device.accept( getInitUsbInfoV() );
		getInitUsbInfoV().setUsbDeviceStringDescriptor( index, desc );
	}

	//*************************************************************************
	// Topology updating methods

	/**
	 * Connect a UsbDevice to its parent UsbHub
	 * @param usbDevice the UsbDevice to connect
	 * @param usbHub the parent UsbHub
	 * @param port the port the UsbDevice is connected on
	 */
	static void connectUsbDevice( UsbDevice usbDevice, UsbHub usbHub, byte port )
	{
		usbDevice.accept( getInitUsbInfoV() );
		try {
			getInitUsbInfoV().connect( usbHub, port );
		} catch ( UsbException uE ) {
			/* This is not the correct exception handling */
			System.err.println( "Could not connect UsbDevice : " + uE.getMessage() );
		}
	}

	/**
	 * Disconnect a UsbDevice
	 * @param usbDevice the UsbDevice to disconnect
	 */
	static void disconnectUsbDevice( UsbDevice usbDevice )
	{
		usbDevice.accept( getInitUsbInfoV() );
		try {
			getInitUsbInfoV().disconnect();
		} catch ( UsbException uE ) {
			/* This is not the correct exception handling */
			System.err.println( "Could not disconnect UsbDevice: " + uE.getMessage() );
		}
	}

	//*************************************************************************
	// Getter methods (for use by creation/setup methods in this class)

	private static DescriptorFactory getDescriptorFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getHelper().getDescriptorFactory();
	}

	private static UsbInfoFactory getUsbInfoFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getHelper().getUsbInfoFactory();
	}

	private static InitUsbInfoV getInitUsbInfoV()
	{
		if ( null == initUsbInfoV ) initUsbInfoV = new InitUsbInfoV();
		return initUsbInfoV;
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
		if (null == virtualRootHub)
			virtualRootHub = createVirtualRootHub();

		return virtualRootHub;
	}

	/** Create a 'fake' virtual root hub */
	private static UsbRootHub createVirtualRootHub()
	{
		UsbRootHub usbRootHub = getUsbInfoFactory().createUsbRootHub();
		String manufacturerString = LinuxUsbConst.VIRTUAL_ROOT_HUB_MANUFACTURER;
		String productString = LinuxUsbConst.VIRTUAL_ROOT_HUB_PRODUCT;
		String serialNumberString = LinuxUsbConst.VIRTUAL_ROOT_HUB_SERIALNUMBER;

		configureUsbDevice( usbRootHub, (byte)0x09, DescriptorConst.DESCRIPTOR_TYPE_DEVICE,
			(byte)0x09, (byte)0, (byte)0, (byte)8, (byte)1, (byte)2, (byte)3, (byte)1,
			(short)0x0000, (short)0x0000, (short)0x0000, (short)0x0100, "12 Mbps" );
		UsbConfig config = createUsbConfig( usbRootHub );
		configureUsbConfig( config, (byte)0x09, DescriptorConst.DESCRIPTOR_TYPE_CONFIG,
			(byte)1, (byte)0, (byte)0, (byte)0x80, (byte)0, true );
		UsbInterface iface = createUsbInterface( config );
		configureUsbInterface( iface, (byte)0x09, DescriptorConst.DESCRIPTOR_TYPE_INTERFACE,
			(byte)0, (byte)0, (byte)0, (byte)0x09, (byte)0, (byte)0, (byte)0 );
		configureStringDescriptor( usbRootHub, (byte)manufacturerString.length(),
			DescriptorConst.DESCRIPTOR_TYPE_STRING, (byte)1, manufacturerString );
		configureStringDescriptor( usbRootHub, (byte)productString.length(),
			DescriptorConst.DESCRIPTOR_TYPE_STRING, (byte)2, productString );
		configureStringDescriptor( usbRootHub, (byte)serialNumberString.length(),
			DescriptorConst.DESCRIPTOR_TYPE_STRING, (byte)3, serialNumberString );
			

		return usbRootHub;
	}

	//*************************************************************************
	// Class variables

	private static UsbRootHub rootHub = null;
	private static UsbRootHub virtualRootHub = null;

	private static InitUsbInfoV initUsbInfoV = null;

	private static Hashtable usbDeviceTable = new Hashtable();
	private static Hashtable usbDeviceKeyTable = new Hashtable();

	private static boolean libraryLoaded = false;

	private static Hashtable msgLevelTable = new Hashtable();

	//*************************************************************************
	// Class constants

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
