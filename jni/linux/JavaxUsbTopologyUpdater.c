
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

static int build_device( JNIEnv *env, jclass JavaxUsb, jclass LinuxTopologyUpdater, jobject linuxTopologyUpdater, jboolean isRootHub, unsigned char dev, jobject parent, int parentport );

static int build_config( JNIEnv *env, jclass JavaxUsb, int fd, jobject device );

static jobject build_interface( JNIEnv *env, jclass JavaxUsb, jobject config, struct jusb_interface_descriptor *if_desc );

static void build_endpoint( JNIEnv *env, jclass JavaxUsb, jobject interface, struct jusb_endpoint_descriptor *ep_desc );

static jobject add_device( JNIEnv *env, jclass JavaxUsb, jclass LinuxTopologyUpdater, jobject linuxTopologyUpdater, jobject device, unsigned char dev );

static void *get_descriptor( int fd );

/**
 * Update topology tree
 * @author Dan Streetman
 */
JNIEXPORT jint JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeTopologyUpdater
			( JNIEnv *env, jclass JavaxUsb, jobject linuxTopologyUpdater )
{
	int busses, port, devices = 0;
	struct dirent **buslist;
	char *orig_dir = getcwd(NULL,0);

	jclass LinuxTopologyUpdater = (*env)->GetObjectClass( env, linuxTopologyUpdater );
	jmethodID updateTopology = (*env)->GetMethodID( env, LinuxTopologyUpdater, "updateTopology", "()V" );
	jmethodID getVirtualRootHubImp = (*env)->GetStaticMethodID( env, JavaxUsb, "getVirtualRootHubImp", "()Lcom/ibm/jusb/UsbRootHubImp;" );
	jmethodID setRootHubImp = (*env)->GetStaticMethodID( env, JavaxUsb, "setRootHubImp", "(Lcom/ibm/jusb/UsbRootHubImp;)V" );
	jobject rootHub = NULL;
	jboolean isRootHub;

	if (chdir(USBDEVFS_PATH) || (0 > (busses = scandir(".", &buslist, select_dirent_dir, alphasort))) ) {
		dbg( MSG_ERROR, "nativeTopologyUpdater : Could not access : %s\n", USBDEVFS_PATH );
		return -1;
	}

	/* Use 'if (1 < busses)' and set isRootHub to JNI_TRUE
	 * to *not* have a virtual hub for a single HC configuration
	 */
	rootHub = (*env)->CallStaticObjectMethod( env, JavaxUsb, getVirtualRootHubImp );
	(*env)->CallStaticVoidMethod( env, JavaxUsb, setRootHubImp, rootHub );
	isRootHub = JNI_FALSE;

	for (port=0; port<busses; port++) {
		if (chdir(buslist[port]->d_name)) {
			dbg( MSG_ERROR, "nativeTopologyUpdater : Could not access %s/%s\n", USBDEVFS_PATH, buslist[port]->d_name );
		} else {
			struct dirent **devlist = NULL;
			int hcAddress, devs;

			devs = scandir(".", &devlist, select_dirent_reg, alphasort);

			errno = 0;
			if (0 > devs) {
				dbg( MSG_ERROR, "nativeTopologyUpdater : Could not access device nodes in %s/%s : %s\n", USBDEVFS_PATH, buslist[port]->d_name, strerror(errno) );
			} else if (!devs) {
				dbg( MSG_ERROR, "nativeTopologyUpdater : No device nodes found in %s/%s\n", USBDEVFS_PATH, buslist[port]->d_name );
			} else {
				/* Hopefully, the host controller has the lowest numbered address on this bus! */
				hcAddress = atoi( devlist[0]->d_name );
				devices += build_device( env, JavaxUsb, LinuxTopologyUpdater, linuxTopologyUpdater, isRootHub, hcAddress, rootHub, port );
			}

			while (0 < devs) free(devlist[--devs]);
			if (devlist) free(devlist);
		}
		chdir(USBDEVFS_PATH);
		free(buslist[port]);
	}
	free(buslist);

	(*env)->CallVoidMethod( env, linuxTopologyUpdater, updateTopology );

	if (rootHub) (*env)->DeleteLocalRef( env, rootHub );

	if (orig_dir) {
		chdir(orig_dir);
		free(orig_dir);
	}

	return devices;
}

static int build_device( JNIEnv *env, jclass JavaxUsb, jclass LinuxTopologyUpdater, jobject linuxTopologyUpdater, jboolean isRootHub, unsigned char dev, jobject parent, int parentport )
{
	int fd, port, ncfg;
	int devices = 0;
	char node[4] = { 0, };
	struct usbdevfs_ioctl *usbioctl;
	struct usbdevfs_hub_portinfo *portinfo = NULL;
	struct usbdevfs_connectinfo *connectinfo;
	struct jusb_device_descriptor *dev_desc;

	jobject device, existingDevice;
	jstring speedString;

	jmethodID setRootHubImp = (*env)->GetStaticMethodID( env, JavaxUsb, "setRootHubImp", "(Lcom/ibm/jusb/UsbRootHubImp;)V" );
	jmethodID createUsbRootHubImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbRootHubImp", "(I)Lcom/ibm/jusb/UsbRootHubImp;" );
	jmethodID createUsbHubImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbHubImp", "(I)Lcom/ibm/jusb/UsbHubImp;" );
	jmethodID createUsbDeviceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbDeviceImp", "()Lcom/ibm/jusb/UsbDeviceImp;" );
	jmethodID configureUsbDeviceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "configureUsbDeviceImp", "(Lcom/ibm/jusb/UsbDeviceImp;BBBBBBBBBBSSSSLjava/lang/String;)V" );
	jmethodID connectUsbDeviceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "connectUsbDeviceImp", "(Lcom/ibm/jusb/UsbDeviceImp;Lcom/ibm/jusb/UsbHubImp;B)V" );

	dbg( MSG_DEBUG2, "nativeTopologyUpdater.build_device : Building device %d\n", dev );

	sprintf( node, "%3.03d", dev );
	fd = open( node, O_RDWR );
	if ( 0 >= fd ) {
		char *wd = (char*)getcwd( NULL, 0 );
		if (!wd) dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get current working directory!\n" );
		else dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not access %s/%s\n", wd, node );
		if (wd) free(wd);
		return 0;
	}

	if (!(dev_desc = get_descriptor( fd ))) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Short read on device descriptor\n" );
		close( fd );
		return 0;
	}

	if (dev_desc->bDeviceClass == USB_CLASS_HUB) {
		usbioctl = malloc(sizeof(struct usbdevfs_ioctl));
		portinfo = malloc(sizeof(struct usbdevfs_hub_portinfo));
		usbioctl->ioctl_code = USBDEVFS_HUB_PORTINFO;
		usbioctl->ifno = 0;
		usbioctl->data = portinfo;
		errno = 0;
		if (0 >= ioctl( fd, USBDEVFS_IOCTL, usbioctl )) {
			dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get portinfo from hub, error = %d\n", errno );
			free(dev_desc);
			free(usbioctl);
			free(portinfo);
			close(fd);
			return 0;
		}
		free(usbioctl);
		if (JNI_TRUE == isRootHub)
			device = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbRootHubImp, portinfo->nports );
		else
			device = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbHubImp, portinfo->nports );
	} else device = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbDeviceImp );

	connectinfo = malloc(sizeof(struct usbdevfs_connectinfo));
	errno = 0;
	if (ioctl( fd, USBDEVFS_CONNECTINFO, connectinfo )) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get connectinfo from device, error = %d\n", errno );
		(*env)->DeleteLocalRef( env, device );
		free(dev_desc);
		if (portinfo) free(portinfo);
		free(connectinfo);
		close(fd);
		return 0;
	}
	speedString = (*env)->NewStringUTF( env, ( connectinfo->slow ? "1.5 Mbps" : "12 Mbps" ) );
	free(connectinfo);

	(*env)->CallStaticVoidMethod( env, JavaxUsb, configureUsbDeviceImp, device, 
		dev_desc->bLength, dev_desc->bDescriptorType,
		dev_desc->bDeviceClass, dev_desc->bDeviceSubClass, dev_desc->bDeviceProtocol,
		dev_desc->bMaxPacketSize0, dev_desc->iManufacturer, dev_desc->iProduct, dev_desc->iSerialNumber,
		dev_desc->bNumConfigurations, dev_desc->idVendor, dev_desc->idProduct,
		dev_desc->bcdDevice, dev_desc->bcdUSB, speedString );
	(*env)->DeleteLocalRef( env, speedString );

	/* Build config descriptors */
	for (ncfg=0; ncfg<dev_desc->bNumConfigurations; ncfg++) {
		if (build_config( env, JavaxUsb, fd, device )) {
			dbg( MSG_ERROR, "nativeTopologyUpdater.build_device : Could not get config %d for device\n", ncfg );
			(*env)->DeleteLocalRef( env, device );
			free(dev_desc);
			if (portinfo) free(portinfo);
			close( fd );
			return 0;
		}
	}

	/* If the device doesn't already exist, connect it to the topology */
	/* If the device does exist, use it instead of the dummy we just created... */
	existingDevice = add_device( env, JavaxUsb, LinuxTopologyUpdater, linuxTopologyUpdater, device, dev );
	if (JNI_TRUE == (*env)->IsSameObject( env, device, existingDevice )) {
		(*env)->DeleteLocalRef( env, existingDevice );

		/*
		 * If this devices has a parent, connect it, otherwise it's the root hub
		 * And don't forget ports are 1-based!
		 */
		if (parent)
			(*env)->CallStaticVoidMethod( env, JavaxUsb, connectUsbDeviceImp, device, parent, parentport+1 );
		else
			(*env)->CallStaticVoidMethod( env, JavaxUsb, setRootHubImp, device );
	} else {
		(*env)->DeleteLocalRef( env, device );
		device = existingDevice;
	}

	/* Hurray! This device is ready to go */
	devices = 1;
	close( fd );

	if ((dev_desc->bDeviceClass == USB_CLASS_HUB) && portinfo)
		for (port=0; port<(portinfo->nports); port++)
			if (portinfo->port[port])
				devices += build_device( env, JavaxUsb, LinuxTopologyUpdater, linuxTopologyUpdater, JNI_FALSE, portinfo->port[port], device, port );

	(*env)->DeleteLocalRef( env, device );
	free(dev_desc);
	if (portinfo) free(portinfo);

	return devices;
}

static int build_config( JNIEnv *env, jclass JavaxUsb, int fd, jobject device )
{
	struct jusb_config_descriptor *cfg_desc;
	unsigned char *desc;
	unsigned short wTotalLength;
	unsigned int pos;
	jobject config, interface = NULL;
	jmethodID createUsbConfigImp, configureUsbConfigImp;

	if (!(cfg_desc = get_descriptor( fd ))) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Short read on config desriptor\n" );
		return -1;
	}

	createUsbConfigImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbConfigImp", "(Lcom/ibm/jusb/UsbDeviceImp;)Lcom/ibm/jusb/UsbConfigImp;" );
	configureUsbConfigImp = (*env)->GetStaticMethodID( env, JavaxUsb, "configureUsbConfigImp", "(Lcom/ibm/jusb/UsbConfigImp;BBBBBBBZ)V" );

	dbg( MSG_DEBUG3, "nativeTopologyUpdater.build_config : Building config %d\n", cfg_desc->bConfigurationValue );

	wTotalLength = cfg_desc->wTotalLength;
	pos = cfg_desc->bLength;

	config = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbConfigImp, device );
	(*env)->CallStaticVoidMethod( env, JavaxUsb, configureUsbConfigImp, config,
		cfg_desc->bLength, cfg_desc->bDescriptorType,
		cfg_desc->bNumInterfaces, cfg_desc->bConfigurationValue, cfg_desc->iConfiguration,
		cfg_desc->bmAttributes, cfg_desc->bMaxPower, /* FIXME */ JNI_TRUE );

	while (pos < wTotalLength) {
		desc = get_descriptor( fd );
		if ((!desc) || (2 > desc[0])) {
			dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Short read on descriptor\n" );
			(*env)->DeleteLocalRef( env, config );
			if (interface) (*env)->DeleteLocalRef( env, interface );
			if (desc) free(desc);
			free(cfg_desc);
			return 0;
		}
		pos += desc[0];
		switch( desc[1] ) {
			case USB_DT_DEVICE:
				dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Got device descriptor inside of config descriptor\n" );
			case USB_DT_CONFIG:
				dbg( MSG_ERROR, "nativeTopologyUpdater.build_config : Got config descriptor inside of config descriptor\n" );
				(*env)->DeleteLocalRef( env, config );
				if (interface) (*env)->DeleteLocalRef( env, interface );
				free(desc);
				free(cfg_desc);
				return 0;
				break;

			case USB_DT_INTERFACE:
				interface = build_interface( env, JavaxUsb, config, (struct jusb_interface_descriptor*)desc );
				break;

			case USB_DT_ENDPOINT:
				build_endpoint( env, JavaxUsb, interface, (struct jusb_endpoint_descriptor*)desc );
				break;

			default:
				/* Ignore proprietary descriptor */
				break;
		}
		free(desc);
	}		

	
	if (interface) (*env)->DeleteLocalRef( env, interface );
	(*env)->DeleteLocalRef( env, config );
	free(cfg_desc);
	return 0;
}

static jobject build_interface( JNIEnv *env, jclass JavaxUsb, jobject config, struct jusb_interface_descriptor *if_desc )
{
	jobject interface;

	jmethodID createUsbInterfaceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbInterfaceImp", "(Lcom/ibm/jusb/UsbConfigImp;)Lcom/ibm/jusb/UsbInterfaceImp;" );
	jmethodID configureUsbInterfaceImp = (*env)->GetStaticMethodID( env, JavaxUsb, "configureUsbInterfaceImp", "(Lcom/ibm/jusb/UsbInterfaceImp;BBBBBBBBB)V" );

	dbg( MSG_DEBUG3, "nativeTopologyUpdater.build_interface : Building interface %d\n", if_desc->bInterfaceNumber );

	interface = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbInterfaceImp, config );
	(*env)->CallStaticVoidMethod( env, JavaxUsb, configureUsbInterfaceImp, interface,
		if_desc->bLength, if_desc->bDescriptorType,
		if_desc->bInterfaceNumber, if_desc->bAlternateSetting, if_desc->bNumEndpoints, if_desc->bInterfaceClass,
		if_desc->bInterfaceSubClass, if_desc->bInterfaceProtocol, if_desc->iInterface );

	return interface;
}

static void build_endpoint( JNIEnv *env, jclass JavaxUsb, jobject interface, struct jusb_endpoint_descriptor *ep_desc )
{
	jobject endpoint;

	jmethodID createUsbEndpointImp = (*env)->GetStaticMethodID( env, JavaxUsb, "createUsbEndpointImp", "(Lcom/ibm/jusb/UsbInterfaceImp;)Lcom/ibm/jusb/UsbEndpointImp;" );
	jmethodID configureUsbEndpointImp = (*env)->GetStaticMethodID( env, JavaxUsb, "configureUsbEndpointImp", "(Lcom/ibm/jusb/UsbEndpointImp;BBBBBS)V" );

	dbg( MSG_DEBUG3, "nativeTopologyUpdater.build_endpoint : Building endpoint 0x%2.02x\n", ep_desc->bEndpointAddress );

	if (!interface) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.build_endpoint : Interface is NULL\n");
		return;
	}

	endpoint = (*env)->CallStaticObjectMethod( env, JavaxUsb, createUsbEndpointImp, interface );
	(*env)->CallStaticVoidMethod( env, JavaxUsb, configureUsbEndpointImp, endpoint,
		ep_desc->bLength, ep_desc->bDescriptorType,
		ep_desc->bEndpointAddress, ep_desc->bmAttributes, ep_desc->bInterval, ep_desc->wMaxPacketSize );

	(*env)->DeleteLocalRef( env, endpoint );
}

static jobject add_device( JNIEnv *env, jclass JavaxUsb, jclass LinuxTopologyUpdater, jobject linuxTopologyUpdater, jobject device, unsigned char dev )
{
	char *path = (char*)getcwd( NULL, 0 );
	char *key;
	jobject newDevice;

	jstring keyString;
	jmethodID addUsbDeviceImp = (*env)->GetMethodID( env, LinuxTopologyUpdater, "addUsbDeviceImp", "(Lcom/ibm/jusb/UsbDeviceImp;Ljava/lang/String;)Lcom/ibm/jusb/UsbDeviceImp;" );

	if (!path) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.add_device : Could not get current directory!\n" );
		return JNI_FALSE;
	}

	key = malloc(strlen(path) + 5);

	sprintf( key, "%s/%3.03d", path, dev );
	dbg( MSG_DEBUG3, "nativeTopologyUpdater.add_device : Adding device with key %s\n", key );

	keyString = (*env)->NewStringUTF( env, key );
	newDevice = (*env)->CallObjectMethod( env, linuxTopologyUpdater, addUsbDeviceImp, device, keyString );
	(*env)->DeleteLocalRef( env, keyString );
	free(path);
	free(key);

	return newDevice;
}

static void *get_descriptor( int fd )
{
	unsigned char *buffer, *len;
	int nread;

	len = malloc(1);
	if (1 > read( fd, len, 1 )) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Cannot read from file!\n" );
		free(len);
		return NULL;
	}

	if (*len == 0) {
		dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Zero-length descriptor?\n" );
		free(len);
		return NULL;
	}

	buffer = malloc(*len);
	buffer[0] = *len;
	free(len);

	nread = read( fd, buffer+1, buffer[0]-1 );
	if (buffer[0]-1 != nread) {
		if (buffer[0]-1 > nread) dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Short read on file\n" );
		else dbg( MSG_ERROR, "nativeTopologyUpdater.get_descriptor : Long read on file\n" );
		free(buffer);
		return NULL;
	}

	return buffer;
}
