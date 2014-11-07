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

/**
 * Created by smobley on 11/6/2014.
 */
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

    public static long getMilesFlown()
    {
        return milesFlown;
    }

    public static long getMinutesFlown()
    {
        return minutesFlown;
    }

    public static void setMilesFlown(long i)
    {
        milesFlown = i;
    }

    public static void setMinutesFlown(long i)
    {
        minutesFlown = i;
    }

    public static long getTotalIncome()
    {
        return totalIncome;
    }

    public static void setTotalIncome(long i)
    {
        totalIncome = i;
    }

    public static List<Statistics> getStatistics()
    {
        return statistics;
    }

    public static int getNumberOfUsers(String usertype) throws DataError
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
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static double getNumberOfHours(String user, int hours) throws DataError
    {
        double result=0;
        try
        {
            String qry = "SELECT SUM((FlightEngineTime)/3600) AS TimeLogged FROM `log` where user= ? and DATE_SUB(CURRENT_TIMESTAMP ,INTERVAL ? hour) <= `time`";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), user, hours);

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    //get pending hours will list when hours are coming available - need to format 48: to hours
    public static List<PendingHours> getPendingHours(String user, int hours) throws DataError
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

    public static List<LatLonCount> getFlightSummary()
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
