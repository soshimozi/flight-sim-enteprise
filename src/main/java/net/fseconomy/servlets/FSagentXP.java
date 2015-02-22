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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fseconomy.beans.*;
import net.fseconomy.data.*;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.dto.DepartFlight;
import net.fseconomy.util.Crypto;
import net.fseconomy.util.GlobalLogger;
import net.fseconomy.util.Helpers;

public class FSagentXP extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    SimClientRequests.SimType simType = SimClientRequests.SimType.XP;

    public void init()
    {
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        String md5sum = req.getParameter("md5sum");

        //Setup for doing an MD5 CRC check on the script calling us from the python plugin
        try
        {
            String qry = "SELECT svalue FROM sysvariables WHERE variablename='XPlaneScriptMD5'";
            String currXP10MD5 = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer());

            qry = "SELECT svalue FROM sysvariables WHERE variablename='XPlane9ScriptMD5'";
            String currXP9MD5 = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer());

            if (!currXP10MD5.equals(md5sum) && !currXP9MD5.equals(md5sum))
            {
                StringBuffer xml = new StringBuffer();
                xml.append("<?xml version=\"1.0\"?>");
                xml.append("<response>");
                xml.append("<error>");
                xml.append("Invalid Python script detected for X-Plane - please redownload from FSE site");
                xml.append("</error>");
                xml.append("</response>");
                resp.getWriter().println(xml);
                return;
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return;
        }

        UserBean userBean;
        String user = req.getParameter("user");
        String password = req.getParameter("pass");

        if (user == null || password == null || (userBean=Accounts.userExists(user, password)) == null)
        {
            //XPlane error return
            StringBuffer xml = new StringBuffer();
            xml.append("<?xml version=\"1.0\"?>");
            xml.append("<response>");
            xml.append("<error>");
            xml.append("Invalid user information provided");
            xml.append("</error>");
            xml.append("</response>");
            resp.getWriter().println(xml);
            return;
        }

        req.setAttribute("user", userBean);
        String action = req.getParameter("action");
        if (action == null)
        {
            //XPlane error return
            StringBuffer xml = new StringBuffer();
            xml.append("<?xml version=\"1.0\"?>");
            xml.append("<response>");
            xml.append("<error>");
            xml.append("No action specified");
            xml.append("</error>");
            xml.append("</response>");
            resp.getWriter().println(xml);
            return;
        }

        //Log entry in clientrequest table
        //Ignore aircraftProbe, accountCheck, addModel
        if("startflight cancel arrive".contains(action.toLowerCase()))
        {
            String reg;
            int aircraftId = -1;

            String ipAddress = req.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null)
                ipAddress = req.getRemoteAddr();

            AircraftBean aircraft = Aircraft.getAircraftForUser(userBean.getId());
            if(aircraft!= null)
            {
                reg = aircraft.getRegistration();
                aircraftId = aircraft.getId();
            }
            else
                reg = null;

            String icao = null;
            if(req.getParameter("lat") != null)
            {
                icao = Airports.closestAirport(Double.parseDouble(req.getParameter("lat")), Double.parseDouble(req.getParameter("lon"))).icao;
            }

            String mac = "000000000000";

            SimClientRequests.addClientRequestEntry(ipAddress, mac, userBean.getId(), userBean.getName(), simType.name(), action, reg, aircraftId, req.getParameter("lat"), req.getParameter("lon"), icao, "");
        }

        String content = "";
        try
        {
            switch (action)
            {
                case "aircraftProbe":
                    content = aircraftProbe(req);
                    break;
                case "accountCheck":
                    // If we've made it this far, the account is OK.
                    content = "<ok/>";
                    break;
                case "startFlight":
                    content = startFlight(userBean, req);
                    break;
                case "cancel":
                    content = doCancel(userBean);
                    break;
                case "arrive":
                    content = doArrive(userBean, req);
                    break;
                case "addModel":
                    content = addModel(req);
                    break;
            }
        }
        catch (DataError e)
        {
            String msg = e.getMessage();

            if(!msg.contains("is not compatible") && !msg.contains("no rented aircraft"))
                GlobalLogger.logFlightLog(new Timestamp(System.currentTimeMillis()) + " DataError: " + msg, FSagentFSX.class);

            if( msg.contains("VALIDATIONERROR"))
                content = "<result>" + msg + "</result>";
            else
                content = "<error>" + msg + "</error>";
        }
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\"?>");
        xml.append("<response>");
        xml.append(content);
        xml.append("</response>");

        resp.getWriter().println(xml);
    }

    CloseAirport closestAirport(HttpServletRequest req)
    {
        String lat = req.getParameter("lat");
        String lon = req.getParameter("lon");
        if (lat == null || lon == null )
            return null;
        double dLat = Double.parseDouble(lat);
        double dLon = Double.parseDouble(lon);
        return Airports.closestAirport(dLat, dLon);
    }

    private StringBuffer airportToXml(CachedAirportBean airport)
    {
        StringBuffer output = new StringBuffer();
        output.append("<airport>\n");
        output.append(xmlNode("icao", airport.getIcao()));
        output.append(xmlNodeCDATA("name", airport.getName()));
        output.append("<fuelPrice>").append(airport.getPrice100ll()).append("</fuelPrice>\n");
        output.append("</airport>\n");
        return output;
    }
    private StringBuffer closeAirportToXml(CloseAirport airport)
    {
        StringBuffer output = new StringBuffer();
        output.append("<closeAirport>\n");
        output.append("<icao>").append(airport.icao).append("</icao>\n");
        output.append("<distance>").append(airport.distance).append("</distance>\n");
        output.append("<bearing>").append(airport.bearing).append("</bearing>\n");
        output.append("</closeAirport>\n");
        return output;
    }
    private StringBuffer aircraftToXml(AircraftBean aircraft)
    {
        StringBuffer output = new StringBuffer();
        output.append("<aircraft>\n");
        output.append("<registration>").append(aircraft.getRegistration()).append("</registration>\n");
        output.append(xmlNodeCDATA("type", aircraft.getMakeModel()));
        output.append("</aircraft>\n");
        return output;
    }
    private String xmlNode(String name, String value)
    {
        return "<" + name + ">" + value + "</" + name + ">\n";
    }
    private String xmlNodeCDATA(String name, String value)
    {
        return "<" + name + "><![CDATA[" + value + "]]></" + name + ">\n";
    }

    private String xmlNode(String name, int value)
    {
        return xmlNode(name, Integer.toString(value));
    }

    String addModel(HttpServletRequest req) throws DataError
    {
        String aircraft = req.getParameter("aircraft");
        String fcapCenter = req.getParameter("c");
        String fcapLeftMain = req.getParameter("lm");
        String fcapLeftAux = req.getParameter("la");
        String fcapLeftTip = req.getParameter("let");
        String fcapRightMain = req.getParameter("rm");
        String fcapRightAux = req.getParameter("ra");
        String fcapRightTip = req.getParameter("rt");
        String fcapCenter2 = req.getParameter("c2");
        String fcapCenter3 = req.getParameter("c3");
        String fcapExt1 = req.getParameter("x1");
        String fcapExt2 = req.getParameter("x2");
        if (fcapCenter == null || fcapLeftMain == null || fcapLeftAux == null ||
                fcapLeftTip == null || fcapRightMain == null || fcapRightAux == null|| fcapRightTip == null ||
                fcapCenter2 == null || fcapCenter3 == null || fcapExt1 == null || fcapExt2 == null)
            throw new DataError("Not enough information recieved.");
        int[] fuelCapacities = new int[] { (int)Float.parseFloat(fcapCenter), (int)Float.parseFloat(fcapLeftMain),
                (int)Float.parseFloat(fcapLeftAux),(int)Float.parseFloat(fcapLeftTip), (int)Float.parseFloat(fcapRightMain),
                (int)Float.parseFloat(fcapRightAux),(int)Float.parseFloat(fcapRightTip), (int)Float.parseFloat(fcapCenter2),
                (int)Float.parseFloat(fcapCenter3), (int)Float.parseFloat(fcapExt1), (int)Float.parseFloat(fcapExt2) };
        Models.addModel(aircraft, fuelCapacities);
        return "<ok/>";
    }

    String aircraftProbe(HttpServletRequest req)
    {
        StringBuilder result = new StringBuilder();
        String aircraftTitle = req.getParameter("aircraft");
        CloseAirport airport = closestAirport(req);

        List<CloseAirport> airportList = new ArrayList<>();
        List<CachedAirportBean> currentAirport = new ArrayList<>();
        List<AircraftBean> alternativeAircraft = new ArrayList<>();
        String modelName = Aircraft.probeAircraft(aircraftTitle, airport, airportList, currentAirport, alternativeAircraft);

        if (modelName == null)
            result.append("<aircraftUnknown/>");
        else
            result.append(xmlNodeCDATA("aircraftType", modelName));

        if (currentAirport.size() > 0)
            result.append(airportToXml(currentAirport.get(0)));

        if (airportList.size() > 0)
        {
            for (Object anAirportList : airportList)
                result.append(closeAirportToXml((CloseAirport) anAirportList));
        }

        if (alternativeAircraft.size() > 0)
        {
            for (Object anAlternativeAircraft : alternativeAircraft)
                result.append(aircraftToXml((AircraftBean) anAlternativeAircraft));
        }

        return result.toString();
    }

    String doCancel(UserBean user) throws DataError
    {
        Flights.cancelFlight(user);
        return "<ok/>";
    }

    String startFlight(UserBean user, HttpServletRequest req) throws DataError
    {
        StringBuilder result = new StringBuilder();

        // Ok, lets get the parameters for aircraft and closest airport so we can do some checks
        // FSAircraft is the title name from FSX/9, we'll compare it against the FSE mappings
        String FSAircraft = req.getParameter("aircraft");
        CloseAirport closest = closestAirport(req);

        // If either are null, bad data, exit
        if (FSAircraft == null || closest == null)
            throw new DataError("Invalid Aircraft name, or Departing ICAO received, flight aborted.");

        // Lets get the users current aircraft
        AircraftBean aircraft = Aircraft.getAircraftForUser(user.getId());

        // If there is no rented aircraft or the aircraft is not at the same location, exit
        if (aircraft == null || !closest.icao.equals(aircraft.getLocation()))
            throw new DataError("You have no rented aircraft at " + closest.icao);

        // Check the aircraft name mapping to make sure we have a match, if not, exit
        if (!Aircraft.aircraftMappingFound(aircraft.getModelId(), FSAircraft))
            throw new DataError(FSAircraft + " is not compatible with your rented " + aircraft.getMakeModel());

        // Check the aircraft not an All-in only aircraft with no All-in assignment, if not unrent and exit
        if(Aircraft.checkAllInAircraftWithOutAssigment(aircraft))
            throw new DataError(aircraft.getMakeModel() + " is an All-In only aircraft and no All-In assignment found.  Please select another aircraft");

        // Check the number of hours the user has flown, if over 30 hours, exit
        if (Stats.getInstance().getNumberOfHours(user.getId(), 48) > 30)
            throw new DataError("Maximum pilot hours in a 48 hour period reached");

        // Lets put together our aircraft data

        // Get the aircraft data
        DepartFlight info = Flights.departAircraft(aircraft, user.getId(), closest.icao);

        float[] fuel = aircraft.getFuelInGallons();
        StringBuilder fuelString = new StringBuilder();

        for (float aFuel : fuel)
            fuelString.append(Float.toString(aFuel)).append(" ");

        long maxrenttime;
        int ultimateOwner = Accounts.accountUltimateGroupOwner(aircraft.getOwner());
        if(user.getId() == ultimateOwner)
            maxrenttime = 100*3600; // unlimited for owner
        else
            maxrenttime = aircraft.getMaxRentTime();

        // Alright, lets form up our return XML data to the client
        result.append(xmlNode("payloadWeight", info.payloadWeight));
        result.append(xmlNode("totalWeight", info.totalWeight));
        result.append(xmlNode("registration", aircraft.getRegistration()));
        result.append(xmlNode("fuel", fuelString.toString()));
        result.append(xmlNode("equipment", aircraft.getEquipment()));
        result.append(xmlNode("leaseExpires", Long.toString(maxrenttime)));
        result.append(xmlNode("accounting", aircraft.getAccounting() == AircraftBean.ACC_HOUR ? "hour" : "tacho"));
        result.append(xmlNode("rentedDry", info.rentedDry ? "true" : "false"));
        result.append(xmlNode("rentalPrice", info.rentedDry ? aircraft.getRentalPriceDry() : aircraft.getRentalPriceWet()));

        // List all our assignments
        for (AssignmentBean as : info.assignments)
        {
            result.append("<assignment>\n");
            result.append(xmlNode("from", as.getFrom()));
            result.append(xmlNode("to", as.getTo()));
            String noHTMLAssignment = as.getSCargo().replaceAll("<.*?>", "");
            result.append(xmlNodeCDATA("cargo", noHTMLAssignment));
            result.append(xmlNodeCDATA("comment", as.getComment()));
            result.append("</assignment>\n");
        }

        return result.toString();
    }

    private List getEngineDamage(HttpServletRequest req, String parameter, int paramValue)
    {
        List returnValue = new ArrayList();
        for (int c=1; c <= 4; c++)
        {
            String param = parameter + c;
            String value = req.getParameter(param);
            if (value == null)
                break;
            returnValue.add(new int[] {c, paramValue, (int)Float.parseFloat(value)});
        }
        return returnValue;
    }
    private int[][] getDamage(HttpServletRequest req)
    {
        List returnValue = new ArrayList();
        returnValue.addAll(getEngineDamage(req, "heat", AircraftMaintenanceBean.DAMAGE_HEATING));
        returnValue.addAll(getEngineDamage(req, "mixture", AircraftMaintenanceBean.DAMAGE_MIXTURE));
        return (int[][]) returnValue.toArray(new int[0][0]);
    }

    private Lock lockProcessFlight = new ReentrantLock();
    private Set<Integer> processFlightLock = new HashSet<>(100);

    public boolean setProcessFlight(int userid) throws DataError
    {
        boolean b;

        lockProcessFlight.lock();
        try
        {
            b = !processFlightLock.add(userid);
        }
        finally
        {
            lockProcessFlight.unlock();
        }

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

    String doArrive(UserBean user, HttpServletRequest req) throws DataError
    {
        //First things first, we need to check if we are already processing a flight for this user
        // Add lock on userid, if already locked we are already processing so kick SENDERROR
        if( setProcessFlight(user.getId()) )
            throw new DataError("Already processing this flight.");

        try
        {
            //System.err.println(new Timestamp(System.currentTimeMillis()) + " - Processing FSX flight data UserID = " + user.getId());

            String message;
            String engineTime = req.getParameter("time1");
            String engineTicks = req.getParameter("rentalTick");
            String fCenter = req.getParameter("c");
            String fLeftMain = req.getParameter("lm");
            String fLeftAux = req.getParameter("la");
            String fLeftTip = req.getParameter("let");
            String fRightMain = req.getParameter("rm");
            String fRightAux = req.getParameter("ra");
            String fRightTip = req.getParameter("rt");
            String fCenter2 = req.getParameter("c2");
            String fCenter3 = req.getParameter("c3");
            String fExt1 = req.getParameter("x1");
            String fExt2 = req.getParameter("x2");

            if (engineTime == null || fCenter == null || fLeftMain == null || fLeftAux == null ||
                    fLeftTip == null || fRightMain == null || fRightAux == null|| fRightTip == null ||
                    fCenter2 == null || fCenter3 == null || fExt1 == null || fExt2 == null)
            {
                GlobalLogger.logFlightLog("Flight data missing parameters: fuel and engine time.", FSagentFSX.class);
                throw new DataError("Flight data missing parameters, flight aborted.");
            }

            String sNight = req.getParameter("night");
            String sEnv = req.getParameter("env");

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

            CloseAirport closest = closestAirport(req);
            if (closest == null)
            {
                throw new DataError("Invalid lat/lon, flight aborted.");
            }

            if (engineTicks == null)
                engineTicks = "0";

            int eticks = Integer.parseInt(engineTicks);
            if(eticks < 0)
                eticks *= -1;

            int[][] damage = getDamage(req);

            assignmentsLeft = Flights.processFlight(user, closest, Integer.parseInt(engineTime), eticks, fuel, night, envFactor, damage, simType);

            message = "Your flight is logged and the results can be found at the website.";
            if (assignmentsLeft > 0)
                message+= "|Your aircraft is still rented because there are uncompleted assignments. You may wish to continue your flight or cancel the rent at the website.";
            else
                message+= "|Your aircraft is no longer rented. If you want to continue, you must rerent the aircraft.";

            return "<result>" + message + "</result>";
        }
        finally
        {
            // clear the lock from userid
            clearProcessFlight(user.getId());
        }
    }
}
