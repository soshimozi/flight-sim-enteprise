package net.fseconomy.data;

import java.io.Serializable;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class AssignmentBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	public static final int TYPE_ALLIN = 1;
	public static final int TYPE_TRIPONLY = 2;
	
	public static final int UNIT_PASSENGERS = 1;
	public static final int UNIT_KG = 2;
	
	int id;
	Timestamp creation, expires;
	int userlock;
	String commodity;
	int amount;
	String from, to, aircraft;
	String location;
	String comment;
	double pay;
	int type;
	int distance;
	int active;
	int bearing;
	boolean group;
	int groupId;
	int pilotFee;
	int units;
	boolean createdByUser, ptAssignment;
	int commodityId;
	int owner;
	AirportBean destinationAirport, locationAirport, fromAirport;
	int actualDistance, actualBearing;	
	int fromTemplate, fromFboTemplate, mptTax;
	int daysClaimedActive;
	
	public AssignmentBean(ResultSet rs) throws SQLException
	{
		String aircraft;
		aircraft = rs.getString("aircraft");
		setCreation(rs.getTimestamp("creation"));
		setId(rs.getInt("id"));
		setExpires(rs.getTimestamp("expires"));
		setCommodity(rs.getString("commodity"));
		setAircraft(aircraft);
		setFrom(rs.getString("fromicao"));
		setTo(rs.getString("toicao"));
		setLocation(rs.getString("location"));
		setPay(rs.getFloat("pay"));
		setType(aircraft == null ? TYPE_TRIPONLY : TYPE_ALLIN);
		setAmount(rs.getInt("amount"));
		setDistance(rs.getInt("distance"));
		setActive(rs.getInt("active"));
		setBearing(rs.getInt("bearing"));
		setGroup(rs.getString("groupId") != null);
		setGroupId(rs.getInt("groupId"));
		setPilotFee(rs.getInt("pilotFee"));
		setUnits(rs.getString("units"));
		comment = rs.getString("comment");
		commodityId = rs.getInt("commodityId");
		owner = rs.getInt("owner");
		userlock = rs.getInt("userlock");
		createdByUser = (rs.getString("fromTemplate") == null) && (rs.getString("fromFboTemplate") == null);
		ptAssignment = (rs.getString("fromTemplate") == null) && (rs.getString("fromFboTemplate") != null);
		fromTemplate = rs.getInt("fromTemplate");
		fromFboTemplate = rs.getInt("fromFboTemplate");
		mptTax = rs.getInt("mpttax");
		daysClaimedActive = rs.getInt("daysClaimedActive");
	}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		rs.updateString("fromicao", getFrom());
		rs.updateString("toicao", getTo());
		rs.updateString("location", getLocation());
		rs.updateFloat("distance", getDistance());
		rs.updateFloat("bearing", getBearing());

		if (comment == null)
			rs.updateNull("comment");
		else
			rs.updateString("comment", comment);

		rs.updateTimestamp("creation", getCreation());
		rs.updateTimestamp("expires", getExpires());

		if (isGroup())
			rs.updateInt("groupId", getGroupId());
		else
			rs.updateNull("groupId");

		if (commodity == null)
			rs.updateNull("commodity");
		else
			rs.updateString("commodity", getCommodity());

		rs.updateInt("pilotFee", getPilotFee());
		rs.updateInt("amount", getAmount());
		rs.updateInt("commodityId", getCommodityId());
		rs.updateInt("owner", getOwner());
		rs.updateString("units", getSUnits());
		rs.updateDouble("pay", getPay());
	}
	
	public void updateData(Data data)
	{
		double distanceBearing[] = data.getDistanceBearing(getFrom(), getTo());

		setDistance((int)Math.round(distanceBearing[0]));
		setBearing((int)Math.round(distanceBearing[1]));
	}
	
	/**
	 * Constructor for AssignmentBean.
	 */
	public AssignmentBean()
	{
		super();
		id = -1;
	}
	/**
	 * Returns the aircraft.
	 * @return String
	 */
	public String getAircraft()
	{
		return aircraft;
	}

	/**
	 * Returns the commodity.
	 * @return String
	 */
	public String getCommodity()
	{
		return commodity;
	}

	/**
	 * Returns the from.
	 * @return String
	 */
	public String getFrom()
	{
		return from == null ? "" : from;
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
	 * Returns the passengers.
	 * @return int
	 */
	public int getAmount()
	{
		return amount;
	}

	/**
	 * Returns the to.
	 * @return String
	 */
	public String getTo()
	{
		return to == null ? "" : to;
	}

	/**
	 * Returns the userlock.
	 * @return int
	 */
	public int getUserlock()
	{
		return userlock;
	}

	/**
	 * Sets the aircraft.
	 * @param aircraft The aircraft to set
	 */
	public void setAircraft(String aircraft)
	{
		this.aircraft = aircraft;
	}

	/**
	 * Sets the commodity.
	 * @param commodity The commodity to set
	 */
	public void setCommodity(String commodity)
	{
		this.commodity = commodity;
	}


	/**
	 * Sets the from.
	 * @param from The from to set
	 */
	public void setFrom(String from)
	{
		this.from = from.toUpperCase();
	}
	
	public AirportBean getFromAirport(Data data)
	{
		if (fromAirport == null)
			setFromAirport(data.getAirport(from));

		return fromAirport;
	}

	public void setFromAirport(AirportBean bean)
	{
		fromAirport = bean;
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
	 * Sets the passengers.
	 * @param passengers The passengers to set
	 */
	public void setAmount(int passengers)
	{
		this.amount = passengers;
	}

	/**
	 * Sets the pay.
	 * @param pay The pay to set
	 */
	public void setPay(double pay)
	{
		this.pay = pay;
	}

	/**
	 * Sets the to.
	 * @param to The to to set
	 */
	public void setTo(String to)
	{
		this.to = to.toUpperCase();
	}

	/**
	 * Sets the userlock.
	 * @param userlock The userlock to set
	 */
	public void setUserlock(int userlock)
	{
		this.userlock = userlock;
	}

	/**
	 * Sets the creation.
	 * @param creation The creation to set
	 */
	public void setCreation(Timestamp creation)
	{
		this.creation = creation;
	}

	/**
	 * Sets the expires.
	 * @param expires The expires to set
	 */
	public void setExpires(Timestamp expires)
	{
		this.expires = expires;
	}

	/**
	 * Returns the type.
	 * @return int
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Sets the type.
	 * @param type The type to set
	 */
	public void setType(int type)
	{
		this.type = type;
	}

	/**
	 * Returns the creation.
	 * @return Timestamp
	 */
	public Timestamp getCreation()
	{
		return creation;
	}

	/**
	 * Returns the expires.
	 * @return Timestamp
	 */
	public Timestamp getExpires()
	{
		return expires;
	}
	
	public String getExpiresGMTDate()
	{
		Calendar cal = GregorianCalendar.getInstance();
		String result;
		
		boolean moved = !from.equals(location);
		boolean locked = (userlock > 0) || (groupId > 0);
		
		if(expires == null)
		{
			return "9999/1/1 00:00:00";
		}
		else if(fromFboTemplate == 0) //non-PAX
		{
			if(moved)
			{
				//return the expired date time + 45 * 24 hours
				long addms = 45l * 86400000l;
				cal.setTimeInMillis(expires.getTime() + addms);// + (45 * 86400000));
			}
			else if(locked)
			{
				//return the expired date time + 24 hours
				cal.setTimeInMillis(expires.getTime() + 86400000l);
			}
		}
		else
		{
			if(locked)
			{
				//return the expired date time + 2*24 hours
				cal.setTimeInMillis(expires.getTime() + 2l*86400000l);
			}
			else
			{
				//default return date time
				cal.setTimeInMillis(expires.getTime());
			}
		}
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		result = dateFormatGmt.format(cal.getTime());

		return result;
	}
	
	public String getSExpires()
	{
		if (expires == null)
			return "never";

		boolean moved = !from.equals(location);
		boolean locked = (userlock > 0) || (groupId > 0);
		String note = "";
		long expiry = expires.getTime() - GregorianCalendar.getInstance().getTime().getTime();
		if (fromFboTemplate == 0)
		{
			long extratime = 0;
			if (moved)
				extratime = 45; // days
			else if (locked) {
				extratime = 1;  // days
			}
			extratime *= 86400000; // miliseconds per day
			expiry += extratime;

			if (!moved && locked && expiry <= extratime)
				note = "*";
		} 
		else 
		{
			long extratime = 0;
			if (locked) 
			{
				extratime = 2 * 24;  //hard coded 2 days extra when locked
				
			/* ADDED PRD hours: get new user input from database here
			variable daysClaimedActive comes from fbofacilities template and is passed
			to daysClaimedActive field in assignments table
			extratime = daysClaimedActive * 24;  
			deletes are hard coded so logic needs rewriting*/
			}
			
			extratime *= 3600000; // miliseconds per hour
			expiry += extratime;

			if (locked && expiry <= extratime)
				note = "*";
		}
		
		boolean expired = expiry < 0;
		if (expiry < 0)
			expiry = -expiry;

		int minutes = (int)(expiry/(60 * 1000));
		int hours = (int)(expiry/(3600 * 1000));
		int days = hours/24;
		String Duration;

		if (days > 0)
			Duration = days + " days";	
		else if (hours > 0)
			Duration = hours + " hours";
		else
			Duration = minutes + " minutes";

		if (expired)
			return "expired (" + Duration + ")";
		else
			return Duration + note;
	}
	
	public int getActualDistance(Data data)
	{
		if (actualDistance == 0)
		{
			AirportBean loc = getLocationAirport(data);
			/*
			if (loc == null)
			{
				AircraftBean ac[] = data.getAircraftForUser(userlock);
				if (ac.length > 0)
					loc = data.getAirport(ac[0].getDepartedFrom());
			}
			*/
			if (loc != null)
			{
				double db[] = data.getDistanceBearing(loc, getDestinationAirport(data));
				actualDistance = (int)Math.round(db[0]);
				actualBearing = (int)Math.round(db[1]);
			} else {
				actualDistance = distance;
				actualBearing = bearing;
			}
		}
		return actualDistance;
	}

	/**
	 * Returns the distance.
	 * @return int
	 */
	public int getDistance()
	{
		return distance;
	}

	/**
	 * Sets the distance.
	 * @param distance The distance to set
	 */
	public void setDistance(int distance)
	{
		if (distance < 1) distance =1;
		//distance = checkDistance(distance);
		this.distance = distance;
	}
	//ADDED PRD: Limit 1,2,3 pax to 300 mile payout	
	public int checkDistance(int distance)
	{
		if(amount < 4 && distance > 300 && ptAssignment)
			distance = 300;
		return distance;
	}
	
	public int calcPay()
	{
		//distance = checkDistance(distance);
		return (int)Math.round(pay * amount * (distance / 100.0));
	}

	/**
	 * Returns the active.
	 * @return int
	 */
	public int getActive()
	{
		return active;
	}

	/**
	 * Sets the active.
	 * @param active The active to set
	 */
	public void setActive(int active)
	{
		this.active = active;
	}

	public int getActualBearing(Data data)
	{
		getActualDistance(data); // initializes
		return actualBearing;
	}
	
	public int getBearing() {
		return bearing;
	}

	public void setBearing(int i) {
		bearing = i;
	}
	public int getActualBearingImage(Data data)
	{
		return (int)Math.round(getActualBearing(data)/45.0)%8;
	}
	public int getBearingImage()
	{
		return (int)Math.round(getBearing()/45.0)%8;
	}
	
	public String getSCargo()
	{
		if (amount == 0)
		{
			String S;
			if ((comment != null) && (comment.length() > 0))
			{
				if (comment.length() > 40)
					S = comment.substring(0, 40) + "...";
				else
					S = comment;
			}
			else
				S = "[Group Assignment]";
			return S;
		}
		if (units == AssignmentBean.UNIT_PASSENGERS)
			return (ptAssignment ? "<font color=Green>" : "") + amount + " " + getCommodity() + (ptAssignment ? "</font>" : "");
		else
			return getCommodity() + " " + amount + getSUnits();
	}

	public boolean isGroup() {
		return group;
	}
	
	public boolean isPtAssignment() {
		return ptAssignment;
	}


	public int getGroupId() {
		return groupId;
	}


	public void setGroup(boolean b) {
		group = b;
	}


	public void setGroupId(int i) {
		groupId = i;
	}


	public int getPilotFee() {
		return pilotFee;
	}


	public void setPilotFee(int i) {
		pilotFee = i;
	}


	public int getUnits() {
		return units;
	}
	
	public String getSUnits()
	{
		return AssignmentBean.getSUnits(getUnits());
	}
	
	public static String getSUnits(int units)
	{
		switch (units)
		{
			case UNIT_PASSENGERS:
				return "passengers";
			case UNIT_KG:
				return "kg";
		}

		return null;
	}

	public void setUnits(int i) {
		units = i;
	}
	
	public static int unitsId(String s)
	{
		if (s.equals("passengers"))
			return AssignmentBean.UNIT_PASSENGERS;
		else
			return AssignmentBean.UNIT_KG;
	
	}

	public void setUnits(String s)
	{
		setUnits(AssignmentBean.unitsId(s));
	}

	public AirportBean getDestinationAirport(Data data)
	{
		if (destinationAirport == null)
			setDestinationAirport(data.getAirport(to));
		return destinationAirport;
	}

	public void setDestinationAirport(AirportBean bean)
	{
		destinationAirport = bean;
	}

	public boolean isCreatedByUser()
	{
		return createdByUser;
	}

	public String getLocation()
	{
		return location;
	}

	public void setLocation(String string)
	{
		location = string;
	}

	public AirportBean getLocationAirport(Data data)
	{
		if (locationAirport == null)
			setLocationAirport(data.getAirport(location));

		return locationAirport;
	}

	public void setLocationAirport(AirportBean bean)
	{
		locationAirport = bean;
	}

	public String getComment()
	{
		return comment == null ? "" : comment;
	}

	public void setComment(String string)
	{
		comment = "".equals(string) ? null : Data.clearHtml(string);
	}

	public int getCommodityId()
	{
		return commodityId;
	}

	public void setCommodityId(int i)
	{
		commodityId = i;
	}

	public int getOwner()
	{
		return owner;
	}

	public void setOwner(int i)
	{
		owner = i;
	}

	public double getPay()
	{
		return pay;
	}
	
	public int getFromTemplate()
	{
		return fromTemplate;
	}

	public int getFromFboTemplate()
	{
		return fromFboTemplate;
	}

	public int getmptTax()
	{
		return mptTax;
	}

	public int getDaysClaimedActive()
	{
		return daysClaimedActive;
	}

	public void setDaysClaimedActive(int i)
	{
		daysClaimedActive = i;
	}
	
/*
 * Edit matrix:
 * 
 * 									System assignments		Ferry assignments		Goods assignments		
 * 	Group staff that have taken			
 * 		assignment into group			comment/p.fee		from/to/comm/p.fee			comm/p.fee
 * 
 * Owner of goods 							x						x					To/Amount/Pay		
 *  
 *  
 * 
 * 
 */
 	public boolean isSystem() 	{	return !createdByUser; 	}

 	public boolean isFerry() 	{	return createdByUser && commodityId == 0;	}

 	public boolean isGoods() 	{	return createdByUser && commodityId != 0; 	}

 	public boolean groupAuthority(UserBean who)
 	{
 		return groupId != 0 && who.groupMemberLevel(groupId) >= UserBean.GROUP_STAFF;
 	}

 	public boolean authorityOverGoods(UserBean who)
 	{
 		return isGoods() && (owner == who.getId() || who.groupMemberLevel(owner) >= UserBean.GROUP_STAFF);
 	}
 	
	public boolean editFromAllowed(UserBean who)
	{
		return isFerry() && groupAuthority(who);
	}

    public boolean editToAllowed(UserBean who)
	{
		return (isFerry() && groupAuthority(who)) ||
				(isGoods() && authorityOverGoods(who));
	}

    public boolean editAmountAllowed(UserBean who)
	{
		return isGoods() && authorityOverGoods(who);
	}

    public boolean editPayAllowed(UserBean who)
	{
		return isGoods() && authorityOverGoods(who);
	}

    public boolean editPilotFeeAllowed(UserBean who)
	{
		return groupAuthority(who);
	}

    public boolean editCommentAllowed(UserBean who)
	{
		return groupAuthority(who);
	}

    public boolean deleteAllowed(UserBean who)
	{
		return (isGoods() && authorityOverGoods(who)) ||
			(isGroup() && groupAuthority(who));
	}

	public void setCreatedByUser(boolean b)
	{
		createdByUser = b;
	}
}

