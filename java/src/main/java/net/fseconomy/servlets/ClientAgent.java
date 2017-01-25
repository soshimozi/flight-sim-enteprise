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

package net.fseconomy.servlets;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.beans.AircraftMaintenanceBean;
import net.fseconomy.beans.AssignmentBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.data.*;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.dto.DepartFlight;
import net.fseconomy.dto.LatLon;
import net.fseconomy.util.Crypto;
import net.fseconomy.util.GlobalLogger;
import net.fseconomy.util.Helpers;

public class ClientAgent extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        UserBean userBean;

        String mp = req.getParameter("mp");
        if(Helpers.isNullOrBlank(mp))
        {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }

        String paramString = Crypto.decrypt(mp);

        HashMap<String, String> hashMap = parseAgentString(paramString.trim());

        String user = hashMap.get("user");
        String password = hashMap.get("pass");

        if (user == null || password == null || (userBean=Accounts.userExists(user, password)) == null)
        {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid account information");
            return;
        }

        req.setAttribute("user", userBean);
        String action = hashMap.get("action");

        if (action == null)
        {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No action specified");
            return;
        }

        //Log entry in clientrequest table
        //Ignore aircraftProbe, accountCheck, addModel
        if("start cancel arrive".contains(action.toLowerCase()))
        {
            String reg;
            int aircraftId = -1;
            String ipAddress = req.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null)
                ipAddress = req.getRemoteAddr();

            AircraftBean aircraft = Aircraft.getAircraftForUser(userBean.getId());
            if(aircraft != null)
            {
                reg = aircraft.getRegistration();
                aircraftId = aircraft.getId();
            }
            else
                reg = null;

            String icao = null;
            if(hashMap.get("lat") != null)
            {
                icao = Airports.closestAirport(Double.parseDouble(hashMap.get("lat")), Double.parseDouble(hashMap.get("lon"))).icao;
            }

            //            String mac;
            //
            //            if(!Helpers.isNullOrBlank(hashmac))
            //            {
            //                if(hashmac.length() < 32+12)
            //                {
            //                    //FSX error return
            //                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid information");
            //                    return;
            //                }
            //
            //                long lmac = Long.decode("#" + hashmac.substring(32));
            //                mac = String.format("%x", ( lmac ^ 0xAAAAAAAAAAAAL)).toUpperCase();
            //
            //                String chkMD5 = Crypto.getMD5(mac);
            //                if(!chkMD5.equals(hashmac.substring(0,32)))
            //                {
            //                    //FSX error return
            //                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid information");
            //                    return;
            //                }
            //            }
            //            else

            String mac = "000000000000";

            SimClientRequests.addClientRequestEntry(ipAddress, mac, userBean.getId(), userBean.getName(), "FS", action, reg, aircraftId, hashMap.get("lat"), hashMap.get("lon"), icao, "");
        }

        try
        {
            String output="";
            //if (!Data.agentVersion.equals(req.getParameter("version")))
            //	throw new DataError("A new version of this program is available.");
            switch (action)
            {
                case "addAircraft":
                    output = addModel(hashMap);
                    if (output == null)
                        output = "mess=Your aircraft is not known, but a request is added to the database.";
                    else
                        output = "mess=Your aircraft is known as " + output;
                    break;
                case "start":
                    output = doStart(hashMap.get("aircraft"), new LatLon(hashMap.get("lat"), hashMap.get("lon")), userBean);
                    break;
                case "test":
                    String version = hashMap.get("version");
                    if (!SimClientRequests.GetFSUIPCClientVersion().equals(version))
                        throw new DataError("A new version of this program is available.");
                    break;
                case "arrive":
                    output = doArrive(hashMap, userBean);
                    break;
                case "cancel":
                    output = doCancel(userBean);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action specified");
                    return;
            }
            resp.getWriter().println(output);
        }
        catch (DataError e)
        {
            resp.getWriter().println("mess=" + e.getMessage());
        }
    }

    HashMap<String, String> parseAgentString(String query)
    {
        String[] params = query.split("&");
        HashMap<String, String> map = new HashMap<>();
        for (String param : params)
        {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }

        return map;
    }

    String addModel(HashMap<String, String> hashMap) throws DataError
    {
        String aircraft = hashMap.get("aircraft");
        String fcapCenter = hashMap.get("c");
        String fcapLeftMain = hashMap.get("lm");
        String fcapLeftAux = hashMap.get("la");
        String fcapLeftTip = hashMap.get("lt");
        String fcapRightMain = hashMap.get("rm");
        String fcapRightAux = hashMap.get("ra");
        String fcapRightTip = hashMap.get("rt");
        String fcapCenter2 = hashMap.get("c2");
        String fcapCenter3 = hashMap.get("c3");
        String fcapExt1 = hashMap.get("x1");
        String fcapExt2 = hashMap.get("x2");
        if (fcapCenter == null || fcapLeftMain == null || fcapLeftAux == null ||
                fcapLeftTip == null || fcapRightMain == null || fcapRightAux == null|| fcapRightTip == null ||
                fcapCenter2 == null || fcapCenter3 == null || fcapExt1 == null || fcapExt2 == null)
            throw new DataError("Not enough information received.");

        int[] fuelCapacities = new int[] { Integer.parseInt(fcapCenter), Integer.parseInt(fcapLeftMain),
                Integer.parseInt(fcapLeftAux),Integer.parseInt(fcapLeftTip), Integer.parseInt(fcapRightMain),
                Integer.parseInt(fcapRightAux),Integer.parseInt(fcapRightTip), Integer.parseInt(fcapCenter2),
                Integer.parseInt(fcapCenter3), Integer.parseInt(fcapExt1), Integer.parseInt(fcapExt2) };

        return Models.addModel(aircraft, fuelCapacities);
    }

    CloseAirport closestAirport(LatLon latlon)
    {
        double dLat = latlon.lat;
        double dLon = latlon.lon;

        return Airports.closestAirport(dLat, dLon);
    }

    String doStart(String FSAircraft, LatLon latlon, UserBean user) throws DataError
    {
        CloseAirport closest= closestAirport(latlon);
        if (FSAircraft == null || closest == null)
        {
            return "";
        }
        AircraftBean aircraft = Aircraft.getAircraftForUser(user.getId());

        if (aircraft == null || !closest.icao.equals(aircraft.getLocation()))
            throw new DataError("You have no active aircraft at " + closest.icao);

        if (!Aircraft.aircraftMappingFound(aircraft.getModelId(), FSAircraft))
            throw new DataError(FSAircraft+" is not compatible with your active "+aircraft.getMakeModel());

        // Check the aircraft not an All-in only aircraft with no All-in assignment, if not unrent and exit
        if(Aircraft.checkAllInAircraftWithOutAssigment(aircraft))
        {
            throw new DataError(aircraft.getMakeModel() + " is an All-In only aircraft and no All-In assignment found.  Please select another aircraft");
        }

        if (Stats.getInstance().getNumberOfHours(user.getId(), 48) > 30)
            throw new DataError("Maximum pilot hours in a 48 hour period reached");

        DepartFlight info = Flights.departAircraft(aircraft, user.getId(), closest.icao);

        StringBuilder buffer = new StringBuilder();
        for (AssignmentBean assignment : info.assignments)
        {
            String item, commodity = assignment.getCommodity();
            if (commodity == null)
            {
                item = " : :" + assignment.getTo();
            }
            else
            {
                int amount = assignment.getAmount();
                String units = assignment.getUnits() == AssignmentBean.UNIT_PASSENGERS ? "" : (" " + assignment.getSUnits());
                item = commodity + ":" + amount + units + ":" + assignment.getTo();
            }
            buffer.append("as=").append(item).append("\n");
        }

        long maxrenttime;
        int ultimateOwner = Accounts.accountUltimateGroupOwner(aircraft.getOwner());
        if(user.getId() == ultimateOwner)
            maxrenttime = ((aircraft.getLockedSince().getTime()/1000) + (1000*60*60));  // unlimited for owner
        else
            maxrenttime = ((aircraft.getLockedSince().getTime()/1000) + aircraft.getMaxRentTime());

        StringBuilder sb = new StringBuilder();
        //Pairs
        sb.append("reg="); sb.append(aircraft.getRegistration());
        sb.append("\nexpiry="); sb.append(maxrenttime);
        sb.append("\naccount="); sb.append(aircraft.getAccounting());
        sb.append("\nequip="); sb.append(aircraft.getEquipment());
        sb.append("\npayload="); sb.append(info.payloadWeight);
        sb.append("\nweight="); sb.append(info.totalWeight);

        //append fuel
        float[] fuel = aircraft.getFuel();
        sb.append("\nfuel=");
        for(int i=0;i<=10;i++)
        {
            if(i!=0)
                sb.append(":");

            sb.append(fuel[i]);
        }

        sb.append("\n");
        sb.append(buffer.toString());

        return sb.toString();

        //		return "reg=" + aircraft[0].getRegistration() + "\nexpiry=" + maxrenttime +
        //			"\naccount=" + aircraft[0].getAccounting() +
        //			"\nfuel=" + fuel[0] + ":" + fuel[1] + ":" + fuel[2] + ":"+ fuel[3] + ":" + fuel[4] + ":" + fuel[5] + ":" + fuel[6] + ":" +
        //				fuel[7] + ":" + fuel[8] + ":" + fuel[9] + ":" + fuel[10] +
        //			"\nequip=" + aircraft[0].getEquipment() +
        //			"\npayload=" + payloadWeight + "\n" +
        //			"\nweight=" + totalWeight + "\n" +
        //			buffer.toString();
    }

    String doCancel(UserBean user) throws DataError
    {
        Flights.cancelFlight(user);
        return "";
    }

    private List getEngineDamage(HashMap<String, String> hashMap, String parameter, int paramValue)
    {
        List returnValue = new ArrayList();
        for (int c=1; c <= 4; c++)
        {
            String param = parameter + c;
            String value = hashMap.get(param);
            if (value == null)
                break;
            returnValue.add(new int[] {c, paramValue, Integer.parseInt(value)});
        }
        return returnValue;
    }
    private int[][] getDamage(HashMap<String, String> hashMap)
    {
        List returnValue = new ArrayList();
        returnValue.addAll(getEngineDamage(hashMap, "heat", AircraftMaintenanceBean.DAMAGE_HEATING));
        returnValue.addAll(getEngineDamage(hashMap, "mixture", AircraftMaintenanceBean.DAMAGE_MIXTURE));
        return (int[][]) returnValue.toArray(new int[0][0]);
    }

    //moved from Data to here, didn't seem to be working from there - Airboss 1/29/11
    private Lock lockProcessFlight = new ReentrantLock();
    private Set<Integer> processFlightLock = new HashSet<>(100);

    //Moved lock methods from Data - Airboss 1/19/11
    public boolean setProcessFlight(int userid) throws DataError
    {
        boolean b;

        //System.out.println("userid: " + userid);
        lockProcessFlight.lock();
        try
        {
            b = !processFlightLock.add(userid);
        }
        finally
        {
            lockProcessFlight.unlock();
        }

        //System.out.println("b found: " + b);
        return b;
    }

    public void clearProcessFlight(int userid) throws DataError
    {
        lockProcessFlight.lock();
        try
        {
            processFlightLock.remove(userid);
        }
        finally
        {
            lockProcessFlight.unlock();
        }
    }

    String doArrive(HashMap<String, String> hashMap, UserBean user) throws DataError
    {
        //First things first, we need to check if we are already processing a flight for this user
        // Add lock on userid, if already locked we are already processing so kick SENDERROR
        if( setProcessFlight(user.getId()) )
            throw new DataError("Already processing this flight.");

        try
        {

            //System.err.println(new Timestamp(System.currentTimeMillis()) + " - Processing FS9 flight data UserID = " + user.getId());

            String message;
            String engineTime = hashMap.get("time");
            String engineTicks = hashMap.get("ticks");
            String fCenter = hashMap.get("c");
            String fLeftMain = hashMap.get("lm");
            String fLeftAux = hashMap.get("la");
            String fLeftTip = hashMap.get("let");
            String fRightMain = hashMap.get("rm");
            String fRightAux = hashMap.get("ra");
            String fRightTip = hashMap.get("rt");
            String fCenter2 = hashMap.get("c2");
            String fCenter3 = hashMap.get("c3");
            String fExt1 = hashMap.get("x1");
            String fExt2 = hashMap.get("x2");

            if (engineTime == null || engineTicks == null || fCenter == null || fLeftMain == null || fLeftAux == null ||
                    fLeftTip == null || fRightMain == null || fRightAux == null|| fRightTip == null ||
                    fCenter2 == null || fCenter3 == null || fExt1 == null || fExt2 == null)
            {
                GlobalLogger.logFlightLog("Flight data missing parameters: fuel and engine time", FSagent.class);
                throw new DataError("Flight data missing parameters, flight aborted.");
            }

            String sNight = hashMap.get("night");
            String sEnv = hashMap.get("env");

            int night = 0, assignmentsLeft;

            if (sNight != null && Integer.parseInt(sNight) == 1)
                night = 1;

            float envFactor = 1.0f;
            if (sEnv != null)
                envFactor = Float.parseFloat(sEnv);

            float[] fuel = new float[] { Float.parseFloat(fCenter), Float.parseFloat(fLeftMain),
                    Float.parseFloat(fLeftAux),Float.parseFloat(fLeftTip), Float.parseFloat(fRightMain),
                    Float.parseFloat(fRightAux),Float.parseFloat(fRightTip), Float.parseFloat(fCenter2), Float.parseFloat(fCenter3),
                    Float.parseFloat(fExt1), Float.parseFloat(fExt2) };

            if(hashMap.get("lat") == null && hashMap.get("lon") == null)
            {
                GlobalLogger.logFlightLog("Flight data missing parameters: lat/lon", FSagent.class);
                throw new DataError("Flight data missing parameters, flight aborted.");
            }

            CloseAirport closest = closestAirport(new LatLon(hashMap.get("lat"), hashMap.get("lon")));
            if (closest == null)
            {
                throw new DataError("Invalid lat/lon, flight aborted.");
            }

            int[][] damage = getDamage(hashMap);

            //assignmentsLeft = data.freeAircraft(user, closest, Integer.parseInt(engineTime), Integer.parseInt(engineTicks), fuel, night, envFactor, damage, false);
            assignmentsLeft = Flights.processFlight(user, closest, Integer.parseInt(engineTime), Integer.parseInt(engineTicks), fuel, night, envFactor, damage, SimClientRequests.SimType.FSUIPC);

            message = "Your flight is logged and the results can be found at the website.";
            if (assignmentsLeft > 0)
                message += "|Your aircraft is still rented because there are uncompleted assignments. You may wish to continue your flight or cancel the rent at the website.";
            else
                message += "|Your aircraft is no longer rented. If you want to continue, you must rerent the aircraft.";

            return "mess=" + message;
        }
        finally
        {
            // clear the lock from userid
            clearProcessFlight(user.getId());
        }
    }
}
