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

public class TMCLRequestCommand
{
	private byte moduleAddress;
	private byte command;
	private byte type;
	private byte motor;
	private int value;
	private byte checksum;
		
	public TMCLRequestCommand(byte command, byte type, byte motor, int value)
	{
		this.moduleAddress = 1;
		this.command = command;
		this.type = type;
		this.motor = motor;
		this.value = value;
		this.checksum = computeCheckSum();
	}
	
	private byte computeCheckSum()
	{
		int temp = this.value;
		return (byte) (this.moduleAddress + this.command + this.type + this.motor +
				(byte)((temp >> 24) & 0xff) + (byte)((temp >> 16) & 0xff) +
				(byte)((temp >> 8) & 0xff) + (byte)(temp & 0xff));
	}

	public String toString()
	{
		return "TMCLRequest(" + (moduleAddress & 0xff) + ", " + (command & 0xff) + ", " + (type & 0xff) + ", " + (motor & 0xff) + ", " + (value) + ", " + (checksum & 0xff) + ")";
	}
	
	public byte[] getData()
	{
		byte[] data = new byte[9];
		data[0] = this.moduleAddress;
		data[1] = this.command;
		data[2] = this.type;
		data[3] = this.motor;
		data[4] = (byte)((this.value >> 24) & 0xff);
		data[5] = (byte)((this.value >> 16) & 0xff);
		data[6] = (byte)((this.value >> 8) & 0xff);
		data[7] = (byte)(this.value & 0xff);
		data[8] = this.checksum;
		return data;
	}
}
