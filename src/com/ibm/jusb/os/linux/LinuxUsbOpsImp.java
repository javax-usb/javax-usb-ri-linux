package com.ibm.jusb.os.linux;

/*
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.*;
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.util.DefaultRequestV;

/**
 * Abstract superclass for all UsbOpsImp interface and sub-interface implementation
 * @author E. Michael Maximilien
 * @author Dan Streetman
 * @version 1.0.0
 */
class LinuxUsbOpsImp extends Object implements UsbOpsImp
{
	/**
	 * Constructor
	 * @param deviceImp the associated LinuxDeviceImp.
	 */
	public LinuxUsbOpsImp( LinuxDeviceImp deviceImp ) 
	{
        linuxDeviceImp = deviceImp;
	}

	//-------------------------------------------------------------------------
	// Public methods
	//

	/**
	 * Performs a synchronous standard operation by submitting the standard request object
	 * @param request the Request object that is used for this submit
	 * @throws javax.usb.RequestException if something went wrong submitting the request
	 */
	public void syncSubmit( Request request ) throws RequestException
	{
		number++;

		//<temp>Need to create a UsbRequest interface or something to make this cast cleaner</temp>
        ( (AbstractRequest)request ).accept( syncSubmitDcpRequestV );

		if( syncSubmitDcpRequestV.isInException() )
			throw syncSubmitDcpRequestV.getRequestException();
	}

	/**
	 * Performs a synchronous operation by submitting all the Request objects in the bundle.
	 * No other request submission can be overlapped.  This means that the Request object in the
	 * bundle are guaranteed to be sent w/o interruption.
	 * @param requestBundle the RequestBundle object that is used for this submit
	 * @exception javax.usb.RequestException if something goes wrong submitting the request for this operation
	 */
	public void syncSubmit( RequestBundle requestBundle ) throws RequestException
	{
		( (DefaultRequestFactory.MyRequestBundle)requestBundle ).setInSubmission( true );
		
		RequestIterator requestIterator = requestBundle.requestIterator();
		
		try
		{
			while( requestIterator.hasNext() )
				syncSubmit( requestIterator.nextRequest() );
		}
		catch( RequestException re ) { throw re; }
		finally{ ( (DefaultRequestFactory.MyRequestBundle)requestBundle ).setInSubmission( false ); }
	}

	/**
	 * Performs an asynchronous standard operation by submitting the standard request object
	 * @param request the Request object that is used for this submit
	 * @exception javax.usb.RequestException if something goes wrong sumitting the request for this operation
	 */
	public UsbOperations.SubmitResult asyncSubmit( Request request ) throws RequestException
	{
		number++;

		//<temp>Need to create a UsbRequest interface or something to make this cast cleaner</temp>
        ( (AbstractRequest)request ).accept( asyncSubmitDcpRequestV );

		if( asyncSubmitDcpRequestV.isInException() )
			throw asyncSubmitDcpRequestV.getRequestException();		

		return asyncSubmitDcpRequestV.getSubmitResult();
	}

	//-------------------------------------------------------------------------
	// Protected methods
	//

	/**
	 * Does a sync submission of a DCP request that is not a SetConfiguration or SetInterface
	 * request.  These need to be done in Linux with a different IOCTL and thus exposed
	 * as a separate LinuxSetConfigurationRequest and LinuxSetInterfaceRequest
	 * @param request the Request object to submit
	 * @exception javax.usb.RequestException if something went wrong while transmitting
	 */
	protected void syncSubmitDcpRequest( Request request ) throws RequestException
	{
		LinuxDcpRequest dcpRequest = (LinuxDcpRequest)getLinuxDcpRequestFactory().takeLinuxRequest();
		byte[] bytes = request.toBytes();
		byte[] requestData = request.getData();

		dcpRequest.setData( bytes );

		try
		{
			getLinuxDeviceImp().getLinuxDeviceProxy().submitRequest( dcpRequest );
		}
		catch( UsbException ue ) { throw new RequestException( "Error submitting request!", ue ); }

		dcpRequest.waitUntilRequestCompleted();

		if( dcpRequest.getCompletionStatus() < 0 ) 
		{
			int error = dcpRequest.getCompletionStatus();
			dcpRequest.recycle();
			throw new RequestException( JavaxUsb.nativeGetErrorMessage( error ), error );
		} 
		else 
		{
			int xferred = dcpRequest.getCompletionStatus() - 8; /* subtract 8 for setup packet */
			if (xferred > requestData.length) /* This should not happen; should something be done? */
				xferred = requestData.length;
			if ((0 < xferred))
				System.arraycopy(bytes, 8, requestData, 0, xferred);

//FIXME - abstraction layer should be passing down settable Request (something like AbstractRequest)
			((AbstractRequest)request).setDataLength(xferred);
//FIXME
		}

		dcpRequest.recycle();
	}

	/**
	 * Does an async submission of a DCP request that is not a SetConfiguration or SetInterface
	 * request.
	 * @param request the Request object to submit
	 * @exception javax.usb.RequestException if something went wrong while transmitting
	 */
	protected UsbOperations.SubmitResult asyncSubmitDcpRequest( final Request request ) throws RequestException
	{
		final LinuxDcpRequest dcpRequest = (LinuxDcpRequest)getLinuxDcpRequestFactory().takeLinuxRequest();
		final byte[] bytes = request.toBytes();
		final byte[] requestData = request.getData();

		dcpRequest.setData( bytes );

		try
		{
			getLinuxDeviceImp().getLinuxDeviceProxy().submitRequest( dcpRequest );
		}
		catch( UsbException ue ) { throw new RequestException( "Error submitting request!", ue ); }

		final LinuxSubmitResult result = new LinuxSubmitResult(dcpRequest);

		result.setNumber(number);
		result.setRequest(request);
		result.setData(requestData);
		result.setDataLength(requestData.length);

		Runnable r = new Runnable() {
				public void run()
				{
					dcpRequest.waitUntilRequestCompleted();

					if( dcpRequest.getCompletionStatus() < 0 ) 
						{
							int error = dcpRequest.getCompletionStatus();
							dcpRequest.recycle();
						    result.setUsbException( new UsbException( JavaxUsb.nativeGetErrorMessage( error ), error ) );
						} 
					else 
						{
							int xferred = dcpRequest.getCompletionStatus() - 8; /* subtract 8 for setup packet */
							if (xferred > requestData.length) /* This should not happen; should something be done? */
								xferred = requestData.length;
							if ((0 < xferred))
								System.arraycopy(bytes, 8, requestData, 0, xferred);
							
							//FIXME - abstraction layer should be passing down settable Request (something like AbstractRequest)
							((AbstractRequest)request).setDataLength(xferred);
							//FIXME

							result.setDataLength(xferred);
						}

					result.setCompleted(true);

					dcpRequest.recycle();
					
				}
			};

		Thread t = new Thread(r);
		t.setDaemon(true);
		t.setName("Async Request " + result.getNumber());
		t.start();

		return result;
	}

	/**
	 * Does a sync submission of a DCP request for a SetConfiguration
	 * @param request the Request object to submit
	 * @exception javax.usb.RequestException if something went wrong while transmitting
	 */
	protected void syncSubmitDcpSetConfigurationRequest( Request request ) throws RequestException
	{
		LinuxSetConfigurationRequest setConfigRequest = (LinuxSetConfigurationRequest)getLinuxSetConfigurationRequestFactory().takeLinuxRequest();

		setConfigRequest.setConfiguration( (byte)request.getValue() );

		try
		{
			getLinuxDeviceImp().getLinuxDeviceProxy().submitRequest( setConfigRequest );
		}
		catch( UsbException ue ) { throw new RequestException( "Error submitting request!", ue ); }

		setConfigRequest.waitUntilRequestCompleted();

		if( setConfigRequest.getCompletionStatus() < 0 ) 
		{
			int error = setConfigRequest.getCompletionStatus();
			setConfigRequest.recycle();
			throw new RequestException( JavaxUsb.nativeGetErrorMessage( error ), error );
		} 
	}

	/**
	 * Does a sync submission of a DCP request for a SetConfiguration
	 * @param request the Request object to submit
	 * @exception javax.usb.RequestException if something went wrong while transmitting
	 */
	protected void syncSubmitDcpSetInterfaceRequest( Request request ) throws RequestException
	{
		LinuxSetInterfaceRequest setInterfaceRequest = (LinuxSetInterfaceRequest)getLinuxSetInterfaceRequestFactory().takeLinuxRequest();

		setInterfaceRequest.setInterface( (byte)request.getIndex() );
		setInterfaceRequest.setSetting( (byte)request.getValue() );

		try
		{
			getLinuxDeviceImp().getLinuxDeviceProxy().submitRequest( setInterfaceRequest );
		}
		catch( UsbException ue ) { throw new RequestException( "Error submitting request!", ue ); }

		setInterfaceRequest.waitUntilRequestCompleted();

		if( setInterfaceRequest.getCompletionStatus() < 0 ) 
		{
			int error = setInterfaceRequest.getCompletionStatus();
			setInterfaceRequest.recycle();
			throw new RequestException( JavaxUsb.nativeGetErrorMessage( error ), error );
		} 
	}

	/**
	 * Get the LinuxDeviceImp
	 * @return the LinuxDeviceImp
	 */
	protected LinuxDeviceImp getLinuxDeviceImp() { return linuxDeviceImp; }

	/** Get a LinuxRequestFactory for LinuxDcpRequests */
	protected LinuxRequestFactory getLinuxDcpRequestFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getLinuxDcpRequestFactory();
	}

	/** Get a LinuxRequestFactory for LinuxSetInterfaceRequests */
	protected LinuxRequestFactory getLinuxSetInterfaceRequestFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getLinuxSetInterfaceRequestFactory();
	}

	/** Get a LinuxRequestFactory for LinuxSetConfigurationRequests */
	protected LinuxRequestFactory getLinuxSetConfigurationRequestFactory()
	{
		return LinuxUsbServices.getLinuxInstance().getLinuxHelper().getLinuxSetConfigurationRequestFactory();
	}

	//-------------------------------------------------------------------------
	// Instance variables
	//

	private LinuxDeviceImp linuxDeviceImp = null;

	private AsyncSubmitDcpRequestV asyncSubmitDcpRequestV = new AsyncSubmitDcpRequestV();
	private SyncSubmitDcpRequestV syncSubmitDcpRequestV = this.new SyncSubmitDcpRequestV();

	private long number = 0;

	//-------------------------------------------------------------------------
	// Inner classes
	//

	/**
	 * Simple visitor to select the correct syncSubmitDcp method
	 * @author E. Michael Maximilien
	 */
	protected class SyncSubmitDcpRequestV extends DefaultRequestV
	{
		//---------------------------------------------------------------------
		// Public methods
		//

		/** @return the RequestException that occurred during submission */
		public RequestException getRequestException() { return requestException; }

		/** @return true if there was an exception while doing sync submission */
		public boolean isInException() { return ( requestException != null ); }

		//---------------------------------------------------------------------
		// Public visitXyz methods - Uses the default sync submit of LinuxDcpRequest
		//

		public void visitStandardRequest( Request request ) { visitRequest( request ); }

		public void visitVendorRequest( Request request ) { visitRequest( request ); }

		public void visitClassRequest( Request request ) { visitRequest( request ); }

		//---------------------------------------------------------------------
		// Public visitXyz methods - Needs to be done as a separate LinuxRequest
		//

        public void visitSetConfigurationRequest( Request request ) 
		{
			requestException = null;

			try{ LinuxUsbOpsImp.this.syncSubmitDcpSetConfigurationRequest( request ); }
			catch( RequestException e ) { requestException = e; }
		}

		public void visitSetInterfaceRequest( Request request ) 
		{
			requestException = null;

			try{ LinuxUsbOpsImp.this.syncSubmitDcpSetInterfaceRequest( request ); }
			catch( RequestException e ) { requestException = e; }
		}

		//---------------------------------------------------------------------
		// Protected methods
		//

		/**
		 * Default visitRequest method
		 * @param request the Request to visit
		 */
		protected void visitRequest( Request request )
		{
			requestException = null;

			try{ LinuxUsbOpsImp.this.syncSubmitDcpRequest( request ); }
			catch( RequestException e ) { requestException = e; }
		}
		
		//---------------------------------------------------------------------
		// Instance variables
		//

		private RequestException requestException = null;
	}

	/**
	 * Simple visitor to select the correct asyncSubmitDcp method
	 * @author E. Michael Maximilien
	 * @author Dan Streetman
	 */
	protected class AsyncSubmitDcpRequestV extends DefaultRequestV
	{
		public RequestException getRequestException() { return requestException; }
		public boolean isInException() { return ( requestException != null ); }

		public UsbOperations.SubmitResult getSubmitResult() { return submitResult; }

		public void visitStandardRequest( Request request ) { visitRequest( request ); }
		public void visitVendorRequest( Request request ) { visitRequest( request ); }
		public void visitClassRequest( Request request ) { visitRequest( request ); }

        public void visitSetConfigurationRequest( Request request ) 
		{
			requestException = new RequestException( "Async SetConfiguration not possible" );
		}

		public void visitSetInterfaceRequest( Request request ) 
		{
			requestException = new RequestException( "Async SetConfiguration not possible" );
		}

		protected void visitRequest( Request request )
		{
			requestException = null;

			try{ submitResult = LinuxUsbOpsImp.this.asyncSubmitDcpRequest( request ); }
			catch( RequestException e ) { requestException = e; }
		}
		
		private RequestException requestException = null;
		private UsbOperations.SubmitResult submitResult = null;
	}

	public class LinuxSubmitResult implements UsbOperations.SubmitResult
	{
		public LinuxSubmitResult(LinuxRequest r) { linuxRequest = r; }

		public long getNumber() { return number; }

		public Request getRequest() { return request; }

		public byte[] getData() { return data; }

		public int getDataLength() { return dataLength; }

		public boolean isCompleted() { return completed; }

		public UsbException getUsbException() { return usbException; }

		public boolean isInUsbException() { return null != usbException; }

		public void recycle() { }

		public void waitUntilCompleted() { linuxRequest.waitUntilRequestCompleted(); }

		public void waitUntilCompleted(long timeout) { linuxRequest.waitUntilRequestCompleted(/* ignore time.  tough. */); }

		public void setNumber(long n) { number = n; }
		public void setRequest(Request r) { request = r; }
		public void setData(byte[] d) { data = d; }
		public void setDataLength(int l) { dataLength = l; }
		public void setCompleted(boolean c) { completed = c; }
		public void setUsbException(UsbException uE) { usbException = uE; }

		private long number = 0;
		private Request request = null;
		private byte[] data = null;
		private int dataLength = 0;
		private boolean completed = false;
		private UsbException usbException = null;
		private LinuxRequest linuxRequest = null;
	}
}
