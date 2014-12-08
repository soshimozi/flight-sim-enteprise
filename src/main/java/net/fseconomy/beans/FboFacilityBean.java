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

import net.fseconomy.util.Converters;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

public class FboFacilityBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final int DEFAULT_RENT = 1500;
	public static final int MAX_ASSIGNMENT_DISTANCE = 20000;
	//public static final int MAX_LOWPAX_DISTANCE = 250;
	public static final int MIN_UNITSPERTRIP_CAP_PERCENT = 50;

	//public static final int MAX_LOWPAX_DISTANCE = 300;
	
	public static String DEFAULT_COMMODITYNAME_PASSENGERS = "Air Taxi Passenger(s)";
	
	int id;
	String location;
	int fboId;
	int occupant;
	int reservedSpace;
	boolean isDefault;
	int size;
	int rent;
	boolean renew;
	boolean allowRenew;
	
	String name;
	int units;
	int minUnitsPerTrip;
	int maxUnitsPerTrip;
	String commodity;
	boolean allowWater;
	int minDistance, maxDistance;
	int matchMinSize, matchMaxSize;
	String icaoset;
	boolean publicByDefault;
	int daysActive, daysClaimedActive;
	
	public FboFacilityBean()
	{
	}
	
	public FboFacilityBean(ResultSet rs) throws SQLException
	{
		id = rs.getInt("id");
		location = rs.getString("location");
		fboId = rs.getInt("fboId");
		occupant = rs.getInt("occupant");
		reservedSpace = rs.getInt("reservedSpace");
		isDefault = reservedSpace >= 0;
		size = rs.getInt("size");
		rent = rs.getInt("rent");
		renew = rs.getBoolean("renew");
		allowRenew = rs.getBoolean("allowRenew");
		name = rs.getString("name");
		String s = rs.getString("units");
		if (s == null)
            s = "";

		units = AssignmentBean.unitsId(s);

		setMinMaxUnitsPerTrip(rs.getInt("minUnitsPerTrip"), rs.getInt("maxUnitsPerTrip"));
		commodity = rs.getString("commodity");
		allowWater = rs.getBoolean("allowWater");
		setMinMaxDistance(rs.getInt("minDistance"), rs.getInt("maxDistance"));
		matchMinSize = rs.getInt("matchMinSize");
		matchMaxSize = rs.getInt("matchMaxSize");
		icaoset = rs.getString("icaoset");
		publicByDefault = rs.getBoolean("publicByDefault");
		daysActive = rs.getInt("daysActive");
		daysClaimedActive = rs.getInt("daysClaimedActive");
	}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		//rs.updateBoolean("publicLogs", publicLogs);
		rs.updateInt("occupant", occupant);
		rs.updateInt("reservedSpace", reservedSpace);
		rs.updateInt("size", size);
		rs.updateInt("rent", rent);
		rs.updateBoolean("renew", renew);
		rs.updateBoolean("allowRenew", allowRenew);
		
		rs.updateString("name", name);
		rs.updateInt("minUnitsPerTrip", minUnitsPerTrip);
		rs.updateInt("maxUnitsPerTrip", maxUnitsPerTrip);
		rs.updateString("commodity", commodity);
		rs.updateBoolean("allowWater", allowWater);
		rs.updateInt("minDistance", minDistance);
		rs.updateInt("maxDistance", maxDistance);
		rs.updateInt("matchMinSize", matchMinSize);
		rs.updateInt("matchMaxSize", matchMaxSize);
		if (icaoset == null || "".equals(icaoset.trim()))
			rs.updateNull("icaoset");
		else
			rs.updateString("icaoset", icaoset);
		rs.updateBoolean("publicByDefault", publicByDefault);
		rs.updateInt("daysActive", daysActive);
		rs.updateInt("daysClaimedActive", daysClaimedActive);
	}
	
	public int getId()
	{
		return id;
	}

	public String getLocation()
	{
		return location;
	}

	public void setLocation(String s)
	{
		location = s;
	}

	public int getFboId()
	{
		return fboId;
	}

	public void setFboId(int i)
	{
		fboId = i;
	}

	public int getOccupant()
	{
		return occupant;
	}

	public void setOccupant(int i)
	{
		occupant = i;
	}

	public int getReservedSpace()
	{
		return reservedSpace;
	}

	public void setReservedSpace(int i)
	{
		reservedSpace = i;
	}

	public boolean getIsDefault()
	{
		return isDefault;
	}

	public int getSize()
	{
		return size;
	}

	public void setSize(int i)
	{
		size = i;
	}

	public int getRent()
	{
		return rent;
	}

	public void setRent(int i)
	{
		rent = i;
	}

	public boolean getRenew()
	{
		return renew;
	}

	public void setRenew(boolean b)
	{
		renew = b;
	}

	public boolean getAllowRenew()
	{
		return allowRenew;
	}

	public void setAllowRenew(boolean b)
	{
		allowRenew = b;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String s)
	{
		name = Converters.clearHtml(s);
	}

	public int getUnits()
	{
		return units;
	}

	public String getSUnits()
	{
		return AssignmentBean.getSUnits(units);
	}

	public int getMinUnitsPerTrip()
	{
		return minUnitsPerTrip;
	}

	public int getMaxUnitsPerTrip()
	{
		return maxUnitsPerTrip;
	}

	public void setMinMaxUnitsPerTrip(int min, int max)
	{
		maxUnitsPerTrip = max;
		int x = max / (100 / MIN_UNITSPERTRIP_CAP_PERCENT);
		minUnitsPerTrip = Math.min(min, x);
	}
	
	static String[] makeCommodityList(String commodity)
	{
		if (commodity == null || "".equals(commodity.trim()))
			return null;

        String items[] = commodity.trim().split(",\\ *");
		String item;
		List dowMatchList = new ArrayList();
		List noDowMatchList = new ArrayList();
		int wd = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        wd -= 1;
		if (wd == 0) // Mon..Sun instead of Sun..Sat
			wd = 7;

        String dow = Integer.toString(wd);

        for (String item1 : items)
        {
            item = item1;
            String[] params = item.trim().split("; *");

            // Name; weight; weekdays
            String name = params[0].trim();
            int weight = 1;
            String weekdays = "1234567";

            if (params.length > 1)
            {
                try
                {
                    weight = Integer.parseInt(params[1].trim());

                    if (weight > 10)
                        weight = 10;
                    else if (weight < 1)
                        weight = 1;
                }
                catch (NumberFormatException ignored)
                {
                }
            }

            if (params.length > 2)
            {
                weekdays = params[2].trim();
            }

            boolean dowmatch = false;

            if (weekdays.contains(dow))
            {
                dowmatch = true;
            }

            for (int i = 0; i < weight; i++)
            {
                if (dowmatch)
                    dowMatchList.add(name);

                noDowMatchList.add(name);
            }
        }

        if (dowMatchList.size() > 0)
			return (String[]) dowMatchList.toArray(new String[dowMatchList.size()]);
		else
			return (String[]) noDowMatchList.toArray(new String[noDowMatchList.size()]);
	}

    public String getRandomCommodity(int quantity)
	{
		String result = null;
		String[] commodities = FboFacilityBean.makeCommodityList(commodity);
		if (commodities != null && commodities.length > 0)
		{
			int i = (int)(Math.random() * commodities.length);
			if (i < commodities.length)
				result = commodities[i];
		}
		if (result == null || "".equals(result.trim()))
			result = FboFacilityBean.DEFAULT_COMMODITYNAME_PASSENGERS;
		if (quantity == 1)
			result = result.replaceAll("\\(s\\)", "");
		else
			result = result.replaceAll("\\(s\\)", "s");
		if (result.length() > 45)
			result = result.substring(0, 45).trim();
		return result;
	}

    public String getCommodity()
	{
		return commodity;
	}

    public void setCommodity(String s)
	{
		commodity = Converters.clearHtml(s);
	}

    public boolean getAllowWater()
	{
		return allowWater;
	}

    public void setAllowWater(boolean b)
	{
		allowWater = b;
	}

    public int getMinDistance()
	{
		return minDistance;
	}

    public int getMaxDistance()
	{
		return maxDistance;
	}

    public void setMinMaxDistance(int min, int max)
	{
		maxDistance = Math.min(max, MAX_ASSIGNMENT_DISTANCE);
		if (min >= maxDistance)
			min = 0;
		minDistance = min;
	}

    public int getMatchMinSize()
	{
		return matchMinSize;
	}

    public void setMatchMinSize(int i)
	{
		matchMinSize = i;
	}

    public int getMatchMaxSize()
	{
		return matchMaxSize;
	}

    public void setMatchMaxSize(int i)
	{
		matchMaxSize = i;
	}

    public String getIcaoSet()
	{
		return icaoset;
	}

    public void setIcaoSet(String s)
	{
		if (s != null)
			icaoset = s.trim();
		else
			icaoset = null;
	}

    public boolean getPublicByDefault()
	{
		return publicByDefault;
	}

    public void setPublicByDefault(boolean b)
	{
		publicByDefault = b;
	}
		
	public int getDaysActive()
	{
		return daysActive;
	}

    public void setDaysActive(int i)
	{
		daysActive = i;
	}

    public int getDaysClaimedActive()
	{
		return daysClaimedActive;
	}

    public void setDaysClaimedActive(int i)
	{
		daysClaimedActive = i;
	}
	
	/* public int getPay(int distance, int amount)
	{
		double slope = 0.4;     // Slope of pay dropoff. 0.4 = pay at slopeEnd is 40% of pay at slopeBegin
		int slopeBegin = 200;   // Distance where price begins to drop
		int slopeEnd = 950;     // Distance where price no longer drops
		int slopeLength = slopeEnd - slopeBegin;
		
		int X = distance - slopeBegin;
		X = Math.max(X, 0);
		X = Math.min(X, slopeLength);
		int Y = Math.min(slopeLength, distance);
		double DistanceModifier = (Y - X * slope) / Y;
		
		//int Bx = 200;  // Maximum base pay
		//int By = 100;   // Minimum base pay
		//double PayPerPax = Bx - (amount - 1) * ((Bx - By) / 26.0);
		
		double basePay = 400;
		double PayPerPax = basePay / Math.sqrt(amount);

		return (int)Math.round(DistanceModifier * PayPerPax);

	} */
	
	public double getPay(int distance, int amount)
	{
		//Variables from Spreadsheet
		double degree = 0.3;
		int maxfare = 1000;
		double crit_dist = 25.0;
		double pay;

        //Added PRD - Limit 1,2,3 pax to 300 mile pay - This is also in AssignmentBean.calcpay()
		//if (amount < 4 && distance > 300)
			//distance = 300;
			
		pay = (Math.pow(1/(double)(amount), degree)*(maxfare/2)*Math.atan((double)distance/crit_dist)/ Math.atan(1.0));

//System.out.println("=======================================================");
//System.out.println("ODistance: " + odistance + " Distance: " + distance + " Amount: " + amount);
//System.out.println("Pow: " + Math.pow(1/(double)(amount-1), degree));
//System.out.println("Maxfare/2: " + maxfare/2);
//System.out.println("atan dist: " + Math.atan((double)distance/crit_dist));		
//System.out.println("atan 1: " + Math.atan(1.0));
//System.out.println("Pay per pax: " + pay);
		return pay / distance * 100;
	
	}

    public boolean updateAllowed(UserBean who)
	{
		return who.getId() == occupant || who.groupMemberLevel(occupant) >= UserBean.GROUP_STAFF;
	}
	
	public boolean deleteAllowed(UserBean who)
	{
		return who.getId() == occupant || who.groupMemberLevel(occupant) >= UserBean.GROUP_OWNER;
	}

    public String getParametersDesc()
	{
		String result;
		if (icaoset == null || "".equals(icaoset))
		{
			result = minDistance + "-" + maxDistance + " NM, ";
			int size1;
			if (matchMinSize < AirportBean.MIN_SIZE_MED)
				size1 = 1;
			else if (matchMinSize < AirportBean.MIN_SIZE_BIG)
				size1 = 2;
			else
				size1 = 3;
			int size2;
			if (matchMaxSize < AirportBean.MIN_SIZE_MED)
				size2 = 1;
			else if (matchMaxSize < AirportBean.MIN_SIZE_BIG)
				size2 = 2;
			else
				size2 = 3;
			
			switch (size1)
			{
				case 1: result = result + "Airstrip"; break;
				case 2: result = result + "Small Airport"; break;
				default: result = result + "Large Airport"; break;
			}
			if (size1 != size2)
			{
				switch (size2)
				{
					case 1: result = result + "-Airstrip"; break;
					case 2: result = result + "-Small Airport"; break;
					default: result = result + "-Large Airport"; break;
				}
			}
		} else
			result = icaoset;
		
		return result;
	}
}
