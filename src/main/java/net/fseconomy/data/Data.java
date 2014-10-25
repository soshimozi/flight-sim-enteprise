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
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;

import javax.sql.rowset.CachedRowSet;
import javax.mail.internet.*;

import com.google.gson.Gson;

import net.fseconomy.util.Converters;
import net.fseconomy.util.Formatters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import ch.qos.logback.classic.LoggerContext;
//import ch.qos.logback.core.util.StatusPrinter;

public class Data implements Serializable
{
	private static final long serialVersionUID = 3L;

	static final String systemLocation = "http://server.fseconomy.net";
	static final String fromAddress = "no-reply@fseconomy.net";

	private String pathToWeb = "";

	public static long defaultCount = 0;
	public static long createCount = 0;
	public static long cacheCount = 0;
	public static long bytesServed = 0;
	public static long totalImagesSent = 0;

	public static final int stepSize = 20;

	public static final double GALLONS_TO_KG = 2.68735;
	
	//for Signature template selection
	public static int currMonth = 1;
	
	public double currFuelPrice = 3.0;
	public double currJetAMultiplier = 1.0;
	
	static final int MAX_MODEL_TITLE_LENGTH = 128;
	static final int MAX_CONNECTION_POOL_SIZE = 5;
	
	final int MAX_FLIGHT_ASSIGNMENTS = 60;

	final long MSEC_ONE_MINUTE = 60*1000;
	final long MSEC_FIVE_MINUTES = 5*60*1000;
	final long MSEC_TEN_MINUTES = 10*60*1000;
	final long MSEC_THIRTY_MINUTES = 30*60*1000;
	public long MILLISECS_PER_HOUR = (60*60*1000);
	
	public static final int ACCT_TYPE_ALL = 1;
	public static final int ACCT_TYPE_PERSON = 2;
	public static final int ACCT_TYPE_GROUP = 3;	

	final int ASSIGNMENT_ACTIVE = 0;
	final int ASSIGNMENT_ENROUTE = 1;
	final int ASSIGNMENT_HOLD = 2;
	
	transient static Thread maintenanceThread;
	transient Properties mailProperties = new Properties();
	
	long milesFlown;
	long minutesFlown;
	long totalIncome;
	statistics[] statistics = null;
	public static HashMap<String, statistics> statsmap = null;
	public static HashMap<String, statistics> prevstatsmap = null;

	public CommodityBean[] commodities = null;
	int maxCommodityId = 0;
	
	
	public static String DataFeedUrl = "";
	public enum SimType {FSUIPC, FSX, XP}

    public final static Logger logger = LoggerFactory.getLogger(Data.class);

	private static Data singletonInstance = null;
	public DALHelper dalHelper = null;
	
	public static Data getInstance()
	{
		return singletonInstance;
	}
	
	public Data()
	{
		// assume SLF4J is bound to logback in the current environment
	    //LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    
		// print logback's internal status
	    //StatusPrinter.print(lc);
		
		logger.info("Data constructor called");

		dalHelper = DALHelper.getInstance();
		
		Locale.setDefault(Locale.US);
		
		initializeSystemVariables();
		
		initializeAirportCache();
		initializeCommodities();
		initializeFuelValues();
		updateBuckets();
		
		singletonInstance = this;
	}
	
	public void setPathToWeb(String path)
	{
		pathToWeb = path;
	}
	
	public String getPathToWeb()
	{
		return pathToWeb;
	}
	
	private void initializeSystemVariables() 
	{
		SetDatafeedUrl();		
	}

	public String GetFSUIPCClientVersion()
	{
		String version = "";
		try
		{
			String qry = "SELECT svalue FROM sysvariables where VariableName='FSUIPCClientVersion'";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			if(rs.next())
			{
				version = rs.getString(1);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return version;
	}
	
	void initializeCommodities()
	{
		ArrayList<CommodityBean> result = new ArrayList<>();
		ResultSet rs;
		
		try
		{
			String qry = "SELECT * FROM commodities ORDER BY id";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);
			int max = 0;

			while (rs.next())
			{
				CommodityBean c = new CommodityBean(rs);
				result.add(c);
				if (c.getId() < 99 && c.getId() > max)
					max = c.getId();
			}
			
			commodities = new CommodityBean[max + 1];				
			for (int c = 0; c < max; c++)
			{
				CommodityBean b = result.get(c);
				commodities[b.getId()] = b;
			}
			
			maxCommodityId = max;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	//Moved this here, not even sure it needs to be done at all.
	void updateBuckets()
	{
		try
		{
			String qry = "SELECT * FROM airports WHERE bucket is null";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				int newbucket = AirportBean.bucket(rs.getDouble("lat"), rs.getDouble("lon"));
				qry = "UPDATE airports set bucket = ? WHERE icao = ?";
				dalHelper.ExecuteUpdate(qry, newbucket, rs.getString("icao"));
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void initializeFuelValues()
	{
		try
		{
			String qry = "Select value from sysvariables where variablename='100LLFuelPrice'";
			currFuelPrice = dalHelper.ExecuteScalar(qry, new DALHelper.DoubleResultTransformer());
			
			qry = "Select value from sysvariables where variablename='JetAMultiplier'";
			currJetAMultiplier = dalHelper.ExecuteScalar(qry, new DALHelper.DoubleResultTransformer());
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public class clientrequest
	{
		public int id;
		public Timestamp time;
		public String ip;
		public int userid;
		public String name;
		public String client;
		public String state;
		public String aircraft;
		public String params;
	}

    public List<LatLonCount> FlightSummaryList = new ArrayList<>();
    public class LatLonCount
    {
        double a;
        double b;
        int c;

        public LatLonCount(double latitude, double longitude, int cnt)
        {
            a = latitude;
            b = longitude;
            c = cnt;
        }
    }

    class LatLonSize
	{
		double lat;
		double lon;
		int size;
		int type;
		
		public LatLonSize(double latitude, double longitude, int sz, int t)
		{
			lat = latitude;
			lon = longitude;
			size = sz;
			type = t;
		}
	}	

    public class PilotStatus
    {
    	public String a; // Name;
    	public String b; // MakeModel;
    	public String c; // Icao;
    	public float  d; // Lat;
    	public float e; // Lon;
    	public String f; // Status;
    }

	/**
	 * Used to hold an ICAO and lat/lon instance in Hashtable
	 * This is initialized in the Data() constructor on startup 
	 */
	public static Hashtable<String,LatLonSize> cachedAPs = new Hashtable<>();
	
	/**
	 * Timing data for daily and routine cycles
	 *
	 */
	public static class closeAirport implements Comparable<closeAirport>
	{
		public String icao;
		public double distance;
		public double bearing;
		public closeAirport(String icao, double distance, double bearing)
		{
			this.icao = icao;
			this.distance = distance;
			this.bearing = bearing;
		}
		public closeAirport(String icao, double distance)
		{
			this.icao = icao;
			this.distance = distance;
			this.bearing = Double.NaN;
		}

		public int compareTo(closeAirport ca)
		{
			return ca.distance == distance ? 0 : ca.distance < distance ? 1 : -1;
		}
	}
	
	public static class groupMemberData implements Serializable
	{
		private static final long serialVersionUID = 1L;
		public int groupId;
		public int memberLevel;
		public String groupName;
		
		public groupMemberData(int groupId, int memberLevel, String name)
		{
			this.groupId = groupId;
			this.memberLevel = memberLevel;
			this.groupName = name;
		}	
	}
	
	public static class AircraftAlias
	{
		public String fsName;
		public String model;
		
		public AircraftAlias(String fsName, String model)
		{
			this.fsName = fsName;
			this.model = model;
		}
	}
	
	//TODO This should be removed and just use ModelBean as there is little to no real difference
	//Modelbean needs to be modified to use fcap instead of array.
	public static class aircraftConfigs
	{
		public String makemodel;
		public int seats,crew,fueltype,cruisespeed,fcapExt1,fcapLeftTip,fcapLeftAux,fcapLeftMain,fcapCenter,fcapCenter2,fcapCenter3,fcapRightMain,fcapRightAux,fcapRightTip,fcapExt2,gph,maxWeight,emptyWeight,price,engines,enginePrice,fcaptotal;
		public boolean canShip;
		public aircraftConfigs(String makemodel, int crew, int fueltype, int seats, int cruisespeed, int fcapExt1, int fcapLeftTip, int fcapLeftAux, int fcapLeftMain, int fcapCenter, int fcapCenter2, int fcapCenter3, int fcapRightMain, int fcapRightAux, int fcapRightTip, int fcapExt2, int gph, int maxWeight, int emptyWeight, int price, int engines, int enginePrice, boolean canShip, int fcaptotal)
		{
			this.makemodel = makemodel;
			this.seats = seats;
			this.crew = crew;
			this.fueltype = fueltype;
			this.cruisespeed = cruisespeed;
			this.fcapExt1 = fcapExt1;
			this.fcapLeftTip = fcapLeftTip;
			this.fcapLeftAux = fcapLeftAux;
			this.fcapLeftMain = fcapLeftMain;
			this.fcapCenter = fcapCenter;
			this.fcapCenter2 = fcapCenter2;
			this.fcapCenter3 = fcapCenter3;
			this.fcapRightMain = fcapRightMain;
			this.fcapRightAux = fcapRightAux;
			this.fcapRightTip = fcapRightTip;
			this.fcapExt2 = fcapExt2;
			this.gph = gph;
			this.maxWeight = maxWeight;
			this.emptyWeight = emptyWeight;
			this.price = price;
			this.engines = engines;
			this.enginePrice = enginePrice;
			this.canShip = canShip;
			this.fcaptotal = fcaptotal;
		}
	}

	public static class pendingHours
	{
		public float phours;
		public String phourtime;
		public String pminutetime;
		
		public pendingHours(float phours, String phourtime, String pminutetime)
		{
			this.phours=phours;
			this.phourtime=phourtime;
			this.pminutetime=pminutetime;
		}
	}

	public static class trendHours
	{
		public String logdate;
		public String duration;
		public float last48Hours;
		
		public trendHours(String logdate, int duration, int last48Hours)
		{
			this.logdate = logdate;
			this.duration = "" + (float)Math.round(duration/3600.0 *10)/10;
			this.last48Hours = (float)Math.round(last48Hours/3600.0*10)/10;
		}
	}
	
	public static class statistics implements Comparable<statistics>
	{
		public int accountId;
		public String accountName;
		public String owner;
		public int money;
		public int flights, totalMiles, totalFlightTime;
		public Timestamp firstFlight;
		public Set<AircraftBean> aircraft;
		public boolean group;

		public statistics(int accountId, String accountName, String owner, int money, int flights, int totalMiles, int totalFlightTime, Timestamp firstFlight, Set<AircraftBean> aircraft, boolean group)
		{
			this.accountId = accountId;
			this.accountName = accountName;
			this.owner = owner;
			this.money = money;
			this.flights = flights;
			this.totalMiles = totalMiles;
			this.totalFlightTime = totalFlightTime;
			this.firstFlight = firstFlight;
			this.aircraft = aircraft;
			this.group = group;
		}
		
		public int compareTo(statistics s)
		{
			return accountName.compareToIgnoreCase(s.accountName);
		}
	}
	
	public void initializeAirportCache()
	{
		if( cachedAPs.size() == 0)
		{
			//pull the airports
			try
			{
				String qry = "SELECT icao, lat, lon, size, type FROM airports";
				ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
				while(rs.next())
				{
					int itype;
					
					String icao = rs.getString("icao");
					double lat = rs.getDouble("lat");
					double lon = rs.getDouble("lon");
					int size = rs.getInt("size");
					String type = rs.getString("type");
					
					if(type.contains("military"))
					{
						itype = AirportBean.TYPE_MILITARY;
					}
					else if(type.contains("water"))
					{
						itype = AirportBean.TYPE_WATER;
					}
					else
					{
						itype = AirportBean.TYPE_CIVIL;					
					}
					
					LatLonSize lls = new LatLonSize(lat, lon, size, itype);
					cachedAPs.put(icao, lls);
				}
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			} 
		}
	}
	
	static String createPassword()
	{
		return createAccessKey(8);
	}
	
	public static String createAccessKey()
	{
		return createAccessKey(10);
	}
	
	public static String createAccessKey(int len)
	{
		StringBuilder result = new StringBuilder();

		for (int loop = 0; loop < len; loop++)
		{
			int ran=(int)Math.round(Math.random()*35);
			if (ran < 10)
				result.append(ran);
			else
				result.append((char)('A'+ran-10));
		}	
		
		return result.toString();
	}
	
//	public String maintenanceStatus()
//	{		
//		return maintenanceObject == null ? "No maintenance object." : maintenanceObject.status();
//	}

	public boolean userExists(String user, String email)
	{
		boolean result = false;
		try
		{
			String qry = "select (count(name) > 0) as found from accounts where name = ? and email=?";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user, email);
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		return result;
	}

	public UserBean userExists(String user, String password, boolean updateLoginTime)
	{
		UserBean result = null;
		try
		{
			String qry = "select * from accounts where name = ? and  password=password(?)";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user, password);

			if (rs.next())
			{
				result = new UserBean(rs);
				UpdateLogonTime(result.getId());
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result;
	}
	
	private void UpdateLogonTime(int id) throws SQLException
	{
		String qry = "UPDATE accounts set logon=? WHERE id = ?";
		dalHelper.ExecuteUpdate(qry, new Timestamp(GregorianCalendar.getInstance().getTime().getTime()), id);
	}
	
	public synchronized void updateGroup(UserBean group, UserBean user) throws DataError
	{
		mustBeLoggedIn(user);
		
		if (group.getName().trim().length() < 4)
			throw new DataError("Group name must be at least 4 characters.");
		
		if (!(group.getDefaultPilotFee() >= 0 && group.getDefaultPilotFee() <= 100))
			throw new DataError("Pilot Fee must be in the range of 0 to 100%");
		
		try
		{
			String qry = "SELECT (count(id) > 0) as found FROM accounts WHERE type = 'group' AND id = ?";
			boolean exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), group.getId());
			if(!exists)
			{	
				System.err.println("id=" + group.getId());
				throw new DataError("Group not found!");
			}
			
			qry = "UPDATE accounts SET name=?, comment=?, url=?, defaultPilotFee=?, banList=?, exposure=?, readAccessKey=? WHERE id=?";
			dalHelper.ExecuteUpdate(qry, 
										group.getName(), 
										group.getComment(), 
										group.getUrl(), 
										group.getDefaultPilotFee(), 
										group.getBanList(), 
										group.getExposure(),
										group.getReadAccessKey(),
										group.getId());			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public synchronized void CreateGroup(UserBean group, UserBean user) throws DataError
	{
		mustBeLoggedIn(user);
		
		if (group.getName().trim().length() < 4)
			throw new DataError("Group name must be at least 4 characters.");
		
		if (group.getDefaultPilotFee() > 100)
			throw new DataError("Pilot Fee cannot exceed 100%");
		
		try
		{
			String qry = "select (count(*) > 0) from accounts where upper(name) = upper(?)";
			boolean exists = dalHelper.ExecuteScalar(qry,new DALHelper.BooleanResultTransformer(), group.getName());
			if (exists) 
				throw new DataError("Group name already exists.");
			
			qry = "INSERT INTO accounts (created, type, name, comment, url, defaultPilotFee, banList, exposure, readAccessKey) VALUES(?,?,?,?,?,?,?,?,?)";
			dalHelper.ExecuteUpdate(qry,
										new Timestamp(System.currentTimeMillis()),
										"group",
										group.getName(), 
										group.getComment(), 
										group.getUrl(), 
										group.getDefaultPilotFee(), 
										group.getBanList(), 
										group.getExposure(),
										group.getReadAccessKey());	
			
			qry = "select id from accounts where upper(name) = upper(?)";
			int id = dalHelper.ExecuteScalar(qry,new DALHelper.IntegerResultTransformer(), group.getName());
			joinGroup(user, id, "owner");
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void updateUser(UserBean user) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		mustBeLoggedIn(user);
		try
		{
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM accounts WHERE type = 'person' AND id = '" + user.getId() + "'");
			if (!rs.next())
				throw new DataError("User not found.");

			user.writeBean(rs);
			rs.updateRow();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);			
		}
	}
	
	public void updateUserOrGroup(UserBean user) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		mustBeLoggedIn(user);
		try
		{
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM accounts WHERE (type = 'person' or type = 'group') AND id = '" + user.getId() + "'");
			if (!rs.next())
				throw new DataError("User not found.");
			
			user.writeBean(rs);
			rs.updateRow();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);			
		}
	}

	public void changePassword(UserBean user, String password, String newPassword) throws DataError
	{
		try
		{
			newPassword = Converters.escapeSQL(newPassword);
			password = Converters.escapeSQL(password);

			String qry = "UPDATE accounts SET password=password(?) WHERE id = ? AND password = password(?)";
			int count = dalHelper.ExecuteUpdate(qry, newPassword, user.getId(), password);
			
			if (count == 0)
				throw new DataError("Invalid password specified.");	
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	public void lockAccount(String login) throws DataError
	{
		try
		{
			String qry = "UPDATE accounts SET email = concat('LockedAccount-', email), password = '*B2C37C48A693188842BC5F24929A4C99209652A5' where name = ?";
			int count = dalHelper.ExecuteUpdate(qry, login);

			if (count == 0)
				throw new DataError("Account Lock Operation Failed.");	
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void unlockAccount(String login) throws DataError
	{
		try
		{
			String qry = "UPDATE accounts SET email = trim(leading 'LockedAccount-' from email) where name = ?";
			int count = dalHelper.ExecuteUpdate(qry, login);

			if (count == 0)
				throw new DataError("Account Unlock Operation Failed.");	
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	/**
	* Update account table - user name and email
	* @ param user = user name newuser = new user name email = email address
	* @ return none
	* @ author - chuck229
	*/ 	
	public void updateAccount(String currUserName, String editedUserName, String email, int exposure, String newpassword) throws DataError
	{
		String qry;
		int count;
		try
		{
			if (currUserName.equals(editedUserName)) //not a name change
			{
				if (newpassword.length() != 0) 
				{
					newpassword = Converters.escapeSQL(newpassword);
					qry = "UPDATE accounts SET email = ?, exposure = ?, password = password(?) where name = ?";
					count = dalHelper.ExecuteUpdate(qry, email, exposure, newpassword, currUserName);
				}
				else 
				{
					qry = "UPDATE accounts SET email = ?, exposure = ? where name = ?";
					count = dalHelper.ExecuteUpdate(qry, email, exposure, currUserName);
				}
			}	
			else //name has changed
			{
				qry = "UPDATE accounts SET name = ?, email = ?, exposure = ? where name = ?";
				count = dalHelper.ExecuteUpdate(qry, editedUserName, email, exposure, currUserName);

				qry = "UPDATE log SET user = ? WHERE user = ?";
				dalHelper.ExecuteUpdate(qry, editedUserName, currUserName);
			}
			
			if (count == 0)
				throw new DataError("Account Update Failed.");	
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public boolean accountNameIsUnique(String accountName)
	{
		boolean exists = false;
		try
		{
			String qry = "SELECT (count(name) > 0) as found FROM accounts WHERE upper(name) = upper(?)";
			exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), accountName);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return !exists;
	}
	
	public boolean accountEmailIsUnique(String accountEmail)
	{
		boolean exists = false;
		try
		{
			String qry = "SELECT (count(email) > 0) as found FROM accounts WHERE upper(email) = upper(?)";
			exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), accountEmail);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return !exists;
	}
	
	public void createUser(String user, String email) throws DataError
	{
		String password = Data.createPassword();		
		
		try
		{
			if (email.indexOf('@') < 0)
				throw new AddressException();

			if (user.length() < 3 || user.indexOf(' ') >= 0)
				throw new DataError("Invalid user name.");
			
			if(userExists(user, email))
				throw new DataError("User already exists!");
			
			if(!accountNameIsUnique(user))
				throw new DataError("User name already exists!");
			
			String qry = "INSERT INTO accounts (name, password, email, exposure) VALUES(?, password(?), ?, ?)";
			dalHelper.ExecuteUpdate(qry, user, password, email, UserBean.EXPOSURE_SCORE);
			
			List<String> toList = new ArrayList<>();
			toList.add(email);
			
			String messageText = "Welcome to FSEconomy.\nYour account has been created. ";
		
			messageText += "You can login at " + systemLocation +
							" with the following account:\n\nUser: " + 
							user + "\nPassword: " + password;
			
			sendAccountEmailMessage(toList, messageText);
		} 
		catch (AddressException e)
		{
			throw new DataError("Invalid email address.");
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
    }
	
	public void resetPassword(String user, String email) throws DataError
	{
		String password = Data.createPassword();		
		
		try
		{
			if (email.indexOf('@') < 0)
				throw new AddressException();

			if (user.length() < 3 || user.indexOf(' ') >= 0)
				throw new DataError("Invalid user name.");
			
			if(!userExists(user, email))
				throw new DataError("User not found!");
			
			String qry = "UPDATE accounts SET password = password(?) WHERE name = ? AND email = ?";
			dalHelper.ExecuteUpdate(qry, password, user, email);
			
			List<String> toList = new ArrayList<>();
			toList.add(email);
			
			String messageText = "A new password has been generated for you. ";

			messageText += "You can login at " + systemLocation +
							" with the following account:\n\nUser: " + 
							user + "\nPassword: " + password;
			

			sendAccountEmailMessage(toList, messageText);
		} 
		catch (AddressException e)
		{
			throw new DataError("Invalid email address.");
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public void sendAccountEmailMessage(List<String> emailTo, String message) throws DataError
	{
		Emailer emailer = Emailer.getInstance();
		
		emailer.sendEmail("no-reply@fseconomy.net", "FSEconomy Account Management System",
						"FSEconomy account details", message, emailTo, Emailer.ADDRESS_TO);
	}
	
	public String[] getUsers(String usertype) throws DataError
	{
		ArrayList<String> result = new ArrayList<>();
		String qry;
		
		try
		{
            if(usertype == null)
                usertype = "";

            switch (usertype)
            {
                case "flying":
                {
                    qry = "SELECT accounts.name FROM aircraft LEFT JOIN accounts on aircraft.userlock = accounts.id WHERE aircraft.location is null ORDER BY accounts.name";
                    ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
                    while (rs.next())
                        result.add(rs.getString(1));
                    break;
                }
                case "parked":
                {
                    qry = "SELECT accounts.name FROM aircraft LEFT JOIN accounts on aircraft.userlock = accounts.id WHERE aircraft.location is not null and aircraft.userlock is not null ORDER BY accounts.name";
                    ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
                    while (rs.next())
                        result.add(rs.getString(1));
                    break;
                }
                default:
                    throw new DataError(usertype + " not implemented!");
            }
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new String[result.size()]);
	}

	public int getNumberOfUsers(String usertype) throws DataError
	{
		int result=0;
		try
		{
            if(usertype == null)
                usertype = "";

			String qry;
            switch (usertype)
            {
                case "onsite":
                    qry = "SELECT count(*) AS number FROM accounts WHERE accounts.logon >= date_sub(curdate(), interval 24 hour)";
                    break;
                case "flying":
                    qry = "SELECT count(*) AS number FROM aircraft LEFT JOIN accounts on aircraft.userlock = accounts.id WHERE aircraft.location is null";
                    break;
                case "parked":
                    qry = "SELECT count(*) AS number FROM aircraft LEFT JOIN accounts on aircraft.userlock = accounts.id WHERE aircraft.location is not null AND aircraft.userlock is not null";
                    break;
                default:
                    throw new DataError(usertype + " not known.");
            }
			result = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer());				
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public double getNumberOfHours(String user, int hours) throws DataError
	{
		double result=0;
		try
		{
			String qry = "SELECT SUM((FlightEngineTime)/3600) AS TimeLogged FROM `log` where user= ? and DATE_SUB(CURRENT_TIMESTAMP ,INTERVAL ? hour) <= `time`";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), user, hours);
				
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return result;
	}

	//get pending hours will list when hours are coming available - need to format 48: to hours
	public pendingHours[] getpendingHours(String user, int hours) throws DataError 
	{
		ArrayList<pendingHours> result = new ArrayList<>();
		try
		{
			String qry = "SELECT FlightEngineTime, hour(timediff('48:00:00',timediff(now(),time))),  minute(timediff('48:00:00',timediff(now(),time))) FROM `log` where user= ? and DATE_SUB(CURRENT_TIMESTAMP ,INTERVAL ? hour) <= `time` and type <> 'refuel'";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user, hours);
			while (rs.next())
			{
				pendingHours pending = new pendingHours(rs.getInt(1)/3600.0f,rs.getString(2), rs.getString(3));
				result.add(pending);
			}				
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		return result.toArray(new pendingHours[result.size()]);
	}
	
	public double getDistance(double lat1, double lon1, double lat2, double lon2)
	{
		LatLonSize lls1 = new LatLonSize(lat1, lon1, 0, 0);
		LatLonSize lls2 = new LatLonSize(lat2, lon2, 0, 0);
		
		double[] result = getDistanceBearing(lls1, lls2, true, false);
		
		return result[0];
	}
	
	public double[] getDistanceBearing(AirportBean from, AirportBean to)
	{
		return getDistanceBearing(from.icao, to.icao);
	}
	
	public double getDistance(String from, String to)
	{			
		LatLonSize lls1 = cachedAPs.get(from.toUpperCase()); //Added .toUpperCase to make sure comparison will work
		LatLonSize lls2 = cachedAPs.get(to.toUpperCase());
		
		if( lls1 == null || lls2 == null)
		{
//			if(lls1 == null)
//				System.err.println("-->distanceBearing Error, bad From ICAO: " + from);
//			if(lls2 == null)
//				System.err.println("-->distanceBearing Error, bad To ICAO: " + to);
			
			return 0;
		}
		
		double[] result = getDistanceBearing(lls1, lls2, true, false);
		
		return result[0]; //distance only
	}
	
	public double[] getDistanceBearing(String from, String to)
	{			
		LatLonSize lls1 = cachedAPs.get(from.toUpperCase()); //Added .toUpperCase to make sure comparison will work
		LatLonSize lls2 = cachedAPs.get(to.toUpperCase());
		
		if( lls1 == null || lls2 == null)
		{
//			if(lls1 == null)
//				System.err.println("-->distanceBearing Error, bad From ICAO: " + from);
//			if(lls2 == null)
//				System.err.println("-->distanceBearing Error, bad To ICAO: " + to);
			
			return null;
		}

        return getDistanceBearing(lls1, lls2);
	}
	
	public static double[] getDistanceBearing(LatLonSize from, LatLonSize to)
	{
		return getDistanceBearing(from, to, true, true);
	}
	
	/**
	 * This returns the computed distance for the passed in from/to latlons
	 * @param from
	 * @param to
	 * @param returnDistance - return distance if true, or 0 if false
	 * @param returnBearing - return beaing if true, or 0 if false
	 * @return double[] - 0 = distance, 1 = bearing
	 **/
	public static double[] getDistanceBearing(LatLonSize from, LatLonSize to, boolean returnDistance, boolean returnBearing)
	{
		if(	(!returnDistance && !returnBearing)|| 
			(from.lat == to.lat && from.lon == to.lon))
			return new double[] { 0,0 };
		
		double lat1 = Math.toRadians(from.lat);
		double lon1 = Math.toRadians(from.lon);
		double lat2 = Math.toRadians(to.lat);
		double lon2 = Math.toRadians(to.lon);
		
		double sinLat1 = Math.sin(lat1);
		double sinLat2 = Math.sin(lat2);
		double cosLat1 = Math.cos(lat1);
		double cosLat2 = Math.cos(lat2);
		
		double distanceRadians = Math.acos(sinLat1 * sinLat2 + cosLat1 * cosLat2 * Math.cos(lon2-lon1));
		
		double distance = 0;		
		if(returnDistance)
			distance = 3443.9 * distanceRadians;

		double bearing = 0;
		if(returnBearing)
		{
			bearing = Math.acos((sinLat2 - sinLat1 * Math.cos(distanceRadians))/(cosLat1 * Math.sin(distanceRadians)));		
			bearing = Math.toDegrees(bearing);
		
			if (Math.sin(lon2 - lon1) < 0.0)
				bearing = 360 - bearing;
		}
		
		return new double[] {distance, bearing};
	}
	
	/**
	 * This returns a hashtable of airports found with the passed in parameters
	 * @param icao
	 * @param clipLat - window in degrees latitude to search
	 * @param clipLat - window in degrees longitude to search adjusted for latitude
	 * @param minSize - minimum airport size to search for
	 * @param maxSize - maximum airport size to search for
	 * @param aptypes - airport types to include in the search
	 * Airboss 5/30/11
	 **/
	public Hashtable<String, LatLonSize> getAirportsInRange(String icao, double clipLat, double clipLon, int minSize, int maxSize, boolean aptypes[])
	{
		//Get the lat/lon to pass in
		LatLonSize lls = cachedAPs.get(icao.toUpperCase());

		if( lls == null)
		{
			//System.err.println("-->getAirportsInRange Error, bad ICAO: " + icao);
			return null;
		}

		Hashtable<String, LatLonSize> results = getAirportsInRange(lls.lat, lls.lon, clipLat, clipLon, minSize, maxSize, aptypes);
		
		//removed center airport
		if(results.size() > 0)
			results.remove(icao);
		
		return results;
	}
	
	/**
	 * This returns a hashtable of airports found with the passed in parameters
     * @param lat
     * @param lon
	 * @param clipLat - window in degrees latitude to search
	 * @param clipLon - window in degrees longitude to search adjusted for latitude
	 * @param minSize - minimum airport size to search for
	 * @param maxSize - maximum airport size to search for
	 * @param aptypes - airport types to include in the search
	 **/
	public Hashtable<String, LatLonSize> getAirportsInRange(double  lat, double lon, double clipLat, double clipLon, int minSize, int maxSize, boolean aptypes[])
	{
		Hashtable<String, LatLonSize> results = new Hashtable<>();
		String key;
		LatLonSize value;
		int minSz;
		int maxSz;
		boolean military;
		boolean civil;
		boolean water;
		
		civil = aptypes[0];
		water = aptypes[1];
		military = aptypes[2];
		
		minSz = minSize;
		maxSz = maxSize == 0 ? Integer.MAX_VALUE : maxSize;		

		Enumeration<String> keys = cachedAPs.keys();
		while( keys.hasMoreElements() ) 
		{
			//get our current loop key and value
			key = keys.nextElement();
			value = cachedAPs.get(key);
			
			double clat = Math.abs(value.lat-lat);
			double clon = Math.abs(value.lon-lon);
			
			//compare against current size and radius
			if(	value.size >= minSz && value.size <= maxSz &&
				clat <= clipLat && clon <= clipLon &&
				((civil && value.type == 1) || 
				 (water && value.type == 2) || 
				 (military && value.type == 3))
			  )
			{
				results.put(key,value);
			}				
		}	
		
		return results;
	}
	
	/**
	 * This returns closest airport found with the passed in parameters
	 * @param lat - window in degrees latitude to search
	 * @param lon - window in degrees longitude to search adjusted for latitude
	 * @param minSize - minimum airport size to search for
	 **/
	public closeAirport closestAirport(double lat, double lon, int minSize)
	{
		return closestAirport(lat, lon, minSize, true);
	}
	
	public closeAirport closestAirport(double lat, double lon, int minSize, boolean waterOk)
	{
		String bestIcao = null;
		double bestDistance = 0;
		
		String key;
		LatLonSize value;

		boolean found = false;
		double degrees = 0.2;
		boolean[] aptypes = {true, waterOk, true};
		
		do
		{
			//convert the distance to degrees (60nm at the equator = 1 degree)
			double degreeClipLat = degrees;
			
			//adjust for compression toward poles
			double degreeClipLon = Math.abs(degreeClipLat/Math.cos(Math.toRadians(lat)));
			
			//get the airports in range
			Hashtable<String, LatLonSize> results = getAirportsInRange(lat, lon, degreeClipLat, degreeClipLon, minSize, 0, aptypes);
			
			//loop through the results to see if any are closest
			Enumeration<String> keys = results.keys();
			while( keys.hasMoreElements() ) 
			{
				//get our current loop key and value
				key = keys.nextElement();
				value = results.get(key);
				
				//get the distance
				double distance = getDistance(lat, lon, value.lat, value.lon);
				
				//check if we found a new match that is better then the previous
				if (bestIcao == null || distance < bestDistance)
				{
					found = true;
					bestIcao = key;
					bestDistance = distance;
				}
			}
			//if we haven't found one, increase the radius 
			if(!found)
			{
				degrees *= 2;
			}
		}while(!found);
	
		if (bestIcao == null)
			return null;
		
		return new closeAirport(bestIcao, bestDistance);		
	}
	
	/**
	 * This returns a randomly selected airport found with the passed in parameters
	 * @param id - airport to center on
	 * @param minDistance - minimum distance to search for
	 * @param maxDistance - maximum distance to search for
	 * @param minsize - minimum airport size to search for
	 * @param maxsize - maximum airport size to search for
	 * @param lat - used to compute correct value for longitudinal degree values for distance
	 * @param icaoSet - preselected icaos to search through
	 * @param waterOk - ok, to include water airports
	 **/
	public closeAirport getRandomCloseAirport(String id, double minDistance, double maxDistance, int minsize, int maxsize, double lat, Set<String> icaoSet, boolean waterOk)
	{
		closeAirport returnValue = null;

		//if the template has defined ICAOs, search them first for one meeting min/max distance criteria
		if (icaoSet != null && !icaoSet.isEmpty())
		{
			String airports[] = icaoSet.toArray(new String[icaoSet.size()]);
			
			//new code that filters the list of airports down to the ones that meet
			//the min/max distance criteria
			Set<String> inrange = new HashSet<>();
            for (String airport : airports)
            {
                double[] distanceBearing = getDistanceBearing(id, airport);
                if (distanceBearing != null &&
                        distanceBearing[0] != 0 &&
                        (distanceBearing[0] >= minDistance && distanceBearing[0] <= maxDistance))
                {
                    inrange.add(airport);
                }
            }
			
			//if 1 or more airports met the criteria, randomly select one and return
			if(inrange.size() > 0)
			{
				String aps[] = inrange.toArray(new String[inrange.size()]);
				int index = (int)(aps.length * Math.random());
				double[] distanceBearing = getDistanceBearing(id, aps[index]);
				
				return new closeAirport(aps[index], distanceBearing[0], distanceBearing[1]);
			}				
		}

		//If no ICAO was found in the passed in ICAOset then
		//query the DB to see if any airport meets the min/max distance criteria
		
		//convert the distance to degrees (60nm at the equator = 1 degree)
		double degreeClipLat = Math.abs(maxDistance / 60.0);
		//adjust for compression toward poles
		double degreeClipLon = Math.abs(degreeClipLat/Math.cos(Math.toRadians(lat)));
		
		//Prepare for what types of aiports we are looking for
		// 0 = civil, 1 = water, 2 = military
		boolean[] aptypes = {false, false, false};		
		
		aptypes[0] = true;
		if(waterOk)
			aptypes[1] = true;
		
		//get the airports that met the criteria passed
		Hashtable<String, LatLonSize> results = getAirportsInRange(id, degreeClipLat, degreeClipLon, minsize, maxsize, aptypes);
		
		//Don't bother if none found
		if(results != null && results.size() != 0)
		{
			LatLonSize icao = cachedAPs.get(id);
			String key;
			LatLonSize value;
			double[] distbearing;
			
			//Iterate through the returned set
			Enumeration<String> keys = results.keys();
			while( keys.hasMoreElements() ) 
			{
				//get our current loop key and value
				key = keys.nextElement();
				value = results.get(key);

				distbearing = getDistanceBearing(icao, value);
				
				//filter out the ones that don't meet the minimum distance, or is the center airport
				if(id.contains(key) || distbearing[0] < minDistance || distbearing[0] > maxDistance)
					results.remove(key);
			}
			
			//Anything left?
			if(results.size() != 0)
			{
				keys = results.keys();
				key = "";
				
				//if there is only one, just return it
				if(results.size() == 1)
				{
					key = keys.nextElement();
					value = results.get(key);
					
					distbearing = getDistanceBearing(icao, value);
					returnValue= new closeAirport(key, distbearing[0], distbearing[1]);
				}
				else
				{
					//if there is more then one, then randomize the selection out of those available
					int index = (int)(results.size() * Math.random());
					for(int i = 0; i <= index; i++)
						key = keys.nextElement();

					value = results.get(key);
					distbearing = getDistanceBearing(icao, value);
					returnValue= new closeAirport(key, distbearing[0], distbearing[1]);
				}
			}			
		}
		
		return returnValue;		
	}
	
	public closeAirport[] closeAirportsWithAssignments(String id, boolean outbound)
	{
		return closeAirportsWithAssignments(id, 0, 50, outbound);
	}
	
	public closeAirport[] closeAirportsWithAssignments(String id, double minDistance, double maxDistance, boolean outbound)
	{
		ArrayList<closeAirport> result = new ArrayList<>();
		try
		{
			String join;
			if (outbound)
				join = "JOIN assignments j ON j.location = b.icao AND j.userlock IS NULL and j.groupId IS NULL";
			else
				join = "JOIN assignments j ON j.toIcao = b.icao AND j.userlock IS NULL AND j.groupId IS NULL";
			
			String qry = "SELECT DISTINCT a.icao, b.icao FROM airports a, airports b " + join + " WHERE a.icao <> b.icao AND ABS(a.lat-b.lat) < 2 AND ABS(a.lon-b.lon)<2 AND a.icao = ? order by (ABS(a.lat-b.lat)+ABS(a.lon-b.lon)) limit 50";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
			while (rs.next())
			{
				LatLonSize lls1 = cachedAPs.get(rs.getString(1));
				LatLonSize lls2 = cachedAPs.get(rs.getString(2));
				double[] distanceBearing = getDistanceBearing(lls1, lls2);

				double distance = distanceBearing[0];
				double bearing = distanceBearing[1];
				
				if (distance >= minDistance && distance < maxDistance)
					result.add(new closeAirport(rs.getString(2), distance, bearing));
			}

			Collections.sort(result);
			if(result.size() > 10)
				result = new ArrayList<>(result.subList(0,10));
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result.toArray(new closeAirport[result.size()]);
	}
	
	/**
	 * This returns an array of closeAirport found with the passed in parameters
	 * @param id - airport to center on
	 * @param minDistance - minimum distance to search for
	 * @param maxDistance - maximum distance to search for
	 * Airboss 5/30/11
	 **/
	public closeAirport[] fillCloseAirports(String id, double minDistance, double maxDistance)
	{
		if(id == null)
			return null;
		
		ArrayList<closeAirport> result = new ArrayList<>();

		String key;
		LatLonSize value;
		LatLonSize icao;
		
		icao = cachedAPs.get(id);

		if(icao == null)
			return null;
		
		//Prepare for what types of aiports we are looking for
		// 0 = civil, 1 = water, 2 = military
		boolean[] aptypes = {true, true, true};		
		
		//convert the distance to degrees (60nm at the equator = 1 degree)
		double degreeClipLat = Math.abs(maxDistance / 60.0);
		
		//adjust for compression toward poles
		double degreeClipLon = Math.abs(degreeClipLat/Math.cos(Math.toRadians(icao.lat)));

		Hashtable<String, LatLonSize> results = getAirportsInRange(id, degreeClipLat, degreeClipLon, 0, 0, aptypes);
		
		//loop through the results to see if any are closest
		Enumeration<String> keys = results.keys();
		while( keys.hasMoreElements() ) 
		{
			//get our current loop key and value
			key = keys.nextElement();
			value = results.get(key);
			
			//get the distance / bearing
			double[] distance = getDistanceBearing(icao, value);
			
			boolean skipself = id.contains(key);
			
			//check if minimum distance met, we already know it meets the max distance (2 degrees or 120nm)
			if (!skipself && distance[0] >= minDistance)
			{
				result.add(new closeAirport(key, distance[0], distance[1]));
			}
		}

		//limit to return the closest 15 only
		Collections.sort(result);
		
		if(result.size() > 12)
			result = new ArrayList<>(result.subList(0,12));
			
		return result.toArray(new closeAirport[result.size()]);
	}
	
	public AirportBean[] getAirportsForFboConstruction(int owner)
	{
		return getAirportSQL(
				"SELECT a.* FROM airports a, goods g" +
				" WHERE a.icao = g.location" +
				" AND g.amount >= " + GoodsBean.CONSTRUCT_FBO +
				" AND g.type = " + GoodsBean.GOODS_BUILDING_MATERIALS +
				" AND g.owner = " + owner +
				" AND NOT EXISTS (SELECT * FROM fbo WHERE fbo.location = a.icao AND fbo.owner = g.owner)" +
				" AND (select" +
				"       case" +
				"        when airports.size < " + AirportBean.MIN_SIZE_MED + " then 1" +
				"        when airports.size < " + AirportBean.MIN_SIZE_BIG + " then 2" +
				"        else 3" +
				"       end - case when ISNULL(fbo.location) then 0 else sum(fbosize) end" +
				"       as fboslotsremain" +
				"      from airports" +
				"      left outer join fbo on fbo.location = airports.icao" +
				"      where airports.icao = a.icao" +
				"      group by airports.icao) > 0"
		);
	}
	
	public AirportBean[] getAirportsByBucket(String icao)
	{
		return getAirportSQL("SELECT * FROM airports WHERE bucket = (SELECT bucket from airports WHERE icao = '" + icao + "') order by name");
	}
	
	public AirportBean getAirport(String icao)
	{
		AirportBean[] result = getAirportSQL("SELECT * FROM airports WHERE icao='" + Converters.escapeSQL(icao) + "'");
		return result.length == 0 ? null : result[0];
	}
	
	public AirportBean[] getAirportSQL(String sql)
	{
		ArrayList<AirportBean> result = new ArrayList<>();
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(sql);
			while (rs.next())
				result.add(new AirportBean(rs));
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result.toArray(new AirportBean[result.size()]);
	}
	
	public int getAirportFboSlotsAvailable(String icao)
	{
		int result = 0;
		try
		{
			String qry = "select" +
				   "       case" +
				   "        when airports.size < " + AirportBean.MIN_SIZE_MED + " then 1" +
				   "        when airports.size < " + AirportBean.MIN_SIZE_BIG + " then 2" +
				   "        else 3" +
				   "       end - case when ISNULL(fbo.location) then 0 else sum(fbosize) end" +
				   "       as SlotsAvailable" +
				   "      from airports" +
				   "      left outer join fbo on fbo.location = airports.icao" +
				   "      where airports.icao = ? " +
				   "      group by airports.icao";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), icao);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	public int getAirportFboSlotsInUse(String icao)
	{
		int result = 0;
		try
		{
			String qry = "select sum(fbosize) as SlotsUsed from fbo where location = ?";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), icao);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	//New class to hold a single data point for flight operations at an ICAO
	public class FlightOp
	{
		int year;
		int month;
		String icao;
		int ops;
		public FlightOp(int opyear, int opmonth, String opicao, int opcount)
		{
			year = opyear;
			month = opmonth;
			icao = opicao;
			ops = opcount;
		}
	}
	
	private void FillZeroOps(ArrayList<FlightOp> ops, String icao, int startmon, int startyear, int endmon, int endyear)
	{
		ArrayList<FlightOp> results = new ArrayList<>();
		
		int curryear = startyear;
		int currmonth = startmon;
		
		//fill in the first year
		if(endyear > curryear)
		{
			for(int i=startmon; i <= 12 ; i++)
				results.add(new FlightOp(endyear, i, icao, 0));
			
			curryear++;
			currmonth = 1;
		}
		//see if we still have a year to go
		if(endyear > curryear)
		{
			for(int i=1; i > 12 ; i++)
				results.add(new FlightOp(curryear, i, icao, 0));
			
			curryear++;
		}
		for(int i=currmonth; i <= endmon ; i++)
			results.add(new FlightOp(curryear, i, icao, 0));
		
		for(int i = results.size()-1; i >= 0; i--)
			ops.add(results.get(i));
	}
	
	/**
	 * Gets an the total FlightOps for the selected ICAO, Month, and Year
	 * @param groupId - Group of interest
	 * @param month - Month of interest
	 * @param year - Year of interest
	 * Airboss 7/1/11
	 **/
	public int getGroupOperationsByMonthYear(int groupId, int month, int year)
	{
		int result = 0;	

		try
		{
			//First we need to get all the FBO locations for the groupId passed in.
			FboBean fbos[] = getFboByOwner(groupId);
			
			StringBuilder sb = new StringBuilder(4096);
			if(fbos.length > 0)
			{
				for(int i=0;i<fbos.length;i++)
				{
					if(i > 0)
						sb.append(", ");
						
					sb.append("'").append(fbos[i].location).append("'");
				}
			
				//get our records
				String qry = "SELECT * FROM flightops WHERE opmonth=? AND opyear=? AND icao in (" + sb.toString() + ")";
				ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, month, year);				
				while(rs.next())
				{
					//get our data
					result += rs.getInt("ops");
				}
			}
			else
			{
				result = -1;
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result;
	}
	
	public String getAirportOperationDataJSON(String icao)
	{
		FlightOp[] ops = new FlightOp[0];
		if(icao != null && cachedAPs.get(icao.toUpperCase()) != null)
			ops = getAirportOperationData(icao);
		
		Gson gson = new Gson();
		return gson.toJson(ops);
	}
	
	/**
	 * Gets an array of FlightOps for the approximately the last 24 months
	 * @param icao - ICAO of interest
	 **/
	public FlightOp[] getAirportOperationData(String icao)
	{
		ArrayList<FlightOp> results = new ArrayList<>();
		
		//exit if icao does not exist
		if(!cachedAPs.containsKey(icao.toUpperCase()))
			return results.toArray(new FlightOp[results.size()]);
		
		//get the current year and month
		Calendar cal = Calendar.getInstance();
		int curryear = cal.get(Calendar.YEAR);
		int currmonth = cal.get(Calendar.MONTH) + 1; //0 based instead of 1

		//setup our loop variables
		int loopyear = curryear;
		int loopmonth = currmonth;
		
		try
		{
			//get our records, using opyear in (?,?,?) allows index to be used
			String qry = "SELECT * FROM flightops WHERE opyear in (?,?,?) AND icao= ? ORDER BY opyear DESC, opmonth DESC";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, curryear-2, curryear-1, curryear, icao);
			
			int opyear;
			int opmonth;
			int ops;
			
			//loop though results, remember this is a spare array and might be missing 1 or more
			while(rs.next())
			{
				//get our data
				opyear = rs.getInt("opyear");
				opmonth = rs.getInt("opmonth");
				ops = rs.getInt("ops");
				
				//if everything matches just add it
				if( opyear == loopyear && opmonth==loopmonth)
				{
					results.add(new FlightOp(opyear, opmonth, icao, ops));
				}
				else //oops no match
				{
					//Fill in months with no ops
					FillZeroOps(results, icao, opmonth+1, opyear, loopmonth, loopyear );
					
					//ok, we are ready to add in our current record now after backfilling the data
					results.add(new FlightOp(opyear, opmonth, icao, ops));
					
					loopmonth = opmonth;
					loopyear = opyear;
				}
				
				//if we have 24 (or more) we are done
				if(results.size() >= 25)
					break;
				
				//decrement to our next anticipated month
				loopmonth--;
				
				//if the month is 0, then we need to loop back around to 12
				if(loopmonth <= 0)
				{
					loopmonth = 12;
					//Since we moved to december, we also need to decrement the year
					loopyear--;
					
					//If we are past the two year mark something bad has happend so exit the loop
					if(loopyear < curryear-2)
						break;
				}
			}
			if(results.size() < 25)
			{
				int amtleft = 24 - results.size();
				if(loopmonth >= amtleft)
				{
					FillZeroOps(results, icao, loopmonth-amtleft, loopyear, loopmonth, loopyear );					
				}
				else
				{
					int amt = amtleft - loopmonth;
					FillZeroOps(results, icao, 12-amt, loopyear-1, loopmonth, loopyear );					
				}
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return results.toArray(new FlightOp[results.size()]);
	}
	
	/**
	 * returns an int of the average number of ops for the last 12 months
	 * @param icao - ICAO of interest
	 **/
	public int[] getAirportOperationsPerMonth(String icao)
	{
		int[] result = new int[2];
		int average = 0;

		//get the data
		FlightOp[] ao = getAirportOperationData(icao);
		
		//if no data found, return 0
		if(ao.length == 0)
		{
			result[0] = 0;
			result[1] = 0;
			return result;
		}
		//only add up the last 12 months, skip current month
		for(int i=1; i<=12; i++) //correction Airboss 8/1/2011
			average += ao[i].ops;
		
		//return the current, and the average
		result[0] = ao[0].ops;
		result[1] = average / 12;
		return result;
	}
	
	public void fillAirport(AirportBean input)
	{
		try
		{
			String qry = "SELECT * FROM airports WHERE icao = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, input.icao);
			if (rs.next())
			{
				input.fill(rs);
				input.setLandingFee(0);			
				input.setFuelPrice(getFuelPrice(input.getIcao()));
				input.setJetaPrice(getJetaMultiplier());
			} 
			else 
				input.available = false;
			
			input.closestAirports = fillCloseAirports(input.icao, 0, 50);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public AirportBean[] findAirports(boolean assignments, int modelId, String name, int distance, String from, boolean ferry, boolean buy, int commodity, int minAmount, boolean fuel, boolean jeta, boolean repair, boolean acForSale, boolean fbo, boolean isRentable) throws DataError
	{
		ArrayList<AirportBean> result = new ArrayList<>();
		String qry;
		try
		{
			StringBuilder tables = new StringBuilder("airports");
			StringBuilder query = new StringBuilder();
			String and = " WHERE ";
			
			if (assignments)
			{
				tables.append(" LEFT JOIN assignments ON airports.icao = assignments.location ");
				query.append(and);
				query.append(" assignments.groupid is null and assignments.userlock is null ");
				and = "AND ";
			}
			
			if (modelId >= 0 || ferry || acForSale)
			{				
				//Code correction to prevent Sql error when fuel and ferry selected in aiport search parameters - Airboss 6/6/11
				tables.append(" LEFT JOIN aircraft on aircraft.location = airports.icao ");				
				
				query.append(and);
				query.append(" aircraft.userlock is null ");
				
				if (modelId >= 0) 
				{
					query.append("AND aircraft.model = ").append(modelId).append(" ");
					//gurka - added suport for rentable flag
					if (isRentable)
						query.append("AND (aircraft.rentalDry > 0 OR aircraft.rentalWet > 0) ");
				}
					
				if (ferry)
					query.append("AND (aircraft.advertise & " + AircraftBean.ADV_FERRY + ") > 0 ");
					
				if (acForSale)
					query.append("AND NOT (aircraft.sellPrice is null) ");
					
				and = "AND ";
			}
			
			if (fuel || repair || fbo || jeta)
			{
				tables.append(" LEFT JOIN fbo on fbo.location = airports.icao ");				
				if (fuel)
				{
					query.append(and);
					query.append(" ((airports.services & " + AirportBean.SERVICES_AVGAS + ") > 0 or (fbo.owner IN (SELECT owner FROM goods WHERE type='3' AND location= airports.icao AND amount > 130) AND fbo.active='1'))");
					and = "AND ";
				}
				if (jeta)
				{
					query.append(and);
					query.append(" ((airports.services & " + AirportBean.SERVICES_AVGAS + ") > 0 or (fbo.owner IN (SELECT owner FROM goods WHERE type='4' AND location= airports.icao AND amount > 130) AND fbo.active='1'))");
					and = "AND ";
				}
				if (repair)
				{
					query.append(and);
					query.append(" (airports.size > "  + AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE +  " OR (fbo.active > 0 AND (fbo.services & " + FboBean.FBO_REPAIRSHOP + ") > 0))");
					and = "AND ";
				}
				if (fbo)
				{
					query.append(and);
					query.append(" (fbo.active > 0)");
					and = "AND ";
				}								
			}
			
			if (commodity > 0)
			{
				if(minAmount < 0)
					minAmount = 0;
				
				tables.append(" LEFT JOIN goods ON goods.type = ").append(commodity).append(" AND goods.location = airports.icao AND ((goods.amount - cast(goods.retain as signed int) > ").append(minAmount).append(") AND (goods.saleFlag &").append(buy ? GoodsBean.SALEFLAG_BUY : GoodsBean.SALEFLAG_SELL).append(") > 0)");
					
				query.append(and);
				query.append(" (goods.type = ").append(commodity).append(" OR size >= ").append(commodities[commodity].getMinAirportSize()).append(")");
				and = " AND ";
			}
			
			if (name != null)
			{
				query.append(and);
				query.append("(airports.name LIKE '%").append(Converters.escapeSQL(name)).append("%' OR city LIKE '%").append(Converters.escapeSQL(name)).append("%')");
				and = "AND ";
			}
			
			double lat = 0, lon = 0;
			if (from != null)
			{
				qry = "SELECT lat, lon FROM airports WHERE icao = ?";
				ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, from);
				if (!rs.next())
					throw new DataError("Airport " + from + " not found.");
					
				lat = rs.getDouble(1);
				lon = rs.getDouble(2);
				double degreeClipLat = distance / 60.0;
				double degreeClipLon = Math.abs(degreeClipLat/Math.cos(Math.toRadians(lat)));

				query.append(and);
				query.append("(abs(lat - ").append(lat).append(") < ").append(degreeClipLat).append(" AND abs(lon - ").append(lon).append(") < ").append(degreeClipLon).append(")");
			}

			query.append(" ORDER BY icao LIMIT 100");
			
			qry = "SELECT DISTINCT airports.* FROM " + tables.toString() + query.toString();
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				AirportBean airport = new AirportBean(rs);
				if (from != null && getDistance(airport.getLat(), airport.getLon(), lat, lon) > distance)
					continue;
					
				result.add(airport);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result.toArray(new AirportBean[result.size()]);
	}
	
	public String getSearchRegionSQL(String region)
	{
		if (region != null)
		{
			if (region.equals("Caribbean"))
				return " airports.country IN ('Antigua and Barbuda', 'Aruba', 'Bahamas, The', 'Cayman Islands', 'Cuba', 'Dominica', 'Dominican Republic', " +
			       "'Grenada', 'Guadeloupe', 'Haiti', 'Jamaica', 'Martinique', 'Puerto Rico', 'St. Kitts and Nevis', 'St. Lucia', " +
			       "'St. Vincent and the Grenadines', 'Trinidad and Tobago', 'Turks and Caicos Islands', 'Virgin Islands', 'Virgin Islands, British') ";
			
			if (region.equals("Central America"))
				return " airports.country IN ('Belize', 'Costa Rica', 'El Salvador', 'Guatemala', 'Honduras', 'Mexico', 'Nicaragua', 'Panama') ";
			
			if (region.equals("South America"))
				return " airports.country IN ('Argentina', 'Bolivia', 'Brazil', 'Chile', 'Colombia', 'Ecuador', 'French Guiana', 'Guyana', " +
				   "'Paraguay', 'Peru', 'Suriname', 'Uruguay', 'Venezuela', 'Falkland Islands (Islas Malvinas)') ";
			
			if (region.equals("Pacific Islands"))
				return " (airports.country IN ('American Samoa', 'Cook Islands', 'Fiji Islands', 'New Caledonia', 'Niue', 'Samoa', 'Solomon Islands', " +
				   "'French Polynesia', 'Tonga', 'Tuvalu', 'Vanuatu', 'Wallis and Futuna') OR " +
				   " airports.state IN ('Hawaii', 'Johnston Atoll', 'Midway Islands', 'Wake Island')) ";
		}
		return null;
	}
	
	public String[] getSearchRegions()
	{
		ArrayList<String> result = new ArrayList<>();
		
		result.add("Caribbean");
		result.add("Central America");
		result.add("Pacific Islands");
		result.add("South America");
		
		return result.toArray(new String[result.size()]);
	}
	
	public AirportBean[] findCertainAirports(String region, String state, String country, String icao, String name,
			boolean fuel, boolean repair, boolean fbo, boolean fboInactive, boolean facilityPTRent, boolean facilityCTRent, int fboOwner) throws DataError
	{
		ArrayList<AirportBean> result = new ArrayList<>();
		try
		{
			StringBuilder tables = new StringBuilder("airports");
			StringBuilder query = new StringBuilder();
			String and = " WHERE ";
			
			if (fuel || repair || fbo || fboInactive || facilityPTRent || facilityCTRent || (fboOwner > 0))
			{
				tables.append(" LEFT JOIN fbo ON fbo.location = airports.icao ");
				if (fboOwner > 0)
				{
					query.append(and);
					query.append(" (fbo.owner = ").append(fboOwner).append(")");
					and = "AND ";
				}
				if (fuel)
				{
					query.append(and);
					query.append(" ((airports.services & " + AirportBean.SERVICES_AVGAS + ") > 0 or (fbo.owner IN (SELECT owner FROM goods WHERE type='3' AND location= airports.icao AND amount > 130) AND fbo.active='1'))");
					and = "AND ";
				}
				if (repair)
				{
					query.append(and);
					query.append(" (airports.size > "  + AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE +  " OR (fbo.active > 0 AND (fbo.services & " + FboBean.FBO_REPAIRSHOP + ") > 0))");
					and = "AND ";
				}
				if (fbo)
				{
					query.append(and);
					query.append(" (fbo.active > 0)");
					and = "AND ";
				}
				if (fboInactive)
				{
					tables.append(" LEFT JOIN accounts ON accounts.id = fbo.owner ");	
					query.append(and);
					query.append(" (fbo.active = 0 AND accounts.id >0)");
					and = "AND ";
				}
				if (facilityPTRent)
				{
					tables.append(" LEFT JOIN fbofacilities pt ON pt.fboId = fbo.id and pt.reservedSpace >= 0 and units = 'passengers' ");
					tables.append(" LEFT JOIN (select fboId, sum(size) as spaceInUse from fbofacilities where reservedSpace < 0 and units = 'passengers' group by fboId) pts ON pts.fboId = fbo.id ");
					query.append(and);
					query.append(" (fbo.fboSize * (case when airports.size < 1000 then 1 when airports.size < 3500 then 2 else 3 end) - pt.reservedSpace - (IF (pts.spaceInUse IS NULL,0,pts.spaceInUse)) > 0) ");
					and = "AND ";
				}
				if (facilityCTRent)
				{
					//??
				}
			}
			
			String regionSQL = getSearchRegionSQL(region);
			if (regionSQL != null)
			{
				query.append(and);
				query.append(regionSQL);
				and = "AND ";
			}
			
			if (state != null)
			{
				query.append(and);
				query.append("airports.state = '").append(Converters.escapeSQL(state)).append("' ");
				and = "AND ";
			}
			
			if (country != null)
			{
				query.append(and);
				query.append("airports.country = '").append(Converters.escapeSQL(country)).append("' ");
				and = "AND ";
			}
			
			if (icao != null)
			{
				query.append(and);
				query.append("airports.icao = '").append(Converters.escapeSQL(icao)).append("' ");
				and = "AND ";
			}
			
			if (name != null)
			{
				query.append(and);
				query.append("airports.name LIKE '%").append(Converters.escapeSQL(name)).append("%' ");
            }

			query.append(" ORDER BY icao");
			String qry = "SELECT DISTINCT airports.* FROM " + tables.toString() + query.toString();
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				AirportBean airport = new AirportBean(rs);

				result.add(airport);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result.toArray(new AirportBean[result.size()]);
	}

	public String[] getDistinctData(String field, String table)throws DataError
	{
		ArrayList<String> result = new ArrayList<>();
		String qry = "";
		try
		{
			qry = "SELECT DISTINCT "+ field +" FROM "+ table +" ORDER BY " + field;
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				String nfield = rs.getString(field);
				result.add(nfield);								
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
			System.err.println(qry);
		} 

		if (result.size() == 0)
			throw new DataError("Nothing to Return!");
		
		return result.toArray(new String[result.size()]);			
	}
	
	public void doTransferFbo(FboBean fbo, int buyer, int owner, String icao, boolean goods) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sUpdate;
		try
		{
			conn = dalHelper.getConnection();
			stmt =	conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			
			int mergeWithId = 0;
			int mergeWithServices = 0;
			rs = stmt.executeQuery("select f.id, f.fbosize, f.services from fbo f where f.location = '" + icao + "' and f.owner = " + buyer);
			if (rs.next())
			{
				mergeWithId = rs.getInt(1);
				mergeWithServices = rs.getInt(3);
				if (mergeWithId == fbo.getId())
					throw new DataError("Buyer already owns this FBO.");
			}
			rs.close();

			if (goods)
			{
				rs = stmt.executeQuery("SELECT type, amount FROM goods where owner = " + owner + " and location = '" + icao + "' and amount <> 0");
				while (rs.next())
				{
					int type = rs.getInt("type");
					int amount = rs.getInt("amount");
					changeGoodsRecord(icao, type, owner, -amount, true);
					changeGoodsRecord(icao, type, buyer, amount, true);
				}
				rs.close();
			}
			else // Keeping goods remove for sell/buy flags
			{
				String qry = "UPDATE goods SET saleFlag=0 WHERE owner=? AND location=?";
				dalHelper.ExecuteUpdate(qry, owner, icao);
			}
			
			if (mergeWithId == 0)
			{
				// Buyer does not have an existing FBO. Just transfer ownership.
				sUpdate = "UPDATE fbo SET fbo.owner = " + buyer + ", fbo.saleprice = 0 WHERE fbo.owner = " + owner + " AND fbo.location ='" + icao + "'";
				stmt.executeUpdate(sUpdate);
				sUpdate = "UPDATE fbofacilities set occupant = " + buyer + " WHERE occupant = " + owner + " and fboId = " + fbo.getId();
				stmt.executeUpdate(sUpdate);
			} 
			else 
			{
				// Buyer has an FBO. Merge the FBOs.				
				mergeWithServices = mergeWithServices | fbo.getServices();
				
				// Increase size of existing FBO by the size of the purchased FBO.
				sUpdate = "UPDATE fbo set fbosize = fbosize + " + fbo.getFboSize() + ", services = " + mergeWithServices + " where id = " + mergeWithId;
				stmt.executeUpdate(sUpdate);
				
				// If the existing FBO has a facility, delete the facility of the purchased FBO.
				rs = stmt.executeQuery("select id from fbofacilities where fboId = " + mergeWithId);
				if (rs.next())
					stmt.executeUpdate("delete from fbofacilities where reservedSpace >= 0 and fboId = " + fbo.getId());
				
				rs.close();
				
				// For the purchased FBOs facilities, where the occupant is the seller, change to the buyer.
				stmt.executeUpdate("update fbofacilities set occupant = " + buyer + " where occupant = " + owner + " and fboId = " + fbo.getId());
				// Link the purchased FBOs facilities with the existing FBO.
				stmt.executeUpdate("update fbofacilities set fboId = " + mergeWithId + " where fboId = " + fbo.getId());
				
				// Delete the purchased FBO and update logs.
				stmt.executeUpdate("delete from fbo where id = " + fbo.getId());
				stmt.executeUpdate("update log set fbo = " + mergeWithId + " where fbo = " + fbo.getId());
				stmt.executeUpdate("update payments set fbo = " + mergeWithId + " where fbo = " + fbo.getId());
			}
			
			doPayment(buyer, owner, 0, PaymentBean.FBO_SALE, 0, fbo.getId(), icao, "", "FBO Transfer", false);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public void buyFbo(int fboId, int account, UserBean user) throws DataError
	{
		if (user.getId() != account && user.groupMemberLevel(account) < UserBean.GROUP_STAFF)
			throw new DataError("Permission denied");
		
		try
		{
			FboBean fbo = getFbo(fboId);
			if ((fbo != null) && fbo.isForSale()) 
			{
				int sellPrice = fbo.getPrice();
				int oldOwner = fbo.getOwner();
				String icao = fbo.getLocation();
				boolean includesGoods = fbo.getPriceIncludesGoods();
				
				String qry = "SELECT (count(id) > 0) AS found FROM accounts WHERE id = ?";
				boolean exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), account);
				if (!exists)
					throw new DataError("Account not found");
				
				qry = "SELECT (money >= ?) as enough FROM accounts WHERE id = ?";
				boolean enough = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), sellPrice, account);
				if (!enough)
					throw new DataError("Not enough money to buy FBO");
				
				doTransferFbo(fbo, account, oldOwner, icao, includesGoods);
				resetAllGoodsSellBuyFlag(oldOwner, icao);
				
				doPayment(account, oldOwner, sellPrice, PaymentBean.FBO_SALE, 0, fboId, icao, "", "", false);				
			} 
			else
			{
				throw new DataError("FBO not found");
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void resetAllGoodsSellBuyFlag(int owner, String icao) throws SQLException
	{
		String qry = "UPDATE goods SET saleFlag=0 WHERE owner=? AND location=?";
		dalHelper.ExecuteUpdate(qry, owner, icao);	
	}
	
	//Added PRD
	public void transferFbo(FboBean fbo, UserBean user, int buyer, int owner, String icao, boolean goods) throws DataError
	{
		if (!fbo.updateAllowed(user) && (!Data.needLevel(user, UserBean.LEV_MODERATOR)))
			throw new DataError("Permission denied.");
		
		doTransferFbo(fbo, buyer, owner, icao, goods);
	}

	public void transferac(String reg, int buyer, int owner, String location) throws DataError
	{
		try
		{
			String qry = "UPDATE aircraft SET owner = ? WHERE owner = ? AND registration = ?";
			dalHelper.ExecuteUpdate(qry, buyer, owner, reg);
			
			doPayment(buyer, owner, 0, PaymentBean.AIRCRAFT_SALE, 0, -1, location, reg, "AC Transfer", false);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	public void leaseac(String reg, int lessee, int owner, String location) throws DataError
	{
		try
		{
			String qry = "UPDATE aircraft SET sellprice=null, owner = ?, lessor = ? WHERE owner = ? AND registration = ?";
			dalHelper.ExecuteUpdate(qry, lessee, owner, owner, reg);
			
			doPayment(lessee, owner, 0, PaymentBean.AIRCRAFT_LEASE, 0, -1, location, reg, "Aircraft Lease", false);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	public void leasereturnac(String reg, int lessee, int owner, String location) throws DataError
	{
		try
		{
			String qry = "UPDATE aircraft SET owner = ?, lessor = null WHERE lessor = ? AND registration = ?";
			dalHelper.ExecuteUpdate(qry, owner, owner, reg);
			
			doPayment(owner, lessee, 0, PaymentBean.AIRCRAFT_LEASE, 0, -1, location, reg, "Aircraft Lease Return", false);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public String addModel(String aircraft, int[] fuelCapacities)
	{
		String result = null;
		
		try
		{
			String qry = "SELECT * FROM fsmappings WHERE fsaircraft = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, aircraft);
			if (!rs.next())
			{
				String title = aircraft.length() > MAX_MODEL_TITLE_LENGTH ? aircraft.substring(0, MAX_MODEL_TITLE_LENGTH-1): aircraft;

				qry = "INSERT INTO fsmappings (fsaircraft, fcapCenter, fcapLeftMain, fcapLeftAux, fcapLeftTip, fcapRightMain, fcapRightAux, fcapRightTip, fcapCenter2, fcapCenter3, fcapExt1, fcapExt2) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
				dalHelper.ExecuteUpdate(qry, 
										title,
										fuelCapacities[ModelBean.fuelTank.Center],
										fuelCapacities[ModelBean.fuelTank.LeftMain],
										fuelCapacities[ModelBean.fuelTank.LeftAux],
										fuelCapacities[ModelBean.fuelTank.LeftTip],
										fuelCapacities[ModelBean.fuelTank.RightMain],
										fuelCapacities[ModelBean.fuelTank.RightAux],
										fuelCapacities[ModelBean.fuelTank.RightTip],
										fuelCapacities[ModelBean.fuelTank.Center2],
										fuelCapacities[ModelBean.fuelTank.Center3],
										fuelCapacities[ModelBean.fuelTank.Ext1],
										fuelCapacities[ModelBean.fuelTank.Ext2]);
			} 
			else
			{
				int model = rs.getInt("model");
				if (model > 0)
				{
					qry = "SELECT CONCAT_WS(' ', make, model) FROM models WHERE id = ?";
                    result = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), model);
					
					ModelBean mb = getModelById(model)[0];
					int[] mcap = mb.getCapacity();
					boolean mismatch = false;
					for(int i = 0; i < ModelBean.fuelTank.NumTanks; i++)
					{
						if(fuelCapacities[i] != mcap[i])
							mismatch = true;
					}

					StringBuilder sb = new StringBuilder();
					sb.append(result);
					
					if(mismatch)
					{
						sb.append("|");
						sb.append("|");
						sb.append("Fuel Tank Mismatch!|");
						sb.append("Center - FSE "); sb.append(mcap[0]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.Center]); sb.append("|");
						sb.append("LeftMain - FSE "); sb.append(mcap[1]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.LeftMain]); sb.append("|");
						sb.append("LeftAux - FSE "); sb.append(mcap[2]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.LeftAux]); sb.append("|");
						sb.append("LeftTip - FSE "); sb.append(mcap[3]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.LeftTip]); sb.append("|");
						sb.append("RightMain - FSE "); sb.append(mcap[4]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.RightMain]); sb.append("|");
						sb.append("RightAux - FSE "); sb.append(mcap[5]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.RightAux]); sb.append("|");
						sb.append("RightTip - FSE "); sb.append(mcap[6]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.RightTip]); sb.append("|");
						sb.append("Center2 - FSE "); sb.append(mcap[7]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.Center2]); sb.append("|");
						sb.append("Center3 - FSE "); sb.append(mcap[8]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.Center3]); sb.append("|");
						sb.append("External1 - FSE "); sb.append(mcap[9]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.Ext1]); sb.append("|");
						sb.append("External2 - FSE "); sb.append(mcap[10]); sb.append(", Aircraft "); sb.append(fuelCapacities[ModelBean.fuelTank.Ext2]); sb.append("|");
					}
					result = sb.toString(); 
				}
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result;
	}
	
	public AircraftBean[] getAircraftForSale()
	{
		return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND sellPrice is not null ORDER BY models.make, models.model, sellPrice");
	}	
	
	public AircraftBean[] getAircraftByRegistration(String reg)
	{
		return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND registration='" + Converters.escapeSQL(reg) + "'");
	}	
	
	public Boolean isAircraftRegistrationUnique(String reg)
	{
		Boolean exists = true; //default is not to allow the registration on error
		
		try
		{
			String qry = "SELECT (Count(registration) = 0) as notfound FROM aircraft where registration = ?";
			exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), reg);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}

		return exists;
	}	
	
	/**
	 * Gets the aircraft by registration and then fills in the shipping config values for passed in aircraft
	 * @param reg
	 * Airboss 12/21/10
	 **/
	public AircraftBean[] getAircraftShippingInfoByRegistration(String reg)
	{
		AircraftBean[] aircraft = getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND registration='" + Converters.escapeSQL(reg) + "'");

		//Currently not used
		//if no shipping size available then return without trying to set.
		//if( aircraft[0].getShippingSize() < 1)
		//	return aircraft;
		
		try
		{
			String qry = "SELECT * FROM shippingConfigsAircraft WHERE minSize <= ? AND maxSize >= ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, aircraft[0].getEmptyWeight(), aircraft[0].getEmptyWeight());
			rs.next();
			aircraft[0].setShippingConfigAircraft(
					rs.getInt("shippingStateDelay"), 
					rs.getDouble("costPerKg"), 
					rs.getInt("costPerCrate"), 
					rs.getInt("costDisposal"));
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return aircraft;
	}	

	/**
	 * Gets the aircraft that are in a shipped state
	 * @return AircraftBean[]
	 **/
	public AircraftBean[] getShippedAircraft()
	{
        return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND ShippingState != 0");
	}	
	
	public double getAccountFundsById(int id) throws DataError
	{
		double money = 0;
		try
		{
			//check if funds available
			String qry = "SELECT money FROM accounts WHERE id = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
			
			if (!rs.next())
				throw new DataError("Account not found");
			
			money = rs.getDouble(1);
			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return money;
	}
	
	/**
	 * This updates aircraft into initial shipping state of disassembly, making payments, etc...
	 * @param user - player that initiated the shipping
	 * @param aircraft - aircraft being shipped
	 * @param shipto - ICAO aircraft is being shipped to
	 * @param departSvc - FBOID for departure service fees
	 * @param destSvc - FBOID for destination service fees
	 **/
	public void processAircraftShipment(UserBean user, AircraftBean aircraft, String shipto, int departSvc, int destSvc) throws DataError
	{
		int departMargin;
		int destMargin;
		
		//make sure that its the owner or grop staff member
		if (user.getId() != aircraft.getOwner() && user.groupMemberLevel(aircraft.getOwner()) < UserBean.GROUP_STAFF)
			throw new DataError("Permission denied");

		//calculate departure and destination shipping costs
		FboBean fromfbo = getFbo(departSvc);
		FboBean tofbo = getFbo(destSvc);

		//system fbo
		if(fromfbo == null)
		{
			AirportBean airport = this.getAirport(aircraft.getLocation());
			FboBean[] fbos = this.getFboForRepair(airport);

			if (fbos.length == 0)
				throw new DataError("Unable to find a repair shop!");

			Arrays.sort(fbos, (o1, o2) -> Integer.compare(o2.getRepairShopMargin(), o1.getRepairShopMargin()));
			fromfbo = fbos[0]; //return the cheapest
		}

		if(tofbo == null)
		{
			// Get a Default FBO if none is specified
			AirportBean airport = this.getAirport(shipto);
			FboBean[] fbos = this.getFboForRepair(airport);

			if (fbos.length == 0)
				throw new DataError("Unable to find a repair shop!");

			Arrays.sort(fbos, (o1, o2) -> Integer.compare(o2.getRepairShopMargin(), o1.getRepairShopMargin()));
			tofbo = fbos[0]; //return the cheapest
		}

		//get the margins
        departMargin = departSvc == 0 ?	departMargin = 25 : fromfbo.getRepairShopMargin();
        destMargin = destSvc == 0 ?	destMargin = 25 : tofbo.getRepairShopMargin();

        AircraftBean acShippingInfo = getAircraftShippingInfoByRegistration(aircraft.getRegistration())[0];

		//Compute total shipping costs
		double[] shippingcost = acShippingInfo.getShippingCosts(1);
		
		double totaldepartcost = shippingcost[0] * (1.0+(departMargin/100.0));
		double totaldestcost = shippingcost[1] * (1.0+(destMargin/100.0));
		
		//Total amount for bank check
		double totalshippingcost = totaldepartcost + totaldestcost;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{			
			double money = getAccountFundsById(aircraft.getOwner());
			if (money < totalshippingcost)
				throw new DataError("Not enough money to ship aircraft");				

			//Payments and log entries are all handled here
			doMaintenanceAircraftShipment(aircraft, AircraftMaintenanceBean.MAINT_SHIPMENTDISASSEMBLY, user, false, fromfbo, totaldepartcost);
			doMaintenanceAircraftShipment(aircraft, AircraftMaintenanceBean.MAINT_SHIPMENTREASSEMBLY, user, true, tofbo, totaldestcost);

			//remove aircraft from use and set shipping details
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration = '" + aircraft.registration + "'");
			
			rs.next();
			
			//set state to disassembly
			rs.updateInt("shippingState", 1);
			
			//compute the time for disassembly
			Date date = new Date();
			Date shippingNext = new Date( date.getTime() + (acShippingInfo.getShippingStateDelay()*1000));
			Timestamp ts = new Timestamp(shippingNext.getTime());
			rs.updateTimestamp("shippingStateNext", ts );

			//set who shipped the aircraft
			rs.updateInt("ShippedBy", user.getId());
			
			//set where its being shipped
			rs.updateString("shippingTo", shipto);
			
			//Remove fuel
			aircraft.emptyAllFuel();
			aircraft.writeFuel(rs);
			
			rs.updateRow();
			rs.close();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public AircraftBean[] getAircraftForUser(int userId)
	{
		return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND userlock="+userId);			
	}
	
	//Modified to add Lessor by Airboss 5/8/11
	public AircraftBean[] getAircraftOwnedByUser(int userId)
	{
		return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND (owner="+ userId + " OR lessor="+ userId + ") ORDER BY make,models.model");			
	}	
	
	public AircraftBean[] getAircraftInArea(String location, closeAirport[] locations)
	{
		StringBuilder where = new StringBuilder("'" + location + "'");

        for (closeAirport location1 : locations)
            where.append(", '" + location1.icao + "'");

		return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND location in (" + where.toString() + ")");
	}
	
	public AircraftBean[] getAircraftOfTypeInArea(String location, closeAirport[] locations, int type)
	{
		StringBuilder where = new StringBuilder("'" + location + "'");
        for (closeAirport location1 : locations)
            where.append(", '" + location1.icao + "'");

		return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND models.id = " + type + " AND location in (" + where.toString() + ")");
	}
	
	public AircraftBean[] getAircraft(String location)
	{
		return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND location='" + location + "'" + "ORDER BY make,models.model");
	}
	
	/**
	 * Finds all aircraft for sale that match user supplied parameters
	 * @param modelId
	 * @param lowPrice
	 * @param highPrice
	 * @param lowTime
	 * @param highTime
	 * @param lowPax
	 * @param highPax
	 * @param lowLoad
	 * @param highLoad
	 * @param distance
	 * @param fromParam
	 * @param hasIfr
	 * @param hasAp
	 * @param hasGps
	 * @param isSystemOwned
	 * @param isPlayerOwned
	 * @param equipment
	 * @return AircraftBean[]
	 */
	public AircraftBean[] findAircraftForSale(int modelId, int lowPrice, int highPrice, int lowTime, int highTime, int lowPax, int highPax, int lowLoad, int highLoad, int distance, String fromParam, boolean hasVfr, boolean hasIfr, boolean hasAp, boolean hasGps, boolean isSystemOwned, boolean isPlayerOwned, String equipment) throws DataError
	{
		ArrayList<AircraftBean> result = new ArrayList<>();

		try
		{
			StringBuilder tables = new StringBuilder("aircraft");
			StringBuilder where = new StringBuilder(" WHERE aircraft.model = models.id AND sellPrice is not null ");
			StringBuilder query = new StringBuilder("SELECT * FROM ");
			StringBuilder query2 = new StringBuilder("SELECT DISTINCT location, lat, lon FROM aircraft, models, airports ");
			
			tables.append(", models");
			
			// Construct equipment code to reflect installed equipment
			if (!equipment.equals("all"))
			{
				int equipmentCode = 0;
				
				if (equipment.equals("vfrOnly"))
				{
					where.append("AND aircraft.equipment = ");
					where.append(equipmentCode);
					where.append(" ");
				}
				else
				{ // equipment.equals("equipmentList")					
					if (hasIfr)
						equipmentCode += ModelBean.EQUIPMENT_IFR_MASK;		
					if (hasAp)
						equipmentCode += ModelBean.EQUIPMENT_AP_MASK;		
					if (hasGps)
						equipmentCode += ModelBean.EQUIPMENT_GPS_MASK;
					
					if(hasVfr)
					{
						where.append("AND (aircraft.equipment & ");
						where.append(equipmentCode);
						where.append(" ) ='");
						where.append(equipmentCode);
						where.append("' AND (aircraft.equipment & ");
						where.append(ModelBean.EQUIPMENT_IFR_MASK);
						where.append(" ) = 0 ");
					}
					else
					{
						where.append("AND (aircraft.equipment & ");
						where.append(equipmentCode);
						where.append(" ) ='");
						where.append(equipmentCode);
						where.append("' ");
					}					
				}
			}
			
			if (modelId > 0) 
			{
				where.append("AND models.id = ");
				where.append(modelId);
				where.append(" ");
			}
						
			if (lowPrice != -1)
			{
				where.append("AND sellPrice >= ");
				where.append(lowPrice);
				where.append(" ");		
			}
			
			if (highPrice != -1)
			{
				where.append("AND sellPrice <= ");
				where.append(highPrice);
				where.append(" ");
			}	
			
			if (lowPax != -1)
			{
				where.append("AND (seats-IF(crew>2,2,crew)) >= ");
				where.append(lowPax);
				where.append(" ");		
			}
			
			if (highPax != -1)
			{
				where.append("AND (seats-IF(crew>2,2,crew)) <= ");
				where.append(highPax);
				where.append(" ");
			}
			
			if (lowLoad != -1)
			{
				where.append("AND (maxWeight-emptyWeight) >= ");
				where.append(lowLoad);
				where.append(" ");
			}
			
			if (highLoad != -1)
			{
				where.append("AND (maxWeight-emptyWeight) <= ");
				where.append(highLoad);
				where.append(" ");	
			}			
			
			if ((lowTime == -1 || lowTime == 0) && highTime != -1)
			{
				where.append("AND (airframe <= ");
				where.append(highTime * 3600);				
				where.append(" OR airframe IS NULL) ");
			}
			else
			{
				if (lowTime != -1)
				{
					where.append("AND airframe >= ");
					where.append(lowTime * 3600);
					where.append(" ");
				}
				
				if (highTime != -1)
				{
					where.append("AND airframe <= ");
					where.append(highTime * 3600);
					where.append(" ");
				}	
			}
			
			if (isSystemOwned != isPlayerOwned)
			{
				if (isSystemOwned)
					where.append("AND owner = 0 ");
				
				if (isPlayerOwned)
					where.append("AND owner != 0 ");
			}
			
			query.append(tables);
			query.append(where);
			query.append("ORDER BY models.make, models.model, aircraft.sellPrice");
			query2.append(where);
			query2.append(" AND icao = location");
			
			double lat = 0, lon = 0;
			
			if (fromParam != null)
			{
				//String qry = "SELECT lat, lon FROM airports WHERE icao = ?";
				//ResultSet rsAp = dalHelper.ExecuteReadOnlyQuery(qry, fromParam);
				//if (!rsAp.next())
				//	throw new DataError("Airport " + fromParam + " not found.");
				LatLonSize lls = cachedAPs.get(fromParam.toUpperCase());
				if(lls == null)
					throw new DataError("Airport " + fromParam.toUpperCase() + " not found.");
				
				lat = lls.lat;
				lon = lls.lon;			
			}			
			
			Map<String, Double> distanceMap = new HashMap<>();
			double lat1, lon1;
			
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(query2.toString());
			while (rs.next())
			{
				lat1 = rs.getDouble(2);
				lon1 = rs.getDouble(3);
				
				distanceMap.put(rs.getString(1), getDistance(lat1, lon1, lat, lon));				
			}
			
			rs = dalHelper.ExecuteReadOnlyQuery(query.toString());
			while (rs.next())
			{
				AircraftBean aircraft = new AircraftBean(rs);
					
				// not searching with a distance parameter, just add aircraft to result.
				if (fromParam == null)
				{
					result.add(aircraft);
				}
				else // searching with a distance parameter
				{
					if (aircraft.getLocation() != null && distanceMap.get(aircraft.getLocation()) < distance)
						result.add(aircraft);
				}				
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new AircraftBean[result.size()]);
	}
	
	/** 
	 * @param modelId aircraft model type
	 * @return count of aircraft available for sale in system now for given model type
	 */
	public int FindAircraftForSaleByModelCount(int modelId) 
	{
		int count=0;

		try
		{
			String qry = "SELECT count(*) FROM aircraft, models WHERE aircraft.model = models.id AND sellPrice is not null AND (aircraft.equipment & 0 ) ='0' AND models.id = ?";
			count = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), modelId); 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return count;
	}
	
	public AircraftBean[] getAircraftSQL(String qry)
	{		
		ArrayList<AircraftBean> result = new ArrayList<>();
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				AircraftBean aircraft = new AircraftBean(rs);
				result.add(aircraft);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result.toArray(new AircraftBean[result.size()]);
	}
	
	public TemplateBean[] getAllTemplates()
	{
		return getTemplateSQL("SELECT * FROM templates");
	}
	
	public TemplateBean[] getTemplateById(int Id)
	{
		return getTemplateSQL("SELECT * FROM templates WHERE id = " + Id);
	}
	
	public TemplateBean[] getTemplateSQL(String qry)
	{
		ArrayList<TemplateBean> result = new ArrayList<>();
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				TemplateBean template = new TemplateBean(rs);
				result.add(template);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new TemplateBean[result.size()]);
	}
	
	public void setAccountXmlKey(UserBean account)
	{
		if (account.xmlKey == null)
		{
			String newKey = createPassword();
			account.setXmlKey(newKey);
			try
			{
				String qry = "UPDATE accounts SET xmlkey = ? WHERE id = ?";
				dalHelper.ExecuteUpdate(qry, newKey, account.getId());
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			} 
		}
	}
	
	public UserBean[] getGroupById(int id)
	{
		return getAccountSQL("SELECT * FROM accounts WHERE type = 'group' AND id = " + id);
	}

	public String getAccountTypeById(int id)
	{
		String result = "person";
		try
		{
			String qry = "SELECT `type` FROM accounts WHERE id = ?";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);			
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return result;
	}

	public BigDecimal getAccountInterest(int id)
	{
	    BigDecimal retval = new BigDecimal("0.00");
	    try
	    {
	    	String qry = "SELECT interest FROM accounts WHERE id = ?";
	    	retval = this.dalHelper.ExecuteScalar(qry, new DALHelper.BigDecimalResultTransformer(), Integer.valueOf(id));
	    }
	    catch (SQLException e)
	    {
	      e.printStackTrace();
	    }
	    return retval;
	}
	  
	 public UserBean getAccountById(int id)
	{
		UserBean[] result = getAccountSQL("SELECT * FROM accounts WHERE id = " + id);
		return result.length == 0 ? null : result[0];
	}

	public String getAccountNameById(int id)
	{
		String retval = null;
		
		try
		{
			String qry = "SELECT name FROM accounts WHERE id = ?";
			retval = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return retval;
	}

	public UserBean[] getAccountByName(String name)
	{
		return getAccountSQL("SELECT * FROM accounts WHERE type = 'person' AND name = '" + name + "'");
	}	
	
	//Added by Airboss 5/8/11
	public UserBean[] getAccountGroupOrUserByName(String name)
	{
		return getAccountSQL("SELECT * FROM accounts WHERE (type = 'person' OR type = 'group') AND name = '" + name + "'");
	}
	
	public UserBean[] getGroupsThatInviteUser(int userId)
	{
		return getAccountSQL("SELECT * FROM accounts WHERE type = 'group' AND EXISTS (SELECT * FROM groupmembership WHERE groupId = accounts.id AND userId = " + userId + " AND level = 'invited') ORDER BY name");
	}
	
	public UserBean[] getGroupsForUser(int userId)
	{
		return getAccountSQL("SELECT * FROM accounts WHERE type = 'group' AND EXISTS (SELECT * FROM groupmembership WHERE groupId = accounts.id AND userId = " + userId + " AND level <> 'invited' ) ORDER BY name");
	}
	
	public UserBean[] getAllExposedGroups()
	{
		return getAccountSQL("SELECT * FROM accounts WHERE type = 'group' AND (exposure & " + UserBean.EXPOSURE_GROUPS + ") > 0  ORDER BY name");
	}
	
	public UserBean[] getUsersForGroup(int groupId)
	{
		return getAccountSQL("SELECT accounts.* FROM accounts, groupmembership WHERE type = 'person' AND userId = accounts.id AND groupId = " + groupId + " ORDER BY FIND_IN_SET(groupmembership.level, 'owner,staff,member'), name ");
	}
	
	public UserBean[] getAccounts()
	{
		return getAccounts(false);
	}
	
	public UserBean[] getAccounts(boolean usersonly)
	{
		if(usersonly)
			return getAccountSQL("SELECT * from accounts WHERE type = 'person' ORDER BY name");
		else
			return getAccountSQL("SELECT * from accounts ORDER BY name");
	}
	
	public UserBean[] getExposedAccounts()
	{
		return getAccountSQL("SELECT * from accounts WHERE exposure <> 0 ORDER BY name");
	}

	public boolean isGroupOwnerStaff(int groupid, int userid)
	{
		boolean result = false;
		try
		{
			String qry = "SELECT (count(groupid) > 0) AS found FROM groupmembership WHERE groupid = ? AND userid = ? AND (level = 'owner' OR level = 'staff')";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), groupid, userid);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}

	public int getUserGroupIdByReadAccessKey(String key)
	{
		int result = -1;
		try
		{
			String qry = "SELECT id FROM accounts WHERE ReadAccessKey = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, key);
			if(rs.next())
				result = rs.getInt("id");
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	public String getNameByReadAccessKey(String key)
	{
		String result = null;
		try
		{
			String qry = "SELECT name FROM accounts WHERE ReadAccessKey = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, key);
			if(rs.next())
				result = rs.getString("name");
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	//Autocomplete work
	public UserBean[] getAccountNames(String partialName, int limit)
	{
		return getAccountNames(partialName, ACCT_TYPE_ALL, limit);
	}
	
	public UserBean[] getAccountNames(String partialName, int acctType, int limit)
	{
		return getAccountNames(partialName, acctType, limit, false);
	}
	
	public UserBean[] getAccountNames(String partialName, int acctType, int limit, boolean displayHidden)
	{
		ArrayList<UserBean> result = new ArrayList<>();
		try
		{
			String accttype = ""; // ACCT_TYPE_ALL
			if( acctType == ACCT_TYPE_PERSON )
				accttype = " AND type = 'person' ";
			else if( acctType == ACCT_TYPE_GROUP )
				accttype = " AND type = 'group' ";

			String qry;
			if(displayHidden)
				qry = "SELECT * FROM accounts WHERE name like ? " + accttype + " ORDER BY name LIMIT " + limit;
			else
				qry = "SELECT * FROM accounts WHERE exposure <> 0 AND name like ? " + accttype + " ORDER BY name LIMIT " + limit;
			
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, partialName+"%");
			while (rs.next())
			{
				UserBean template = new UserBean(rs);
				result.add(template);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new UserBean[result.size()]);
	}

	UserBean[] getAccountSQL(String qry)
	{
		ArrayList<UserBean> result = new ArrayList<>();
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				UserBean template = new UserBean(rs);
				result.add(template);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new UserBean[result.size()]);
	}
	
	public int accountUltimateOwner(int accountId)
	{
		int result = accountId;
		try
		{
			String qry = "SELECT userId FROM groupmembership WHERE level = 'owner' AND groupId = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, accountId);
			if (rs.next())
				result = rs.getInt("userId");
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result;
	}

	public int accountUltimateGroupOwner(int accountId)
	{
		int result = accountId;
		try
		{
			String qry = "SELECT userId FROM groupmembership WHERE level = 'owner' AND groupId = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, accountId);
			if (rs.next())
				result = rs.getInt("userId");
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result;
	}
	
	public ModelBean[] getAllModels()
	{
		return getModelSQL("SELECT * FROM models ORDER By make,model");
	}
	
	public ModelBean[] getModelById(int id)
	{
		return getModelSQL("SELECT * FROM models WHERE id = " + id);
	}
	
	public ModelBean[] getModelSQL(String qry)
	{
		ArrayList<ModelBean> result = new ArrayList<>();
		
		try
		{
			ResultSet rs= dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				ModelBean model = new ModelBean(rs);
				result.add(model);
			} 
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new ModelBean[result.size()]);
	}
	
	public String[] getMappingsFilterList()
	{
		ArrayList<String> result = new ArrayList<>();

		try
		{
			String startchar = null, endchar = null;
			int groupcount = 0;
			int thiscount;

			String qry = "select substr(fsaircraft, 1, 1) as firstchar, count(*) from fsmappings group by firstchar order by firstchar";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				thiscount = rs.getInt(2);
				if (startchar != null && thiscount + groupcount > 200) 
				{
					if (startchar.equals(endchar))
						result.add(startchar);
					else
						result.add(startchar + ".." + endchar);

					startchar = null;
                    groupcount = 0;
				}
				
				groupcount += thiscount;
				endchar = rs.getString(1).toUpperCase();
				
				if (startchar == null)
					startchar = endchar;
			}

			if (startchar.equals(endchar)) 
				result.add(startchar);
			else
				result.add(startchar + ".." + endchar);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return result.toArray(new String[result.size()]);
	}
		
	public FSMappingBean[] getFilteredMappings(String filter)
	{
		if (filter.length() != 1 && filter.length() != 4)
			return null;
		
		char[] chars = filter.toCharArray();
		String infilter = "";
		
		if (filter.length() == 4) 
		{
			for (int c = (int)chars[0]; c <= (int)chars[3]; c++) 
			{
				if (infilter.length() > 0)
					infilter = infilter + ", ";
				
				infilter = infilter + "'" + (char)c + "'";
			}
		} 
		else 
		{
			infilter = "'" + filter + "'";
		}
		
		return getMappingSQL("SELECT * FROM fsmappings where substr(fsaircraft, 1, 1) in (" + infilter + ") ORDER By fsaircraft");
	}
		
	public FSMappingBean[] getAllMappings()
	{
		return getMappingSQL("SELECT * FROM fsmappings ORDER By fsaircraft");
	}
	
	public FSMappingBean[] getRequestedMappings()
	{
		return getMappingSQL("SELECT * FROM fsmappings WHERE model = 0 ORDER By fsaircraft");
	}
	
	public FSMappingBean[] getMappingById(int id)
	{
		return getMappingSQL("SELECT * FROM fsmappings WHERE id = " + id);
	}	
	
	public FSMappingBean[] getMappingByFSAircraft(String target)
	{
		return getMappingSQL("SELECT * FROM fsmappings WHERE fsaircraft LIKE '%" + target + "%' order by fsaircraft");
	}
	
	public FSMappingBean[] getMappingSQL(String qry)
	{
		ArrayList<FSMappingBean> result = new ArrayList<>();

		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				FSMappingBean mapping = new FSMappingBean(rs);
				result.add(mapping);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new FSMappingBean[result.size()]);
	}
	
	public void setMapping(int id, int modelId)
	{		
		if(modelId < 0)
			deleteMapping(id);
		else
			updateMapping(id, modelId);
	}
	
	private void updateMapping(int id, int modelId)
	{
		try
		{
			String qry = "UPDATE fsmappings SET model = ? WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, modelId, id);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private void deleteMapping(int id)
	{
		try
		{
			String qry = "DELETE from fsmappings WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, id);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public int getAmountLogForGroup(int groupId)
	{
		return getIntSQL("SELECT count(*) FROM log WHERE groupId='" + groupId + "' ");
	}
	
	public LogBean[] getLogForAircraftByMonth(String reg, int month, int year)
	{				
		String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + ""
		+ " AND aircraft = '" + Converters.escapeSQL(reg) + "'"; 

		return getLogSQL(sql);
	}	

	public LogBean[] getLogForAircraftFromId(String reg, int fromid)
	{				
		String sql = "SELECT * FROM log where id > " + fromid
		+ " AND aircraft = '" + Converters.escapeSQL(reg) + "'"
		+ " order by id LIMIT 500"; 

		return getLogSQL(sql);
	}	

	public LogBean[] getLogForGroupByMonth(int groupId, int month, int year)
	{				
		String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + ""
		+ " AND groupId = " + groupId; 

		return getLogSQL(sql);
	}	

	public LogBean[] getLogForUserByMonth(String username, int month, int year)
	{				
		String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + ""
		+ " AND user = '" + Converters.escapeSQL(username) + "'"; 

		return getLogSQL(sql);
	}	
	
	public LogBean[] getLogForGroupFromId(int groupId, int fromid)
	{				
		String sql = "SELECT * FROM log where id > " + fromid
		+ " AND groupId = " + groupId
		+ " order by id LIMIT 500"; 

		return getLogSQL(sql);
	}	

	public LogBean[] getLogForGroupFromRegistrations(String registrations, int fromid)
	{				
		String sql = "SELECT * FROM log where id > " + fromid
		+ " AND aircraft in (" + registrations + ")"
		+ " order by id LIMIT 500"; 

		return getLogSQL(sql);
	}	

	public LogBean[] getLogForUserFromId(String username, int fromid)
	{				
		String sql = "SELECT * FROM log where id > " + fromid
		+ " AND user = '" + Converters.escapeSQL(username) + "'" 
		+ " order by id LIMIT 500"; 

		return getLogSQL(sql);
	}		

	public LogBean[] getLogForGroup(int groupId, int from, int to)
	{
		return getLogSQL("SELECT * FROM log WHERE groupId=" + groupId + " ORDER BY time DESC LIMIT " + from + "," + to);
	}
	
	public LogBean[] getLogForUser(UserBean user, int from, int amount)
	{
		return getLogSQL("SELECT * FROM log WHERE type <> 'refuel' and type <> 'maintenance' and user='" + Converters.escapeSQL(user.getName()) + "' ORDER BY time DESC LIMIT " + from + "," + amount);
	}	
	
	public LogBean[] getLogForUser(UserBean user, int afterLogId)
	{
		return getLogSQL("SELECT * FROM log WHERE id > " + afterLogId + " AND type <> 'refuel' and type <> 'maintenance' and user='" + Converters.escapeSQL(user.getName()) + "' ORDER BY time DESC ");
	}	
	
	public int getAmountLogForUser(UserBean user)
	{
		return getIntSQL("SELECT count(*) FROM log WHERE user='" + Converters.escapeSQL(user.getName()) + "'");		
	}
	
	public LogBean[] getLogById(int id)
	{
		return getLogSQL("SELECT * FROM log WHERE id = " + id);
	}
	
	public LogBean[] getLogForFbo(int fbo, int from, int amount)
	{
		return getLogSQL("SELECT * FROM log WHERE fbo=" + fbo + " ORDER BY time DESC LIMIT " + from + "," + amount);
	}
	
	public LogBean[] getLogForAircraft(String registration, int from, int amount)
	{
		return getLogSQL("SELECT * FROM log WHERE aircraft='" + Converters.escapeSQL(registration) + "' ORDER BY time DESC LIMIT " + from + "," + amount);
	}
	
	public LogBean[] getLogForAircraft(String registration, int entryid)
	{
		return getLogSQL("SELECT * FROM log WHERE aircraft='" + Converters.escapeSQL(registration) + "' AND ID > " + entryid + " ORDER BY time DESC");
	}
	
	public int getAmountLogForAircraft(String registration)
	{
		return getIntSQL("SELECT count(*) FROM log WHERE aircraft='" + Converters.escapeSQL(registration) + "'");
	}
	
	public int getAmountLogForFbo(int fbo)
	{
		return getIntSQL("SELECT count(*) FROM log WHERE fbo=" + fbo);
	}			

	public LogBean[] getLogForMaintenanceAircraft(String registration)
	{
		return getLogSQL("SELECT * FROM log WHERE type = 'maintenance' AND aircraft='" + Converters.escapeSQL(registration) + "' ORDER BY time DESC");
	}
	
	LogBean[] getLogSQL(String qry)
	{
		ArrayList<LogBean> result = new ArrayList<>();

		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				LogBean log = new LogBean(rs);
				result.add(log);
			} 
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new LogBean[result.size()]);
	}
	
	public int getAmountPaymentsForUser(int user, int fboId, String aircraft, boolean paymentsToSelf)
	{
		String where = "";
		if (fboId > 0)
			where = where + " AND fbo = " + fboId;
		
		if ((aircraft != null) && !aircraft.equals(""))
			where = where + " AND aircraft = '" + Converters.escapeSQL(aircraft) + "'";
		
		if (!paymentsToSelf)
			where = where + " AND user <> otherparty";
		
		return getIntSQL("SELECT count(*) FROM payments WHERE user = " + user + where) + 
				getIntSQL("SELECT count(*) FROM payments WHERE otherparty = " + user + where);		
	}
	
	public PaymentBean[] getPaymentsForUser(int user, int from, int amount, int fboId, String aircraft, boolean paymentsToSelf)
	{
		String where = "";
		if (fboId > 0)
			where = where + " AND fbo = " + fboId;
		
		if ((aircraft != null) && !aircraft.equals(""))
			where = where + " AND aircraft = '" + Converters.escapeSQL(aircraft) + "'";
		
		if (!paymentsToSelf)
			where = where + " AND user <> otherparty ";
		
		if(from < 0) 
			from = 0;
		
		if(amount <= 0) 
			amount = 10;
		
		//Airboss 7/11/13
		//See: http://explainextended.com/2011/02/11/late-row-lookups-innodb/
		//See: http://explainextended.com/2009/10/23/mysql-order-by-limit-performance-late-row-lookups/
		return getPaymentLogSQL("Select m.* from (Select id from payments where (user = " + user + " OR otherparty = " + user + ") " + where + " order by id desc Limit " + from + "," + amount + ") q join payments m on q.id=m.id order by id desc");
	}
	
	public PaymentBean[] getPaymentsForIdByMonth(int id, int month, int year)
	{	
		String sql = "Select m.* from (Select id, year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00')) year, month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00')) month from payments where (user = " + id + " OR otherparty = " + id + ") order by id desc) q join payments m on q.id=m.id WHERE (q.year= " + year + " AND q.month=" + month + ")  order by id desc";
		return getPaymentLogSQL(sql);
	}
	
	public PaymentBean[] getPaymentLogSQL(String qry)
	{
		ArrayList<PaymentBean> result = new ArrayList<>();

		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				PaymentBean log = new PaymentBean(rs);
				result.add(log);
			} 
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result.toArray(new PaymentBean[result.size()]);
	}
	
	public Object[] outputLog(String selection)
	{
		StringBuilder result1 = new StringBuilder();
		Set<String> aircraft = new HashSet<>();
		Set<String> users = new HashSet<>();

		try
		{
			int count=0;
			
			String qry = "SELECT log.user, log.aircraft, `from`, `to`, a.lat,  a.lon,  b.lat,  b.lon, log.time FROM log, airports a, airports b where log.`from` = a.icao and log.`to` = b.icao and log.type='flight' and " + selection;
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				if (count++ > 0)
					result1.append(",\n");
				
				result1.append("['").append(rs.getString(1)).append("','").append(rs.getString(2)).append("','").append(rs.getString(3)).append("','").append(rs.getString(4)).append("',").append(rs.getDouble(5)).append(",").append(rs.getDouble(6)).append(",").append(rs.getDouble(7)).append(",").append(rs.getDouble(8)).append(",'").append(Formatters.datemmddyy.format(rs.getDate(9))).append("']");
				users.add(rs.getString(1));
				aircraft.add(rs.getString(2));
			} 	
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		List<String> aircraftList = new ArrayList<>(aircraft);
		List<String> usersList = new ArrayList<>(users);
		Collections.sort(aircraftList);
		Collections.sort(usersList);
		
		return new Object[]{result1.toString(), aircraftList, usersList};
	}
	
	int getIntSQL(String qry)
	{
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			if (rs.next())
				return rs.getInt(1);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return -1;
	}
	
	public String BuildAssignmentCargoFilter(int minPax, int maxPax, int minKG, int maxKG)
	{
		String paxFilter = "";
		if (minPax > -1 || maxPax > -1)
		{
			paxFilter = "units = " + AssignmentBean.UNIT_PASSENGERS + " AND ";
			if (minPax > -1)
			{
				paxFilter = paxFilter + "amount >= " + minPax + " ";
				if (maxPax > -1)
					paxFilter = paxFilter + "AND ";
			}
			if (maxPax > -1)
				paxFilter = paxFilter + "amount <= " + maxPax;
			
			paxFilter = "(" + paxFilter + ")";
		}
		
		String kgFilter = "";
		if (minKG > -1 || maxKG > -1)
		{
			kgFilter = "units = " + AssignmentBean.UNIT_KG + " AND ";
			if (minKG > -1)
			{
				kgFilter = kgFilter + "amount >= " + minKG + " ";
				if (maxKG > -1)
					kgFilter = kgFilter + "AND ";
			}
			if (maxKG > -1)
				kgFilter = kgFilter + "amount <= " + maxKG;
			
			kgFilter = "(" + kgFilter + ")";
		}
		
		if (paxFilter.length() > 0 && kgFilter.length() > 0)
			return "(" + paxFilter + " OR " + kgFilter + ") AND ";
		else if (paxFilter.length() > 0)
			return paxFilter + " AND ";
		else if (kgFilter.length() > 0)
			return kgFilter + " AND ";
		else
			return "";
	}
	
	public AssignmentBean[] getAssignmentsToArea(String location, closeAirport[] locations, int minPax, int maxPax, int minKG, int maxKG)
	{
		StringBuilder where = new StringBuilder("'" + Converters.escapeSQL(location) + "'");
        for (closeAirport location1 : locations)
            where.append(", '").append(location1.icao).append("'");

		String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);
		
		return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " toicao in (" + where.toString() + ") ORDER BY bearing");
	}
	
	public AssignmentBean[] getAssignmentsInArea(String location, closeAirport[] locations, int minPax, int maxPax, int minKG, int maxKG)
	{
		StringBuilder where = new StringBuilder("'" + Converters.escapeSQL(location) + "'");
        for (closeAirport location1 : locations)
            where.append(", '" + location1.icao + "'");

		String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);

		return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " location in (" + where.toString() + ") ORDER BY bearing");
	}
	
	public AssignmentBean[] getAssignmentById(int id)
	{
		return getAssignmentsSQL("SELECT * FROM assignments WHERE id=" + id);
	}
	
	public AssignmentBean[] getAssignmentsForGroup(int groupId, boolean includelocked)
	{
		if (includelocked) 
			return getAssignmentsSQL("SELECT * FROM assignments WHERE groupId=" + groupId + " ORDER BY location");
		else
			return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId=" + groupId + " ORDER BY location");
	}
	
	public AssignmentBean[] getAssignmentsForUser(int userId)
	{
		return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock=" + userId);
	}
	
	public AssignmentBean[] getAssignmentsForTransfer(int userId)
	{
		return getAssignmentsSQL("SELECT * FROM assignments WHERE owner=" + userId);
	}
	
	public AssignmentBean[] getAssignments(String location, int minPax, int maxPax, int minKG, int maxKG)
	{
		String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);
		return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " location='" + location + "' AND (aircraft is null OR location = (SELECT location FROM aircraft WHERE aircraft.registration = assignments.aircraft)) ORDER BY bearing");
	}
	
	public AssignmentBean[] getAssignmentsToAirport(String location, int minPax, int maxPax, int minKG, int maxKG)
	{
		String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);
		return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " toicao='" + location + "' AND (aircraft is null OR location = (SELECT location FROM aircraft WHERE aircraft.registration = assignments.aircraft)) ORDER BY bearing");
	}
	
	public AssignmentBean[] getAssignmentsFromAirport(String location)
	{
		String sql = "SELECT * FROM assignments WHERE userlock is null AND groupId is null AND location IN(" + location + ") ORDER BY location, distance";

		return getAssignmentsSQL(sql);
	}
	
	public AssignmentBean[] getAssignmentsToAirport(String location)
	{
		String sql = "SELECT * FROM assignments WHERE userlock is null AND groupId is null AND toicao IN(" + location + ") ORDER BY location, distance";

		return getAssignmentsSQL(sql);
	}
	
	public AssignmentBean[] getAssignmentsSQL(String qry)
	{
		ArrayList<AssignmentBean> result = new ArrayList<>();
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				AssignmentBean assignment = new AssignmentBean(rs);
				result.add(assignment);
			} 
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}	

		return result.toArray(new AssignmentBean[result.size()]);
	}
	
	public void unlockAssignment(int id, boolean unlockAll)
	{
		try
		{
			String qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE (active = 0 or active = 2) AND id = ?";
			boolean exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
			if(exists)
			{
				if(unlockAll)
					qry ="UPDATE assignments SET userlock = NULL, active = 0, groupId = null, pilotFee = 0, comment = null WHERE id = ?";
				else
					qry = "UPDATE assignments SET userlock = NULL, active = 0 WHERE id = ?";

				dalHelper.ExecuteUpdate(qry, id);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void holdAssignment(int id, boolean hold)
	{
		try
		{
			String qry;
			int activeflag = hold ? ASSIGNMENT_HOLD : ASSIGNMENT_ACTIVE;
			
			if (hold)
				qry = "UPDATE assignments SET active = ? WHERE active = 0 AND id = ? AND aircraft IS NULL";
			else 
				qry = "UPDATE assignments SET active = ? WHERE active = 2 and id = ?";

			dalHelper.ExecuteUpdate(qry, activeflag, id);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}	
	
	public void moveAssignment(UserBean user, int id, int group) throws DataError
	{
		UserBean[] groups = getGroupById(group);		
		if (groups.length == 0)
			throw new DataError("Group not found.");
			
		try
		{
			String qry = "SELECT (count(*) > 0) from assignments where id = ?";
			boolean idExists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
			if (!idExists) //just return if not found
				return; 

			//All-In check - can't add All-In flight to a group
			qry = "SELECT (count(*) > 0) from assignments where id = ? and aircraft is not null";
			boolean isAllIn = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
			if (isAllIn)
				throw new DataError("All-In assignments cannot be added to a group queue.");
			
			//See if already locked
			qry = "SELECT userlock, groupId from assignments where id = ?";
			int userlock = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), id);
			if (userlock != 0 && userlock != user.getId())
				throw new DataError("Assignment is already selected by a pilot.");
			
			//Move it to group assignments
			qry = "UPDATE assignments SET pilotFee=pay*amount*distance*0.01*?, groupId = ? WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, groups[0].getDefaultPilotFee()/100.0, group, id);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void commentAssignment(int id, String comment) throws DataError
	{
		try
		{
			String qry = "SELECT (count(*) > 0) from assignments where id = ?";
			boolean idExists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
			if (!idExists) //just return if not found
				return; 

			//Move it to group assignments
			qry = "UPDATE assignments SET comment=? WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, comment, id);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void addAssignment(int id, int user, boolean add) throws DataError
	{
		try
		{
			//Myflight assignment limit rule
			String qry = "SELECT (count(*) >= ?) FROM assignments WHERE userlock= ?";
			boolean toManyJobs = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), MAX_FLIGHT_ASSIGNMENTS, user);
			if (toManyJobs )
				throw new DataError("You have reached the limit of allowed assignments. Limit: " + MAX_FLIGHT_ASSIGNMENTS);

			//No assignment found
			qry = "SELECT (count(*) = 0) AS notFound FROM assignments WHERE id = ?";
			boolean noRecord = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
			if(noRecord)
				throw new DataError("No assignment found for id: " + id);
			
			//Get aircraft registration for assignment
			qry = "SELECT aircraft FROM assignments WHERE id = ?";
			String aircraft = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);

			//This is an All-In job if it has an assigned airplane to the job already
			//Do our checks and rent the aircraft
			if (aircraft != null)	
			{
				//No jobs in the loading area
				qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE userlock = ? AND active <> 2";
				boolean noLoadAreaJobs = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);
				if (noLoadAreaJobs)
					throw new DataError("Cannot have any jobs in the Loading Area when trying to select an All-In job.");
				
				//existing all-in jobs in queue for user
				qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE userLock = ? AND aircraft IS NOT NULL";
				boolean foundAllIn = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);
				if (foundAllIn)
					throw new DataError("FSE Validation Error: cannot have more than 1 All-In job in My Flight queue.");
				
				rentAircraft(aircraft, user, false);
			}
			else
			{
				//All-In validation - cannot add regular assignments when there is an active all-in job
				if (hasAllInJobInQueue(user))
					 throw new DataError("FSE Validation Error: cannot mix All-In jobs with regular jobs.");
			}
			
			qry = "UPDATE assignments SET userlock = ? where id = ?";
			dalHelper.ExecuteUpdate(qry, user, id);
 		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void removeAssignment(int id, int user, boolean add) throws DataError
	{
		try
		{
			//No assignment found
			String qry = "SELECT (count(*) = 0) AS notFound FROM assignments WHERE id = ?";
			boolean noRecord = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
			if(noRecord)
				throw new DataError("No assignment found for id: " + id);
			
			//Get aircraft registration for assignment
			qry = "SELECT aircraft FROM assignments WHERE id = ?";
			String aircraft = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);

			//if All-In job, remove lock on aircraft now that job is canceled
			if (aircraft != null)
				releaseAircraft(aircraft, user);
			
			qry = "UPDATE assignments SET userlock = null, active = 0 where id = ?";
			dalHelper.ExecuteUpdate(qry, id);
 		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}	
	
	public void rentAircraft(String reg, int user, boolean rentedDry) throws DataError
	{
		try
		{
			//get aircraft info
			String qry = "SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND registration = ?";
			ResultSet aircraftRS = dalHelper.ExecuteReadOnlyQuery(qry, reg);
			
			if (!aircraftRS.next())
				throw new DataError("No aircraft found!");

			AircraftBean aircraft = new AircraftBean(aircraftRS);
			
			//get renter info
			qry = "SELECT * FROM accounts WHERE id = ?";
			ResultSet renterRS = dalHelper.ExecuteReadOnlyQuery(qry, user);

			if (!renterRS.next())
				throw new DataError("user not found!");
			
			UserBean renter = new UserBean(renterRS);
			reloadMemberships(renter);
			
			//get owner of aircraft info
			qry = "SELECT * FROM accounts WHERE id = ?";
			ResultSet ownerRS = dalHelper.ExecuteReadOnlyQuery(qry, aircraft.getOwner());

			if (!ownerRS.next())
				throw new DataError("owner not found!");
			
			UserBean owner = new UserBean(ownerRS);
			
			//compare the renter to the values in the owner's BanList
			if (owner.isInBanList(renter.getName())) 
			{
				throw new DataError("The owner [" + owner.getName()+ "] has indicated that you are not permitted to rent aircraft from them. " +
						"If you wish to contact them about this issue, you must do so privately. " +
						"You may use the forum PM (Private Message) system at <a href='http://www.fseconomy.net/inbox'> http://www.fseconomy.net/inbox</a> if you have no other contact means. " +
						"<b>DO NOT</b> post any public message about this issue in the forums.");
			}			
			
			//The following check allows ALLIN only aircraft to be rented, bypassing the exploit canceling code
			if ((aircraft.getRentalPriceDry() + aircraft.getRentalPriceWet() == 0) && !(aircraft.canAlwaysRent(renter))) 
			{
				ModelBean mb[] = getModelById(aircraft.getModelId());
				if(mb[0].getFuelSystemOnly() == 0) // 0 == can be fueled, so not an ALLIN limited aircraft
					throw new DataError("Rental not authorized");
			}
			
			//normal flow for renting a plane begins			
			qry = "SELECT (count(*) > 0) AS Found FROM aircraft WHERE userlock = ?";
			boolean found = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);
			if (found)
				throw new DataError("There is already an aircraft selected.");
			
			qry = "SELECT (count(*) = 0) AS rented FROM aircraft WHERE userlock is null AND registration = ?";
			boolean rented = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), reg);
			if (rented)
				throw new DataError("Aircraft is already locked.");
			
			qry = "SELECT * FROM aircraft, models WHERE userlock is null AND location is not null AND aircraft.model = models.id AND registration = ?";			
			ResultSet fuelRS = dalHelper.ExecuteReadOnlyQuery(qry, reg);
			fuelRS.next();
			AircraftBean thisCraft = new AircraftBean(fuelRS);
			
			Float initialFuel = rentedDry ? (float)thisCraft.getTotalFuel() : null;
			
			qry = "UPDATE aircraft SET userlock = ?, lockedSince = ?, initialFuel = ? where registration = ?";
			dalHelper.ExecuteUpdate(qry, user, new Timestamp(GregorianCalendar.getInstance().getTime().getTime()), initialFuel, reg);			
 		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void releaseAircraft(String reg, int user) throws DataError
	{
		try
		{
			String qry = "SELECT (count(registration) > 0) AS found FROM aircraft WHERE location is not null AND registration = ? AND userlock = ?";
			boolean found = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), reg, user);
			if(!found)
				return;

			qry = "SELECT location FROM aircraft WHERE location is not null AND registration = ? AND userlock = ?";
			String location = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), reg, user);
			
			if (location == null)
				throw new DataError("No aircraft to cancel.");

			qry = "UPDATE aircraft SET holdRental=0, userlock = ?, lockedSince = ?, initialFuel = ? WHERE registration = ?";
			dalHelper.ExecuteUpdate(qry, null, null, null, reg);			
 		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	public boolean setHoldRental(String reg, int userId, boolean hold)
	{
		boolean result = false;
		try
		{
			String qry = "UPDATE aircraft SET holdRental = ? WHERE registration = ? and userlock = ?";
			dalHelper.ExecuteUpdate(qry, hold, reg, userId);
			result = true; 
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return result;
	}
	
	//Added ability to just add up total value, and not do any DB updates - Airboss 3/5/11
	double payFboGroundCrewFees(String fboIcao, AssignmentBean assignment, int payAssignmentToAccount, String location, String registration, boolean checkonly)
	{
		if (assignment.isFerry())
			return 0.0;
		
		double fboAssignmentFee = 0.0;
		double fbofee = assignment.calcPay() * 0.05;
		FboBean[] fbos = getFboByLocation(fboIcao);
		if (fbos.length > 0)
		{
			FboBean ownerFbo = null;
			if (assignment.getFromFboTemplate() > 0)
			{
				FboFacilityBean facility = getFboFacility(assignment.getFromFboTemplate());
				if (facility != null)
				{
					FboBean facilityFbo = getFbo(facility.getFboId());
					if (facilityFbo != null)
					{
						int facilityFboUltimateOwner = accountUltimateOwner(facilityFbo.getOwner());
                        for (FboBean fbo : fbos)
                        {
                            if (accountUltimateOwner(fbo.getOwner()) == facilityFboUltimateOwner)
                            {
                                ownerFbo = fbo;
                                break;
                            }
                        }
					}
				}
			}
			if (ownerFbo == null)
			{
				int flightUltimateOwner = accountUltimateOwner(payAssignmentToAccount);
                for (FboBean fbo : fbos)
                {
                    if (accountUltimateOwner(fbo.getOwner()) == flightUltimateOwner)
                    {
                        ownerFbo = fbo;
                        break;
                    }
                }
			}
			if (ownerFbo != null)
			{
                fbos = new FboBean[]{ownerFbo};
			}
			
			fboAssignmentFee = fbofee;

			if( !checkonly )
			{
				// Divide fee equally between originating FBOs
				int lotsTotal = 0;
                for (FboBean fbo : fbos)
                    lotsTotal += fbo.getFboSize();

				double thisFboFee;
                for (FboBean fbo : fbos)
                {
                    thisFboFee = fbofee * ((double) fbo.getFboSize() / lotsTotal);
                    doPayment(payAssignmentToAccount, fbo.owner, thisFboFee, PaymentBean.FBO_ASSIGNMENT_FEE, 0, fbo.getId(), location, registration, "", false);
                }
			}
		}

		return fboAssignmentFee;
	}
	
	/**
	 * This method processes an End flight conditon
	 * @param user - logged in user
	 * @param location - aircraft lat/lon
	 * @param engineTime - seconds
	 * @param engineTicks - tach ticks
	 * @param fuel - aircraft fuel by tank (percentage)
	 * @param night - is sim time currently night tiem
	 * @param envFactor - sim weather condition
	 * @param damage - aircraft damage array
	 * @param simType - is the sim FSX
	 * @return int - number of active assignments
	 */
	public synchronized int processFlight(UserBean user, closeAirport location, int engineTime, int engineTicks, float[] fuel, int night, float envFactor, int[][] damage, SimType simType) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try //This try block is for SQL Exceptions
		{
			//Defined outside our inner try so that the aircraft lock can be evaluated in the finally block
			AircraftBean[] aircraft;
			int freeAircraft = 0;

			//Setup our connection needs
			conn = dalHelper.getConnection();			
			
			try //This try block is to free the aircraft processing lock whether processing completed or failed
			{
				/////////////////////////////////////////////////////////////////
				//Validity checks
				/////////////////////////////////////////////////////////////////
				
				// Get the currently locked aircraft for user
				aircraft = getAircraftForUser(user.getId());
				
				//Is valid aircraft
				if (aircraft.length == 0)
				{
					System.err.println(new Timestamp(System.currentTimeMillis()) + " processFlight: No Aircraft lock.  SimType = " + simType.name() + ", User = " + user.getId());
					throw new DataError("VALIDATIONERROR: No aircraft in use, flight aborted.");
				}
				
				//Check flight in progress 
				if (aircraft[0].getDepartedFrom() == null) 
				{ 
					System.err.println(new Timestamp(System.currentTimeMillis()) + " processFlight: No flight in progress.  SimType = " + simType.name() + ", User = " + user.getId()); 
					throw new DataError("VALIDATIONERROR: It appears that a duplicate flight was submitted and canceled. Please check your My Flight page for current flight status."); 
				} 
				
				// Load up our model parameters for later checks
				// Need to do this after getAircraftForUser is validated
				ModelBean[] m = getModelById(aircraft[0].getModelId());  

				// Speed checks
				// Average Speed of flight should be less than 2.5 the models cruise speed, this is very generous
				int flightDistance = (int)Math.round(getDistance(location.icao, aircraft[0].getDepartedFrom()));
				Float flightMPH = (float) (flightDistance/(engineTime/3600.0));
				if (flightMPH > (m[0].getCruise()*2.5))
				{
					cancelFlight(user);
					//Added more debugging variables to the system message, this happens rarely but we have no idea why
					
					System.err.println(new Timestamp(System.currentTimeMillis()) + " Excess Speed Calculated, rejecting flight. SimType = " + simType.name() + ", Reg = " + aircraft[0].getRegistration() + " User = " + user.getId() + " DepartICAO = " + aircraft[0].getDepartedFrom() + " ArriveICAO = " + location.icao + " Distance = " + flightDistance + " Airspeed = " + flightMPH + " EngineTime = " + engineTime);	
					throw new DataError("VALIDATIONERROR: Invalid speed calculated. (" + flightMPH + "-MPH) between DepartICAO = " + aircraft[0].getDepartedFrom() + " to ArriveICAO = " + location.icao + " in " + (int)(engineTime/60.0 + .5) + " Minutes, flight aborted");
				}
				
				///////////////////////////////////////////////////////
				// Checks done, start processing flight data
				///////////////////////////////////////////////////////
				
				//modified to always zero out unused fuel tanks based on model data
				for (int i = 0; i < fuel.length; i++)
				{
					int capacity = m[0].getCap(i);
					if (capacity == 0) 	// Model has 0 fuel for this tank
					{
						fuel[i] = 0;	//Added just to make sure no ghost tanks with fuel - Airboss 3/5/11
						continue; 		// so we need to continue or we get a divide by zero
					}
					
					//If FSX then we need to load up the percentage of fuel left in tank
					if (simType == SimType.FSX || simType == SimType.XP)
						fuel[i] /= capacity;
				}
				
				//Set the current aircraft fuel levels
				aircraft[0].setFuel(fuel);				
	
				// Fuel checks, only sets flag, does not toss data error
				double initialFuel = aircraft[0].getInitialFuel();

				// More fuel then when started, if only that could be true!
				boolean invalidFuel = (!aircraft[0].wasWetRent()) && (initialFuel < aircraft[0].getTotalFuel());				
				
				// Before we start we need to process Pax assignment to assign the 
				// tax rate to each for computing costs so...
				
				// Get the total pax assignments on board
				int ptAssignmentCount = getPTAssignmentCount(user.getId());
				
				// Compute Pax Taxes
				// If the pax count is greater then 5, compute the tax rate and assign to
				// all onboard pax assignments
				updateMultiPTtax(user.getId(), ptAssignmentCount);					
				
				/////////////////////////////////////////////////
				//Start Computing Flight Costs
				/////////////////////////////////////////////////

				float income = 0, fuelCost = 0, rentalCost = 0, bonus = 0, rentalTime;
				float price = 0, crewCost = 0, fboAssignmentFee = 0, mpttax = 0;	
				int totalPilotFee = 0;
				
				//Distance from home ICAO bonus charges
				double[] distanceFromHome = new double[] {0.0,0.0};
				
				float toPayOwner = 0;

				//Flight cost starts here at 0.00
				float flightCost = 0.0f;
				
				//All-In change to ensure no extra charges are added to the bill
				boolean allIn; 
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); 

				//Is the pilot using an AllIn aircraft?
				String qry = "SELECT (count(id) > 0) AS found FROM aircraft, assignments WHERE aircraft.registration = assignments.aircraft AND aircraft.userlock = ?";
				allIn = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user.getId());

				//AllIn flight with no assignment check - Airboss 11-9-12
				String allInAssignmentToIcao = "";
				if(allIn)
				{
					//Get AllIn assignments that are enroute using the reported aircraft
					qry = "SELECT (count(id) > 0) AS found FROM assignments WHERE active = 1 AND aircraft = ?";
					boolean allInAssignment = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft[0].getRegistration());
					if( !allInAssignment )
					{
						System.err.println("  Flight kicked: AllIn flight with no assignment at current aircraft location");
						String errmsg = "VALIDATIONERROR: AllIn flights must have the assignment co-located with aircraft. It appears the assignment was not transported -- flight aborted!";
						
						cancelFlight(user);
						throw new DataError(errmsg);
					}
					qry = "SELECT toicao FROM assignments WHERE active = 1 AND aircraft = ?";
					allInAssignmentToIcao = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), aircraft[0].getRegistration());
				}
				
				//All-In change - don't pay rental, fuel, or distance fees
				if (!allIn)
				{
					// Get Rental cost (Dry/Wet)					
					if (aircraft[0].wasWetRent())
						price = aircraft[0].getRentalPriceWet(); // Wet rental
					else
						price = aircraft[0].getRentalPriceDry();  //Dry rental					
					
					// Get rental time in hours
					rentalTime = (float)(engineTime/3600.0);
					
					// Set our aircraft Rental Cost
					rentalCost = price * rentalTime;
					
					// Check to see if we need to add in fuel costs for Dry rental
					// Currently if no fuel was used no fuel cost, Scot-Free???
					if (!aircraft[0].wasWetRent() && !invalidFuel)
					{
						double cost = (initialFuel - aircraft[0].getTotalFuel()) * getFuelPrice(location.icao);
						if (aircraft[0].getFuelType() == AircraftBean.FUELTYPE_JETA)
							cost *= getJetaMultiplier();
						
						fuelCost = (float)cost;
					}
					
					// if bonus setting is 0 or we are back to where we started, leave pilot bonus at 0
					if(aircraft[0].getBonus() != 0 && !location.icao.contentEquals(aircraft[0].getDepartedFrom()))
					{
						// If we are not at home, we need to calculate distance/bearing otherwise leave at 0, 0
						if(!location.icao.equals(aircraft[0].getHome()))
							distanceFromHome = getDistanceBearing(location.icao, aircraft[0].getHome());

						double dist = 0.0;

						// If departure airport was home icao, no need to calculate the distance
						if(!aircraft[0].getHome().contentEquals(aircraft[0].getDepartedFrom()))
							dist = getDistance(aircraft[0].getHome(), aircraft[0].getDepartedFrom());
						
						double b = aircraft[0].getBonus();
						
						// If both distance and distanceFromHome are 0, bonus is 0
						if( dist != 0 || distanceFromHome[0] != 0.0 )
							bonus =(float)((b * (dist - distanceFromHome[0])/100.0));
					}
					
					//Bonus can be positive or negative, if the plane moves toward home, 
					//then pilot is payed, if away from home then owner is paid
					flightCost = rentalCost + fuelCost - bonus;
					
					toPayOwner = flightCost;  // Pay this to the owner of the aircraft
	
					if(flightCost == 0 && price != 0)
						System.err.println("****> Rental costs for Reg: " + aircraft[0].getRegistration() + 
							", Date: " + new Timestamp(System.currentTimeMillis()) +
							", Owner: " + getAccountNameById(aircraft[0].getOwner()) +  
							", By: " + user.getName() +
							", From: " + aircraft[0].getDepartedFrom() + 
							", To: " + location.icao + 
							", EngineTimeType: " + (aircraft[0].getAccounting() == AircraftBean.ACC_HOUR ? "Hourly" : "Tach") +
							", EngineTime: " + engineTime + 
							", RentalTime: " + rentalTime + 
							", RentalType: " + (aircraft[0].wasWetRent() ? "Wet" : "Dry") + 
							", InvalidFuel: " + invalidFuel + 
							", pricePerHour: " + price + 
							", Rental Cost: " + rentalCost + 
							", FuelCost: " + fuelCost +
							", Bonus: " + bonus + 
							", TotalToOwner: " + flightCost );
					
					// Crew Cost added  - deducts $100 per hour per additional crew member from payout
					crewCost = m[0].getCrew() * 100 * (float)(engineTime/3600.0);					
				}	//All-In clause
	
				// Determine if Group flight and if so payments
				////////////////////////////////////////////////////
				
				boolean groupFlight = false;
				int groupId = -1;
				UserBean groupToPay = null;
				
				// Determine if this is a group flight
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				rs = stmt.executeQuery("SELECT groupId FROM assignments WHERE active=1 AND userlock=" + user.getId() + " AND groupId is not null");
				if (rs.next())
				{
					groupFlight = true;
					groupId = rs.getInt(1);
				}				
				rs.close();
				stmt.close();
				
				// If a group flight recalc pilot pay before payouts.	
				// This adds in the Pilot Fee for those jobs they might have grabbed
				// while flying currently flying for a group that were not from the group assignment page, 
				// and thus have no group ID and pilot fee assigned
				if (groupFlight)
				{
					UserBean[] groups = getGroupById(groupId);
					stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
					stmt.executeUpdate("UPDATE assignments SET pilotFee=pay*amount*distance*0.01*" + groups[0].getDefaultPilotFee()/100.0 + ", groupId = " + groupId + " WHERE active=1 and userlock = " + user.getId() + " and groupId is null");
					stmt.close();
					
					//gurka - added to keep track of the group to pay for closing exploit - to be used later on just before the committ to check balances of group bank account
					groupToPay = groups[0];
				}
				
				//////////////////////////////////////////////
				// Start Payouts
				//////////////////////////////////////////////
				
				// Determine who gets paid
				int payAssignmentToAccount = groupFlight ? groupId : user.getId();
				
				//-- Start exploit code
				//Determine what the current flight income is going to be
				//so that we can disallow negative personal balances for Players and Groups
				// Get all the current active assignments
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				rs = stmt.executeQuery("SELECT * FROM assignments WHERE active=1 AND userlock=" + user.getId() + " AND toicao='" + location.icao + "'");			
				while (rs.next())
				{
					AssignmentBean assignment = new AssignmentBean(rs);
					int pilotFee = assignment.getPilotFee();
					
					//Get assignment PAY here
					float value = (float)assignment.calcPay();

					int mptTaxRate = assignment.getmptTax();
						
					// Charge mptTax - convert tax rate to a percent
					if (mptTaxRate > 0) 
						mpttax += (value * (mptTaxRate * .01)); 
					
					// Used for tracking log
					totalPilotFee += pilotFee;
					income += value;				
					
					fboAssignmentFee += payFboGroundCrewFees(assignment.getFrom(), assignment, payAssignmentToAccount, location.icao, aircraft[0].getRegistration(), true);
					fboAssignmentFee += payFboGroundCrewFees(location.icao, assignment, payAssignmentToAccount, location.icao, aircraft[0].getRegistration(), true);
				}
				rs.close();
				stmt.close();
				
				//added vaidation to make sure exploit not possible for group flights and rentals
				//validate the rental cost is not more then the bank account in the group account
				double netIncome = income - (flightCost + crewCost + totalPilotFee + fboAssignmentFee + mpttax);

				double balance;

				if( groupToPay != null )
					balance = groupToPay.getMoney() + groupToPay.getBank();
				else
					balance = user.getMoney() + user.getBank();
				 
				boolean balanceKick = false;
				
				//group/pilot balance fail check 
				if( groupToPay != null && netIncome < 0 && (balance + netIncome) < 0 )		
					balanceKick = true;
				else if(groupToPay == null && netIncome < 0 && (balance + netIncome) < -40000) 	
					balanceKick = true;
				
				if( balanceKick )
				{
					String errmsg;
					if( groupToPay != null)
						errmsg = "VALIDATIONERROR: Insufficient funds in the group \' " + groupToPay.name + "\' cash account to pay for the flight costs -- flight aborted!";
					else
						errmsg = "VALIDATIONERROR: Insufficient funds in your cash account to pay for the flight costs -- flight aborted!";
					
					cancelFlight(user);
					throw new DataError(errmsg);
				}
				//-- end exploit code

				//rezero out the the variables for actual payment loop
				income = 0;
				fboAssignmentFee = 0;
				mpttax = 0;	
				totalPilotFee = 0;		
				
				//--------------------------------------------
				//Fixed payouts based on aircraft rental
				//--------------------------------------------

				// Pay rental + fuel + bonus to owner of aircraft
				if (toPayOwner != 0)
					doPayment(payAssignmentToAccount, aircraft[0].getOwner(), toPayOwner, PaymentBean.RENTAL, 0, -1, location.icao, aircraft[0].getRegistration(), "", false);
				
				// Pay crew cost
				if (crewCost > 0)
					doPayment(payAssignmentToAccount, 0, crewCost, PaymentBean.CREW_FEE, 0, -1, location.icao, aircraft[0].getRegistration(), "", false);				
				
				//---------------------------------------------
				// Variable payouts based on each assignment
				//---------------------------------------------
				
				// Get all the current active assignments
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				rs = stmt.executeQuery("SELECT * FROM assignments WHERE active=1 AND userlock=" + user.getId() + " AND toicao='" + location.icao + "'");
				while (rs.next())
				{
					AssignmentBean assignment = new AssignmentBean(rs);
					int pilotFee = assignment.getPilotFee();
					
					//Get assignment PAY here
					float value = (float)assignment.calcPay();
					int owner = assignment.getOwner();
					int mptTaxRate = assignment.getmptTax();

                    //log template jobs
                    if(assignment.getFromTemplate() > 0)
                        logTemplateAssignment(assignment, payAssignmentToAccount);

					// Pay assignment to operator of flight
					doPayment(owner, payAssignmentToAccount, value, PaymentBean.ASSIGNMENT, 0, -1, location.icao, aircraft[0].getRegistration(), "", false);

					// If group flight, pay the pilot fee
					if (groupFlight && pilotFee > 0)
						doPayment(payAssignmentToAccount, user.getId(), pilotFee, PaymentBean.PILOT_FEE, 0, -1, location.icao, aircraft[0].getRegistration(), "", false);
					
					// Charge mptTax - convert tax rate to a percent
					if (mptTaxRate > 0) 
					{
						doPayment(payAssignmentToAccount, 0, (value * (mptTaxRate * .01)), PaymentBean.MULTIPLE_PT_TAX, 0, -1, location.icao, aircraft[0].getRegistration(), "", false);
						
						// Used for tracking log
						mpttax += (value * (mptTaxRate * .01)); 
			        }
					
					int commodityId = assignment.getCommodityId();
					if (commodityId > 0)
					{
						// added in check for aircraft shipping commodity id
						if( commodityId < 99)
						{
							changeGoodsRecord(location.icao, commodityId, owner, assignment.getAmount(), false);
						}
						else
						{
							//Commodity field holds registration number in []
							String sa = assignment.getCommodity();
							
							//setup our indexes for pulling registration number out
							int start = 9;
							int end = sa.indexOf(" Shipment Crate");
							
							//Get Registration and trim off any excess space at start and end
							String reg = sa.substring(start,end).trim();
							
							//Finalize our shipment
							finalizeAircraftShipment(reg, false, false);
						}
					}			
					
					// Used for tracking log
					totalPilotFee += pilotFee;
					income += value;				
					
					fboAssignmentFee += payFboGroundCrewFees(assignment.getFrom(), assignment, payAssignmentToAccount, location.icao, aircraft[0].getRegistration(), false);
					fboAssignmentFee += payFboGroundCrewFees(location.icao, assignment, payAssignmentToAccount, location.icao, aircraft[0].getRegistration(), false);
				}
				rs.close();
				stmt.close();

				//For completed Assignments, remove them
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				stmt.executeUpdate("DELETE from assignments WHERE active=1 AND userlock=" + user.getId() + " AND toicao='" + location.icao + "'");
				stmt.close();
				
				//For assignments that have not reached their destination, reset location ICAO to current ICAO
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				stmt.executeUpdate("UPDATE assignments SET location = '" + location.icao + "', active=0 WHERE active=1 AND userlock=" + user.getId());
				stmt.close();
				
				// Determine if the user has any assignments are still active, or at the current location 
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				rs = stmt.executeQuery("SELECT count(*) FROM assignments WHERE userlock=" + user.getId() + " AND (active = 1 OR location='" + location.icao + "')");
				rs.next();

				// if < 1 then no more assignments
				freeAircraft = rs.getInt(1);
				
				rs.close();
				stmt.close();
				
				// update the aircraft parameters
				/////////////////////////////////////////////////////
				int totalEngineTime = 0;
				int totalAirframeTime = 0;
	
				// Get an updatable recordset for the current aircraft
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);			
				rs = stmt.executeQuery("SELECT * FROM aircraft WHERE userlock=" + user.getId());
				rs.next();
				
				totalEngineTime = rs.getInt("engine1") + engineTime;				
				totalAirframeTime = rs.getInt("airframe") + engineTime;				

				rs.updateInt("engine1", totalEngineTime);
				rs.updateInt("airframe", totalAirframeTime);
				rs.updateNull("departedFrom");
				rs.updateString("location", location.icao);				
				rs.updateInt("distanceFromHome", (int)Math.round(distanceFromHome[0]));
				
				if (distanceFromHome[0] == 0)
					rs.updateNull("bearingToHome");
				else
					rs.updateInt("bearingToHome", (int)Math.round(distanceFromHome[1]));
				
				if (!invalidFuel)
					aircraft[0].writeFuel(rs);
				
				// if no awaiting assignments and no aircraft hold, or allin at it destination, then release the aircraft lock/rental
				if ((freeAircraft < 1 && !aircraft[0].getHoldRental()) 
					|| (allIn && location.icao.equals(allInAssignmentToIcao)) )
				{
					rs.updateNull("userlock");
					rs.updateNull("lockedSince");
					rs.updateNull("initialFuel");
				} 
				else // otherwise keep the aircraft locked / rented, and update initial fuel
				{
					if (!aircraft[0].wasWetRent() && !invalidFuel)
						rs.updateFloat("initialFuel", (float)aircraft[0].getTotalFuel());
				}
				
				// Update the aircraft record
				rs.updateRow();
				
				rs.close();
				stmt.close();
	
				// Update any damage conditions
				//////////////////////////////////////////////////////
				// Currently only Heat, and Mixture > 95% above 1000ft is checked in client
				for (int c=0; c< damage.length; c++)
					addAircraftDamage(aircraft[0].getRegistration(), damage[c][0], damage[c][1], damage[c][2]);
				
				for (int c=1; c<= aircraft[0].getEngines(); c++)
					addAircraftDamage(aircraft[0].getRegistration(), c, AircraftMaintenanceBean.DAMAGE_RUNTIME, engineTime);
				
				// Add log entry
				///////////////////////////////////////////////////
				
				// Get a blank record, and move to insert
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);			
				rs = stmt.executeQuery("SELECT * from log where 1=2");
				rs.moveToInsertRow();
				
				rs.updateTimestamp("time",new Timestamp(System.currentTimeMillis()));
				rs.updateString("user", user.getName());
				rs.updateString("aircraft", aircraft[0].getRegistration());
				rs.updateString("from", aircraft[0].getDepartedFrom());
				rs.updateString("to", location.icao);
				rs.updateString("type", "flight");
				rs.updateInt("totalEngineTime", totalEngineTime);
				rs.updateFloat("fuelCost", fuelCost);
				rs.updateFloat("rentalCost", rentalCost);
				rs.updateFloat("income", income);
				rs.updateFloat("landingCost", 0); // not used		
				rs.updateFloat("crewCost", crewCost);
				rs.updateFloat("rentalPrice", price);
				rs.updateString("rentalType", aircraft[0].wasWetRent() ? "wet" : "dry"); 
				rs.updateInt("flightEngineTime", engineTime);
				rs.updateInt("flightEngineTicks", engineTicks);
				rs.updateInt("accounting", aircraft[0].getAccounting());
				rs.updateInt("distance", flightDistance);
				rs.updateInt("night", night);
				rs.updateInt("pilotFee", totalPilotFee);
				rs.updateFloat("envFactor", envFactor);
				rs.updateFloat("fboAssignmentFee", fboAssignmentFee);
				rs.updateFloat("mpttax", mpttax);
				
				if (!Double.isNaN(bonus))
					rs.updateFloat("bonus", bonus);
				
				if (groupFlight)
					rs.updateInt("groupId", groupId);
				
				// insert the new log entry
				rs.insertRow();
			
				dalHelper.tryClose(rs);
				dalHelper.tryClose(stmt);

				// increment flight ops entry
				///////////////////////////////////////////////////
				
				// Get a blank record, and move to insert
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);		
				
				Calendar cal = Calendar.getInstance();
				int year = cal.get(Calendar.YEAR);
				int month = cal.get(Calendar.MONTH) + 1; //0 based instead of 1
				String fromICAO = aircraft[0].getDepartedFrom();
				String toICAO = location.icao;
				
				rs = stmt.executeQuery("SELECT * from flightops where opyear=" + year + " AND opmonth=" + month + " AND icao='" + fromICAO + "'");				
				if(!rs.next())
				{
					rs.moveToInsertRow();
					rs.updateShort("opyear", (short)year);					
					rs.updateShort("opmonth", (short)month);					
					rs.updateString("icao", fromICAO);					
					rs.updateShort("ops", (short)1);
					
					// update entry
					rs.insertRow();
				}
				else
				{
					int currops = rs.getShort("ops");
					currops++;
					rs.updateShort("ops", (short)currops);

					// update entry
					rs.updateRow();
				}

				//Close for next icao
				dalHelper.tryClose(rs);
				
				rs = stmt.executeQuery("SELECT * from flightops where opyear=" + year + " AND opmonth=" + month + " AND icao='" + toICAO + "'");				
				if(!rs.next())
				{
					rs.moveToInsertRow();
					rs.updateShort("opyear", (short)year);					
					rs.updateShort("opmonth", (short)month);					
					rs.updateString("icao", toICAO);					
					rs.updateShort("ops", (short)1);
					
					// update entry
					rs.insertRow();
				}
				else
				{
					int currops = rs.getShort("ops") + 1;
					rs.updateShort("ops", (short)currops);

					// update entry
					rs.updateRow();
				}
			}
			finally
			{
				dalHelper.tryClose(rs);
				dalHelper.tryClose(stmt);
				dalHelper.tryClose(conn);
			}
			return freeAircraft;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}
		
		return -1;	
	}

    public void logTemplateAssignment(AssignmentBean assignment, int payee)
    {
        try
        {
            String qry = "INSERT INTO templatelog (created, expires, templateid, fromicao, toicao, pay, payee) VALUES (?,?,?,?,?,?,?)";
            dalHelper.ExecuteUpdate(qry, assignment.getCreation(), assignment.getExpires(),assignment.getFromTemplate(), assignment.getFrom(), assignment.getTo(), assignment.calcPay(), payee);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public boolean aircraftOk(AircraftBean bean, String aircraft)
	{
		boolean result = false;
		try
		{
			Timestamp now = new Timestamp(GregorianCalendar.getInstance().getTime().getTime());
			
			String qry = "UPDATE fsmappings SET lastused = ? WHERE fsaircraft=? AND model = ?";
			int numrecs = dalHelper.ExecuteUpdate(qry, now, aircraft, bean.getModelId());
			
			if(numrecs == 1)
				result = true;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		return result;
	}
	
	public Object[] departAircraft(AircraftBean bean, int user, String location) throws DataError
	{
		ArrayList<AssignmentBean> result = new ArrayList<AssignmentBean>();
		int totalWeight = 0;
		double fuelWeight = 0;
		boolean rentedDry = false;
		ModelBean[] Models = getModelById(bean.modelId);
		boolean allInFlight = false;

		try
		{			
			int seats=bean.getSeats()-1;
			int crewWeight = bean.getCrew();

			// subtract first officer seat
			if (bean.getCrew() > 0)
				seats -=1;
			
			// * seats subtracts weight of pilot (77) + any addt'l crew members
			totalWeight = 77 + (77 * crewWeight);
			
			//Get our avaliable payload
			double weightLeft = bean.getMaxWeight() - bean.getEmptyWeight();
			
			// Add total fuel weight
			fuelWeight  += bean.getTotalFuel() * Data.GALLONS_TO_KG;
			
			//This should be added to total weight!!
			weightLeft -= totalWeight + fuelWeight;
			
			Timestamp now = new Timestamp(GregorianCalendar.getInstance().getTime().getTime());
			bean.setLockedSince(now);
			
			String qry = "SELECT * FROM aircraft WHERE registration = ? AND location = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, bean.registration, location);
			if (!rs.next())
				throw new DataError("No active aircraft found");
						
			if(hasAllInJobInQueue(user))
				allInFlight = true;

			qry = "SELECT (initialFuel is not null) AS rentedDry FROM aircraft WHERE registration = ?";
			rentedDry = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), bean.registration);

			if (bean.getCanFlyAssignments(Models[0]))
			{
				int onBoard = 0;
				qry = "SELECT * FROM assignments WHERE (active = 1 OR (location = ? AND active <> 2)) AND userlock = ? ORDER BY active DESC";
				rs = dalHelper.ExecuteReadOnlyQuery(qry, location, user);
				while (rs.next())
				{
					int passengers, weight;
					if (rs.getString("units").equals("passengers"))
					{
						passengers = rs.getInt("amount");
						weight = passengers * 77;
					} 
					else
					{
						passengers = 0;
						weight = rs.getInt("amount");
					}
				
					if (passengers <= seats && weight <= weightLeft)
					{
						seats-=passengers;
						weightLeft -= weight;
						totalWeight += weight;
						
						AssignmentBean abean = new AssignmentBean(rs);
						abean.setActive(ASSIGNMENT_ENROUTE);
						result.add(abean);
						
						qry = "UPDATE assignments SET active = ? where id = ?";
						dalHelper.ExecuteUpdate(qry, ASSIGNMENT_ENROUTE, rs.getInt("id"));
						onBoard++;
					}
				}
				
				if(allInFlight && onBoard != 1)
					throw new DataError("All-In assignment not loaded. Cannot start the flight.");
			}
			qry = "UPDATE aircraft SET departedFrom = ?, lockedSince = ?, location = null where registration = ?";
			dalHelper.ExecuteUpdate(qry, location, now, bean.registration);				
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 
		
		return new Object[] 
                   {
                        Math.round(totalWeight),
                        (int) Math.round(totalWeight + fuelWeight + bean.getEmptyWeight()),
                        result.toArray(new AssignmentBean[result.size()]),
                        rentedDry
                   };
	}
	
	public Map<String, Integer> getMyFlightInfo(AircraftBean bean, int user) throws DataError
	{
		Map<String, Integer> result = new HashMap<String, Integer>();
		String location = bean.getLocation();
		try
		{
			int seats=bean.getSeats()-1;
			int crewWeight = bean.getCrew();
			if (bean.getCrew() > 0)  					// subtract first officer seat
				seats -=1;
			
			int totalWeight = 77 + (77 * crewWeight);  	// * seats subtracts weight of pilot (77) + any addt'l crew members
			double weightLeft = bean.maxPayloadWeight();
			ModelBean[] Models = getModelById(bean.modelId);
			int group = -1;
			int passengerCount = 0;
			
			if (bean.getCanFlyAssignments(Models[0]))
			{
				String qry = "SELECT * FROM assignments WHERE userlock = ? AND (active =1 OR location=?) ORDER BY active DESC";
				ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user, location);
				while (rs.next())
				{
					int passengers, weight, active;
					active = rs.getInt("active");
					if (rs.getString("units").equals("passengers") && active < 2)
					{ // system or pt passenger assignment
						passengers = rs.getInt("amount");
						weight = passengers * 77;
					} 
					else
					{ // cargo assignment
						passengers = 0;
						weight = rs.getInt("amount");
					}
									
					if (passengers <= seats && weight <= weightLeft && active < 2)
					{   
						seats-=passengers;
						weightLeft -= weight;
						totalWeight += weight;
						passengerCount += passengers;
						result.put((new Integer(rs.getInt("id"))).toString(), null);
						result.put("hasAssignment", null);
						
						if (rs.getString("groupId") != null && group == -1)
							group = rs.getInt("groupId");
					}
				}
			}
			result.put("weight", new Integer(totalWeight));	
			result.put("passengers", new Integer(passengerCount));
			
			if (group != -1)
				result.put("group", new Integer(group));
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result;
	}
	
	public void reloadMoney(UserBean bean)
	{
		try
		{
			String qry = "SELECT money, bank FROM accounts WHERE id = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, bean.getId());
			if (rs.next())
			{
				bean.setMoney(rs.getDouble("money"));
				bean.setBank(rs.getDouble("bank"));
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void reloadMemberships(UserBean user)
	{
		LinkedHashMap<Integer, groupMemberData> memberships = new LinkedHashMap<Integer, groupMemberData>();
		try
		{
			boolean hasItems = false;
			
			String qry = "SELECT * FROM groupmembership, accounts WHERE groupId = accounts.id AND userId = ? order by accounts.name";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user.getId());
			while (rs.next())
			{
				int groupId = rs.getInt("groupId");
				int groupLevel =  UserBean.getGroupLevel(rs.getString("level"));
				
				memberships.put(groupId, new groupMemberData(groupId, groupLevel, rs.getString("name")));

				hasItems = true;
			}
			
			if (!hasItems)
				memberships = null;
			
			user.setMemberships(memberships);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void updateFuelPrice(int bucket, double price)
	{
		try
		{
			String qry = "SELECT (count(*) > 0) AS found FROM fuel WHERE bucket = ?";
			boolean exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), bucket);
			if(exists)
			{
				qry = "UPDATE fuel SET price = ? WHERE bucket = ?";
				dalHelper.ExecuteUpdate(qry, price, bucket);
			}
			else
			{
				qry = "INSERT INTO fuel (price) VALUES(?) WHERE bucket = ?";
				dalHelper.ExecuteUpdate(qry, price, bucket);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	double getFuelPrice(String icao)
	{
		double result = currFuelPrice;

		try
		{
			String qry = "SELECT price FROM fuel, airports WHERE airports.bucket = fuel.bucket AND icao=?";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), icao);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	/**
	 * for getting the jeta price multiplier
	 * @return double
	 */
	public double getJetaMultiplier()
	{
		return currJetAMultiplier;
	}
	
	float getLandingFee(String icao, Connection conn)
	{
		float result = 0.0f;
		Statement stmt = null;
		ResultSet rs = null;		
		try
		{
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery("SELECT price, fixed FROM landingfees WHERE icao='" + icao + "' OR icao='" + icao.substring(0,3) + "' OR icao='" + icao.substring(0,2) + "' OR icao='" + icao.substring(0,1) + "' ORDER BY char_length(icao) DESC LIMIT 1" );
			if (rs.next())
			{
				result = rs.getFloat(1);
				if (rs.getInt(2) != 1)
				{
					rs.close();
					rs = stmt.executeQuery("SELECT size FROM airports WHERE icao='" + icao + "'");
					if (rs.next())
					{
						int size = rs.getInt(1);
						result *= (size/3000.0);
					}
				}
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
		}		

		return result;
	}	
	
	public void defuelAircraft(AircraftBean aircraft, int userid, int amount) throws DataError
	{
		UserBean user = getAccountById(userid);
		reloadMemberships(user);
		ModelBean[] mb = getModelById( aircraft.getModelId());
		if (!aircraft.changeAllowed(user) && mb[0].getFuelSystemOnly()!= 1)
			throw new DataError("Only the owner or group staff may defuel.");
		
		if (amount < 0)
			amount = 0;
		
		if (amount >= aircraft.getTotalFuel())
			return;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			aircraft.emptyAllFuel();
			aircraft.addFuel(amount);
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration='" + aircraft.getRegistration() + "' AND userlock=" + user.getId());
			if (rs.next())
			{
				aircraft.writeFuel(rs);
				rs.updateRow();
			}
		
			rs.close();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}
	}
	
	public void refuelAircraft(String reg, int user, int amount, int provider, int type) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		AircraftBean[] aircraft = getAircraftByRegistration(reg);
		UserBean pilot = getAccountById(user);
		String location;
		
		if (aircraft.length == 0)
			throw new DataError("Aircraft not found.");
		
		location = aircraft[0].getLocation();
		if (location == null)
			throw new DataError("Cannot refuel while aircraft is in the air.");
		
		if (aircraft[0].getUserLock() != user)
			throw new DataError("Permission denied.");
		
		if (provider == -2) 
		{
			defuelAircraft(aircraft[0], user, amount);
			return;
		}
		
		if (aircraft[0].getTotalCapacity() <= aircraft[0].getTotalFuel())
			throw new DataError("Aircraft is already filled up.");
		
		double fuelBefore = aircraft[0].getTotalFuel();
		if (fuelBefore >= amount)
			return;
		
		int capacity = aircraft[0].getTotalCapacity();
		if (amount > capacity)
			amount = capacity;
			
		aircraft[0].addFuel(amount);
					
		FboBean fbo = null;
		double added = amount - fuelBefore;
		int kg = (int)Math.floor(GALLONS_TO_KG * added);
		int fboId = -1;
		if (provider > 0)							// Refuel from FBO
		{
			fbo = getFbo(provider);
			fboId = fbo.getId();
			GoodsBean fuel = this.getGoods(location, fbo.getOwner(), GoodsBean.GOODS_FUEL100LL);
			if (type > 0)
				fuel = this.getGoods(location, fbo.getOwner(), GoodsBean.GOODS_FUELJETA);
			
			if (fuel == null || fuel.getAmount() < kg)
				throw new DataError("Not enough fuel available.");
		} 
		else if (provider == -1)					// Refuel from private drums
		{
			GoodsBean fuel = this.getGoods(location, user, GoodsBean.GOODS_FUEL100LL);
			if (type > 0)
				fuel = this.getGoods(location, user, GoodsBean.GOODS_FUELJETA);
			
			if (fuel == null || fuel.getAmount() < kg)
				throw new DataError("Not enough fuel available.");
		}
		
		try
		{
			conn = dalHelper.getConnection();

			double fuelPrice;
			switch (provider)
			{
				case -1: 
					fuelPrice = 0.0; 
					break;
				case  0: 
					fuelPrice = getFuelPrice(location);
					if (type > 0)
						fuelPrice = getFuelPrice(location) * getJetaMultiplier();
					break;
			 	default: 
			 		if (aircraft[0].getOwner() > 0) // System aircraft always pay bucket rate
			 		{
			 			fuelPrice = fbo.getFuelByType(type);
			 		} 
			 		else 
			 		{
			 			fuelPrice = getFuelPrice(location);
			 			if (type > 0)
			 				fuelPrice = getFuelPrice(location) * getJetaMultiplier();
			 		}
			 		break;
			}
			
			float cost = (float)(added * fuelPrice);			

			//Owner have enough cash to pay for fuel?
			UserBean account = getAccountById(aircraft[0].getOwner());
			if( cost > account.getMoney())
				throw new DataError("Aircraft owner does not have enough cash money to purchase fuel amount requested");
			
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs= stmt.executeQuery("SELECT * FROM aircraft WHERE registration='" + reg + "' AND userlock=" + user);
			if (rs.next())
			{
				aircraft[0].writeFuel(rs);
				rs.updateRow();
			}
			rs.close();
			rs = null;
			
			if (provider > 0)
			{
				if (type < 1)
					this.changeGoodsRecord(location, GoodsBean.GOODS_FUEL100LL, fbo.getOwner(), -kg, false);
				else
					this.changeGoodsRecord(location, GoodsBean.GOODS_FUELJETA, fbo.getOwner(), -kg, false);
			} 
			else if (provider == -1)
			{
				if (type < 1)
					this.changeGoodsRecord(location, GoodsBean.GOODS_FUEL100LL, user, -kg, false);
				else
					this.changeGoodsRecord(location, GoodsBean.GOODS_FUELJETA, user, -kg, false);
			}

			if (cost > 0)
			{
				rs = stmt.executeQuery("SELECT * from log where 1=2");
				rs.moveToInsertRow();
				rs.updateTimestamp("time",new Timestamp(System.currentTimeMillis()));			
				rs.updateString("aircraft", reg);
				rs.updateString("user", pilot.getName());
				rs.updateString("type", "refuel");
				rs.updateFloat("fuelCost", cost);
				rs.updateInt("fbo", provider);	
				rs.insertRow();
				rs.last();
				int logId = rs.getInt("id");
				rs.close();
				rs = null;
				
				String comment = "User ID: " + user + " Amount (gals): " + Formatters.oneDecimal.format(added) + ", $ per Gal: " + Formatters.currency.format(fuelPrice);
				if (type < 1)
				{
					doPayment(aircraft[0].getOwner(), fbo == null ? 0 : fbo.getOwner(), cost, PaymentBean.REASON_REFUEL, logId, fboId, location, reg, comment, false);
				} 
				else 
				{
					doPayment(aircraft[0].getOwner(), fbo == null ? 0 : fbo.getOwner(), cost, PaymentBean.REASON_REFUEL_JETA, logId, fboId, location, reg, comment, false);
				}				
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public void cancelFlight(UserBean user)
	{
		try
		{
			String qry = "UPDATE assignments SET active = 0 WHERE active = 1 AND userlock = ?";
			dalHelper.ExecuteUpdate(qry, user.getId());
			
			qry = "SELECT * from aircraft WHERE userlock = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user.getId());
			if (rs.next())
			{	
				String location = "";
				if(rs.getString("location") != null)
				{
					location = rs.getString("location");
				}
				else if(rs.getString("departedFrom") == null)
				{
					System.err.println("Data error for aircraft " + rs.getString("registration") + ": location = null and departedFrom = null, reverting to home");
					location = rs.getString("home");
				}
				else
				{					 
					location = rs.getString("departedFrom");
				}
				
				qry = "UPDATE aircraft SET location = ?, departedFrom = NULL WHERE registration = ?";
				dalHelper.ExecuteUpdate(qry, location, rs.getString("registration"));
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void buyAircraft(String aircraft, int account, UserBean user) throws DataError
	{
		if (user.getId() != account && user.groupMemberLevel(account) < UserBean.GROUP_STAFF)
			throw new DataError("Permission denied");
		
		try
		{
			String qry = "SELECT * from aircraft WHERE sellPrice > 0 AND registration = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, aircraft);
			if (rs.next())
			{
				int sellPrice = rs.getInt("sellPrice");
				int oldOwner = rs.getInt("owner");
				String location = rs.getString("location");
				
				if (!checkFunds(account, sellPrice))
					throw new DataError("Not enough money to buy aircraft");
				
				qry = "UPDATE aircraft SET owner = ?, sellPrice = null, marketTimeout = null where registration = ?";
				dalHelper.ExecuteUpdate(qry, account, aircraft);
				
				doPayment(account, oldOwner, sellPrice, PaymentBean.AIRCRAFT_SALE, 0, -1, location, aircraft, "", false);				
			} 
			else 
			{
				throw new DataError("Aircraft not found");
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void sellAircraft(String aircraft, UserBean user) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = dalHelper.getConnection();

			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * from aircraft WHERE registration = '" + aircraft + "'");
			if (rs.next())
			{
				AircraftBean[] ac = getAircraftByRegistration(aircraft);
				ModelBean[] Models = getModelById(ac[0].modelId);
				
				if (ac.length == 0)
					throw new DataError("Aircraft not found.");
				
				if (!ac[0].changeAllowed(user))
					throw new DataError("Not your aircraft.");
				
				if (ac[0].isBroken())
					throw new DataError("The Bank of FSE does not buy broken aircraft.");
				
				int sellPrice = ac[0].getMinimumPrice();
				int oldOwner = rs.getInt("owner");
				String location = rs.getString("location");
				rs.updateInt("owner", 0);
				rs.updateNull("lessor");
				rs.updateInt("advertise", 0); 
				rs.updateNull("sellPrice");
				rs.updateNull("bonus");
				rs.updateNull("maxRentTime");
				rs.updateNull("accounting");
				
				// Randomize rent
				ModelBean[] model = getModelById(rs.getInt("model"));
				int equipment = rs.getInt("equipment");
				int rent = model[0].getTotalRentalTarget(equipment);
				rent *= 1+(Math.random()*0.40) - 0.2;					
				rs.updateInt("RentalDry", rent);
				String home = rs.getString("home");
				int fuelCost = (int)Math.round(model[0].getGph() * getFuelPrice(home));
				rs.updateInt("RentalWet", fuelCost + rent);
				rs.updateRow();
				rs.close();
				rs = null;

				doPayment(0, oldOwner, sellPrice, PaymentBean.AIRCRAFT_SALE, 0, -1, location, aircraft, "", false);				
			} 
			else
			{
				throw new DataError("Aircraft not found.");
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}			
	}
	
	public void addAircraftDamage(String aircraft, int engine, int parameter, int value)
	{
		if (value == 0)
			return;
			
		try
		{
			String qry = "SELECT (count(aircraft) > 0) AS found from damage WHERE aircraft = ? AND engine = ? AND parameter = ?";
			boolean exists = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft, engine, parameter);
			if (exists)
			{
				qry = "UPDATE damage SET value = value + ? WHERE aircraft = ? AND engine = ? and parameter = ?";
				dalHelper.ExecuteUpdate(qry, value, aircraft, engine, parameter);
			}
			else
			{
				qry = "INSERT INTO damage (aircraft, engine, parameter, value) VALUES(?,?,?,?)";
				dalHelper.ExecuteUpdate(qry, aircraft, engine, parameter, value);
			}
 		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void updateAircraft(AircraftBean aircraft, String newRegistration, UserBean user) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = dalHelper.getConnection();

			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * from aircraft WHERE registration = '" + aircraft.getRegistration() + "'");
			if (!rs.next())
				throw new DataError("No aircraft found.");
			
			if (rs.getInt("userlock") > 0)
				throw new DataError("Aircraft is rented.");
			
			aircraft.setLocation(rs.getString("location"));
			aircraft.setOwner(rs.getInt("owner"));
			aircraft.setLessor(rs.getInt("lessor")); 	//Added by Airboss 5/8/11
			aircraft.setEquipment(rs.getInt("equipment"));
			
			if (!aircraft.changeAllowed(user))
				throw new DataError("Permission denied");
			
			if (newRegistration!= null && getAircraftByRegistration(newRegistration).length > 0)
				throw new DataError("Registration already in use.");	
			
			double[] distanceFromHome = getDistanceBearing(aircraft.getLocation(), aircraft.getHome());
			aircraft.setDistance((int)Math.round(distanceFromHome[0]));
			aircraft.setBearing((int)Math.round(distanceFromHome[1]));	
			aircraft.writeBean(rs);
			if (newRegistration != null)
			{
				newRegistration = newRegistration.trim();
				PreparedStatement logUpdate = conn.prepareStatement("UPDATE log SET aircraft = ? WHERE aircraft = ?");
				logUpdate.setString(1, newRegistration);
				logUpdate.setString(2, aircraft.registration);
				logUpdate.execute();
				logUpdate.close();
				PreparedStatement paymentsUpdate = conn.prepareStatement("UPDATE payments SET aircraft = ? WHERE aircraft = ?");
				paymentsUpdate.setString(1, newRegistration);
				paymentsUpdate.setString(2, aircraft.registration);
				paymentsUpdate.execute();
				paymentsUpdate.close();
				PreparedStatement damageUpdate = conn.prepareStatement("UPDATE damage SET aircraft = ? WHERE aircraft = ?");
				damageUpdate.setString(1, newRegistration);
				damageUpdate.setString(2, aircraft.registration);
				damageUpdate.execute();
				damageUpdate.close();
				
				rs.updateString("registration", newRegistration);
			}
			rs.updateRow();
			rs.close();
			rs = null;						
				
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}			
	}
	
	/**
	 * Aircraft shipping
	 * @param reg - Aircraft registration to finalize shipment 
	 * @param resetdepart - indicates if the aircraft should returned to its departure location
	 * @param deleteassignment - Indicates if we should also make sure the assignment is removed
	 * Airboss 12/26/10
	 **/
	public void finalizeAircraftShipment(String reg, boolean resetdepart, boolean deleteassignment)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			conn = dalHelper.getConnection();			
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

			//***********
			//Remember, aircraft is BEFORE any changes that follow, as we are in a transaction!!
			//***********
			AircraftBean[] aircraft = getAircraftShippingInfoByRegistration(reg);
			
			rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration='" + reg + "'");
			
			if(rs.next())
			{
				Date shippingNext = new Date( new Date().getTime() + (aircraft[0].getShippingStateDelay()*1000)); 
				Timestamp ts = new Timestamp(shippingNext.getTime());
				rs.updateTimestamp("shippingStateNext", ts );
				
				rs.updateInt("shippingState", 3);
				
				//if not admin reset to departure, set the aircraft to its shipped to location
				if( !resetdepart)
					rs.updateString("location", aircraft[0].getShippingTo());	
				
				rs.updateRow();
				rs.close();
			}
			rs = stmt.executeQuery("SELECT * FROM log WHERE type='maintenance' and subtype=" + AircraftMaintenanceBean.MAINT_SHIPMENTREASSEMBLY + " and aircraft='" + reg + "' order by id desc");
			
			if(rs.next())
			{
				rs.updateTimestamp("time", new Timestamp(System.currentTimeMillis()));
				rs.updateRow();
				rs.close();
			}
			
			stmt.close();
			
			//if we are calling from admin page, check if we need to remove any assigments
			if( deleteassignment )
			{
				//For completed Assignments, remove them
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				stmt.executeUpdate("DELETE from assignments WHERE commodityid=99 AND commodity like '%" + aircraft[0].getRegistration() + "%'");
				stmt.close();
			}			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(rs);
			dalHelper.tryClose(conn);
		}			
	}
	
	public void updateModel(ModelBean model, UserBean user) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			boolean newEntry;
			conn = dalHelper.getConnection();

			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * from models WHERE id = " + model.getId());
			if (!rs.next())
			{
				newEntry = true;
				rs.moveToInsertRow();
			} 
			else
			{
				newEntry = false;
			}
			
			model.writeBean(rs);
			if (newEntry)
				rs.insertRow();
			else
				rs.updateRow();						
				
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}			
	}
	
	public void updateTemplate(TemplateBean template, UserBean user) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			boolean newEntry;
			conn = dalHelper.getConnection();

			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * from templates WHERE id = " + template.getId());
			if (!rs.next())
			{
				newEntry = true;
				rs.moveToInsertRow();
			} 
			else 
			{
				newEntry = false;
			}
			
			template.writeBean(rs);
			if (newEntry)
				rs.insertRow();
			else
				rs.updateRow();						
				
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}			
	}
	
	public int findDistance(String from, String to)
	{
		int returnval = 0;
		
		if (from != null && to != null)
		{	
			double distanceBearing[] = getDistanceBearing(from, to);
			returnval = (int)Math.round(distanceBearing[0]);
		}
		
		return returnval;
	}
	
	public boolean checkGoodsAvailable(String icao, int userId, int commodityId, int amount) throws DataError
	{
		boolean result = false;
		
		if (icao != null)
		{
			try
			{
				int currentAmount = 0;
				String qry = "SELECT * FROM goods WHERE location = ? AND owner = ? AND type = ?";
				ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, icao, userId, commodityId);				
				if (rs.next())
				{ 
					currentAmount = rs.getInt("amount");
					if (currentAmount >= amount)
						result = true;
				}					
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	public void checkMoneyAvailable( UserBean user, double cost) throws DataError
	{
		if (user != null)
		{
			try
			{
				if (cost != 0 && !checkAnyFunds(user.getId(), cost))
					throw new DataError("Not enough money for paying this assignment. ");					
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			} 
		}
		else
		{
			throw new DataError("No user provided");
		}
	}
	
	public void updateAssignment(AssignmentBean assignment, UserBean user) throws DataError
	{
		Boolean localconn = false;
		Connection conn = null;
		Statement stmt = null, check = null;
		ResultSet rs = null, checkrs = null;
		
		if (assignment.getOwner() != user.getId() &&
			user.groupMemberLevel(assignment.getOwner()) < UserBean.GROUP_STAFF &&
			user.groupMemberLevel(assignment.getGroupId()) < UserBean.GROUP_STAFF)
		{
			throw new DataError("Permission denied");
		}
		
		try
		{
			boolean newEntry;
			
			if( conn == null)
			{
				localconn = true;
				conn = dalHelper.getConnection();
			}
			
			assignment.updateData(this);
			
			if (assignment.getActive() == 1)
				throw new DataError("The assignment is in flight");
			
			if (assignment.isGoods() && assignment.getAmount() < 1)
				throw new DataError("Transfer assignments must have a quantity greater than zero");
			
			if (assignment.isCreatedByUser() && assignment.calcPay() < 0)
				throw new DataError("Assignment pay may not be less than zero");
			
			if (assignment.getPilotFee() < 0)
				throw new DataError("Pilot fee may not be less than zero");
			
			if (assignment.isGroup() && !checkAnyFunds(assignment.groupId, assignment.getPilotFee()))
				throw new DataError("Not enough money for paying this pilot fee.");
			
			if (assignment.calcPay() != 0 && assignment.isCreatedByUser() && !checkAnyFunds(assignment.owner, assignment.calcPay()))
				throw new DataError("Not enough money for paying this assignment. ");
			
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * from assignments WHERE id = " + assignment.getId());
			int oldAmount = 0;
			if (!rs.next())
			{
				newEntry = true;
				rs.moveToInsertRow();
				oldAmount = 0;
			} 
			else
			{
				newEntry = false;
				oldAmount = rs.getInt("amount");
			} 
			int diffAmount = assignment.getAmount() - oldAmount;
			
			//added for aircraft shipping - Airboss 1/10/11
			if (diffAmount != 0 && assignment.getCommodityId() > 0 && assignment.getCommodityId() < 99) //ignore aircraft crate
				changeGoodsRecord(assignment.getLocation(), assignment.getCommodityId(), assignment.getOwner(), -diffAmount, false);
					
			assignment.writeBean(rs);
			
			if (newEntry)
				rs.insertRow();
			else
				rs.updateRow();				
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(checkrs);
			dalHelper.tryClose(check);
			
			if(localconn)
				dalHelper.tryClose(conn);
		}			
	}		
	
	public void doBanking(int user, double value)
	{
		try
		{
			String sValue = Formatters.twoDecimals.format(value);

			//I want to use these but can't because of how the rounding is being done in a string
			//I don't want to introduce a difference in how the banking is done yet.
			//String qry = "UPDATE accounts SET bank = ROUND(bank + ?, 2) WHERE id = ?";
			//qry = "UPDATE accounts SET money = ROUND(money - ?, 2) WHERE id = ?";
			
			String qry = "UPDATE accounts SET bank = ROUND(bank + " + sValue + ", 2) WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, user);

			qry = "UPDATE accounts SET money = ROUND(money - " + sValue + ", 2) WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, user);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void doPayGroup(int user, int group, float value)
	{
		doPayment(user, group, value, PaymentBean.GROUP_PAYMENT, 0, -1, "", "", "", false);
	}
	
	public void doPayGroup(int user, int group, float value, String comment)
	{
		doPayment(user, group, value, PaymentBean.GROUP_PAYMENT, 0, -1, "", "", comment, false);
	}	
	
	public void doPayBulkFuel(int user, int group, float value, int fbo, String comment, String icao, int type)
	{
		doPayment(user, group, value, (type==3) ? PaymentBean.SALE_GOODS_FUEL : PaymentBean.SALE_GOODS_JETA, 0, fbo, icao, "", comment, false);
	}
	
	public void doPayBulkFuelDelivered(int user, int group, float value, int fbo, String comment, String icao)
	{
		doPayment(user, group, value, PaymentBean.BULK_FUEL, 0, fbo, icao, "", comment, false);
	}
	
	public void doInvitation(UserBean user, int group, boolean accept) throws DataError
	{
		mustBeLoggedIn(user);
		try
		{
			String qry = "";
			
			if (accept)
				qry = "UPDATE groupmembership SET level = 'member' where level = 'invited' AND userId = ? AND groupId = ?";
			else
				qry = "DELETE from groupmembership WHERE level = 'invited' AND userId = ? AND groupId = ?";
			
			dalHelper.ExecuteUpdate(qry, user.getId(), group);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		reloadMemberships(user);			
	}	
	
	public void joinGroup(UserBean user, int group, String level) throws DataError
	{
		mustBeLoggedIn(user);
		
		try
		{
			String qry = "SELECT (count(userId) > 0) as found FROM groupmembership WHERE userId = ? AND groupId = ?";
			boolean found = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user.getId(), group);
			if (!found)
			{
				qry = "INSERT INTO groupmembership (userId, groupId, level) VALUES (?, ?, ?)";
				dalHelper.ExecuteUpdate(qry, user.getId(), group, level);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		reloadMemberships(user);			
	}
	
	public void changeMembership(int user, int group, String level) throws DataError
	{
		try
		{
			String qry = "UPDATE groupmembership SET level = ? WHERE userId = ? AND groupId = ?";
			dalHelper.ExecuteUpdate(qry, level, user, group);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void cancelGroup(UserBean user, int group) throws DataError
	{
		try
		{
			String qry = "DELETE FROM groupmembership WHERE userId = ? AND groupId = ?";
			dalHelper.ExecuteUpdate(qry, user.getId(), group);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		reloadMemberships(user);		
	}
	
	public void deleteGroup(UserBean user, int group) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		mustBeLoggedIn(user);
		try
		{
			conn = dalHelper.getConnection();

			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT money + bank, bank, name FROM accounts WHERE type = 'group' AND id = " + group);
			if (!rs.next())
				throw new DataError("Group not found.");
	
			float groupMoney = rs.getFloat(1);
			String groupName = rs.getString(3);
			if (groupMoney < 0)
				throw new DataError("This group has a negative bank balance.");
			
			// Remove system assignments from group assignments list
			stmt.executeUpdate("UPDATE assignments SET groupId = null, pilotFee = 0, comment = null WHERE fromTemplate is not null and groupId = " + group);
			
			// Remove assignments owned by others from group assignments list
			stmt.executeUpdate("UPDATE assignments SET groupId = null, pilotFee = 0, comment = null WHERE owner <> groupid and groupId = " + group);
			
			// Delete Ferry assignments. They can not be transfered to an individual.
			stmt.executeUpdate("DELETE FROM assignments WHERE fromTemplate is null and (commodityId is null or commodityId = 0) and groupId = " + group);
			
			// Change ownership of transfer assignments to group owner
			stmt.executeUpdate("UPDATE assignments SET owner = " + user.getId() + " WHERE owner = " + group);
			
			// Change ownership of leases to group owner - 8-26-12 Airboss
			stmt.executeUpdate("UPDATE aircraft SET lessor = " + user.getId() + " WHERE lessor = " + group);

			// No assignments should remain now where owner or groupid equals group
			
			
			// Transfer ownership of group owned aircraft
			stmt.executeUpdate("UPDATE aircraft SET owner = " + user.getId() + " WHERE owner = " + group);
			
			// Transfer ownership of group owned FBOs
			stmt.executeUpdate("UPDATE fbo SET owner = " + user.getId() + " WHERE owner = " + group);
			
			// Transfer ownership of FBO facilities
			stmt.executeUpdate("update fbofacilities set occupant = " + user.getId() + " where occupant = " + group);
					
			// Transfer ownership of group owned goods
			GoodsBean[] goods = this.getGoodsForAccountAvailable(group);
            for (GoodsBean good : goods)
            {
                String location = good.getLocation();
                int type = good.getType();
                int amount = good.getAmount();
                changeGoodsRecord(location, type, group, -amount, false);
                changeGoodsRecord(location, type, user.getId(), amount, false);
            }
			
			// Delete group owned goods records (all zero amount now)
			stmt.executeUpdate("DELETE FROM goods WHERE owner = " + group);
			
			// The group should have no property now			
			
			// Transfer funds
			doPayment(group, user.getId(), groupMoney, PaymentBean.GROUP_DELETION, 0, -1, "", "", groupName, false);
						
			// Delete group membership
			stmt.executeUpdate("DELETE FROM groupmembership WHERE groupId = " + group);
			
			// Delete the group account
			stmt.executeUpdate("UPDATE accounts SET exposure = 0, comment = '" + user.getName() + "'  WHERE type='group' AND id = " + group);		
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}
		
		reloadMemberships(user);
	}
	
	public void flyForGroup(UserBean user, int group) throws DataError
	{
		UserBean[] groups = getGroupById(group);		
		if (groups.length == 0)
			throw new DataError("Group not found.");
	
		try
		{
			if (user.groupMemberLevel(group) < UserBean.GROUP_MEMBER)
				throw new DataError("Permission denied.");
				
			String qry = "UPDATE assignments SET pilotFee=pay*amount*distance*0.01*?, groupId = ? WHERE userlock = ? and active <> 2";
			dalHelper.ExecuteUpdate(qry, groups[0].getDefaultPilotFee()/100.0, group, user.getId());			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void payMembers(UserBean user, int group, double money, String[] members, String comment) throws DataError
	{
		UserBean[] groups = getGroupById(group);
		
		if (groups.length == 0)
			throw new DataError("Group not found.");
	
		if (user.groupMemberLevel(group) < UserBean.GROUP_OWNER)
			throw new DataError("Permission denied.");
		
		if (groups[0].getMoney() < money)
			throw new DataError("Group does not have enough money.");
			
		Money topay = new Money(money);
		topay.divide(members.length);
		
		for (int c = 0; c < members.length; c++)
			doPayment(group, Integer.parseInt(members[c]), topay, PaymentBean.GROUP_PAYMENT, 0, -1, "", "", comment, false);			
	}
	
	public void mailMembers(UserBean user, int group, String[] members, String text) throws DataError
	{
		UserBean[] groups = getGroupById(group);
		
		if (groups.length == 0)
			throw new DataError("Group not found.");
		
		if (user.groupMemberLevel(group) < UserBean.GROUP_OWNER)
			throw new DataError("Permission denied.");
			
		try
		{
			StringBuffer list = new StringBuffer("(");
			for (int c = 0; c < members.length; c++)
			{
				if (c > 0)
					list.append(", ");
				list.append(members[c]);
			}
			list.append(")");
			
			List<String> toList = new ArrayList<String>();
			String qry = "SELECT email FROM accounts, groupmembership WHERE accounts.id = groupmembership.userId AND groupId = ? AND accounts.id IN " + list.toString();
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, group);			
			while (rs.next()) //add recipients to receive this message
			{
				toList.add(rs.getString(1));
			}
			
			String messageText = "This message is sent to you by the administrator of the FSEconomy flight group \"" + groups[0].getName() + "\".\n----------\n"  + text;
			
			Emailer emailer = Emailer.getInstance();
			emailer.sendEmail("no-reply@fseconomy.net", "FS Economy flight group "  + groups[0].getName(),
					"FSEconomy Group Message", messageText, toList, Emailer.ADDRESS_BCC);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	public void doMaintenance(AircraftBean aircraft, int maintenanceType, UserBean user, FboBean fbo) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		Statement damageStmt = null;
		Statement maintenanceStmt = null;
		ResultSet damage = null;
		ResultSet maintenance = null;
		
		// Get a Default FBO if none is specified
		if (fbo == null)
		{
			String location = aircraft.getLocation();
			if (location == null)
				return;
			AirportBean airport = this.getAirport(aircraft.getLocation());
			FboBean[] fbos = this.getFboForRepair(airport);
			if (fbos.length == 0)
				return;
			
			//Sort by owner, bank will be first and should be available
			Arrays.sort(fbos, (o1, o2) -> Integer.compare(o2.getOwner(), o1.getOwner()));

			fbo = fbos[0];
		}
			
		try
		{
			int logId;
			
			conn = dalHelper.getConnection();
			
			// First call for the maintenance price	
			int price = aircraft.getMaintenancePrice(maintenanceType, fbo);
			int repairmargin = fbo.getRepairShopMargin();
			
			// Prevent system planes from being repaired when FBO Repair Margin  > system default FBO Repair Margin.
			if (aircraft.getOwner() == 0 && repairmargin > FboBean.FBO_DEFAULT_REPAIRSHOPMARGIN)
				return;
			
			if (aircraft.getOwner() > 0)
			{
				UserBean owner = this.getAccountById(aircraft.getOwner());
			
				if (owner != null && owner.getMoney() < price)
					throw new DataError("Not enough money.");
			}							
		   
			//To close possible maintenace exploit 
			if (!aircraft.getLocation().equals(fbo.getLocation()))
		    	throw new DataError("The Aircraft Location and the FBO Location are not the same.");
		    
			damageStmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			damage = damageStmt.executeQuery("SELECT * FROM damage WHERE aircraft = '" + aircraft.getRegistration() + "'");

			maintenanceStmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			maintenance = maintenanceStmt.executeQuery("SELECT * FROM maintenance");
			
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				
			rs = stmt.executeQuery("SELECT * FROM log where 1=2");
			rs.moveToInsertRow();
			rs.updateTimestamp("time", new Timestamp(System.currentTimeMillis()));
			rs.updateString("type", "maintenance");
			rs.updateString("aircraft", aircraft.getRegistration());
			rs.updateInt("totalEngineTime", aircraft.getTotalEngineTime());
			rs.updateInt("subType", maintenanceType);
			rs.updateInt("fbo", fbo.getId());
			rs.insertRow();
			rs.last();
			logId = rs.getInt("id");
			rs.close();		
					
			rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration = '" + aircraft.getRegistration() + "'");

			if (!rs.next())
				throw new DataError("Aircraft not found.");	
			
			// Second call for the maintenance price - what was actually done
			price = aircraft.performMaintenance(maintenanceType, logId, fbo, rs, damage, maintenance);
			double factor = 1 + fbo.getRepairShopMargin() / 100.0;
			
			// First call for additional cost during 100-hour
			int[] conditionPrice = aircraft.getConditionPrice(aircraft, maintenanceType);
	        int addedPrice=0;
	        int condition =0;
			if (maintenanceType != AircraftMaintenanceBean.MAINT_FIXAIRCRAFT) 
			{
		        for (int i = 0; i < conditionPrice.length; i++) 
		        {

		              addedPrice += conditionPrice[i];
		        }
				price += (addedPrice * factor);
			} 
			else  //Emergency Aircraft Repair between $1000-5000, reset condition
			{				
				do //reset condition so its not in the 'broken' range
				{
					condition = (int) (50000 + (59999 - 50000)* Math.random());
				}while(condition > AircraftBean.REPAIR_RANGE_LOW-1 && condition < AircraftBean.REPAIR_RANGE_HIGH+1);
				rs.updateInt("condition", condition);
				rs.updateInt("lastFix", rs.getInt("airframe"));
			}
			rs.updateRow();
			
			stmt.executeUpdate("UPDATE log SET maintenanceCost = " + price + ", ageECost = " + conditionPrice[0] + ", ageAvCost = " + conditionPrice[1] + ", ageAfCost = " + conditionPrice[2] + ", ageAdwCost = " + conditionPrice[3] + " WHERE id = " + logId);
			doPayment(aircraft.getOwner(), fbo.getOwner(), price, PaymentBean.MAINTENANCE, logId, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);
			
			if (maintenanceType != AircraftMaintenanceBean.MAINT_FIXAIRCRAFT)
				doPayment(fbo.getOwner(), 0, (float)(price / factor), PaymentBean.MAINTENANCE_FBO_COST, logId, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);		
		
			rs.close();			
	
			// clear engine damage and hours if new engine 
			if(maintenanceType == AircraftMaintenanceBean.MAINT_REPLACEENGINE)
			{
				String sQuery = "UPDATE damage SET value = 0 WHERE aircraft = '" + aircraft.getRegistration() + "' and 'engine' < 3";
				stmt.executeUpdate(sQuery);
			}			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(damage);
			dalHelper.tryClose(damageStmt);
			dalHelper.tryClose(maintenance);
			dalHelper.tryClose(maintenanceStmt);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}

	public void doMaintenanceAircraftShipment(AircraftBean aircraft, int maintenanceType, UserBean user, boolean istofbo, FboBean fbo, double shippingcost) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		Statement maintenanceStmt = null;

		try
		{
			int logId;
			
			conn = dalHelper.getConnection();

			// First call for the maintenance price
            maintenanceStmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				
			rs = stmt.executeQuery("SELECT * FROM log where 1=2");
			rs.moveToInsertRow();
			rs.updateTimestamp("time", new Timestamp(System.currentTimeMillis()));
			rs.updateString("user", user.getName());
			rs.updateString("type", "maintenance");
			rs.updateString("aircraft", aircraft.getRegistration());
			rs.updateInt("subType", maintenanceType);
			rs.updateInt("totalEngineTime", aircraft.getTotalEngineTime());
			rs.updateInt("fbo", fbo.getId());
			rs.insertRow();
			rs.last();
			logId = rs.getInt("id");
			rs.close();		
					
			double factor = 1 + fbo.getRepairShopMargin() / 100.0;

			stmt.executeUpdate("UPDATE log SET maintenanceCost = " + shippingcost + ", ageECost = " + 0 + ", ageAvCost = " + 0 + ", ageAfCost = " + 0 + ", ageAdwCost = " + 0 + " WHERE id = " + logId);
			
			String comment;
			if(maintenanceType == AircraftMaintenanceBean.MAINT_SHIPMENTDISASSEMBLY)
				comment = "Aircraft shipment disassembly.";
			else
				comment = "Aircraft shipment reassembly.";
			
			String PayLocation;
			if(istofbo)
				PayLocation = fbo.getLocation();
			else
				PayLocation = aircraft.getLocation();
			
			doPayment(aircraft.getOwner(), fbo.getOwner(), shippingcost, PaymentBean.MAINTENANCE, logId, fbo.getId(), PayLocation, aircraft.getRegistration(), comment, false);
	
			if (maintenanceType != AircraftMaintenanceBean.MAINT_FIXAIRCRAFT)
				doPayment(fbo.getOwner(), 0, (float)(shippingcost / factor), PaymentBean.MAINTENANCE_FBO_COST, logId, fbo.getId(), PayLocation, aircraft.getRegistration(), comment, false);
		
			rs.close();			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(maintenanceStmt);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public AircraftMaintenanceBean getMaintenance(int logId) throws DataError
	{
		try
		{	
			LogBean log[] = getLogById(logId);
			
			if (log.length == 0)
				throw new DataError("Maintenance record not found.");
			
			AircraftBean aircraft[] = this.getAircraftByRegistration(log[0].getAircraft());
			if (aircraft.length == 0)
				throw new DataError("Aircraft not found.");
			
			FboBean fbo = log[0].getFbo() == 0 ? FboBean.getInstance() : this.getFbo(log[0].getFbo());
			
			if (fbo == null)
				throw new DataError("Repairshop not found.");
							
			String qry = "SELECT * FROM maintenance WHERE log = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, logId);
			
			return new AircraftMaintenanceBean(rs, aircraft[0], log[0].maintenanceCost, fbo, log[0].ageECost, log[0].ageAvCost, log[0].ageAfCost, log[0].ageAdwCost);			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return null;
	}
	
	public InputStream getInvoiceBackground(int fbo)
	{
		InputStream returnValue = null;
		try
		{
			String qry = "SELECT invoice FROM fbo WHERE id = ?";
			Blob image;
			image = dalHelper.ExecuteScalarBlob(qry, fbo);
			if (image != null)
				returnValue = image.getBinaryStream();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return returnValue;
	}
	
	public void updateInvoiceBackground(FboBean fbo, InputStream data, int length, UserBean user) throws DataError
	{
		if (!fbo.updateAllowed(user))
			throw new DataError("Permission denied.");
		
		try
		{
			String qry = "SELECT invoice, id FROM fbo WHERE id = ?";
			if(!dalHelper.ExecuteUpdateBlob(qry, "invoice", data, length, fbo.getId()))
				throw new DataError("Update to invoice failed!");
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}		
	
	public void doEquipment(AircraftBean aircraft, int equipmentType, FboBean fbo) throws DataError
	{
		// Get a default FBO if none is specified.
		if (fbo == null)
		{
			String location = aircraft.getLocation();
			if (location == null)
				return;
			AirportBean airport = this.getAirport(aircraft.getLocation());
			FboBean[] fbos = this.getFboForRepair(airport);
			if (fbos.length == 0)
				return;
			Arrays.sort(fbos, (o1, o2) -> {
                int v1 = o1.getEquipmentInstallMargin();
                int v2 = o2.getEquipmentInstallMargin();
                return v1 < v2 ? -1 : v1 > v2 ? 1 : 0;
            });
			fbo = fbos[0];
		}
		
		try
		{						
			int price = aircraft.getEquipmentPriceFBO(equipmentType, fbo);			
			UserBean owner = getAccountById(aircraft.getOwner());	
			
			if (owner == null)
				throw new DataError("Owner not found.");
			
			if (owner.getMoney() < price)
				throw new DataError("Not enough money.");					

			String qry = "update aircraft set equipment = equipment|? where registration = ?";
			if(dalHelper.ExecuteUpdate(qry, equipmentType, aircraft.getRegistration()) != 1)
				throw new DataError("Aircraft not found.");
			
			double factor = 1+ fbo.getEquipmentInstallMargin() / 100.0;

			doPayment(aircraft.getOwner(), fbo.getOwner(), price, PaymentBean.EQUIPMENT, 0, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);
			doPayment(fbo.getOwner(), 0, (float)(price/factor), PaymentBean.EQUIPMENT_FBO_COST, 0, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	public static int hourEquipmentPrice(int equipment)
	{
		int returnValue = 0;
		
		if ((equipment & ModelBean.EQUIPMENT_IFR_MASK) != 0)
			returnValue += ModelBean.IFR_COST_HOUR;
		
		if ((equipment & ModelBean.EQUIPMENT_GPS_MASK) != 0)
			returnValue += ModelBean.GPS_COST_HOUR;
		
		if ((equipment & ModelBean.EQUIPMENT_AP_MASK) != 0)
			returnValue += ModelBean.AP_COST_HOUR;
		
		return returnValue;
	}
	
	public static boolean needLevel(UserBean user, int level)
	{
		return (user.getLevel() == level);	
	}
	
	public AircraftAlias[] getAircraftAliasesOld()
	{
		ArrayList<AircraftAlias> result = new ArrayList<AircraftAlias>();
		String qry;
		ResultSet rs;
		
		try
		{
			qry = "SELECT fsaircraft, models.make, models.model FROM fsmappings, models WHERE fsmappings.model = models.id ORDER BY models.make, models.model, fsaircraft";			
			rs = dalHelper.ExecuteReadOnlyQuery(qry);
			
			while (rs.next())
			{
				AircraftAlias aircraft = new AircraftAlias(rs.getString(1), rs.getString(2) + " " + rs.getString(3));
				result.add(aircraft);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}		
		
		return result.toArray(new AircraftAlias[result.size()]);
	}
	

	public Map<String, Set<String>> getAircraftAliases()
	{
		Map<String, Set<String>> result = new TreeMap<String, Set<String>>();
		String qry;
		ResultSet rs = null;
		
		try
		{
			qry = "SELECT models.make, models.model, models.id FROM models ORDER BY models.make, models.model";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);			
			while (rs.next())
			{
				Set<String> aliases = new HashSet<String>();
				result.put(rs.getString(1)+" "+rs.getString(2), aliases);
			
				qry = "SELECT fsaircraft FROM fsmappings WHERE  model=? ORDER BY fsaircraft";
				ResultSet rsAlias = dalHelper.ExecuteReadOnlyQuery(qry, rs.getInt(3));			
				while (rsAlias.next())
				{
					aliases.add(rsAlias.getString(1));
				}
			}			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}		
		
		return result;
	}
	
	public static class MakeModel
	{
		public String MakeName;
		public Model[] Models;
	}
	
	public static class ModelAliases
	{
		public String MakeModel;
		public String[] Aliases;
	}
	
	public static class Model
	{
		public int Id;
		public String ModelName;
	}
	
	public MakeModel[] getMakeModels()
	{
		String qry;
		ResultSet rs;
		ArrayList<MakeModel> makemodels = new ArrayList<MakeModel>();
		
		try
		{
			
			qry = "SELECT DISTINCT models.make FROM models ORDER BY models.make";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);			
			while (rs.next())
			{
				MakeModel mm = new MakeModel();
				mm.MakeName = rs.getString(1);
			
				makemodels.add(mm);
			}			

			for(MakeModel item : makemodels)
			{
				ArrayList<Model> models = new ArrayList<>();
				qry = "SELECT models.id, models.model FROM models where models.make = ? ORDER BY models.make, models.model";
				rs = dalHelper.ExecuteReadOnlyQuery(qry, item.MakeName);			
				while (rs.next())
				{
					Model m = new Model();
					m.Id = rs.getInt(1);
					m.ModelName = rs.getString(2);
				
					models.add(m);
				}
				item.Models = models.toArray(new Model[models.size()]);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}		
		
		return makemodels.toArray(new MakeModel[makemodels.size()]);
	}
	
	public ModelAliases getModelAliases(int modelId)
	{
		String qry;
		ResultSet rs = null;
		ModelAliases modelaliases = new ModelAliases();
		
		try
		{			
			qry = "SELECT models.make, models.model FROM models where id=? ORDER BY models.make";
			rs = dalHelper.ExecuteReadOnlyQuery(qry, modelId);			
			if(!rs.next())
			{
				Data.logger.error("getModelAliases() unable to find modelId: " + modelId);
				return new ModelAliases();
			}			
			modelaliases.MakeModel = rs.getString(1) + " " + rs.getString(2);
			
			ArrayList<String> aliases = new ArrayList<>();
			qry = "SELECT fsaircraft FROM fsmappings where model = ? ORDER BY fsaircraft;";
			rs = dalHelper.ExecuteReadOnlyQuery(qry, modelId);			
			while (rs.next())
			{
				aliases.add(rs.getString(1));
			}
			modelaliases.Aliases = aliases.toArray(new String[aliases.size()]);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}		
		
		return modelaliases;
	}
	
	public aircraftConfigs getAircraftConfigs(int modelid)
	{
		ResultSet rs;
		aircraftConfigs aircraft = null;
		try
		{
			String qry = "SELECT make, model, crew, fueltype, seats, cruisespeed, " +
								"fcapExt1, fcapLeftTip, fcapLeftAux, fcapLeftMain, " +
								"fcapCenter, fcapCenter2, fcapCenter3, fcapRightMain, " +
								"fcapRightAux, fcapRightTip, fcapExt2, " +
								"gph, maxWeight, emptyWeight, price, engines, engineprice, canShip, fcaptotal " +
								"FROM models WHERE id=? ORDER BY make, model";
			
			rs = dalHelper.ExecuteReadOnlyQuery(qry, modelid);
			if(rs.next())
			{
				aircraft = new aircraftConfigs
							(
								rs.getString(1) + " " + rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),rs.getInt(6), 
								rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10),
								rs.getInt(11), rs.getInt(12), rs.getInt(13), rs.getInt(14),
								rs.getInt(15), rs.getInt(16), rs.getInt(17), 
								rs.getInt(18), rs.getInt(19), rs.getInt(20), rs.getInt(21), rs.getInt(22), rs.getInt(23), rs.getBoolean(24), (int)rs.getDouble(25)
							);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}	
		
		return aircraft;
	}
	
	public aircraftConfigs[] getAircraftConfigs()
	{
		ArrayList<aircraftConfigs> result = new ArrayList<>();
		ResultSet rs;
		
		try
		{
			String qry = "SELECT make, model, crew, fueltype, seats, cruisespeed, " +
								"fcapExt1, fcapLeftTip, fcapLeftAux, fcapLeftMain, " +
								"fcapCenter, fcapCenter2, fcapCenter3, fcapRightMain, " +
								"fcapRightAux, fcapRightTip, fcapExt2, " +
								"gph, maxWeight, emptyWeight, price, engines, engineprice, canShip, fcaptotal " +
								"FROM models ORDER BY make, model";
			
			rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				aircraftConfigs aircraft = new aircraftConfigs
											(
												rs.getString(1) + " " + rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),rs.getInt(6), 
												rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10),
												rs.getInt(11), rs.getInt(12), rs.getInt(13), rs.getInt(14),
												rs.getInt(15), rs.getInt(16), rs.getInt(17), 
												rs.getInt(18), rs.getInt(19), rs.getInt(20), rs.getInt(21), rs.getInt(22), rs.getInt(23), rs.getBoolean(24), (int)rs.getDouble(25)
											);
				result.add(aircraft);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}	
		
		return result.toArray(new aircraftConfigs[result.size()]);
	}
	
	public GoodsBean[] getGoodsAtAirportToSell(String icao, int type, int size, double fuelPrice, double JetAPrice)
	{
		return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " + 
			"goods, commodities, accounts WHERE ( goods.owner = 0 or exists (select * from fbo WHERE fbo.location = goods.location AND fbo.owner = goods.owner AND " +
			" fbo.active = 1 AND (saleFlag&" + GoodsBean.SALEFLAG_BUY + ") > 0)) AND goods.owner = accounts.id AND goods.type = commodities.id AND goods.type = " + type +
			" AND (max = 0 OR max > amount) AND goods.location = '" + icao + "'",
			icao, type, size, fuelPrice, JetAPrice);
	}
	
	public GoodsBean[] getGoodsForFbo(String icao, int owner)
	{
		return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " + 
			"goods, commodities, accounts WHERE goods.owner = accounts.id AND goods.type = commodities.id AND owner = " + 
			owner + " AND location = '" + icao + "'",	icao, 0, -1, 0, 0);
	}
	
	public GoodsBean[] getGoodsAtAirportGMap(String icao, int size, double fuelPrice, double JetAPrice)
	{
		return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " +
			"goods, commodities, accounts WHERE (goods.owner = 0 OR exists (select * from fbo WHERE fbo.location = goods.location AND fbo.owner = goods.owner AND " +
			"fbo.active = 1  AND saleFlag > 0)) AND commodities.id between 1 and 2 AND " +  //only showing BMs and Supplies after fuel everywhere update - changed by airboss - 8/15/12
			"goods.location = '" + icao + "' AND goods.owner = accounts.id", icao, 0, size, fuelPrice, JetAPrice);
	}
	
	public GoodsBean[] getGoodsAtAirport(String icao, int size, double fuelPrice, double JetAPrice)
	{
		return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " +
			"goods, commodities, accounts WHERE (goods.owner = 0 OR exists (select * from fbo WHERE fbo.location = goods.location AND fbo.owner = goods.owner AND " +
			"fbo.active = 1  AND saleFlag > 0)) AND goods.type = commodities.id AND " +
			"goods.location = '" + icao + "' AND goods.owner = accounts.id", icao, 0, size, fuelPrice, JetAPrice);
	}
	
	public GoodsBean[] getGoodsAtAirportSQL(String SQL, String icao, int type, int size, double fuelPrice, double JetAPrice)
	{		
		GoodsBean[] returnValue = getGoodsSQL(SQL);
		if (commodities != null && size > 0)
		{
			ArrayList<GoodsBean> result = new ArrayList<GoodsBean>();
			int amount[] = new int[commodities.length + 1];
			for (int c=0; c < returnValue.length; c++)
			{
				if (returnValue[c].owner == 0)
					amount[returnValue[c].getType()]+=returnValue[c].amount;
				else
					result.add(returnValue[c]);
			}		
			
			for (int c = 0; c < commodities.length; c++)
				if (commodities[c] != null && size > commodities[c].getMinAirportSize() && (type == 0 || c == type))
					result.add(new GoodsBean(commodities[c], icao, size, fuelPrice, amount[commodities[c].getId()], JetAPrice));
			
			returnValue = result.toArray(new GoodsBean[result.size()]);
		}
		return returnValue;
	}
	
	public GoodsBean[] getGoodsForAccountAvailable(int id)
	{
		return getGoodsSQL("SELECT goods.*, commodities.name, accounts.name FROM goods, commodities, accounts WHERE goods.owner = accounts.id AND goods.type = commodities.id AND amount > 0 AND owner=" + id);
	}
	
	public GoodsBean getGoods(String location, int owner, int type)
	{
		GoodsBean[] returnValue = getGoodsSQL("SELECT goods.*, commodities.name, accounts.name FROM goods, commodities, accounts WHERE goods.owner = accounts.id AND goods.type = commodities.id AND goods.type = " + type + " AND goods.owner = " + owner + " AND goods.location = '" + location + "'");

        return returnValue.length == 0 ? null : returnValue[0];
	}

	GoodsBean[] getGoodsSQL(String qry)
	{
		ArrayList<GoodsBean> result = new ArrayList<GoodsBean>();
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				GoodsBean goods = new GoodsBean(rs);
				result.add(goods);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result.toArray(new GoodsBean[result.size()]);
	}
	
	public int getGoodsQty(FboBean fbo, int type)
	{
		GoodsBean goods = getGoods(fbo.getLocation(), fbo.getOwner(), type);

        return goods != null ? goods.getAmount() : 0;
	}
	
	public int getGoodsQty(String location, int owner, int type)
	{
		GoodsBean goods = getGoods(location, owner, type);

        return goods != null ? goods.getAmount() : 0;
	}
	
	public void buildRepairShop(FboBean fbo) throws DataError
	{
		if ((fbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0)
			throw new DataError("Repairshop already built.");
		
		GoodsBean goods = getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
		if (goods == null || goods.getAmount() < GoodsBean.CONSTRUCT_REPAIRSHOP)
			throw new DataError("Not enough building materials available.");
		
		try
		{
			changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_REPAIRSHOP, false);		

			String qry = "UPDATE fbo SET services = services | ?, margin = 20, equipmentmargin = 50 where id = ?";
			dalHelper.ExecuteUpdate(qry, FboBean.FBO_REPAIRSHOP, fbo.getId());
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public boolean hasSuppliesForSale(String icao)
	{
		boolean result = false;
		
		try
		{
			String qry = "SELECT COUNT(goods.owner) > 0 as supplies FROM goods LEFT JOIN airports ON goods.location = airports.icao WHERE goods.location = ?  AND goods.type between 1 AND 4 AND goods.saleFlag > 0 AND (goods.amount - cast(goods.retain as signed int) > 0)";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(),icao);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result;
	}
	
	public boolean hasRepairShop(String icao)
	{
		boolean result = false;
		
		try
		{
			String qry = "SELECT count(id) > 0 AS repairshop FROM fbo WHERE (services & 1)  > 0 AND location = ?";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(),icao);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result;
	}
	
	public boolean buildPassengerTerminal(FboBean fbo) throws DataError
	{
		if (fbo.getName().length() > 45)
			throw new DataError("FBO Name cannot exceed 45 characters.");
		
		if ((fbo.getServices() & FboBean.FBO_PASSENGERTERMINAL) > 0)
			throw new DataError("Passenger terminal already built.");
		
		GoodsBean goods = getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
		if (goods == null || goods.getAmount() < GoodsBean.CONSTRUCT_PASSENGERTERMINAL)
			throw new DataError("Not enough building materials available.");
		
		try
		{
			changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_PASSENGERTERMINAL, false);			

			String qry = "UPDATE fbo SET services = services | ? WHERE owner = ? AND location = ?";
			dalHelper.ExecuteUpdate(qry, FboBean.FBO_PASSENGERTERMINAL, fbo.getOwner(), fbo.getLocation());
			
			AirportBean airport = getAirport(fbo.getLocation());
			
			qry = "INSERT INTO fbofacilities (location, fboId, occupant, reservedSpace, size, rent, name, units, commodity, maxDistance, matchMaxSize, publicByDefault) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
			dalHelper.ExecuteUpdate(qry, 
					fbo.getLocation(), fbo.getId(), fbo.getOwner(), fbo.getFboSize() * airport.getFboSlots(), 
					0, FboFacilityBean.DEFAULT_RENT, fbo.getName(), AssignmentBean.UNIT_PASSENGERS, 
					FboFacilityBean.DEFAULT_COMMODITYNAME_PASSENGERS, 300, 99999, 1);

			return true;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return false;
	}
	
	public void rentFboFacility(UserBean user, int occupantId, int facilityId, int blocks) throws DataError
	{
		if (blocks < 1)
			throw new DataError("No gates selected to rent.");
		
		try
		{
			if ((user.getId() != occupantId) && (user.groupMemberLevel(occupantId) < UserBean.GROUP_STAFF))
				throw new DataError("Permission denied.");
			
			int existingFacilityId = -1;
			
			FboFacilityBean landlord = getFboFacility(facilityId);
			if (!landlord.getIsDefault())
			{
				existingFacilityId = landlord.getId();
				landlord = getFboDefaultFacility(landlord.getFboId());
			}
			
			FboBean fbo = getFbo(landlord.getFboId());
			AirportBean airport = getAirport(landlord.getLocation());
			
			if (blocks > calcFboFacilitySpaceAvailable(landlord, fbo, airport))
				throw new DataError("Not enough space available.");
			
			Calendar paymentDate = GregorianCalendar.getInstance();
			int daysInMonth = paymentDate.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
			int daysLeftInMonth = daysInMonth - paymentDate.get(GregorianCalendar.DAY_OF_MONTH) + 1;
			int rent = Math.round(landlord.getRent() * ((float)daysLeftInMonth / (float)daysInMonth)) * blocks;

			if ((occupantId != landlord.getOccupant()) && (!checkFunds(occupantId, (double)rent)))
				throw new DataError("Not enough money to pay first month rent. $" + rent + ".00 needed.");
			
			if (existingFacilityId != -1)
			{
				String qry = "UPDATE fbofacilities SET size = size + ?, lastRentPayment = ? WHERE id = ?";
				dalHelper.ExecuteUpdate(qry, blocks, new Timestamp(paymentDate.getTime().getTime()), existingFacilityId);
			} 
			else 
			{
				String qry = "INSERT INTO fbofacilities (location, fboId, occupant, size, name, units, commodity, maxDistance, matchMaxSize, publicByDefault, lastRentPayment) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
				dalHelper.ExecuteUpdate(qry, 
						fbo.getLocation(), fbo.getId(), occupantId, blocks, "Rented Facility", landlord.getUnits(),
						FboFacilityBean.DEFAULT_COMMODITYNAME_PASSENGERS, 300, 99999, 1, new Timestamp(paymentDate.getTime().getTime()));
			}
			
			doPayment(occupantId, landlord.getOccupant(), (double)rent, PaymentBean.FBO_FACILITY_RENT, 0, fbo.getId(), fbo.getLocation(), "", "", false);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	synchronized void changeGoodsRecord(String location, int type, int owner, int amount, boolean allowNegativeGoods ) throws DataError
	{
		try
		{
			boolean recordExists = true;
			int currentAmount = 0;

			String qry = "SELECT amount FROM goods WHERE location = ? AND owner = ? AND type = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, location, owner, type);
			
			if (!rs.next())
				recordExists = false;
			else 
				currentAmount = rs.getInt(1);
			
			if (owner > 0 && amount < 0 && (currentAmount + amount) < 0 && !allowNegativeGoods)
				throw new DataError("Not enough goods available.");
			
			int newAmount = currentAmount + amount;
			
			if (recordExists)
			{
				qry = "UPDATE goods set amount = ? WHERE location = ? AND owner = ? AND `type` = ?";
				dalHelper.ExecuteUpdate(qry, newAmount, location, owner, type);
			}
			else
			{
				qry = "INSERT INTO goods (location, owner, `type`, amount) VALUES(?,?,?,?)";
				dalHelper.ExecuteUpdate(qry, location, owner, type, newAmount);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public double quoteGoods(String location, int type, int amount, int src, boolean buying)
	{
		try
		{
			if (cachedAPs.get(location.toUpperCase()) == null)
				throw new Exception("Unknown airport.");

			// Step 1: If otherParty = 0, price is not fixed, Calculate the price
			double kgPrice;
			if (src == 0)
			{
				int overstock = 0;				
				int airportSize = cachedAPs.get(location.toUpperCase()).size;				
				double fuelPrice = getFuelPrice(location);
				double mult = getJetaMultiplier();
				double JetAPrice = fuelPrice * mult;
				
				kgPrice = commodities[type].getKgSalePrice(amount, airportSize, fuelPrice, overstock, JetAPrice);
			} 
			else
			{
				String qry;
				if(buying)
					qry = "SELECT sellprice FROM goods WHERE owner=? and type=? and location=?";
				else
					qry = "SELECT buyprice FROM goods WHERE owner=? and type=? and location=?";
				
				ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, src, type, location);
				if(!rs.next())
					throw new Exception("No price found.");
				
				kgPrice = rs.getDouble(1);
			}
			
			return kgPrice * amount;
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 
		
		return 0.0;
	}
		
	public double quoteFuel(String location, int type, int amount)
	{
		try
		{
			int overstock = 0;				
			double fuelPrice = getFuelPrice(location);
			int airportSize = cachedAPs.get(location.toUpperCase()).size;				
			double mult = getJetaMultiplier();
			double JetAPrice = fuelPrice * mult;
			
			double kgPrice = commodities[type].getKgSalePrice(amount, airportSize, fuelPrice, overstock, JetAPrice);
			
			return kgPrice * amount;
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 
		return 0.0;
	}
	
	public synchronized void transferGoods(int from, int to, int initiator, String location, int type, int amount) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		//System.out.println("Transfer: from=" + from + ", to=" + to + ", initiator=" + initiator + ", location=" + location + ", type=" + type + ", amount=" + amount);
		if (amount < 1)
			throw new DataError("Not enough goods available.");
		
		try
		{
			int otherParty = initiator == from ? to : from;
			String typeLocation = " AND type=" + type + " AND location='" + location + "'";
			String fromWhere = "owner=" + from + typeLocation;
			String toWhere = "owner=" + to + typeLocation;
			String otherPartyWhere = otherParty == from ? fromWhere : toWhere;
			
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
						
			// Step 1: Check if <from> has enough of <type> at <location>
			// If from == 0, there's always enough			
			rs = stmt.executeQuery("SELECT amount, saleFlag, retain FROM goods WHERE " + fromWhere);
			int available = 0;
			if (rs.next())
			{
				if (from == 0 || from == initiator)
					available = rs.getInt(1);
				else if ((rs.getInt(2) & GoodsBean.SALEFLAG_SELL ) > 0)
					available = rs.getInt(1) - rs.getInt(3);
			}
			if (available < amount && from > 0)
				throw new DataError("Not enough goods available.");
			rs.close();
			
			// Step 2: In case of sale, check if other party wants to receive
			if (to != initiator && to != 0)
			{
				rs = stmt.executeQuery("SELECT saleFlag, max FROM goods WHERE " + toWhere);
				boolean saleOk = false;
				if (rs.next())
				{
					saleOk = (rs.getInt(1) & GoodsBean.SALEFLAG_BUY) > 0;
					if (saleOk && rs.getInt(2) > 0 && amount > (rs.getInt(2) - amount)) // Airboss 8/22/13 - moved subtract amount here to prevent unsigned bigint error in db
						throw new DataError("Other party does not want to accept this quantity.");
				}
				if (!saleOk)
					throw new DataError("Other party does not accept the goods.");
				
				rs.close();
			}
							
			// Step 3: If otherParty = 0, price is not fixed, Calculate the price
			double kgPrice;
			if (otherParty == 0)
			{
				int overstock = 0;
				if (from == 0)
					overstock = available;
				else
				{
					rs = stmt.executeQuery("SELECT amount FROM goods WHERE " + toWhere);
					if (rs.next())
						overstock = rs.getInt(1);
					rs.close();
				}

				LatLonSize lls = cachedAPs.get(location);
				if(lls == null)
					throw new DataError("Unknown airport.");
				
				int airportSize = lls.size;

				double fuelPrice = getFuelPrice(location);
				double mult = getJetaMultiplier();
				double JetAPrice = fuelPrice * mult;
				if (from == 0)
					kgPrice = commodities[type].getKgSalePrice(amount, airportSize, fuelPrice, overstock, JetAPrice);
				else
					kgPrice = commodities[type].getKgBuyPrice(airportSize, fuelPrice, overstock, JetAPrice);
			} 
			else
			{
				rs = stmt.executeQuery("SELECT buyPrice, sellPrice FROM goods WHERE " + otherPartyWhere);
				if (!rs.next())
					throw new DataError("No price found.");
				kgPrice = rs.getDouble(from == initiator ? 1 : 2);
				rs.close();
			}
			
			
			// Step 4: Check if <to> has enough money
			double price = kgPrice * amount;
			
			if (to > 0)
			{
				rs = stmt.executeQuery("SELECT money FROM accounts WHERE id=" + to);
				if (!rs.next())
					throw new DataError("Unknown user.");
				double money = rs.getDouble(1);
				if (money < price)
					throw new DataError("Not enough money to pay for the goods.");

				rs.close();
			}
			
			int fboId = -1;
			
			rs = null;
			// Step 5: perform transfer
			stmt.close();
			stmt = null;
			short logType = type == GoodsBean.GOODS_BUILDING_MATERIALS ? PaymentBean.SALE_GOODS_BUILDING_MATERIALS : type == GoodsBean.GOODS_FUEL100LL ? PaymentBean.SALE_GOODS_FUEL : type == GoodsBean.GOODS_FUELJETA ? PaymentBean.SALE_GOODS_JETA : PaymentBean.SALE_GOODS_SUPPLIES;
			doPayment(to, from, price, logType, 0, fboId, location, "", "", false);
				
			changeGoodsRecord(location, type, from, -amount, false);
			changeGoodsRecord(location, type, to, amount, false);			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public synchronized void transferFBOGoods(int buyer, int seller, String location, int type, int amount) throws DataError
	{
		// The amount to transfer is validated in transfergoods.jsp and does not have to be repeated here			
		changeGoodsRecord(location, type, seller, -amount, false);
		changeGoodsRecord(location, type, buyer, amount, false);		
		
		int fboId = -1;
		short cnvCommodityToTransfer[] = {0, PaymentBean.TRANSFER_GOODS_BUILDING_MATERIALS, PaymentBean.TRANSFER_GOODS_SUPPLIES, PaymentBean.TRANSFER_GOODS_FUEL, PaymentBean.TRANSFER_GOODS_JETA};

		doPayment(buyer, seller, 0, cnvCommodityToTransfer[type], 0, fboId, location, "", amount + " Units", false);
	}
		
	public void deleteAssignment(int id, UserBean user) throws DataError
	{
		try
		{
			String qry = "SELECT * FROM assignments WHERE id = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
			if (!rs.next())
				throw new DataError("Assignment not found.");
			
			AssignmentBean assignment = new AssignmentBean(rs);
			
			if (assignment.getUserlock() > 0 && assignment.getUserlock() != user.getId())
				throw new DataError("Assignment is currently locked by a pilot.");
			
			if (!assignment.deleteAllowed(user))
				throw new DataError("Permission denied.");
			
			if (assignment.isGoods() && assignment.authorityOverGoods(user) && assignment.getCommodityId() < 99)
			{
				changeGoodsRecord(assignment.getLocation(), assignment.getCommodityId(), assignment.getOwner(), assignment.getAmount(), false);

				qry = "DELETE FROM assignments WHERE id = ?";
				dalHelper.ExecuteUpdate(qry, id);
			} 
			else if (assignment.isFerry())
			{
				qry = "DELETE FROM assignments WHERE id = ?";
				dalHelper.ExecuteUpdate(qry, id);
			} 
			else if (assignment.isGroup())
			{
				qry = "UPDATE assignments SET groupId = NULL, pilotFee = NULL, comment = NULL WHERE id = ?";
				dalHelper.ExecuteUpdate(qry, id);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public FboBean[] getFbo()
	{
		return getFboSql("SELECT * from fbo ORDER BY id");
	}

	public FboBean[] getFboByOwner(int owner)
	{
		return getFboSql("SELECT * FROM fbo WHERE owner=" + owner + " ORDER BY id");
	}
	
	public FboBean getFboByID(int fboID)
	{
		return getSingleFboSql("SELECT * FROM fbo WHERE id=" + fboID);
	}
	
	public String getFboNameById(int fboID)
	{
		String retval = null;
		
		try
		{
			String qry = "SELECT name FROM fbo WHERE id = ?";
			retval = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), fboID);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return retval;
	}

	public FboBean[] getFboByOwner(int owner, String sortFieldName)
	{
		return getFboSql("SELECT * FROM fbo WHERE owner=" + owner + " ORDER BY " + sortFieldName);
	}
	
	public FboBean[] getFboByLocation(String location)
	{
		return getFboSql("SELECT * FROM fbo WHERE active = 1 AND location='" + location + "' ORDER BY id");
	}
	
	public FboBean[] getInactiveFboByLocation(String location)
	{
		return getFboSql("SELECT * FROM fbo WHERE active = 0 AND location='" + location + "' ORDER BY id");
	}
	
	public FboBean[] getFboForRepair(AirportBean airport)
	{
		FboBean[] returnValue = getFboSql("SELECT * from fbo WHERE active = 1 AND (services & " + FboBean.FBO_REPAIRSHOP + ") > 0 AND location='" + airport.getIcao() + "' ORDER BY id");
		
		if (airport.size >= AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE)
		{
			List<FboBean> list = new ArrayList<FboBean>(Arrays.asList(returnValue));
			
			FboBean fb = FboBean.getInstance();
			fb.location = airport.getIcao();
			list.add(fb);
			
			returnValue = list.toArray(new FboBean[list.size()]);
		}
		
		return returnValue;
	}
	
	public FboBean[] getFboForSale()
	{
		return getFboSql("SELECT f.* FROM fbo f WHERE f.saleprice > 0 ORDER BY f.saleprice");
	}
	
	public FboBean getFbo(int id)
	{
        if(id == 0)
            return null;

		FboBean[] result = getFboSql("SELECT * FROM fbo WHERE id=" + id);
		return result.length == 0 ? null : result[0];
	}
	
	public FboBean[] getFboSql(String qry)
	{
		ArrayList<FboBean> result = new ArrayList<>();

		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
				result.add(new FboBean(rs));
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result.toArray(new FboBean[result.size()]);
	}
	
	public FboBean getSingleFboSql(String qry)
	{
		FboBean fbo = null;
		
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			if (rs.next())
				fbo = new FboBean(rs);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return fbo;
	}
	
	public FboFacilityBean[] getFboFacilitiesByOccupant(int account)
	{
		return getFboFacilitiesSql("select t.* from fbofacilities t where t.occupant = " + account + " order by location, id");
	}
	
	public FboFacilityBean[] getFboDefaultFacilitiesForAirport(AirportBean airport)
	{
		return getFboFacilitiesForAirport(airport.getIcao());
	}
	
	public FboFacilityBean[] getFboDefaultFacilitiesForAirport(String icao)
	{
		return getFboFacilitiesSql("SELECT * FROM fbofacilities WHERE reservedSpace >= 0 AND location ='" + icao + "' order by id");
	}
	
	public FboFacilityBean[] getFboFacilitiesForAirport(AirportBean airport)
	{
		return getFboFacilitiesForAirport(airport.getIcao());
	}
	
	public FboFacilityBean[] getFboFacilitiesForAirport(String icao)
	{
		return getFboFacilitiesSql("select t.* from fbofacilities t, fbo f where t.fboId = f.id and f.active = 1 and f.location = '" + icao + "' order by id");
	}
	
	public FboFacilityBean getFboDefaultFacility(FboBean fbo)
	{
		return getFboDefaultFacility(fbo.getId());
	}
	
	public FboFacilityBean getFboDefaultFacility(int fboId)
	{
		FboFacilityBean[] result = getFboFacilitiesSql("SELECT * FROM fbofacilities WHERE reservedSpace >= 0 AND fboId=" + fboId);
		return result.length == 0 ? null : result[0];
	}
	
	public FboFacilityBean[] getFboRenterFacilities(FboBean fbo)
	{
		return getFboFacilitiesSql("select * from fbofacilities where reservedSpace < 0 and fboId = " + fbo.getId() + " order by id");
	}
	
	public FboFacilityBean getFboFacility(int id)
	{
		FboFacilityBean[] result = getFboFacilitiesSql("SELECT * FROM fbofacilities WHERE id=" + id);
		return result.length == 0 ? null : result[0];
	}
	
	public FboFacilityBean[] getFboFacilitiesSql(String qry)
	{
		ArrayList<FboFacilityBean> result = new ArrayList<FboFacilityBean>();
		
		try
		{
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
			while (rs.next())
				result.add(new FboFacilityBean(rs));
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result.toArray(new FboFacilityBean[result.size()]);
	}
	
	public int calcFboFacilitySpaceAvailable(FboFacilityBean facility, FboBean fbo, AirportBean airport)
	{
		int spaceInUse = getFboFacilityBlocksInUse(fbo.getId());
		int totalSpace = fbo.getFboSize() * airport.getFboSlots();
		return Math.max(0, totalSpace - spaceInUse - facility.getReservedSpace());
	}
	
	public int getFboFacilityBlocksInUse(int fboId)
	{
		int result = 0;
		try
		{
			String qry = "select sum(size) from fbofacilities where reservedSpace < 0 and fboId = ?";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), fboId);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	// Note: deleteFbo() changed to reduce size by one, and only delete when size reaches 0
	public void deleteFbo(int fboId, UserBean user) throws DataError
	{
		try
		{
			FboBean fbo = getFbo(fboId);
			if (fbo == null)
				throw new DataError("FBO not found.");
			
			if (!fbo.deleteAllowed(user))				
				throw new DataError("Permission denied.");
			
			if (doesBulkFuelRequestExist(fbo.getId()))
				throw new DataError("Cannot teardown an FBO that has an active pending bulk fuel delivery.");
			
			int inUse = getFboFacilityBlocksInUse(fbo.getId());
			AirportBean airport = getAirport(fbo.getLocation());
			int newSpace = (fbo.getFboSize() - 1) * airport.getFboSlots();
			if (inUse > newSpace)
				throw new DataError("An FBO with tennants can not be torn down.");
			
			int recover = fbo.recoverableBuildingMaterials();
			changeGoodsRecord(fbo.location, GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), recover, false);
			
			String qry = "UPDATE fbo SET fbosize = fbosize - 1 WHERE id = " + fbo.getId() + ";";
			qry += "UPDATE fbofacilities set reservedSpace = " + newSpace + " WHERE reservedSpace > " + newSpace + " and fboId = " + fboId + ";";
			qry += "DELETE FROM fbofacilities where fboId IN (select id from fbo where fboSize < 1);";
			qry += "DELETE FROM fbo WHERE fbosize < 1;";
			dalHelper.ExecuteBatchUpdate(qry);
			
			//if fbo deleted remove sell/buy flag from any goods
			qry = "SELECT id FROM fbo where id=?";
			int id = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), fbo.getId());
			if(id == 0)
			{
				resetAllGoodsSellBuyFlag(fbo.getOwner(), fbo.getLocation());
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void upgradeFbo(int fboId, UserBean user) throws DataError
	{
		try
		{
			FboBean fbo = getFbo(fboId);
			if (fbo == null)
				throw new DataError("FBO not found.");
			
			if (!fbo.updateAllowed(user))
				throw new DataError("Permission denied.");
			
			if (getAirportFboSlotsAvailable(fbo.getLocation()) < 1)
				throw new DataError("There is no room for construction at this airport.");
			
			if (!checkGoodsAvailable(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS, GoodsBean.CONSTRUCT_FBO))
				throw new DataError("Not enough building materials available.");
			
			changeGoodsRecord(fbo.location, GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_FBO, false);
			
			String qry = "UPDATE fbo SET fbosize = fbosize + 1 WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, fbo.getId());
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void updateFbo(FboBean fbo, UserBean user) throws DataError
	{
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			if (!fbo.updateAllowed(user))
				throw new DataError("Permission denied.");
				
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			
			rs = stmt.executeQuery("SELECT * FROM fbo WHERE id = " + fbo.getId());
			if (!rs.next())
				throw new DataError("Could not find FBO!");
			
			fbo.writeBean(rs);
			rs.updateRow();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public void createFbo(FboBean fbo, UserBean user) throws DataError
	{
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			if (!fbo.updateAllowed(user))
				throw new DataError("Permission denied.");
				
			String qry = "select" +
					"       case" +
					"        when airports.size < " + AirportBean.MIN_SIZE_MED + " then 1" +
					"        when airports.size < " + AirportBean.MIN_SIZE_BIG + " then 2" +
					"        else 3" +
					"       end - case when ISNULL(fbo.location) then 0 else sum(fbosize) end" +
					"        > 0 as found" +
					"      from airports" +
					"      left outer join fbo on fbo.location = airports.icao" +
					"      where airports.icao='" + fbo.getLocation() + "'" +
					"      group by airports.icao";
			boolean result =  dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer());
			if (!result)
					throw new DataError("No lots open!");

			qry = "SELECT count(*) > 0 as found FROM fbo WHERE owner = ? AND location = ? AND id <> ?";
			result =  dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), fbo.getOwner(), fbo.getLocation(), fbo.getId());
			if (result)
				throw new DataError("You already own an FBO at this location.");

			int availableBM = 0;
			int availableSupplies = 0;
			qry = "SELECT amount FROM goods WHERE type=? AND location=? AND owner=?";
			availableBM = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getLocation(), fbo.getOwner());

			if (availableBM < GoodsBean.CONSTRUCT_FBO)
				throw new DataError("Not enough building materials available.");			

			qry = "SELECT amount FROM goods WHERE type=? AND location=? AND owner=?";
			availableSupplies = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), GoodsBean.GOODS_SUPPLIES, fbo.getLocation(), fbo.getOwner());
			
			if(availableSupplies < 10)
				throw new DataError("Unable to build. The required 1 day of supplies(10kg) are not available.");			
			
			conn = dalHelper.getConnection();	
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM fbo WHERE id = " + fbo.getId());
			
			rs.moveToInsertRow();
			
			rs.updateString("location", fbo.getLocation());
			rs.updateInt("owner", fbo.getOwner());
			rs.updateInt("active", 1);
			fbo.writeBean(rs);

			rs.insertRow();
			changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_FBO, false);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public void deleteFboFacility(UserBean user, int facilityId) throws DataError
	{
		try
		{
			FboFacilityBean facility = getFboFacility(facilityId);
			if (facility == null)
				throw new DataError("Facility not found.");
			
			if (!facility.deleteAllowed(user))
				throw new DataError("Permission denied.");
			
			if (facility.getIsDefault())
				throw new DataError("The default facility can not be removed.");
			
			String qry = "UPDATE fbofacilities SET size = size - 1 WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, facilityId);
			
			qry = "DELETE FROM fbofacilities WHERE reservedspace < 0 and size < 1";
			dalHelper.ExecuteUpdate(qry);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public void updateFboFacility(FboFacilityBean facility, FboFacilityBean[] renters, UserBean user) throws DataError
	{
		if (facility == null)
			return;
		
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{
			if (!facility.updateAllowed(user))
				throw new DataError("Permission denied.");
				
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			
			String icaos = facility.getIcaoSet();
			if (icaos != null && !"".equals(icaos.trim()))
			{
				String items[] = icaos.toUpperCase().trim().split(", *");
				icaos = "";
				if (items.length == 0)
					throw new DataError("ICAO set returned zero items.");  // should not happen
				
				String item;
                for (String item1 : items)
                {
                    item = item1.trim();
                    LatLonSize lls = Data.cachedAPs.get(item);
                    if (lls == null)
                        throw new DataError("ICAO '" + item + "' not found.");

                    if (getDistance(facility.getLocation(), item) > FboFacilityBean.MAX_ASSIGNMENT_DISTANCE)
                        throw new DataError("ICAO '" + item + "' is too far. " + FboFacilityBean.MAX_ASSIGNMENT_DISTANCE + " NM limit in place.");

                    if (icaos.length() == 0)
                        icaos = item;
                    else
                        icaos = icaos + ", " + item;
                }
				
				facility.setIcaoSet(icaos);
			}
			
			rs = stmt.executeQuery("SELECT * FROM fbofacilities WHERE fboId = " + facility.getFboId());
			while (rs.next())
			{
				if (rs.getInt("id") == facility.getId())
				{
					facility.writeBean(rs);
					rs.updateRow();
				} 
				else if (renters != null )
				{
                    for (FboFacilityBean renter : renters)
                    {
                        if (rs.getInt("id") == renter.getId())
                        {
                            rs.updateBoolean("allowRenew", renter.getAllowRenew());
                            rs.updateRow();
                            break;
                        }
                    }
				}
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	public void updateGoods(GoodsBean goods, UserBean user) throws DataError
	{
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try
		{	
			conn = dalHelper.getConnection();
			stmt =	conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM goods WHERE owner=" + goods.getOwner() + " AND location='" + goods.getLocation() + "' AND type=" + goods.getType());
			boolean exists;
			if (rs.next())
			{
				exists = true;
				
			} 
			else
			{
				rs.moveToInsertRow();
				rs.updateString("location", goods.getLocation());
				rs.updateInt("owner", goods.getOwner());
				rs.updateInt("type", goods.getType());
				exists = false;
			}
			
			goods.writeBean(rs);
			
			if (exists)
				rs.updateRow();
			else
				rs.insertRow();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}		
	}
	
	/** 
	 * Probe the database for an aicraft with a specific title. If found, return the nearest aircraft of this type
	 * @param aircraft The title of the aircraft
	 * @param airport closeAirport object
	 * @param airportList The list to fill with aircraft
	 * @param currentAirport A list that will be filled with the airport found at the specified location
	 * @param alternativeAircraft A list that will be filled with the aircraft found at the specified location
	 * @return The name of the model, Null if aircraft title is unknown.
	 */
	public String probeAircraft(String aircraft, closeAirport airport, List<closeAirport> airportList, List<AirportBean> currentAirport, List<AircraftBean> alternativeAircraft)
	{
		String aircraftModelName = null;
		int modelId = -1;
		
		try
		{			
			String qry = "SELECT models.id, models.make, models.model FROM fsmappings, models where models.id = fsmappings.model AND fsaircraft= ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, aircraft);
			if (rs.next())
			{				
				modelId = rs.getInt(1);
				aircraftModelName = rs.getString(2) + " " + rs.getString(3);
			}
						
			AirportBean thisAirport = getAirport(airport.icao);			
			thisAirport.setFuelPrice(getFuelPrice(thisAirport.getIcao()));
			currentAirport.add(thisAirport);
			if (modelId != -1)
			{
				HashMap<String, closeAirport> airportMap = new HashMap<>();
				closeAirport[] closeAirports = fillCloseAirports(airport.icao, 0, 100);
                for (Data.closeAirport closeAirport : closeAirports)
                    airportMap.put(closeAirport.icao.toLowerCase(), closeAirport);

				AircraftBean[] areaAircraft = getAircraftOfTypeInArea(airport.icao, closeAirports, modelId);
				Set<closeAirport> airportSet = new HashSet<>();
                for (AircraftBean anAreaAircraft : areaAircraft)
                {
                    if (anAreaAircraft.getLocation().toLowerCase().equals(thisAirport.getIcao().toLowerCase()))
                    {
                        airportList.add(new closeAirport(thisAirport.getIcao(), 0, 0));
                    }
                    else
                    {
                        closeAirport thisAp = airportMap.get((anAreaAircraft.getLocation().toLowerCase()));

                        if (thisAp != null)
                            airportList.add(thisAp);
                    }
                }
				airportList.addAll(airportSet);				
			}
			
			AircraftBean[] otherAircraft = getAircraft(thisAirport.getIcao());
            for (AircraftBean anOtherAircraft : otherAircraft)
            {
                if (anOtherAircraft.getUserLock() == 0)
                    alternativeAircraft.add(anOtherAircraft);
            }
			
			return aircraftModelName;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return null;
	}
	
	/**
	 * Add a payment record to the payments table
	 * @param user			The originating party
	 * @param otherParty	The other party
	 * @param amount		The amount
	 * @param reason		The reason for payment
	 * @param logEntry		The optional log entry this payment is associated with
	 */
	void addPaymentRecord(int user, int otherParty, Money amount, short reason, long logEntry, int fbo, String location, String aircraft, String comment)
	{
		StringBuilder fields = new StringBuilder();
		StringBuilder values = new StringBuilder();
		try
		{
			fields.append("time, user, otherParty, amount, reason, logEntry");
			values.append("'").append(new Timestamp(System.currentTimeMillis())).append("'");
			values.append(", ").append(user);
			values.append(", ").append(otherParty);
			values.append(", ").append(amount.getAsFloat());
			values.append(", ").append(reason);
			values.append(", ").append(logEntry);
			
			if (fbo >= 0)
			{
				fields.append(", fbo");
				values.append(", ").append(fbo);
			}
			
			if (!"".equals(location))
			{
				fields.append(", location");
				values.append(", '").append(location).append("'");
			}
			
			if (!"".equals(aircraft))
			{
				fields.append(", aircraft");
				values.append(", '").append(Converters.escapeSQL(aircraft)).append("'");
			}
			
			if (!"".equals(comment))
			{
				fields.append(", comment");
				values.append(", '").append(Converters.escapeSQL(comment)).append("'");
			}
			
			String qry = "INSERT INTO payments (" + fields.toString() + ") VALUES(" + values.toString() + ")";
			dalHelper.ExecuteUpdate(qry);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	/**
	 * Perform and log a payment
	 * 
	 * @param user			The originating party
	 * @param otherParty	The other party
	 * @param amount		The amount
	 * @param reason		The reason for payment
	 * @param logEntry		The optional log entry this payment is associated with
	 */
	boolean doPayment(int user, int otherParty, double amount, short reason, long logEntry, int fbo, String location, String aircraft, String comment, boolean blockOnDept)
	{
		return doPayment(user, otherParty, new Money(amount), reason, logEntry, fbo, location, aircraft, comment, blockOnDept);
	}
	
	boolean doPayment(int user, int otherParty, Money amount, short reason, long logEntry, int fbo, String location, String aircraft, String comment, boolean blockOnDept)
	{
		// if any of the following are true, then let the zero payment through, otherwise exit
     	if(		amount.getAsDouble() == 0 && 
     			reason != PaymentBean.FBO_SALE && 
     			reason != PaymentBean.AIRCRAFT_SALE && 
     			reason != PaymentBean.AIRCRAFT_LEASE && 	//Added by Airboss 5/8/11
     			reason != PaymentBean.TRANSFER_GOODS_BUILDING_MATERIALS && 
     			reason != PaymentBean.TRANSFER_GOODS_FUEL && 
     			reason != PaymentBean.TRANSFER_GOODS_JETA && 
     			reason != PaymentBean.TRANSFER_GOODS_SUPPLIES &&
     			reason != PaymentBean.BULK_FUEL )	//added by gurka bulk fuel delivery transfers
     	{
			return true;
     	}
     	
     	// Check if amount is negative, reverse payee/payer
		if (amount.getAsDouble() < 0)
		{
			return doPayment(otherParty, user, amount.times(-1), reason, logEntry, fbo, location, aircraft, comment, blockOnDept);			
		}
		
		try
		{
			boolean blocked = false;
			
			// Trying to pay themselves?
			if (user != otherParty)
			{				
				if (blockOnDept && user != 0)
				{
					// Credit check required first.
					blocked = !checkAnyFunds(user, amount.getAsDouble());				
				}
				
				// If they have funds continue
				if (!blocked)
				{
					String qry = "SELECT * FROM accounts WHERE id in (" + user + ", " + otherParty + ")";
					ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
					while (rs.next())
					{
						Money balance = new Money(rs.getDouble("Money"));
						if (rs.getInt("id") == 0)
							continue;
						else if (rs.getInt("id") == user)
							balance = balance.minus(amount);
						else if (rs.getInt("id") == otherParty)
							balance = balance.plus(amount);
						
						qry = "UPDATE accounts SET money = ? where id = ?";
						dalHelper.ExecuteUpdate(qry, balance.getAsDouble(), rs.getInt("id"));
					}
				}
			}
			
			// No funds
			if (blocked)
				comment = "[BLOCKED] " + comment;
			
			// Log payment attempt
			addPaymentRecord(otherParty, user, amount, reason, logEntry, fbo, location, aircraft, comment);
			
			return !blocked;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return false;
	}
	
	public boolean checkFunds(int accountId, double requiredAmount) throws SQLException
	{
		boolean result = false;
		try
		{
			String qry = "SELECT money FROM accounts WHERE id = ?";
			double cash = dalHelper.ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), accountId);

			result = requiredAmount <= cash;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	public boolean checkAnyFunds(int accountId, double requiredAmount) throws SQLException
	{
		boolean result = false;
		try
		{		
			String qry = "SELECT money + bank FROM accounts WHERE id = ?";
			double cash = dalHelper.ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), accountId);

			result = requiredAmount <= cash;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}
	
	public double[][] getStatement(Calendar month, int user, int fboId, String aircraft, boolean paymentsToSelf)
	{
		double[][] result = new double[PaymentBean.MAX_REASON+1][2];

		try
		{
//TODO does this need to change to UTC returned values or is server time ok?
			String sql = 
				"SELECT reason, sum(amount) FROM payments WHERE MONTH(time) = " + 
				(month.get(Calendar.MONTH)+1) + 
				" AND YEAR(time) = " + month.get(Calendar.YEAR) + " AND ";
			
			if (!paymentsToSelf)
				sql = sql + "user <> otherparty AND ";
			
			if (fboId > 0)
				sql = sql + " fbo = " + fboId + " AND ";
			
			if ((aircraft != null) && !aircraft.equals(""))
				sql = sql + " aircraft = '" + aircraft + "' AND ";
			
			double total = 0.0f;
			
			String qry = sql + "user = ? AND amount > 0 GROUP BY reason";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user);
			while (rs.next())
			{
				result[rs.getInt(1)][0] += rs.getDouble(2);
				total += rs.getDouble(2);
			}
			rs.close();
			
			qry = sql + "user = ? AND amount < 0 GROUP BY reason";
			rs = dalHelper.ExecuteReadOnlyQuery(qry, user);
			while (rs.next())
			{
				result[rs.getInt(1)][1] += rs.getDouble(2);
				total += rs.getDouble(2);
			}
			rs.close();
			
			qry = sql + "otherParty = ? AND amount > 0 GROUP BY reason";
			rs = dalHelper.ExecuteReadOnlyQuery(qry, user);
			while (rs.next())
			{
				result[rs.getInt(1)][1] += -rs.getDouble(2);
				total -= rs.getDouble(2);
			}
			rs.close();
			
			qry = sql + "otherParty = ? AND amount < 0 GROUP BY reason";
			rs = dalHelper.ExecuteReadOnlyQuery(qry, user);
			while (rs.next())
			{
				result[rs.getInt(1)][0] += -rs.getDouble(2);
				total -= rs.getDouble(2);
			}
			
			result[0][0] = total; 
			rs.close();
        }
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return result;
	}	
		
	void mustBeLoggedIn(UserBean user) throws DataError
	{
		if (user == null || user.getId() == -1)
			throw new DataError("Not logged in.");
	}
	
	public long getMilesFlown()
	{
		return milesFlown;
	}

	public long getMinutesFlown()
	{
		return minutesFlown;
	}

	public void setMilesFlown(long i)
	{
		milesFlown = i;
	}

	public void setMinutesFlown(long i)
	{
		minutesFlown = i;
	}

	public long getTotalIncome()
	{
		return totalIncome;
	}

	public void setTotalIncome(long i)
	{
		totalIncome = i;
	}
	
	public statistics[] getStatistics()
	{
		return statistics;
	}
	
	public static String oddLine(int line)
	{
		return line%2 == 1 ? "class=\"odd\" ":"";
	}
	
	public static String sortHelper(String helper)
	{
		return "<span style=\"display: none\">" + helper + "</span>";
	}	
	
	/* airportLink with a gmap pop require in the jsp page:
	 * <head>
	 * <script type="text/javascript" src="scripts/PopupWindow.js"></script>
	 * <script type="text/javascript"> var gmap = new PopupWindow(); </script>
	 * </head>	  
	 */
	public String airportLink(AirportBean airport, javax.servlet.http.HttpServletResponse response)
	{
		return airportLink(airport, null, null, null, response);
	}
	
	public String airportLink(AirportBean airport, AirportBean gmapAirport, javax.servlet.http.HttpServletResponse response)
	{
		return airportLink(airport, null, gmapAirport, null, response);
	}
	
	public String airportLink(AirportBean airport, AirportBean gmapAirport, AirportBean gmapAirportTo, javax.servlet.http.HttpServletResponse response)
	{
		return airportLink(airport, null, gmapAirport, gmapAirportTo, response);
	}
	
	public String airportLink(AirportBean airport, String bulletCodeLocation, AirportBean gmapAirport, AirportBean gmapAirportTo, javax.servlet.http.HttpServletResponse response)
	{
		if (airport == null)
			return "";
		
		String sorthelp = "";
		String image = "";
		if (gmapAirport != null)
		{
			sorthelp = sortHelper(airport.getIcao());
			
			String icaodPart = "";
			if ((gmapAirportTo != null) && !gmapAirportTo.getIcao().equals(gmapAirport.getIcao()))
				icaodPart = "&icaod=" + gmapAirportTo.getIcao();
			
			image = "<A HREF=\"#\" onClick=\"gmap.setSize(620,530);gmap.setUrl('gmap.jsp?icao=" +
			        gmapAirport.getIcao() +
			        icaodPart +
			        "');gmap.showPopup('gmap');return false;\" NAME=\"gmap\" ID=\"gmap\">" +
			        "<img src=\"" +
			        airport.getDescriptiveImage(getFboByLocation(airport.getIcao())) +
			        "\" align=\"absmiddle\" border=\"0\" /></a>";
		}
		
		String bulletPart = "";
		if ((bulletCodeLocation != null) && (!bulletCodeLocation.equals("")))
			bulletPart = bulletCodeLocation + " onMouseOut=\"hideBullet()\" ";
		
		String href= response.encodeRedirectURL("airport.jsp?icao=" + airport.getIcao());
		String textLink = "<a title=\"" + airport.getTitle() + "\" " +
						  bulletPart +
						  " class=\"normal\" href=\"" + href +"\">" +
						  airport.getIcao() +
						  "</a>";
		
		return sorthelp + image + textLink;
	}
	
	public String getBearingImageURL(double bearing)
	{
		int id = (int)Math.round(bearing/45.0)%8;
		return "img/set2_" + id + ".gif";
	}
	
	public static String clearHtml(String input)
	{
		if (input == null)
			return null;
		
		return input.replaceAll("<[^>]*>", "").trim();
	}

	public int getMaxCommodityId()
	{
		return maxCommodityId;
	}

	public int getPTAssignmentCount(int user)
	{
		int ptAssignmentCount = 0;
		
		try
		{
			String qry = "SELECT count(*) FROM assignments WHERE fromTemplate is null AND fromFboTemplate is not null AND userlock = ? AND active = 1";
			ptAssignmentCount = dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), user);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	
		return ptAssignmentCount;
	}
	
	/*
	 * Update assignment.mpttax field for user enroute jobs
	 */ 	
	public void updateMultiPTtax(int user, int ptAssignmentCount)
	{
		int mptTaxRate; // * 1; change if rate changes per assignment
		
		if( ptAssignmentCount > 5) //We only apply the tax on more then 5 assignments
		{
			//Tax is limited to 60, which just happens to be the limit of jobs as well
			if( ptAssignmentCount > 60 ) 
				mptTaxRate = 60;
			else
				mptTaxRate = ptAssignmentCount;
		}
		else //Otherwise there should be zero tax
		{
			mptTaxRate = 0;
		}
		
		try
		{
			String qry = "UPDATE assignments SET mpttax = ? WHERE userlock = ? AND active = 1 AND fromTemplate is null AND fromFboTemplate is not null AND mpttax < ?";
			dalHelper.ExecuteUpdate(qry, mptTaxRate, user, mptTaxRate);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
		
	public void updateAircraft4Admins(AircraftBean aircraft, String newRegistration) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * from aircraft WHERE registration = '" + aircraft.getRegistration() + "'");
			if (!rs.next())
				throw new DataError("No aircraft found.");
			
			aircraft.setEquipment(rs.getInt("equipment"));
			if (newRegistration != null && getAircraftByRegistration(newRegistration).length > 0)
				throw new DataError("Registration already in use.");
			
			double[] distanceFromHome = getDistanceBearing(aircraft.getLocation(), aircraft.getHome());
			aircraft.setDistance((int)Math.round(distanceFromHome[0]));
			aircraft.setBearing((int)Math.round(distanceFromHome[1]));	
			rs.updateString("home", aircraft.getHome());
			rs.updateString("location", aircraft.getLocation());
			rs.updateInt("owner", aircraft.getOwner());
			
			if (aircraft.getUserLock() == 0) 
			{
				rs.updateNull("userlock");
				rs.updateNull("lockedSince");
			}
			else
			{
				rs.updateInt("userlock", aircraft.getUserLock());
			}
			rs.updateInt("bonus", aircraft.getBonus());
			rs.updateInt("accounting", aircraft.getAccounting());
			rs.updateInt("rentalDry", aircraft.getRentalPriceDry());
			rs.updateInt("rentalWet", aircraft.getRentalPriceWet());
			rs.updateInt("maxRentTime", aircraft.getMaxRentTime());
			rs.updateInt("equipment", aircraft.getEquipment());
			rs.updateInt("advertise", aircraft.getAdvertise());
			rs.updateInt("allowFix", aircraft.getAllowFix());
			if (aircraft.home.equals(aircraft.getLocation()))
			{
				rs.updateNull("bearingToHome");
				rs.updateInt("distanceFromHome", 0);
			} 
			else
			{
				rs.updateInt("bearingToHome", aircraft.getBearing());
				rs.updateInt("distanceFromHome", aircraft.getDistance());
			}
			
			if (aircraft.getSellPrice() != 0)
				rs.updateInt("sellPrice", aircraft.getSellPrice());
			else
				rs.updateNull("sellPrice");
				
			if (newRegistration != null)
			{
				newRegistration = newRegistration.trim();
				PreparedStatement logUpdate = conn.prepareStatement("UPDATE log SET aircraft = ? WHERE aircraft = ?");
				logUpdate.setString(1, newRegistration);
				logUpdate.setString(2, aircraft.registration);
				logUpdate.execute();
				logUpdate.close();
				PreparedStatement paymentsUpdate = conn.prepareStatement("UPDATE payments SET aircraft = ? WHERE aircraft = ?");
				paymentsUpdate.setString(1, newRegistration);
				paymentsUpdate.setString(2, aircraft.registration);
				paymentsUpdate.execute();
				paymentsUpdate.close();
				PreparedStatement damageUpdate = conn.prepareStatement("UPDATE damage SET aircraft = ? WHERE aircraft = ?");
				damageUpdate.setString(1, newRegistration);
				damageUpdate.setString(2, aircraft.registration);
				damageUpdate.execute();
				damageUpdate.close();
				
				rs.updateString("registration", newRegistration);
			}
			
			rs.updateRow();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}			
	}

	public void updateGoods4Admins(int owner, String icao, int type, int amount) throws DataError
	{
		changeGoodsRecord(icao, type, owner, amount, false);
	}
	
	/**
	* return a collection of email addresses associated with renter ID's (Account ID's in the Accounts table or GrouID's in the GroupMembership table)
	* @ param renters - ArrayList of renter ID's	
	* @ return ArrayList - collection of email addresses
	* @ author - Gurka
	*/ 	
	public List<String> getEmailAddressForRenterIDs(List<Integer> renters)
	{
		String qry;
		String result;
		List<String> emails = new ArrayList<String>();
		
		try 
		{
			for(int id : renters) 
			{
				String type = getAccountTypeById(id);
				if("group".contains(type))
				{
					//get email ID's for the staff belonging to the group which rented this facility
					qry = "SELECT email FROM accounts, groupmembership AS gm WHERE id = userID and (gm.level = 'staff' or gm.level = 'owner') and groupID = ?";
					ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
					while(rs.next())
						emails.add(rs.getString(1));
				}
				else
				{
					//get email ID for the user
					qry = "SELECT email FROM accounts WHERE id = ?";
					result = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);
					if(result != null) //this should never be null, but check anyways
						emails.add(result);
				}
			}			
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
				
		return emails;
	}
	
//	/*
//	 * Gets the aircraft shipping size from the shipping config table using the aircraft empty weight
//	 * param emptyweight
//	 */
//	public int getAircraftShippingSize(int emptyweight)
//	{
//		int shippingSize = 0;
//
//		try
//		{
//			String qry = "SELECT shippingSize FROM shippingConfigsAircraft WHERE minSize <= ? AND maxSize >= ?";
//			Object o = dalHelper.ExecuteScalar(qry, emptyweight, emptyweight);
//			if(o != null && o instanceof Integer)
//				shippingSize = (Integer)o;
//		} 
//		catch (SQLException e)
//		{
//			e.printStackTrace();
//		} 
//
//		return shippingSize;
//	}	

	public ServiceProviderBean getServiceProviderById(int serviceid)
	{
		ServiceProviderBean[] result = null;

		try
		{
			String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id  WHERE s.id = ? order by status, a1.name";			
			CachedRowSet crs = dalHelper.ExecuteReadOnlyQuery(qry, serviceid);
			result = getServiceProviderArray(crs);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return result.length == 0 ? null : result[0];
	}

	public ServiceProviderBean getServiceProviderByOwner(int userid)
	{
		ServiceProviderBean[] result = null;

		try
		{
			String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id  WHERE s.owner = ? order by status, a1.name";			
			CachedRowSet crs = dalHelper.ExecuteReadOnlyQuery(qry, userid);
			result = getServiceProviderArray(crs);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return result.length == 0 ? null : result[0];
	}

	public ServiceProviderBean[] getServiceProviders()
	{
		ServiceProviderBean[] result = null;

		try
		{
			String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id";
			CachedRowSet crs = dalHelper.ExecuteReadOnlyQuery(qry);
			result = getServiceProviderArray(crs);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return result;
	}

	public ServiceProviderBean getServiceProviderByKey(String key)
	{
		ServiceProviderBean[] result = null;

		try
		{
			String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id where s.key = ?";
			CachedRowSet crs = dalHelper.ExecuteReadOnlyQuery(qry, key);
			result = getServiceProviderArray(crs);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		
		return result.length == 0 ? null : result[0];
	}

	public String getServiceProviderNameByKey(String key)
	{
		String result = null;
		
		try
		{
			String qry = "select name from serviceproviders where `key`= ?";
			result = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), key);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result;
	}

	ServiceProviderBean[] getServiceProviderArray(ResultSet rs)
	{
		ArrayList<ServiceProviderBean> result = new ArrayList<>();
		try
		{
			while (rs.next())
			{
				ServiceProviderBean item = new ServiceProviderBean(rs);
				result.add(item);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		return result.toArray(new ServiceProviderBean[result.size()]);
	}

	public void addServiceProvider(ServiceProviderBean service)
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		Date date = new Date();
		String note = Formatters.dateyyyymmddhhmmss.format(date) + " - Created";
		service.setNotes(note);
		service.setKey(createAccessKey());
		try
		{
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("select * from serviceproviders where id=-1");

			rs.moveToInsertRow();
			service.writeBean(rs);
			rs.insertRow();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}
	}
	
	public void updateServiceProvider(ServiceProviderBean service)
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try
		{
			conn = dalHelper.getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM serviceproviders WHERE id=" + service.getId());

			rs.first();
			service.writeBean(rs);
			rs.updateRow();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			dalHelper.tryClose(rs);
			dalHelper.tryClose(stmt);
			dalHelper.tryClose(conn);
		}
	}
	
	public void doServiceProviderNotification(ServiceProviderBean service, String subject, String messageText, boolean adminonly) throws DataError
	{
		List<String> toList = new ArrayList<>();
		
		Emailer emailer = Emailer.getInstance();

		if(!adminonly)
		{
			UserBean owner = getAccountById(service.getOwner());
			UserBean alt = null;
			if(service.getAlternate() != -1)
				alt = getAccountById(service.getAlternate());
			
			toList.add(owner.getEmail());

			if(alt != null)
				toList.add(alt.getEmail());
		}

		toList.add("fseadmin@gmail.com");
		
		emailer.sendEmail("no-reply@fseconomy.net", "FSEconomy Service Notification System",
				subject, messageText, toList, Emailer.ADDRESS_TO);
	}
	
	public boolean hasAllInJobInQueue(int user)
	{
		boolean hasAllInJob=false;
		
		try
		{
			String qry = "SELECT (count(id) > 0) AS found FROM assignments WHERE userlock = ? AND aircraft IS NOT NULL";
			hasAllInJob = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);			
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return hasAllInJob;
	}

	/**
	* return a mysql resultset as an ArrayList. Mysql query of log table to return a users last 500 flights 
	* and calulate the total hours flown in last 48 hours.
	* @ param users - user name input from adminuser48hourtrend.jsp. Access this screen from admin.jsp	
	* @ return Mysql Resulset as an ArrayList to adminuser48hourtrend.jsp
	* @ author - chuck229
	*/ 		
	public trendHours[] getTrendHoursQuery(String user) throws DataError
	{
		ArrayList<trendHours> result = new ArrayList<>();
		try
		{
			String qry = "SELECT `time` as LOGDATE, cast(flightenginetime as signed) as Duration, cast((SELECT SUM(flightenginetime) FROM `log` where user = ? and `time` <= LOGDATE and `time` > DATE_SUB(LOGDATE, INTERVAL 48 HOUR)) as signed) as last48hours FROM log WHERE `user` = ? and TYPE = 'flight' ORDER BY TIME DESC Limit 500";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user, user);
			while (rs.next())
			{
				trendHours trend = new trendHours(rs.getString("LOGDATE"),rs.getInt("Duration"), rs.getInt("last48hours"));
				result.add(trend);
			}				
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		} 

		return result.toArray(new trendHours[result.size()]);
	}
	
	public void logBulkFuelRequest(int fboId) 
	{		
		//log the current date/time for this request so another one cannot be requested for 24hrs.
		Timestamp timestamp = new Timestamp(new Date().getTime());

		try
		{
			String qry = "UPDATE fbo SET bulkFuelOrderTimeStamp = ? WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, timestamp, fboId);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	//reset the timestamp for a given FBO so they can process a request for bulk fuel without waiting for the 24 hour rule
	public void resetBulkFuelOrder(int fboId) 
	{
		try
		{
			String qry = "UPDATE fbo SET bulkFuelOrderTimeStamp = null, bulk100llOrdered = null, bulkJetAOrdered = null, bulkFuelDeliveryDateTime = null WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, fboId);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	//check to see if a request has been made in the past 24 hrs for an FBO
	public boolean doesBulkFuelRequestExist(int id) 
	{
		Calendar calNow = Calendar.getInstance();
		try
		{
			String qry = "SELECT bulkFuelOrderTimeStamp, bulkFuelDeliveryDateTime FROM fbo WHERE id = ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
			
			if(!rs.next())
				return false;
			
			Timestamp orderTS = rs.getTimestamp(1);
			Timestamp deliveryTS = rs.getTimestamp(2);
			
			if (orderTS == null)
				return false;

			if(deliveryTS != null)
				return true;
			
			//check for a request in the past 24 hours
			Calendar calTS = Calendar.getInstance();
			calTS.setTimeInMillis(orderTS.getTime());

			long diff = calNow.getTimeInMillis() - calTS.getTimeInMillis();
	        long diffHours = diff / MILLISECS_PER_HOUR;

	        if (diffHours < 24)
	        	return true;
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}			
			
		return false;
	}
		
	//check to see if a order in progress
	public boolean doesBulkFuelOrderExist(int id) 
	{
		try
		{
			String qry = "SELECT (bulkFuelDeliveryDateTime is not null) as found FROM fbo WHERE id = ?";

            return dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}			
			
		return false;
	}
		
	//calculate a shipping day for bulk fuel
	public int calculateShippingDay() 
	{
		Random randomGenerator = new Random();
		int randomInt;
		int base=3;	//from 0 to 3 days out
		randomInt = randomGenerator.nextInt(base);
	
		return randomInt;
	}
	
	public String deliveryDateFormatted(int daysOut) 
	{
		Calendar deliveryDate = Calendar.getInstance();
		if (daysOut == 0)	
			deliveryDate.add(Calendar.HOUR_OF_DAY, 4); //add 4 hours if same day
		else
			deliveryDate.add(Calendar.DATE, daysOut);
		
		Timestamp deliveryDateSql = new Timestamp(deliveryDate.getTimeInMillis());

		return Formatters.dateyyyymmddhhmmzzz.format(deliveryDateSql);
	}	
	
	//record the transaction request for bulk fuel - to be delivered later by the maintenance code
	public void registerBulkFuelOrder(UserBean user, int fboID, int amount100ll, int amountJetA, int daysOut, int accountToPay, int location, String icao) throws DataError
	{
		UserBean account = getAccountById(accountToPay);
		
		if (account.getId() != user.getId() && user.groupMemberLevel(account.getId()) < UserBean.GROUP_STAFF)
			throw new DataError("Permission denied");		

		if (doesBulkFuelOrderExist(fboID) ) 
			throw new DataError("Fuel order already exists.");		

		double price100ll = quoteFuel(icao, GoodsBean.GOODS_FUEL100LL, amount100ll);
		double priceJetA = quoteFuel(icao, GoodsBean.GOODS_FUELJETA, amountJetA);		

		double total = price100ll + priceJetA;
		if(account.getMoney() < total)
			throw new DataError(account.getName() + " has insufficent funds for this purchase!");

		Calendar calDeliveryDate = Calendar.getInstance();
		if (daysOut == 0)	
			calDeliveryDate.add(Calendar.HOUR_OF_DAY, 4); //add 4 hours if same day
		else
			calDeliveryDate.add(Calendar.DATE, daysOut);
		
		Timestamp deliveryTS = new Timestamp(calDeliveryDate.getTimeInMillis());		
		
		try
		{
			String daysMsg = " -- delivery ETA: " + deliveryDateFormatted(daysOut);			
			String comment1="", comment2="";
			
			if (amount100ll > 0)
				comment1 = "100LL:" + amount100ll + " Kg";
			
			if (amountJetA>0)
				comment2 = " JetA:" + amountJetA + " Kg";			
			
			String qry = "UPDATE fbo SET bulk100llOrdered = ?, bulkJetAOrdered = ?, bulkFuelDeliveryDateTime = ? WHERE id = ?";
			dalHelper.ExecuteUpdate(qry, amount100ll, amountJetA, deliveryTS, fboID);

			//Now deduct the $ from the account paying for the order - transfer amount to Bank of FSE, log each payment seperately
			doPayBulkFuel(accountToPay, 0, (int)price100ll, location, comment1+daysMsg, icao, GoodsBean.GOODS_FUEL100LL);
			doPayBulkFuel(accountToPay, 0, (int)priceJetA, location, comment2+daysMsg, icao, GoodsBean.GOODS_FUELJETA);			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}	
	
	/**
	 * Fuel Exploit Check container class
	 */
	public class FuelExploitCheck
	{
		public int PaymentId;
		public Date Time;
		public float TotalAmount;
		public String FuelType;
		public BigDecimal PerGal;
		public String Location;
		public String Aircraft;
		public String User;
		public String OtherParty;
		public String Buyer;	
		public String Comment;
	}
	
	public List<FuelExploitCheck> getFuelExploitCheckData(int pricepoint, int count) 
	{
		List<FuelExploitCheck> list = new ArrayList<>();
		String qry;
		
		try
		{
			qry = "select p.*, a1.name as userName, a1.Type as userNameType, a2.name as otherPartyName, a2.type as otherPartyNameType, a3.name as buyerName, a3.type buyerNameType from " +
			"(select *, cast(substring(comment, locate('ID: ', comment)+4) as unsigned) as buyer,  cast(substring(comment, locate('Gal: ', comment)+5) as decimal(18,2)) as PerGal from payments   where (reason = 1 or reason = 22) and cast(substring(comment, locate('Gal: ', comment)+5) as decimal(18,2))  >= ?) p " +
			"join accounts a1 on  a1.id=p.user " +
			"join accounts a2 on  a2.id=p.otherParty " +
			"join accounts a3 on  a3.id=p.buyer " +
			"order by id desc " +
			"limit ?";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, pricepoint, count);
			
			while(rs.next())
			{
				FuelExploitCheck fec = new FuelExploitCheck();
				fec.Aircraft = rs.getString("aircraft");				
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

	public void SetDatafeedUrl() 
	{
		try
		{
			String qry = "SELECT svalue from sysvariables where variablename='DataFeedUrl'";
			DataFeedUrl = (String)dalHelper.ExecuteScalar(qry);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	public void addClientRequestEntry(String ipAddress, int id, String name, String client, String state, String aircraft, String params) 
	{
		try
		{
			String qry = "INSERT INTO clientrequests ( ip, pilotid, pilot, client, state, aircraft, params) VALUES(?,?,?,?,?,?,?)";
			dalHelper.ExecuteUpdate(qry, ipAddress, id, name, client, state, aircraft, params);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	public List<String> getClientRequestCountsByAccountId(int id) throws DataError
	{
		String qry = "select ip, count(ip) from (select ip from clientrequests where pilotid=?) a group by ip";
		
		return getClientRequestCounts(qry, id, "");
	}
	
	public List<String> getClientRequestCountsByIp(String ip) throws DataError
	{
		String qry = "select pilot, count(pilot) from (select pilot from clientrequests where ip=?) a group by pilot";
		
		return getClientRequestCounts(qry, 0, ip);
	}
	
	public List<String> getClientRequestCounts(String qry, int id, String ip) throws DataError
	{
		List<String> list = new ArrayList<>();
		try
		{
			ResultSet rs;
			
			if(ip.equals(""))
				rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
			else
				rs = dalHelper.ExecuteReadOnlyQuery(qry, ip);
			
			while(rs.next())
				list.add(rs.getString(1) + "|" + rs.getString(2));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new DataError("getClientRequestCounts: SQL Error");
		} 
		
		return list;
	}
	
	
	public List<clientrequest> getClientRequestsByAccountId(int id) throws DataError
	{
		String qry = "Select * from clientrequests where pilotid = ? order by id desc limit 100";
		
		return getClientRequests(qry, id, "");
	}
	
	public List<clientrequest> getClientRequestsByIp(String ip) throws DataError
	{
		String qry = "Select * from clientrequests where ip = ? order by id desc limit 100";
		
		return getClientRequests(qry, 0, ip);
	}

	public List<clientrequest> getClientRequests(String qry, int id, String ip) throws DataError
	{
		List<clientrequest> list = new ArrayList<>();
		try
		{
			ResultSet rs;
			
			if(ip.equals(""))
				rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
			else
				rs = dalHelper.ExecuteReadOnlyQuery(qry, ip);
			
			while(rs.next())
			{
				clientrequest c = new clientrequest();
				c.id = rs.getInt("id");
				c.time = rs.getTimestamp("time");
				c.ip = rs.getString("ip");
				c.userid = rs.getInt("pilotid");
				c.name = rs.getString("pilot");
				c.client = rs.getString("client");
				c.state = rs.getString("state");
				c.aircraft = rs.getString("aircraft");
				c.params = rs.getString("params");
				list.add(c);
			}			
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new DataError("getClientRequests: SQL Error");
		} 
		
		return list;
	}
	
	public List<String> getClientRequestIps() throws DataError
	{
		List<String> list = new ArrayList<>();
		try
		{
			String qry = "Select DISTINCT ip from clientrequests order by ip";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);

			while(rs.next())
				list.add(rs.getString("ip"));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new DataError("getClientRequestIps: SQL Error");
		} 
		
		return list;
	}
	
	
	public List<String> getClientRequestIpWithMultipleUsers() throws DataError
	{
		List<String> list = new ArrayList<>();
		try
		{
			String qry = "SELECT ip, GROUP_CONCAT(DISTINCT pilot) AS users FROM clientrequests GROUP BY ip HAVING COUNT(DISTINCT pilot) > 1 ORDER BY COUNT(DISTINCT pilot) DESC";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);

			while(rs.next())
				list.add(rs.getString("ip") + "|" + rs.getString("users"));
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new DataError("getClientRequestIps: SQL Error");
		} 
		
		return list;
	}

	public int getFacilityJobCount(int id, String location)
	{
		int cnt = 0;
		try
		{
			String qry = "select t.* from fbofacilities t where t.occupant = ? AND t.location = ? order by location, id";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, id, location);

			while(rs.next())
			{
				int facid = rs.getInt("id");
				qry = "SELECT sum(amount) FROM assignments where fromfbotemplate = ?";
				cnt += dalHelper.ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), facid);
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return cnt;
	}

	public boolean checkAllInAircraftWithOutAssigment(AircraftBean aircraft) throws DataError
	{
		try
		{	
			String qry = "SELECT (fuelSystemOnly=1) as found FROM aircraft, models where aircraft.model=models.id AND fuelSystemOnly=1 AND (rentalPrice is null OR rentalPrice=0) AND registration = ? AND location = ?";
			boolean isAllInAircraft = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft.getRegistration(), aircraft.getLocation());			

			if (isAllInAircraft &&!hasAllInJobInQueue(aircraft.getUserLock()))
				return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public boolean checkAllInFlightWithAssignment(AircraftBean aircraft) throws DataError
	{
		try
		{	
			String qry = "SELECT y=1) as found FROM aircraft, models where aircraft.model=models.id AND fuelSystemOnly=1 AND (rentalPrice is null OR rentalPrice=0) AND registration = ? AND location = ?";
			boolean isAllInAircraft = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft.getRegistration(), aircraft.getLocation());			

			if (isAllInAircraft && !hasAllInJobInQueue(aircraft.getUserLock()))
				return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public List<PilotStatus> getPilotStatus()
	{
		List<PilotStatus> toList = new ArrayList<>();
		
		try
		{
			String qry = "select  a.name, Concat(m.make,  \" \", m.model) as makemodel, case when d.location is not null then  \"p\" else \"f\" end as status, case when d.location is not null then  d.location else d.departedfrom end as icao,  case when d.location is not null then  loc.lat else dep.lat end as lat, case when d.location is not null then  loc.lon else dep.lon end as lon  from (select model, location, departedfrom, userlock from aircraft where userlock is not null) d left join accounts a on d.userlock=a.id left join models m on m.id=d.model left join airports loc on loc.icao=d.location left join airports dep on dep.icao=d.departedfrom order by status";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);

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

    public List<LatLonCount> getFlightSummary()
    {
        List<LatLonCount> toList = new ArrayList<LatLonCount>();

        try
        {
            Date date = new Date(System.currentTimeMillis()-(60*60*1000*24));

            String qry = "select lat, lon, count from (select `to`, count(`to`) as count from ( select `to` from log where type='flight' AND  `time` > '" + Formatters.dateyyyymmddhhmmss.format(date) + "') b group by `to`) a join airports ap on ap.icao=`to`";
            ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);

            while(rs.next())
            {
                LatLonCount llc = new LatLonCount(rs.getDouble("lat"), rs.getDouble("lon"), rs.getInt("count"));
                toList.add(llc);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return toList;
    }

}