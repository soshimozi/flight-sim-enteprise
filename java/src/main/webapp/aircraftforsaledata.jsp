<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>
<%@ page import="net.fseconomy.util.Formatters" %>
<%@ page import="net.fseconomy.beans.*" %>
<%@ page import="net.fseconomy.data.*" %>
<%@ page import="net.fseconomy.util.Helpers" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
    if(!user.isLoggedIn())
    {
%>
        <script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }
%>

<%
    String sAction = request.getParameter("action");
    if (Helpers.isNullOrBlank(sAction))
        return;

    int modelId = Helpers.isNullOrBlank(request.getParameter("model")) ? -1 : Integer.parseInt(request.getParameter("model"));
    int lowPrice = Helpers.isNullOrBlank(request.getParameter("lowPrice")) ? -1 : Integer.parseInt(request.getParameter("lowPrice"));
    int highPrice = Helpers.isNullOrBlank(request.getParameter("highPrice")) ? -1: Integer.parseInt(request.getParameter("highPrice"));
    int lowTime = Helpers.isNullOrBlank(request.getParameter("lowTime")) ? -1 : Integer.parseInt(request.getParameter("lowTime"));
    int highTime = Helpers.isNullOrBlank(request.getParameter("highTime")) ? -1 : Integer.parseInt(request.getParameter("highTime"));
    int distance = Helpers.isNullOrBlank(request.getParameter("distance")) ? -1 : Integer.parseInt(request.getParameter("distance"));
    int lowPax = Helpers.isNullOrBlank(request.getParameter("lowPax")) ? -1 : Integer.parseInt(request.getParameter("lowPax"));
    int highPax = Helpers.isNullOrBlank(request.getParameter("highPax")) ? -1 : Integer.parseInt(request.getParameter("highPax"));
    int lowLoad = Helpers.isNullOrBlank(request.getParameter("lowLoad")) ? -1 : Integer.parseInt(request.getParameter("lowLoad"));
    int highLoad = Helpers.isNullOrBlank(request.getParameter("highLoad")) ? -1 : Integer.parseInt(request.getParameter("highLoad"));
    String equipment = Helpers.isNullOrBlank(request.getParameter("equipment")) ? "all" : request.getParameter("equipment");
    String fromParam = Helpers.isNullOrBlank(request.getParameter("from")) ? null : request.getParameter("from");
    boolean hasVfr = request.getParameter("hasVfr") != null;
    boolean hasIfr = request.getParameter("hasIfr") != null;
    boolean hasAp = request.getParameter("hasAp") != null;
    boolean hasGps = request.getParameter("hasGps") != null;
    boolean isSystemOwned = request.getParameter("isSystemOwned") != null;
    boolean isPlayerOwned = request.getParameter("isPlayerOwned") != null;

    StringBuilder queryURL = new StringBuilder("gmapmarket.jsp?");

    queryURL.append("modelId=");
    queryURL.append(modelId);
    queryURL.append("&lowPrice=");
    queryURL.append(lowPrice);
    queryURL.append("&highPrice=");
    queryURL.append(highPrice);
    queryURL.append("&lowTime=");
    queryURL.append(lowTime);
    queryURL.append("&highTime=");
    queryURL.append(highTime);
    queryURL.append("&lowPax=");
    queryURL.append(lowPax);
    queryURL.append("&highPax=");
    queryURL.append(highPax);
    queryURL.append("&lowLoad=");
    queryURL.append(lowLoad);
    queryURL.append("&highLoad=");
    queryURL.append(highLoad);
    queryURL.append("&distance=");
    queryURL.append(distance);

    if (fromParam != null )
    {
        queryURL.append("&from=");
        queryURL.append(fromParam);
    }

    List<AircraftBean> aircraftList = null;
    String error = null;
    switch(sAction)
    {
        case "privatesale":
            aircraftList = Aircraft.getAircraftForPrivateSaleById(user.getId());
            break;
        case "search":
            if(!Helpers.isNullOrBlank(fromParam) && !Airports.cachedAirports.containsKey(fromParam.toUpperCase()))
            {
                error = "Error: Bad From ICAO";
                aircraftList = new ArrayList<AircraftBean>();
            }
            else
                aircraftList = Aircraft.findAircraftForSale(modelId, lowPrice, highPrice, lowTime, highTime, lowPax, highPax, lowLoad, highLoad, distance, fromParam, hasVfr, hasIfr, hasAp, hasGps, isSystemOwned, isPlayerOwned, equipment);
            break;
        default:
            return;
    }

    if(error != null)
    {
%>
<div class="error"><%= error %></div>
<%
        return;
    }
%>

<div class="dataTable">
    <form method="post" action="userctl" id="aircraftForm">
        <div>
            <input type="hidden" name="event" value="Market"/>
            <input type="hidden" name="id" />
            <input type="hidden" name="account" value="<%= user.getId() %>" />


            <table class="aircraftTable tablesorter-default tablesorter">
                <caption><%="privatesale".contains(sAction) ? "Private Aircraft Sale Offers" : "Aircraft for sale"%>
                    <a href="#" onclick="gmapfs.setSize(620,520);gmapfs.setUrl('<%= response.encodeURL(queryURL.toString()) %>');gmapfs.showPopup('gmapfs');return false;" id="gmapfs">
                        <img src="img/wmap.gif" width="50" height="32" style="border-style: none; vertical-align:middle;" />
                    </a>
                </caption>

                <thead>
                <tr>
                    <th colspan="7" class="sorter-false disabledtext" style="background-color: lightsalmon">Click Aircraft Registration to view information and purchase</th>
                </tr>
                <tr>
                    <th>Registration</th>
                    <th>Type</th>
                    <th>Equipment</th>
                    <th>Location</th>
                    <th>Price</th>
                    <th>Engine</th>
                    <th>Airframe (100Hr due in)</th>
                </tr>
                </thead>
                <tbody>
<%
    for (AircraftBean aircraft : aircraftList)
    {
        String reg = aircraft.getRegistration();
        String reg2;
        String acLocation = "In Flight";
        String acICAO = acLocation;
        String price = Formatters.currency.format(aircraft.getSellPrice());
        if (aircraft.getLocation() != null)
        {
            CachedAirportBean airportInfo = Airports.cachedAirports.get(aircraft.getLocation());

            acLocation=airportInfo.getTitle();
            acICAO=aircraft.getLocation();
        }

        int owner=aircraft.getOwner();
        if (owner != 0)
        {
            reg2 = reg + "*";
        }
        else
        {
            reg2 = reg;
        }

        // Calculate the airframe time
        int afminutes = aircraft.getAirframe()/60;
        String afTime = aircraft.getAirframeHoursString();
%>
                <tr onclick="selectAircraft(<%=aircraft.getId()%>)" style="cursor: pointer;">
                    <td><%= reg2 %></td>
                    <td><%= aircraft.getMakeModel() %></td>
                    <td><%= aircraft.getSEquipment() %></td>
                    <td><a title="<%=acLocation%>" class="normal"><%= acICAO %></a></td>
                    <td><%= price %></td>
                    <td><%= aircraft.getEngineHoursString() %></td>
                    <td><%= afTime %></td>
                </tr>
<%
    }
%>
                </tbody>
            </table>
        </div>
    </form>
    <ul class="footer">
        <li>A * in the registration column means the aircraft is privately owned.</li>
        <li>A number in (parens) in the Airframe column is hours:mins to the next 100hr maintenance.</li>
    </ul>

</div>

<script>
    $('.aircraftTable').tablesorter();
</script>
