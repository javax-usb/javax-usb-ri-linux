
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

#ifdef CONFIG_SETTING_USE_DEVICES_FILE
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

	errno = 0;
	if (!(file = fopen(USBDEVFS_DEVICES, "r"))) {
		dbg( MSG_ERROR, "use_devices_file : Could not open %s : %d\n", USBDEVFS_DEVICES, -errno);
		goto end;
	}

	dbg( MSG_DEBUG3, "use_devices_file : Checking %s\n", USBDEVFS_DEVICES );

	while (1) {
		memset(line, 0, LINELEN);

		errno = 0;
		if (0 > (len = getline(&line, &linelen, file))) {
			dbg( MSG_ERROR, "use_devices_file : Could not read from %s : %d\n", USBDEVFS_DEVICES, -errno);
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
#endif /* CONFIG_SETTING_USE_DEVICES_FILE */

#ifdef CONFIG_SETTING_ASK_DEVICE
static int config_ask_device( int fd, unsigned char config )
{
	int ret = -1;
//FIXME - implement
	return ret;
}
#endif /* CONFIG_SETTING_ASK_DEVICE */

#ifdef INTERFACE_SETTING_ASK_DEVICE
static int interface_ask_device( int fd, __u8 interface, __u8 setting )
{
	int ret = -1;
//FIXME - implement
	return ret;
}
#endif /* INTERFACE_SETTING_ASK_DEVICE */

jboolean isConfigActive( int fd, unsigned char bus, unsigned char dev, unsigned char config )
{
	int ret = -1; /* -1 = failure, 0 = active, 1 = inactive */

#ifdef CONFIG_SETTING_ASK_DEVICE
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isConfigActive : Checking config %d using GET_CONFIGURATION standard request.\n", config );
		ret = config_ask_device( fd, config );
		dbg( MSG_DEBUG3, "isConfigActive : Device returned %s.\n", (0>ret?"failure":(ret?"inactive":"active")));
	}
#endif
#ifdef CONFIG_SETTING_USE_DEVICES_FILE
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isConfigActive : Checking config %d using %s.\n", config, USBDEVFS_DEVICES );
		ret = config_use_devices_file( bus, dev, config );
		dbg( MSG_DEBUG3, "isConfigActive : %s returned %s\n", USBDEVFS_DEVICES, (0>ret?"failure":(ret?"inactive":"active")) );
	}
#endif
#ifdef CONFIG_SETTING_1_ALWAYS_ACTIVE
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

#ifdef INTERFACE_SETTING_ASK_DEVICE
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isInterfaceSettingActive : Checking interface %d setting %d using GET_INTERFACE standard request.\n", interface, setting );
		ret = interface_ask_device( fd, interface, setting );
		dbg( MSG_DEBUG3, "isInterfaceSettingActive : Device returned %s.\n", (0>ret?"failure":(ret?"inactive":"active")));
	}
#endif
#ifdef INTERFACE_SETTING_USE_DEVICES_FILE
	if (0 > ret) {
		dbg( MSG_DEBUG3, "isInterfaceSettingActive : Checking interface %d setting %d using %s.\n", interface, setting, USBDEVFS_DEVICES );
		ret = interface_use_devices_file( bus, dev, interface, setting );
		dbg( MSG_DEBUG3, "isInterfaceSettingActive : %s returned %s.\n", USBDEVFS_DEVICES, (0>ret?"failure":(ret?"inactive":"active")) )
	}
#endif

	return (!ret ? JNI_TRUE : JNI_FALSE); /* failure defaults to inactive */
}

