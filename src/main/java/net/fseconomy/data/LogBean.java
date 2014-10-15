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
import java.sql.Timestamp;

public class LogBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	int id;
	Timestamp time;
	String user;
	String aircraft;
	String from;
	String to;
	String type;
	int totalEngineTime;
	float income;
	float fuelCost;
	float rentalCost;
	String rentalType;
	int flightEngineTime;
	int flightEngineTicks;
	float landingCost;
	float bonus;
	int accounting;
	float rentalPrice;
	int distance;
	boolean nightBonus;
	float envFactor;
	int groupId;
	int pilotFee;
	int subType;
	float maintenanceCost;
	int fbo;
	float crewCost;
	float fboAssignmentFee;
	int ageECost;
	int ageAvCost;
	int ageAfCost;
	int ageAdwCost;
	float mpttax;
	
	/**
	 * Constructor for LogBean.
	 */
	public LogBean()
	{
		super();
	}
	
	public LogBean(ResultSet rs) throws SQLException
	{
		setId(rs.getInt("id"));
		setTime(rs.getTimestamp("time"));
		setUser(rs.getString("user"));
		setAircraft(rs.getString("aircraft"));		
		setFrom(rs.getString("from"));
		setTo(rs.getString("to"));
		setType(rs.getString("type"));
		setTotalEngineTime(rs.getInt("totalEngineTime"));
		setIncome(rs.getFloat("income"));
		setFuelCost(rs.getFloat("fuelCost"));		
		setRentalCost(rs.getFloat("rentalCost"));		
		setRentalType(rs.getString("rentalType"));
		setFlightEngineTime(rs.getInt("flightEngineTime"));
		setFlightEngineTicks(rs.getInt("flightEngineTicks"));
		setLandingCost(rs.getFloat("landingCost"));
		setBonus(rs.getFloat("bonus"));
		setRentalPrice(rs.getFloat("rentalPrice"));
		setAccounting(rs.getInt("accounting"));
		setDistance(rs.getInt("distance"));
		setNightBonus(rs.getInt("night") != 0);
		setEnvFactor(rs.getFloat("envFactor"));
		setGroupId(rs.getInt("groupId"));
		setPilotFee(rs.getInt("pilotFee"));
		setSubType(rs.getInt("subType"));
		setMaintenanceCost(rs.getFloat("maintenanceCost"));
		setFbo(rs.getInt("fbo"));
		setCrewCost(rs.getFloat("crewCost"));
		setFboAssignmentFee(rs.getFloat("fboAssignmentFee"));
		setAgeECost(rs.getInt("ageECost"));
		setAgeAvCost(rs.getInt("ageAvCost"));
		setAgeAfCost(rs.getInt("ageAfCost"));
		setAgeAdwCost(rs.getInt("ageAdwCost"));
		setmptTax(rs.getFloat("mpttax"));
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
	 * Returns the bonus.
	 * @return float
	 */
	public float getBonus()
	{
		return bonus;
	}

	/**
	 * Returns the flightEngineTicks.
	 * @return int
	 */
	public int getFlightEngineTicks()
	{
		return flightEngineTicks;
	}

	/**
	 * Returns the flightEngineTime.
	 * @return int
	 */
	public int getFlightEngineTime()
	{
		return flightEngineTime;
	}

	/**
	 * Returns the from.
	 * @return String
	 */
	public String getFrom()
	{
		return from;
	}

	/**
	 * Returns the fuelCost.
	 * @return float
	 */
	public float getFuelCost()
	{
		return fuelCost;
	}

	/**
	 * Returns the crewCost.
	 * @return float
	 */
	public float getCrewCost()
	{
		return crewCost;
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
	 * Returns the income.
	 * @return float
	 */
	public float getIncome()
	{
		return income;
	}

	/**
	 * Returns the landingCost.
	 * @return float
	 */
	public float getLandingCost()
	{
		return landingCost;
	}

	/**
	 * Returns the rentalCost.
	 * @return float
	 */
	public float getRentalCost()
	{
		return rentalCost;
	}

	/**
	 * Returns the rentalType.
	 * @return String
	 */
	public String getRentalType()
	{
		return rentalType;
	}

	/**
	 * Returns the time.
	 * @return Timestamp
	 */
	public Timestamp getTime()
	{
		return time;
	}

	/**
	 * Returns the to.
	 * @return String
	 */
	public String getTo()
	{
		return to;
	}

	/**
	 * Returns the totalEngineTime.
	 * @return int
	 */
	public int getTotalEngineTime()
	{
		return totalEngineTime;
	}

	/**
	 * Returns the type.
	 * @return String
	 */
	public String getType()
	{
		return type;
	}
	
	public String getSType()
	{
		if (type.equals("refuel"))
			return type;
		if (type.equals("flight"))
			return type + ":" + from + " &rarr; " + to;
		if (type.equals("maintenance"))
			return type + " [" + AircraftMaintenanceBean.maintenanceType(subType) + "]";
			
		return "";
	}

	/**
	 * Returns the user.
	 * @return String
	 */
	public String getUser()
	{
		return user;
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
	 * Sets the bonus.
	 * @param bonus The bonus to set
	 */
	public void setBonus(float bonus)
	{
		this.bonus = bonus;
	}

	/**
	 * Sets the flightEngineTicks.
	 * @param flightEngineTicks The flightEngineTicks to set
	 */
	public void setFlightEngineTicks(int flightEngineTicks)
	{
		this.flightEngineTicks = flightEngineTicks;
	}

	/**
	 * Sets the flightEngineTime.
	 * @param flightEngineTime The flightEngineTime to set
	 */
	public void setFlightEngineTime(int flightEngineTime)
	{
		this.flightEngineTime = flightEngineTime;
	}

	/**
	 * Sets the from.
	 * @param from The from to set
	 */
	public void setFrom(String from)
	{
		this.from = from;
	}

	/**
	 * Sets the fuelCost.
	 * @param fuelCost The fuelCost to set
	 */
	public void setFuelCost(float fuelCost)
	{
		this.fuelCost = fuelCost;
	}
	
	/**
	 * Sets the crewCost.
	 * @param crewCost The crewCost to set
	 */
	public void setCrewCost(float crewCost)
	{
		this.crewCost = crewCost;
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
	 * Sets the income.
	 * @param income The income to set
	 */
	public void setIncome(float income)
	{
		this.income = income;
	}

	/**
	 * Sets the landingCost.
	 * @param landingCost The landingCost to set
	 */
	public void setLandingCost(float landingCost)
	{
		this.landingCost = landingCost;
	}

	/**
	 * Sets the rentalCost.
	 * @param rentalCost The rentalCost to set
	 */
	public void setRentalCost(float rentalCost)
	{
		this.rentalCost = rentalCost;
	}

	/**
	 * Sets the rentalType.
	 * @param rentalType The rentalType to set
	 */
	public void setRentalType(String rentalType)
	{
		this.rentalType = rentalType;
	}

	/**
	 * Sets the time.
	 * @param time The time to set
	 */
	public void setTime(Timestamp time)
	{
		this.time = time;
	}

	/**
	 * Sets the to.
	 * @param to The to to set
	 */
	public void setTo(String to)
	{
		this.to = to;
	}

	/**
	 * Sets the totalEngineTime.
	 * @param totalEngineTime The totalEngineTime to set
	 */
	public void setTotalEngineTime(int totalEngineTime)
	{
		this.totalEngineTime = totalEngineTime;
	}

	/**
	 * Sets the type.
	 * @param type The type to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * Sets the user.
	 * @param user The user to set
	 */
	public void setUser(String user)
	{
		this.user = user;
	}

	/**
	 * Returns the accounting.
	 * @return int
	 */
	public int getAccounting()
	{
		return accounting;
	}

	/**
	 * Returns the rentalPrice.
	 * @return int
	 */
	public float getRentalPrice()
	{
		return rentalPrice;
	}

	/**
	 * Sets the accounting.
	 * @param accounting The accounting to set
	 */
	public void setAccounting(int accounting)
	{
		this.accounting = accounting;
	}

	/**
	 * Sets the rentalPrice.
	 * @param rentalPrice The rentalPrice to set
	 */
	public void setRentalPrice(float rentalPrice)
	{
		this.rentalPrice = rentalPrice;
	}

	/**
	 * Returns the distance.
	 * @return int
	 */
	public int getDistance()
	{
		return distance;
	}

	/**
	 * Sets the distance.
	 * @param distance The distance to set
	 */
	public void setDistance(int distance)
	{
		this.distance = distance;
	}

	/**
	 * Returns the envFactor.
	 * @return float
	 */
	public float getEnvFactor()
	{
		return envFactor;
	}

	/**
	 * Returns the nightBonus.
	 * @return boolean
	 */
	public boolean isNightBonus()
	{
		return nightBonus;
	}

	/**
	 * Sets the envFactor.
	 * @param envFactor The envFactor to set
	 */
	public void setEnvFactor(float envFactor)
	{
		this.envFactor = envFactor;
	}

	/**
	 * Sets the nightBonus.
	 * @param nightBonus The nightBonus to set
	 */
	public void setNightBonus(boolean nightBonus)
	{
		this.nightBonus = nightBonus;
	}

	public int getGroupId() {
		return groupId;
	}

	public int getPilotFee() {
		return pilotFee;
	}

	public void setGroupId(int i) {
		groupId = i;
	}

	public void setPilotFee(int i) {
		pilotFee = i;
	}

	public int getSubType()
	{
		return subType;
	}

	public void setSubType(int i)
	{
		subType = i;
	}

	public float getMaintenanceCost()
	{
		return maintenanceCost;
	}

	public void setMaintenanceCost(float f)
	{
		maintenanceCost = f;
	}

	public int getFbo()
	{
		return fbo;
	}

	public void setFbo(int i)
	{
		fbo = i;
	}
	
	public float getFboAssignmentFee()
	{
		return fboAssignmentFee;
	}
	
	public void setFboAssignmentFee(float f)
	{
		fboAssignmentFee = f;
	}

	public void setAgeECost(int i)
	{
		ageECost = i;
	}

	public void setAgeAvCost(int i)
	{
		ageAvCost = i;
	}

	public void setAgeAfCost(int i)
	{
		ageAfCost = i;
	}

	public void setAgeAdwCost(int i)
	{
		ageAdwCost = i;
	}

	public float getmptTax()
	{
		return mpttax;
	}
	
	public void setmptTax(float f)
	{
		mpttax = f;
	}
}
