/*
 * Created on Aug 9, 2005
 *
 *
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

import net.fseconomy.util.Formatters;


/**
 * @author Marty
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AircraftMaintenanceBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	public static final int DAMAGE_RUNTIME = 0;
	public static final int DAMAGE_HEATING = 1;
	public static final int DAMAGE_MIXTURE = 2;
	
	public static final int MAINT_CYLINDERCLEANING = 1;
	public static final int MAINT_WORSTCOMPRESSION = 2;
	public static final int MAINT_ENGINETIME = 3;
	public static final int MAINT_TYPE = 5;
	public static final int MAINT_MARGIN = 6;
	
	public static final int MAINT_100HOUR = 1;
	public static final int MAINT_REPLACEENGINE = 2;
	public static final int MAINT_FIXAIRCRAFT = 3;
	public static final int MAINT_SHIPMENTDISASSEMBLY = 4; //added Airboss 1/22/11
	public static final int MAINT_SHIPMENTREASSEMBLY = 5; //added Airboss 1/22/11
	
	public static final int REPAIR_AVAILABLE_AIRPORT_SIZE  = 2000; // TODO: Find suitable value
	
	AircraftBean aircraft;
	int type;
	int margin;
	double totalPrice;
	FboBean fbo;
	boolean isPistonEngine;
	int repairPrice;
	int ageECost;
	int ageAvCost;
	int ageAfCost;
	int ageAdwCost;
	
	class EngineData
	{
		public int engineTime;
		public int incorrectMixture;
		public int heating; 
		
		public int cylinderCleaning;
		public int worstCompression;
		public int logEngineTime;
		
		
		
		public int maintenance()
		{
			int price = 0;
			logEngineTime = engineTime;
			// First do a compression check
			if (!isPistonEngine)
				return price;
			
				// Cracks in cylinder, reduced compression
				worstCompression = 100 - heating/10000;				// TODO: find suitable value
				
				if (worstCompression < 80)
					worstCompression = 80;
			
			if (heating < 10000 && incorrectMixture > 10 * 3600) 	// TODO: find suitable value
			{
				// Too much deposits
				worstCompression = 100 + (int)(Math.random() * 5);
			} 
			
			// If there's too much carbon, fix the engine
			if (incorrectMixture > 10 * 3600)
			{
				double factor = (Math.random() * 0.20) + 0.60;		// Cleaning carbon, betweem 60% and 80% effective
		
				cylinderCleaning = (int) Math.round(incorrectMixture * factor);
				incorrectMixture -= cylinderCleaning;
				price += cylinderCleaning/360;						// Cost = $10 per hour of carbon removal
			}
			return (int)(price * marginFactor());
		}
		
		public void addRecord(ResultSet rs, int log, int engine, int param, int value) throws SQLException
		{
			rs.moveToInsertRow();
			rs.updateInt("log", log);
			rs.updateInt("engine", engine);
			rs.updateInt("type", param);	
			rs.updateInt("value", value);
			rs.insertRow();				
		}
		public void writeRecord(ResultSet rs, int log, int engine) throws SQLException
		{
			if (cylinderCleaning > 0)
				addRecord(rs, log, engine, MAINT_CYLINDERCLEANING, cylinderCleaning);
			if (worstCompression > 0)
				addRecord(rs, log, engine, MAINT_WORSTCOMPRESSION, worstCompression);
			addRecord(rs, log, engine, MAINT_ENGINETIME, logEngineTime);
		}
		
		public String report(boolean printNumber, int number)
		{
			StringBuffer report = new StringBuffer();
			if (printNumber)
			{
				report.append("<tr><td class=\"oneunderline\">Engine " + number + "</td></tr>\n");
			} else
			{
				report.append("<tr><td class=\"oneunderline\">Engine information</td></tr>\n");
			}
			report.append("<tr><td class=\"one\">Engine running time: " + logEngineTime/3600 + " hours.</td></tr>");
			if (cylinderCleaning > 0)
			{
				int hours = cylinderCleaning/3600;
				String severity = hours > 40 ? "serious" : hours > 20 ? "moderate" : "low"; 
				report.append("<tr><td class=\"one\">A " + severity + " amount of carbon deposits was detected and removed from the engine cylinders and " +
					"exhaust valves.</td><td class=\"two\">"+ Formatters.currency.format(Math.round(marginFactor() * cylinderCleaning/360)) + "</td></tr>\n");
			} else {
				report.append("<tr><td class=\"one\">The engine looks clean.</td></tr>\n");
			}
			if (worstCompression > 0)
				report.append("<tr><td class=\"one\">Compression test shows the worst cylinder at " + worstCompression + "% compression.</td></tr>\n");
			return report.toString();
		}
	}
	
	EngineData[] engineData;
	
	AircraftMaintenanceBean()
	{
		super();
	}
	
	AircraftMaintenanceBean(AircraftBean aircraft, int type)
	{
		this.aircraft = aircraft;
		this.type = type;
		this.engineData = new EngineData[aircraft.getEngines()];
		this.isPistonEngine = false;
		for (int c=0; c < engineData.length; c++)
			this.engineData[c] = new EngineData();
	}
	
	// Construct a mainteanceBean for pending maintenance
	AircraftMaintenanceBean(AircraftBean aircraft, int type, FboBean fbo)
	{
		this(aircraft, type);
		this.margin = fbo.getRepairShopMargin();
		this.fbo = fbo; 
	}
	
	/* Construct a maintenanceBean from a maintenance record - I think we can remove this one.
	MaintenanceBean(ResultSet rs, AircraftBean aircraft, double totalPrice, FboBean fbo) throws SQLException
	{
		this(aircraft, MaintenanceBean.MAINT_100HOUR);
		this.totalPrice = totalPrice;
		this.fbo = fbo;
		while (rs.next())
		{
			int engine = rs.getInt("engine");
			int value = rs.getInt("value");
			if (engine > engineData.length)
				continue;
			engine--;
			switch (rs.getInt("type"))
			{
				case MAINT_MARGIN:
					this.margin = value;
					break;					
				case MAINT_TYPE:
					this.type = value;
					break;
				case MAINT_CYLINDERCLEANING:
					engineData[engine].cylinderCleaning = value;
					break;
				case MAINT_WORSTCOMPRESSION:
					engineData[engine].worstCompression = value;
					break;
				case MAINT_ENGINETIME:
					engineData[engine].logEngineTime = value;
					break;
					
			}
		}		
	}
*/
	// Construct a maintenanceBean from a maintenance record for 100-hour inspections only

	public AircraftMaintenanceBean(ResultSet rs, AircraftBean aircraft,double totalPrice, FboBean fbo, int ageECost,
			int ageAvCost,int ageAfCost, int ageAdwCost) throws SQLException
	{
		this(aircraft, AircraftMaintenanceBean.MAINT_100HOUR);
		this.totalPrice = totalPrice;
		this.fbo = fbo;
		this.ageECost = ageECost;
		this.ageAvCost = ageAvCost;
		this.ageAfCost = ageAfCost;
		this.ageAdwCost = ageAdwCost;
		
		while (rs.next())
		{
			int engine = rs.getInt("engine");
			int value = rs.getInt("value");
			if (engine > engineData.length)
				continue;
			engine--;
			switch (rs.getInt("type"))
			{
				case MAINT_MARGIN:
					this.margin = value;
					break;					
				case MAINT_TYPE:
					this.type = value;
					break;
				case MAINT_CYLINDERCLEANING:
					engineData[engine].cylinderCleaning = value;
					break;
				case MAINT_WORSTCOMPRESSION:
					engineData[engine].worstCompression = value;
					break;
				case MAINT_ENGINETIME:
					engineData[engine].logEngineTime = value;
					break;
					
			}
		}
	}

	void loadEngineData(ResultSet rs) throws SQLException
	{
		while (rs.next())
		{
			int engine = rs.getInt("engine");
			if (engine <= 0)
				continue;
			if (engine > engineData.length)
				continue;

			engine--;
			int value = rs.getInt("value");
			
			switch (rs.getInt("parameter"))
			{
				case DAMAGE_RUNTIME:
					engineData[engine].engineTime = value;
					break;
				case DAMAGE_HEATING:
					engineData[engine].heating = value;
					isPistonEngine = true;
					break;
				case DAMAGE_MIXTURE:
					engineData[engine].incorrectMixture = value;
					isPistonEngine = true;
					break; 
			}
		}
	}
	
	void writeEngineData(ResultSet rs) throws SQLException
	{
		while (rs.next())
		{
			int engine = rs.getInt("engine");
			if (engine <= 0)
				continue;
			if (engine > engineData.length)
				continue;

			engine--;
			switch (rs.getInt("parameter"))
			{
				case DAMAGE_RUNTIME:
					rs.updateInt("value", engineData[engine].engineTime);
					break;
				case DAMAGE_HEATING:
					rs.updateInt("value", engineData[engine].heating);
					break;
				case DAMAGE_MIXTURE:
					rs.updateInt("value", engineData[engine].incorrectMixture);;
					break; 
			}
			rs.updateRow();
		}		
	}
	
	void writeMaintenanceLog(ResultSet rs, int log) throws SQLException
	{
		engineData[0].addRecord(rs, log, 0, MAINT_TYPE, type);
		engineData[0].addRecord(rs, log, 0, MAINT_MARGIN, margin);
		for (int c=0; c < engineData.length; c++)
		{
			engineData[c].writeRecord(rs, log, c + 1);
		}
	}
	
	int doMaintenance(ResultSet damageRs, ResultSet maintenanceRs, int log) throws SQLException
	{
		int totalCost = 0;
		
		loadEngineData(damageRs);
		damageRs.beforeFirst();
		for (int c=0; c < engineData.length; c++)
			totalCost += engineData[c].maintenance();  
		writeEngineData(damageRs);
		writeMaintenanceLog(maintenanceRs, log);		
		return totalCost;		
	}

	int calculateRepair()
	{
		repairPrice = (int) (1000 + (3000 - 1000) * Math.random());
		return repairPrice;
	}
	
	int doRepair(ResultSet damageRs, ResultSet maintenanceRs, int log) throws SQLException
	{
		calculateRepair();
		writeMaintenanceLog(maintenanceRs, log);
		return repairPrice;
	}
	public String report()
	{
		StringBuffer report = new StringBuffer();
		report.append("<table>");
		report.append("<tr><td class=\"one\">Aircraft Serviced: " + aircraft.getRegistration() + "</td><td class=\"two\"></td></tr>");
		report.append("<tr><td class=\"oneunderline\">Maintenance Handled By: " + fbo.getName() + "</td><td class=\"two\"></td></tr>");
		
		if (type == MAINT_FIXAIRCRAFT)
			report.append("<tr><td class=\"one\">" + maintenanceType(type) + "</td><td class=\"two\">" + Formatters.currency.format(totalPrice) + "</td></tr>");
		else if(type == MAINT_SHIPMENTDISASSEMBLY)
			report.append("<tr><td class=\"one\">Aircraft Disassembly for Shipment </td><td class=\"two\">" + Formatters.currency.format(totalPrice) + "</td></tr>");				
		else if(type == MAINT_SHIPMENTREASSEMBLY)
			report.append("<tr><td class=\"one\">Aircraft Reassembly for Shipment </td><td class=\"two\">" + Formatters.currency.format(totalPrice) + "</td></tr>");				
		else
			report.append("<tr><td class=\"one\">" + maintenanceType(type) + "</td><td class=\"two\">" + Formatters.currency.format(marginFactor() * aircraft.getRawMaintenancePrice(type)) + "</td></tr>");
		
		if (type == MAINT_100HOUR)
		{
			report.append("<tr><td class=\"one\">Additional Engine Repair </td><td class=\"two\">" + Formatters.currency.format(Math.round(marginFactor() * ageECost)) + "</td></tr>");				
			report.append("<tr><td class=\"one\">Avionics inspection & repair </td><td class=\"two\">" + Formatters.currency.format(Math.round(marginFactor() * ageAvCost)) + "</td></tr>");	
			report.append("<tr><td class=\"one\">Airframe inspection & repair </td><td class=\"two\">" + Formatters.currency.format(Math.round(marginFactor() * ageAfCost)) + "</td></tr>");					
			report.append("<tr><td class=\"one\">Airworthiness Directives Compliance </td><td class=\"two\">" + Formatters.currency.format(Math.round(marginFactor() * ageAdwCost)) + "</td></tr>");	
			report.append("<tr><td>&nbsp;</td></tr>");
			for (int c=0; c < engineData.length; c++)
			{	
					if (c > 0 )	
					report.append("<tr><td>&nbsp;</td></tr>");
					report.append(engineData[c].report(engineData.length>1, c + 1));
			}
		}
		report.append("</table>");
		report.append("<div id=\"total\">" + Formatters.currency.format(totalPrice)+ "</div>");
		
		return report.toString();
	}
	
	public static String maintenanceType(int type)
	{
		switch(type)
		{
			case AircraftMaintenanceBean.MAINT_100HOUR : 
				return "100 hour check";
				
			case AircraftMaintenanceBean.MAINT_REPLACEENGINE : 
				return "engine replacement";
				
			case AircraftMaintenanceBean.MAINT_FIXAIRCRAFT: 
				return "Aircraft Repair";

			case AircraftMaintenanceBean.MAINT_SHIPMENTDISASSEMBLY: //added Airboss 1/22/11
				return "Shipment disassembly";

			case AircraftMaintenanceBean.MAINT_SHIPMENTREASSEMBLY: //added Airboss 1/22/11
				return "Shipment reassembly";

			default:
				return null;
		}		
	}
	
	/**
	 * @return
	 */
	public FboBean getFbo()
	{
		return fbo;
	}
	double marginFactor()
	{
		return 1 + (margin/100.0);
	}
	
}
