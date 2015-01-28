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
import java.sql.Timestamp;
import java.util.Date;

public class FboBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	String name;
	String location;
	boolean active;
	boolean invoiceBackground;
	int owner;
	double fuel100LL,fueljeta;
	int services;
	int repairShopMargin;
	int id;
	int equipmentInstallMargin;
	int price;
	boolean priceincludesgoods;
	int fbosize;
	boolean publicLogs = false;
	
	//fuel delivery delay changes - gurka
	Date bulkFuelOrderTimeStamp=null,  bulkFuelDeliveryDateTime=null;
	int bulk100llOrdered, bulkJetAOrdered;
	
	
	public static final int FBO_REPAIRSHOP			= 1;
	public static final int FBO_RESTAURANT			= 2;
	public static final int FBO_PASSENGERTERMINAL	= 4;
	
	public static final int FBO_DEFAULT_REPAIRSHOPMARGIN = 25;
	public static final int FBO_DEFAULT_EQUIPMENTMARGIN = 50;
	
	public FboBean()
	{
	}
	
	public FboBean(String location, int owner)
	{
		this.location = location;
		this.owner = owner;
	}
	
	public static FboBean getInstance()
	{
		FboBean fbo = new FboBean(null, 0);
		fbo.setRepairShopMargin(FboBean.FBO_DEFAULT_REPAIRSHOPMARGIN);
		fbo.setEquipmentInstallMargin(FboBean.FBO_DEFAULT_EQUIPMENTMARGIN);
		fbo.active = true;
		fbo.invoiceBackground = false;
		fbo.name = "local FBO";
		fbo.services = FboBean.FBO_REPAIRSHOP;
		fbo.id = 0;
		return fbo;
	}
	
	public FboBean(ResultSet rs) throws SQLException
	{
		name = rs.getString("name");
		owner = rs.getInt("owner");
		fuel100LL = rs.getDouble("fuel100ll");
		fueljeta = rs.getDouble("fueljeta");
		services = rs.getInt("services");
		active = rs.getInt("active") > 0;
		location = rs.getString("location");
		repairShopMargin = rs.getInt("margin");
		invoiceBackground = (rs.getObject("invoice") != null); //Had to change from getBlob to prevent type mismatch
		id = rs.getInt("id");	
		equipmentInstallMargin = rs.getInt("equipmentmargin");
		price = rs.getInt("saleprice");
		priceincludesgoods = true;
		fbosize = rs.getInt("fbosize");
		bulkFuelOrderTimeStamp = rs.getTimestamp("bulkFuelOrderTimeStamp");
		bulk100llOrdered = rs.getInt("bulk100llOrdered");
		bulkJetAOrdered = rs.getInt("bulkJetAOrdered");
		bulkFuelDeliveryDateTime = rs.getTimestamp("bulkFuelDeliveryDateTime");
	}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		rs.updateInt("services", services);
		rs.updateString("name", name == null ? "" : name);
		rs.updateDouble("fuel100ll", fuel100LL);
		rs.updateDouble("fueljeta", fueljeta);
		rs.updateInt("margin", repairShopMargin);
		rs.updateInt("equipmentmargin",equipmentInstallMargin);
		rs.updateInt("saleprice", price);
		rs.updateInt("saleincludesgoods", 1);
		
		//bulk fuel changes
		if (bulkFuelOrderTimeStamp != null)
			rs.updateTimestamp("bulkFuelOrderTimeStamp", new Timestamp(bulkFuelOrderTimeStamp.getTime()));
		
		rs.updateInt("bulk100llOrdered", bulk100llOrdered);
		rs.updateInt("bulkJetAOrdered", bulkJetAOrdered);
		
		if (bulkFuelDeliveryDateTime != null)
			rs.updateTimestamp("bulkFuelDeliveryDateTime", new Timestamp(bulkFuelDeliveryDateTime.getTime()));
		
		//rs.updateBoolean("publicLogs", publicLogs);
	}
	
	public boolean updateAllowed(UserBean who)
	{
		return who.getId() == owner || who.groupMemberLevel(owner) >= UserBean.GROUP_STAFF;
	}
	
	public boolean deleteAllowed(UserBean who)
	{
		return who.getId() == owner || who.groupMemberLevel(owner) >= UserBean.GROUP_OWNER;
	}
	
	public int recoverableBuildingMaterials()
	{
		int result = GoodsBean.CONSTRUCT_FBO;
		if ((fbosize == 1) && ((services & FboBean.FBO_REPAIRSHOP) > 0))
			result += GoodsBean.CONSTRUCT_REPAIRSHOP;
		if ((fbosize == 1) && ((services & FboBean.FBO_PASSENGERTERMINAL) > 0))
			result += GoodsBean.CONSTRUCT_PASSENGERTERMINAL;
		return (int)Math.round(result * 0.6);
	}
	
	public void setServices(int service)
	{
		services = service;
	}
	
	public String getInvoiceBackground()
	{
		return invoiceBackground ? ("Image?id=" + id) : "img/invoice.jpg";
	}
	
	/**
	 * gets fuel price at an FBO by type 0=avgas 1=jeta
	 * @param d - fueltype returned by aircraft.getFuelType()
	 * @return double - either price of 100LL or JetA
	 */
	public double getFuelByType(int d)
	{
		if (d < 1)
		{
			return fuel100LL;
		} else {
			return fueljeta;
		}
	}
	
	public double getFuel100LL()
	{
		return fuel100LL;
	}

	/**
	 * for getting jeta price
	 * @return double
	 */
	public double getFueljeta()
	{
		return fueljeta;
	}

    public void setLocation(String loc)
    {
         location = loc;
    }
	public String getLocation()
	{
		return location;
	}

	public String getName()
	{
		return name == null ? "" : name;
	}

	public void setOwner(int pOwner)
	{
		owner = pOwner;
	}

	public int getOwner()
	{
		return owner;
	}

	public boolean isActive()
	{
		return active;
	}
	public void setActive(int pActive)
	{
		active = pActive > 0;
	}

	public void setFuel100LL(double d)
	{
		fuel100LL = d;
	}

	public void setFueljeta(double d)
	{
		fueljeta = d;
	}

	public void setName(String string)
	{
		name = Converters.clearHtml(string);
	}

	public int getServices()
	{
		return services;
	}

	public String getSServices()
	{
		return repairShopMargin + "% / " + equipmentInstallMargin + "%";
	}

	public int getRepairShopMargin()
	{
		return repairShopMargin;
	}

	public void setRepairShopMargin(int i)
	{
		repairShopMargin = i;
		if (repairShopMargin < 0)
			repairShopMargin = 0;
	}

	public int getEquipmentInstallMargin()
	{
		return equipmentInstallMargin;
	}

	public void setEquipmentInstallMargin(int i)
	{
		equipmentInstallMargin = i;
		if (equipmentInstallMargin < 0)
			equipmentInstallMargin = 0;
	}
	
	public void setId(int pId)
	{
		id = pId;
	}

	public int getId()
	{
		return id;
	}

	public int getPrice()
	{
		return price;
	}
	
	public void setPrice(int i)
	{
		price = i;
		if (price < 0)
			price = 0;
	}
	
	public boolean isForSale()
	{
		return price > 0;
	}
	
	public boolean getPriceIncludesGoods()
	{
		return priceincludesgoods;
	}

	public void setFboSize(int pSize)
	{
		 fbosize = pSize;
	}
	public int getFboSize()
	{
		return fbosize;
	}
	
	public int getSuppliesPerDay(int airportLots)
	{
		return fbosize * airportLots * GoodsBean.AMOUNT_FBO_SUPPLIES_PER_DAY;
	}
	
	public boolean getPublicLogs()
	{
		return publicLogs;
	}
	public boolean logsVisibleToAll()
	{
		return isForSale() && getPublicLogs();
	}
	
	public Date getBulkFuelOrderTimeStamp()	{ return bulkFuelOrderTimeStamp; }
	public int getBulk100llOrdered(){ return bulk100llOrdered; }	
	public int getBulkJetAOrdered()	{ return bulkJetAOrdered; }	
	public Date getBulkFuelDeliveryDateTime(){	return bulkFuelDeliveryDateTime; }
	
}
