/*
 * FS Economy
 * Copyright (C) 2005, 2006, 2007  Marty Bochane & Paul Dahlen
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

public class ModelBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	/**
	 * Constructor for ModelBean.
	 */
	public static final int EQUIPMENT_VFR_ONLY = 0;
	public static final int EQUIPMENT_VFR_IFR = 1;
	public static final int EQUIPMENT_IFR_ONLY = 2;
	
	public static final int EQUIPMENT_IFR_MASK = 1;
	public static final int EQUIPMENT_GPS_MASK = 2;
	public static final int EQUIPMENT_AP_MASK = 4;
	
	public static final int EQUIPMENT_MASK_ALL = (1|2|4);
	
	public static final int IFR_COST_HOUR = 15;
	public static final int AP_COST_HOUR = 15;	
	public static final int GPS_COST_HOUR = 20;	
	
	public static final int IFR_COST= 20000;
	public static final int AP_COST = 30000;
	public static final int GPS_COST = 60000;	
	
	public static class fuelTank 
	{
		public static final int NumTanks = 11;
		
		public static final int Center = 0;
		public static final int LeftMain = 1;
		public static final int LeftAux = 2;
		public static final int LeftTip = 3;
		public static final int RightMain = 4;
		public static final int RightAux = 5;
		public static final int RightTip = 6;
		public static final int Center2 = 7;
		public static final int Center3 = 8;
		public static final int Ext1 = 9;
		public static final int Ext2 = 10;
	};
	
	String make;
	String model;
	int id;
	int seats;
	int cruise;
	int crew;
	int fueltype;
	int price;
	int gph;
	int rental;
	int accounting;
	int maxRentTime;
	int bonus;
	int amount;
	int minAirportSize;
	int capacity[];
	int equipment;
	int maxWeight;
	int emptyWeight;
	int engines;
	int enginePrice;
	int numSell;
	int fcaptotal;
	
	int canShip; //Added for Aircraft Shipping - Airboss 12-24-10 	
	int fuelSystemOnly; //added for refuel limition to system only - 7-7-12	 
	
	public ModelBean()
	{
		super();
		capacity = new int[11];
	}
	
	public ModelBean(ResultSet rs) throws SQLException
	{
		setMake(rs.getString("make"));
		setModel(rs.getString("model"));
		setId(rs.getInt("id"));
		setSeats(rs.getInt("seats"));
		setCruise(rs.getInt("cruisespeed"));
		setCrew(rs.getInt("crew"));
		setFueltype(rs.getInt("fueltype"));
		setRental(rs.getInt("rentalPrice"));
		setPrice(rs.getInt("price"));		
		setSAccounting(rs.getString("accounting"));
		setMaxRentTime(rs.getInt("maxRentTime"));
		setBonus(rs.getInt("bonus"));
		setMinAirportSize(rs.getInt("minAirportSize"));
		setAmount(rs.getInt("amount"));
		setGph(rs.getInt("gph"));
		setEquipment(rs.getInt("equipment"));
		setMaxWeight(rs.getInt("maxWeight"));
		setEmptyWeight(rs.getInt("emptyWeight"));
		setEngines(rs.getInt("engines"));
		setEnginePrice(rs.getInt("enginePrice"));
		setNumSell(rs.getInt("numSell"));
		
		//Added for Aircraft Shipping - Airboss 12-24-10 
		setCanShip(rs.getBoolean("canShip") ? 1 : 0);

		//Added for refueling limitation to system only - Airboss 7-7-12 
		setFuelSystemOnly(rs.getBoolean("fuelSystemOnly") ? 1 : 0);
		
		capacity = new int[] {rs.getInt("fcapCenter"), rs.getInt("fcapLeftMain"), rs.getInt("fcapLeftAux"), rs.getInt("fcapLeftTip"),
			rs.getInt("fcapRightMain"), rs.getInt("fcapRightAux"), rs.getInt("fcapRightTip"), rs.getInt("fcapCenter2"), rs.getInt("fcapCenter3"),
			rs.getInt("fcapExt1"), rs.getInt("fcapExt2")};
	}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		rs.updateString("make", getMake());
		rs.updateString("model", getModel());
		rs.updateInt("seats", getSeats());
		rs.updateInt("cruisespeed", getCruise());
		rs.updateInt("crew", getCrew());
		rs.updateInt("fueltype", getFueltype());
		if (rs.getInt("rentalPrice") != getRental())
		{
			rs.updateInt("rentalPrice", getRental());
			rs.updateInt("priceDirty", 1);
		}
		rs.updateInt("price", getPrice());
		rs.updateString("accounting", getSAccounting());
		rs.updateInt("maxRentTime", getMaxRentTime());
		rs.updateInt("bonus", getBonus());
		rs.updateInt("minAirportSize", getMinAirportSize());
		rs.updateInt("amount", getAmount());
		rs.updateInt("gph", getGph());
		rs.updateInt("maxWeight", getMaxWeight());
		rs.updateInt("emptyWeight", getEmptyWeight());
		rs.updateInt("equipment", getEquipment());
		rs.updateInt("engines", getEngines());
		rs.updateInt("enginePrice", getEnginePrice());
		rs.updateInt("fcapCenter", capacity[fuelTank.Center]);
		rs.updateInt("fcapLeftMain", capacity[fuelTank.LeftMain]);
		rs.updateInt("fcapLeftAux", capacity[fuelTank.LeftAux]);
		rs.updateInt("fcapLeftTip", capacity[fuelTank.LeftTip]);
		rs.updateInt("fcapRightMain", capacity[fuelTank.RightMain]);
		rs.updateInt("fcapRightAux", capacity[fuelTank.RightAux]);
		rs.updateInt("fcapRightTip", capacity[fuelTank.RightTip]);
		rs.updateInt("fcapCenter2", capacity[fuelTank.Center2]);
		rs.updateInt("fcapCenter3", capacity[fuelTank.Center3]);
		rs.updateInt("fcapExt1", capacity[fuelTank.Ext1]);
		rs.updateInt("fcapExt2", capacity[fuelTank.Ext2]);
		rs.updateInt("fcaptotal", this.getTotalCapacity());
		rs.updateInt("numSell", getNumSell());
		
		//Added for Aircraft Shipping - Airboss 12-24-10 
		rs.updateBoolean("canShip", getCanShip()==1 ? true : false);
		//Added for refueling limitation to system only - Airboss 7-7-12 
		rs.updateBoolean("fuelSystemOnly", getFuelSystemOnly()==1 ? true : false);
	}
	public void setCenter(int cap)
	{
		capacity[fuelTank.Center] = cap;
	}	
	public void setLeftMain(int cap)
	{
		capacity[fuelTank.LeftMain] = cap;
	}
	public void setLeftAux(int cap)
	{
		capacity[fuelTank.LeftAux] = cap;
	}	
	public void setLeftTip(int cap)
	{
		capacity[fuelTank.LeftTip] = cap;
	}
	public void setRightMain(int cap)
	{
		capacity[fuelTank.RightMain] = cap;
	}		
	public void setRightAux(int cap)
	{
		capacity[fuelTank.RightAux] = cap;
	}
	public void setRightTip(int cap)
	{
		capacity[fuelTank.RightTip] = cap;
	}
	public void setCenter2(int cap)
	{
		capacity[fuelTank.Center2] = cap;
	}
	public void setCenter3(int cap)
	{
		capacity[fuelTank.Center3] = cap;
	}
	public void setExt1(int cap)
	{
		capacity[fuelTank.Ext1] = cap;
	}
	public void setExt2(int cap)
	{
		capacity[fuelTank.Ext2] = cap;
	}
	public int getCap(int id)
	{
		return capacity[id];
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
	 * Returns the make.
	 * @return String
	 */
	public String getMake()
	{
		return make;
	}

	/**
	 * Returns the model.
	 * @return String
	 */
	public String getModel()
	{
		return model;
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
	 * Sets the make.
	 * @param make The make to set
	 */
	public void setMake(String make)
	{
		this.make = make;
	}

	/**
	 * Sets the model.
	 * @param model The model to set
	 */
	public void setModel(String model)
	{
		this.model = model;
	}
	
	public String getMakeModel()
	{
		return make + " " + model;
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
	 * Returns the airportSize.
	 * @return int
	 */
	public int getMinAirportSize()
	{
		return minAirportSize;
	}

	/**
	 * Returns the amount.
	 * @return int
	 */
	public int getAmount()
	{
		return amount;
	}

	/**
	 * Returns the bonus.
	 * @return int
	 */
	public int getBonus()
	{
		return bonus;
	}

	/**
	 * Returns the crew.
	 * @return int
	 */
	public int getCrew()
	{
		return crew;
	}
	
	/**
	 * Returns the fueltype.
	 * @return int
	 */
	public int getFueltype()
	{
		return fueltype;
	}

	/**
	 * Returns the cruise.
	 * @return int
	 */
	 
	public int getCruise()
	{
		return cruise;
	}
	/**
	 * Returns the gph.
	 * @return int
	 */
	public int getGph()
	{
		return gph;
	}

	/**
	 * Returns the maxRentTime.
	 * @return int
	 */
	public int getMaxRentTime()
	{
		return maxRentTime;
	}

	/**
	 * Returns the price.
	 * @return int
	 */
	public int getPrice()
	{
		return price;
	}

	/**
	 * Returns the rental.
	 * @return int
	 */
	public int getRental()
	{
		return rental;
	}
	
	public int getTotalRentalTarget(int mask)
	{		
		int returnValue = getRental();
		
		if(returnValue != 0)
		{
			if ((mask & EQUIPMENT_IFR_MASK) > 0)
				returnValue += IFR_COST_HOUR;
			if ((mask & EQUIPMENT_AP_MASK) > 0)
				returnValue += AP_COST_HOUR;
			if ((mask & EQUIPMENT_GPS_MASK) > 0)
				returnValue += GPS_COST_HOUR;	
		}
		return returnValue;		
	}

	/**
	 * Returns the seats.
	 * @return int
	 */
	public int getSeats()
	{
		return seats;
	}

	/**
	 * Sets the airportSize.
	 * @param airportSize The airportSize to set
	 */
	public void setMinAirportSize(int airportSize)
	{
		this.minAirportSize = airportSize;
	}

	/**
	 * Sets the amount.
	 * @param amount The amount to set
	 */
	public void setAmount(int amount)
	{
		this.amount = amount;
	}

	/**
	 * Sets the bonus.
	 * @param bonus The bonus to set
	 */
	public void setBonus(int bonus)
	{
		this.bonus = bonus;
	}

	/**
	 * Sets the cruise.
	 * @param cruise The cruise to set
	 */
	public void setCruise(int cruise)
	{
		this.cruise = cruise;
	}

	/**
	 * Sets the crew.
	 * @param crew The crew to set
	 */
	public void setCrew(int crew)
	{
		this.crew = crew;
	}
	
		/**
	 * Sets the fueltype.
	 * @param fueltype The fuel type to set
	 */
	public void setFueltype(int fueltype)
	{
		this.fueltype = fueltype;
	}

	/**
	 * Sets the gph.
	 * @param gph The gph to set
	 */
	public void setGph(int gph)
	{
		this.gph = gph;
	}

	/**
	 * Sets the maxRentTime.
	 * @param maxRentTime The maxRentTime to set
	 */
	public void setMaxRentTime(int maxRentTime)
	{
		this.maxRentTime = maxRentTime;
	}

	/**
	 * Sets the price.
	 * @param price The price to set
	 */
	public void setPrice(int price)
	{
		this.price = price;
	}

	/**
	 * Sets the rental.
	 * @param rental The rental to set
	 */
	public void setRental(int rental)
	{
		this.rental = rental;
	}

	/**
	 * Sets the seats.
	 * @param seats The seats to set
	 */
	public void setSeats(int seats)
	{
		this.seats = seats;
	}
	
	public void setAccounting(int accounting)
	{
		this.accounting = accounting;
	}
	public void setSAccounting(String accounting)
	{
		this.accounting = AircraftBean.ACC_HOUR;		
	}
	public String getSAccounting()
	{
		return accounting == AircraftBean.ACC_TACHO ? "tacho" : "hour";
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

	/**
	 * Returns the equipment.
	 * @return int
	 */
	public int getEquipment()
	{
		return equipment;
	}

	/**
	 * Sets the equipment.
	 * @param equipment The equipment to set
	 */
	public void setEquipment(int equipment)
	{
		this.equipment = equipment;
	}

	/**
	 * @return
	 */
	public int getMaxWeight() {
		return maxWeight;
	}

	/**
	 * @param i
	 */
	public void setMaxWeight(int i) {
		maxWeight = i;
	}

	/**
	 * @return
	 */
	public int getEmptyWeight() {
		return emptyWeight;
	}
	
	public int getMaxCargoWeight()
	{
		return (int) Math.round(maxWeight - emptyWeight - getTotalCapacity() * Data.GALLONS_TO_KG);
	}

	/**
	 * @param i
	 */
	public void setEmptyWeight(int i) {
		emptyWeight = i;
	}

	/**
	 * @return
	 */
	public int getEnginePrice()
	{
		return enginePrice;
	}

	/**
	 * @return
	 */
	public int getEngines()
	{
		return engines;
	}

	/**
	 * @return
	 */
	public int getNumSell()
	{
		return numSell;
	}
	/**
	 * @param i
	 */
	public void setEnginePrice(int i)
	{
		enginePrice = i;
	}

	/**
	 * @param i
	 */
	public void setEngines(int i)
	{
		engines = i;
	}
	
	/**
	 * @param i
	 */
	public void setNumSell(int i)
	{
		numSell = i;
	}
	
	/**
	 * @param i
	 */
	public void setCanShip(int i)
	{
		canShip = i;
	}
	
	/**
	 * @return
	 */
	public int getCanShip()
	{
		return canShip;
	}

	/**
	 * @param i
	 */
	public void setFuelSystemOnly(int i)
	{
		fuelSystemOnly = i;
	}
	
	/**
	 * @return
	 */
	public int getFuelSystemOnly()
	{
		return fuelSystemOnly;
	}

	public int getTotalCapacity()
	{
		int totalCapacity = 0;
		for (int count = 0; count< capacity.length; count++)
			totalCapacity+=capacity[count];	

		return totalCapacity;
	}

}
