
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

#include <linux/usbdevice_fs.h>
#include <linux/usb.h>

#if defined (USBDEVFS_URB_DISABLE_SPD)
#define NO_ACCEPT_SHORT_PACKET USBDEVFS_URB_DISABLE_SPD
#elif defined (USBDEVFS_URB_SHORT_NOT_OK)
#define NO_ACCEPT_SHORT_PACKET USBDEVFS_URB_SHORT_NOT_OK
#else
#error Could not find definition for disabling short packets
#endif

/* check kernel version */
#define INTERRUPT_USES_BULK

#if defined (USBDEVFS_URB_QUEUE_BULK)
#define QUEUE_BULK USBDEVFS_URB_QUEUE_BULK
#endif

#if defined (USBDEVFS_GETCONFIGURATION) && defined (USBDEVFS_GETINTERFACE)
# define CAN_USE_GET_IOCTLS
#endif

#endif /* _JAVAX_USB_KERNEL_H */
