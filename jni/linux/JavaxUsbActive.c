
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/* This file uses getline(), a GNU extention */
#define _GNU_SOURCE

#include "JavaxUsb.h"

#ifdef INTERFACE_USE_GET_IOCTL
static int interface_use_get_ioctl( int fd, __u8 interface, __u8 setting )
{
	struct usbdevfs_setinterface *iface = NULL;
	int ret = -1;

	if (!(iface = malloc(sizeof(*iface)))) {
		dbg( MSG_ERROR, "interface_use_get_ioctls : Out of memory!\n" );
		goto end;
	}

	iface->interface = interface;
	iface->altsetting = 0xff;

	/* We don't care about the actual error, if any, just if successful */
	if (!ioctl(fd, USBDEVFS_GETINTERFACE, iface))
		ret = (iface->altsetting == setting ? 0 : 1);

end:
	if (iface) free(iface);

	return ret;
}
#endif /* INTERFACE_USE_GET_IOCTL */

#ifdef CONFIG_USE_GET_IOCTL
static int config_use_get_ioctl( int fd, unsigned char config )
{
	unsigned int *cfg = NULL;
	int ret = -1;

	if (!(cfg = malloc(sizeof(*cfg)))) {
		dbg( MSG_ERROR, "config_use_get_ioctls : Out of memory!\n" );
		goto end;
	}

	*cfg = 0;

	/* We don't care about the actual error, if any, just if successful */
	if (!ioctl(fd, USBDEVFS_GETCONFIGURATION, cfg))
		ret = (*cfg == config ? 0 : 1);

end:
	if (cfg) free(cfg);

	return ret;
}
#endif /* CONFIG_USE_GET_IOCTL */

#ifdef CONFIG_USE_DEVICES_FILE
static int config_use_devices_file( unsigned char bus, unsigned char dev, unsigned char config )
{
	FILE *file = NULL;
#define LINELEN 1024
	size_t linelen, len;
	char *line = NULL, busstr[32], devstr[32], cfgstr[32];
	int in_dev = 0;
	int ret = -1;

	if (!(line = malloc(LINELEN))) {
		dbg( MSG_CRITICAL, "use_devices_file : Out of memory!\n" );
		goto end;
	}

	linelen = LINELEN - 1;

	sprintf(busstr, "Bus=%2.2d", bus);
	sprintf(devstr, "Dev#=%3d", dev);
	sprintf(cfgstr, "Cfg#=%2d", config);

#define DEVICES_FILE "/proc/bus/usb/devices"
	errno = 0;
	if (!(file = fopen(DEVICES_FILE, "r"))) {
		dbg( MSG_ERROR, "use_devices_file : Could not open %s : %d\n", DEVICES_FILE, -errno);
		goto end;
	}

	dbg( MSG_DEBUG3, "use_devices_file : Checking %s\n", DEVICES_FILE );

	while (1) {
		memset(line, 0, LINELEN);

		errno = 0;
		if (0 > (len = getline(&line, &linelen, file))) {
			dbg( MSG_ERROR, "use_devices_file : Could not read from %s : %d\n", DEVICES_FILE, -errno);
			break;
		}

		if (!len) {
			dbg( MSG_ERROR, "use_devices_file : No device matching %s/%s found!\n", busstr, devstr );
			break;
		}

		if (strstr(line, "T:")) {
			if (in_dev) {
				dbg( MSG_ERROR, "use_devices_file : No config matching %s found in device %s/%s!\n", cfgstr, busstr, devstr );
				break;
			}
			if (strstr(line, busstr) && strstr(line, devstr)) {
				dbg( MSG_DEBUG1, "use_devices_file : Found section for device %s/%s\n", busstr, devstr );
				in_dev = 1;
				continue;
			}
		}

		if (in_dev) {
			if (strstr(line, cfgstr)) {
				ret = strstr(line, "C:*") ? 0 : 1;
				break;
			}
		}
	}

end:
	if (line) free(line);
	if (file) fclose(file);

	return ret;
}
#endif /* CONFIG_USE_DEVICES_FILE */

jboolean isConfigActive( int fd, unsigned char bus, unsigned char dev, unsigned char config )
{
	int ret = -1; /* -1 = failure, 0 = active, 1 = inactive */
#ifdef CONFIG_USE_GET_IOCTL
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isConfigActive : Checking config %d using GETCONFIGURATION ioctl.\n", config );
		ret = config_use_get_ioctl( fd, config );
		dbg( MSG_DEBUG3, "isConfigActive : GETCONFIGURATION ioctl returned %s\n", (0>ret?"failure":(ret?"inactive":"active")));
	}
#endif
#ifdef CONFIG_USE_DEVICES_FILE
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isConfigActive : Checking config %d using devices file.\n", config );
		ret = config_use_devices_file( bus, dev, config );
		dbg( MSG_DEBUG3, "isConfigActive : devices file returned %s\n", (0>ret?"failure":(ret?"inactive":"active")));
	}
#endif
#ifdef CONFIG_ALWAYS_ACTIVE
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isConfigActive : All configs set to active; no checking.\n" );
		ret = 0;
	}
#endif
	return (!ret ? JNI_TRUE : JNI_FALSE); /* failure defaults to inactive */
}

jboolean isInterfaceSettingActive( int fd, __u8 interface, __u8 setting )
{
	int ret = -1; /* -1 = failure, 0 = active, 1 = inactive */
#ifdef INTERFACE_USE_GET_IOCTL
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isInterfaceSettingActive : Checking interface %d setting %d using GETINTERFACE ioctl.\n", interface, setting );
		ret = interface_use_get_ioctl( fd, interface, setting );
		dbg( MSG_DEBUG3, "isInterfaceSettingActive : GETINTERFACE ioctl returned %s\n", (0>ret?"failure":(ret?"inactive":"active")));
	}
#endif
	return (!ret ? JNI_TRUE : JNI_FALSE); /* failure defaults to inactive */
}

