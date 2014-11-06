package net.fseconomy.data;

import net.fseconomy.beans.LogBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.util.Converters;
import net.fseconomy.util.Formatters;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Logging implements Serializable
{
    public static int getAmountLogForGroup(int groupId)
    {
        int result = -1;
        try
        {
            String qry = "SELECT count(*) FROM log WHERE groupId = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), groupId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getAmountLogForUser(UserBean user)
    {
        int result = -1;
        try
        {
            String qry = "SELECT count(*) FROM log WHERE user = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), Converters.escapeSQL(user.getName()));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getAmountLogForAircraft(String registration)
    {
        int result = -1;
        try
        {
            String qry = "SELECT count(*) FROM log WHERE aircraft = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), Converters.escapeSQL(registration));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getAmountLogForFbo(int fbo)
    {
        int result = -1;
        try
        {
            String qry = "SELECT count(*) FROM log WHERE fbo = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), fbo);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<LogBean> getLogForAircraftByMonth(String reg, int month, int year)
    {
        String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + "" + " AND aircraft = '" + Converters.escapeSQL(reg) + "'";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForAircraftFromId(String reg, int fromid)
    {
        String sql = "SELECT * FROM log where id > " + fromid + " AND aircraft = '" + Converters.escapeSQL(reg) + "'" + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroupByMonth(int groupId, int month, int year)
    {
        String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + "" + " AND groupId = " + groupId;

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForUserByMonth(String username, int month, int year)
    {
        String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + "" + " AND user = '" + Converters.escapeSQL(username) + "'";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroupFromId(int groupId, int fromid)
    {
        String sql = "SELECT * FROM log where id > " + fromid + " AND groupId = " + groupId + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroupFromRegistrations(String registrations, int fromid)
    {
        String sql = "SELECT * FROM log where id > " + fromid + " AND aircraft in (" + registrations + ")" + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForUserFromId(String username, int fromid)
    {
        String sql = "SELECT * FROM log where id > " + fromid + " AND user = '" + Converters.escapeSQL(username) + "'" + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroup(int groupId, int from, int to)
    {
        return getLogSQL("SELECT * FROM log WHERE groupId=" + groupId + " ORDER BY time DESC LIMIT " + from + "," + to);
    }

    public static List<LogBean> getLogForUser(UserBean user, int from, int amount)
    {
        return getLogSQL("SELECT * FROM log WHERE type <> 'refuel' and type <> 'maintenance' and user='" + Converters.escapeSQL(user.getName()) + "' ORDER BY time DESC LIMIT " + from + "," + amount);
    }

    public static List<LogBean> getLogForUser(UserBean user, int afterLogId)
    {
        return getLogSQL("SELECT * FROM log WHERE id > " + afterLogId + " AND type <> 'refuel' and type <> 'maintenance' and user='" + Converters.escapeSQL(user.getName()) + "' ORDER BY time DESC ");
    }

    public static LogBean getLogById(int id)
    {
        List<LogBean> result = getLogSQL("SELECT * FROM log WHERE id = " + id);
        return result.size() == 0 ? null : result.get(0);
    }

    public static List<LogBean> getLogForFbo(int fbo, int from, int amount)
    {
        return getLogSQL("SELECT * FROM log WHERE fbo=" + fbo + " ORDER BY time DESC LIMIT " + from + "," + amount);
    }

    public static List<LogBean> getLogForAircraft(String registration, int from, int amount)
    {
        return getLogSQL("SELECT * FROM log WHERE aircraft='" + Converters.escapeSQL(registration) + "' ORDER BY time DESC LIMIT " + from + "," + amount);
    }

    public static List<LogBean> getLogForAircraft(String registration, int entryid)
    {
        return getLogSQL("SELECT * FROM log WHERE aircraft='" + Converters.escapeSQL(registration) + "' AND ID > " + entryid + " ORDER BY time DESC");
    }

    public static List<LogBean> getLogForMaintenanceAircraft(String registration)
    {
        return getLogSQL("SELECT * FROM log WHERE type = 'maintenance' AND aircraft='" + Converters.escapeSQL(registration) + "' ORDER BY time DESC");
    }

    static List<LogBean> getLogSQL(String qry)
    {
        ArrayList<LogBean> result = new ArrayList<LogBean>();

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
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

        return result;
    }

    public static Object[] outputLog(String selection)
    {
        StringBuilder result1 = new StringBuilder();
        Set<String> aircraft = new HashSet<String>();
        Set<String> users = new HashSet<String>();

        try
        {
            int count = 0;

            String qry = "SELECT log.user, log.aircraft, `from`, `to`, a.lat,  a.lon,  b.lat,  b.lon, log.time FROM log, airports a, airports b where log.`from` = a.icao and log.`to` = b.icao and log.type='flight' and " + selection;
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                if (count++ > 0)
                {
                    result1.append(",\n");
                }

                result1.append("['").append(rs.getString(1)).append("','").append(rs.getString(2)).append("','").append(rs.getString(3)).append("','").append(rs.getString(4)).append("',").append(rs.getDouble(5)).append(",").append(rs.getDouble(6)).append(",").append(rs.getDouble(7)).append(",").append(rs.getDouble(8)).append(",'").append(Formatters.datemmddyy.format(rs.getDate(9))).append("']");
                users.add(rs.getString(1));
                aircraft.add(rs.getString(2));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        List<String> aircraftList = new ArrayList<String>(aircraft);
        List<String> usersList = new ArrayList<String>(users);
        Collections.sort(aircraftList);
        Collections.sort(usersList);

        return new Object[]{result1.toString(), aircraftList, usersList};
    }
}