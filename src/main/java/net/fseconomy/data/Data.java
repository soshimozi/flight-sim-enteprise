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

import com.google.gson.Gson;

import static net.fseconomy.data.Airports.*;

import net.fseconomy.beans.*;
import net.fseconomy.dto.*;
import net.fseconomy.util.Formatters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Data implements Serializable
{
	private static final long serialVersionUID = 3L;

	static final String systemLocation = "http://server.fseconomy.net";
	static final String fromAddress = "no-reply@fseconomy.net";

    private String pathToWeb = "";

    public List<LatLonCount> FlightSummaryList = new ArrayList<>();

	public static long defaultCount = 0;
	public static long createCount = 0;
	public static long cacheCount = 0;
	public static long bytesServed = 0;
	public static long totalImagesSent = 0;

	public static final int stepSize = 20;

	public static final double GALLONS_TO_KG = 2.68735;
	
	//for Signature template selection
	public static int currMonth = 1;
	
	static final int MAX_MODEL_TITLE_LENGTH = 128;



	public static final int ACCT_TYPE_ALL = 1;
	public static final int ACCT_TYPE_PERSON = 2;
	public static final int ACCT_TYPE_GROUP = 3;	

	long milesFlown;
	long minutesFlown;
	long totalIncome;
	Statistics[] statistics = null;
	public static HashMap<String, Statistics> statsmap = null;
	public static HashMap<String, Statistics> prevstatsmap = null;
	
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
		logger.info("Data constructor called");

		dalHelper = DALHelper.getInstance();
		
		Locale.setDefault(Locale.US);
		
		initializeSystemVariables();
		
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
                    qry = "SELECT count(*) AS number FROM accounts WHERE Accounts.logon >= date_sub(curdate(), interval 24 hour)";
                    break;
                case "flying":
                    qry = "SELECT count(*) AS number FROM aircraft LEFT JOIN accounts on aircraft.userlock = Accounts.id WHERE aircraft.location is null";
                    break;
                case "parked":
                    qry = "SELECT count(*) AS number FROM aircraft LEFT JOIN accounts on aircraft.userlock = Accounts.id WHERE aircraft.location is not null AND aircraft.userlock is not null";
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
	public List<PendingHours> getPendingHours(String user, int hours) throws DataError
	{
		ArrayList<PendingHours> result = new ArrayList<>();
		try
		{
			String qry = "SELECT FlightEngineTime, hour(timediff('48:00:00',timediff(now(),time))),  minute(timediff('48:00:00',timediff(now(),time))) FROM `log` where user= ? and DATE_SUB(CURRENT_TIMESTAMP ,INTERVAL ? hour) <= `time` and type <> 'refuel'";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user, hours);
			while (rs.next())
			{
				PendingHours pending = new PendingHours(rs.getInt(1)/3600.0f,rs.getString(2), rs.getString(3));
				result.add(pending);
			}				
		}  
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result;
	}


	public List<String> getDistinctColumnData(String field, String table)throws DataError
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
		
		return result;
	}
	
	public List<TemplateBean> getAllTemplates()
	{
		return getTemplateSQL("SELECT * FROM templates");
	}
	
	public TemplateBean getTemplateById(int Id)
	{
        List<TemplateBean> result = getTemplateSQL("SELECT * FROM templates WHERE id = " + Id);

        return result.size() == 0 ? null : result.get(0);
	}
	
	public List<TemplateBean> getTemplateSQL(String qry)
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

		return result;
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
	public synchronized int processFlight(UserBean user, CloseAirport location, int engineTime, int engineTicks, float[] fuel, int night, float envFactor, int[][] damage, SimType simType) throws DataError
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try //This try block is for SQL Exceptions
		{
			//Defined outside our inner try so that the aircraft lock can be evaluated in the finally block
			AircraftBean aircraft;
			int freeAircraft = 0;

			//Setup our connection needs
			conn = dalHelper.getConnection();			
			
			try //This try block is to free the aircraft processing lock whether processing completed or failed
			{
				/////////////////////////////////////////////////////////////////
				//Validity checks
				/////////////////////////////////////////////////////////////////
				
				// Get the currently locked aircraft for user
				aircraft = Aircraft.getAircraftForUser(user.getId());
				
				//Is valid aircraft
				if (aircraft == null)
				{
					System.err.println(new Timestamp(System.currentTimeMillis()) + " processFlight: No Aircraft lock.  SimType = " + simType.name() + ", User = " + user.getId());
					throw new DataError("VALIDATIONERROR: No aircraft in use, flight aborted.");
				}
				
				//Check flight in progress 
				if (aircraft.getDepartedFrom() == null)
				{ 
					System.err.println(new Timestamp(System.currentTimeMillis()) + " processFlight: No flight in progress.  SimType = " + simType.name() + ", User = " + user.getId()); 
					throw new DataError("VALIDATIONERROR: It appears that a duplicate flight was submitted and canceled. Please check your My Flight page for current flight status."); 
				} 
				
				// Load up our model parameters for later checks
				// Need to do this after getAircraftForUser is validated
				ModelBean m = Models.getModelById(aircraft.getModelId());

				// Speed checks
				// Average Speed of flight should be less than 2.5 the models cruise speed, this is very generous
				int flightDistance = (int)Math.round(getDistance(location.icao, aircraft.getDepartedFrom()));
				Float flightMPH = (float) (flightDistance/(engineTime/3600.0));
				if (flightMPH > (m.getCruise()*2.5))
				{
					cancelFlight(user);
					//Added more debugging variables to the system message, this happens rarely but we have no idea why
					
					System.err.println(new Timestamp(System.currentTimeMillis()) + " Excess Speed Calculated, rejecting flight. SimType = " + simType.name() + ", Reg = " + aircraft.getRegistration() + " User = " + user.getId() + " DepartICAO = " + aircraft.getDepartedFrom() + " ArriveICAO = " + location.icao + " Distance = " + flightDistance + " Airspeed = " + flightMPH + " EngineTime = " + engineTime);
					throw new DataError("VALIDATIONERROR: Invalid speed calculated. (" + flightMPH + "-MPH) between DepartICAO = " + aircraft.getDepartedFrom() + " to ArriveICAO = " + location.icao + " in " + (int)(engineTime/60.0 + .5) + " Minutes, flight aborted");
				}
				
				///////////////////////////////////////////////////////
				// Checks done, start processing flight data
				///////////////////////////////////////////////////////
				
				//modified to always zero out unused fuel tanks based on model data
				for (int i = 0; i < fuel.length; i++)
				{
					int capacity = m.getCap(i);
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
				aircraft.setFuel(fuel);
	
				// Fuel checks, only sets flag, does not toss data error
				double initialFuel = aircraft.getInitialFuel();

				// More fuel then when started, if only that could be true!
				boolean invalidFuel = (!aircraft.wasWetRent()) && (initialFuel < aircraft.getTotalFuel());
				
				// Before we start we need to process Pax assignment to assign the 
				// tax rate to each for computing costs so...
				
				// Get the total pax assignments on board
				int ptAssignmentCount = Assignments.getPTAssignmentCount(user.getId());
				
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
					boolean allInAssignment = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft.getRegistration());
					if( !allInAssignment )
					{
						System.err.println("  Flight kicked: AllIn flight with no assignment at current aircraft location");
						String errmsg = "VALIDATIONERROR: AllIn flights must have the assignment co-located with aircraft. It appears the assignment was not transported -- flight aborted!";
						
						cancelFlight(user);
						throw new DataError(errmsg);
					}
					qry = "SELECT toicao FROM assignments WHERE active = 1 AND aircraft = ?";
					allInAssignmentToIcao = dalHelper.ExecuteScalar(qry, new DALHelper.StringResultTransformer(), aircraft.getRegistration());
				}
				
				//All-In change - don't pay rental, fuel, or distance fees
				if (!allIn)
				{
					// Get Rental cost (Dry/Wet)					
					if (aircraft.wasWetRent())
						price = aircraft.getRentalPriceWet(); // Wet rental
					else
						price = aircraft.getRentalPriceDry();  //Dry rental
					
					// Get rental time in hours
					rentalTime = (float)(engineTime/3600.0);
					
					// Set our aircraft Rental Cost
					rentalCost = price * rentalTime;
					
					// Check to see if we need to add in fuel costs for Dry rental
					// Currently if no fuel was used no fuel cost, Scot-Free???
					if (!aircraft.wasWetRent() && !invalidFuel)
					{
						double cost = (initialFuel - aircraft.getTotalFuel()) * Goods.getFuelPrice(location.icao);
						if (aircraft.getFuelType() == AircraftBean.FUELTYPE_JETA)
							cost *= Goods.getJetaMultiplier();
						
						fuelCost = (float)cost;
					}
					
					// if bonus setting is 0 or we are back to where we started, leave pilot bonus at 0
					if(aircraft.getBonus() != 0 && !location.icao.contentEquals(aircraft.getDepartedFrom()))
					{
						// If we are not at home, we need to calculate distance/bearing otherwise leave at 0, 0
						if(!location.icao.equals(aircraft.getHome()))
							distanceFromHome = getDistanceBearing(location.icao, aircraft.getHome());

						double dist = 0.0;

						// If departure airport was home icao, no need to calculate the distance
						if(!aircraft.getHome().contentEquals(aircraft.getDepartedFrom()))
							dist = getDistance(aircraft.getHome(), aircraft.getDepartedFrom());
						
						double b = aircraft.getBonus();
						
						// If both distance and distanceFromHome are 0, bonus is 0
						if( dist != 0 || distanceFromHome[0] != 0.0 )
							bonus =(float)((b * (dist - distanceFromHome[0])/100.0));
					}
					
					//Bonus can be positive or negative, if the plane moves toward home, 
					//then pilot is payed, if away from home then owner is paid
					flightCost = rentalCost + fuelCost - bonus;
					
					toPayOwner = flightCost;  // Pay this to the owner of the aircraft
	
					if(flightCost == 0 && price != 0)
						System.err.println("****> Rental costs for Reg: " + aircraft.getRegistration() +
							", Date: " + new Timestamp(System.currentTimeMillis()) +
							", Owner: " + Accounts.getAccountNameById(aircraft.getOwner()) +
							", By: " + user.getName() +
							", From: " + aircraft.getDepartedFrom() +
							", To: " + location.icao + 
							", EngineTimeType: " + (aircraft.getAccounting() == AircraftBean.ACC_HOUR ? "Hourly" : "Tach") +
							", EngineTime: " + engineTime + 
							", RentalTime: " + rentalTime + 
							", RentalType: " + (aircraft.wasWetRent() ? "Wet" : "Dry") +
							", InvalidFuel: " + invalidFuel + 
							", pricePerHour: " + price + 
							", Rental Cost: " + rentalCost + 
							", FuelCost: " + fuelCost +
							", Bonus: " + bonus + 
							", TotalToOwner: " + flightCost );
					
					// Crew Cost added  - deducts $100 per hour per additional crew member from payout
					crewCost = m.getCrew() * 100 * (float)(engineTime/3600.0);
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
					UserBean group = Accounts.getGroupById(groupId);
					stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
					stmt.executeUpdate("UPDATE assignments SET pilotFee=pay*amount*distance*0.01*" + group.getDefaultPilotFee()/100.0 + ", groupId = " + groupId + " WHERE active=1 and userlock = " + user.getId() + " and groupId is null");
					stmt.close();
					
					//gurka - added to keep track of the group to pay for closing exploit - to be used later on just before the committ to check balances of group bank account
					groupToPay = group;
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
					
					fboAssignmentFee += Fbos.payFboGroundCrewFees(assignment.getFrom(), assignment, payAssignmentToAccount, location.icao, aircraft.getRegistration(), true);
					fboAssignmentFee += Fbos.payFboGroundCrewFees(location.icao, assignment, payAssignmentToAccount, location.icao, aircraft.getRegistration(), true);
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
						errmsg = "VALIDATIONERROR: Insufficient funds in the group \' " + groupToPay.getName() + "\' cash account to pay for the flight costs -- flight aborted!";
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
                    Banking.doPayment(payAssignmentToAccount, aircraft.getOwner(), toPayOwner, PaymentBean.RENTAL, 0, -1, location.icao, aircraft.getRegistration(), "", false);
				
				// Pay crew cost
				if (crewCost > 0)
                    Banking.doPayment(payAssignmentToAccount, 0, crewCost, PaymentBean.CREW_FEE, 0, -1, location.icao, aircraft.getRegistration(), "", false);
				
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
                    Banking.doPayment(owner, payAssignmentToAccount, value, PaymentBean.ASSIGNMENT, 0, -1, location.icao, aircraft.getRegistration(), "", false);

					// If group flight, pay the pilot fee
					if (groupFlight && pilotFee > 0)
                        Banking.doPayment(payAssignmentToAccount, user.getId(), pilotFee, PaymentBean.PILOT_FEE, 0, -1, location.icao, aircraft.getRegistration(), "", false);
					
					// Charge mptTax - convert tax rate to a percent
					if (mptTaxRate > 0) 
					{
                        Banking.doPayment(payAssignmentToAccount, 0, (value * (mptTaxRate * .01)), PaymentBean.MULTIPLE_PT_TAX, 0, -1, location.icao, aircraft.getRegistration(), "", false);
						
						// Used for tracking log
						mpttax += (value * (mptTaxRate * .01)); 
			        }
					
					int commodityId = assignment.getCommodityId();
					if (commodityId > 0)
					{
						// added in check for aircraft shipping commodity id
						if( commodityId < 99)
						{
							Goods.changeGoodsRecord(location.icao, commodityId, owner, assignment.getAmount(), false);
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
							Aircraft.finalizeAircraftShipment(reg, false, false);
						}
					}			
					
					// Used for tracking log
					totalPilotFee += pilotFee;
					income += value;				
					
					fboAssignmentFee += Fbos.payFboGroundCrewFees(assignment.getFrom(), assignment, payAssignmentToAccount, location.icao, aircraft.getRegistration(), false);
					fboAssignmentFee += Fbos.payFboGroundCrewFees(location.icao, assignment, payAssignmentToAccount, location.icao, aircraft.getRegistration(), false);
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
					aircraft.writeFuel(rs);
				
				// if no awaiting assignments and no aircraft hold, or allin at it destination, then release the aircraft lock/rental
				if ((freeAircraft < 1 && !aircraft.getHoldRental())
					|| (allIn && location.icao.equals(allInAssignmentToIcao)) )
				{
					rs.updateNull("userlock");
					rs.updateNull("lockedSince");
					rs.updateNull("initialFuel");
				} 
				else // otherwise keep the aircraft locked / rented, and update initial fuel
				{
					if (!aircraft.wasWetRent() && !invalidFuel)
						rs.updateFloat("initialFuel", (float)aircraft.getTotalFuel());
				}
				
				// Update the aircraft record
				rs.updateRow();
				
				rs.close();
				stmt.close();
	
				// Update any damage conditions
				//////////////////////////////////////////////////////
				// Currently only Heat, and Mixture > 95% above 1000ft is checked in client
				for (int c=0; c< damage.length; c++)
					Aircraft.addAircraftDamage(aircraft.getRegistration(), damage[c][0], damage[c][1], damage[c][2]);
				
				for (int c=1; c<= aircraft.getEngines(); c++)
					Aircraft.addAircraftDamage(aircraft.getRegistration(), c, AircraftMaintenanceBean.DAMAGE_RUNTIME, engineTime);
				
				// Add log entry
				///////////////////////////////////////////////////
				
				// Get a blank record, and move to insert
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);			
				rs = stmt.executeQuery("SELECT * from log where 1=2");
				rs.moveToInsertRow();
				
				rs.updateTimestamp("time",new Timestamp(System.currentTimeMillis()));
				rs.updateString("user", user.getName());
				rs.updateString("aircraft", aircraft.getRegistration());
				rs.updateString("from", aircraft.getDepartedFrom());
				rs.updateString("to", location.icao);
				rs.updateString("type", "flight");
				rs.updateInt("totalEngineTime", totalEngineTime);
				rs.updateFloat("fuelCost", fuelCost);
				rs.updateFloat("rentalCost", rentalCost);
				rs.updateFloat("income", income);
				rs.updateFloat("landingCost", 0); // not used		
				rs.updateFloat("crewCost", crewCost);
				rs.updateFloat("rentalPrice", price);
				rs.updateString("rentalType", aircraft.wasWetRent() ? "wet" : "dry");
				rs.updateInt("flightEngineTime", engineTime);
				rs.updateInt("flightEngineTicks", engineTicks);
				rs.updateInt("accounting", aircraft.getAccounting());
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
				String fromICAO = aircraft.getDepartedFrom();
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
		ModelBean model = Models.getModelById(bean.getModelId());
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
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, bean.getRegistration(), location);
			if (!rs.next())
				throw new DataError("No active aircraft found");
						
			if(Assignments.hasAllInJobInQueue(user))
				allInFlight = true;

			qry = "SELECT (initialFuel is not null) AS rentedDry FROM aircraft WHERE registration = ?";
			rentedDry = dalHelper.ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), bean.getRegistration());

			if (bean.getCanFlyAssignments(model))
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
						abean.setActive(Assignments.ASSIGNMENT_ENROUTE);
						result.add(abean);
						
						qry = "UPDATE assignments SET active = ? where id = ?";
						dalHelper.ExecuteUpdate(qry, Assignments.ASSIGNMENT_ENROUTE, rs.getInt("id"));
						onBoard++;
					}
				}
				
				if(allInFlight && onBoard != 1)
					throw new DataError("All-In assignment not loaded. Cannot start the flight.");
			}
			qry = "UPDATE aircraft SET departedFrom = ?, lockedSince = ?, location = null where registration = ?";
			dalHelper.ExecuteUpdate(qry, location, now, bean.getRegistration());
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 
		
		return new Object[] 
                   {
                        Math.round(totalWeight),
                        (int) Math.round(totalWeight + fuelWeight + bean.getEmptyWeight()),
                        result,
                        rentedDry
                   };
	}
	
	public Map<String, Integer> getMyFlightInfo(AircraftBean bean, int user) throws DataError
	{
		Map<String, Integer> result = new HashMap<>();
		String location = bean.getLocation();
		try
		{
			int seats=bean.getSeats()-1;
			int crewWeight = bean.getCrew();
			if (bean.getCrew() > 0)  					// subtract first officer seat
				seats -=1;
			
			int totalWeight = 77 + (77 * crewWeight);  	// * seats subtracts weight of pilot (77) + any addt'l crew members
			double weightLeft = bean.maxPayloadWeight();
			ModelBean model = Models.getModelById(bean.getModelId());
			int group = -1;
			int passengerCount = 0;
			
			if (bean.getCanFlyAssignments(model))
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
	
	public Statistics[] getStatistics()
	{
		return statistics;
	}

	public static String sortHelper(String helper)
	{
		return "<span style=\"display: none\">" + helper + "</span>";
	}	

	public static String clearHtml(String input)
	{
		if (input == null)
			return null;
		
		return input.replaceAll("<[^>]*>", "").trim();
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

	/**
	* return a mysql resultset as an ArrayList. Mysql query of log table to return a users last 500 flights 
	* and calulate the total hours flown in last 48 hours.
	* @ param users - user name input from checkuser48hourtrend.jsp. Access this screen from index.jsp
	* @ return Mysql Resulset as an ArrayList to checkuser48hourtrend.jsp
	* @ author - chuck229
	*/ 		
	public TrendHours[] getTrendHoursQuery(String user) throws DataError
	{
		ArrayList<TrendHours> result = new ArrayList<>();
		try
		{
			String qry = "SELECT `time` as LOGDATE, cast(flightenginetime as signed) as Duration, cast((SELECT SUM(flightenginetime) FROM `log` where user = ? and `time` <= LOGDATE and `time` > DATE_SUB(LOGDATE, INTERVAL 48 HOUR)) as signed) as last48hours FROM log WHERE `user` = ? and TYPE = 'flight' ORDER BY TIME DESC Limit 500";
			ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry, user, user);
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

		return result.toArray(new TrendHours[result.size()]);
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
	
	
	public List<ClientRequest> getClientRequestsByAccountId(int id) throws DataError
	{
		String qry = "Select * from clientrequests where pilotid = ? order by id desc limit 100";
		
		return getClientRequests(qry, id, "");
	}
	
	public List<ClientRequest> getClientRequestsByIp(String ip) throws DataError
	{
		String qry = "Select * from clientrequests where ip = ? order by id desc limit 100";
		
		return getClientRequests(qry, 0, ip);
	}

	public List<ClientRequest> getClientRequests(String qry, int id, String ip) throws DataError
	{
		List<ClientRequest> list = new ArrayList<>();
		try
		{
			ResultSet rs;
			
			if(ip.equals(""))
				rs = dalHelper.ExecuteReadOnlyQuery(qry, id);
			else
				rs = dalHelper.ExecuteReadOnlyQuery(qry, ip);
			
			while(rs.next())
			{
                ClientRequest c = new ClientRequest();
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
        List<LatLonCount> toList = new ArrayList<>();

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