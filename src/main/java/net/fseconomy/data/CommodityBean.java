/*
 * Created on May 10, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.fseconomy.data;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Marty
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CommodityBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	String name;
	int id;
	int minAirportSize;
	double basePrice;	 
	int discount, maxDiscountAmount;
	int overstockDiscount, maxOverstockDiscountAmount;
	double sizeFactor, fuelFactor;
	
	public CommodityBean()
	{
		super();
	}
	
	public CommodityBean(ResultSet rs) throws SQLException
	{
		this.id = rs.getInt("id");
		this.name = rs.getString("name");
		this.minAirportSize = rs.getInt("minairportsize");
		this.basePrice = rs.getDouble("baseprice");
		this.discount = rs.getInt("discount");
		this.maxDiscountAmount = rs.getInt("maxdiscountamount");
		this.sizeFactor = rs.getDouble("sizefactor");
		this.fuelFactor = rs.getDouble("fuelfactor");
		this.overstockDiscount = rs.getInt("overstockDiscount");
		this.maxOverstockDiscountAmount = rs.getInt("maxOverstockDiscountAmount");
	}
	
	public double getWeightedPrice(int airportSize, double fuelPrice, int overstock, double JetAPrice)
	{
		double price = basePrice > 0 ? basePrice : (fuelPrice / Data.GALLONS_TO_KG);
		if (id == 4)
		{
			price = (JetAPrice / Data.GALLONS_TO_KG);
		}
		double sizePart = Math.log(airportSize)/9.0;
		sizePart = (sizePart - 1) * sizeFactor + 1;		
		double fuelPart = 0.75+ Math.log(fuelPrice)/7.0;
		fuelPart = (fuelPart - 1) * fuelFactor + 1;
		double overstockPart = overstock / (double)maxOverstockDiscountAmount;
		if (overstockPart > 1)
			overstockPart = 1;
		else if (overstockPart < -1)
			overstockPart = -1;
		overstockPart *= overstockDiscount;
		
		return price * sizePart * fuelPart * (1-overstockPart/100.0);
	}
	
	// User transfers goods to company for money
	public double getKgBuyPrice(int amount, int airportSize, double fuelPrice, int overstock, double JetAPrice)
	{
		double price = getWeightedPrice(airportSize, fuelPrice, overstock, JetAPrice); 
		
		return price * (1-discount/100.0) * (1-overstockDiscount/100.0);
	}
	
	// User acquires goods for money
	public double getKgSalePrice(int amount, int airportSize, double fuelPrice, int overstock, double JetAPrice)
	{
		double price = getWeightedPrice(airportSize, fuelPrice, overstock, JetAPrice);
		double discount = amount/(double)maxDiscountAmount;
		if (discount > 1)
			discount = 1;
		discount *= this.discount;
		
		return price * (1-discount/100.0);		
	}
	
	/**
	 * @return
	 */
	public double getBasePrice()
	{
		return basePrice;
	}

	/**
	 * @return
	 */
	public double getFuelFactor()
	{
		return fuelFactor;
	}

	/**
	 * @return
	 */
	public int getMinAirportSize()
	{
		return minAirportSize;
	}

	/**
	 * @return
	 */
	public int getDiscount()
	{
		return discount;
	}

	/**
	 * @return
	 */
	public int maxDiscountAmount()
	{
		return maxDiscountAmount;
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
	public double getSizeFactor()
	{
		return sizeFactor;
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
	public int getMaxDiscountAmount()
	{
		return maxDiscountAmount;
	}

}
