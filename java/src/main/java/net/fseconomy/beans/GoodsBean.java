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

public class GoodsBean implements Serializable
{
	private static final long serialVersionUID = 1L;
    public static final int SALEFLAG_SELL = 1;					// Transfer from market to user
    public static final int SALEFLAG_BUY = 2; 					// Transfer from user to market
	
	public static final int GOODS_BUILDING_MATERIALS = 1;
	public static final int GOODS_SUPPLIES = 2;
	public static final int GOODS_FUEL100LL = 3;
	public static final int GOODS_FUELJETA = 4;

	public static final int GOODS_LOWEST = GOODS_BUILDING_MATERIALS;
	public static final int GOODS_HIGHEST = GOODS_FUELJETA;
	
	public static final int CONSTRUCT_FBO = 10000;				// 10.000 kg needed to construct an FBO
	public static final int CONSTRUCT_REPAIRSHOP = 2000;		// 2.000 kg needed to construct repair shop
	public static final int CONSTRUCT_PASSENGERTERMINAL = 2000;
	public static final int AMOUNT_FBO_SUPPLIES_PER_DAY = 10;  // 10 kg supplies per day for an FBO

	public static final int GOODS_ORDER_FUEL_MIN = 0;
	public static final int GOODS_ORDER_FUEL_MAX = 3;
	public static final int GOODS_ORDER_SUPPLIES_MIN = 5;
	public static final int GOODS_ORDER_SUPPLIES_MAX = 10;
	public static final double GOODS_ORDER_SUPPLIES_MULTIPLIER = 1.5d;

	String location;
	String commodity;
	String ownerName;
	int type;
	int owner;
	int amount;											// For owner == 0, this indicates the overstock at the airport
	boolean sell, buy;
	double priceBuy, priceSell;
	int maxDiscount, maxDiscountAmount;
	int retain, max;
	
	public GoodsBean()
	{
		super();
	}
	
	// Create goods from an anonymous resource at a large airport
	public GoodsBean(CommodityBean commodity, String location, int airportSize, double fuelPrice, int overstock, double JetAPrice)
	{
		this.ownerName = null;
		this.location = location;
		this.commodity = commodity.getName();
		this.type = commodity.getId();
		this.owner = 0;
		this.amount = -1;
		this.sell = true;
		this.buy = true; 			
		this.priceBuy = commodity.getKgBuyPrice(airportSize, fuelPrice, overstock, JetAPrice);
		this.priceSell = commodity.getKgSalePrice(0, airportSize, fuelPrice, overstock, JetAPrice);
		this.maxDiscount = commodity.getDiscount();
		this.maxDiscountAmount = commodity.getMaxDiscountAmount();
		this.retain = 0;
		this.max = 0;		
	}
	
	// Create goods from the goods table
	public GoodsBean(ResultSet rs) throws SQLException
	{
		this.ownerName = rs.getString("accounts.name");
		this.commodity = rs.getString("commodities.name");
		this.location = rs.getString("location");
		this.type = rs.getInt("type");
		this.owner = rs.getInt("owner");
		this.amount = rs.getInt("amount");
		this.setSaleFlag(rs.getInt("saleFlag"));
		this.priceBuy = rs.getDouble("buyPrice");
		this.priceSell = rs.getDouble("sellPrice");
		this.retain = rs.getInt("retain");
		this.max = rs.getInt("max");
	}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		rs.updateInt("saleFlag", getSaleFlag());
		rs.updateInt("retain", retain);
		rs.updateInt("max", max);
		rs.updateDouble("BuyPrice", priceBuy);
		rs.updateDouble("SellPrice", priceSell);
	}
	
	public boolean changeAllowed(UserBean who)
	{
        return who != null
                && (who.getId() == owner || who.groupMemberLevel(owner) >= UserBean.GROUP_STAFF);
    }
	
	public int getAmountForSale()
	{
		if (owner == 0)					// owner 0 always has an infinite amount
			return -1;

		if (!sell)
			return 0;

		return amount > retain ? (amount - retain) : 0; 
	}
	
	public int getAmountAccepted()
	{
		if (owner == 0 || max == 0)					// owner 0 always has an infinite amount
			return -1;

		if (!buy)
			return 0;

		return amount < max ? (max - amount) : 0; 
	}
		
	public int getAmount()
	{
		return amount;
	}

	public String getLocation()
	{
		return location;
	}

	public int getOwner()
	{
		return owner;
	}

	public double getPriceBuy()
	{
		return priceBuy;
	}

	public int getRetain()
	{
		return retain;
	}

	public int getType()
	{
		return type;
	}

	public void setAmount(int i)
	{
		amount = i;
	}

	public void setLocation(String string)
	{
		location = string;
	}

	public void setOwner(int i)
	{
		owner = i;
	}

	public void setRetain(int i)
	{
		retain = i;
	}

	public void setType(int i)
	{
		type = i;
	}

	public int getSaleFlag()
	{
		int saleFlag = 0;

		if (sell)
			saleFlag |= SALEFLAG_SELL;

		if (buy)
			saleFlag |= SALEFLAG_BUY;
			
		return saleFlag;
	}

	public void setSaleFlag(int i)
	{
		this.sell = (i & SALEFLAG_SELL) > 0;
		this.buy = (i & SALEFLAG_BUY) > 0;
	}
	
	public String getBuySell()
	{
		return (buy && sell) ? "Buy/Sell" : buy ? "Buy" : sell ? "Sell" : "";
	}

	public double getPriceSell()
	{
		return priceSell;
	}

	public String getCommodity()
	{
		return commodity;
	}

	public boolean isBuy()
	{
		return buy;
	}

	public boolean isSell()
	{
		return sell;
	}

	public int getMax()
	{
		return max;
	}

	public int getMaxDiscount()
	{
		return maxDiscount;
	}

	public String getOwnerName()
	{
		return ownerName == null ? "[Local Market]" : ownerName;
	}

	public void setBuy(boolean b)
	{
		buy = b;
	}

	public void setSell(boolean b)
	{
		sell = b;
	}

	public void setPriceBuy(double d)
	{
		priceBuy = d;
	}

	public void setPriceSell(double d)
	{
		priceSell = d;
	}

	public void setMax(int i)
	{
		max = i;
	}

	public int getMaxDiscountAmount()
	{
		return maxDiscountAmount;
	}
}
