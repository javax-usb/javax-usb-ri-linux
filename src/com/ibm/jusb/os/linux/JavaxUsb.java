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
 * Interface to/from JNI.
 * @author Dan Streetman
 */
class JavaxUsb
{
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

		if ( null != System.getProperties().getProperty( MSG_ENV_NAME ) )
			setMsgLevel( System.getProperties().getProperty( MSG_ENV_NAME ) );
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

	//*************************************************************************
	// Native methods

	/**
	 * @param level the new msg level to use
	 */
	private static native void nativeSetMsgLevel( int level );

		//*********************************
		// JavaxUsbTopologyUpdater methods

	/**
	 * Call the native function that updates the topology.
	 * @param services The LinuxUsbServices instance.
	 * @param list The List to fill with newly connected devices.
	 * @param list The List of currently connected devices, which still connected devices will be removed from.
	 * @return The error number if one occurred.
	 */
	static native int nativeTopologyUpdater( LinuxUsbServices services, List connected, List disconnected );

		//*********************************
		// JavaxUsbTopologyListener methods

	/**
	 * Call the native function that listens for topology changes
	 * @param services The LinuxUsbServices instance.
	 * @return The error that caused the listener to exit.
	 */
	static native int nativeTopologyListener( LinuxUsbServices services );

		//*********************************
		// JavaxUsbDeviceProxy methods

	/**
	 * Start a LinuxDeviceProxy
	 * @param io A LinuxInterfaceIO object
	 */
	static native void nativeDeviceProxy( LinuxDeviceProxy proxy );

		//*********************************
		// JavaxUsbError methods

	/**
	 * @param error the error number
	 * @return the message associated with the specified error number
	 */
	static native String nativeGetErrorMessage( int error );

	//*************************************************************************
	// Creation methods

	/** @return A new UsbHubImp with max ports */
	private static UsbHubImp createUsbHubImp( String key, int maxPorts )
	{
		UsbHubImp hub = new UsbHubImp( maxPorts, null, null );

		LinuxDeviceOsImp linuxDeviceOsImp = new LinuxDeviceOsImp( hub, new LinuxDeviceProxy(key) );
		hub.setUsbDeviceOsImp( linuxDeviceOsImp );

		return hub;
	}

	/** @return A new UsbDeviceImp */
	private static UsbDeviceImp createUsbDeviceImp( String key )
	{
		UsbDeviceImp device = new UsbDeviceImp( null, null );

		LinuxDeviceOsImp linuxDeviceOsImp = new LinuxDeviceOsImp( device, new LinuxDeviceProxy(key) );
		device.setUsbDeviceOsImp( linuxDeviceOsImp );

		return device;
	}

	/** @return A new UsbConfigImp */
	private static UsbConfigImp createUsbConfigImp( UsbDeviceImp device,
		byte length, byte type, short totalLen,
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

		ConfigDescriptorImp desc = new ConfigDescriptorImp( length, type, totalLen,
			numInterfaces, configValue, configIndex, attributes, maxPowerNeeded );

		UsbConfigImp config = new UsbConfigImp( device, desc );

		if (active)
			device.setActiveUsbConfigNumber(configValue);

		return config;
	}

	/** @return A new UsbInterfaceImp */
	private static UsbInterfaceImp createUsbInterfaceImp( UsbConfigImp config,
		byte length, byte type,
		byte interfaceNumber, byte alternateNumber, byte numEndpoints,
		byte interfaceClass, byte interfaceSubClass, byte interfaceProtocol, byte interfaceIndex, boolean active )
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

		UsbInterfaceImp iface = new UsbInterfaceImp( config, desc );

		/* If the config is not active, neither are its interface settings */
		if (config.isActive() && active)
			iface.setActiveSettingNumber( iface.getInterfaceDescriptor().bAlternateSetting() );

		LinuxDeviceOsImp linuxDeviceOsImp = (LinuxDeviceOsImp)iface.getUsbConfigImp().getUsbDeviceImp().getUsbDeviceOsImp();
		LinuxInterfaceOsImp linuxInterfaceOsImp = new LinuxInterfaceOsImp( iface, linuxDeviceOsImp );
		iface.setUsbInterfaceOsImp( linuxInterfaceOsImp );

		return iface;
	}

	/** @return A new UsbEndpointImp */
	private static UsbEndpointImp createUsbEndpointImp( UsbInterfaceImp iface,
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

		UsbEndpointImp ep = new UsbEndpointImp( iface, desc );
		UsbPipeImp pipe = new UsbPipeImp( ep, null );

		LinuxInterfaceOsImp linuxInterfaceOsImp = (LinuxInterfaceOsImp)iface.getUsbInterfaceOsImp();
		switch (ep.getType()) {
		case UsbConst.ENDPOINT_TYPE_CONTROL:
			pipe.setUsbPipeOsImp( new LinuxControlPipeImp( pipe, linuxInterfaceOsImp ) );
			break;
		case UsbConst.ENDPOINT_TYPE_BULK:
			pipe.setUsbPipeOsImp( new LinuxBulkPipeImp( pipe, linuxInterfaceOsImp ) );
			break;
		case UsbConst.ENDPOINT_TYPE_INTERRUPT:
			pipe.setUsbPipeOsImp( new LinuxInterruptPipeImp( pipe, linuxInterfaceOsImp ) );
			break;
		case UsbConst.ENDPOINT_TYPE_ISOCHRONOUS:
			pipe.setUsbPipeOsImp( new LinuxIsochronousPipeImp( pipe, linuxInterfaceOsImp ) );
			break;
		default:
//FIXME - log?
			throw new RuntimeException("Invalid UsbEndpoint type " + ep.getType());
		}

		return ep;
	}

	//*************************************************************************
	// Setup methods

	private static void configureUsbDeviceImp( UsbDeviceImp targetDevice,
		byte length, byte type,
		byte deviceClass, byte deviceSubClass, byte deviceProtocol, byte maxDefaultEndpointSize,
		byte manufacturerIndex, byte productIndex, byte serialNumberIndex, byte numConfigs, short vendorId,
		short productId, short bcdDevice, short bcdUsb, int speed )
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

		DeviceDescriptorImp desc = new DeviceDescriptorImp( length, type,
			bcdUsb, deviceClass, deviceSubClass, deviceProtocol, maxDefaultEndpointSize, vendorId, productId,
			bcdDevice, manufacturerIndex, productIndex, serialNumberIndex, numConfigs );

		targetDevice.setDeviceDescriptor(desc);

		switch (speed) {
		case SPEED_LOW:
			targetDevice.setSpeed(UsbConst.DEVICE_SPEED_LOW);
		case SPEED_FULL:
			targetDevice.setSpeed(UsbConst.DEVICE_SPEED_FULL);
		default:
			/* log */
			targetDevice.setSpeed(UsbConst.DEVICE_SPEED_UNKNOWN);
		}
	}

	//*************************************************************************
	// Class variables

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

	private static final int SPEED_LOW = 1;
	private static final int SPEED_FULL = 2;
}
