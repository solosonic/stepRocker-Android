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

import java.util.Vector;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

public class TorqueDrawerView extends View
{
    private Paint textPaint, linePaint, pointPaint;
    private Paint backgroundPaint;
    private Float textSize = 30.0f;
    
	private int maxYValue = 100;
	private int minYValue = 0;
	private int maxXValue = 100;
	private int minXValue = 0;
	private String yAxisCaption = "unknown";
	private String xAxisCaption = "unknown";
	private Vector<Chart> charts;
	private int surfaceHeight, surfaceWidth;
	private int xScaleCount = 10;
	private int yScaleCount = 10;
	private int workingVelocity = 0; 
	private byte velocityCommand = 0;
	
	private RefreshHandler mRedrawHandler;

	public TorqueDrawerView(Context context)
	{
		super(context);
		init();
	}

    public TorqueDrawerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    
    private void init()
    {
    	mRedrawHandler = new RefreshHandler(this);
    	
    	this.charts = new Vector<Chart>();

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

        this.backgroundPaint = new Paint();
        this.backgroundPaint.setARGB(255, 255, 255, 255);
    }
    
    @Override
    public void onDraw(Canvas canvas)
    {
		this.textPaint.setColor(Color.BLACK);
		this.pointPaint.setColor(Color.BLACK);
		this.linePaint.setColor(Color.BLACK);
		
		int h = this.surfaceHeight;
		int w = this.surfaceWidth;

        // calculate scale division
		int xValueSpread = this.maxXValue - this.minXValue;
		float dXParts = (float)xValueSpread/xScaleCount;

		int yValueSpread = this.maxYValue - this.minYValue;
		float dYParts = (float)yValueSpread/yScaleCount;
	
		int border = 100;
        int chartWidth = w - (border+(int)(textSize/2)) - (int)(textSize/2 * this.xAxisCaption.length());
        int chartHeight = h - border;
        if (this.yAxisCaption.length() > 0)
        	chartHeight -= (int)(textSize/2);
        
        int x0 = border;
        int y0 = h - 2*border/3;

    	// clear the background
    	canvas.drawRect(0, 0, w-1, h-1, this.backgroundPaint);

    	// draw the border of the paint area
		int realHeight = h-3;
    	int realWidth = w-3;
    	canvas.drawRect(1, 1, realWidth, realHeight, this.linePaint);

        // draw y-axis
        canvas.drawLine(x0, y0, x0, y0-chartHeight-5, this.linePaint);

        // draw caption for the y axis
        canvas.drawText(this.yAxisCaption, x0 - 3*textSize, y0-chartHeight-2*textSize/3, this.textPaint);

        float dy = (float)chartHeight/(float)(yScaleCount * dYParts);
        float dx = (float)chartWidth/(float)(xScaleCount *dXParts);
        
        // draw small lines for Y-axis
        for (int i = 1; i <= yScaleCount; i++)
        	canvas.drawLine(x0-2, y0-i*dYParts*dy, x0+3, y0-i*dYParts*dy, this.linePaint);

        // draw numbers for Y-axis
        this.textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= yScaleCount; i++)
        {
			canvas.drawText(i*100/yScaleCount + "%", x0-textSize/2, y0-i*dYParts*dy + textSize/2, this.textPaint);
        }        

        this.textPaint.setTextAlign(Paint.Align.LEFT);

        // draw zero line
        this.linePaint.setColor(Color.GRAY);
    	canvas.drawLine(x0, y0+this.minYValue*dy, x0+chartWidth+5, y0+this.minYValue*dy, this.linePaint);
    	
    	// draw small lines for X-axis
    	for (int i = 1; i <= xScaleCount; i++)
    		canvas.drawLine(x0+i*dXParts*dx, y0-2, x0+i*dXParts*dx, y0+3, this.linePaint);
        
    	// draw numbers for X-axis
    	for (int i = 0; i <= xScaleCount; i++)
    		canvas.drawText(i*100/xScaleCount + "%", x0+i*dXParts*dx-this.textSize/2, y0+3*this.textSize/2, this.textPaint);
    	
        // draw caption for the X axis
        canvas.drawText(this.xAxisCaption, x0 + chartWidth + textSize/2, y0+textSize/3, this.textPaint);

        // get the width of the longest chart caption 
        int dCS = 0;
        for (int i = 0; i < this.charts.size(); i++)
        {
        	int length = this.charts.get(i).getLabel().length();
        	if (dCS < length) 
        		dCS = length;
        }
        int dC = (int)(dCS * textSize);

        // draw the charts
        for (int i = 0; i < this.charts.size(); i++)
        {
        	Chart chart = this.charts.get(i);
        	this.pointPaint.setColor(chart.getColor());
        	Vector<Float> values = chart.getValues();

        	// scale all chart values into the available chartWidth
        	for (int j = 0; j <  values.size(); j++)
        	{
        		float value = values.get(j);
        		
        		// draw only inside the bounds of the diagram (x0, y0, chartWidht, chartHeight)
        		int newY = (int)(y0 - (value - this.minYValue)*dy);
        		int newX = (int)(x0 + j*dx);
        		if ((newY < (y0 + 25)) && newY > (y0-chartHeight-5))
        		{
        			canvas.drawCircle(newX, newY, 2, this.pointPaint);
        		}
        	}
            // draw the working point if available for the actual chart
        	if (values.size() > this.workingVelocity)
        	{
        		int newY = (int)(y0 - (values.get(this.workingVelocity) - this.minYValue)*dy);
        		int newX = (int)(x0 + this.workingVelocity*dx);

        		this.pointPaint.setColor(Color.GREEN);
        		canvas.drawCircle(newX, newY, 10, this.pointPaint);
        		this.linePaint.setColor(Color.GREEN);
        		
        		// draw line to the x axis
        		canvas.drawLine(newX, newY, newX, y0, this.linePaint);
				
        		// draw line to the y axis
        		canvas.drawLine(newX, newY, x0, newY, this.linePaint);
        		this.pointPaint.setColor(chart.getColor());
        	}
        }
        
        // draw the chart names above the data points
        for (int i = 0; i < this.charts.size(); i++)
        {
        	Chart chart = this.charts.get(i);
        	this.textPaint.setColor(chart.getColor());
        	canvas.drawText(chart.getLabel(), x0+chartWidth-border-dC/2, y0-chartHeight*4/5+i*2*textSize, this.textPaint);
        }

        // wait a little bit with the next repaint
        mRedrawHandler.sleep(50);
    }

	public byte getVelocityCommand()
	{
		return velocityCommand;
	}

	public void setVelocityCommand(byte velocityCommand)
	{
		this.velocityCommand = velocityCommand;
	}
    
	public void setYAxisCaption(String caption)
	{
		this.yAxisCaption = caption;
	}

	public void setXAxisCaption(String caption)
	{
		this.xAxisCaption = caption;
	}

	public void setMaxXValue(int maxValue)
	{
		this.maxXValue = maxValue;
	}
	
	public void setMinXValue(int minValue)
	{
		this.minXValue = minValue;
	}
	
	public void setMaxYValue(int maxValue)
	{
		this.maxYValue = maxValue;
	}
	
	public void setMinYValue(int minValue)
	{
		this.minYValue = minValue;
	}

	public void setXScaleCount(int count)
	{
		this.xScaleCount = count;
	}

	public void setYScaleCount(int count)
	{
		this.yScaleCount = count;
	}

	public void addChart(Chart newChart)
	{
		this.charts.add(newChart);
	}
	
	public void clearCharts()
	{
		this.charts.clear();
	}
	
	public void setWorkingVelocity(int velocity)
	{
		this.workingVelocity = velocity;
	}

	public void addChart(String caption, byte type, int color)
	{
		this.addChart(new Chart(caption, (byte)type, color));
	}
	
	public Vector<Chart> getCharts()
	{
		return this.charts;
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
    	private TorqueDrawerView view;
    	
    	public RefreshHandler(TorqueDrawerView view)
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
