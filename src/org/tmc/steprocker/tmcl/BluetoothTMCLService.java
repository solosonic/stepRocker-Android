/****************************************************************
*
* Copyright (c) 2011, TRINAMIC Motion Control GmbH & Co. KG
* All rights reserved.
*
* +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
*
* This sofware is published under a dual-license: GNU Lesser General
* Public License LGPL 3 and BSD 3-clause license. The dual-license 
* implies that users of this code may choose which terms they prefer.
*
* +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* * Redistributions of source code must retain the above copyright
* notice, this list of conditions and the following disclaimer.
*
* * Redistributions in binary form must reproduce the above copyright
* notice, this list of conditions and the following disclaimer in the
* documentation and/or other materials provided with the distribution.
*
* * Neither the name of the TRINAMIC Motion Control GmbH & Co. KG nor
* the names of its contributors may be used to endorse or promote 
* products derived from this software without specific prior written permission.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License LGPL as
* published by the Free Software Foundation, either version 3 of the
* license, or (at your option) any later version or the BSD license.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License LGPL and the BSD license for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License LGPL and BSD license along with this program.
*
****************************************************************/

package org.tmc.steprocker.tmcl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.tmc.steprocker.ConnectThread;
import org.tmc.steprocker.StepRockerActivity;

public class BluetoothTMCLService
{
	private final boolean DEBUG = false;
	private static final String TAG = "BluetoothTMCLService";
    private static final UUID STD_RFCOMM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// constants that indicate the actual connection state
    public static final int STATE_NONE = 0;       		// we're doing nothing
    public static final int STATE_CONNECTING = 1; 		// now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  		// now connected to a remote device
    public static final int STATE_CONNECTION_ERROR = 3; // could not connect to the remote device

    public static final int MESSAGE_FORWARD_TOAST_TO_PARENT = 0;
    public static final int MESSAGE_STATE_CHANGE = 1;

	private BluetoothSocket mSocket;
	private InputStream btInStream;
	private OutputStream btOutStream;
    private final Handler stepRockerHandler;
    private int mState;
    private ConnectThread connectThread;

    public BluetoothTMCLService(Context context, Handler handler)
    {
    	mState = STATE_NONE;
    	stepRockerHandler = handler;
    	btInStream = null;
    	btOutStream = null;
    }
    
    public synchronized int getState()
    {
    	return mState;
    }

    public void connect(BluetoothDevice device)
    {
        Log.d(TAG, "SR: connecting to " + device.getName() + " - " + device.getAddress());

        this.mState = STATE_CONNECTING;

        // get a BluetoothSocket for a connection with the given BluetoothDevice
        try {
        	this.mSocket = device.createRfcommSocketToServiceRecord(STD_RFCOMM_UUID);
        } catch (IOException e) {
        	Log.e(TAG, "SR: Socket Type: create() failed", e);
        }

        // start a thread to connect with the given remote device
        connectThread = new ConnectThread(BluetoothAdapter.getDefaultAdapter(), device, mSocket, mHandler);
        connectThread.start();
    }
  
    public void disconnect()
    {
    	this.mState = STATE_NONE;
    	try {
    		if (this.btInStream != null)
    			this.btInStream.close();
    		if (this.btOutStream != null)
    			this.btOutStream.close();
    		if (this.mSocket != null)
    			this.mSocket.close();
		} catch (IOException e) {
			Log.e(TAG, "SR: closing socket and streams failed");
		}
    }
 
	 // the Handler that gets information back from the ConnectThread
	 private final Handler mHandler = new Handler()
	 {
		 @Override
		 public void handleMessage(Message msg)
		 {
			 switch (msg.what)
			 {
			 	case MESSAGE_FORWARD_TOAST_TO_PARENT:
					Message newMsg = stepRockerHandler.obtainMessage(StepRockerActivity.MESSAGE_TOAST);
		            newMsg.setData(msg.getData());
		            stepRockerHandler.sendMessage(newMsg);
			 		break;
			 	case MESSAGE_STATE_CHANGE:
	                switch (msg.arg1)
	                {
	                	case BluetoothTMCLService.STATE_CONNECTED:
	                		Log.d(TAG, "SR: service state: connected");
	                		Log.d(TAG, "SR: creating input/output streams");
	                		
	                		// get the BluetoothSocket's input and output streams
	                		try {
	                			btInStream = mSocket.getInputStream();
	                			btOutStream = mSocket.getOutputStream();
	                			mState = STATE_CONNECTED;
	                			
	                			// inform the parent activity
	                			stepRockerHandler.obtainMessage(StepRockerActivity.MESSAGE_STATE_CHANGE, BluetoothTMCLService.STATE_CONNECTED, -1, "").sendToTarget();
	                		} catch (IOException e) {
	                			Log.e(TAG, "SR: input/output sockets not created", e);
	                		}
	                		break;
	                	case BluetoothTMCLService.STATE_CONNECTING:
	                		Log.d(TAG, "SR: service state: connecting...");
	                		break;
	                	case BluetoothTMCLService.STATE_CONNECTION_ERROR:
	                		Log.d(TAG, "SR: service state: connection error");
	                		break;
	                }
	                break;
			 }
		 }
	};
    
	public synchronized TMCLReplyCommand requestTMCLCommand(TMCLRequestCommand request)
	{
		writeTMCLRequest(request);
		return readTMCLReply();		
	}
	
    private void writeTMCLRequest(TMCLRequestCommand request)
    {
    	if (mState != STATE_CONNECTED)
    		return;

    	if (DEBUG)
    		Log.d(TAG, "SR: " + request.toString());
    	this.write(request.getData());    	
    }
    
    private TMCLReplyCommand readTMCLReply()
    {
    	if (mState != STATE_CONNECTED)
    		return null;

    	byte[] buffer = this.read(9);
    	if (buffer != null)
    	{
    		TMCLReplyCommand reply = new TMCLReplyCommand(buffer);
    		if (DEBUG)
    			Log.d(TAG, "SR: " + reply.toString());
    		return reply;
    	} else {
    		return null;
    	}
    }

	public void write(byte[] buffer)
	{
		try {
			btOutStream.write(buffer);
			btOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "SR: Exception during write", e);
		}
	}
	
	public byte[] read(int length)
	{
		byte[] buffer = new byte[9];
		byte[] temp = new byte[9];
		int dataIndex = 0;
		for (int i = 0; i < 20; i++)
		{
			try {
				if (btInStream.available() > 0)
				{
					if (DEBUG)
						Log.d(TAG, "SR: available data " + btInStream.available());
					int readBytes = btInStream.read(temp);
				
					int j;
					for (j = 0; j < readBytes; j++)
					{
						if (dataIndex+j < 9)
							buffer[dataIndex+j] = temp[j];
						else
							break;
					}
					dataIndex += j;
				
					if (dataIndex >= 8)
					{
						byte checksum = 0;
						for (j = 0; j < 8; j++)
							checksum += buffer[j];
						
						if (checksum == buffer[8])
							return buffer;
						else {
							String buff = "";
							for (int k = 0; k < 8; k++)
							{
								buff += buffer[k] + ", ";
							}
							buff += buffer[8];
							Log.d(TAG, "SR: ==> wrong checksum! (" + buff + ")");
							return null;
						} 
					}
				} else {
					if (DEBUG)
						Log.d(TAG, "SR: waiting for data ");
					Thread.sleep(10);
				}
			} catch (IOException e) {
				break;
			} catch (InterruptedException e) {
				break;
			}
		}
		return null;
	}
}
