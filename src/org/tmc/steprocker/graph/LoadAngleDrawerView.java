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

package org.tmc.steprocker.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

public class LoadAngleDrawerView extends View
{
	private byte loadAngleCommand;
	private float actualLoadAngle;
	private String description = "";

	private Paint textPaint, linePaint, pointPaint;
    private Paint backgroundFill, transparentFill;
    private Paint borderPaint;
    private Paint nPaint, sPaint, housingFill;
    private Float textSize = 30.0f;
    
	private int surfaceHeight, surfaceWidth;
	private RefreshHandler mRedrawHandler;

	public LoadAngleDrawerView(Context context)
	{
		super(context);
		init();
	}

    public LoadAngleDrawerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    
    private void init()
    {
    	mRedrawHandler = new RefreshHandler(this);
    	actualLoadAngle = 90;
    	
    	this.textPaint = new Paint();
        this.textPaint.setAntiAlias(true);
        this.textPaint.setARGB(255, 0, 0, 0);
        this.textPaint.setTextSize(this.textSize);
        
        this.linePaint = new Paint();
        this.linePaint.setAntiAlias(true);
        this.linePaint.setARGB(255, 0, 0, 0);
        this.linePaint.setStyle(Style.STROKE);
        
        this.pointPaint = new Paint();
        this.pointPaint.setAntiAlias(true);
        this.pointPaint.setARGB(255, 0, 0, 255);

        this.backgroundFill = new Paint();
        this.backgroundFill.setAntiAlias(true);
        this.backgroundFill.setARGB(255, 255, 255, 255);
        this.backgroundFill.setStyle(Style.FILL);
        
        this.transparentFill = new Paint();
        this.transparentFill.setAntiAlias(true);
        this.transparentFill.setARGB(0, 255, 255, 255);
        this.transparentFill.setStyle(Style.FILL);
        
        this.nPaint = new Paint();
        this.nPaint.setAntiAlias(true);
        this.nPaint.setARGB(255, 255, 0, 0);
        this.nPaint.setStyle(Style.FILL);

        this.sPaint = new Paint();
        this.sPaint.setAntiAlias(true);
        this.sPaint.setARGB(255, 0, 255, 0);
        this.sPaint.setStyle(Style.FILL);
        
        this.borderPaint = new Paint();
        this.borderPaint.setAntiAlias(true);
        this.borderPaint.setARGB(255, 0, 0, 0);
        this.borderPaint.setStyle(Style.STROKE);
        
        this.housingFill = new Paint();
        this.housingFill.setAntiAlias(true);
        this.housingFill.setARGB(255, 150, 150, 150);
        this.housingFill.setStyle(Style.FILL);
    }
    
    @Override
    public void onDraw(Canvas canvas)
    {
		this.textPaint.setColor(Color.BLACK);
		this.pointPaint.setColor(Color.BLACK);
		this.linePaint.setColor(Color.BLACK);
		
		int h = this.surfaceHeight;
		int w = this.surfaceWidth;

		int padding = 10;
		int magnetHalfHeight = 60;
        int chartWidth = w - 2*padding;
        int chartHeight = h - 2*padding;
        
        int x0 = padding + chartWidth/2;
        int y0 = padding + chartHeight/2;

        // determine the max drawable radius
        int maxR;
        if (chartHeight < chartWidth)
        	maxR = chartHeight/2;
        else 
        	maxR = chartWidth/2;

        int middleR = maxR-20;
		int magnetWidth = maxR/2;
        int innerR = magnetWidth;
        int innermostR = innerR-10;
        int clipBuff = 10;
        
    	// clear the background
    	canvas.drawRect(0, 0, w, h, this.backgroundFill);

        // draw actual load value
        //canvas.drawText(Float.toString(Math.round(this.actualLoadAngle*10)/(float)10), 100, 100, this.textPaint);
    	
        Rect leftMagnetN = new Rect(x0-2*magnetWidth, y0-magnetHalfHeight, x0-magnetWidth+clipBuff, y0+magnetHalfHeight);
        Rect rightMagnetS = new Rect(x0+2*magnetWidth, y0-magnetHalfHeight, x0+magnetWidth-clipBuff, y0+magnetHalfHeight);
        Rect mainMagnetN = new Rect(x0, y0-magnetHalfHeight, x0+magnetWidth+clipBuff, y0+magnetHalfHeight);
        Rect mainMagnetS = new Rect(x0-magnetWidth-clipBuff, y0-magnetHalfHeight, x0, y0+magnetHalfHeight);

        // draw the housing
        canvas.save();
        canvas.drawCircle(x0, y0, maxR, this.housingFill);
        canvas.drawCircle(x0, y0, maxR, this.borderPaint);
        canvas.drawCircle(x0, y0, middleR, this.backgroundFill);
        canvas.drawCircle(x0, y0, middleR, this.borderPaint);
        canvas.restore();

        // draw left magnet
        canvas.save();
        canvas.clipRect(leftMagnetN);
        canvas.drawRect(leftMagnetN, this.backgroundFill);
        canvas.drawCircle(x0, y0, maxR, this.housingFill);
        canvas.drawCircle(x0, y0, maxR, this.borderPaint);
        canvas.drawCircle(x0, y0, middleR, this.nPaint);
        canvas.drawCircle(x0, y0, innerR, this.backgroundFill);
        canvas.drawText("N", leftMagnetN.centerX()-this.textSize/2, leftMagnetN.centerY()+this.textSize/2, this.textPaint);
        canvas.restore();
        
        // draw right magnet
        canvas.save();
        canvas.clipRect(rightMagnetS);
        canvas.drawRect(rightMagnetS, this.backgroundFill);
        canvas.drawCircle(x0, y0, maxR, this.housingFill);
        canvas.drawCircle(x0, y0, maxR, this.borderPaint);
        canvas.drawCircle(x0, y0, middleR, this.sPaint);
        canvas.drawCircle(x0, y0, innerR, this.backgroundFill);
        canvas.drawText("S", rightMagnetS.centerX()-this.textSize/2, rightMagnetS.centerY()+this.textSize/2, this.textPaint);
        canvas.restore();

        // draw the rotor
        canvas.save();

        // rotate the canvas at the center of the drawer
        canvas.rotate(-1*(this.actualLoadAngle+(float)-90), x0, y0);

        // draw main magnet S
        canvas.save();
        canvas.clipRect(mainMagnetS);
        canvas.drawRect(mainMagnetS, this.transparentFill);
        canvas.drawCircle(x0, y0, innermostR, this.sPaint);
        //canvas.drawText("S", mainMagnetS.centerX()-this.textSize/2, mainMagnetS.centerY()+this.textSize/2, this.textPaint);
        canvas.restore();

        // draw main magnet N
        canvas.save();
        canvas.clipRect(mainMagnetN);
        canvas.drawRect(mainMagnetN, this.transparentFill);
        canvas.drawCircle(x0, y0, innermostR, this.nPaint);
        //canvas.drawText("N", mainMagnetN.centerX()-this.textSize/2, mainMagnetN.centerY()+this.textSize/2, this.textPaint);
        canvas.restore();
        
        // undo the rotate
        canvas.restore();
        
        // draw description
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(this.description, x0, y0 + innerR + 2*this.textSize, this.textPaint);
        
        // wait a little bit with the next repaint
        mRedrawHandler.sleep(50);
    }

    public void setLoadAngleCommand(byte command)
    {
    	this.loadAngleCommand = command;
    }

    public byte getLoadAngleCommand()
    {
    	return this.loadAngleCommand;
    }

    public float getActualLoadAngle()
    {
    	return actualLoadAngle;
	}

	public void setActualLoadAngle(float actualLoadAngle)
	{
		this.actualLoadAngle = actualLoadAngle;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	@Override
	public void onSizeChanged(int width, int height, int oldW, int oldH)
	{
		this.surfaceHeight = height;
		this.surfaceWidth = width;
	}

	public void setTypeface(Typeface typeface)
	{
		this.textPaint.setTypeface(typeface);
	}

    class RefreshHandler extends Handler
    {
    	private LoadAngleDrawerView view;
    	
    	public RefreshHandler(LoadAngleDrawerView view)
    	{
    		this.view = view;
    	}
    	
        @Override
        public void handleMessage(Message msg)
        {
        	this.view.invalidate();
        }

        public void sleep(long delayMillis)
        {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }
}
