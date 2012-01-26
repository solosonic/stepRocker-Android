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
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

public class SmartCurrentDrawerView extends View
{
	private byte smartCurrentCommand;
	private float actualPercent;
	private String description = "";

	private Paint textPaint, linePaint, pointPaint;
    private Paint backgroundFill, transparentFill;
    private Paint borderPaint;
    private Paint needlePaint, housingFill;
    private Float textSize = 30.0f;
    
	private int surfaceHeight, surfaceWidth;
	private RefreshHandler mRedrawHandler;

	public SmartCurrentDrawerView(Context context)
	{
		super(context);
		init();
	}

    public SmartCurrentDrawerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    
    private void init()
    {
    	mRedrawHandler = new RefreshHandler(this);
    	actualPercent = 0;
    	
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
        
        this.needlePaint = new Paint();
        this.needlePaint.setAntiAlias(true);
        this.needlePaint.setARGB(255, 255, 0, 0);
        this.needlePaint.setStyle(Style.FILL);

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
		int needleHalfHeight = 20;
		int degreePadding = 40;
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
        int needleWidth = middleR-10;
        int innerR = needleWidth/2;
        
    	// clear the background
    	canvas.drawRect(0, 0, w, h, this.backgroundFill);

        // draw actual percent value
        //canvas.drawText(Float.toString(Math.round(this.actualPercent*10)/(float)10), 100, 100, this.textPaint);
    	
        // draw the housing
        canvas.save();
        canvas.drawCircle(x0, y0, maxR, this.housingFill);
        canvas.drawCircle(x0, y0, maxR, this.borderPaint);
        canvas.drawCircle(x0, y0, middleR, this.backgroundFill);
        canvas.drawCircle(x0, y0, middleR, this.borderPaint);
        canvas.restore();

        canvas.save();
        
        int parts = 6;
        float preRotate = 90-(parts*degreePadding/2);
        canvas.rotate(preRotate, x0, y0);
        
        // draw the small lines and the text for the speedometer scale
        this.textPaint.setTextAlign(Align.CENTER);
        for (int i = 0; i < (parts+1); i++)
        {
        	// draw the % text
        	canvas.save();
        	canvas.rotate(i*degreePadding, x0, y0);
        	canvas.translate(-middleR+2*this.textSize, 0);
        	canvas.rotate(-i*degreePadding-preRotate, x0, y0);
        	canvas.drawText(Integer.toString(i*25) + "%", x0, y0, this.textPaint);
        	canvas.restore();
        	
            // draw the small line
        	canvas.save();
        	canvas.rotate(i*degreePadding, x0, y0);
            canvas.drawRect(x0-middleR, y0+2, x0-middleR+8, y0-2, this.housingFill);
        	canvas.drawRect(x0-middleR, y0+2, x0-middleR+8, y0-2, this.linePaint);
        	canvas.restore();
        }

        // draw the rotated needle
        canvas.save();

        // rotate the canvas at the center of the drawer
        float angle = (4*degreePadding*this.actualPercent)/(float)100;
        canvas.rotate(angle, x0, y0);

        // draw the needle
        canvas.save();
        Path needle = new Path();
        needle.moveTo(x0-needleWidth, y0);
        needle.lineTo(x0, y0+needleHalfHeight);
        needle.lineTo(x0, y0-needleHalfHeight);
        needle.lineTo(x0-needleWidth, y0);
        needle.setLastPoint(x0-needleWidth, y0);

        canvas.drawPath(needle, this.needlePaint);
        canvas.drawPath(needle, this.borderPaint);

        canvas.drawCircle(x0, y0, 30, this.pointPaint);
        canvas.restore();

        // undo the rotate
        canvas.restore();
        
        // undo the rotate of the % text, small lines and needle
        canvas.restore();
        
        // draw description
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(this.description, x0, y0 + innerR + 2*this.textSize, this.textPaint);
        
        // wait a little bit with the next repaint
        mRedrawHandler.sleep(50);
    }

    public void setSmartCurrentCommand(byte command)
    {
    	this.smartCurrentCommand = command;
    }

    public byte getSmartCurrentCommand()
    {
    	return this.smartCurrentCommand;
    }

    public float getActualPercent()
    {
    	return this.actualPercent;
	}

	public void setActualPercent(float actualPercent)
	{
		this.actualPercent = actualPercent;
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
	public void onSizeChanged(int width, int height, int oldW, int oldH){
		this.surfaceHeight = height;
		this.surfaceWidth = width;
	}

	public void setTypeface(Typeface typeface)
	{
		this.textPaint.setTypeface(typeface);
	}

    class RefreshHandler extends Handler
    {
    	private SmartCurrentDrawerView view;
    	
    	public RefreshHandler(SmartCurrentDrawerView view)
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
