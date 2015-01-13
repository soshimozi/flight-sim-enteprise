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

import org.apache.commons.lang3.BitField;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

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
	int surfaceTypes;
	
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
		setAllowedSurfaceTypes(rs.getInt("allowedSurfaceTypes"));

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
		rs.updateInt("allowedSurfaceTypes", surfaceTypes);
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
	
	public String getComment() {
		return comment;
	}

	public String getCommodity() {
		return commodity;
	}

	public int getDistanceDev() {
		return distanceDev;
	}

	public float getFrequency()
	{
		return frequency;
	}

	public int getId() {
		return id;
	}

	public int getMatchMaxSize() {
		return matchMaxSize;
	}

	public int getMatchMinSize() {
		return matchMinSize;
	}

	public int getTargetAmount() {
		return targetAmount;
	}

	public int getPayDev() {
		return payDev;
	}

	public int getTargetDistance() {
		return targetDistance;
	}

	public int getTargetKeepAlive() {
		return targetKeepAlive;
	}

	public float getTargetPay() {
		return targetPay;
	}

	public int getTypeOfPay() {
		return typeOfPay;
	}

	public void setComment(String string) {
		comment = string;
	}

	public void setCommodity(String string) {
		commodity = string;
	}

	public void setDistanceDev(int i) {
		distanceDev = i;
	}

	public void setFrequency(float d) {
		frequency = d;
	}

	public void setId(int i) {
		id = i;
	}

	public void setMatchMaxSize(int i) {
		matchMaxSize = i;
	}

	public void setMatchMinSize(int i) {
		matchMinSize = i;
	}

	public void setTargetAmount(int i) {
		targetAmount = i;
	}

	public void setPayDev(int i) {
		payDev = i;
	}

	public void setTargetDistance(int i) {
		targetDistance = i;
	}

	public void setTargetKeepAlive(int i) {
		targetKeepAlive = i;
	}

	public void setTargetPay(float d) {
		targetPay = d;
	}

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

	public String getIcaoSet1() {
		return icaoSet1;
	}

	public String getIcaoSet2() {
		return icaoSet2;
	}

	public void setIcaoSet1(String string) {
		icaoSet1 = string;
	}

	public void setIcaoSet2(String string) {
		icaoSet2 = string;
	}

	public int getAmountDev() {
		return amountDev;
	}

	public void setAmountDev(int i) {
		amountDev = i;
	}

	public int getUnits() {
		return units;
	}

	public String getSUnits()
	{
		return AssignmentBean.getSUnits(getUnits());
	}

	public void setUnits(int i) {
		units = i;
	}
	
	public void setUnits(String s)
	{
		setUnits(AssignmentBean.unitsId(s));
	}

	public void setAllowedSurfaceTypes(int i)
	{
		surfaceTypes = i;
	}

	public void setAllowedSurfaceTypes(List<Integer> list)
	{
		BitSet bs = BitSet.valueOf(new long[]{(long)surfaceTypes});

		for (int i : list)
			bs.set(i - 1);

		surfaceTypes = bs.isEmpty() ? 0 : (int)bs.toLongArray()[0];
	}

	public List<Integer> getAllowedSurfaceTypes()
	{
		BitSet bs = BitSet.valueOf(new long[]{(long)surfaceTypes});
		List<Integer> list = new ArrayList<>();

		for(int i = 0; i<11; i++)
			if(bs.get(i))
				list.add(i+1);

		return list;
	}

	public int getSeatsFrom() {
		return seatsFrom;
	}

	public void setSeatsFrom(int i) {
		seatsFrom = i;
	}
	
	public int getSeatsTo() {
		return seatsTo;
	}

	public void setSeatsTo(int i) {
		seatsTo = i;
	}
	
	public int getSpeedFrom() {
		return speedFrom;
	}

	public void setSpeedFrom(int i) {
		speedFrom = i;
	}
	
	public int getSpeedTo() {
		return speedTo;
	}

	public void setSpeedTo(int i) {
		speedTo = i;
	}
}
