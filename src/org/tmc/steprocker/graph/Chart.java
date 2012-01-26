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

public class Chart
{
	private String label;
	private Vector<Float> values = new Vector<Float>();
	private int maxValues = 500;
	private byte traceCommand;
	private int color;
	private boolean enabled = true;
	
	public Chart(byte command)
	{
		this.label = "unknown";
		this.traceCommand = command;
	}
	
	public Chart(String label, byte command, int color)
	{
		this.label = label;
		this.traceCommand = command;
		this.color = color;
	}

	public void addValue(float newValue)
	{
		this.values.add(newValue);
		this.reduceGraph();
	}
	
	public Vector<Float> getValues()
	{
		return this.values;
	}
	
	public String getLabel()
	{
		return this.label;
	}
	
	public byte getTraceCommand()
	{
		return this.traceCommand;
	}
	
	public int getColor()
	{
		return this.color;
	}

	public void setMaxValues(int max)
	{
		if (this.maxValues > 0)
		{
			this.maxValues = max;
		}
		this.reduceGraph();
	}
	
	private void reduceGraph()
	{
		while (this.values.size() > this.maxValues)
		{
			this.values.removeElementAt(0);
		}	
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	public boolean isEnabled()
	{
		return this.enabled;
	}
}
