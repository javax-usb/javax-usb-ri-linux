
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

#ifdef CONFIG_USE_DEVICES_FILE
static int use_devices_file( unsigned char bus, unsigned char dev, unsigned char config );
#endif /* CONFIG_USE_DEVICES_FILE */

jboolean isConfigActive( int fd, unsigned char bus, unsigned char dev, unsigned char config )
{
	int ret = -1; /* -1 = failure, 0 = active, 1 = inactive */
#ifdef CONFIG_USE_DEVICES_FILE
	if (0 > ret) {
		dbg( MSG_DEBUG2, "isConfigActive : Checking config using devices file.\n" );
		ret = use_devices_file( bus, dev, config );
	}
#endif /* CONFIG_USE_DEVICES_FILE */
#ifdef CONFIG_ALWAYS_ACTIVE
	if (0 > ret) {
		dbg( MSG_DEBUG2, "isConfigActive : All configs set to active; no checking.\n" );
		ret = 0;
	}
#endif /* CONFIG_ALWAYS_ACTIVE */
	return (!ret ? JNI_TRUE : JNI_FALSE);
}

#ifdef CONFIG_USE_DEVICES_FILE
static int use_devices_file( unsigned char bus, unsigned char dev, unsigned char config )
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