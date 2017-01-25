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

package net.fseconomy.beans;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FSMappingBean implements Serializable
{

	private static final long serialVersionUID = 1L;
	String aircraft;
	int model;
	int id;
	int capacity[];
	
	/**
	 * Constructor for FSMappingBean.
	 */	
	public FSMappingBean()
	{
		super();
	}
	
	public FSMappingBean(ResultSet rs) throws SQLException
	{
		setAircraft(rs.getString("fsaircraft"));
		setModel(rs.getInt("model"));
		setId(rs.getInt("id"));
		setCapacity(new int[] {rs.getInt("fcapCenter"), rs.getInt("fcapLeftMain"), rs.getInt("fcapLeftAux"), rs.getInt("fcapLeftTip"),
			rs.getInt("fcapRightMain"), rs.getInt("fcapRightAux"), rs.getInt("fcapRightTip"), rs.getInt("fcapCenter2"), rs.getInt("fcapCenter3"),
			rs.getInt("fcapExt1"), rs.getInt("fcapExt2")});
	}
	
	/**
	 * Returns the aircraft.
	 * @return String
	 */
	public String getAircraft()
	{
		return aircraft;
	}

	/**
	 * Returns the model.
	 * @return int
	 */
	public int getModel()
	{
		return model;
	}

	/**
	 * Sets the aircraft.
	 * @param aircraft The aircraft to set
	 */
	public void setAircraft(String aircraft)
	{
		this.aircraft = aircraft;
	}

	/**
	 * Sets the model.
	 * @param model The model to set
	 */
	public void setModel(int model)
	{
		this.model = model;
	}

	/**
	 * Returns the id.
	 * @return int
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	/**
	 * Returns the capacity.
	 * @return int[]
	 */
	public int[] getCapacity()
	{
		return capacity;
	}

	/**
	 * Sets the capacity.
	 * @param capacity The capacity to set
	 */
	public void setCapacity(int[] capacity)
	{
		this.capacity = capacity;
	}

}
