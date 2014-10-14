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

/**
 * @author Marty
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TemplateBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	public static final int TYPE_ALLIN = 1;
	public static final int TYPE_TRIPONLY = 2;
	
	int id;
	float frequency;
	int targetKeepAlive;
	String commodity;
	int targetAmount, amountDev;
	float targetPay;
	int payDev;
	int targetDistance, distanceDev;
	int typeOfPay;
	int matchMaxSize, matchMinSize;
	String comment;
	String icaoSet1, icaoSet2;
	int units;
	
	//All-In template changes
	int speedFrom, speedTo;
	int seatsFrom, seatsTo;
	
	public TemplateBean()
	{
	}
	
	public TemplateBean(ResultSet rs) throws SQLException
	{
		setId(rs.getInt("id"));
		setFrequency(rs.getFloat("frequency"));
		setTargetKeepAlive(rs.getInt("targetKeepAlive"));
		setCommodity(rs.getString("commodity"));
		setTargetAmount(rs.getInt("targetAmount"));
		setAmountDev(rs.getInt("amountDev"));
		setTargetPay(rs.getFloat("targetPay"));
		setPayDev(rs.getInt("payDev"));
		setTargetDistance(rs.getInt("targetDistance"));
		setDistanceDev(rs.getInt("distanceDev"));
		setTypeOfPay(rs.getString("typeOfPay"));
		setMatchMaxSize(rs.getInt("matchMaxSize"));
		setMatchMinSize(rs.getInt("matchMinSize"));
		setComment(rs.getString("comment"));
		setIcaoSet1(rs.getString("icaoset1"));
		setIcaoSet2(rs.getString("icaoset2"));
		setUnits(rs.getString("units"));
		
		//All-In changes
		setSeatsFrom(rs.getInt("seatsFrom"));
		setSeatsTo(rs.getInt("seatsTo"));
		setSpeedFrom(rs.getInt("speedFrom"));
		setSpeedTo(rs.getInt("speedTo"));
	}
	public void writeBean(ResultSet rs) throws SQLException
	{
		rs.updateFloat("frequency", getFrequency());
		rs.updateInt("targetKeepAlive", getTargetKeepAlive());
		rs.updateString("commodity", getCommodity());
		rs.updateInt("targetAmount", getTargetAmount());
		rs.updateInt("amountDev", getAmountDev());
		rs.updateFloat("targetPay", getTargetPay());
		rs.updateInt("payDev", getPayDev());
		rs.updateInt("targetDistance", getTargetDistance());
		rs.updateInt("distanceDev", getDistanceDev());
		rs.updateString("typeOfPay", getSTypeOfPay());
		rs.updateString("units", getSUnits());
		if (getMatchMaxSize() == 0)
			rs.updateNull("matchMaxSize");
		else
			rs.updateInt("matchMaxSize", getMatchMaxSize());
		if (getMatchMinSize() == 0)
			rs.updateNull("matchMinSize");
		else
			rs.updateInt("matchMinSize", getMatchMinSize());
		rs.updateString("comment", getComment());
		
		icaoSet1 = icaoSet1 == null ? "" : icaoSet1.trim();
		icaoSet2 = icaoSet2 == null ? "" : icaoSet2.trim();
		if (icaoSet1.equals(""))
			rs.updateNull("icaoSet1");
		else
			rs.updateString("icaoSet1", icaoSet1);
		if (icaoSet2.equals(""))
			rs.updateNull("icaoSet2");
		else
			rs.updateString("icaoSet2", icaoSet2);
		
		//All-In changes
		rs.updateInt("seatsFrom", getSeatsFrom());
		rs.updateInt("seatsTo", getSeatsTo());
		rs.updateInt("speedFrom", getSpeedFrom());
		rs.updateInt("speedTo", getSpeedTo());
	}
	
	/**
	 * @return
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @return
	 */
	public String getCommodity() {
		return commodity;
	}

	/**
	 * @return
	 */
	public int getDistanceDev() {
		return distanceDev;
	}

	/**
	 * @return
	 */
	public float getFrequency()
	{
		return frequency;
	}

	/**
	 * @return
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return
	 */
	public int getMatchMaxSize() {
		return matchMaxSize;
	}

	/**
	 * @return
	 */
	public int getMatchMinSize() {
		return matchMinSize;
	}

	/**
	 * @return
	 */
	public int getTargetAmount() {
		return targetAmount;
	}

	/**
	 * @return
	 */
	public int getPayDev() {
		return payDev;
	}

	/**
	 * @return
	 */
	public int getTargetDistance() {
		return targetDistance;
	}

	/**
	 * @return
	 */
	public int getTargetKeepAlive() {
		return targetKeepAlive;
	}

	/**
	 * @return
	 */
	public float getTargetPay() {
		return targetPay;
	}

	/**
	 * @return
	 */
	public int getTypeOfPay() {
		return typeOfPay;
	}

	/**
	 * @param string
	 */
	public void setComment(String string) {
		comment = string;
	}

	/**
	 * @param string
	 */
	public void setCommodity(String string) {
		commodity = string;
	}

	/**
	 * @param i
	 */
	public void setDistanceDev(int i) {
		distanceDev = i;
	}

	/**
	 * @param d
	 */
	public void setFrequency(float d) {
		frequency = d;
	}

	/**
	 * @param i
	 */
	public void setId(int i) {
		id = i;
	}

	/**
	 * @param i
	 */
	public void setMatchMaxSize(int i) {
		matchMaxSize = i;
	}

	/**
	 * @param i
	 */
	public void setMatchMinSize(int i) {
		matchMinSize = i;
	}

	/**
	 * @param i
	 */
	public void setTargetAmount(int i) {
		targetAmount = i;
	}

	/**
	 * @param i
	 */
	public void setPayDev(int i) {
		payDev = i;
	}

	/**
	 * @param i
	 */
	public void setTargetDistance(int i) {
		targetDistance = i;
	}

	/**
	 * @param i
	 */
	public void setTargetKeepAlive(int i) {
		targetKeepAlive = i;
	}

	/**
	 * @param d
	 */
	public void setTargetPay(float d) {
		targetPay = d;
	}

	/**
	 * @param i
	 */
	public void setTypeOfPay(int i) {
		typeOfPay = i;
	}
	
	public void setTypeOfPay(String s)
	{
		if (s.equals("allin"))
			setTypeOfPay(TYPE_ALLIN);
		else
			setTypeOfPay(TYPE_TRIPONLY);
	}
	public String getSTypeOfPay()
	{
		switch (getTypeOfPay())
		{
			case TYPE_ALLIN:
				return "allin";
			case TYPE_TRIPONLY:
				return "triponly";
		}
		return null;
	}
	/**
	 * @return
	 */
	public String getIcaoSet1() {
		return icaoSet1;
	}

	/**
	 * @return
	 */
	public String getIcaoSet2() {
		return icaoSet2;
	}

	/**
	 * @param string
	 */
	public void setIcaoSet1(String string) {
		icaoSet1 = string;
	}

	/**
	 * @param string
	 */
	public void setIcaoSet2(String string) {
		icaoSet2 = string;
	}

	/**
	 * @return
	 */
	public int getAmountDev() {
		return amountDev;
	}

	/**
	 * @param i
	 */
	public void setAmountDev(int i) {
		amountDev = i;
	}

	/**
	 * @return
	 */
	public int getUnits() {
		return units;
	}
	public String getSUnits()
	{
		return AssignmentBean.getSUnits(getUnits());
	}

	/**
	 * @param i
	 */
	public void setUnits(int i) {
		units = i;
	}
	
	public void setUnits(String s)
	{
		setUnits(AssignmentBean.unitsId(s));
	}
	
	public int getSeatsFrom() {
		return seatsFrom;
	}

	/**
	 * @param i
	 */
	public void setSeatsFrom(int i) {
		seatsFrom = i;
	}
	
	public int getSeatsTo() {
		return seatsTo;
	}

	/**
	 * @param i
	 */
	public void setSeatsTo(int i) {
		seatsTo = i;
	}
	
	public int getSpeedFrom() {
		return speedFrom;
	}

	/**
	 * @param i
	 */
	public void setSpeedFrom(int i) {
		speedFrom = i;
	}
	
	public int getSpeedTo() {
		return speedTo;
	}

	/**
	 * @param i
	 */
	public void setSpeedTo(int i) {
		speedTo = i;
	}
	
	

}
