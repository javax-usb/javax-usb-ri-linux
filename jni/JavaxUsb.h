
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#ifndef _JAVAXUSBUTIL_H
#define _JAVAXUSBUTIL_H

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

#include "JavaxUsbKernel.h"
#include "com_ibm_jusb_os_linux_JavaxUsb.h"

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

// This will cause COMPLETE JVM EXIT when any JNI exception happens.
// Kinda extreme, but when you realize JVM crash is the likely alternative,
// you might as well exit cleanly, since you know exactly what the problem/exception is.
#define EXIT_ON_EXCEPTION

#ifdef NO_DEBUG
#	define dbg(lvl, args...)		do { } while(0)
#else
extern int msg_level;
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
int dcp_request( JNIEnv *env, int fd, jobject linuxRequest );
int isochronous_request( JNIEnv *env, int fd, jobject linuxRequest );

void cancel_pipe_request( JNIEnv *env, int fd, jobject linuxRequest );
void cancel_dcp_request( JNIEnv *env, int fd, jobject linuxRequest );
void cancel_isochronous_request( JNIEnv *env, int fd, jobject linuxRequest );

int complete_pipe_request( JNIEnv *env, jobject linuxRequest );
int complete_dcp_request( JNIEnv *env, jobject linuxRequest );
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

// Pick a way to determine active config.
// The devices file generates bus traffic, which is BAD when using non-queueing
// (up to 2.5.44) UHCI Host Controller Driver.
// The ioctl is the best way, but not available in all kernels.
// All or none may be used, attempts are in order shown, failure moves to the next one.
// If none are defined (or all fail) then the result will be no configs active.
// Most people want at least the CONFIG_ALWAYS_ACTIVE define.
#ifdef CAN_USE_GET_IOCTLS
# define CONFIG_USE_GET_IOCTL
#endif
#undef CONFIG_USE_DEVICES_FILE
#define CONFIG_ALWAYS_ACTIVE

// Pick a way to determine active interface alternate setting.
// Without the ioctl, there is NO way to determine it.
// If this is not defined (or fails) then the result will be first setting is active.
#ifdef CAN_USE_GET_IOCTLS
# define INTERFACE_USE_GET_IOCTL
#endif

jboolean isConfigActive( int fd, unsigned char bus, unsigned char dev, unsigned char config );
jboolean isInterfaceSettingActive( int fd, __u8 interface, __u8 setting );

//******************************************************************************
// Utility methods

static inline void check_for_exception( JNIEnv *env ) 
{
	jthrowable e;

	if (!(e = (*env)->ExceptionOccurred( env ))) return;
	dbg( MSG_CRITICAL, "Exception occured!\n" );
#ifdef EXIT_ON_EXCEPTION
	exit(1);
#endif
}

static inline __u16 bcd( __u8 msb, __u8 lsb ) 
{
    return ( (msb << 8) & 0xff00 ) | ( lsb & 0x00ff );
}

static inline int open_device( JNIEnv *env, jstring javaKey, int oflag ) 
{
    const char *node;
    int filed;

    node = (*env)->GetStringUTFChars( env, javaKey, NULL );
    dbg( MSG_DEBUG1, "Opening node %s\n", node );
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
 * @param calling_method The name of the calling method.
 * @param urb The usbdevfs_urb.
 */
static inline void debug_urb( char *calling_method, struct usbdevfs_urb *urb )
{
	int i;

	dbg( MSG_DEBUG3, "%s : URB endpoint = %x\n", calling_method, urb->endpoint );
	dbg( MSG_DEBUG3, "%s : URB status = %d\n", calling_method, urb->status );
	dbg( MSG_DEBUG3, "%s : URB signal = %d\n", calling_method, urb->signr );
	dbg( MSG_DEBUG3, "%s : URB buffer length = %d\n", calling_method, urb->buffer_length );
	dbg( MSG_DEBUG3, "%s : URB actual length = %d\n", calling_method, urb->actual_length );
	if (urb->buffer) {
		dbg( MSG_DEBUG3, "%s : URB data = ", calling_method );
		for (i=0; i<urb->buffer_length; i++) dbg( MSG_DEBUG3, "%2.2x ", ((unsigned char *)urb->buffer)[i] );
		dbg( MSG_DEBUG3, "\n" );
	} else {
		dbg( MSG_DEBUG3, "%s : URB data empty!\n", calling_method );
	}

}

#endif /* _JAVAXUSBUTIL_H */

