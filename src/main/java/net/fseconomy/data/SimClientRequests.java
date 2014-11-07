package net.fseconomy.data;

import net.fseconomy.dto.ClientRequest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SimClientRequests
{
    public static enum SimType {FSUIPC, FSX, XP}

    public static String GetFSUIPCClientVersion()
    {
        String version = "";
        try
        {
            String qry = "SELECT svalue FROM sysvariables where VariableName='FSUIPCClientVersion'";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
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

    public static void addClientRequestEntry(String ipAddress, int id, String name, String client, String state, String aircraft, String params)
    {
        try
        {
            String qry = "INSERT INTO clientrequests ( ip, pilotid, pilot, client, state, aircraft, params) VALUES(?,?,?,?,?,?,?)";
            DALHelper.getInstance().ExecuteUpdate(qry, ipAddress, id, name, client, state, aircraft, params);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<String> getClientRequestCountsByAccountId(int id) throws DataError
    {
        String qry = "select ip, count(ip) from (select ip from clientrequests where pilotid=?) a group by ip";

        return getClientRequestCounts(qry, id, "");
    }

    public static List<String> getClientRequestCountsByIp(String ip) throws DataError
    {
        String qry = "select pilot, count(pilot) from (select pilot from clientrequests where ip=?) a group by pilot";

        return getClientRequestCounts(qry, 0, ip);
    }

    public static List<String> getClientRequestCounts(String qry, int id, String ip) throws DataError
    {
        List<String> list = new ArrayList<>();
        try
        {
            ResultSet rs;

            if(ip.equals(""))
                rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);
            else
                rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, ip);

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


    public static List<ClientRequest> getClientRequestsByAccountId(int id) throws DataError
    {
        String qry = "Select * from clientrequests where pilotid = ? order by id desc limit 100";

        return getClientRequests(qry, id, "");
    }

    public static List<ClientRequest> getClientRequestsByIp(String ip) throws DataError
    {
        String qry = "Select * from clientrequests where ip = ? order by id desc limit 100";

        return getClientRequests(qry, 0, ip);
    }

    public static List<ClientRequest> getClientRequests(String qry, int id, String ip) throws DataError
    {
        List<ClientRequest> list = new ArrayList<>();
        try
        {
            ResultSet rs;

            if(ip.equals(""))
                rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);
            else
                rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, ip);

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

    public static List<String> getClientRequestIps() throws DataError
    {
        List<String> list = new ArrayList<>();
        try
        {
            String qry = "Select DISTINCT ip from clientrequests order by ip";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

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

    public static List<String> getClientRequestIpWithMultipleUsers() throws DataError
    {
        List<String> list = new ArrayList<>();
        try
        {
            String qry = "SELECT ip, GROUP_CONCAT(DISTINCT pilot) AS users FROM clientrequests GROUP BY ip HAVING COUNT(DISTINCT pilot) > 1 ORDER BY COUNT(DISTINCT pilot) DESC";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

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
}
