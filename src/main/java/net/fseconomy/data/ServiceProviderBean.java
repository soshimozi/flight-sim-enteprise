/*
 * FS Economy
 * Copyright (C) 2005  Marty Bochane
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.fseconomy.data;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServiceProviderBean implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final int STATUS_PENDING = 0;
	public static final int STATUS_ACTIVE = 1;
	public static final int STATUS_DISABLED = 2;
	public static final int STATUS_REJECTED = 3;
	public static final int STATUS_BANNED = 4;
	
	int id;
	int owner;
	String ownerName;
	int alternate;
	String alternateName;
	String name;
	String ip;
	String url;
	String description;
	int status;
	String key;
	String notes;
	
	public ServiceProviderBean()
	{
		super();
	}
	
	// Create goods from the goods table
	public ServiceProviderBean(ResultSet rs) throws SQLException
	{
		id = rs.getInt("id");
		owner = rs.getInt("owner");
		ownerName = rs.getString("ownername");
		alternate = rs.getInt("alternate");
		alternateName = rs.getString("alternatename");
		name  = rs.getString("servicename");
		ip = rs.getString("ip");
		url = rs.getString("url");
		description = rs.getString("description");
		status= rs.getInt("status");
		key = rs.getString("key");
		notes = rs.getString("notes");
	}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		rs.updateInt("owner", owner);
		rs.updateInt("alternate", alternate);
		rs.updateString("servicename", name);
		rs.updateString("ip", ip);
		rs.updateString("url", url);
		rs.updateString("description", description);
		rs.updateInt("status", status);
		rs.updateString("key", key);
		rs.updateString("notes", notes);
	}
	
	/**
	 * @return
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * @return
	 */
	public int getOwner()
	{
		return owner;
	}

	/**
	 * @return
	 */
	public void setOwner(int newowner, String newownername)
	{
		owner = newowner;
		setOwnerName(newownername);
	}

	/**
	 * @return
	 */
	public String getOwnerName()
	{
		return ownerName;
	}

	/**
	 * @return
	 */
	public void setOwnerName(String name)
	{
		ownerName = name;
	}

	/**
	 * @return
	 */
	public int getAlternate()
	{
		return alternate;
	}

	/**
	 * @return
	 */
	public void setAlternate(int newalt, String newaltname)
	{
		alternate = newalt;
		setAlternateName(newaltname);
	}

	/**
	 * @return
	 */
	public String getAlternateName()
	{
		return alternateName;
	}

	/**
	 * @return
	 */
	public void setAlternateName(String name)
	{
		alternateName = name;
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return
	 */
	public void setName(String newname)
	{
		name = newname;
	}

	/**
	 * @return
	 */
	public String getIP()
	{
		return ip;
	}

	/**
	 * @return
	 */
	public void setIP(String newip)
	{
		ip = newip;
	}

	/**
	 * @return
	 */
	public String getUrl()
	{
		return url;
	}

	/**
	 * @return
	 */
	public void setUrl(String newurl)
	{
		url = newurl;
	}

	/**
	 * @return
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * @return
	 */
	public void setDescription(String newdesc)
	{
		description = newdesc;
	}

	/**
	 * @return
	 */
	public int getStatus()
	{
		return status;
	}

	/**
	 * @return
	 */
	public void setStatus(int newstatus) // throws DataError
	{
		//if the new status is outside the proper values, ignore it!!
		//if(newstatus < STATUS_PENDING || newstatus > STATUS_BANNED)
		//	throw new DataError("setStatus: new status invalid");
		
		status = newstatus;
	}

	/**
	 * @return
	 */
	public String getStatusString()
	{
		String result = "Unknown Value";
		switch(status)
		{
		case STATUS_PENDING:
			result = "Pending";
			break;
		case STATUS_ACTIVE:
			result = "Active";
			break;
		case STATUS_DISABLED:
			result = "Disabled";
			break;
		case STATUS_REJECTED:
			result = "Rejected";
			break;
		case STATUS_BANNED:
			result = "Banned";
			break;
		}
		return result;
	}


	/**
	 * @return
	 */
	public String getKey()
	{
		return key;
	}
	
	/**
	 * @return
	 */
	public void setKey(String newkey)
	{
		//truncate the new key to the max of 10 characters!
		
		key = newkey.substring(0, 9);
	}
	
	/**
	 * @return
	 */
	public String getNotes()
	{
		return notes;
	}
	
	/**
	 * @return
	 */
	public void setNotes(String newnotes)
	{
		notes = newnotes;
	}
}
