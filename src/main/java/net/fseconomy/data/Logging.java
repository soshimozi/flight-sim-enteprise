package net.fseconomy.data;

import net.fseconomy.beans.AssignmentBean;
import net.fseconomy.beans.LogBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.dto.DbLog;
import net.fseconomy.util.Converters;
import net.fseconomy.util.Formatters;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class Logging implements Serializable
{
    public static List<DbLog> getDbLog(int offset, int size)
    {
        List<DbLog> result = new ArrayList<>();

        try
        {
            String qry = "SELECT timestmp, level_string, caller_class, formatted_message FROM logging_event order by timestmp desc limit ?, ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, offset, size);
            while (rs.next())
            {
                DbLog log = new DbLog();
                log.timestamp = new Timestamp(rs.getLong(1));
                log.level = rs.getString(2);
                log.callerClass = rs.getString(3);
                log.message = rs.getString(4);
                result.add(log);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<DbLog> getDbExploitAuditLog(int offset, int size)
    {
        List<DbLog> result = new ArrayList<>();

        try
        {
            String qry = "SELECT timestmp, level_string, caller_class, formatted_message FROM logging_event where formatted_message like '[Exploit-Audit]%' order by timestmp desc limit ?, ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, offset, size);
            while (rs.next())
            {
                DbLog log = new DbLog();
                log.timestamp = new Timestamp(rs.getLong(1));
                log.level = rs.getString(2);
                log.callerClass = rs.getString(3);
                log.message = rs.getString(4);
                result.add(log);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void logTemplateAssignment(AssignmentBean assignment, int payee)
    {
        try
        {
            String qry = "INSERT INTO templatelog (created, expires, templateid, fromicao, toicao, pay, payee) VALUES (?,?,?,?,?,?,?)";
            DALHelper.getInstance().ExecuteUpdate(qry, assignment.getCreation(), assignment.getExpires(),assignment.getFromTemplate(), assignment.getFrom(), assignment.getTo(), assignment.calcPay(), payee);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

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
            String qry = "SELECT count(*) FROM log WHERE userid = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), user.getId());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getAmountLogForAircraft(int aircraftId)
    {
        int result = -1;
        try
        {
            String qry = "SELECT count(*) FROM log WHERE aircraftid = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), aircraftId);
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

    public static List<LogBean> getLogForAircraftByMonth(int aircraftId, int month, int year)
    {
        String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + "" + " AND aircraftid = " + aircraftId;

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForAircraftFromId(int aircraftId, int fromid)
    {
        String sql = "SELECT * FROM log where id > " + fromid + " AND aircraftid = " + aircraftId + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroupByMonth(int groupId, int month, int year)
    {
        String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + "" + " AND groupId = " + groupId;

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForUserByMonth(int userId, int month, int year)
    {
        String sql = "SELECT * FROM log where Year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))= " + year + " and Month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00'))=" + month + "" + " AND userid = " + userId;

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroupFromId(int groupId, int fromid)
    {
        String sql = "SELECT * FROM log where id > " + fromid + " AND groupId = " + groupId + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroupFromIds(String aircraftIds, int fromid)
    {
        String sql = "SELECT * FROM log where id > " + fromid + " AND aircraftid in (" + aircraftIds + ")" + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForUserFromId(String username, int fromid)
    {
        int userId = Accounts.getAccountIdByName(username);
        String sql = "SELECT * FROM log where id > " + fromid + " AND userid = " + userId + " order by id LIMIT 500";

        return getLogSQL(sql);
    }

    public static List<LogBean> getLogForGroup(int groupId, int from, int to)
    {
        return getLogSQL("SELECT * FROM log WHERE groupId=" + groupId + " ORDER BY time DESC LIMIT " + from + "," + to);
    }

    public static List<LogBean> getLogForUser(UserBean user, int from, int amount)
    {
        ArrayList<LogBean> result = new ArrayList<>();

        try
        {
            String qry = "SELECT * FROM log WHERE  userid = ? AND type <> 'refuel' AND type <> 'maintenance' ORDER BY time DESC LIMIT " + from + "," + amount;
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user.getId());
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

    public static List<LogBean> getLogForUser(UserBean user, int afterLogId)
    {
        return getLogSQL("SELECT * FROM log WHERE id > " + afterLogId + " AND type <> 'refuel' and type <> 'maintenance' and userid = " + user.getId() + " ORDER BY time DESC ");
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

    public static List<LogBean> getLogForAircraft(int aircraftId, int from, int amount)
    {
        return getLogSQL("SELECT * FROM log WHERE aircraftid=" + aircraftId + " ORDER BY time DESC LIMIT " + from + "," + amount);
    }

    public static List<LogBean> getLogForAircraft(int aircraftId, int entryid)
    {
        return getLogSQL("SELECT * FROM log WHERE aircraftid=" + aircraftId + " AND ID > " + entryid + " ORDER BY time DESC");
    }

    public static List<LogBean> getLogForMaintenanceAircraft(int aircraftId)
    {
        ArrayList<LogBean> result = new ArrayList<>();

        try
        {
             String qry = "SELECT * FROM log WHERE type = 'maintenance' AND aircraftid = ? ORDER BY time DESC";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, aircraftId);
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

    static List<LogBean> getLogSQL(String qry)
    {
        ArrayList<LogBean> result = new ArrayList<>();

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

    public static final int AIRCRAFTLOG = 1;
    public static final int USERLOG = 2;
    public static final int GROUPLOG = 3;

    public static Object[] outputLog(int type, int id)
    {
        StringBuilder result1 = new StringBuilder();
        Set<String> aircraft = new HashSet<>();
        Set<String> users = new HashSet<>();

        try
        {
            int count = 0;
            String reg = "";

            String qry = "SELECT log.userid, log.aircraftid, `from`, `to`, a.lat,  a.lon,  b.lat,  b.lon, log.time FROM log, airports a, airports b where log.`from` = a.icao and log.`to` = b.icao and log.type='flight' and ";
            if(type == AIRCRAFTLOG)
            {
                qry = qry + " aircraftid = ?";
                reg = Aircraft.getAircraftRegistrationById(id);
            }
            else if(type == USERLOG)
                qry =  qry + " userid = ?";
            else if(type == GROUPLOG)
                qry =  qry + " groupId = ?";
            else
                throw new IllegalArgumentException("Unknown parameter: type = " + type);

            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);
            while (rs.next())
            {
                if (count++ > 0)
                {
                    result1.append(",\n");
                }

                String name = Accounts.getAccountNameById(rs.getInt(1));
                result1.append("['").append(name).append("','").append(reg).append("','").append(rs.getString(3)).append("','").append(rs.getString(4)).append("',").append(rs.getDouble(5)).append(",").append(rs.getDouble(6)).append(",").append(rs.getDouble(7)).append(",").append(rs.getDouble(8)).append(",'").append(Formatters.datemmddyy.format(rs.getDate(9))).append("']");
                users.add(name);
                aircraft.add(reg);
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
}