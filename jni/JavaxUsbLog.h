
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
  if (100 > level) log_named(level,"default",args); else \
  if (200 > level) log_xfer((level-100),args); else \
  if (300 > level) log_hotplug((level-200),args); \
} while(0)

/* Logging levels: */
#define LOG_CRITICAL  0 /* critical messages, this is the default */
#define LOG_ERROR     1 /* error messages */
#define LOG_FUNC      2 /* function entry/exit */
#define LOG_INFO      3 /* function internal */
#define LOG_DEBUG     4 /* debugging */
#define LOG_OTHER     5 /* all other logging */

/* Log data transfers */
#define log_xfer(level,args...) log_named(level,"xfer",args)
#define LOG_XFER_CRITICAL  100 /* critical xfers errors */
#define LOG_XFER_ERROR     101 /* xfer errors */
#define LOG_XFER_DATA      102 /* raw data only */
#define LOG_XFER_META      103 /* metadata (device, endpoint, setup, etc) */
#define LOG_XFER_REQUEST   104 /* request received or completed */
#define LOG_XFER_OTHER     105 /* all other transfer logging */

/* Log hotplug / initialization */
#define log_hotplug(level,args...) log_named(level,"hotplug",args)
#define LOG_HOTPLUG_CRITICAL 200 /* critical hotplug errors */
#define LOG_HOTPLUG_ERROR    201 /* hotplug errors */
#define LOG_HOTPLUG_CHANGE   202 /* connect/disconnect notices */
#define LOG_HOTPLUG_DEVICE   203 /* device information */
#define LOG_HOTPLUG_OTHER    204 /* all other logging */

/* log_named() should not be directly used */
#define DEFAULT_LOG_LEN 256
#define OLD_GLIBC_MAX_LOG_LEN 1024 /* If glibc is 2.0 or lower, snprintf does not report needed length, so set this as max */
#define log_named(level,logname,args...) \
do { \
  char buf1[DEFAULT_LOG_LEN],*buffer = buf1; \
  int real_len; \
  real_len = snprintf(buffer, DEFAULT_LOG_LEN, args); \
  if (0 > real_len || DEFAULT_LOG_LEN <= real_len) { \
    int full_len = (0 > real_len ? OLD_GLIBC_MAX_LOG_LEN : real_len+1); \
    char buf2[full_len]; \
    buffer = buf2; \
    real_len = snprintf(buffer, full_len, args); \
    buffer[full_len-1] = 0; \
  } \
  java_log(env,logname,level,__FILE__,__FUNCTION__,__LINE__,buffer); \
} while (0)

/* Do not use this, use log() */
extern void java_log(JNIEnv *env, char *logname, int level, char *file, char *func, int line, char *msg);

#endif /* _JAVAXUSBLOG_H */

