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

package org.tmc.steprocker;

import java.io.IOException;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.tmc.steprocker.tmcl.BluetoothTMCLService;

public class ConnectThread extends Thread
{
    private static final String TAG = "ConnectThread";

    private final BluetoothAdapter mAdapter;
	private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final Handler tmclHandler;

    public ConnectThread(BluetoothAdapter adapter, BluetoothDevice device, BluetoothSocket socket, Handler handler)
    {
    	this.mAdapter = adapter;
    	this.mmDevice = device;
        this.mmSocket = socket;
    	this.tmclHandler = handler;
    }

    public void run()
    {
        // cancel discovery because it will slow down a connection
        mAdapter.cancelDiscovery();

        // make a connection to the Bluetooth socket
        try {
            // this is a blocking call and will only return on a successful connection or an exception
            mmSocket.connect();

            Log.d(TAG, "SR: Connected to: " + mmDevice.getName() + " " + mmDevice.getAddress());
            
            // show connected toast
            Message msg = tmclHandler.obtainMessage(BluetoothTMCLService.MESSAGE_FORWARD_TOAST_TO_PARENT);
            Bundle bundle = new Bundle();
            bundle.putString(StepRockerActivity.TOAST, "Connected to: " + mmDevice.getName() + " " + mmDevice.getAddress());
            msg.setData(bundle);
            tmclHandler.sendMessage(msg);

            // inform the BluetoothTMCLService
            tmclHandler.obtainMessage(BluetoothTMCLService.MESSAGE_STATE_CHANGE, BluetoothTMCLService.STATE_CONNECTED, -1, "").sendToTarget();
        } catch (IOException e) {
        	// connection failed
        	// close the socket 
            try {
            	Log.e(TAG, "SR: closing socket because of connection failure", e);
                mmSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "SR: unable to close() socket during connection failure", e2);
                tmclHandler.obtainMessage(BluetoothTMCLService.MESSAGE_STATE_CHANGE, BluetoothTMCLService.STATE_CONNECTION_ERROR, -1, "").sendToTarget();
            }
            // show a connection failure toast
            Message msg = tmclHandler.obtainMessage(BluetoothTMCLService.MESSAGE_FORWARD_TOAST_TO_PARENT);
            Bundle bundle = new Bundle();
            bundle.putString(StepRockerActivity.TOAST, "Unable to connect to: " + mmDevice.getName() + " " + mmDevice.getAddress());
            msg.setData(bundle);
            tmclHandler.sendMessage(msg);
            return;
        }
    }
}
