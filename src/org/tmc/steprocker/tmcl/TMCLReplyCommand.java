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

public class TMCLReplyCommand
{
	private byte replyAddress;
	private byte moduleAddress;
	private byte status;
	private byte command;
	private int value;
	private byte checksum;
	
	public TMCLReplyCommand(byte[] data)
	{
		this.replyAddress = data[0];
		this.moduleAddress = data[1];
		this.status = data[2];
		this.command = data[3];
		this.value = ((data[4] & 0xff) << 24) | ((data[5] & 0xff) << 16) |
					 ((data[6] & 0xff) << 8) | (data[7] & 0xff);
		this.checksum = data[8];
	}

	public int getValue()
	{
		return this.value;
	}
	
	public String toString()
	{
		return "TMCLReply(" + (replyAddress & 0xff)+ ", " + (moduleAddress & 0xff)+ ", " + (status & 0xff)+ ", " + (command & 0xff)+ ", " + (value) + ", " + (checksum & 0xff) + ")";
	}
}
