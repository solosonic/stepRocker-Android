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

import java.util.Set;
import java.util.Vector;

import org.tmc.steprocker.graph.Chart;
import org.tmc.steprocker.graph.LoadAngleDrawerView;
import org.tmc.steprocker.graph.SmartCurrentDrawerView;
import org.tmc.steprocker.graph.TorqueDrawerView;
import org.tmc.steprocker.graph.DrawerView;
import org.tmc.steprocker.tmcl.BluetoothTMCLService;
import org.tmc.steprocker.tmcl.TMCLReplyCommand;
import org.tmc.steprocker.tmcl.TMCLRequestCommand;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class StepRockerActivity extends Activity implements OnTouchListener, OnClickListener
{
	private static final String TAG = "StepRocker";
	
	// message types sent from the BluetoothTMCLService handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
	
    // request codes
    private static final int REQUEST_ENABLE_BT = 3;
    public static final int CHECK_VELOCITY = 0;
    public static final int CHECK_TORQUE = 1;
    public static final int CHECK_LOAD_ANGLE = 2;
	
	ViewFlipper flipper;
	private int xTouchDown = 0;
	private int actualView = 0;
	
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothTMCLService mTMCLService = null;
	
	// name of the connected device
    private String mConnectedDeviceName = null;
    
    // key names received from the BluetoothTMCLService handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

	private Button btnSelectDevice, btnExit, btnScanDevices, btnCancelScan;
	private Button btnROLVelocityDrawer, btnMotorStopVelocityDrawer, btnRORVelocityDrawer;
	private Button btnEnableDCStep, btnEnableCoolStep;

	private LinearLayout layoutDeviceSelector;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;
	private ValueCheckerThread velocityValueChecker, torqueValueChecker, loadValueChecker;
    private DrawerView velocityDrawer;
    private TorqueDrawerView torqueDrawer;
    private LoadAngleDrawerView loadAngleDrawer;
    private SmartCurrentDrawerView smartCurrentDrawer;
    private Chart velocityDrawerVelocityChart, defaultTorqueChart;
	private int maxSpeed = 2047;//2047;
	private int maxDCStepSpeed = 900;//799;
		
	private boolean liveMeasurementMode = true;//false;
	private float xVelocityDrawer = 0;
	private float xTorqueDrawer = 0;
	private float demoLoadAngle = 0;
	private float demoSmartCurrent = 0;
	private boolean demoSmartCurrentUp = true;
		
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.main);

        flipper = (ViewFlipper) findViewById(R.id.flipper);
        flipper.setOnTouchListener(this);
        
        // use the TMC standard font
        Typeface tmc_font = Typeface.createFromAsset(this.getAssets(), "fonts/MaOr_Bd.ttf");

        // change the font of the TextViews
        ((TextView)findViewById(R.id.tvPairedDevices)).setTypeface(tmc_font);
        ((TextView)findViewById(R.id.tvNewDevices)).setTypeface(tmc_font);

        // get the buttons
        this.btnSelectDevice = (Button)findViewById(R.id.btnSelectDevice);
        this.btnSelectDevice.setOnClickListener(this);
        this.btnExit = (Button)findViewById(R.id.btnExit);
        this.btnExit.setOnClickListener(this);
        this.btnScanDevices = (Button)findViewById(R.id.btnScanDevices);
        this.btnScanDevices.setOnClickListener(this);
        this.btnCancelScan = (Button)findViewById(R.id.btnCancelScan);
        this.btnCancelScan.setOnClickListener(this);
        
		// drawer control buttons
		this.btnROLVelocityDrawer = (Button)findViewById(R.id.btnROLVelocityDrawer);
		this.btnROLVelocityDrawer.setOnClickListener(this);
		this.btnMotorStopVelocityDrawer = (Button)findViewById(R.id.btnMotorStopVelocityDrawer);
		this.btnMotorStopVelocityDrawer.setOnClickListener(this);
		this.btnRORVelocityDrawer = (Button)findViewById(R.id.btnRORVelocityDrawer);
		this.btnRORVelocityDrawer.setOnClickListener(this);
        
		// dcStep button
		this.btnEnableDCStep = (Button)findViewById(R.id.btnEnableDCStep);
		this.btnEnableDCStep.setOnClickListener(this);
		
		// stall guard button
		this.btnEnableCoolStep = (Button)findViewById(R.id.btnEnableCoolStep);
		this.btnEnableCoolStep.setOnClickListener(this);
		
        // change the font of the buttons
        this.btnSelectDevice.setTypeface(tmc_font);
        this.btnExit.setTypeface(tmc_font);
        this.btnScanDevices.setTypeface(tmc_font);
        this.btnCancelScan.setTypeface(tmc_font);
        this.btnEnableDCStep.setTypeface(tmc_font);
        this.btnEnableCoolStep.setTypeface(tmc_font);

        // get the layout for the device selector
        this.layoutDeviceSelector = (LinearLayout)findViewById(R.id.layoutDeviceSelector);
        
        // initialize the array adapters used for already paired 
        // devices and newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        
        // find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.pairedDevices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.newDevices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

		// velocity drawer
		this.velocityDrawer = (DrawerView)findViewById(R.id.velocityDrawer);
		this.velocityDrawer.setYAxisCaption("");
		this.velocityDrawer.setTypeface(tmc_font);
		this.velocityDrawer.setMaxValue(this.maxDCStepSpeed);
		this.velocityDrawer.setMinValue(-(this.maxDCStepSpeed));
		this.velocityDrawer.setScaleCount(8);
		this.velocityDrawer.setDxPerPoint(20);
		this.velocityDrawer.setConnectDataPoints(true);
		
		// add some charts to the velocity drawer
		this.velocityDrawerVelocityChart = new Chart("actual velocity", (byte)3, Color.BLUE);
		this.velocityDrawerVelocityChart.setEnabled(false);
		this.velocityDrawer.addChart(velocityDrawerVelocityChart);
		this.velocityDrawer.addChart("actual load", (byte)206, Color.GREEN);

		// torque <-> velocity drawer
		this.torqueDrawer = (TorqueDrawerView)findViewById(R.id.torqueDrawer);
		this.torqueDrawer.setYAxisCaption("Torque");
		this.torqueDrawer.setXAxisCaption("Velocity");
		this.torqueDrawer.setTypeface(tmc_font);
		this.torqueDrawer.setVelocityCommand((byte)3);
		this.torqueDrawer.setMaxXValue(2047);
		this.torqueDrawer.setMinXValue(0);
		this.torqueDrawer.setMaxYValue(3929);
		this.torqueDrawer.setMinYValue(0);
		this.torqueDrawer.setXScaleCount(5);
		
		// add charts to the torque drawer
		defaultTorqueChart = new Chart("torque", (byte)0, Color.BLACK);
		defaultTorqueChart.setMaxValues(this.maxSpeed+1);
		for (int i = this.maxSpeed+1; i >= 0; i--)
			defaultTorqueChart.addValue(78*(float)Math.sqrt(i) + 400);

		this.torqueDrawer.addChart(defaultTorqueChart);
		this.torqueDrawer.addChart(new Chart("velocity", (byte)0, Color.GREEN));

		// load angle drawer
		this.loadAngleDrawer = (LoadAngleDrawerView)findViewById(R.id.loadAngleDrawer);
		this.loadAngleDrawer.setLoadAngleCommand((byte)206);
		this.loadAngleDrawer.setDescription("load angle");
		this.loadAngleDrawer.setTypeface(tmc_font);
		
		// smart current drawer
		this.smartCurrentDrawer = (SmartCurrentDrawerView)findViewById(R.id.smartCurrentDrawer);
		this.smartCurrentDrawer.setSmartCurrentCommand((byte)180);
		this.smartCurrentDrawer.setDescription("smartCurrent");
		this.smartCurrentDrawer.setTypeface(tmc_font);
		
		// get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // if the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            return;
        } else {
	        // get a set of currently paired devices
	        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
	        
	        // if there are paired devices, add each one to the ArrayAdapter
	        if (pairedDevices.size() > 0)
	        {
	            for (BluetoothDevice device : pairedDevices)
	                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	        } else {
	            mPairedDevicesArrayAdapter.add("no paired devices found");
	        }
        }
    }

    @Override
	 public void onStart()
	 {
		 super.onStart();

		 Log.d(TAG, "SR: .............................................");

		 // request to enable Bluetooth if not already switched on
		 if (!mBluetoothAdapter.isEnabled())
		 {
			 Log.d(TAG, "SR: enabling Bluetooth...");
			 Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			 startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		 }
		 
		 if (mTMCLService == null)
		 {
			 Log.d(TAG, "SR: starting TMCL Bluetooth service...");
			 mTMCLService = new BluetoothTMCLService(this, mHandler);
		 }
	 }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	switch (requestCode)
    	{
        	case REQUEST_ENABLE_BT:
        		// the request to enable Bluetooth returns
        		if (resultCode == Activity.RESULT_OK)
        		{
        			// Bluetooth enabled, so the communication can be started
        			Log.d(TAG, "SR: Bluetooth enabled.");
        			Toast.makeText(this, "Bluetooth enabled.", Toast.LENGTH_LONG).show();
        		} else {
        			// user has not enabled Bluetooth or an error occurred
        			Log.d(TAG, "SR: Bluetooth not enabled");
        			Toast.makeText(this, "Bluetooth not enabled!", Toast.LENGTH_LONG).show();
        		}
    	}
    }

    // the Handler that gets information back from the BluetoothTMCLService
    private final Handler mHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		switch (msg.what)
    		{
    			case MESSAGE_STATE_CHANGE:
    				switch (msg.arg1)
    				{
    					case BluetoothTMCLService.STATE_CONNECTED:
    						Log.d(TAG, "SR: State: connected");
	                        
    						// set default values
    						setDefaultValues();
    					
    						Toast.makeText(getApplicationContext(), "Connected and ready setting default values.", Toast.LENGTH_LONG).show();
    						break;
    					case BluetoothTMCLService.STATE_CONNECTING:
    						Log.d(TAG, "SR: State: connecting...");
	                		Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
	                		break;
	                	case BluetoothTMCLService.STATE_CONNECTION_ERROR:
	                		Log.d(TAG, "SR: State: Disconnected");
	                		break;
    				}
	                break;
    			case MESSAGE_WRITE:
	                break;
	            case MESSAGE_READ:
	                break;
	            case MESSAGE_DEVICE_NAME:
	                // save the connected device's name
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	                Toast.makeText(getApplicationContext(), "connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
	                break;
    		}
    	}
    };

    // animation for switching the view
    private Animation inFromRightAnimation()
	{
		Animation inFromRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT,  +1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
				Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f);
		inFromRight.setDuration(500);
		inFromRight.setInterpolator(new AccelerateInterpolator());
		return inFromRight;
	}

	private Animation outToLeftAnimation()
	{
		Animation outtoLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  -1.0f,
				Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f);
		outtoLeft.setDuration(500);
		outtoLeft.setInterpolator(new AccelerateInterpolator());
		return outtoLeft;
	}
	
    // animation for switching the view
	private Animation inFromLeftAnimation()
	{
		Animation inFromLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT,  -1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
				Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f);
		inFromLeft.setDuration(500);
		inFromLeft.setInterpolator(new AccelerateInterpolator());
		return inFromLeft;
	}
	
	private Animation outToRightAnimation()
	{
		Animation outtoRight = new TranslateAnimation(
		Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  +1.0f,
		Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f);
		outtoRight.setDuration(500);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}

	private void doViewChange(int xTouchDiff)
	{
		if (xTouchDiff > 0)
		{
			switch (this.actualView)
			{
				case 0:
					// do nothing here
					break;
				case 1:
				case 2:
				case 3:
	        		flipper.setInAnimation(inFromLeftAnimation());
	        		flipper.setOutAnimation(outToRightAnimation());
	        		flipper.showPrevious();
	        		actualView--;
	        		Log.d(TAG, "SR: actualView=" + actualView);
					break;
			}
		} else if (xTouchDiff < 0) {
			switch (this.actualView)
			{
				case 0:
				case 1:
				case 2:
	        		flipper.setInAnimation(inFromRightAnimation());
	        		flipper.setOutAnimation(outToLeftAnimation());
	        		flipper.showNext();
	        		actualView++;
	        		Log.d(TAG, "SR: actualView=" + actualView);
					break;
				case 3:
					// do nothing here
					break;
			}
		}		
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
			this.xTouchDown = (int)event.getX();
			// return true to indicate that we are interested in the following ACTION_UP event 
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			int xTouchUp = (int)event.getX();
			int xTouchDiff = xTouchUp-this.xTouchDown;
			if (Math.abs(xTouchDiff) >= 300)
			{
				this.doViewChange(xTouchDiff);
				switch (actualView)
				{
					case 0:
						this.stopVelocityDrawer();
						this.stopTorqueDrawer();
						break;
					case 1:
						this.startVelocityDrawer();
						this.stopTorqueDrawer();
						break;
					case 2:
						this.stopVelocityDrawer();
						this.stopLoadAngleDrawer();
						this.startTorqueDrawer();
						break;
					case 3:
						this.stopTorqueDrawer();
						this.startLoadAngleDrawer();
						break;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v)
	{
		if (v == btnSelectDevice) {
			Log.d(TAG, "SR: starting DeviceListActivity...");
			this.layoutDeviceSelector.setVisibility(View.VISIBLE);
			this.btnSelectDevice.setClickable(false);
			this.btnExit.setClickable(false);
			this.btnScanDevices.setEnabled(true);
		} else if (v == btnExit) {
			Log.d(TAG, "SR: app finished!");
			this.mTMCLService.disconnect();
			finish();
		} else if (v == btnCancelScan) {
			Log.d(TAG, "SR: cancel scan");
			this.layoutDeviceSelector.setVisibility(View.GONE);
			this.btnSelectDevice.setClickable(true);
			this.btnExit.setClickable(true);
		} else if (v == btnScanDevices) {
			Log.d(TAG, "SR: starting device discovery...");
			this.btnScanDevices.setEnabled(false);
			this.mNewDevicesArrayAdapter.clear();
	        // if we're already discovering, stop it
	        if (mBluetoothAdapter.isDiscovering())
	        {
	            mBluetoothAdapter.cancelDiscovery();
	        }
	        mBluetoothAdapter.startDiscovery();
		} else if (v == btnRORVelocityDrawer) {
			Log.d(TAG, "SR: rotate right");
			mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)1, (byte)0, (byte)0, this.maxSpeed));
		} else if (v == btnROLVelocityDrawer) {
			Log.d(TAG, "SR: rotate left");
			mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)2, (byte)0, (byte)0, this.maxSpeed));
		} else if (v == btnMotorStopVelocityDrawer) {
			Log.d(TAG, "SR: motor stop");
			mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)3, (byte)0, (byte)0, 0));
		} else if (v == btnEnableCoolStep) {
			if (this.btnEnableCoolStep.getText().equals("coolStep on")) {
				mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)172, (byte)0, 5));
				this.btnEnableCoolStep.setText("coolStep off");
			} else {
				mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)172, (byte)0, 0));
				this.btnEnableCoolStep.setText("coolStep on");
			}
		} else if (v == btnEnableDCStep) {
			if (this.btnEnableDCStep.getText().equals("dcStep on"))
			{
				TMCLRequestCommand request = new TMCLRequestCommand((byte)64, (byte)0, (byte)0, 1);
				TMCLReplyCommand reply = mTMCLService.requestTMCLCommand(request);
				if (reply != null)
					Log.d(TAG, "SR: " + reply.toString());
				this.btnEnableDCStep.setText("dcStep off");
				this.velocityDrawerVelocityChart.setEnabled(true);
				this.velocityDrawer.setMaxValue(this.maxDCStepSpeed);
				this.velocityDrawer.setMinValue(-this.maxDCStepSpeed);
			} else {
				TMCLRequestCommand request = new TMCLRequestCommand((byte)64, (byte)0, (byte)0, 0);
				TMCLReplyCommand reply = mTMCLService.requestTMCLCommand(request);
				if (reply != null)
					Log.d(TAG, "SR: " + reply.toString());
				this.btnEnableDCStep.setText("dcStep on");
				this.velocityDrawerVelocityChart.setEnabled(false);
				this.velocityDrawer.setMaxValue(this.maxDCStepSpeed);
				this.velocityDrawer.setMinValue(-this.maxDCStepSpeed);
			}
		}
	}

	private void stopTorqueDrawer()
	{
		Log.d(TAG, "SR: stopping torque drawer...");
		if (this.torqueValueChecker != null)
		{
			this.torqueValueChecker.stopRunning();
			this.torqueValueChecker = null;
		}
	}

	private void startTorqueDrawer()
	{
		Log.d(TAG, "SR: starting torque drawer...");
		if (this.torqueValueChecker == null)
		{
			this.torqueValueChecker = new ValueCheckerThread(this, StepRockerActivity.CHECK_TORQUE);
			this.torqueValueChecker.setDelay(10);
			this.torqueValueChecker.start();
		}
	}

	private void stopVelocityDrawer()
	{
		Log.d(TAG, "SR: stopping velocity drawer...");
		if (this.velocityValueChecker != null)
		{
			this.velocityValueChecker.stopRunning();
			this.velocityValueChecker = null;
		}
	}

	private void startVelocityDrawer()
	{
		Log.d(TAG, "SR: starting velocity drawer...");
		if (this.velocityValueChecker == null)
		{
			this.velocityValueChecker = new ValueCheckerThread(this, StepRockerActivity.CHECK_VELOCITY);
			this.velocityValueChecker.setDelay(10);
			this.velocityValueChecker.start();
		}
	}
	
	private void stopLoadAngleDrawer()
	{
		Log.d(TAG, "SR: stopping load angle drawer...");
		if (this.loadValueChecker != null)
		{
			this.loadValueChecker.stopRunning();
			this.loadValueChecker = null;
		}
	}

	private void startLoadAngleDrawer()
	{
		Log.d(TAG, "SR: starting load angle drawer...");
		if (this.loadValueChecker == null)
		{
			this.loadValueChecker = new ValueCheckerThread(this, StepRockerActivity.CHECK_LOAD_ANGLE);
			this.loadValueChecker.setDelay(10);
			this.loadValueChecker.start();
		}
	}
	
	public void setDefaultValues()
	{
		Log.d(TAG, "SR: setting default values...");
		
		// reset the module
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)255, (byte)0, (byte)0, 1234));
		
		// request a motor stop before changing the parameters
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)3, (byte)0, (byte)0, 0));
		
		// set motor run current
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)6, (byte)0, 230));
		
		// set motor standby current
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)7, (byte)0, 8));
		
		// set stallGuard2 filter setting
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)173, (byte)0, 1));

		// set stallGuard2 threshold value
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)174, (byte)0, 4));
    
		// set stop on stall value
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)181, (byte)0, 0));

		// set coolStep minimum current setting
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)168, (byte)0, 1));
    
		// set coolStep down step setting
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)169, (byte)0, 3));
		
		// set coolStep up step setting
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)171, (byte)0, 3));
		
		// set coolStep hysteresis width
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)170, (byte)0, 3));

		// set coolStep hysteresis start
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)172, (byte)0, 5));

		// set coolStep threshold speed
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)182, (byte)0, 800));

		// set coolStep slow run current
		mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)5, (byte)183, (byte)0, 0));
		
		Log.d(TAG, "SR: ready setting default values.");
	}
	
	public void checkValues(int checkType)
	{
		if (checkType == StepRockerActivity.CHECK_VELOCITY)
		{
			Vector<Chart> charts = this.velocityDrawer.getCharts();
			for (int i = 0; i < charts.size(); i++)
			{
				Chart chart = charts.get(i);

				// only get live values for enabled charts
				if (!chart.isEnabled())
					continue;

				if (this.liveMeasurementMode)
				{
					byte type = chart.getTraceCommand();
					TMCLRequestCommand request = new TMCLRequestCommand((byte)6, type, (byte)0, (byte)0);
					TMCLReplyCommand reply = mTMCLService.requestTMCLCommand(request);
					if (reply != null)
					{
						if (type == (byte)206)
							chart.addValue(this.maxDCStepSpeed-3*reply.getValue());
						else
							chart.addValue(reply.getValue());
					}
				} else {
					// pretend demo values for velocity and load 
					chart.addValue((int)((i+1) * 800 * Math.sin(xVelocityDrawer)));
				}
			}
			// only for demo mode
			if (!this.liveMeasurementMode)
				xVelocityDrawer += 0.02;
		}
		else if (checkType == StepRockerActivity.CHECK_TORQUE)
		{
			if (this.liveMeasurementMode)
			{
				TMCLReplyCommand reply = mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)6, this.torqueDrawer.getVelocityCommand(), (byte)0, (byte)0));
				if (reply != null)
				{
					if (this.btnEnableDCStep.getText().equals("dcStep off"))
						// DCStep is running
						this.torqueDrawer.setWorkingVelocity((int)(Math.abs((reply.getValue()*this.maxSpeed)/(float)this.maxDCStepSpeed)));
					else
						// normal step
						this.torqueDrawer.setWorkingVelocity(4*this.maxSpeed/10);
				}
			} else {
				// pretend a demo velocity 
				this.torqueDrawer.setWorkingVelocity((int) (1024 + 500 * Math.sin(xTorqueDrawer)));
				xTorqueDrawer += 0.02;
			}
		}
		else if (checkType == StepRockerActivity.CHECK_LOAD_ANGLE)
		{
			if (this.liveMeasurementMode)
			{
				// get the actual load value
				TMCLReplyCommand reply = mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)6, this.loadAngleDrawer.getLoadAngleCommand(), (byte)0, (byte)0));
				if (reply != null)
				{
					int reverseLoad = 1023-reply.getValue();
					float loadAngle = (float)(reverseLoad*180)/(float)1024;
					Log.d(TAG, "SR: loadAngle: " + loadAngle);
					this.loadAngleDrawer.setActualLoadAngle(Math.round(2.3*(loadAngle-129))+90); // -134
				}

				// get the actual smart current value
				TMCLReplyCommand reply2 = mTMCLService.requestTMCLCommand(new TMCLRequestCommand((byte)6, this.smartCurrentDrawer.getSmartCurrentCommand(), (byte)0, (byte)0));
				if (reply2 != null)
				{
					//float percent = (float)(reply2.getValue()*160*this.maxCurrent)/(float)(31*this.actualMaxCurrent);
					float percent = (float)(((float)(reply2.getValue()-14)/14)*100) + 25;
					Log.d(TAG, "SR: smartCurrent: " + percent + " (" + reply2.getValue() + ")");

					if (this.btnEnableCoolStep.getText().equals("coolStep off"))
						this.smartCurrentDrawer.setActualPercent(percent);
					else
						this.smartCurrentDrawer.setActualPercent(100);
				}
			} else {
				// pretend a demo load angle
				this.loadAngleDrawer.setActualLoadAngle(demoLoadAngle);
				demoLoadAngle += 0.1;
				if (demoLoadAngle >= 360)
					demoLoadAngle = 0;

				// pretend a demo smart current angle
				this.smartCurrentDrawer.setActualPercent(demoSmartCurrent);
				if (this.demoSmartCurrentUp)
					this.demoSmartCurrent +=0.1;
				else
					this.demoSmartCurrent -= 0.1;
				
				if (this.demoSmartCurrent >= 150)
					this.demoSmartCurrentUp = false;
				
				if (this.demoSmartCurrent <= 0)
					this.demoSmartCurrentUp = true;
			}
		}
	}

    // the BroadcastReceiver listens for discovered devices
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
    	@Override
    	public void onReceive(Context context, Intent intent)
    	{
    		String action = intent.getAction();
    		// when discovery finds a device
    		if (BluetoothDevice.ACTION_FOUND.equals(action))
    		{
    			// get the Bluetooth device object from the Intent
    			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    			// if it's already paired, skip it, because it has been listed already
    			if (device.getBondState() != BluetoothDevice.BOND_BONDED)
    				mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
    		} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
    			if (mNewDevicesArrayAdapter.getCount() == 0)
    				mNewDevicesArrayAdapter.add("no devices found");
    		}
    	}
    };
    
    // the on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener()
    {
    	public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
    	{
    		// cancel discovery because it's costly and we're about to connect
    		mBluetoothAdapter.cancelDiscovery();

    		// put the select device view to background
			layoutDeviceSelector.setVisibility(View.GONE);
			btnSelectDevice.setClickable(true);
			btnExit.setClickable(true);

    		// get the device MAC address, which is the last 17 chars in the view
    		String info = ((TextView)v).getText().toString();
    		String address = info.substring(info.length()-17);

			// get the selected remote Bluetooth device and connect with this device
			mTMCLService.connect(mBluetoothAdapter.getRemoteDevice(address));
    	}
    };
}
