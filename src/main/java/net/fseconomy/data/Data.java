/*
 * FS Economy
 * Copyright (C) 2005, 2006, 2007  Marty Bochane
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

import java.io.*;
import java.sql.*;
import java.util.*;

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.dto.*;
import net.fseconomy.util.GlobalLogger;

public class Data implements Serializable
{
	private static final long serialVersionUID = 3L;

    //for Signature template selection
    public static int currMonth = 1;

    //Current Admin rest api key
    public static String adminApiKey = "";
    
	private static Data singletonInstance = null;
	
	public static Data getInstance()
	{
		return singletonInstance;
	}
	
	public Data()
	{
		Locale.setDefault(Locale.US);
		
		singletonInstance = this;
	}

	public static List<String> getDistinctColumnData(String field, String table)throws DataError
	{
		ArrayList<String> result = new ArrayList<>();
		String qry = "";
		try
		{
			qry = "SELECT DISTINCT "+ field +" FROM "+ table +" ORDER BY " + field;
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				String nfield = rs.getString(field);
				result.add(nfield);								
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
			GlobalLogger.logDebugLog(qry, Data.class);
		} 

		if (result.size() == 0)
			throw new DataError("Nothing to Return!");
		
		return result;
	}

	public static String sortHelper(String helper)
	{
		return "<span style=\"display: none\">" + helper + "</span>";
	}

	/**
	* return a mysql resultset as an ArrayList. Mysql query of log table to return a users last 500 flights 
	* and calulate the total hours flown in last 48 hours.
	* @ param users - user name input from checkuser48hourtrend.jsp. Access this screen from admin.jsp
	* @ return Mysql Resulset as an ArrayList to checkuser48hourtrend.jsp
	* @ author - chuck229
	*/
	public static List<TrendHours> getTrendHoursQuery(int userId, int limit)
	{
        if(limit <= 0)
            limit = 1; //minimum

		ArrayList<TrendHours> result = new ArrayList<>();
		try
		{
			String qry = "SELECT `time` as LOGDATE, cast(flightenginetime as signed) as Duration, cast((SELECT SUM(flightenginetime) FROM `log` where userid = ? and `time` <= LOGDATE and `time` > DATE_SUB(LOGDATE, INTERVAL 48 HOUR)) as signed) as last48hours FROM log WHERE `userid` = ? and TYPE = 'flight' ORDER BY TIME DESC Limit " + limit;
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, userId, userId);
			while (rs.next())
			{
				TrendHours trend = new TrendHours(rs.getString("LOGDATE"),rs.getInt("Duration"), rs.getInt("last48hours"));
				result.add(trend);
			}				
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}


	
	public static  List<FuelExploitCheck> getFuelExploitCheckData(int pricepoint, int count)
	{
		List<FuelExploitCheck> list = new ArrayList<>();
		String qry;
		
		try
		{
			qry = "select p.*, a1.name as userName, a1.Type as userNameType, a2.name as otherPartyName, a2.type as otherPartyNameType, a3.name as buyerName, a3.type buyerNameType from " +
			"(select *, cast(substring(comment, locate('ID: ', comment)+4) as unsigned) as buyer,  cast(substring(comment, locate('Gal: $', comment)+6) as decimal(18,2)) as PerGal from payments   where (reason = 1 or reason = 22) and cast(substring(comment, locate('Gal: $', comment)+6) as decimal(18,2))  >= ?) p " +
			"join accounts a1 on  a1.id=p.user " +
			"join accounts a2 on  a2.id=p.otherParty " +
			"join accounts a3 on  a3.id=p.buyer " +
			"order by id desc " +
			"limit ?";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, pricepoint, count);
			
			while(rs.next())
			{
				FuelExploitCheck fec = new FuelExploitCheck();
				int acid = rs.getInt("aircraftid");
				AircraftBean ac = Aircraft.getAircraftById(acid);
				fec.Aircraft = ac.getRegistration() + " [" + acid + "]";
				fec.Buyer = rs.getString("buyerName") + (rs.getString("buyerNameType").equals("group") ? " (group)" : "");				
				fec.FuelType = rs.getInt("reason") == 1 ? "100LL": "JetA";				
				fec.Location = rs.getString("location");				
				fec.OtherParty = rs.getString("otherPartyName") + (rs.getString("otherPartyNameType").equals("group") ? " (group)" : "");				
				fec.PaymentId = rs.getInt("id");				
				fec.PerGal = rs.getBigDecimal("PerGal");				
				fec.Time = rs.getTimestamp("time");				
				fec.TotalAmount = rs.getFloat("amount");				
				fec.User = rs.getString("userName") + (rs.getString("userNameType").equals("group") ? " (group)" : "");
				fec.Comment = rs.getString("comment");
				
				list.add(fec);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		return list;
	}



	public static List<PilotStatus> getPilotStatus()
	{
		List<PilotStatus> toList = new ArrayList<>();
		
		try
		{
			String qry = "select  a.name, Concat(m.make,  \" \", m.model) as makemodel, case when d.location is not null then  \"p\" else \"f\" end as status, case when d.location is not null then  d.location else d.departedfrom end as icao,  case when d.location is not null then  loc.lat else dep.lat end as lat, case when d.location is not null then  loc.lon else dep.lon end as lon  from (select model, location, departedfrom, userlock from aircraft where userlock is not null) d left join accounts a on d.userlock=a.id left join models m on m.id=d.model left join airports loc on loc.icao=d.location left join airports dep on dep.icao=d.departedfrom order by status";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

			while(rs.next())
			{
				PilotStatus ou = new PilotStatus();
				ou.a = rs.getString("name");
				ou.b = rs.getString("makemodel");
				ou.c = rs.getString("icao");
				ou.d = rs.getFloat("lat");
				ou.e = rs.getFloat("lon");
				ou.f = rs.getString("status");
				
				toList.add(ou);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return toList;
	}
}