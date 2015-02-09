package net.fseconomy.data;

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.beans.CachedAirportBean;
import net.fseconomy.beans.ModelBean;
import net.fseconomy.dto.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public static void addClientRequestEntry(String ipAddress, String mac, int id, String name, String client, String state, String aircraft, int aircraftId, String lat, String lon, String icao, String params)
    {
        try
        {
            String qry = "INSERT INTO clientrequests ( ip, mac, pilotid, pilot, client, state, aircraft, aircraftId, lat, lon, icao, params) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
            DALHelper.getInstance().ExecuteUpdate(qry, ipAddress, mac, id, name, client, state, aircraft, aircraftId, lat, lon, icao, params);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<String> getClientRequestCountsByAccountId(int id)
    {
        String qry = "select ip, count(ip) from (select ip from clientrequests where pilotid=?) a group by ip";

        return getClientRequestCounts(qry, id, "");
    }

    public static List<String> getClientRequestCountsByIp(String ip)
    {
        String qry = "select pilot, count(pilot) from (select pilot from clientrequests where ip=?) a group by pilot";

        return getClientRequestCounts(qry, 0, ip);
    }

    public static List<String> getClientRequestCounts(String qry, int id, String ip)
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

    public static List<String> lookupClientRequestIps(String query) throws DataError
    {
        List<String> list = new ArrayList<>();
        try
        {
            String qry = "Select DISTINCT ip, pilotid, pilot from clientrequests where ip like CONCAT(?, '%') order by ip";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, query);

            while(rs.next())
                list.add(rs.getString("ip") + "|" + rs.getString("pilotid") + "|" + rs.getString("pilot"));
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
            String qry = "SELECT ip, GROUP_CONCAT(DISTINCT pilot) AS users, COUNT(DISTINCT pilot) as count FROM clientrequests GROUP BY ip HAVING COUNT(DISTINCT pilot) > 1 ORDER BY COUNT(DISTINCT pilot) DESC";
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

    public static HashMap<String,ClientIP> getCountIpWithMultipleUsers() throws DataError
    {
        HashMap<String,ClientIP> list = new HashMap<>();
        try
        {
            String qry = "SELECT ip, GROUP_CONCAT(DISTINCT pilot) AS users, COUNT(DISTINCT pilot) as count FROM clientrequests GROUP BY ip HAVING COUNT(DISTINCT pilot) > 1 ORDER BY COUNT(DISTINCT pilot) DESC";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

            while(rs.next())
                list.put(rs.getString("ip"), new ClientIP(rs.getString("ip"), rs.getInt("count"), rs.getString("users")));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new DataError("getClientRequestIps: SQL Error");
        }

        return list;
    }

    public static List<ClientFlightStats> getClientFlightStats(int pilotId) throws DataError
    {
        List<ClientFlightStats> list = new ArrayList<>();

        try
        {
            String qry = "select min(id) from (SELECT id FROM clientrequests WHERE state like 'start%' and pilotid = ? order by id desc limit 200 ) t";
            int startId = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), pilotId);

            qry = "SELECT * FROM clientrequests where pilotid=? and id >= ? order by id";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, pilotId, startId);

            boolean start = false;
            Timestamp stime = new Timestamp(0);
            String sclient = "";
            String sicao = "";
            float slat = 0;
            float slon = 0;

            while(rs.next())
            {
                String state = rs.getString("state");
                if(state.equals("cancel"))
                    continue;

                if(state.contains("start"))
                {
                    sicao = rs.getString("icao");

                    if(sicao == null)
                        continue;

                    stime = rs.getTimestamp("time");
                    sclient = rs.getString("client");
                    slat = rs.getFloat("lat");
                    slon = rs.getFloat("lon");
                    start = true;
                }
                if(state.contains("arrive"))
                {
                    if(start)
                    {
                        start = false;

                        Timestamp etime = rs.getTimestamp("time");
                        String eclient = rs.getString("client");
                        String eicao = rs.getString("icao");
                        float elat = rs.getFloat("lat");
                        float elon = rs.getFloat("lon");
                        int aircraftId = rs.getInt("aircraftid");

                        int realTime = (int)((etime.getTime() - stime.getTime()) / 1000);

                        double dist = Airports.getDistance(sicao, eicao);
                        AircraftBean aircraft = Aircraft.getAircraftById(aircraftId);
                        int fltTime = -1;
                        int tc = -1;
                        String makeModel = "[missing]";
                        if(aircraft != null)
                        {
                            ModelBean model = Models.getModelById(aircraft.getModelId());
                            makeModel = model.getMakeModel();
                            fltTime = (int) (3600 * (dist / model.getCruise()));
                            if(realTime != 0)
                                tc = fltTime / realTime;
                        }

                        CachedAirportBean eap = Airports.cachedAirports.get(eicao);
                        LatLon ll = new LatLon(elat, elon);
                        double stopdist = Airports.getDistance(eap.getLatLon(), ll);

                        ClientFlightStats cfs = new ClientFlightStats(pilotId, sclient+"/"+eclient, stime, etime, sicao, eicao, makeModel, dist, stopdist, fltTime, realTime, tc );
                        list.add(cfs);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new DataError("getClientRequestIps: SQL Error");
        }

        Collections.reverse(list);

        return list;
    }

    public static List<ClientFlightStats> getCurrentFlightStats() throws DataError
    {
        List<ClientFlightStats> list = new ArrayList<>();

        try
        {
            String qry = "select min(id) from (SELECT id FROM clientrequests WHERE state like 'start%' order by id desc limit 200 ) t";
            int startId = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer());

            qry = "SELECT * FROM clientrequests where id >= ? group by pilotid, id";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, startId);


            boolean first = true;
            boolean start = false;
            Timestamp stime = new Timestamp(0);
            String sclient = "";
            String sicao = "";
            float slat = 0;
            float slon = 0;
            int currPilotId = 0;
            int lastPilotId = 0;

            while(rs.next())
            {
                currPilotId = rs.getInt("pilotid");
                if(first)
                {
                    first = false;
                    lastPilotId=currPilotId;
                }

                if(currPilotId != lastPilotId)
                    start = false;

                String state = rs.getString("state");
                if(state.equals("cancel"))
                    continue;

                if(state.contains("start"))
                {
                    sicao = rs.getString("icao");

                    if(sicao == null)
                        continue;

                    stime = rs.getTimestamp("time");
                    sclient = rs.getString("client");
                    slat = rs.getFloat("lat");
                    slon = rs.getFloat("lon");
                    start = true;
                }
                if(state.contains("arrive"))
                {
                    if(start)
                    {
                        start = false;

                        int pilotId = rs.getInt("pilotid");
                        Timestamp etime = rs.getTimestamp("time");
                        String eclient = rs.getString("client");
                        String eicao = rs.getString("icao");
                        float elat = rs.getFloat("lat");
                        float elon = rs.getFloat("lon");
                        int aircraftId = rs.getInt("aircraftid");

                        int realTime = (int)((etime.getTime() - stime.getTime()) / 1000);

                        double dist = Airports.getDistance(sicao, eicao);
                        AircraftBean aircraft = Aircraft.getAircraftById(aircraftId);
                        int fltTime = -1;
                        int tc = -1;
                        String makeModel = "[missing]";
                        if(aircraft != null)
                        {
                            ModelBean model = Models.getModelById(aircraft.getModelId());
                            makeModel = model.getMakeModel();
                            fltTime = (int) (3600 * (dist / model.getCruise()));
                            if(realTime != 0)
                                tc = fltTime / realTime;
                        }

                        CachedAirportBean eap = Airports.cachedAirports.get(eicao);
                        LatLon ll = new LatLon(elat, elon);
                        double stopdist = Airports.getDistance(eap.getLatLon(), ll);

                        ClientFlightStats cfs = new ClientFlightStats(pilotId, sclient+"/"+eclient, stime, etime, sicao, eicao, makeModel, dist, stopdist, fltTime, realTime, tc );
                        list.add(cfs);
                    }
                }

                lastPilotId = currPilotId;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new DataError("getClientRequestIps: SQL Error");
        }

        Collections.reverse(list);

        return list;
    }
}
