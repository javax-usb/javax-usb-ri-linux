
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
