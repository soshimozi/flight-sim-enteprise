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
import java.sql.Timestamp;

public class PaymentBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final short INTEREST_PAYMENT = 0;
	public static final short REASON_REFUEL = 1;	
	public static final short AIRCRAFT_SALE = 2;
	public static final short GROUP_PAYMENT = 3;
	public static final short GROUP_DELETION = 4;
	public static final short MAINTENANCE = 5;
	public static final short MAINTENANCE_FBO_COST = 6;
	public static final short EQUIPMENT = 7;
	public static final short SALE_GOODS_FUEL = 8;
	public static final short SALE_GOODS_BUILDING_MATERIALS = 9;
	public static final short SALE_GOODS_SUPPLIES = 10;
	public static final short ASSIGNMENT = 11;
	public static final short PILOT_FEE = 12;
	public static final short RENTAL = 13;
	public static final short LANDING_FEE = 14;
	public static final short CREW_FEE = 15;
	public static final short FBO_ASSIGNMENT_FEE = 16;
	public static final short EQUIPMENT_FBO_COST = 17;
	public static final short FBO_SALE = 18;
	public static final short FBO_FACILITY_RENT = 19;
	public static final short MULTIPLE_PT_TAX = 20;
	public static final short SALE_GOODS_JETA = 21;
	public static final short REASON_REFUEL_JETA = 22;
	
	public static final short TRANSFER_GOODS_FUEL = 23;
	public static final short TRANSFER_GOODS_JETA = 24;
	public static final short TRANSFER_GOODS_BUILDING_MATERIALS = 25;
	public static final short TRANSFER_GOODS_SUPPLIES = 26;
	public static final short AIRCRAFT_SHIPPING = 27; //Added for Aircraft Shipping - Airboss 12/31/10
	public static final short AIRCRAFT_LEASE = 28; //Added Airboss 5/7/11
	public static final short BULK_FUEL = 29;
    public static final short TRANSFER_GROUP = 30;
	public static final short BULK_SUPPLIES = 31;

	public static final short OWNERSHIP_FEE = 32;
	public static final short EQUIPMENT_REMOVAL = 33;
	public static final short EQUIPMENT_FBO_REMOVAL_COST = 34;

	public static final short MAX_REASON = 34;
	
	long id;
	Timestamp time;
	int user;
	int otherParty;
	float amount;
	short reason;
	int logEntry;
	int fboId;
	String location;
	int aircraftId;
	String comment;
	
	/**
	 * Constructor for PaymentBean.
	 */
	public PaymentBean(ResultSet rs) throws SQLException
	{
		id = rs.getLong("id");
		time = rs.getTimestamp("time");
		user = rs.getInt("user");
		otherParty = rs.getInt("otherParty");
		amount = rs.getFloat("amount");
		reason = rs.getShort("reason");
		logEntry = rs.getInt("logEntry");
		if (rs.getString("fbo") == null)
			fboId = -1;
		else
			fboId = rs.getInt("fbo");
		location = rs.getString("location");
		aircraftId = rs.getInt("aircraftid");
		comment = rs.getString("comment");
	}
	
	
	/**
	 * Normalize transaction, to amount is always positive
	 */
	public PaymentBean normalize()
	{
		if (amount > 0)
			return this;

		int backup = otherParty;
		otherParty = user;
		user = backup;
		amount *= -1;

		return this;
	}

	public String getSReason()
	{
		switch (reason)
		{
			case INTEREST_PAYMENT: return "Monthly Interest";
			case REASON_REFUEL: return "Refuelling with 100LL";
			case REASON_REFUEL_JETA: return "Refuelling with JetA";
			case AIRCRAFT_SALE: return "Aircraft sale";
			case AIRCRAFT_LEASE: return "Aircraft Lease";
			case GROUP_PAYMENT: return "Group payment";
			case GROUP_DELETION: return "Recovery of group money after deletion";
			case MAINTENANCE: return "Aircraft maintenance";
			case MAINTENANCE_FBO_COST: return "Cost of repairshop for doing aircraft maintenance";
			case EQUIPMENT: return "Install of equipment in aircraft";
			case EQUIPMENT_FBO_COST: return "Cost of repairshop for doing equipment install";
			case SALE_GOODS_FUEL: return "Sale of wholesale 100LL";
			case SALE_GOODS_JETA: return "Sale of wholesale JetA";
			case SALE_GOODS_BUILDING_MATERIALS: return "Sale of building materials";
			case SALE_GOODS_SUPPLIES: return "Sale of supplies";
			case ASSIGNMENT: return "Pay for assignment";
			case PILOT_FEE: return "Pilot fee";
			case RENTAL: return "Rental of aircraft";
			case LANDING_FEE: return "Landing fee";
			case CREW_FEE: return "Crew fee";
			case FBO_ASSIGNMENT_FEE: return "FBO ground crew fee";
			case FBO_SALE: return "FBO sale";
			case FBO_FACILITY_RENT: return "Facility rent";
			case MULTIPLE_PT_TAX: return "Booking Fee"; 
			case TRANSFER_GOODS_FUEL: return "Transfer of 100LL";
			case TRANSFER_GOODS_JETA: return "Transfer of JetA";
			case TRANSFER_GOODS_BUILDING_MATERIALS: return "Transfer of building materials";
			case TRANSFER_GOODS_SUPPLIES: return "Transfer of supplies";
			case AIRCRAFT_SHIPPING: return "Aircraft Shipment";
			case BULK_FUEL: return "Fuel Delivered";
            case TRANSFER_GROUP: return "Group Transfer";
			case BULK_SUPPLIES: return "Supplies Delivered";
			case OWNERSHIP_FEE: return "Ownership Fee";
			case EQUIPMENT_REMOVAL: return "Removal of equipment in aircraft";
			case EQUIPMENT_FBO_REMOVAL_COST: return "Cost of repairshop for doing equipment removal";
			default: return "Unknown";
		}
	}

	public float getAmount() {
		return amount;
	}

	public long getId() {
		return id;
	}

	public int getLogEntry() {
		return logEntry;
	}

	public int getOtherParty() {
		return otherParty;
	}

	public int getReason() {
		return reason;
	}

	public Timestamp getTime() {
		return time;
	}

	public int getUser() {
		return user;
	}

	public int getFboId() {
		return fboId;
	}
	
	public String getLocation() {
		return location;
	}
	
	public int getAircraftId() {
		return aircraftId;
	}
	
	public String getComment() {
		return comment;
	}
}
