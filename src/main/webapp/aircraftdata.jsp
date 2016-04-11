<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>
<%@ page import="net.fseconomy.util.Formatters" %>
<%@ page import="net.fseconomy.beans.*" %>
<%@ page import="net.fseconomy.data.*" %>
<%@ page import="net.fseconomy.util.Helpers" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
    String sAircraftId = request.getParameter("aircraftid");
    if (sAircraftId == null || !Helpers.isInteger(sAircraftId))
        return;

    int aircraftId = Integer.parseInt(sAircraftId);
    AircraftBean aircraft = Aircraft.getAircraftById(aircraftId);

    UserBean aircraftOwner = Accounts.getAccountById(aircraft.getOwner());
    int groupOwnerid = Accounts.accountUltimateGroupOwner(aircraft.getOwner());
    UserBean ultimateOwner = Accounts.getAccountById(groupOwnerid);

    String icao = aircraft.getLocation();
    CachedAirportBean airportInfo = null;
    String airportTitle = "";
    String location = "";

    if(!Helpers.isNullOrBlank(icao))
    {
        airportTitle = Airports.cachedAirports.get(icao).getTitle();
        location = Airports.airportLink(icao, icao, response);
    }
    else
    {
        airportTitle = "In flight.";
        location = "In flight.";
    }
    String homeUrl = Airports.airportLink(aircraft.getHome(), aircraft.getHome(), response);

    String price = Formatters.currency.format(aircraft.getSellPrice());

    //find how many planes of this type for sale in the FSE world
    int acForSale = Aircraft.FindAircraftForSaleByModelCount(aircraft.getModelId());

    String forSaleUrl = "<a class=\"normal\" href=\"" + response.encodeURL("aircraftforsale.jsp?&model=" + aircraft.getModelId()) + "\">" + acForSale + " for sale</a>";

    // Calculate the airframe time
    String afTime = "";

    // If the last 100hr check was > 75 hours ago, calculate the time until the next one
    int checkminutes = (aircraft.getTotalEngineTime() - aircraft.getLastCheck())/60;
    int temp = 6000 - checkminutes;
    afTime += " (" + Formatters.twoDigits.format(temp / 60) + ":" + Formatters.twoDigits.format(Math.abs(temp % 60)) + ")";
%>

<div class="row clearfix">
    <div class="col-sm-12 column">
        <div class="panel panel-primary">
            <h3 class="text-center"><%=aircraft.getMakeModel()%></h3>
            <h3 class="text-center"><%=price%></h3>
            <h4 class="text-center"><%= airportTitle %></h4>
            <div class="row clearfix">
                <div class="col-xs-4 col-sm-4 col-md-4 column">
                    <div class="text-right">Aircraft ID</div>
                    <div class="text-right">Registration</div>
                    <div class="text-right">Type <%=forSaleUrl%></div>
                    <div class="text-right">Home</div>
                    <div class="text-right">Current Location</div>
                    <div class="text-right">Equipment</div>
                    <div class="text-right">Seats</div>
                    <div class="text-right">Addtional Crew</div>
                    <div class="text-right">Cruise (TAS)</div>
                    <div class="text-right">GPH</div>
                    <div class="text-right">Fuel Capacity</div>
                    <div class="text-right">Fuel Type</div>
                    <div class="text-right">Engine Time</div>
                    <div class="text-right">Airframe Time (next 100hr)</div>
<%
    if(aircraftOwner.isGroup())
    {
%>
                    <div class="text-right">Group</div>
                    <div class="text-right">Owner</div>
<%
    }
    else
    {
%>
                    <div class="text-right">Owner</div>
<%
    }
%>
                    <div class="text-right"></div>
                </div>
                <div class="col-xs-8 col-sm-8 col-md-8 column">
                    <div class="text-left"><a href="aircraftlog.jsp?id=<%=aircraft.getId()%>"><%=aircraft.getId()%></a></div>
                    <div class="text-left"><%=aircraft.getRegistration()%></div>
                    <div class="text-left"><%=aircraft.getMakeModel()%></div>
                    <div class="text-left"><%=homeUrl%></div>
                    <div class="text-left"><%=location%></div>
                    <div class="text-left"><%=aircraft.getSEquipment()%></div>
                    <div class="text-left"><%=aircraft.getSeats()%></div>
                    <div class="text-left"><%=aircraft.getCrew()%></div>
                    <div class="text-left"><%=aircraft.getCruise()%></div>
                    <div class="text-left"><%=aircraft.getGph()%></div>
                    <div class="text-left"><%=Math.round(aircraft.getTotalCapacity())%></div>
                    <div class="text-left"><%=aircraft.getFuelType() == AircraftBean.FUELTYPE_100LL ? "100LL" : "JetA"%></div>
                    <div class="text-left"><%=aircraft.getEngineHoursString()%></div>
                    <div class="text-left"><%=aircraft.getAirframeHoursString() + " " + afTime %></div>
<%
    if(aircraftOwner.isGroup())
    {
%>
                    <div class="text-left"><%=aircraftOwner.getName()%></div>
                    <div class="text-left"><%=ultimateOwner.getName()%></div>
<%
    }
    else
    {
%>
                    <div class="text-left"><%=aircraftOwner.getName()%></div>
<%
    }
%>
                </div>
            </div>
        </div>
    </div>
</div>

