
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#ifndef _JAVAX_USB_KERNEL_H
#define _JAVAX_USB_KERNEL_H

//******************************************************************************
// Kernel-specific

#include <sys/utsname.h>
#include <linux/usbdevice_fs.h>
#include <linux/usb.h>

// The names of this struct's fields change from 2.4 to 2.6.
// But it's binary compatible, so let's just define it here.
struct javaxusb_usbdevfs_ctrltransfer {
	__u8 bmRequestType;
	__u8 bRequest;
	__u16 wValue;
	__u16 wIndex;
	__u16 wLength;
	__u32 timeout;  /* in milliseconds */
	void *data;
};

// This is defined only in later kernel versions
#ifndef USBDEVFS_DISCONNECT
#define USBDEVFS_DISCONNECT        _IO('U', 22)
#endif

int getShortPacketFlag(int accept);

int getIsochronousFlags(void);
int getInterruptFlags(void);
int getControlFlags(void);
int getBulkFlags(void);

int getIsochronousType(void);
int getInterruptType(void);
int getControlType(void);
int getBulkType(void);

#endif /* _JAVAX_USB_KERNEL_H */
