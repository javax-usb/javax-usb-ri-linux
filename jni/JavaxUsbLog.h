
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#ifndef _JAVAXUSBLOG_H
#define _JAVAXUSBLOG_H

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

/*
 * Log to Java.
 *
 * This allows logging to Java.
 * This automatically redirects xfer and hotplug logging
 * to that logger, otherwise the default logger is used.
 * The JNIEnv* must be available in the variable "env".
 * @level The log level.
 * @args... The sprintf() format string and replacement parameters.
 */
#define log(level,args...) do { \
  if (LOG_XFER_FLAG&level) log_xfer(level,args); else \
  if (LOG_HOTPLUG_FLAG&level) log_hotplug(level,args); else \
  log_named(level,"default",args); \
} while(0)

#define LOG_LEVEL_MASK    0x00ff
#define LOG_FLAG_MASK     0x0f00
#define LOG_XFER_FLAG     0x0100
#define LOG_HOTPLUG_FLAG  0x0200

/* Logging levels: */
#define LOG_CRITICAL  0x00 /* critical messages, this is the default */
#define LOG_ERROR     0x01 /* error messages */
#define LOG_FUNC      0x02 /* function entry/exit */
#define LOG_INFO      0x03 /* function internal */
#define LOG_DEBUG     0x04 /* debugging */
#define LOG_OTHER     0x05 /* all other logging */

/* Log data transfers */
#define log_xfer(level,args...) log_named(level,"xfer",args)
#define LOG_XFER_CRITICAL  (LOG_XFER_FLAG | 0x00) /* critical xfers errors */
#define LOG_XFER_ERROR     (LOG_XFER_FLAG | 0x01) /* xfer errors */
#define LOG_XFER_DATA      (LOG_XFER_FLAG | 0x02) /* raw data only */
#define LOG_XFER_META      (LOG_XFER_FLAG | 0x03) /* metadata (device, endpoint, setup, etc) */
#define LOG_XFER_REQUEST   (LOG_XFER_FLAG | 0x04) /* request received or completed */
#define LOG_XFER_OTHER     (LOG_XFER_FLAG | 0x05) /* all other transfer logging */

/* Log hotplug / initialization */
#define log_hotplug(level,args...) log_named(level,"hotplug",args)
#define LOG_HOTPLUG_CRITICAL (LOG_HOTPLUG_FLAG | 0x00) /* critical hotplug errors */
#define LOG_HOTPLUG_ERROR    (LOG_HOTPLUG_FLAG | 0x01) /* hotplug errors */
#define LOG_HOTPLUG_CHANGE   (LOG_HOTPLUG_FLAG | 0x02) /* connect/disconnect notices */
#define LOG_HOTPLUG_DEVICE   (LOG_HOTPLUG_FLAG | 0x03) /* device information */
#define LOG_HOTPLUG_OTHER    (LOG_HOTPLUG_FLAG | 0x04) /* all other logging */

/* log_named() should not be directly used */
static char *log_oom = "Out of memory while logging!";
#define DEFAULT_LOG_LEN 256
#define OLD_GLIBC_MAX_LOG_LEN 1024 /* If glibc is 2.0 or lower, snprintf does not report needed length, so set this as max */
#define log_named(level,logname,args...) \
do { \
  char buf1[DEFAULT_LOG_LEN], *buf2 = NULL, *buffer = buf1; \
  int real_len; \
  real_len = snprintf(buffer, DEFAULT_LOG_LEN, args); \
  if (0 > real_len || DEFAULT_LOG_LEN <= real_len) { \
    int full_len = (0 > real_len ? OLD_GLIBC_MAX_LOG_LEN : real_len+1); \
    if (!(buf2 = malloc(full_len))) { \
      buffer = log_oom; \
    } else { \
      buffer = buf2; \
      real_len = snprintf(buffer, full_len, args); \
      buffer[((real_len < full_len-1 && 0 <= real_len) ? real_len : full_len-1)] = 0; \
    } \
  } \
  java_log(env,logname,(LOG_LEVEL_MASK&level),__FILE__,__FUNCTION__,__LINE__,buffer); \
  if (buf2) free(buf2); \
} while (0)

/* Do not use this, use log() */
extern void java_log(JNIEnv *env, char *logname, int level, char *file, char *func, int line, char *msg);

#endif /* _JAVAXUSBLOG_H */

