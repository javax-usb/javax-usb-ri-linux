
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#ifndef _JAVAUSBUTIL_H
#define _JAVAUSBUTIL_H

#include "com_ibm_jusb_os_linux_JavaxUsb.h"
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/poll.h>
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
#include <linux/usbdevice_fs.h>
#include <linux/usb.h>

/**
 * Sigsuspend isn't working for me.  Maybe I don't understand it right...
 * but until I can get it to correctly function, this uses a 1 jiffy (10ms on x86) delay busy wait.
 * Define SIGSUSPEND_WORKS to use sigsuspend instead.
 */
#undef SIGSUSPEND_WORKS
/* And this is included for the 1 ms delay busy wait. */
#include<sys/time.h>

#ifndef USBDEVFS_URB_QUEUE_BULK
#error *******************************************************************
#error *******************************************************************
#error ** Cannot find USBDEVFS_URB_QUEUE_BULK in your kernel headers.   **
#error ** The Linux 2.4.0-test9 or later kernel headers must be located **
#error ** or linked to under /usr/include/linux.                        **
#error ** To do this you can install linux in /usr/src/linux            **
#error ** and link /usr/include/linux to /usr/src/linux/include/linux   **
#error ** Questions : ddstreet@us.ibm.com or ddstreet@ieee.org          **
#error *******************************************************************
#error *******************************************************************
#endif

#define MSG_OFF -1
#define MSG_CRITICAL 0
#define MSG_ERROR 1
#define MSG_WARNING 2
#define MSG_NOTICE 3
#define MSG_INFO 4
#define MSG_DEBUG1 5
#define MSG_DEBUG2 6
#define MSG_DEBUG3 7

#define MSG_MIN MSG_OFF
#define MSG_MAX MSG_DEBUG3

#ifdef NO_DEBUG
#	define dbg(lvl, args...)		do { } while(0)
#else
#	define dbg(lvl, args...)		do { if ( lvl <= msg_level ) printf( args ); } while(0)
#endif /* NO_DEBUG */

#define USBDEVFS_PATH            "/proc/bus/usb"
#define USBDEVFS_DEVICES         "/proc/bus/usb/devices"
#define USBDEVFS_DRIVERS         "/proc/bus/usb/drivers"

#define USBDEVFS_SPRINTF_NODE    "/proc/bus/usb/%3.03d/%3.03d"
#define USBDEVFS_SSCANF_NODE     "/proc/bus/usb/%3d/%3d"

#define MAX_LINE_LENGTH 255
#define MAX_KEY_LENGTH 255
#define MAX_PATH_LENGTH 255

#define MAX_POLLING_ERRORS 64
#define URB_NOTIFY_SIGNAL SIGRTMIN+13
#define JAVA_NOTIFY_SIGNAL SIGRTMIN+14

extern int msg_level;

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

inline __u16 bcd( __u8 msb, __u8 lsb );

inline int open_device( JNIEnv *env, jstring jkey, int oflag );

inline int bus_node_to_name( int bus, int node, char *name );
inline int get_busnum_from_name( const char *name );
inline int get_devnum_from_name( const char *name );

inline int select_dirent_dir( const struct dirent *dir );
inline int select_dirent_reg( const struct dirent *reg );
inline int select_dirent( const struct dirent *dir_ent, unsigned char type );

inline void check_for_exception( JNIEnv *env );

inline void debug_urb( char *calling_method, struct usbdevfs_urb *urb );

#endif /* _JAVAUSBUTIL_H */

