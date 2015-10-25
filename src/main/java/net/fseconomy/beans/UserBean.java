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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import net.fseconomy.data.Groups;
import net.fseconomy.util.Converters;
import net.fseconomy.util.GlobalLogger;
import net.fseconomy.util.Helpers;

public class UserBean implements Serializable
{
	private static final long serialVersionUID = 2L;
	public static final int LEV_NONE = 0;
	public static final int LEV_ACTIVE = 1;
	public static final int LEV_MODERATOR = 2;
	public static final int LEV_ADMIN = 3;
	public static final int LEV_CSR = 4;
	public static final int LEV_ACA = 5;
	
	public static final int GROUP_INVITED = 1;
	public static final int GROUP_MEMBER = 2;
	public static final int GROUP_STAFF = 5;
	public static final int GROUP_OWNER = 10;
	
	public static final int EXPOSURE_JOIN = 1;
	public static final int EXPOSURE_SCORE = 2;
	public static final int EXPOSURE_GROUPS = 4;

    int MAXNAMESIZE = 45;
	int MAXCOMMENTSIZE = 255;

	int id;
    Timestamp created;
    Timestamp logon;
	String name, email;
	int level;
	double money;
	boolean loggedIn;
	double bank;
	String comment;
	Map<Integer, Groups.groupMemberData> memberships;
	boolean group;
	boolean exposedJoin, exposedScore, exposedGrouplist;
	String url;
	int defaultPilotFee;
	int loanLimit;
	TimeZone timeZone;
	String xmlKey;
	int dateformat;
	boolean showPaymentsToSelf;
	String banList;
    double earnedInterest;
	
	String readAccessKey;
	String writeAccessKey;
	
	/**
	 * Constructor for UserBean.
	 */
	public UserBean()
	{
		super();
		id = -1;
		setExposure(EXPOSURE_SCORE|EXPOSURE_GROUPS|EXPOSURE_JOIN);
	}
	
	public UserBean(ResultSet rs) throws SQLException
	{
		setId(rs.getInt("id"));
        setCreated(rs.getTimestamp("created"));
        setLogon(rs.getTimestamp("logon"));
		setName(rs.getString("name"));
		setMoney(rs.getDouble("money"));
		setEmail(rs.getString("email"));
		setLevel(rs.getString("level"));
		setBank(rs.getDouble("bank"));
		setComment(rs.getString("comment"));
		setGroup(rs.getString("type").equals("group"));
		setUrl(rs.getString("url"));
		setDefaultPilotFee(rs.getInt("defaultPilotFee"));
		setLoanLimit(rs.getInt("limit"));
		setExposure(rs.getInt("exposure"));
		setDateFormat(rs.getInt("dateformat"));
		setShowPaymentsToSelf(rs.getBoolean("showPaymentsToSelf"));
		setBanList(rs.getString("banList"));
        setEarnedInterest(rs.getDouble("interest"));

		setReadAccessKey(rs.getString("readAccessKey"));
		setWriteAccessKey(rs.getString("writeAccessKey"));
}
	
	public void writeBean(ResultSet rs) throws SQLException
	{
		//rs.updateInt("exposure", getExposure());
		rs.updateString("email", email);
		rs.updateInt("dateformat", dateformat);
		rs.updateBoolean("showPaymentsToSelf", showPaymentsToSelf);
		rs.updateString("banlist", banList);
		rs.updateString("readAccessKey", readAccessKey);
		rs.updateString("writeAccessKey", writeAccessKey);
	}

    private void setEarnedInterest(double interest)
    {
        earnedInterest = interest;
    }

    public double getEarnedInterest()
    {
        return earnedInterest;
    }

    private void setCreated(Timestamp ts)
    {
        created = ts;
    }
    private void setLogon(Timestamp ts)
    {
        logon = ts;
    }

    public Timestamp getCreated()
    {
        return created;
    }

    public Timestamp getLogon()
    {
        return logon;
    }

	public void setLevel(String level)
	{
		if (level == null)
			this.level = 0;
		else if (level.equals("active"))
			this.level = LEV_ACTIVE;
		else if (level.equals("moderator"))
			this.level = LEV_MODERATOR;
		else if (level.equals("admin"))
			this.level = LEV_ADMIN;
		else if (level.equals("csr"))
			this.level = LEV_CSR;
		else if (level.equals("aca"))
			this.level = LEV_ACA;
	}

	/**
	 * Returns the email.
	 * @return String
	 */
	public String getEmail()
	{
		return email;
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
	 * Returns the level.
	 * @return int
	 */
	public String getLevelString(int level)
	{
        String result;

        switch(level)
        {
            case LEV_NONE:
                result = "none";
                break;
            case LEV_ACTIVE:
                result = "active";
                break;
            case LEV_ACA:
                result = "aca";
                break;
            case LEV_CSR:
                result = "csr";
                break;
            case LEV_MODERATOR:
                result = "moderator";
                break;
            case LEV_ADMIN:
                result = "admin";
                break;
            default:
                result = "not found";
                break;
        }

		return result;
	}

    /**
     * Returns the level.
     * @return int
     */
    public int getLevel()
    {
        return level;
    }

    /**
	 * Returns the money.
	 * @return int
	 */
	public double getMoney()
	{
		return money;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the money.
	 * @param money The money to set
	 */
	public void setMoney(double money)
	{
		this.money = money;
	}

	/**
	 * Returns the loggedIn.
	 * @return boolean
	 */
	public boolean isLoggedIn()
	{
		return loggedIn;
	}

	/**
	 * Sets the email.
	 * @param email The email to set
	 */
	public void setEmail(String email)
	{
		if(email != null) 
			email = email.trim();
		
		this.email = email;
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
	 * Sets the level.
	 * @param level The level to set
	 */
	public void setLevel(int level)
	{
		this.level = level;
	}

	/**
	 * Sets the loggedIn.
	 * @param loggedIn The loggedIn to set
	 */
	public void setLoggedIn(boolean loggedIn)
	{
		this.loggedIn = loggedIn;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name)
	{
		if(name == null)
			GlobalLogger.logGroupAuditLog("Group name NULL", UserBean.class);

        //Trim and clean up name!
        this.name = Helpers.truncate(Converters.clearHtml(name.trim()), MAXNAMESIZE);
	}

	/**
	 * Returns the bank.
	 * @return int
	 */
	public double getBank()
	{
		return bank;
	}

	/**
	 * Sets the bank.
	 * @param bank The bank to set
	 */
	public void setBank(double bank)
	{
		this.bank = bank;
	}
	
	public int groupMemberLevel(int group)
	{
		if (memberships == null)
			return -1;

        Groups.groupMemberData data = memberships.get(group);
		
		return data == null ? -1 : data.memberLevel;
	}
	
	public Groups.groupMemberData[] getStaffGroups()
	{
		ArrayList<Groups.groupMemberData> returnValue = new ArrayList<>();
		if (memberships == null)
			return new Groups.groupMemberData[0];

        for (Groups.groupMemberData item : memberships.values())
        {
            if (item.memberLevel >= UserBean.GROUP_STAFF)
                returnValue.add(item);
        }
		
		return returnValue.toArray(new Groups.groupMemberData[returnValue.size()]);
	}
	
	public String getComment()
	{
		return comment == null ? "" : comment;
	}

	public void setComment(String string)
	{
		comment = Helpers.truncate(Converters.clearHtml(string), MAXCOMMENTSIZE);
	}

	public Map<Integer, Groups.groupMemberData> getMemberships()
	{
		return memberships;
	}

	public void setMemberships(Map<Integer, Groups.groupMemberData> map)
	{
		memberships = map;
	}
	
	public static int getGroupLevel(String role)
	{
        if(role == null)
            role = "";

        switch (role)
        {
            case "member":
                return GROUP_MEMBER;
            case "staff":
                return GROUP_STAFF;
            case "owner":
                return GROUP_OWNER;
            case "invited":
                return GROUP_INVITED;
            default:
                return -1;
        }
	}
	
	public static String getGroupLevelName(int level)
	{
		switch (level)
		{
			case GROUP_MEMBER : return "member";
			case GROUP_OWNER  : return "owner";
			case GROUP_STAFF  : return "staff";
			case GROUP_INVITED: return "invited";
		}
		
		return null;
	}

	public boolean isGroup()
	{
		return group;
	}

	public void setGroup(boolean b)
	{
		group = b;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String string)
	{
		if (string != null && string.length() == 0)
			string = null;
		if (string != null && !string.toLowerCase().startsWith("http"))
			string = "http://" + string;
		url = string;
	}

	public int getDefaultPilotFee()
	{
		return defaultPilotFee;
	}

	public void setDefaultPilotFee(int i)
	{
		if (i < 0)
			i = 0;
		if(i > 100)
			i = 100;

		defaultPilotFee = i;
	}

	public int getLoanLimit()
	{
		return loanLimit;
	}

	public void setLoanLimit(int i)
	{
		loanLimit = i;
	}
	
	public double getInterest()
	{
		double bank = getBank();
		
		bank = (bank > 1000000 ? 1000000 : bank);
		
		return isGroup() ? 0 : bank * (Math.pow(bank < 0 ? 1.1 : 1.05, 1/365.0) - 1);
	}

	public int getExposure()
	{
		int exposure = 0;
		if (exposedJoin)
			exposure |= EXPOSURE_JOIN;
		
		if (exposedGrouplist)
			exposure |= EXPOSURE_GROUPS;
		
		if (exposedScore)
			exposure |= EXPOSURE_SCORE;
		
		return exposure;
	}

	public void setExposure(int exposure)
	{
		setExposedJoin((exposure & EXPOSURE_JOIN) > 0);
		setExposedGrouplist((exposure & EXPOSURE_GROUPS) > 0);
		setExposedScore((exposure & EXPOSURE_SCORE) > 0);
	}

	public boolean isExposedGrouplist()
	{
		return exposedGrouplist;
	}

	public boolean isExposedScore()
	{
		return exposedScore;
	}

	public void setExposedGrouplist(boolean b)
	{
		exposedGrouplist = b;
	}

	public void setExposedScore(boolean b)
	{
		exposedScore = b;
	}

	public boolean isExposedJoin()
	{
		return exposedJoin;
	}

	public void setExposedJoin(boolean b)
	{
		exposedJoin = b;
	}

	public TimeZone getTimeZone()
	{
		return timeZone;
	}

	public void setTimeZone(TimeZone zone)
	{
		timeZone = zone;
	}

	public void setDateFormat(int i)
	{
		dateformat = i;
	}
	
	public int getDateFormatIndex()
	{
		return dateformat;
	}
	
	public SimpleDateFormat getDateFormatByIndex(int i)
	{
		SimpleDateFormat result;
		switch (i)
		{
			case 0: result = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"); break;
			case 1: result = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"); break;
			default: result = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"); break;
		}
	
		return result;
	}
	
	public SimpleDateFormat getDateFormat()
	{
		return getDateFormatByIndex(dateformat);
	}
	
	public int getUserTimezone()
	{
		return dateformat;
	}
	
	public boolean getShowPaymentsToSelf()
	{
		return showPaymentsToSelf;
	}
	
	public void setShowPaymentsToSelf(boolean b)
	{
		showPaymentsToSelf = b;
	}

	public String getBanList()
	{		
		return banList;
	}
	
	public void setBanList(String list)
	{
		banList = list;
	}
	
	public boolean isInBanList(String renter) 
	{
		if (banList == null) 
			return false;	//if no entries in list
		
		//uses default token delimiters, SPACE in our case
		StringTokenizer st =new StringTokenizer(banList);	
		
		//loop through each item in the list and put into a unique Collection
		Set<String> renters = new HashSet<>();
		while (st.hasMoreTokens())
			renters.add(st.nextToken().toLowerCase());
		
		//if renter account ID exists in our collection return true
        return renters.contains(renter.toLowerCase());

    }
	
	public void setReadAccessKey(String s)
	{
		readAccessKey = s;
	}
	
	public String getReadAccessKey()
	{
		return readAccessKey;
	}
	
	public void setWriteAccessKey(String s)
	{
		writeAccessKey = s;
	}
	
	public String getWriteAccessKey()
	{
		return writeAccessKey;
	}

    public boolean isLocked()
    {
        return email.contains("LockedAccount");
    }
}
