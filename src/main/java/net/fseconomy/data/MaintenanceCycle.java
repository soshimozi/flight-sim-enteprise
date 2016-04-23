package net.fseconomy.data;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

import javax.sql.rowset.CachedRowSet;

import net.fseconomy.beans.*;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.dto.DistanceBearing;
import net.fseconomy.dto.Statistics;
import net.fseconomy.util.Converters;
import net.fseconomy.util.Formatters;
import net.fseconomy.util.GlobalLogger;


public class MaintenanceCycle implements Runnable
{
	static final int REG_ICAO = 0;
	static final int REG_PREFIX = 1;
	static final int REG_POSTFIX = 2;

	public static final int CycleType30Min = 0;
	public static final int CycleTypeDaily = 1;

	public static final int ASSGN_COUNT = 0;
    public static final int ASSGN_GLOCKED = 1;
	public static final int ASSGN_MAX = 2;
	public static final int ASSGN_MIN = 3;
	public static final int ASSGN_AVG = 4;

    public static final int ASSGN_EXT_DAYS = 21;

	
	int lastDailyRun;
	double interestPositive;
	double interestNegative;
	static String lastStatus;
	static Date lastUpdate;
	
	private static CycleTimeData cycleTimeHistory;
	public static Date assignmentsLastUpdate = new Date();
	public static Map<Integer, Integer[]> assignmentsPerTemplate = null;

	private static MaintenanceCycle maintenanceInstance = null;
	
	public static MaintenanceCycle getInstance()
	{
		return maintenanceInstance;
	}

	public MaintenanceCycle()
	{
		GlobalLogger.logApplicationLog("MaintenanceCycle Constructor called", MaintenanceCycle.class);
		
		lastDailyRun = -1;
		//interestPositive = Math.pow(1.05, 1/365.0) - 1;
		//interestNegative = Math.pow(1.10, 1/365.0) - 1;	
		interestPositive = .05 /365.0;
		interestNegative = .10 / 365.0;	
		
		maintenanceInstance = this;
		
		// for signature template selection
		GregorianCalendar today = new GregorianCalendar();
		Data.currMonth = today.get(Calendar.MONTH) + 1;
	}
	
	public class CycleTimeData
	{ 
		public long[] logstarttime = new long[2];
		public long[] hitcount = new long[2];
		public long[] totaltime = new long[2];
		public long[] mintime = new long[2];
		public long[] maxtime = new long[2];
	}

	private synchronized void doCycleElapsedTimeUpdate(int cycletype, long starttime, long endtime)
	{
        long elapsed = endtime-starttime;
        long currTime = System.currentTimeMillis();

		GlobalLogger.logApplicationLog("Cycle Completed: type = " + cycletype + ", elapsed time = " + elapsed + "ms", MaintenanceCycle.class);

		if(cycleTimeHistory == null)
			cycleTimeHistory = new CycleTimeData();

		//Last recorded cycle time
		cycleTimeHistory.logstarttime[cycletype] = currTime;
		
		cycleTimeHistory.hitcount[cycletype]++;
		
		cycleTimeHistory.totaltime[cycletype] += elapsed;

		//set initial value if first update
		if(cycleTimeHistory.mintime[cycletype] == 0) 
			cycleTimeHistory.mintime[cycletype] = elapsed;
		
		if(cycleTimeHistory.mintime[cycletype] > elapsed)
			cycleTimeHistory.mintime[cycletype] = elapsed;
		
		if(cycleTimeHistory.maxtime[cycletype] < elapsed)
			cycleTimeHistory.maxtime[cycletype] = elapsed;
	}
	
	public static CycleTimeData getCycleTimeData()
	{
		return cycleTimeHistory;
	}	
	
	void updateStatus(String message)
	{
		lastStatus = message;
		lastUpdate = new Date();
	}
	
	public static String status()
	{
		if (lastStatus == null)
			return "Unknown";
		
		return  Formatters.dateyyyymmddhhmmzzz.format(lastUpdate) + ":" + lastStatus;
	}
	
	boolean isOncePerDay()
	{
		GregorianCalendar today = new GregorianCalendar();
		int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
		Data.currMonth = today.get(Calendar.MONTH) + 1;
		getLastInterestRun();

        return dayOfWeek != lastDailyRun;
    }
	
	boolean oneTimeStats = false;
	public void SetOneTimeStatsOnly(boolean flag)
	{
		oneTimeStats = flag;
	}
	
	public void run()
	{
		if(oneTimeStats)
		{
			oneTimeStats = false;
			doStatsAndLoanLimitChecks();
			Stats.FlightSummaryList = Stats.getInstance().getFlightSummary();
			return;
		}
		
		//start time 
		long starttime = System.currentTimeMillis();			
		int cycletype = CycleType30Min;

        Stats.FlightSummaryList = Stats.getInstance().getFlightSummary();


        if(Boolean.getBoolean("Debug"))
            Data.adminApiKey = "ABC123";
        else
            Data.adminApiKey = ServiceProviders.createAccessKey(10);

        logSignatureStats();
		
		doStatsAndLoanLimitChecks();
		doAircraftMaintenance();
		
		//TODO: comment out for test server for faster cycle times
		//Goods cleanup, opens and closes fbo's
	    checkGoodsRecords();
	    processFboStatus();
		processFboRenters();
		processBulkFuelOrders();
		processSupplyOrders();
		
		//TODO: comment out for test server for faster cycle times
		processFboAssignments(); //green assignments
		processTemplateAssignments(); //template assignments
		
		//TODO examine to see if we need to randomize release times on this a bit more
		freeExpiredFBOs();

		if(isOncePerDay())
		{
			cycletype = CycleTypeDaily;
			
			processBankInterest();
			processAircraftCondition();				
			processFboSupplyUsage();

			checkRegistrations();
		}		

		//For Data Feeds
		assignmentsLastUpdate = new Date();

		//update timing info
		doCycleElapsedTimeUpdate(cycletype, starttime, System.currentTimeMillis());
		
		updateStatus("Maintenance cycle finished");							
	}

	void logSignatureStats()
	{
		long defaultCount = Stats.defaultCount;
		long createCount = Stats.createCount;
		long cacheCount = Stats.cacheCount;
		long bytesServed = Stats.bytesServed;
		long totalImagesSent = Stats.totalImagesSent;

		GlobalLogger.logApplicationLog("Signature stats: defaultCount = " + defaultCount + ", createCount = " + createCount + " , cacheCount = " + cacheCount + ", totalImagesSent = " + totalImagesSent + ", totalBytes = " + bytesServed, MaintenanceCycle.class);
	}

	void doAircraftMaintenance()
	{
		createAircraft();
		sellAircraft();

		checkRentalPrices();
		checkAircraftHomes();
		checkAircraftMaintenance();		
		checkAircraftShipping();		
		checkStalledAircraft();		
	}

	void getLastInterestRun()
	{
		updateStatus("Reading sysvariables");
		try
		{
			String qry = "SELECT VarDateTime FROM sysvariables WHERE VariableName = 'LastInterestRun'";
			Timestamp ts = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.TimestampResultTransformer());
			if(ts != null)
			{
                Calendar calendar = new GregorianCalendar();
				calendar.setTime(ts);
				lastDailyRun = calendar.get(Calendar.DAY_OF_WEEK);
			} 
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	void checkGoodsRecords()
	{
	    updateStatus("Cleaning up goods");
	    try
	    {
	    	String qry = "";
	      
	    	qry = qry + "DELETE FROM goods WHERE amount = 0 AND not exists (SELECT * FROM fbo where fbo.owner = goods.owner AND fbo.location = goods.location);";
	    	qry = qry + "UPDATE goods SET amount = amount - LEAST(20, amount) WHERE owner = 0 AND amount > 0;";
	    	qry = qry + "UPDATE goods SET amount = amount + LEAST(20, -amount) WHERE owner = 0 AND amount < 0;";
	      
	    	DALHelper.getInstance().ExecuteBatchUpdate(qry);
	    }
	    catch (SQLException e)
	    {
	    	e.printStackTrace();
	    }
	}
	  
	void processFboStatus()
	{
	    updateStatus("Cleaning up goods");
	    try
	    {
	    	String qry = "";
	      
	      	qry = qry + "UPDATE fbo SET active = 1, inactivesince = NULL WHERE active = 0 AND EXISTS (SELECT * FROM goods WHERE fbo.owner = goods.owner AND fbo.location = goods.location AND type = 2 AND amount >= 10);";
	      	qry = qry + "UPDATE fbo SET active = 0, inactivesince = current_timestamp WHERE active = 1 AND NOT EXISTS (SELECT * FROM goods WHERE fbo.owner = goods.owner AND fbo.location = goods.location AND type = 2 AND amount >= 10);";
	      
	      	DALHelper.getInstance().ExecuteBatchUpdate(qry);
	    }
	    catch (SQLException e)
	    {
	    	e.printStackTrace();
	    }
	}
	  
	void doStatsAndLoanLimitChecks()
	{
		updateStatus("Gathering statistics");
		
		ResultSet rs;
		
		try
		{
			List<Statistics> stats = new ArrayList<>();
			
			// Overall total stats
			String qry = "SELECT sum(flightenginetime), sum(distance), sum(income) FROM log";
			rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			if(rs.next())
			{
                Stats.getInstance().setMinutesFlown(rs.getLong(1) / 60);
                Stats.getInstance().setMilesFlown(rs.getLong(2));
                Stats.getInstance().setTotalIncome((long) rs.getDouble(3));
			}
			
			Map<Integer, Set<AircraftBean>> aircraft = new HashMap<>();

			qry = "SELECT aircraft.*, models.* from aircraft, models WHERE owner > 0 AND aircraft.model = models.id";
			rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				AircraftBean thisAircraft = new AircraftBean(rs);
				Integer iOwner = thisAircraft.getOwner();
				Set<AircraftBean> ownerSet = aircraft.get(iOwner);
				
				if (ownerSet == null)
				{
					ownerSet = new HashSet<>();
					aircraft.put(iOwner, ownerSet);
				}
				
				ownerSet.add(thisAircraft);
			}

			StringBuilder idSet = new StringBuilder();

			HashMap<Integer, UserBean> usersById = new HashMap<>();
			//HashMap<String, UserBean> usersByName = new HashMap<>();

			qry = "SELECT * from accounts WHERE accounts.id > 0";
			rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				UserBean user = new UserBean(rs);
				usersById.put(user.getId(), user);
				//usersByName.put(user.getName().toLowerCase(), user);
			}
			
			HashMap<Integer, String> ownersByGroup = new HashMap<>();

			qry = "SELECT name, groupId from groupmembership, accounts where groupmembership.level='owner' and userId = accounts.id";
			rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
				ownersByGroup.put(rs.getInt(2), rs.getString(1));
			
			qry = "SELECT userid, count(log.userid), sum(distance), sum(flightEngineTime), min(time) from log WHERE log.type = 'flight' group by userid";
			rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				UserBean thisUser = usersById.get(rs.getInt(1));
				if (thisUser == null)
					continue;
				
				if ((thisUser.getExposure() & UserBean.EXPOSURE_SCORE) > 0)
					stats.add(new net.fseconomy.dto.Statistics(thisUser.getId(), thisUser.getName(), null, (int)Math.round(thisUser.getMoney() + thisUser.getBank()), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getTimestamp(5), aircraft.get(thisUser.getId()), false));
				
				//Loan check, if 10 flights and current limit is 0, add name to update list
				if (thisUser.getLoanLimit() == 0 && rs.getInt(2) >= 10)
				{
					if (idSet.length() > 0)
						idSet.append(",");

					idSet.append(thisUser.getId());
				}
			}

			qry = "SELECT groupId, count(log.userid), sum(distance), sum(flightEngineTime), min(time) from log WHERE log.groupid is not null AND log.type = 'flight' group by groupId";
			rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				UserBean thisUser = usersById.get(new Integer(rs.getInt(1)));
				if (thisUser == null)
					continue;
				String owner = ownersByGroup.get(new Integer(rs.getInt(1)));
				if ((thisUser.getExposure() & UserBean.EXPOSURE_SCORE) > 0)
					stats.add(new Statistics(thisUser.getId(), thisUser.getName(), owner, (int)Math.round(thisUser.getMoney() + thisUser.getBank()), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getTimestamp(5), aircraft.get(thisUser.getId()), true));
			}

			qry = "SELECT id FROM accounts WHERE type='group' AND NOT EXISTS (SELECT * FROM log WHERE log.groupid = accounts.id) order by name";
			rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				UserBean thisUser = usersById.get(new Integer(rs.getInt(1)));
				if (thisUser == null)
					continue;
				String owner = ownersByGroup.get(new Integer(rs.getInt(1)));
				if ((thisUser.getExposure() & UserBean.EXPOSURE_SCORE) > 0)
					stats.add(new Statistics(thisUser.getId(), thisUser.getName(), owner, (int)Math.round(thisUser.getMoney() + thisUser.getBank()), 0, 0, 0, null, aircraft.get(thisUser.getId()), true));
			}

            Collections.sort(stats, (i1, i2) -> (i1.accountName.compareTo(i2.accountName)));

            Stats.statistics = stats;

			HashMap<String, Statistics> hm = new HashMap<>();
			for( net.fseconomy.dto.Statistics s : stats)
				hm.put(s.accountName.toLowerCase(), s);


            Stats.prevstatsmap = Stats.statsmap;
            Stats.statsmap = hm;
			
			if (idSet.length() > 0)
			{
				qry = "UPDATE accounts SET `limit` = 40000 WHERE id in (" + idSet.toString() + ")";
				DALHelper.getInstance().ExecuteUpdate(qry);
			}				
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		updateStatus("Stats finished");
	}
	
	/**
	* Sets random 4-digit aircraft condition
	* Called from processInterest() once a day
	*/
	void processAircraftCondition()
	{
		updateStatus("Setting Aircraft Condition");

		try
		{
			String qry = "SELECT * FROM aircraft";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				int condition = rs.getInt("condition");
				
				//if aircraft is not broken or if it is owned by the bank whether broken or not
				if (condition > AircraftBean.REPAIR_RANGE_HIGH || rs.getInt("owner") == 0)
				{
					int randomCondition = (int) (50000 + (59999 - 50000) * Math.random());

					qry = "UPDATE aircraft SET `condition` = ? WHERE registration = ?";						
					DALHelper.getInstance().ExecuteUpdate(qry, randomCondition, rs.getString("registration"));
				}
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	void processBankInterest()
	{
	    updateStatus("Processing bank interest");
	    try
	    {
	    	GregorianCalendar today = new GregorianCalendar();
	    	int day = today.get(Calendar.DAY_OF_MONTH);
	    	if (1 == day)
	    	{
	    		String qry = "UPDATE accounts SET bank = bank + interest WHERE type = 'person' AND exposure > 0";
	    		DALHelper.getInstance().ExecuteUpdate(qry);
	        

	    		int counter = 0;
	    		BigDecimal totalPaid = new BigDecimal(0.00);
	    		
	    		qry = "select id, interest from accounts WHERE type = 'person' AND exposure > 0 AND interest > 0.00";
	    		ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
	    		while (rs.next())
	    		{
	    			Money interest = new Money(rs.getBigDecimal("interest").doubleValue());
                    Banking.addPaymentRecord(rs.getInt("id"), 0, interest, PaymentBean.INTEREST_PAYMENT, 0, 0, "", 0, "");

	    			totalPaid = totalPaid.add(rs.getBigDecimal("interest"));
	    			counter++;
	    		}
	    		
	    		qry = "UPDATE accounts SET interest = 0.00 where type = 'person'";
	    		DALHelper.getInstance().ExecuteUpdate(qry);

				GlobalLogger.logApplicationLog("Interest Payments: Total Payout - " + totalPaid.toString() + " to " + counter + " pilots", MaintenanceCycle.class);
	    	}
	    	
	    	//1 million balance cap - no additional interest on balances over 1 million
			//exclude group accounts - no interest paid to group accounts
	    	String qry = "UPDATE accounts SET interest = interest + ROUND(LEAST((LEAST(money, 0) + bank), 1000000) * (case when (LEAST(money, 0) + bank) < 0 then " + this.interestNegative + "when (LEAST(money, 0) + bank) > 0 then " + this.interestPositive + " else 1 end), 2) where type = 'person' and exposure > 0";
	    	DALHelper.getInstance().ExecuteUpdate(qry);
	      
	    	qry = "UPDATE sysvariables SET VarDateTime = Now() WHERE VariableName = 'LastInterestRun'";
	    	DALHelper.getInstance().ExecuteUpdate(qry);
	    }
	    catch (SQLException e)
	    {
	    	e.printStackTrace();
	    }
	}	  
	
	/**
	 * Calculate Supply usage
	 * 
	 * This is kind of bass ackwards in that it queries based upon the goods table versus
	 * owned FBOs. The 'assumption' is that there is a goods entry for all owned facilities
	 * so if that is not the case then those facilities would never use supplies.
	 */
	void processFboSupplyUsage()
	{
		updateStatus("Processing fbo supply usage");

		try
		{
			int suppliesKG = GoodsBean.AMOUNT_FBO_SUPPLIES_PER_DAY;  

			String qry = 									
				" UPDATE goods, (SELECT goods.type, goods.location, goods.owner, case when airports.size < " + AirportBean.MIN_SIZE_MED + " then 1*fbo.fbosize when airports.size < " + AirportBean.MIN_SIZE_BIG + " then 2*fbo.fbosize else 3*fbo.fbosize end size FROM goods, fbo, airports " +
				" WHERE  goods.type=" + GoodsBean.GOODS_SUPPLIES + " AND fbo.location = goods.location AND fbo.owner = goods.owner AND airports.icao = goods.location) t "+
				" Set amount = amount - (" + suppliesKG + " * t.size) "+
				" where goods.type=t.type AND goods.location=t.location AND goods.owner=t.owner ";
			DALHelper.getInstance().ExecuteUpdate(qry);				
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	void freeExpiredFBOs()		
	{
		updateStatus("Freeing FBOs > 45 Days Inactive");
		
		try
		{
			String qry = "select * from fbo where inactivesince < date_sub(curdate(), interval 45 day)";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while(rs.next())
			{
				qry = "delete from goods where owner= ? and location= ? and type= ?";
				DALHelper.getInstance().ExecuteUpdate(qry, rs.getInt("owner"), rs.getString("location"), GoodsBean.GOODS_SUPPLIES);
				
				Goods.resetAllGoodsSellBuyFlag(rs.getInt("owner"), rs.getString("location"));

				qry = "delete from fbofacilities where fboid = ?";
				DALHelper.getInstance().ExecuteUpdate(qry, rs.getInt("id"));

				qry = "delete from fbo where id = ?";
				DALHelper.getInstance().ExecuteUpdate(qry, rs.getInt("id"));					
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	Set<String> parseIcaoSet(String icaos, boolean allowMacros, int templateId)
	{
		if (icaos == null)
			return null;
		
		String items[] = icaos.toUpperCase().trim().split(", *");
		
		if (items.length == 0)
			return null;
		
		Set<String> airports = new HashSet<>();
		Set<String> M = new HashSet<>();
		String item;

        for (String item1 : items)
        {
            item = item1.trim();

            if (allowMacros && item.startsWith("$"))
			{
				M.add(item);
			}
            else
			{
				if(Airports.isValidIcao(item))
					airports.add(item);
				else
					GlobalLogger.logApplicationLog("Bad ICAO in template " + templateId + ": " + item, MaintenanceCycle.class);
			}
        }
		
		if (!M.isEmpty())
		{
			//String macros[] = M.toArray(new String[M.size()]);
			try
			{
				String sql;
                for (String macro : M)
                {
                    sql = null;
                    switch (macro)
                    {
                        case "$FBO":
                            sql = "SELECT DISTINCT f.location as icao FROM fbo f WHERE f.active = 1";
                            break;
                        case "$WATER":
                            sql = "SELECT icao FROM airports where type = 'water'";
                            break;
                        case "$MILITARY":
                            sql = "SELECT icao FROM airports where type = 'military'";
                            break;
                    }

                    if (sql != null)
                    {
                        ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(sql);
                        while (rs.next())
                            airports.add(rs.getString(1));
                    }
                }
			} 
			catch (SQLException e)
			{
				e.printStackTrace();
			} 
		}
		
		if (airports.isEmpty())
			airports = null;
		
		return airports;
	}
	
	void processTemplateAssignments()
	{
		try
		{
			deleteExpiredTemplateAssignments();

			//We are caching here several of the bigger ICAO lists created by the template
			//macros $FBO, and $MILITARY so that its called only once per cycle, instead of
			//for each template that uses them
			
			//Get the $FBO ICAOs and save them
			Set<String> icaosetFBO = parseIcaoSet("$FBO", true, 0);
			StringBuffer whereSetFBO = new StringBuffer();
			if (icaosetFBO != null)
			{
				String tempicaos[] = icaosetFBO.toArray(new String[icaosetFBO.size()]);
                for (String tempicao : tempicaos)
                {
                    if (whereSetFBO.length() > 0)
                        whereSetFBO.append(", ");

                    whereSetFBO.append("'").append(tempicao).append("'");
                }
			}

			//get the $MILITARY ICAOs and save them
			Set<String> icaosetMilitary = parseIcaoSet("$MILITARY", true, 0);
			StringBuffer whereSetMILITARY = new StringBuffer();
			if (icaosetMilitary != null)
			{
				String tempicaos[] = icaosetMilitary.toArray(new String[icaosetMilitary.size()]);
                for (String tempicao : tempicaos)
                {
                    if (whereSetMILITARY.length() > 0)
                        whereSetMILITARY.append(", ");

                    whereSetMILITARY.append("'").append(tempicao).append("'");
                }
			}
			
			String qry = "SELECT * from templates";
			ResultSet rsTemplate = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rsTemplate.next())
			{
				TemplateBean template = new TemplateBean(rsTemplate);
				int templateId = rsTemplate.getInt("id");

				updateStatus("Working on assignments for template " + templateId);

				double frequency = rsTemplate.getDouble("frequency");
				int maxDistance = rsTemplate.getInt("targetDistance");

				//if its a dead template, skip it
				if (frequency == 0 || maxDistance == 0)
					continue;

				double targetPay = rsTemplate.getDouble("targetPay");
				double Deviation = maxDistance * ((double) rsTemplate.getInt("distanceDev") / 100.0);
				double amountDev = rsTemplate.getInt("amountDev") / 100.0;
				double payDev = (double) rsTemplate.getInt("payDev") / 100.0;

				int keepAlive = rsTemplate.getInt("targetKeepAlive");
				boolean noExt = rsTemplate.getBoolean("noext");
				int targetAmount = rsTemplate.getInt("targetAmount");
				int maxSize = rsTemplate.getInt("matchMaxSize");
				int minSize = rsTemplate.getInt("matchMinSize");
				int surfType = rsTemplate.getInt("allowedSurfaceTypes");

				List<AircraftBean> allInAircraft = null;
				boolean isAllIn = rsTemplate.getString("typeOfPay").equals("allin");
                boolean direct = rsTemplate.getBoolean("direct");
				boolean waterOk = !isAllIn;

				String commodity = rsTemplate.getString("commodity");
				String units = rsTemplate.getString("units");
				String icaos1 = rsTemplate.getString("icaoSet1") != null ? rsTemplate.getString("icaoSet1").toUpperCase() : null;
				String icaos2 = rsTemplate.getString("icaoSet2") != null ? rsTemplate.getString("icaoSet2").toUpperCase() : null;

				int seatsFrom = rsTemplate.getInt("seatsFrom");
				int seatsTo = rsTemplate.getInt("seatsTo");
				int speedFrom = rsTemplate.getInt("speedFrom");
				int speedTo = rsTemplate.getInt("speedTo");

				boolean isFilterByModel = rsTemplate.getBoolean("modelfilter");
				String filterModels = rsTemplate.getString("modelset");


				StringBuffer where = new StringBuffer();
				Set<String> icaoSet1 = null;
				Set<String> icaoSet2 = null;

				boolean noReversal = false;
				boolean singleIcao = false;

				//make sure that From icaos do not contain these tags
				if(icaos1 != null)
				{
					if (icaos1.contains("$NOREVERSE"))
					{
						icaos1 = icaos1.replace("$NOREVERSE", "").trim();
					}
					if (icaos1.contains("$SINGLE"))
					{
						icaos1 = icaos1.replace("$SINGLE", "").trim();
					}
				}

				//if Dest icaos contain these tags, set the flags
				if(icaos2 != null)
				{
					if (icaos2.contains("$NOREVERSE"))
					{
						noReversal = true;
						icaos2 = icaos2.replace("$NOREVERSE", "").trim();
					}
					if (icaos2.contains("$SINGLE"))
					{
						singleIcao = true;
						icaos2 = icaos2.replace("$SINGLE", "").trim();
					}
				}

				if (isAllIn)
				{
					String whatModels = "";

					// check for seats and cruise speed filters on aircraft assignment for template
					// if no filters are set for seats size filter, just add a condition to be bigger then the units specified in the job
					// this will ensure a 172 is not chosen to take a 10 pax job
					// if filter values are set those are used instead in the where clause

					if(isFilterByModel) //use specified models
					{
						whatModels = filterModels;
						if(whatModels == "")
						{
							GlobalLogger.logApplicationLog("Error: Template " + templateId + ", missing aircraft models!", MaintenanceCycle.class);
							continue;
						}
					}
					else if (seatsFrom != 0 && seatsTo != 0 && speedFrom !=  0 && speedTo != 0) //use to/from values
					{	//filters set
						whatModels = "select t.id from (select id from models where  seats between " + seatsFrom + " and " + seatsTo + " and cruisespeed  between " + speedFrom + " and " + speedTo + ") as t";
					}
					else //no filters set, log template error
					{
						GlobalLogger.logApplicationLog("Error: Template " + templateId + ", missing To/From values!", MaintenanceCycle.class);
						continue;
					}

					double multipler = 0;
					if (units.equals("passengers"))
						multipler = 77;

					icaoSet1 = new HashSet<>();

					qry = "SELECT * FROM aircraft, models WHERE  owner=0 AND location is not null AND userlock is null"
					+ " AND aircraft.model=models.id"
					+ " AND aircraft.model in (" + whatModels + ")"
					+ " AND (emptyWeight + ((aircraft.fueltotal *  models.fcaptotal) * 2.68735) ) + (crew * 77) + " + (targetAmount * multipler) + " < maxWeight"
					+ " AND aircraft.id not in( select * from (select aircraftid from assignments where aircraftid is not null) as t)";

					allInAircraft = Aircraft.getAircraftSQL(qry);

					for(AircraftBean a: allInAircraft)
						icaoSet1.add(a.getLocation());
				}
				else if (icaos1 != null)
                {
                    switch (icaos1)
                    {
                        case "$FBO":
                            icaoSet1 = icaosetFBO;
                            where = whereSetFBO;
                            frequency = frequency * icaoSet1.size();
                            break;
                        case "$MILITARY":
                            icaoSet1 = icaosetMilitary;
                            where = whereSetMILITARY;
                            frequency = frequency * icaoSet1.size();
                            break;
                        default:
                            icaoSet1 = parseIcaoSet(icaos1, true, templateId);

                            if (icaoSet1 != null)
                            {
                                String tempicaos[] = icaoSet1.toArray(new String[icaoSet1.size()]);
                                for (String tempicao : tempicaos)
                                {
                                    if (where.length() > 0)
                                        where.append(", ");

                                    where.append("'").append(tempicao).append("'");
                                }
                                if (icaos1.contains("$"))
                                    frequency = frequency * icaoSet1.size();
                            }
                            break;
                    }
                }

				if(icaos2 != null)
                {
                    switch (icaos2)
                    {
                        case "$FBO":
                            icaoSet2 = icaosetFBO;
                            break;
                        case "$MILITARY":
                            icaoSet2 = icaosetMilitary;
                            break;
                        default:
                            icaoSet2 = parseIcaoSet(icaos2, true, templateId);
                            break;
                    }
                }

				String query;
				String needed;
				if (icaoSet1 == null && icaoSet2 == null)
				{
					String having = "needed > got";
					if (maxSize > 0)
						having = having + " AND smallest < " + maxSize;
					
					if (minSize > 0)
						having = having + " AND largest > " + minSize;

					if(surfType != 0)
					{
						String sSurfaceTypes = BitSet.valueOf(new long[]{(long) surfType}).toString();
						sSurfaceTypes = sSurfaceTypes.replace("{", "").replace("}", "");

						having = having + " AND surfaceType in (" + sSurfaceTypes + ")";
					}
					needed = frequency + " * sqrt(sum(size)/6000) as needed";
					query = "SELECT bucket, min(longestRwy) AS smallest, max(longestRwy) AS largest, " + needed +
						 ", count(assignments.id) AS got, avg(lat), surfaceType FROM airports LEFT join assignments ON assignments.fromicao = airports.icao AND fromtemplate = " +
						 templateId + " GROUP by bucket HAVING " + having;
				} 
				else
				{			
					String whereString = "";			
					
					if (where.length() > 0)
						whereString = whereString + "WHERE icao in (" + where.toString() + ")";
					
					if(isAllIn)
						needed = icaoSet1.size() * frequency + " as needed";
					else
						needed = frequency + " as needed";

					query = "SELECT 0, 0, 0, " + needed + ", count(assignments.id) AS got, avg(lat) FROM airports LEFT JOIN assignments ON assignments.fromicao = airports.icao AND fromtemplate = " + templateId + " " + whereString ;
				}

				ResultSet bucketRs = DALHelper.getInstance().ExecuteReadOnlyQuery(query);
				while (bucketRs.next())
				{
					int bucket = bucketRs.getInt(1);
					double dTogo = bucketRs.getDouble(4) - bucketRs.getInt(5); 
					double latitude = bucketRs.getDouble(6);

					int togo;
					List<String> airportFromList;
					
					if (dTogo <= 0 || (dTogo < 1 && Math.random() > dTogo))
						continue;
					
					togo = (int) Math.max(1, dTogo);

					if (icaoSet1 != null || icaoSet2 != null)
					{
						airportFromList = new ArrayList<>(icaoSet1);
					} 
					else
					{							
						where = new StringBuffer("bucket = " + bucket);
						
						if (waterOk)
							where.append(" AND type in ('civil','water')");
						else
							where.append(" AND type='civil'");
						
						if (maxSize > 0)
							where.append(" AND longestRwy < ").append(maxSize);
						
						if (minSize > 0)
							where.append(" AND longestRwy > ").append(minSize);

						if(surfType != 0)
						{
							String sSurfaceTypes = BitSet.valueOf(new long[]{(long) surfType}).toString();
							sSurfaceTypes = sSurfaceTypes.replace("{", "").replace("}", "");

							where.append (" AND surfaceType in (" + sSurfaceTypes + ")");
						}

						airportFromList = new ArrayList<>();

						qry = "SELECT icao FROM airports WHERE  " + where.toString();
						ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
						while (rs.next())
							airportFromList.add(rs.getString(1));
					}
					
					if (airportFromList.isEmpty())
						continue;
					
					while (togo-- > 0)
					{
						String icao = airportFromList.get((int)(Math.random() * airportFromList.size()));
							
						int cargoAmount = (int)(targetAmount * (1 + (Math.random() * 2*amountDev) - amountDev));
						if (cargoAmount == 0)
							cargoAmount = 1;
						
						double pay = targetPay * (1 + (Math.random() * 2*payDev) - payDev);

						CloseAirport to;
						if(singleIcao)
						{
							String target = icaoSet2.iterator().next().toString();
							String airports[] = icaoSet1.toArray(new String[icaoSet1.size()]);

							//the min/max distance criteria
							List<CloseAirport> inRange = new ArrayList<>();
							for (String airport : airports)
							{
								DistanceBearing distanceBearing = Airports.getDistanceBearing(airport, target);
								if (distanceBearing != null &&
										distanceBearing.distance != 0 &&
										(distanceBearing.distance >= (maxDistance - Deviation) && distanceBearing.distance <= maxDistance))
								{
									inRange.add(new CloseAirport(airport, distanceBearing.distance, distanceBearing.bearing));
								}
							}
							//System.out.println("singleIcao found: " + inRange.size() + ", out of: " + airports.length);

							Optional<CloseAirport> opt = inRange.stream().filter(p -> p.icao.contains(icao) ).findFirst();
							if(opt.isPresent())
							{
								to = opt.get();
								to.icao = target;
							}
							else
								to = null;
						}
						else
							to = Airports.getRandomCloseAirport(icao, maxDistance - Deviation, maxDistance + Deviation, minSize, maxSize, latitude, icaoSet2, waterOk, surfType);

						if (to == null)
							continue;
	
						int aircraftId = 0;
						if (isAllIn) 
						{
							//All-In change - find available aircraft at airport that have not already been assigned to an All-In job
							List<AircraftBean> acList = allInAircraft.stream().filter(p -> p.getLocation().contains(icao) ).collect(Collectors.toList());

							//if no aircraft skip this one
							if(acList.size() == 0)
								continue;

							int index = 0; //default aircraft selection is the first one

							//if more then 1 airplane available randomly select
							if(acList.size() > 1)
							{
								Random rnd = new Random();
								index = rnd.nextInt(acList.size());
							}

							AircraftBean selected = acList.get(index);
							aircraftId = selected.getId();

							//remove so not selected again
							allInAircraft.remove(selected);
						}
						
						int distance = (int) Math.round(to.distance);
						int bearing = (int) Math.round(to.bearing);
						
						String fromIcao;
						String toIcao;

						if (isAllIn || noReversal || Math.random() < 0.5) // never reverse an AllIn flight
						{
							fromIcao = icao;
							toIcao = to.icao;
						} 
						else 
						{
							// reverse the flight
							fromIcao = to.icao;
							toIcao = icao;
							if (bearing < 180)
								bearing += 180;
							else
								bearing -= 180;
						}
						
						Timestamp now = new Timestamp(System.currentTimeMillis());				
						Calendar expires = GregorianCalendar.getInstance();
						expires.add(GregorianCalendar.DAY_OF_MONTH, keepAlive);
						expires.add(GregorianCalendar.HOUR, (int)(Math.random() * 24 - 12));

						StringBuilder fields = new StringBuilder();
						StringBuilder values = new StringBuilder();
						
						fields.append("bearing, creation, expires, commodity, units, amount, fromicao, location, toicao, distance, pay, fromTemplate, noext");
						values.append("").append(bearing);
						values.append(", '").append(now).append("'");
						values.append(", '").append(new Timestamp(expires.getTime().getTime())).append("'");
						values.append(", '").append(Converters.escapeSQL(template.getRandomCommodity(cargoAmount))).append("'");
						values.append(", '").append(units).append("'");
						values.append(", ").append(cargoAmount);
						values.append(", '").append(fromIcao).append("'");
						values.append(", '").append(fromIcao).append("'");
						values.append(", '").append(toIcao).append("'");
						values.append(", ").append(distance);
						values.append(", ").append((float) pay);
						values.append(", ").append(templateId);
						values.append(", ").append(noExt ? "1" : "0");

						if (isAllIn)
						{
							fields.append(", aircraftid");
                            fields.append(", direct");
							values.append(", '").append(aircraftId).append("'");
                            values.append(", '").append(direct ? "1" : "0").append("'");
						}
						
						qry = "INSERT INTO assignments (" + fields.toString() + ") VALUES(" + values.toString() + ")";
						DALHelper.getInstance().ExecuteUpdate(qry);
                    }
				}
			}

			assignmentsPerTemplate = new HashMap<>();

			qry = "SELECT fromtemplate, count(*) as count, count(assignments.groupId) as grouplock, max(pay * amount * (distance / 100.0)) as pmax, min(pay * amount * (distance / 100.0)) as pmin, avg(pay * amount * (distance / 100.0)) as pmean from assignments group by fromtemplate";
			ResultSet rsTemplatesFound = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rsTemplatesFound.next())
			{
				Integer[] ia = new Integer[5];
				ia[ASSGN_COUNT] = rsTemplatesFound.getInt(2);
                ia[ASSGN_GLOCKED] = rsTemplatesFound.getInt(3);
				ia[ASSGN_MAX] = (int) (rsTemplatesFound.getDouble(4) + .5);
				ia[ASSGN_MIN] = (int) (rsTemplatesFound.getDouble(5) + .5);
				ia[ASSGN_AVG] = (int) (rsTemplatesFound.getDouble(6) + .5);
				
				assignmentsPerTemplate.put(rsTemplatesFound.getInt(1), ia );
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	private void deleteExpiredTemplateAssignments() throws SQLException
	{
		// Log assignments to be deleted then:
		// Delete unmoved unlocked expired assignments
		// Delete unmoved expired assignments after 1 day
		// Delete moved expired assignments after 45 days
		String qry =
			"INSERT INTO templatelog (created, expires, templateid, fromicao, toicao, pay, payee) "
			+ "select creation, expires, fromtemplate, fromicao, toicao, 0, 0 FROM assignments "
			+ "WHERE (fromFboTemplate is null and active = 0 AND userlock is null AND groupId is null AND location = fromicao AND expires is not null AND now() > expires)"
			+ " OR (fromFboTemplate is null AND active = 0 AND userlock is null AND groupId is null AND noext=1 AND expires is not null AND now() > expires)"
			+ " OR  (fromFboTemplate is null and active <> 1 AND location = fromicao AND expires is not null AND DATE_SUB(now(), INTERVAL 1 DAY) > expires)"
			+ "	OR (fromFboTemplate is null and active <> 1 AND noext=1 AND expires is not null AND DATE_SUB(now(), INTERVAL 1 DAY) > expires)"
			+ " OR (fromFboTemplate is null and active <> 1 AND expires is not null AND DATE_SUB(now(), INTERVAL " + ASSGN_EXT_DAYS + " DAY) > expires);"

			+ " DELETE FROM assignments WHERE "
			+ "    (fromFboTemplate is null AND active = 0 AND userlock is null AND groupId is null AND location = fromicao AND expires is not null AND now() > expires)"
			+ " OR (fromFboTemplate is null AND active = 0 AND userlock is null AND groupId is null AND noext=1 AND expires is not null AND now() > expires)"
			+ "	OR (fromFboTemplate is null and active <> 1 AND location = fromicao AND expires is not null AND DATE_SUB(now(), INTERVAL 1 DAY) > expires)"
			+ "	OR (fromFboTemplate is null and active <> 1 AND noext=1 AND expires is not null AND DATE_SUB(now(), INTERVAL 1 DAY) > expires)"
			+ "	OR (fromFboTemplate is null and active <> 1 AND expires is not null AND DATE_SUB(now(), INTERVAL " + ASSGN_EXT_DAYS + " DAY) > expires);";

		DALHelper.getInstance().ExecuteBatchUpdate(qry);
	}

	void processFboRenters()
	{
		try
		{
			int RentDayOfMonth = 1;
			
			String qry = "select r.id, r.occupant, r.fboId, r.location, r.size, o.occupant as owner, o.rent, f.active " +
						 	"from fbofacilities r " +
							"join fbo f on f.id = r.fboId " +
							"join fbofacilities o on o.fboId = r.fboId and o.reservedSpace >= 0 and o.allowRenew = 1 " +
							"where r.reservedSpace < 0 and r.renew = 1 and r.allowRenew = 1 and r.lastRentPayment < date_add(current_date, interval -day(current_date) + ? day)";				
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, RentDayOfMonth);			
			while (rs.next())
			{
				int occupant = rs.getInt("occupant");
				int rent = rs.getInt("rent") * rs.getInt("size");
				if (rs.getInt("active") > 0)
				{
					if (Banking.checkAnyFunds(occupant, (double) rent))
					{
                        Banking.doPayment(occupant, rs.getInt("owner"), (double) rent, PaymentBean.FBO_FACILITY_RENT, 0, rs.getInt("fboId"), rs.getString("location"), 0, "", false);
						
						qry = "update fbofacilities set lastRentPayment = current_timestamp where id = ?";
						DALHelper.getInstance().ExecuteUpdate(qry, rs.getInt("id"));
					}
				}
			}
		
			// Delete delinquent Renters
			qry = "delete from fbofacilities where reservedSpace < 0 and lastRentPayment < date_add(current_date, interval -day(current_date) + ? day)";
			DALHelper.getInstance().ExecuteUpdate(qry, RentDayOfMonth);
		}
		catch(SQLException e)
		{		
			e.printStackTrace();
		}			
	}
	
	void deleteExpiredFboAssignments()
	{
		// NOTE: Adjust the extratime values in AssignmentBean.getSExpires() if changing these queries.
		
		// Delete unmoved unlocked expired assignments
		String qry = "DELETE FROM assignments WHERE fromFboTemplate is not null and active = 0 AND userlock is null AND groupId is null AND expires is not null AND now() > expires;";
		// Delete locked expired assignments after 24hrs
		qry = qry + "DELETE FROM assignments WHERE fromFboTemplate is not null and active <> 1 AND expires is not null AND DATE_SUB(now(), INTERVAL 1 DAY) > expires;";

		try
		{
			DALHelper.getInstance().ExecuteBatchUpdate(qry);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	void processFboAssignments()
	{
		try
		{
			deleteExpiredFboAssignments();
			
			int ChancesPerDay = 6;
			boolean oneAssignmentPerLoop = true;
			int keepAlive;
			int fboLoopCounter = 0;
			
			String qry = 
				"Select t.* from " +
				"(select rand() as chance, f.id, f.fbosize from fbo f where f.active = 1)  t " + 
				"where t.chance < ?/48 " +
				"order by t.chance, t.id;";
			CachedRowSet rsFBOs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, ChancesPerDay);
			
			//Get the FBO count
			int fboCount = rsFBOs.size();

			while (rsFBOs.next())
			{
				fboLoopCounter += 1;
				boolean flgNoGatesRented = false;
				
				int fboJobs = 1; //default is one job
				int fboSize = rsFBOs.getInt("fbosize");

				if(fboSize > 1)
				{
					fboJobs = (int)(Math.random() * fboSize + .5);
					if( fboJobs < 1 )
						fboJobs = 1; // minimum is one job
				}			
				
				qry = "Select t.* from " + 
						"(select rand() as chance, ff.* from fbofacilities ff where fboid = ?) t " + 
						" order by t.chance, t.id " +
						" limit 0,?;";
				CachedRowSet rsFacilities = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, rsFBOs.getInt("id"), fboSize);
			
				//Get the facility count
				int facCount = rsFacilities.size();

				if( facCount <= 0)
					continue;
				
				for(int i = 0; i < fboJobs; i++ )
				{
					if( i == 0 )
					{
						rsFacilities.next();
						
						//Check if only a single facility is returned, assume the owner reserved all gates
						if( rsFacilities.getInt("reservedspace") == fboSize*3)
							flgNoGatesRented = true;
					}
					else
					{
						if( !flgNoGatesRented )
						{
							//Get the next record
							rsFacilities.next();
							
							//if we have moved past the last record, reuse the last
							//this handles larger airfields that might only have 2 facilities but 3 jobs generated
							if( rsFacilities.isAfterLast())
							{
								rsFacilities.last();
							}
						}
					}
					
					//Get the current facility data
					FboFacilityBean template = new FboFacilityBean(rsFacilities);
					
					//user-set # of days to set for initial PT job expiration
					//Added minimum live time is 1 day
					keepAlive = template.getDaysActive() <= 0 ? 1 : template.getDaysActive();
					int id = template.getId();

					updateStatus("Working on FBO Assignments for template " + id + " Processing " + fboLoopCounter + " of " + fboCount + " FBO's");

					FboBean fbo = Fbos.getFbo(template.getFboId());

					//This section will either use the facility size if player rented
					//or if the fbo own has reserved all gates or none have rented (size==0)
					//then the available units allowed are calculated
					int unitsAllowed = template.getSize();
					if (unitsAllowed == 0) //owner reserved or none rented
					{
						// small - 1*3, medium - 2*3, large - 3*3
						unitsAllowed = fbo.getFboSize() * Airports.getTotalFboSlots(fbo.getLocation());
						
						//see if any gates are rented (reservedspace == -1)
						qry = "select sum(size) from fbofacilities where reservedSpace < 0 and fboId = ?";
						ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, template.getFboId());

						//remove rented gate counts if any found
						if (rs.next())
							unitsAllowed -= rs.getInt(1);
					}

					//compute total pax count at 3 per gate
					unitsAllowed *= 3;
					
					//Go to next winner if no available paxs, this could occur if owner facility, and all gates rented
					if (unitsAllowed <= 0)
						continue;

//TODO minUnits needs to be looked at					
					//this is always going to be 0 if I read the code right
					int minUnits;
					
					//this is going to be the pax upper limit set by the gate owner/renter
					int maxUnits = template.getMaxUnitsPerTrip();
					if (maxUnits <= 0)
						maxUnits = unitsAllowed;
					
					//Why is this set to half the max value?
					//If the gate is set to 10, the smallest value is going to be 5
					minUnits = maxUnits / 2;     // Bean minUnits setting is ignored.
					
					int minDistance = template.getMinDistance();
					int maxDistance = template.getMaxDistance();
					
					if (minDistance > maxDistance || minDistance < 0)
						minDistance = 0;
					
					if (minDistance > maxDistance || maxDistance > FboFacilityBean.MAX_ASSIGNMENT_DISTANCE)
						maxDistance = FboFacilityBean.MAX_ASSIGNMENT_DISTANCE;
					
					int minSize = template.getMatchMinSize();
					int maxSize = template.getMatchMaxSize();
					
					if (minSize > maxSize || minSize < 0)
						minSize = 0;
					
					if (minSize > maxSize || maxSize > 99999)
						maxSize = 99999;
					
					String icaos = template.getIcaoSet();
					if (icaos != null && "".equals(icaos))
						icaos = null;
					
					Set<String> icaoSet2 = parseIcaoSet(icaos, false, 0);
					if (icaoSet2 != null && !icaoSet2.isEmpty())
					{
						minDistance = 0;
						maxDistance = FboFacilityBean.MAX_ASSIGNMENT_DISTANCE;
						minSize = 0;
						maxSize = 99999;
					}
					
					qry = "select sum(amount) FROM assignments where fromFboTemplate = ?"; 
					int jobsFound = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), id);

					int togo = unitsAllowed - jobsFound; 
					
					while (togo > 0)
					{
						if (maxUnits > togo)
							maxUnits = togo;
						
						if (minUnits >= maxUnits)
							minUnits = maxUnits / 2;

						String icao = template.getLocation();
						
						int cargoAmount = minUnits + (int)(Math.random() * (1 + (maxUnits - minUnits)));
						
						if (cargoAmount == 0)
							cargoAmount = 1;
						
						togo -= cargoAmount;
						
						qry = "select avg(lat) FROM assignments LEFT join  airports ON fromicao = airports.icao where fromFboTemplate = ?"; 
						double lat = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), id);
						
						CloseAirport to = Airports.getRandomCloseAirport(icao, minDistance, maxDistance, minSize, maxSize, lat, icaoSet2, template.getAllowWater(), 0);
						if (to == null)
							continue;
	
						int distance = (int) Math.round(to.distance);
						int bearing = (int) Math.round(to.bearing);
						
						String fromIcao;
						String toIcao;
						if (Math.random() < 0.5)
						{
							fromIcao = icao;
							toIcao = to.icao;
						} 
						else 
						{
							// reverse the flight
							fromIcao = to.icao;
							toIcao = icao;
							if (bearing < 180)
								bearing += 180;
							else
								bearing -= 180;
						}
						
						// Next two lines for user-input active days for unclaimed and claimed jobs
						// Calendar Expires + KeepAlive is what the job is set to expire at when created
						// KeepAlive is initialized to the user-set # of days
						// daysClaimedActive is a data field in assignments that was created for locked jobs
						// but is currently not being used.
						Timestamp now = new Timestamp(System.currentTimeMillis());
						Calendar expires = GregorianCalendar.getInstance();
						expires.add(GregorianCalendar.DAY_OF_MONTH, keepAlive);
						int pay = (int) Math.round(template.getPay(distance, cargoAmount));
//
//						StringBuilder fields = new StringBuilder();
//						StringBuilder values = new StringBuilder();
//
//						fields.append("bearing, creation, expires, commodity, units, amount, fromicao, location, toicao, distance, pay, fromFboTemplate, DaysClaimedActive");
//						values.append("").append(bearing);
//						values.append(", '").append(now).append("'");
//						values.append(", '").append(new Timestamp(expires.getTime().getTime())).append("'");
//						values.append(", '").append(Converters.escapeSQL(template.getRandomCommodity(cargoAmount))).append("'");
//						values.append(", '").append(template.getSUnits()).append("'");
//						values.append(", ").append(cargoAmount);
//						values.append(", '").append(fromIcao).append("'");
//						values.append(", '").append(fromIcao).append("'");
//						values.append(", '").append(toIcao).append("'");
//						values.append(", ").append(distance);
//						values.append(", ").append((float) pay);
//						values.append(", ").append(template.getId());
//						values.append(", ").append(template.getDaysClaimedActive());
//
//						if (!template.getPublicByDefault())
//						{
//							UserBean account = Accounts.getAccountById(template.getOccupant());
//							if (account != null)
//							{
//								if (account.isGroup())
//								{
//									float pilotFee = (float)(pay * cargoAmount * distance * 0.01 * account.getDefaultPilotFee() / 100);
//									fields.append(", groupId");
//									values.append(", ").append(account.getId());
//
//									fields.append(", pilotFee");
//									values.append(", ").append(pilotFee);
//								}
//								else
//								{
//									fields.append(", userlock");
//									values.append(", ").append(account.getId());
//								}
//							}
//						}
//
//						qry = "INSERT INTO assignments (" + fields.toString() + ") VALUES(" + values.toString() + ")";
//						DALHelper.getInstance().ExecuteUpdate(qry);

						Object groupId = null;
						float pilotFee = 0;
						Object userlock = null;
						if (!template.getPublicByDefault())
						{
							UserBean account = Accounts.getAccountById(template.getOccupant());
							if (account != null)
							{
								if (account.isGroup())
								{
									pilotFee = (float)(pay * cargoAmount * distance * 0.01 * account.getDefaultPilotFee() / 100);
									groupId = account.getId();
								}
								else
								{
									userlock = account.getId();
								}
							}
						}

						qry = "{call AddFboAssignment(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
						DALHelper.getInstance().ExecuteStoredProcedure(qry,
								groupId,
								pilotFee,
								userlock,
								bearing,
								now,
								new Timestamp(expires.getTime().getTime()),
								template.getRandomCommodity(cargoAmount),
								template.getSUnits(),
								cargoAmount,
								fromIcao,
								fromIcao, //location
								toIcao,
								distance,
								pay,
								template.getId(),
								template.getDaysClaimedActive());

						if (oneAssignmentPerLoop)
							break;
					}
				}
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}

	void sellAircraft()
	{
		updateStatus("Putting aircraft on the market");
		try
		{
			//remove aircraft from the sale list that has expired
			String qry = "UPDATE aircraft SET sellPrice = null, marketTimeout = null WHERE owner = 0 AND now() > marketTimeout";
			DALHelper.getInstance().ExecuteUpdate(qry);
			
			// get a count of each model of plane where the planes are not for sale
			qry = "SELECT models.make, models.model, models.numSell, models.id, sum(CASE WHEN sellprice is not null THEN 1 ELSE 0 END) as forSale, sum(CASE WHEN sellprice is null THEN 1 ELSE 0 END) as notForSale FROM aircraft, models where aircraft.model = models.id and aircraft.owner = 0 group by (models.id)";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				int model = rs.getInt("id");
				int toSell = rs.getInt("numSell");
				int forSale = rs.getInt("forSale");
				int notForSale = rs.getInt("notForSale");
				
				toSell = toSell - forSale;
				
				//break out of the loop if we have met our goal
				if (toSell <= 0)  
					continue;
				
				double probability = toSell/(double)notForSale;					
				
				qry = "SELECT id FROM aircraft WHERE sellPrice is null AND owner=0 AND model = ?";
				ResultSet toSellRS = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, model);
				while (toSellRS.next())
				{
					if (Math.random() > probability)  //introduced randomness in putting up ac for sale put back in 4/28/08
						continue;
					
					if (toSell <= 0) // break out of the loop when we have the number for sale we want.
						continue;
						
					toSell = toSell -1;  // added this instead of the random comparison above
					
					Calendar expires = GregorianCalendar.getInstance();
					expires.add(GregorianCalendar.DAY_OF_MONTH, 2);  // was 7
					expires.add(GregorianCalendar.HOUR, (int)(Math.random() * 72));  // was 24

					AircraftBean aircraft = Aircraft.getAircraftById(toSellRS.getInt("id"));

					int sellPrice = aircraft.getSystemSellPrice(); //includes raw equipment costs
					sellPrice = (int) Math.round(sellPrice * (1 + Math.random() * 0.4 - 0.2));
					
					qry = "UPDATE aircraft SET sellPrice = ?, markettimeOut = ? where id = ?";
					DALHelper.getInstance().ExecuteUpdate(qry, sellPrice,  new Timestamp(expires.getTime().getTime()), aircraft.getId());
					GlobalLogger.logApplicationLog("Selling aircraft: " + aircraft.getMakeModel() + ", " + aircraft.getRegistration() + ", Price = " + sellPrice + ", expires = " + expires.getTime().toString(), MaintenanceCycle.class);
					
					//remove any AllIn assignments that might be attached to this aircraft
					qry = "DELETE FROM assignments WHERE aircraftid = ?";
					DALHelper.getInstance().ExecuteUpdate(qry, aircraft.getId());
				}
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	void checkStalledAircraft()
	{
		updateStatus("Freeing aircraft that have not been checked in");
		try
		{
			String qry = "SELECT * FROM aircraft WHERE userlock is not null";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry); 
			while (rs.next())
			{
				Timestamp lockedSince = rs.getTimestamp("lockedSince");
				if (lockedSince == null)
					continue;

				long maxRentTime = 10800;
				int ultimateOwner = Accounts.accountUltimateOwner(rs.getInt("owner"));
				if(rs.getInt("userlock") == ultimateOwner)
				{
					maxRentTime = 1000 * 60 * 60; //1000 hours in seconds
				}
				else if (rs.getString("maxRentTime") == null)
				{
					qry = "SELECT maxRentTime FROM models WHERE id = ?";
					int modelMaxRentTime = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), rs.getInt("model"));
					if (modelMaxRentTime > 0)
						maxRentTime = modelMaxRentTime;
				} 
				else 
				{
					maxRentTime = rs.getInt("maxRentTime");
				}					
				
				if (new Timestamp(System.currentTimeMillis() - 1000 * maxRentTime).after(lockedSince))
				{
					int userlock = rs.getInt("userlock");
					String reg = rs.getString("registration");
					String location = rs.getString("location") == null ? rs.getString("departedFrom") : rs.getString("location");
					
					//free aircraft
					qry = "UPDATE aircraft SET lockedSince=null, userlock=null, departedFrom=null, holdRental=false, location = ? WHERE registration = ?";
					DALHelper.getInstance().ExecuteUpdate(qry, location, reg);

					//release enroute assignments
					qry = "UPDATE assignments SET active = 0 WHERE active = 1 AND userlock = ?";
					DALHelper.getInstance().ExecuteUpdate(qry, userlock);
					
					//clean any All-In jobs associated with released plane
					qry = "UPDATE assignments SET userlock=null WHERE aircraft = ?";
					DALHelper.getInstance().ExecuteUpdate(qry, reg);
				}					
			}
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	void processBulkFuelOrders() 
	{
		updateStatus("Doing Bulk Fuel orders");

		try
		{
			String qry = "select id, owner, location, bulk100llOrdered, bulkJetAOrdered from fbo where bulkFuelOrderTimeStamp IS NOT NULL AND bulkFuelDeliveryDateTime < now()";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				int id = rs.getInt("id");
				int owner = rs.getInt("owner");
				String icao = rs.getString("location");
				int amount100ll = rs.getInt("bulk100llOrdered");
				int amountJetA = rs.getInt("bulkJetAOrdered");					

				updateFboFuelRecord(amount100ll, amountJetA, icao, owner);
				Fbos.resetBulkGoodsOrder(id, Fbos.FBO_ORDER_FUEL);

				//log the delivery as a $0 transaction
				Banking.doPayBulkGoodsDelivered(owner, 0, 0, id, "Delivery report --  100LL " + amount100ll + " Kg -- JetA " + amountJetA + " Kg", icao, GoodsBean.GOODS_FUEL100LL);
			}
		}
		catch (SQLException e ) 
		{
			e.printStackTrace();
		}
	}
	
	void updateFboFuelRecord(int amount100ll, int amountJetA, String icao, int owner) throws SQLException
	{
		if(amount100ll <= 0 && amountJetA <= 0)
			return;
		
		String qry = ""; 
		
		if(amount100ll > 0 && doesGoodsRecordExist(GoodsBean.GOODS_FUEL100LL, icao, owner))
			qry = "update goods set amount=amount + " + amount100ll + " where type = " + GoodsBean.GOODS_FUEL100LL + " AND location = '" + icao + "' AND owner = " + owner+ ";";
		else if(amount100ll > 0)
			qry = "insert into goods(amount, type, location, owner) values(" + amount100ll + ", " + GoodsBean.GOODS_FUEL100LL + ", '" + icao + "', " + owner + ");";
		
		if(amountJetA > 0 && doesGoodsRecordExist(GoodsBean.GOODS_FUELJETA, icao, owner))
			qry += "update goods set amount=amount + " + amountJetA + " where type = " + GoodsBean.GOODS_FUELJETA + " AND location = '" + icao + "' AND owner = " + owner + ";";
		else if(amountJetA > 0)
			qry += "insert into goods(amount, type, location, owner) values(" + amountJetA + ", " + GoodsBean.GOODS_FUELJETA + ", '" + icao + "', " + owner + ");";
		
		DALHelper.getInstance().ExecuteBatchUpdate(qry);			
	}
	
	boolean doesGoodsRecordExist(int type, String icao, int owner) throws SQLException
	{
		String qry = "SELECT (count(type) > 0) as found FROM goods where type = ? AND location = ? AND owner = ?";
		return DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), type, icao, owner);
	}

	void processSupplyOrders()
	{
		updateStatus("Doing Bulk Supply orders");

		try
		{
			String qry = "select id, owner, location, bulkSuppliesOrdered from fbo where bulkSupplyOrderTimeStamp IS NOT NULL AND bulkSupplyDeliveryDateTime < now()";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				int id = rs.getInt("id");
				int owner = rs.getInt("owner");
				String icao = rs.getString("location");
				int amount = rs.getInt("bulkSuppliesOrdered");

				updateFboSupplyRecord(amount, icao, owner);
				Fbos.resetBulkGoodsOrder(id, Fbos.FBO_ORDER_SUPPLIES);

				//log the delivery as a $0 transaction
				Banking.doPayBulkGoodsDelivered(owner, 0, 0, id, "Delivery report --  Supplies " + amount + " Kg", icao, GoodsBean.GOODS_SUPPLIES);
			}
		}
		catch (SQLException e )
		{
			e.printStackTrace();
		}
	}

	void updateFboSupplyRecord(int amount, String icao, int owner) throws SQLException
	{
		if(amount <= 0)
			return;

		String qry = "";

		if(amount > 0 && doesGoodsRecordExist(GoodsBean.GOODS_SUPPLIES, icao, owner))
			qry = "update goods set amount=amount + " + amount + " where type = " + GoodsBean.GOODS_SUPPLIES + " AND location = '" + icao + "' AND owner = " + owner+ ";";
		else if(amount > 0)
			qry = "insert into goods(amount, type, location, owner) values(" + amount + ", " + GoodsBean.GOODS_SUPPLIES + ", '" + icao + "', " + owner + ");";

		DALHelper.getInstance().ExecuteBatchUpdate(qry);
	}


	/**
	 * This method checks for aircraft with shipping states of 1 or 3, for processing
	 * State 1 is disassembly and upon completion creates a new assignment, and changes
	 * state to 2 (crated)
     * State 3 is reassembly and upon completion resets the aircraft back to normal active state (0)
	 **/
	void checkAircraftShipping()
	{
		updateStatus("Doing Aircraft shipping update.");
		
		try
		{
			String qry = "SELECT id, owner, registration, location, shippingTo, shippingState, shippingStateNext FROM aircraft WHERE (shippingState = 1 OR shippingState = 3)";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			
			while (rs.next())
			{
				int shippingState = rs.getInt("shippingState");
                int aircraftId = rs.getInt("id");

				Timestamp statenext = rs.getTimestamp("shippingStateNext");
				Timestamp now = new Timestamp(new Date().getTime());					
				
				//Check if its met the required delay
				if( statenext.compareTo(now) < 0)
				{
					//Crate up and create the assignment if disassembly has completed
					if(shippingState == 1)
					{
						qry = "UPDATE aircraft SET shippingState = 2 WHERE id = ?";
						DALHelper.getInstance().ExecuteUpdate(qry, aircraftId);
						
						//Needed to get the aircraft empty weight
						AircraftBean aircraft = Aircraft.getAircraftById(aircraftId);
						
						AssignmentBean assignment;
						assignment = new AssignmentBean();
						
						assignment.setId(-1);
						assignment.setCreation(new Timestamp(System.currentTimeMillis()));
						assignment.setCreatedByUser(true);
						assignment.setUnits(AssignmentBean.UNIT_KG);
						
						
						assignment.setAmount(aircraft.getEmptyWeight());
						assignment.editAmountAllowed(null);
						assignment.setGroup(false);
						assignment.setCommodityId(99);
						assignment.setCommodity("Aircraft " + rs.getString("registration") + " Shipment Crate");
						assignment.setOwner(rs.getInt("owner"));
						assignment.setLocation(rs.getString("location"));
						assignment.setFrom(rs.getString("location"));
						assignment.setTo(rs.getString("shippingTo"));
						assignment.setPilotFee(0);	
						
						try
						{
							UserBean user = Accounts.getAccountById(rs.getInt("owner"));
							Assignments.updateAssignment(assignment, user);
						}
						catch(DataError e)
						{
							e.printStackTrace();
						}
					}
					
					if(shippingState == 3)
					{
						qry = "UPDATE aircraft SET shippingState = 0 WHERE id = ?";
						DALHelper.getInstance().ExecuteUpdate(qry, rs.getInt("id"));
					}																	
				}
			}
		} 
		catch (SQLException e)
		{
				e.printStackTrace();
		} 
	}
	
	void checkAircraftMaintenance()
	{
	    updateStatus("Doing maintenance on aircraft");
	    
	    try
	    {
	    	//100 hour check
	    	String qry = "SELECT * FROM aircraft, models WHERE userlock is null AND models.id = aircraft.model AND owner = 0 AND (engine1 - lastCheck)/3600 > 100";
	    	ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
	    	while (rs.next())
	    	{
	    		AircraftBean aircraft = new AircraftBean(rs);

	    		try
	    		{
	    			Aircraft.doMaintenance(aircraft, 1, null);
	    		}
	    		catch (DataError e1)
	    		{
	    			e1.printStackTrace();
	    		}
	    	}
	    	
	    	//Avgas TBO Check
	      	qry = "SELECT * FROM aircraft, models WHERE userlock is null AND models.id = aircraft.model AND models.fueltype=0 AND owner = 0 AND (engine1/3600) > 1500";
	      	rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
	      	while (rs.next())
	      	{
	      		AircraftBean aircraft = new AircraftBean(rs);
	        	try
	        	{
	        		Aircraft.doMaintenance(aircraft, 2, null);
	        	}
	        	catch (DataError e1)
	        	{
	        		e1.printStackTrace();
	        	}
	      	}
	      
	      	//JetA TBO check
	      	qry = "SELECT * FROM aircraft, models WHERE userlock is null AND models.id = aircraft.model AND models.fueltype=1 AND owner = 0 AND (engine1/3600) > 2000";
	      	rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
	      	while (rs.next())
	      	{
	      		AircraftBean aircraft = new AircraftBean(rs);
	        	try
	        	{
	        		Aircraft.doMaintenance(aircraft, 2, null);
	        	}
	        	catch (DataError e1)
	        	{
	        		e1.printStackTrace();
	        	}
	      	}
	    }
	    catch (SQLException e)
	    {
	    	e.printStackTrace();
	    }
	}
	  
	void checkRentalPrices()
	{
		updateStatus("Resetting rental prices");
		try
		{
			String qry = "SELECT * FROM models WHERE priceDirty = 1";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				ModelBean model = new ModelBean(rs);
				
				qry = "UPDATE models SET priceDirty = 0 where id = ?";
				DALHelper.getInstance().ExecuteUpdate(qry, model.getId());
			
				qry = "SELECT * FROM aircraft WHERE owner = 0 and model = ?";
				ResultSet rs2 = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, model.getId());
				while (rs2.next())
				{
					int aircraftId = rs2.getInt("id");
					int equipment = rs2.getInt("equipment");
					int price = model.getTotalRentalTarget(equipment);
					
					//randomize price +/- 20%
					price *= 1+(Math.random()*0.40) - 0.2;
					
					//get fuel cost for wet rental
					int fuelCost = (int)Math.round(model.getGph() * Goods.getFuelPrice(rs2.getString("home")));
					
					if (rs2.getString("rentalDry") != null)
					{
						qry = "UPDATE aircraft SET rentalDry = ? where id = ?";
						DALHelper.getInstance().ExecuteUpdate(qry, price, aircraftId);
					}
					
					if (rs2.getString("rentalWet") != null)
					{
						qry = "UPDATE aircraft SET rentalWet = ? where id = ?";
						DALHelper.getInstance().ExecuteUpdate(qry, price + fuelCost, aircraftId);
					}
				}
			}	
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	void checkAircraftHomes()
	{
		updateStatus("Moving aircraft to new home");
		try
		{
			String qry = "SELECT aircraft.id, models.minairportsize, lat, lon FROM aircraft, airports, models where home = icao AND models.id = aircraft.model and models.minairportsize > size AND aircraft.owner = 0 AND aircraft.userlock is NULL;";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{
				CloseAirport newHome = Airports.closestAirport(rs.getDouble(3), rs.getDouble(4), rs.getInt(2), false);
				if (newHome == null)
					continue;
					
				qry = "UPDATE aircraft SET home = ?, location = ? WHERE id = ?";
				DALHelper.getInstance().ExecuteUpdate(qry, newHome.icao, newHome.icao, rs.getInt(1));
			}	
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
	}
	
	void createAircraft()
	{
		Statement stmt = null, updater = null, airports = null;
		ResultSet rs = null, updateSet = null, airportSet = null;
		Connection conn = null;
		updateStatus("Creating new aircraft");
		try
		{
			conn = DALHelper.getInstance().getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet. CONCUR_READ_ONLY);
			rs = stmt.executeQuery("SELECT models.amount, count(aircraft.model), models.*  FROM models LEFT JOIN aircraft ON aircraft.model = models.id GROUP BY models.id");
			updater = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet. CONCUR_UPDATABLE);					
			airports = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet. CONCUR_READ_ONLY);

			Set<String> usedRegistrations = getAircraftRegistrationSet();
			
			while(rs.next())
			{
				int amountToCreate = rs.getInt(1) - rs.getInt(2);
				if (amountToCreate == 0)
					continue;
				
				ModelBean model = new ModelBean(rs);												
				int[] capacity = model.getCapacity();
				
				if (amountToCreate > 0)
				{
					updateSet = updater.executeQuery("SELECT * FROM aircraft where 1=2");
					airportSet = airports.executeQuery("SELECT airports.icao, size, prefix, registration FROM airports, registrations WHERE type='civil' AND airports.country = registrations.country AND airports.size > " + model.getMinAirportSize() + " ORDER BY rand() LIMIT " + amountToCreate);

					while (airportSet.next())
					{
						String home = airportSet.getString(1);
						String registration = getNewAircraftRegistration(usedRegistrations, airportSet.getString(3), airportSet.getString(4));									
						usedRegistrations.add(registration);
						
						updateSet.moveToInsertRow();						
						updateSet.updateString("registration", registration);
						updateSet.updateString("home", home);
						updateSet.updateString("location", home);
						updateSet.updateInt("model", model.getId());
						int mask = createNewAircraftBaseEquipment(model.getEquipment());
						updateSet.updateInt("equipment", mask);
						int rent = model.getTotalRentalTarget(mask);
						rent *= 1+(Math.random()*0.40) - 0.2;
						updateSet.updateInt("RentalDry", rent);
						int fuelCost = (int)Math.round(model.getGph() * Goods.getFuelPrice(home));

						//update so that models with 0 rental price cannot be rented
						if(rent != 0)
							updateSet.updateInt("RentalWet", fuelCost + rent);

						if (capacity[0] > 0)
							updateSet.updateDouble("fuelCenter", 0.5);
						
						if (capacity[1] > 0)
							updateSet.updateDouble("fuelLeftMain", 0.5);
						
						if (capacity[2] > 0)
							updateSet.updateDouble("fuelLeftAux", 0.5);
						
						if (capacity[3] > 0)
							updateSet.updateDouble("fuelLeftTip", 0.5);
						
						if (capacity[4] > 0)
							updateSet.updateDouble("fuelRightMain", 0.5);
						
						if (capacity[5] > 0)
							updateSet.updateDouble("fuelRightAux", 0.5);
						
						if (capacity[6] > 0)
							updateSet.updateDouble("fuelRightTip", 0.5);
						
						updateSet.insertRow();
						GlobalLogger.logApplicationLog("CreateAircraft creating unit - Model:" + model.getId(), MaintenanceCycle.class);
					}
					
					airportSet.close();
					updateSet.close();
				} 
				else
				{
					double probability = (double)rs.getInt(2) / (double)-amountToCreate;
					updateSet = updater.executeQuery("SELECT * FROM aircraft WHERE userlock IS NULL AND owner = 0 AND engine1 IS NULL AND NOT EXISTS (SELECT * FROM assignments WHERE assignments.aircraft = aircraft.registration) AND model = " + model.getId() + " AND rand() < " + probability);
					while (updateSet.next())
					{
						GlobalLogger.logApplicationLog("CreateAircraft deleting unit - Model:" + updateSet.getInt("model"), MaintenanceCycle.class);
						updateSet.deleteRow();
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
			DALHelper.getInstance().tryClose(updateSet);
			DALHelper.getInstance().tryClose(airportSet);
			DALHelper.getInstance().tryClose(airports);
			DALHelper.getInstance().tryClose(updater);
			DALHelper.getInstance().tryClose(rs);
			DALHelper.getInstance().tryClose(stmt);
			DALHelper.getInstance().tryClose(conn);
		}
	}
	
	Set<String> getAircraftRegistrationSet() throws SQLException
	{
		Set<String> usedRegistrations = new HashSet<>();
	
		String qry = "SELECT registration from aircraft";
		ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

		while (rs.next())
			usedRegistrations.add(rs.getString(1));

		return usedRegistrations;
	}
	
	String getNewAircraftRegistration(Set<String> usedRegistrations, String prefix, String postfix)
	{
		StringBuffer registration;
		int loopCounter = 0;
		
		do
		{
			registration = new StringBuffer(prefix);
			registration.append('-');
			
			for (int loop = 0; loop < postfix.length(); loop++)
			{
				char thisChar = postfix.charAt(loop);
				
				if (Character.isDigit(thisChar))
				{
					registration.append((int)Math.round(Math.random()*9));
				}
				else if (Character.isLowerCase(thisChar))
				{
					registration.append((char)('A'+(int)Math.round(Math.random()*25)));
				}
				else if (Character.isUpperCase(thisChar))
				{
					registration.append(thisChar);
				}
				else
				{
					int ran = (int)Math.round(Math.random()*35);
					
					if (ran < 10)
						registration.append(ran);
					else
						registration.append((char)('A'+ran-10));
				}
			}
			
			loopCounter++;
			if(loopCounter > 1000)
			{
				//Apparently we have ran out of registration codes, add a extended postfix
				GlobalLogger.logDebugLog("New Registration generator excessive looping: prefix [" + prefix + "], postfix [" + postfix + "]", MaintenanceCycle.class);
				
				registration.append('-');
				int ran = (int)Math.round(Math.random()*100);
				registration.append(ran);
			}	
		} while(usedRegistrations.contains(registration.toString()));
		
		return registration.toString();
	}
	
	int createNewAircraftBaseEquipment(int modelequipment)
	{
		int mask = 0;
		switch (modelequipment)
		{
			case ModelBean.EQUIPMENT_VFR_ONLY :
				break;
				
			case ModelBean.EQUIPMENT_IFR_ONLY:
				mask = ModelBean.EQUIPMENT_GPS_MASK|ModelBean.EQUIPMENT_IFR_MASK|ModelBean.EQUIPMENT_AP_MASK;
				break;
				
			case ModelBean.EQUIPMENT_VFR_IFR:
				switch ((int)(Math.random()*6))
				{
					case 0:
					case 1:
					case 2:
						break;
						
					case 3:												
						mask = ModelBean.EQUIPMENT_AP_MASK;
						break;	
						
					case 4: 
						mask = ModelBean.EQUIPMENT_IFR_MASK|ModelBean.EQUIPMENT_AP_MASK;
						break;
						
					case 5: 
						mask = ModelBean.EQUIPMENT_GPS_MASK|ModelBean.EQUIPMENT_IFR_MASK|ModelBean.EQUIPMENT_AP_MASK;
						break;											
				}
				break;
				
			default:
				GlobalLogger.logApplicationLog("createNewAircraftBaseEquipment(): model equipment not defined for: " + modelequipment +
						", Default VFR used.", MaintenanceCycle.class);
		}
		
		return mask;
	}

	private Set<String> getCurrentRegistrations()
	{
		HashSet<String> regSet = new HashSet<>();
		
		try
		{
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery("SELECT registration from aircraft");
			while (rs.next())
			{
				regSet.add(rs.getString(1));
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();				
		}
		
		return regSet;
	}
	
	private Map<String, List<String[]>> getCountryRegistrationCodes()
	{
		Map<String, List<String[]>> map = new HashMap<>();
		try
		{
			String qry = "SELECT country, icao, prefix, registration FROM registrations";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			
			while (rs.next())
			{
				List<String[]> list = map.get(rs.getString(1));
				if(list != null)
				{
					list.add(new String[]{rs.getString(2),rs.getString(3),rs.getString(4)});
				}
				else
				{
					list = new ArrayList<>();
					list.add(new String[]{rs.getString(2),rs.getString(3),rs.getString(4)});
					map.put(rs.getString(1), list);
				}
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();				
		}
		
		return map;
	}
	
	String getICAORegistrationCountry(String icao)
	{
		String qry = "";
		try 
		{
			qry = "SELECT r.country FROM airports a, registrations r WHERE a.country = r.country AND a.ICAO = ?";
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, icao);
			if(rs.next())
			{
				return rs.getString(1);
			}
			else
			{
				GlobalLogger.logDebugLog("Error in isValidRegistration(), did not find country for ICAO: " + icao, MaintenanceCycle.class);

				return "Default";
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		} 
		
		return null;
	}
	
	boolean isValidPrefix(String reg, String prefix)
	{
        return reg.startsWith(prefix);

    }
	
	boolean isValidRegistrationFormat(String postfix, String reg)
	{
		for(int i=0;i<postfix.length();i++)
		{
			char c = postfix.charAt(i);
			if(Character.isDigit(c))
			{
				if(!Character.isDigit(reg.charAt(i)))
					return false;				
			}
			else if(Character.isLetter(c))
			{
				//Literal
				if(Character.isUpperCase(c))
				{
					if(reg.charAt(i) != c)
						return false;
				}
				else
				{
					if(!Character.isLetter(reg.charAt(i)))
						return false;
				}
			}
			else if(c == '#') // letter or digit
			{
				if(!Character.isLetterOrDigit(reg.charAt(i)))
					return false;
			}
			else //should never reach, force new registration
			{
				GlobalLogger.logDebugLog("Error in isValidRegistrationFormat(), registration postfix not a valid format symbol!: [" + c + "] of [" + postfix + "]", MaintenanceCycle.class);
				return false;
			}
		}
		
		return true;
	}
	
	boolean isValidRegistrationPostfix(String reg, String prefix, String postfix)
	{
		String body = reg.substring(prefix.length());

        return body.length() == postfix.length() && isValidRegistrationFormat(postfix, body);
    }
	
	boolean isValidRegistration(String reg, List<String[]> coding) // String homeIcao, Map<String, String[]> countryRegistrationCodes)
	{
		int index = 0;
		boolean found = false;
		
		do
		{
			String prefix = (coding.get(index))[REG_PREFIX];
			String postfix = (coding.get(index))[REG_POSTFIX];				
	
			//checks use the separator -
			prefix = prefix.concat("-");
			
			if( isValidPrefix(reg, prefix) && isValidRegistrationPostfix(reg, prefix, postfix))
				found = true;

			index++;
		} while(index < coding.size());
			
		return found;
	}
	
	private void updateAircraftToNewRegistration(String reg, String newreg)
	{
		try
		{
            String qry ="UPDATE aircraft SET registration = ? WHERE registration = ?;";
            DALHelper.getInstance().ExecuteUpdate(qry, newreg, reg);
		}
		catch (SQLException e)
		{
			GlobalLogger.logDebugLog("ERROR: Changing aircraft Reg: [" + reg + "] To new reg: [" + newreg + "]", MaintenanceCycle.class);
			e.printStackTrace();				
		} 
	}
	
	void resetRegistration(String home, String reg, List<String[]> coding, Set<String> currRegs)
	{
		int index = 0;
		int foundCount = 0;
		List<Integer> list = new ArrayList<>();

		do
		{
			String icaoprefix = coding.get(index)[REG_ICAO];
			if( home.startsWith(icaoprefix))
			{
				foundCount++;
				list.add(index);
			}
			index++;
		} while(index < coding.size());
		
		//assumption - there will always be at least 1 coding
		index = 0;

		if(foundCount > 1)
			index = list.get((int)Math.round(Math.random()*foundCount));

		String prefix = coding.get(index)[REG_PREFIX];
		String postfix = coding.get(index)[REG_POSTFIX];
		String newreg = getNewAircraftRegistration(currRegs, prefix, postfix);
		
		currRegs.add(newreg);		
		updateAircraftToNewRegistration(reg, newreg);

		GlobalLogger.logApplicationLog("Changed aircraft Reg: [" + reg + "] To new reg: [" + newreg + "]", MaintenanceCycle.class);
	}	
	
	void checkRegistrations()
	{
		updateStatus("Checking registrations");
		
		try 
		{
			//do these once here for optimization
			Map<String, List<String[]>> countryRegistrationCodes = getCountryRegistrationCodes();
			Set<String> regs = getCurrentRegistrations();

            String qry = "SELECT * from aircraft WHERE owner = 0 and userlock is null";
			CachedRowSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next()) 
			{
				String reg = rs.getString("registration");
				String home = rs.getString("home");
				String icaocountry = getICAORegistrationCountry(home);				
				List<String[]> coding = countryRegistrationCodes.get(icaocountry);

				if(!isValidRegistration(reg, coding))
				{
                    resetRegistration(home, reg, coding, regs);
				}				
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();				
		} 
	}	
}
