
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/**
 * Define DEBUG_URB_DATA in order to debug the data in the urb.
 * Warning, this generates a lot of overhead, typically by a factor
 * of 40.
 */
/* #define DEBUG_URB_DATA */
#undef DEBUG_URB_DATA

#ifndef _JAVAXUSBUTIL_H
#define _JAVAXUSBUTIL_H

#include "com_ibm_jusb_os_linux_JavaxUsb.h"
#include "JavaxUsbLog.h"
#include "JavaxUsbChecks.h"

#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/poll.h>
#include <sys/time.h>
#include <sys/dir.h>
#include <dirent.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <pthread.h>
#include <errno.h>

/* Need to include this last or gcc will give warnings */
#include "JavaxUsbKernel.h"

#define USBDEVFS_PATH            "/proc/bus/usb"
#define USBDEVFS_DEVICES         "/proc/bus/usb/devices"
#define USBDEVFS_DRIVERS         "/proc/bus/usb/drivers"

#define USBDEVFS_SPRINTF_NODE    "/proc/bus/usb/%3.03d/%3.03d"
#define USBDEVFS_SSCANF_NODE     "/proc/bus/usb/%3d/%3d"

#define MAX_LINE_LENGTH 255
#define MAX_KEY_LENGTH 255
#define MAX_PATH_LENGTH 255

#define MAX_POLLING_ERRORS 64

/* These must match the defines in JavaxUsb.java */
#define SPEED_UNKNOWN 0
#define SPEED_LOW 1
#define SPEED_FULL 2

//******************************************************************************
// Descriptor structs 

struct jusb_device_descriptor {
	__u8 bLength;
	__u8 bDescriptorType;
	__u16 bcdUSB;
	__u8 bDeviceClass;
	__u8 bDeviceSubClass;
	__u8 bDeviceProtocol;
	__u8 bMaxPacketSize0;
	__u16 idVendor;
	__u16 idProduct;
	__u16 bcdDevice;
	__u8 iManufacturer;
	__u8 iProduct;
	__u8 iSerialNumber;
	__u8 bNumConfigurations;
};

struct jusb_config_descriptor {
	__u8 bLength;
	__u8 bDescriptorType;
	__u16 wTotalLength;
	__u8 bNumInterfaces;
	__u8 bConfigurationValue;
	__u8 iConfiguration;
	__u8 bmAttributes;
	__u8 bMaxPower;
};

struct jusb_interface_descriptor {
	__u8 bLength;
	__u8 bDescriptorType;
	__u8 bInterfaceNumber;
	__u8 bAlternateSetting;
	__u8 bNumEndpoints;
	__u8 bInterfaceClass;
	__u8 bInterfaceSubClass;
	__u8 bInterfaceProtocol;
	__u8 iInterface;
};

struct jusb_endpoint_descriptor {
	__u8 bLength;
	__u8 bDescriptorType;
	__u8 bEndpointAddress;
	__u8 bmAttributes;
	__u16 wMaxPacketSize;
	__u8 bInterval;
};

struct jusb_string_descriptor {
	__u8 bLength;
	__u8 bDescriptorType;
	unsigned char bString[254];
};

//******************************************************************************
// Request methods

int pipe_request( JNIEnv *env, int fd, jobject linuxRequest );
int isochronous_request( JNIEnv *env, int fd, jobject linuxRequest );

void cancel_pipe_request( JNIEnv *env, int fd, jobject linuxRequest );
void cancel_isochronous_request( JNIEnv *env, int fd, jobject linuxRequest );

int complete_pipe_request( JNIEnv *env, jobject linuxRequest );
int complete_isochronous_request( JNIEnv *env, jobject linuxRequest );

int set_configuration( JNIEnv *env, int fd, jobject linuxRequest );
int set_interface( JNIEnv *env, int fd, jobject linuxRequest );

int claim_interface( JNIEnv *env, int fd, int claim, jobject linuxRequest );
int is_claimed( JNIEnv *env, int fd, jobject linuxRequest );

int control_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int bulk_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int interrupt_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int isochronous_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );

int complete_control_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int complete_bulk_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int complete_interrupt_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int complete_isochronous_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );

//******************************************************************************
// Config and Interface active checking methods

/* Pick a way to determine active config.
 *
 * Most of these generate bus traffic to one or more devices.
 * This is BAD when using non-queueing (up to 2.5.44) UHCI Host Controller Driver,
 * as it can interfere with other drivers and the results are unpredictable - ranging
 * from nothing to complete loss of use of the device(s).
 *
 * CONFIG_SETTING_ASK_DEVICE:
 * Asking the device directly is the best available way,
 * as bus traffic is generated only for the specific device in question,
 * and only 1 standard request.
 *
 * CONFIG_SETTING_USE_DEVICES_FILE:
 * Reading/parsing the /proc/bus/usb/devices file generates bus traffic,
 * by asking ALL connected devices for their 3 standard String-descriptors;
 * Manufacturer, Product, and SerialNumber.  This is a lot of bus traffic and
 * can cause problems with any or all connected devices (if using a non-queueing UHCI driver).
 *
 * CONFIG_SETTING_1_ALWAYS_ACTIVE:
 * This does not communicate with the device at all, but always marks the first
 * configuration (number 1, as configs must be numbered consecutively starting with 1)
 * as active.  This should work for all devices, but will produce incorrect results
 * for devices whose active configuration has been changed outside of the current javax.usb
 * instance.
 *
 * All or none may be used, attempts are in order shown, failure moves to the next one.
 * If none are defined (or all fail) then the result will be no configs active, i.e.
 * the device will appear to be (but will not really be) in a Not Configured state.
 *
 * Most people want at least the CONFIG_1_ALWAYS_ACTIVE define, as it is always
 * the last attempted and will do the right thing in many more cases than leaving the
 * device to appear as Not Configured.
 */
#undef CONFIG_SETTING_ASK_DEVICE
#undef CONFIG_SETTING_USE_DEVICES_FILE
#define CONFIG_SETTING_1_ALWAYS_ACTIVE

/* Pick a way to determine active interface alternate setting.
 *
 * INTERFACE_SETTING_ASK_DEVICE:
 * This directly asks the device in the same manner as above.  The only difference is,
 * to communicate with an interface, the interface must be claimed;
 * for a device that already has a driver (which is usually most devices)
 * this will not work since the interface will already be claimed.
 *
 * INTERFACE_SETTING_USE_DEVICES_FILE:
 * This uses the /proc/bus/usb/devices file in the same manner as above.
 * However, until kernel 2.5.XX, the devices file does not provide active
 * interface setting information, so this will fail on those kernels.
 *
 * If none are defined (or all fail) then the result will be first setting is active.
 */
#undef INTERFACE_SETTING_ASK_DEVICE
#undef INTERFACE_SETTING_USE_DEVICES_FILE

jboolean isConfigActive( JNIEnv *env, int fd, unsigned char bus, unsigned char dev, unsigned char config );
jboolean isInterfaceSettingActive( JNIEnv *env, int fd, unsigned char bus, unsigned char dev, __u8 interface, __u8 setting );

//******************************************************************************
// Utility methods

static inline __u16 bcd( __u8 msb, __u8 lsb ) 
{
    return ( (msb << 8) & 0xff00 ) | ( lsb & 0x00ff );
}

static inline int open_device( JNIEnv *env, jstring javaKey, int oflag ) 
{
    const char *node;
    int filed;

    node = (*env)->GetStringUTFChars( env, javaKey, NULL );
    log( LOG_INFO, "Opening node %s", node );
    filed = open( node, oflag );
    (*env)->ReleaseStringUTFChars( env, javaKey, node );
    return filed;
}

static inline int bus_node_to_name( int bus, int node, char *name )
{
	sprintf( name, USBDEVFS_SPRINTF_NODE, bus, node );
	return strlen( name );
}

static inline int get_busnum_from_name( const char *name )
{
	int bus, node;
	if (1 > (sscanf( name, USBDEVFS_SSCANF_NODE, &bus, &node )))
		return -1;
	else return bus;
}

static inline int get_devnum_from_name( const char *name )
{
	int bus, node;
	if (2 > (sscanf( name, USBDEVFS_SSCANF_NODE, &bus, &node )))
		return -1;
	else return node;
}

static inline int select_dirent( const struct dirent *dir_ent, unsigned char type ) 
{
	struct stat stbuf;
	int n;

	stat(dir_ent->d_name, &stbuf);
	if ( 3 != strlen(dir_ent->d_name) || !(DTTOIF(type) & stbuf.st_mode) ) {
		return 0;
	}
	errno = 0;
	n = strtol( dir_ent->d_name, NULL, 10 );
	if ( errno || n < 1 || n > 127 ) {
		errno = 0;
		return 0;
	}
	return 1;
}

static inline int select_dirent_dir( const struct dirent *dir ) { return select_dirent( dir, DT_DIR ); }

static inline int select_dirent_reg( const struct dirent *reg ) { return select_dirent( reg, DT_REG ); }

/**
 * Debug a URB.
 * @env The JNIEnv*.
 * @param calling_method The name of the calling method.
 * @param urb The usbdevfs_urb.
 */
static inline void debug_urb( JNIEnv *env, char *calling_method, struct usbdevfs_urb *urb )
{
	static char hex[] = "0123456789abcdef";  
//FIXME - add device number and/or other dev info
	log( LOG_XFER_OTHER, "%s : URB endpoint = %x", calling_method, urb->endpoint );
	log( LOG_XFER_OTHER, "%s : URB status = %d", calling_method, urb->status );
	log( LOG_XFER_OTHER, "%s : URB signal = %d", calling_method, urb->signr );
	log( LOG_XFER_OTHER, "%s : URB buffer length = %d", calling_method, urb->buffer_length );
	log( LOG_XFER_OTHER, "%s : URB actual length = %d", calling_method, urb->actual_length );
#ifdef DEBUG_URB_DATA
 	if (urb->buffer) { 
 		int i, loglen = strlen(calling_method) + (3*urb->buffer_length) + 15; 
 		char logbuf[loglen], *bufp = logbuf; 
		char* p = (char *)urb->buffer;
 		bufp += sprintf(bufp, "%s : URB data = ", calling_method );
 		for (i=0; i<urb->buffer_length; i++) { 
			int c = *p++;
			*bufp++ = hex[(c>>4)&0xf]; // index to array
			*bufp++ = hex[c&0xf]; // index to array
			*bufp++ = ' '; 
		}
 		log( LOG_XFER_DATA, logbuf ); 
 	} else { 
 		log( LOG_XFER_DATA, "%s : URB data empty", calling_method ); 
 	} 
#endif /* DEBUG_URB_DATA */

}

#endif /* _JAVAXUSBUTIL_H */

