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
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

public class DrawerView extends View
{
    private Paint textPaint, linePaint, pointPaint;
    private Paint backgroundPaint;
    private Float textSize = 30.0f;
    
	private int maxValue = 100;
	private int minValue = 0;
	private int maxChartWidth = 0;
	private boolean dynScale = true;
	private boolean connectDataPoints = false;
	private String yAxisCaption = "unknown";
	private Vector<Chart> charts;
	private int surfaceHeight, surfaceWidth;
	private int scaleCount = 10;
    private int dxPerPoint = 1;
	
	private RefreshHandler mRedrawHandler;

	public DrawerView(Context context)
	{
		super(context);
		init();
	}

    public DrawerView(Context context, AttributeSet attrs)
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
		
		int h = this.surfaceHeight;
		int w = this.surfaceWidth;

        // calculate scale division
		int valueSpread = this.maxValue - this.minValue;
		float dParts = (float)valueSpread/scaleCount;
		int dS = 80;
		
		int border = 20;
        int chartWidth = w - (8*border + dS);
        int chartHeight = h - 2*border;
        
        int x0 = 2*border + dS;
        int y0 = h - 15;

    	// clear the background
    	canvas.drawRect(0, 0, w-1, h-1, this.backgroundPaint);

    	// draw the border of the paint area
		int realHeight = h-3;
    	int realWidth = w-3;
    	canvas.drawRect(1, 1, realWidth, realHeight, this.linePaint);

        // draw y-axis
        canvas.drawLine(x0, y0, x0, y0-chartHeight-5, this.linePaint);

        // draw caption for the y axis
        canvas.drawText(this.yAxisCaption, x0 + 10*textSize, y0-chartHeight+textSize/3, this.textPaint);

        float dy = (float)chartHeight/(float)(scaleCount * dParts);
        
        // draw small lines for Y-axis
        for (int i = 1; i <= scaleCount; i++)
        	canvas.drawLine(x0-2, y0-i*dParts*dy, x0+3, y0-i*dParts*dy, this.linePaint);

        // draw numbers for Y-axis
        this.textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= scaleCount; i++)
        	canvas.drawText(Integer.toString((int)(i*dParts + this.minValue)), x0-textSize, y0-i*dParts*dy + textSize/2 - 7, this.textPaint);
        
        this.textPaint.setTextAlign(Paint.Align.LEFT);

        // draw zero line
        this.linePaint.setColor(Color.GRAY);
    	canvas.drawLine(x0, y0+this.minValue*dy, x0+chartWidth, y0+this.minValue*dy, this.linePaint);
        
        // get the width of the longest chart caption 
        int dCS = 0;
        for (int i = 0; i < this.charts.size(); i++)
        {
        	int length = this.charts.get(i).getLabel().length();
        	if (dCS < length) 
        		dCS = length;
        }
        int dC = (int)(dCS * textSize);

        // draw the graphs
        int maxV;
        int minV;

        if (this.dynScale) {
        	maxV = 0;
        	minV = 0;
        } else {
        	maxV = this.maxValue;
        	minV = this.minValue;
        }

        // draw the charts
        for (int i = 0; i < this.charts.size(); i++)
        {
            int revIndex = 0;
        	Chart chart = this.charts.get(i);
        	
        	// only draw enabled charts
        	if (!chart.isEnabled())
        		continue;
        	
        	this.pointPaint.setColor(chart.getColor());
        	this.pointPaint.setStrokeWidth(3);
        	Vector<Float> values = chart.getValues();

        	// begin drawing the chart with the last value at the right side
            int beginDrawingAt = values.size();
        	
            int oldX = 0, oldY = 0;
        	for (int j = beginDrawingAt-1; j >= 0; j--)
        	{
        		float value = values.get(j);
        		
        		if (this.dynScale)
        		{
        			if (value > maxV)
        				maxV = (int)value + 1;
        			if (value < minV)
        				minV = (int)value -1;
        		}

        		// do not draw more points than visible chartWidth
    			if ((revIndex + dxPerPoint) > chartWidth)
    				break;	

        		// draw only inside the bounds of the diagram (x0, y0, chartWidht, chartHeight)
        		int newX = x0 + chartWidth-revIndex;
    			int newY = (int)(y0 - (value - this.minValue)*dy);
        		if ((newY < (y0 + 5)) && (newY > (y0-chartHeight-5)))
        		{
        			if ((this.connectDataPoints) && (j != (beginDrawingAt-1)))
        				canvas.drawLine(oldX, oldY, newX, newY, this.pointPaint);
        			else
        				canvas.drawCircle(newX, newY, 2, this.pointPaint);
        		}
        		oldX = newX;
        		oldY = newY;
        		revIndex += dxPerPoint;
        	}
        	// use the max diagram width to restrict the number of stored values
        	// (used to have enough values when switching back from vertical to horizontal orientation)
        	if (chartWidth > maxChartWidth)
        		maxChartWidth = chartWidth;
        	chart.setMaxValues(maxChartWidth);
        }
        
        // draw the chart names above the data points
        for (int i = 0; i < this.charts.size(); i++)
        {
        	Chart chart = this.charts.get(i);

        	// only draw enabled charts
        	if (!chart.isEnabled())
        		continue;

        	this.textPaint.setColor(chart.getColor());
        	canvas.drawText(chart.getLabel(), x0+chartWidth-border-dC/2, y0-chartHeight*2/3+i*3*textSize/2, this.textPaint);
        }
        this.maxValue = maxV;
        this.minValue = minV;

        // wait a little bit with the next repaint
        mRedrawHandler.sleep(5);
    }
	
	public void setYAxisCaption(String caption)
	{
		this.yAxisCaption = caption;
	}

	public void setMaxValue(int maxValue)
	{
		this.maxValue = maxValue;
		this.dynScale = false;
	}
	
	public void setMinValue(int minValue)
	{
		this.minValue = minValue;
		this.dynScale = false;
	}

	public void doDynamicScale()
	{
		this.dynScale = true;
	}

	public void setScaleCount(int count)
	{
		this.scaleCount = count;
	}

	public int getDxPerPoint()
	{
		return dxPerPoint;
	}

	public void setDxPerPoint(int dxPerPoint)
	{
		this.dxPerPoint = dxPerPoint;
	}

	public boolean isConnectDataPoints()
	{
		return connectDataPoints;
	}

	public void setConnectDataPoints(boolean connectDataPoints)
	{
		this.connectDataPoints = connectDataPoints;
	}

	public void addChart(Chart newChart)
	{
		this.charts.add(newChart);
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
    	private DrawerView view;
    	
    	public RefreshHandler(DrawerView view)
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
