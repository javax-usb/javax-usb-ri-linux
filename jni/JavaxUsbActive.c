
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
static int config_use_devices_file( JNIEnv *env, unsigned char bus, unsigned char dev, unsigned char config )
{
	FILE *file = NULL;
#define LINELEN 1024
	size_t linelen, len;
	char *line = NULL, busstr[32], devstr[32], cfgstr[32];
	int in_dev = 0;
	int ret = -1;

	if (!(line = malloc(LINELEN))) {
		log( LOG_CRITICAL, "Out of memory!" );
		goto end;
	}

	linelen = LINELEN - 1;

	sprintf(busstr, "Bus=%2.2d", bus);
	sprintf(devstr, "Dev#=%3d", dev);
	sprintf(cfgstr, "Cfg#=%2d", config);

	errno = 0;
	if (!(file = fopen(USBDEVFS_DEVICES, "r"))) {
		log( LOG_HOTPLUG_ERROR, "Could not open %s : %d", USBDEVFS_DEVICES, -errno);
		goto end;
	}

	log( LOG_HOTPLUG_OTHER, "Checking %s", USBDEVFS_DEVICES );

	while (1) {
		memset(line, 0, LINELEN);

		errno = 0;
		if (0 > (len = getline(&line, &linelen, file))) {
			log( LOG_HOTPLUG_ERROR, "Could not read from %s : %d", USBDEVFS_DEVICES, -errno);
			break;
		}

		if (!len) {
			log( LOG_HOTPLUG_ERROR, "No device matching %s/%s found!", busstr, devstr );
			break;
		}

		if (strstr(line, "T:")) {
			if (in_dev) {
				log( LOG_HOTPLUG_ERROR, "No config matching %s found in device %s/%s!", cfgstr, busstr, devstr );
				break;
			}
			if (strstr(line, busstr) && strstr(line, devstr)) {
				log( LOG_HOTPLUG_OTHER, "Found section for device %s/%s", busstr, devstr );
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
static int config_ask_device( JNIEnv *env, int fd, unsigned char config )
{
	int ret = -1;
//FIXME - implement
	return ret;
}
#endif /* CONFIG_SETTING_ASK_DEVICE */

#ifdef INTERFACE_SETTING_ASK_DEVICE
static int interface_ask_device( JNIEnv *env, int fd, __u8 interface, __u8 setting )
{
	int ret = -1;
//FIXME - implement
	return ret;
}
#endif /* INTERFACE_SETTING_ASK_DEVICE */

jboolean isConfigActive( JNIEnv *env, int fd, unsigned char bus, unsigned char dev, unsigned char config )
{
	int ret = -1; /* -1 = failure, 0 = active, 1 = inactive */

#ifdef CONFIG_SETTING_ASK_DEVICE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Checking config %d using GET_CONFIGURATION standard request.", config );
		ret = config_ask_device( env, fd, config );
		log( LOG_HOTPLUG_OTHER, "Device returned %s.", (0>ret?"failure":(ret?"inactive":"active")));
	}
#endif
#ifdef CONFIG_SETTING_USE_DEVICES_FILE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Checking config %d using %s.", config, USBDEVFS_DEVICES );
		ret = config_use_devices_file( env, bus, dev, config );
		log( LOG_HOTPLUG_OTHER, "%s returned %s", USBDEVFS_DEVICES, (0>ret?"failure":(ret?"inactive":"active")) );
	}
#endif
#ifdef CONFIG_SETTING_1_ALWAYS_ACTIVE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "All configs set to active; no checking." );
		ret = 0;
	}
#endif

	return (!ret ? JNI_TRUE : JNI_FALSE); /* failure defaults to inactive */
}

jboolean isInterfaceSettingActive( JNIEnv *env, int fd, __u8 interface, __u8 setting )
{
	int ret = -1; /* -1 = failure, 0 = active, 1 = inactive */

#ifdef INTERFACE_SETTING_ASK_DEVICE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Checking interface %d setting %d using GET_INTERFACE standard request.", interface, setting );
		ret = interface_ask_device( env, fd, interface, setting );
		log( LOG_HOTPLUG_OTHER, "Device returned %s.", (0>ret?"failure":(ret?"inactive":"active")));
	}
#endif
#ifdef INTERFACE_SETTING_USE_DEVICES_FILE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Checking interface %d setting %d using %s.", interface, setting, USBDEVFS_DEVICES );
		ret = interface_use_devices_file( env, bus, dev, interface, setting );
		log( LOG_HOTPLUG_OTHER, "%s returned %s.", USBDEVFS_DEVICES, (0>ret?"failure":(ret?"inactive":"active")) )
	}
#endif

	return (!ret ? JNI_TRUE : JNI_FALSE); /* failure defaults to inactive */
}

