
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

static inline int build_device( JNIEnv *env, jclass JavaxUsb, jobject linuxUsbServices, unsigned char bus, unsigned char dev,
	jobject parent, int parentport, jobject connectedDevices, jobject disconnectedDevices );

static inline int build_config( JNIEnv *env, jclass JavaxUsb, int fd, jobject device, unsigned char bus, unsigned char dev );

static inline jobject build_interface( JNIEnv *env, jclass JavaxUsb, int fd, jobject config, struct jusb_interface_descriptor *if_desc );

static inline void build_endpoint( JNIEnv *env, jclass JavaxUsb, jobject interface, struct jusb_endpoint_descriptor *ep_desc );

static inline void *get_descriptor( int fd );

/**
 * Update topology tree
 * @author Dan Streetman
 */
JNIEXPORT jint JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeTopologyUpdater
			( JNIEnv *env, jclass JavaxUsb, jobject linuxUsbServices, jobject connectedDevices, jobject disconnectedDevices )
{
	int busses, port, devices = 0;
	struct dirent **buslist;
	char *orig_dir = getcwd(NULL,0);

	jclass LinuxUsbServices = (*env)->GetObjectClass( env, linuxUsbServices );
	jfieldID usbRootHubImpID = (*env)->GetFieldID( env, LinuxUsbServices, "usbRootHubImp", "Lcom/ibm/jusb/UsbRootHubImp;" );
	jobject rootHub = (*env)->GetObjectField( env, linuxUsbServices, usbRootHubImpID );

	if (chdir(USBDEVFS_PATH) || (0 > (busses = scandir(".", &buslist, select_dirent_dir, alphasort))) ) {
		dbg( MSG_ERROR, "nativeTopologyUpdater : Could not access : %s\n", USBDEVFS_PATH );
		return -1;
	}

	for (port=0; port<busses; port++) {
		if (chdir(buslist[port]->d_name)) {
			dbg( MSG_ERROR, "nativeTopologyUpdater : Could not access %s/%s\n", USBDEVFS_PATH, buslist[port]->d_name );
		} else {
			struct dirent **devlist = NULL;
			int bus, hcAddress, devs;

			bus = atoi( buslist[port]->d_name );
			devs = scandir(".", &devlist, select_dirent_reg, alphasort);

			errno = 0;
			if (0 > devs) {
				dbg( MSG_ERROR, "nativeTopologyUpdater : Could not access device nodes in %s/%s : %s\n", USBDEVFS_PATH, buslist[port]->d_name, strerror(errno) );
			} else if (!devs) {
				dbg( MSG_ERROR, "nativeTopologyUpdater : No device nodes found in %s/%s\n", USBDEVFS_PATH, buslist[port]->d_name );
			} else {
				/* Hopefully, the host controller has the lowest numbered address on this bus! */
				hcAddress = atoi( devlist[0]->d_name );
				devices += build_device( env, JavaxUsb, linuxUsbServices, bus, hcAddress, rootHub, port, connectedDevices, disconnectedDevices );
			}

			while (0 < devs) free(devlist[--devs]);
			if (devlist) free(devlist);
		}
		chdir(USBDEVFS_PATH);
		free(buslist[port]);
	}
	free(buslist);

	(*env)->DeleteLocalRef( env, LinuxUsbServices );

	if (rootHub) (*env)->DeleteLocalRef( env, rootHub );

	if (orig_dir) {
		chdir(orig_dir);
		free(orig_dir);
	}

	return 0;
}

static inline int build_device( JNIEnv *env, jclass JavaxUsb, jobject linuxUsbServices, unsigned char bus, unsigned char dev,
	jobject parent, int parentport, jobject connectedDevices, jobject disconnectedDevices )
{
	int fd = 0, port, ncfg;
	int devices = 0;
	char node[4] = { 0, };
	char *path = (char*)getcwd( NULL, 0 );
	char *key = NULL;
	struct usbdevfs_ioctl *usbioctl = NULL;
	struct usbdevfs_hub_portinfo *portinfo = NULL;
	struct usbdevfs_connectinfo *connectinfo = NULL;
	struct jusb_device_descriptor *dev_desc;

	jobject device = NULL, existingDevice;
	jstring speedString = NULL, keyString = NULL;

	jclass LinuxUsbServices = (*env)->GetObjectClass( env, linuxUsbServices );
	jmethodID createUsbHubImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbHubImp", "(Ljava/lang/String;I)Lcom/ibm/jusb/UsbHubImp;" );
	jmethodID createUsbDeviceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbDeviceImp", "(Ljava/lang/String;)Lcom/ibm/jusb/UsbDeviceImp;" );
	jmethodID configureUsbDeviceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "configureUsbDeviceImp", "(Lcom/ibm/jusb/UsbDeviceImp;BBBBBBBBBBSSSSLjava/lang/String;)V" );
	jmethodID checkUsbDeviceImp = (*env)->GetMethodID( env, LinuxUsbServices, "checkUsbDeviceImp", "(Lcom/ibm/jusb/UsbHubImp;ILcom/ibm/jusb/UsbDeviceImp;Ljava/util/List;Ljava/util/List;)Lcom/ibm/jusb/UsbDeviceImp;" );

	if (!path) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get current directory!\n" );
		goto BUILD_DEVICE_EXIT;
	}

	key = malloc(strlen(path) + 5);
	sprintf( key, "%s/%3.03d", path, dev );
	keyString = (*env)->NewStringUTF( env, key );

	dbg( MSG_DEBUG2, "nativeTopologyUpdater.build_device : Building device %s\n", key );

	sprintf( node, "%3.03d", dev );
	fd = open( node, O_RDWR );
	if ( 0 >= fd ) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not access %s\n", key );
		goto BUILD_DEVICE_EXIT;
	}

	if (!(dev_desc = get_descriptor( fd ))) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Short read on device descriptor\n" );
		goto BUILD_DEVICE_EXIT;
	}

	if (dev_desc->bDeviceClass == USB_CLASS_HUB) {
		usbioctl = malloc(sizeof(*usbioctl));
		portinfo = malloc(sizeof(*portinfo));
		usbioctl->ioctl_code = USBDEVFS_HUB_PORTINFO;
		usbioctl->ifno = 0;
		usbioctl->data = portinfo;
		errno = 0;
		if (0 >= ioctl( fd, USBDEVFS_IOCTL, usbioctl )) {
			dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get portinfo from hub, error = %d\n", errno );
			goto BUILD_DEVICE_EXIT;
		} else {
		  dbg( MSG_DEBUG2, "nativeTopologyUpdater.build_device : Device is hub with %d ports\n",portinfo->nports );
		}
		free(usbioctl);
		usbioctl = NULL;
		device = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbHubImp, keyString, portinfo->nports );
	} else {
	  device = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbDeviceImp, keyString );
	}

	connectinfo = malloc(sizeof(*connectinfo));
	errno = 0;
	if (ioctl( fd, USBDEVFS_CONNECTINFO, connectinfo )) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get connectinfo from device, error = %d\n", errno );
		goto BUILD_DEVICE_EXIT;
	} else {
	  dbg( MSG_DEBUG2, "nativeTopologyUpdater.build_device : Device speed is %s\n", (connectinfo->slow?"1.5 Mbps":"12 Mbps") );
	}
	speedString = (*env)->NewStringUTF( env, ( connectinfo->slow ? "1.5 Mbps" : "12 Mbps" ) );
	free(connectinfo);
	connectinfo = NULL;

	(*env)->CallStaticVoidMethod( env, JavaxUsb, configureUsbDeviceImp, device, 
		dev_desc->bLength, dev_desc->bDescriptorType,
		dev_desc->bDeviceClass, dev_desc->bDeviceSubClass, dev_desc->bDeviceProtocol,
		dev_desc->bMaxPacketSize0, dev_desc->iManufacturer, dev_desc->iProduct, dev_desc->iSerialNumber,
		dev_desc->bNumConfigurations, dev_desc->idVendor, dev_desc->idProduct,
		dev_desc->bcdDevice, dev_desc->bcdUSB, speedString );
	(*env)->DeleteLocalRef( env, speedString );
	speedString = NULL;

	/* Build config descriptors */
	for (ncfg=0; ncfg<dev_desc->bNumConfigurations; ncfg++) {
		if (build_config( env, JavaxUsb, fd, device, bus, dev )) {
			dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get config %d for device %d\n", ncfg, dev );
			goto BUILD_DEVICE_EXIT;
		}
	}

	existingDevice = (*env)->CallObjectMethod( env, linuxUsbServices, checkUsbDeviceImp, parent, parentport+1, device, connectedDevices, disconnectedDevices );
	(*env)->DeleteLocalRef( env, device );
	device = existingDevice;

	/* This device is set up and ready! */
	devices = 1;
	close( fd );
	fd = 0;

	if ((dev_desc->bDeviceClass == USB_CLASS_HUB) && portinfo)
		for (port=0; port<(portinfo->nports); port++)
			if (portinfo->port[port]) {
				dbg( MSG_DEBUG2, "nativeTopologyUpdater.build_device : Building device attached to port %d\n", portinfo->port[port]);
				devices += build_device( env, JavaxUsb, linuxUsbServices, bus, portinfo->port[port], device, port, connectedDevices, disconnectedDevices );
			}

BUILD_DEVICE_EXIT:
	if (fd) close(fd);
	if (device) (*env)->DeleteLocalRef( env, device );
	if (connectinfo) free(connectinfo);
	if (dev_desc) free(dev_desc);
	if (usbioctl) free(usbioctl);
	if (portinfo) free(portinfo);
	if (keyString) (*env)->DeleteLocalRef( env, keyString );
	if (speedString) (*env)->DeleteLocalRef( env, speedString );
	if (path) free(path);
	if (key) free(key);

	return devices;
}

static inline int build_config( JNIEnv *env, jclass JavaxUsb, int fd, jobject device, unsigned char bus, unsigned char dev )
{
	int result = -1;
	struct jusb_config_descriptor *cfg_desc = NULL;
	unsigned char *desc = NULL;
	unsigned short wTotalLength;
	unsigned int pos;
	jobject config = NULL, interface = NULL;
	jmethodID createUsbConfigImp;
	jboolean isActive = JNI_FALSE;

	if (!(cfg_desc = get_descriptor( fd ))) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Short read on config desriptor\n" );
		goto BUILD_CONFIG_EXIT;
	}

	createUsbConfigImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbConfigImp", "(Lcom/ibm/jusb/UsbDeviceImp;BBSBBBBBZ)Lcom/ibm/jusb/UsbConfigImp;" );

	dbg( MSG_DEBUG3, "nativeTopologyUpdater.build_config : Building config %d\n", cfg_desc->bConfigurationValue );

	wTotalLength = cfg_desc->wTotalLength;
	pos = cfg_desc->bLength;

	isActive = isConfigActive( fd, bus, dev, cfg_desc->bConfigurationValue );
	config = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbConfigImp, device,
		cfg_desc->bLength, cfg_desc->bDescriptorType, wTotalLength,
		cfg_desc->bNumInterfaces, cfg_desc->bConfigurationValue, cfg_desc->iConfiguration,
		cfg_desc->bmAttributes, cfg_desc->bMaxPower, isActive );

	while (pos < wTotalLength) {
		desc = get_descriptor( fd );
		if ((!desc) || (2 > desc[0])) {
			dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Short read on descriptor\n" );
			goto BUILD_CONFIG_EXIT;
		}
		pos += desc[0];
		switch( desc[1] ) {
			case USB_DT_DEVICE:
				dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Got device descriptor inside of config descriptor\n" );
				goto BUILD_CONFIG_EXIT;

			case USB_DT_CONFIG:
				dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Got config descriptor inside of config descriptor\n" );
				goto BUILD_CONFIG_EXIT;

			case USB_DT_INTERFACE:
				if (interface) (*env)->DeleteLocalRef( env, interface );
				interface = build_interface( env, JavaxUsb, fd, config, (struct jusb_interface_descriptor*)desc );
				break;

			case USB_DT_ENDPOINT:
				build_endpoint( env, JavaxUsb, interface, (struct jusb_endpoint_descriptor*)desc );
				break;

			default:
				/* Ignore proprietary descriptor */
				break;
		}
		free(desc);
		desc = NULL;
	}

	result = 0;

BUILD_CONFIG_EXIT:
	if (config) (*env)->DeleteLocalRef( env, config );
	if (interface) (*env)->DeleteLocalRef( env, interface );
	if (cfg_desc) free(cfg_desc);
	if (desc) free(desc);

	return result;
}

static inline jobject build_interface( JNIEnv *env, jclass JavaxUsb, int fd, jobject config, struct jusb_interface_descriptor *if_desc )
{
	jobject interface;
	jboolean isActive;

	jmethodID createUsbInterfaceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbInterfaceImp", "(Lcom/ibm/jusb/UsbConfigImp;BBBBBBBBBZ)Lcom/ibm/jusb/UsbInterfaceImp;" );

	dbg( MSG_DEBUG3, "nativeTopologyUpdater.build_interface : Building interface %d\n", if_desc->bInterfaceNumber );

	isActive = isInterfaceSettingActive( fd, if_desc->bInterfaceNumber, if_desc->bAlternateSetting );
	interface = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbInterfaceImp, config,
		if_desc->bLength, if_desc->bDescriptorType,
		if_desc->bInterfaceNumber, if_desc->bAlternateSetting, if_desc->bNumEndpoints, if_desc->bInterfaceClass,
		if_desc->bInterfaceSubClass, if_desc->bInterfaceProtocol, if_desc->iInterface, isActive );

	return interface;
}

static inline void build_endpoint( JNIEnv *env, jclass JavaxUsb, jobject interface, struct jusb_endpoint_descriptor *ep_desc )
{
	jmethodID createUsbEndpointImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbEndpointImp", "(Lcom/ibm/jusb/UsbInterfaceImp;BBBBBS)Lcom/ibm/jusb/UsbEndpointImp;" );

	dbg( MSG_DEBUG3, "nativeTopologyUpdater.build_endpoint : Building endpoint 0x%2.02x\n", ep_desc->bEndpointAddress );

	if (!interface) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_endpoint : Interface is NULL\n");
		return;
	}

	(*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbEndpointImp, interface,
		ep_desc->bLength, ep_desc->bDescriptorType,
		ep_desc->bEndpointAddress, ep_desc->bmAttributes, ep_desc->bInterval, ep_desc->wMaxPacketSize );
}

static inline void *get_descriptor( int fd )
{
	unsigned char *buffer = NULL, *len = NULL;
	int nread;

	len = malloc(1);
	if (1 > read( fd, len, 1 )) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Cannot read from file!\n" );
		goto GET_DESCRIPTOR_EXIT;
	}

	if (*len == 0) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Zero-length descriptor?\n" );
		goto GET_DESCRIPTOR_EXIT;
	}

	buffer = malloc(*len);
	buffer[0] = *len;
	free(len);
	len = NULL;

	nread = read( fd, buffer+1, buffer[0]-1 );
	if (buffer[0]-1 != nread) {
		if (buffer[0]-1 > nread) dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Short read on file\n" );
		else dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Long read on file\n" );
		free(buffer);
		buffer = NULL;
	}

GET_DESCRIPTOR_EXIT:
	if (len) free(len);

	return buffer;
}
