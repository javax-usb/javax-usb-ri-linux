
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

int msg_level = MSG_MIN;

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetMsgLevel
	(JNIEnv *env, jclass JavaxUsb, jint level)
{
	if ( MSG_MIN <= level && level <= MSG_MAX )
		msg_level = level;
}

__u16 bcd( __u8 msb, __u8 lsb ) 
{
    return ( (msb << 8) & 0xff00 ) | ( lsb & 0x00ff );
}

int open_device( JNIEnv *env, jstring javaKey, int oflag ) 
{
    const char *node;
    int filed;

    node = (*env)->GetStringUTFChars( env, javaKey, NULL );
    dbg( MSG_DEBUG1, "Opening node %s\n", node );
    filed = open( node, oflag );
    (*env)->ReleaseStringUTFChars( env, javaKey, node );
    return filed;
}

int bus_node_to_name( int bus, int node, char *name )
{
	sprintf( name, USBDEVFS_SPRINTF_NODE, bus, node );
	return strlen( name );
}

int get_busnum_from_name( const char *name )
{
	int bus, node;
	if (1 > (sscanf( name, USBDEVFS_SSCANF_NODE, &bus, &node )))
		return -1;
	else return bus;
}

int get_devnum_from_name( const char *name )
{
	int bus, node;
	if (2 > (sscanf( name, USBDEVFS_SSCANF_NODE, &bus, &node )))
		return -1;
	else return node;
}

int select_dirent_dir( const struct dirent *dir ) { return select_dirent( dir, DT_DIR ); }

int select_dirent_reg( const struct dirent *reg ) { return select_dirent( reg, DT_REG ); }

int select_dirent( const struct dirent *dir_ent, unsigned char type ) 
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
