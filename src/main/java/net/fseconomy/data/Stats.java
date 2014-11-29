package net.fseconomy.data;

import net.fseconomy.dto.LatLonCount;
import net.fseconomy.dto.PendingHours;
import net.fseconomy.dto.Statistics;
import net.fseconomy.util.Formatters;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Stats
{
    //sig stats
    public static long defaultCount = 0;
    public static long createCount = 0;
    public static long cacheCount = 0;
    public static long bytesServed = 0;
    public static long totalImagesSent = 0;

    static long milesFlown;
    static long minutesFlown;
    static long totalIncome;

    static List<Statistics> statistics = null;

    public static HashMap<String, Statistics> statsmap = null;
    public static HashMap<String, Statistics> prevstatsmap = null;

    public static List<LatLonCount> FlightSummaryList = new ArrayList<>();

    private static Stats stats = null;

    private Stats()
    {
    }

    public static Stats getInstance()
    {
        if ( stats == null )
            stats = new Stats();

        return stats;
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

    public List<Statistics> getStatistics()
    {
        return statistics;
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
                    qry = "SELECT count(*) AS number FROM aircraft LEFT JOIN accounts on aircraft.userlock=accounts.id WHERE aircraft.location is null";
                    break;
                case "parked":
                    qry = "SELECT count(*) AS number FROM aircraft LEFT JOIN accounts on aircraft.userlock = accounts.id WHERE aircraft.location is not null AND aircraft.userlock is not null";
                    break;
                default:
                    throw new DataError(usertype + " not known.");
            }
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public double getNumberOfHours(int userId, int hours)
    {
        double result=0;
        try
        {
            String qry;
            int linkId = getLinkSet(userId);
            if(linkId > 0)
            {
                qry = "SELECT SUM((FlightEngineTime)/3600) AS TimeLogged FROM (Select FlightEngineTime from `log` where DATE_SUB(CURRENT_TIMESTAMP ,INTERVAL ? hour) <= `time`  AND userid in (Select id from accounts join (Select accountid from linkedaccounts where linkid = ?)  as la where accounts.id=la.accountid) ) as b;";
                result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), hours, linkId);
            }
            else
            {
                qry = "SELECT SUM((FlightEngineTime)/3600) AS TimeLogged FROM `log` where userid= ? and DATE_SUB(CURRENT_TIMESTAMP ,INTERVAL ? hour) <= `time`";
                result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), userId, hours);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private int getLinkSet(int userId)
    {
        int result = 0;

        try
        {
            String qry = "SELECT linkid FROM linkedaccounts where accountid = ? and status = 1";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), userId);
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
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user, hours);
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

    public List<LatLonCount> getFlightSummary()
    {
        List<LatLonCount> toList = new ArrayList<>();

        try
        {
            Date date = new Date(System.currentTimeMillis()-(60*60*1000*24));

            String qry = "select lat, lon, count from (select `to`, count(`to`) as count from ( select `to` from log where type='flight' AND  `time` > '" + Formatters.dateyyyymmddhhmmss.format(date) + "') b group by `to`) a join airports ap on ap.icao=`to`";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

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
