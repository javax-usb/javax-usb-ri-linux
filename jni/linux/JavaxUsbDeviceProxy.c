
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/*
 * JavaxUsbDeviceProxy.c
 *
 * This manages I/O between a LinuxDeviceProxy and usbdevfs device node
 *
 */

void urb_notify_signal( int sig )
{
	dbg( MSG_DEBUG3, "urb_notify_signal : signal %d received by thread 0x%x\n", sig, getpid() );
}

void java_notify_signal( int sig )
{
	dbg( MSG_DEBUG3, "java_notify_signal : signal %d received by thread 0x%x\n", sig, getpid() );
}

#ifndef SIGSUSPEND_WORKS
#define MAX_LOOP_COUNT 20
#endif

/*
 * Proxy for all I/O with a device
 * @author Dan Streetman
 */
JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeDeviceProxy
  ( JNIEnv *env, jclass JavaxUsb, jobject linuxDeviceProxy )
{
	int fd;
	struct usbdevfs_urb *urb;
#ifdef SIGSUSPEND_WORKS
	struct sigaction java_action, urb_action, old_java_action, old_urb_action;
	sigset_t notifymask, oldmask;
#else
	int loop_count;
#endif /* SIGSUSPEND_WORKS */

	jclass LinuxDeviceProxy, LinuxProxyThread, LinuxRequest;
	jobject linuxProxyThread, linuxRequest, usbDevice;
	jstring usbDeviceKey;
	jmethodID startupCompleted, getLinuxProxyThread, setPID, setSignal, dequeueRequestVector, dequeueCancelVector;
	jmethodID getUsbDevice, getUsbDeviceKey, submitNative, abortNative, completeNative, setFileDescriptor;
	jfieldID startupErrnoID, isRunningID;

	LinuxDeviceProxy = (*env)->GetObjectClass( env, linuxDeviceProxy );
	setFileDescriptor = (*env)->GetMethodID( env, LinuxDeviceProxy, "setFileDescriptor", "(I)V" );
	startupErrnoID = (*env)->GetFieldID( env, LinuxDeviceProxy, "startupErrno", "I" );
	isRunningID = (*env)->GetFieldID( env, LinuxDeviceProxy, "isRunning", "Z" );
	startupCompleted = (*env)->GetMethodID( env, LinuxDeviceProxy, "startupCompleted", "()V" );
	getLinuxProxyThread = (*env)->GetMethodID( env, LinuxDeviceProxy, "getLinuxProxyThread", "()Ljava/lang/Thread;" );
	linuxProxyThread = (*env)->CallObjectMethod( env, linuxDeviceProxy, getLinuxProxyThread );
	LinuxProxyThread = (*env)->GetObjectClass( env, linuxProxyThread );
	setPID = (*env)->GetMethodID( env, LinuxProxyThread, "setPID", "(I)V" );
	setSignal = (*env)->GetMethodID( env, LinuxProxyThread, "setSignal", "(I)V" );
	dequeueRequestVector = (*env)->GetMethodID( env, LinuxDeviceProxy, "dequeueRequestVector", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	dequeueCancelVector = (*env)->GetMethodID( env, LinuxDeviceProxy, "dequeueCancelVector", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	getUsbDevice = (*env)->GetMethodID( env, LinuxDeviceProxy, "getUsbDevice", "()Ljavax/usb/UsbDevice;" );
	usbDevice = (*env)->CallObjectMethod( env, linuxDeviceProxy, getUsbDevice );
	getUsbDeviceKey = (*env)->GetStaticMethodID( env, JavaxUsb, "getUsbDeviceKey", "(Ljavax/usb/UsbDevice;)Ljava/lang/String;" );
	usbDeviceKey = (*env)->CallStaticObjectMethod( env, JavaxUsb, getUsbDeviceKey, usbDevice );

	if ( !usbDeviceKey ) {
		dbg( MSG_ERROR, "nativeDeviceProxy : Could not find device!\n" );
		(*env)->SetIntField( env, linuxDeviceProxy, startupErrnoID, -ENODEV );
		(*env)->SetBooleanField( env, linuxDeviceProxy, isRunningID, JNI_FALSE );
		(*env)->CallVoidMethod( env, linuxDeviceProxy, startupCompleted );
		goto DEVICE_PROXY_CLEANUP;
	}

	errno = 0;
	if (0 >= (fd = open_device( env, usbDeviceKey, O_RDWR ))) {
		dbg( MSG_ERROR, "nativeDeviceProxy : Could not open node for device!\n" );
		(*env)->SetIntField( env, linuxDeviceProxy, startupErrnoID, -errno );
		(*env)->SetBooleanField( env, linuxDeviceProxy, isRunningID, JNI_FALSE );
		(*env)->CallVoidMethod( env, linuxDeviceProxy, startupCompleted );
		goto DEVICE_PROXY_CLEANUP;
	}

	(*env)->CallVoidMethod( env, linuxDeviceProxy, setFileDescriptor, fd );

	// Enable signal handler and start blocking signal
#ifdef SIGSUSPEND_WORKS
	java_action.sa_handler = java_notify_signal;
	urb_action.sa_handler = urb_notify_signal;
	sigemptyset(&java_action.sa_mask);
	sigemptyset(&urb_action.sa_mask);
	sigaddset( &java_action.sa_mask, JAVA_NOTIFY_SIGNAL );
	sigaddset( &urb_action.sa_mask, URB_NOTIFY_SIGNAL );
	java_action.sa_flags = 0;
	urb_action.sa_flags = 0;
	sigaction(JAVA_NOTIFY_SIGNAL, &java_action, &old_java_action);
	sigaction(URB_NOTIFY_SIGNAL, &urb_action, &old_urb_action);

	sigemptyset( &notifymask );
	sigemptyset( &oldmask );
	sigaddset( &notifymask, URB_NOTIFY_SIGNAL );
	sigaddset( &notifymask, JAVA_NOTIFY_SIGNAL );
	sigprocmask( SIG_UNBLOCK, &notifymask, NULL );
	sigprocmask( SIG_BLOCK, &notifymask, &oldmask );
#else
	loop_count = 0;
#endif /* SIGSUSPEND_WORKS */

	(*env)->CallVoidMethod( env, linuxProxyThread, setPID, getpid() );
	(*env)->CallVoidMethod( env, linuxProxyThread, setSignal, JAVA_NOTIFY_SIGNAL );

	(*env)->CallVoidMethod( env, linuxDeviceProxy, startupCompleted );

	while (JNI_TRUE == (*env)->GetBooleanField( env, linuxDeviceProxy, isRunningID )) {

#ifdef SIGSUSPEND_WORKS
		dbg( MSG_DEBUG3, "nativeDeviceProxy : Waiting for signal\n" );
		sigsuspend(&oldmask);
		dbg( MSG_DEBUG3, "nativeDeviceProxy : Received signal\n" );
#else
		/* What's going on?  Does sigsuspend simply NOT WORK?!? */
		if ( loop_count > MAX_LOOP_COUNT ) {
		        usleep( 0 );
			loop_count = 0;
		}
		loop_count ++;
#endif /* SIGSUSPEND_WORKS */

		if ((linuxRequest = (*env)->CallObjectMethod( env, linuxDeviceProxy, dequeueRequestVector ))) {
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Got Request\n" );
			LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
			submitNative = (*env)->GetMethodID( env, LinuxRequest, "submitNative", "()V" );
			(*env)->CallVoidMethod( env, linuxRequest, submitNative );
			(*env)->DeleteLocalRef( env, linuxRequest );
			(*env)->DeleteLocalRef( env, LinuxRequest );
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Completed Request\n" );
		}

		if ((linuxRequest = (*env)->CallObjectMethod( env, linuxDeviceProxy, dequeueCancelVector ))) {
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Got Abort Request\n" );
			LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
			abortNative = (*env)->GetMethodID( env, LinuxRequest, "abortNative", "()V" );
			(*env)->CallVoidMethod( env, linuxRequest, abortNative );
			(*env)->DeleteLocalRef( env, linuxRequest );
			(*env)->DeleteLocalRef( env, LinuxRequest );
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Completed Abort Request\n" );
		}

		if (!(ioctl( fd, USBDEVFS_REAPURBNDELAY, &urb ))) {
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Got completed URB\n" );
			linuxRequest = urb->usercontext;
			LinuxRequest = (*env)->GetObjectClass( env, linuxRequest );
			completeNative = (*env)->GetMethodID( env, LinuxRequest, "completeNative", "()V" );
			(*env)->CallVoidMethod( env, linuxRequest, completeNative );
			(*env)->DeleteGlobalRef( env, linuxRequest );
			(*env)->DeleteLocalRef( env, LinuxRequest );
			dbg( MSG_DEBUG1, "nativeDeviceProxy : Finished completed URB\n" );
		}

	}

	dbg( MSG_DEBUG2, "nativeDeviceProxy : Proxy finished, closing device\n" );

#ifdef SIGSUSPEND_WORKS
	sigprocmask( SIG_UNBLOCK, &notifymask, NULL );
#endif /* SIGSUSPEND_WORKS */

	close( fd );

DEVICE_PROXY_CLEANUP:
	(*env)->DeleteLocalRef( env, LinuxDeviceProxy );
	(*env)->DeleteLocalRef( env, linuxProxyThread );
	(*env)->DeleteLocalRef( env, LinuxProxyThread );
	(*env)->DeleteLocalRef( env, usbDevice );
	if (usbDeviceKey) (*env)->DeleteLocalRef( env, usbDeviceKey );
}

/** Signal a process */
JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSignalPID
  (JNIEnv *env, jclass JavaxUsb, jint pid, jint sig)
{
#ifdef SIGSUSPEND_WORKS
	pid_t interfaceThread = (pid_t)pid;

	dbg( MSG_DEBUG3, "nativeSignalPID : sending signal %d to thread 0x%x\n", (int)sig, (int)pid );

	errno = 0;
	if (kill( interfaceThread, sig ))
		dbg( MSG_ERROR, "nativeSignalPID : unable to send signal %d to thread 0x%x\n", (int)sig, (int)pid );
#endif /* SIGSUSPEND_WORKS */
}

inline void debug_urb( char *calling_method, struct usbdevfs_urb *urb )
{
	int i;

	dbg( MSG_DEBUG3, "%s : URB endpoint = %x\n", calling_method, urb->endpoint );
	dbg( MSG_DEBUG3, "%s : URB status = %d\n", calling_method, urb->status );
	dbg( MSG_DEBUG3, "%s : URB signal = %d\n", calling_method, urb->signr );
	dbg( MSG_DEBUG3, "%s : URB buffer length = %d\n", calling_method, urb->buffer_length );
	dbg( MSG_DEBUG3, "%s : URB actual length = %d\n", calling_method, urb->actual_length );
	if (urb->buffer) {
		dbg( MSG_DEBUG3, "%s : URB data = ", calling_method );
		for (i=0; i<urb->buffer_length; i++) dbg( MSG_DEBUG3, "%2.2x ", ((unsigned char *)urb->buffer)[i] );
		dbg( MSG_DEBUG3, "\n" );
	} else {
		dbg( MSG_DEBUG3, "%s : URB data empty!\n", calling_method );
	}

}

