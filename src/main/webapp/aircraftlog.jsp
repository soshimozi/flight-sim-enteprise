<%@page
        language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, java.util.List, net.fseconomy.data.*, net.fseconomy.util.*"
%>
<%@ page import="net.fseconomy.servlets.UserCtl" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    String sFrom = request.getParameter("from");
    int from = 0;

    if (sFrom != null)
    {
        from = Integer.parseInt(sFrom);
    }

    if (from < 0) //prevent negative log id
    {
        from = 0;
    }

    if(request.getParameter("id") == null || request.getParameter("id").equals(""))
    {
        request.getSession().setAttribute("message", "Missing aircraft parameter.");
        request.getRequestDispatcher("error.jsp").forward(request, response);
    }

    int id = -1;
    try
    {
        id = Integer.parseInt(request.getParameter("id"));
    }
    catch(NumberFormatException e)
    {
        UserCtl.logger.error("aircraftlog.jsp: id=" + request.getParameter("id") + ", user: " + user.getName() + ", url=" + request.getRequestURI());
    }


    String linkOptions = "id=" + id + "&";

    AircraftBean aircraftData = Aircraft.getAircraftById(id);

    List<LogBean> logs = Logging.getLogForAircraft(aircraftData.getId(), from, Constants.stepSize);
    String owner = "-";
    if (aircraftData.getOwner() != 0)
    {
        UserBean uOwner = Accounts.getAccountById(aircraftData.getOwner());
        if (uOwner != null)
        {
            if (uOwner.isGroup())
            {
                UserBean gOwner = Accounts.getAccountById(Accounts.accountUltimateGroupOwner(uOwner.getId()));
                if (gOwner != null)
                {
                    owner = uOwner.getName() + " (" + gOwner.getName() + ")";
                }
                else
                {
                    owner = uOwner.getName();
                }
            }
            else
            {
                owner = uOwner.getName();
            }
        }
    }

    int eminutes = aircraftData.getTotalEngineTime()/60;
    int minutes = (aircraftData.getTotalEngineTime() - aircraftData.getLastCheck())/60;
    int afminutes = aircraftData.getAirframe()/60;

    String lastCheck = (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
    String airFrame = (Formatters.twoDigits.format(afminutes/60) + ":" + Formatters.twoDigits.format(afminutes%60));
    String engineHours = (Formatters.twoDigits.format(eminutes/60) + ":" + Formatters.twoDigits.format(eminutes%60));

    int additionalcrew = aircraftData.getCrew();

    double fuelCap = aircraftData.getTotalCapacity();
    double payLoad = aircraftData.getMaxWeight() - aircraftData.getEmptyWeight() - (77 * (1 + additionalcrew));
    int payload25 = (int)Math.round(payLoad - fuelCap * 0.25 * Constants.GALLONS_TO_KG);
    int payload50 = (int)Math.round(payLoad - fuelCap * 0.50 * Constants.GALLONS_TO_KG);
    int payload75 = (int)Math.round(payLoad - fuelCap * 0.75 * Constants.GALLONS_TO_KG);
    int payload100 = (int)Math.round(payLoad - fuelCap * Constants.GALLONS_TO_KG);
    int payloadnow = (int)Math.round(payLoad - aircraftData.getTotalFuel() * Constants.GALLONS_TO_KG);
    int crewseats;

    if (additionalcrew > 0)
    {
        crewseats = 2;
    }
    else
    {
        crewseats = 1;
    }

    int seats = aircraftData.getSeats() - crewseats;

    int amount = Logging.getAmountLogForAircraft(aircraftData.getId());
    int sellprice=aircraftData.getSellPrice();
    String saleprice = Formatters.currency.format(aircraftData.getSellPrice());
    String price = Formatters.currency.format(aircraftData.getSellPrice());
    String reg = aircraftData.getRegistration();
    Groups.groupMemberData[] staffGroups = user.getStaffGroups();

    //find how many planes of this type for sale in the FSE world
    int acForSale = Aircraft.FindAircraftForSaleByModelCount(aircraftData.getModelId());
    //data.AircraftBean[] aircraftForSale = data.findAircraftForSale(aircraftData[0].getModelId(), -1, -1, -1, -1, -1, -1, -1, -1, -1, null, false, false, false, false, false, false, "");
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript">

        function doSubmit(id)
        {
            document.aircraftForm.id.value = id;
            document.aircraftForm.submit();
        }

        function doSubmit2(reg, price, id)
        {
            if (window.confirm("Do you want to buy " + reg + " for " + price + "?"))
            {
                document.aircraftForm.id.value = reg;
                document.aircraftForm.account.value = id;
                document.aircraftForm.submit();
            }
        }

    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	<div class="dataTable">
	<form method="post" action="userctl" name="aircraftForm">
        <input type="hidden" name="event" value="Market"/>
        <input type="hidden" name="id">
        <input type="hidden" name="account" value="<%= user.getId() %>"/>
        <input type="hidden" name="return" value="market.jsp" />
        <table border="1">
            <caption>Aircraft</caption>
            <thead>
            <tr>
                <th>Registration</th>
                <th>Owner</th>
                <th>Type (<a class="normal" href="<%= response.encodeURL("market.jsp?model=" + aircraftData.getModelId() + "&submit=" +"true" )%>"><%= acForSale %> for sale</a>)</th>
                <th>Home</th>
                <th>Current Location</th>
            </tr>
            </thead>
            <tr>
                <td align="center"><%= aircraftData.getRegistration() %></td>
                <td align="center"><%= owner %></td>
                <td align="center"><%= aircraftData.getMakeModel() %></td>
                <td align="center"><%= aircraftData.getHome() %><%= aircraftData.isAdvertiseFerry() ? " (Aircraft is advertised for a ferry flight home)" : "" %></td>
                <td align="center"><%= aircraftData.getSLocation() %></td>
            </tr>
        </table><br>
        <table border="1">
            <caption>Specifications</caption>
            <thead>
            <tr>
                <th>Seats</th>
                <th>Addtl Crew</th>
                <th>Cruise Speed</th>
            </tr>
            </thead>
            <tr>
                <td align="center"><%= seats + crewseats %></td>
                <td align="center"><%= additionalcrew %></td>
                <td align="center"><%= aircraftData.getCruise() %> kts</td>
            </tr>
        </table><br>
        <table border="1">
            <caption>Fuel</caption>
            <thead>
            <tr>
                <th>Capacity</th>
                <th>Current Load</th>
                <th>Consumption</th>
                <th>Fuel Type</th>
            </tr>
            </thead>
            <tr>
                <td><%= aircraftData.getTotalCapacity()%> Gallons</td>
                <td><%= Formatters.oneDigit.format(aircraftData.getTotalFuel()) %> Gallons</td>
                <td><%= aircraftData.getGph() %> Gallons/Hour</td>
                <td><%= aircraftData.getFuelType() > 0 ? "JetA" : "100LL" %></td>
            </tr>
        </table><br>
<%
    if( sellprice != 0)
    {
%>
        <table border="1">
            <thead>
            <tr>
                <th>Asking Price</th>
                <th>Purchase</th>
            </tr>
            </thead>
            <tr>
                <td><%=saleprice%></td>
                <td>
                    <a class="link" href="javascript:doSubmit2('<%= aircraftData.getId() %>', '<%= price %>', <%= user.getId() %>)">Buy</a>
<%
        for (Groups.groupMemberData staffGroup : staffGroups)
        {
%>
                    <a class="link"
                       href="javascript:doSubmit2('<%= aircraftData.getId() %>', '<%= price %>', <%= staffGroup.groupId %>)">Buy
                        for <%= staffGroup.groupName %>
                    </a>
                    <%
                        }
%>
                </td>
            </tr>
        </table>
<%
        }
%>
        <table border="1">
            <caption>Payload capacity</caption>
            <thead>
            <tr>
                <th>Currently</th>
                <th>25% Fuel</th>
                <th>50% Fuel</th>
                <th>75% Fuel</th>
                <th>100% Fuel</th>
            </tr>
            </thead>
            <tr>
                <td>
                    <%= payloadnow %> Kg/<%= (int)Math.round(payloadnow/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, payloadnow/77) %> passengers
                </td>
                <td>
                    <%= payload25 %> Kg/<%= (int)Math.round(payload25/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, payload25/77) %> passengers
                </td>
                <td>
                    <%= payload50 %> Kg/<%= (int)Math.round(payload50/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, payload50/77) %> passengers
                </td>
                <td>
                    <%= payload75 %> Kg/<%= (int)Math.round(payload75/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, payload75/77) %> passengers
                </td>
                <td>
                    <%= payload100 %> Kg/<%= (int)Math.round(payload100/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, payload100/77) %> passengers
                </td>
            </tr>
        </table><br>
        <table border="1">
            <caption>Equipment</caption>
            <tr>
                <td align="center">VFR</td>
<%
        int equipment = aircraftData.getEquipment();

        StringBuilder sb = new StringBuilder();
        if ((equipment & ModelBean.EQUIPMENT_IFR_MASK) != 0)
        {
            sb.append("<td>IFR</td>");
        }
        if ((equipment & ModelBean.EQUIPMENT_AP_MASK) != 0)
        {
            sb.append("	<td>Autopilot</td>");
        }
        if ((equipment & ModelBean.EQUIPMENT_GPS_MASK) != 0)
        {
            sb.append("	<td>GPS</td>");
        }
%>
            <%=sb.toString()%>
            </tr>
        </table><br>
        <table border="1">
            <caption>Hours</caption>
            <thead>
            <tr>
                <th>Engine</th>
                <th>TBO</th>
                <th>Since 100hr</th>
                <th>Airframe</th>
                <th>Max Rental</th>
            </tr>
            </thead>
            <tr>
                <td><%= engineHours %></td>
                <td><%= aircraftData.getFuelType()==0 ? AircraftBean.TBO_RECIP/3600 : AircraftBean.TBO_JET/3600 %></td>
                <td><%= lastCheck %></td>
                <td><%= airFrame %></td>
                <td align="center"><%=aircraftData.getMaxRentTime()/3600%></td>
            </tr>
        </table>
	</form>
	</div>
	
	<div class="dataTable">
<%
	if (logs.size() > 0)
	{
%>
	<table>
	<caption>Aircraft log </caption>
	<thead>
	    <tr>
            <th>Date</th>
            <th>Pilot</th>
            <th>Action</th>
            <th>Duration</th>
            <th>Distance</th>
            <th>Total engine time</th>
            <th>Money</th>
            <th>Action</th>
        </tr>
	</thead>
<%
        for (LogBean log : logs)
        {
            minutes = log.getTotalEngineTime()/60;
            String engineTime = minutes == 0 ? "" : (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
            minutes = log.getFlightEngineTime()/60;
            String flightTime = minutes == 0 ? "" : (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
            String action = log.getType();
            float money = 0;

            if (action.equals("disassembly") || action.equals("reassembly"))
            {
                money = log.getIncome();
            }

            switch(action)
            {
            case "flight":
                money = log.getRentalCost() + log.getFuelCost();
                break;
            case "refuel":
                money = -log.getFuelCost();
                break;
            case "maintenance":
                money = -log.getMaintenanceCost();
                break;
            }
%>
	<tr>
	<td><%= Formatters.getUserTimeFormat(user).format(log.getTime()) %></td>
	<td><%= log.getUserId() == 0 ? "" : Accounts.getAccountNameById(log.getUserId()) %></td>
	<td><%= log.getSType() %></td>
	<td><%= flightTime %></td>
	<td><%= log.getDistance() == 0 ? "" : ("" + log.getDistance()) %></td>
	<td><%= engineTime %></td>
	<td class="numeric"><%= Formatters.currency.format(money) %></td>
	<td>
<%
	        if (action.equals("maintenance") && !log.getSType().contains("Shipment"))
            {
%>
		<a class="link" href="javascript:void(window.open('<%= response.encodeURL("maintenancelog.jsp?id=" + log.getId())%>','LogViewer','status=no,toolbar=no,height=705,width=640'))">View report</a></td>
<%
            }
%>
	</tr>
<%
	    }
%>
	<tr><td colspan="7"><table width="100%"><tr>
		<td align="left">
<%
        if (from > 0)
        {
            int newFrom = from - 5*Constants.stepSize;
            if (newFrom < 0)
            {
                newFrom = 0;
            }
%>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + newFrom) %>">&lt;&lt;</a>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + (from-Constants.stepSize)) %>">&lt;</a>
<%
        }
%>
		</td>
		<td align="right">
<%
        if ((from+Constants.stepSize) < amount)
        {
            int newFrom = from+5*Constants.stepSize;
            if ((newFrom + Constants.stepSize) > amount)
            {
                newFrom = amount - Constants.stepSize;
            }
%>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + (from+Constants.stepSize)) %>">&gt;</a>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + newFrom) %>">&gt;&gt;</a>
<%
        }
%>
		</td></tr>
	</table>
	<a class="link" href="javascript:void(window.open('<%= response.encodeURL("logviewer.jsp?aircraftid=" + id)%>','LogViewer','status=no,toolbar=no,height=750,width=680'))">[View maps]</a>
<%
	}
%>
</div>
</div>
</div>
</body>
</html>
