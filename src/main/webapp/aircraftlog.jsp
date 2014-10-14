<%@page
        language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.Formatters"%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
<%
    String aircraft = request.getParameter("registration");
    String sFrom = request.getParameter("from");
    int from = 0;
    if (sFrom != null)
        from = Integer.parseInt(sFrom);

    if(from < 0) // airboss 8/22/13 - prevent negative numbers
        from = 0;

    String linkOptions = "registration=" + aircraft + "&";
    AircraftBean[] aircraftData = data.getAircraftByRegistration(aircraft);
    LogBean[] logs = data.getLogForAircraft(aircraft, from, Data.stepSize);
    String owner = "-";
    if (aircraftData[0].getOwner() != 0)
    {
        UserBean uOwner = data.getAccountById(aircraftData[0].getOwner());
        if (uOwner != null)
            if (uOwner.isGroup())
            {
                UserBean gOwner = data.getAccountById(data.accountUltimateGroupOwner(uOwner.getId()));
                if (gOwner != null)
                {
                    owner = uOwner.getName() + " (" + gOwner.getName() + ")";
                }
                else
                    owner = uOwner.getName();
            }
            else
                owner = uOwner.getName();
    }

    int eminutes = aircraftData[0].getTotalEngineTime()/60;
    int minutes = (aircraftData[0].getTotalEngineTime() - aircraftData[0].getLastCheck())/60;
    int afminutes = aircraftData[0].getAirframe()/60;

    String lastCheck = (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
    String airFrame = (Formatters.twoDigits.format(afminutes/60) + ":" + Formatters.twoDigits.format(afminutes%60));
    String engineHours = (Formatters.twoDigits.format(eminutes/60) + ":" + Formatters.twoDigits.format(eminutes%60));

    int additionalcrew = aircraftData[0].getCrew();

    double fuelCap = aircraftData[0].getTotalCapacity();
    double payLoad = aircraftData[0].getMaxWeight() - aircraftData[0].getEmptyWeight() - (77 * (1 + additionalcrew));
    int payload25 = (int)Math.round(payLoad - fuelCap * 0.25 * Data.GALLONS_TO_KG);
    int payload50 = (int)Math.round(payLoad - fuelCap * 0.50 * Data.GALLONS_TO_KG);
    int payload75 = (int)Math.round(payLoad - fuelCap * 0.75 * Data.GALLONS_TO_KG);
    int payload100 = (int)Math.round(payLoad - fuelCap * Data.GALLONS_TO_KG);
    int payloadnow = (int)Math.round(payLoad - aircraftData[0].getTotalFuel() * Data.GALLONS_TO_KG);
    int crewseats;
    if (additionalcrew > 0)
        crewseats = 2;
    else
        crewseats = 1;
    int seats = aircraftData[0].getSeats() - crewseats;

    int amount = data.getAmountLogForAircraft(aircraft);
    int sellprice=aircraftData[0].getSellPrice();
    String saleprice = Formatters.currency.format(aircraftData[0].getSellPrice());
    String price = Formatters.currency.format(aircraftData[0].getSellPrice());
    String reg = aircraftData[0].getRegistration();
    Data.groupMemberData[] staffGroups = user.getStaffGroups();

    //find how many planes of this type for sale in the FSE world
    int acForSale = data.FindAircraftForSaleByModelCount(aircraftData[0].getModelId());
    //data.AircraftBean[] aircraftForSale = data.findAircraftForSale(aircraftData[0].getModelId(), -1, -1, -1, -1, -1, -1, -1, -1, -1, null, false, false, false, false, false, false, "");
%>
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />
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
                <th>Registration</th>
                <th>Owner</th>
                <th>Type (<a class="normal" href="<%= response.encodeURL("market.jsp?model=" + aircraftData[0].getModelId() + "&submit=" +"true" )%>"><%= acForSale %> for sale</a>)</th>
                <th>Home</th>
                <th>Current Location</th>
            </thead>
            <tr>
                <td align="center"><%= aircraft %></td>
                <td align="center"><%= owner %></td>
                <td align="center"><%= aircraftData[0].getMakeModel() %></td>
                <td align="center"><%= aircraftData[0].getHome() %><%= aircraftData[0].isAdvertiseFerry() ? " (Aircraft is advertised for a ferry flight home)" : "" %></td>
                <td align="center"><%= aircraftData[0].getSLocation() %></td>
            </tr>
        </table><br>
        <table border="1">
            <caption>Specifications</caption>
            <thead>
                <th>Seats</th>
                <th>Addtl Crew</th>
                <th>Cruise Speed</th>
            </thead>
            <tr>
                <td align="center"><%= seats + crewseats %></td>
                <td align="center"><%= additionalcrew %></td>
                <td align="center"><%= aircraftData[0].getCruise() %> kts</td>
            </tr>
        </table><br>
        <table border="1">
            <caption>Fuel</caption>
            <thead>
                <th>Capacity</th>
                <th>Current Load</th>
                <th>Consumption</th>
                <th>Fuel Type</th>
            </thead>
            <tr>
                <td><%= aircraftData[0].getTotalCapacity()%> Gallons</td>
                <td><%= Formatters.oneDigit.format(aircraftData[0].getTotalFuel()) %> Gallons</td>
                <td><%= aircraftData[0].getGph() %> Gallons/Hour</td>
                <td><%= aircraftData[0].getFuelType() > 0 ? "JetA" : "100LL" %></td>
            </tr>
        </table><br>
<%
    if( sellprice != 0)
    {
%>
        <table border="1">
            <thead>
                <th>Asking Price</th>
                <th>Purchase</th>
            </thead>
            <tr>
                <td><%=saleprice%></td>
                <td>
                    <a class="link" href="javascript:doSubmit2('<%= reg %>', '<%= price %>', <%= user.getId() %>)">Buy</a>
<%
        for (int loop=0; loop < staffGroups.length; loop++)
        {
%>
                    <a class="link" href="javascript:doSubmit2('<%= reg %>', '<%= price %>', <%= staffGroups[loop].groupId %>)">Buy for <%= staffGroups[loop].groupName %></a>
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
                <th>Currently</th>
                <th>25% Fuel</th>
                <th>50% Fuel</th>
                <th>75% Fuel</th>
                <th>100% Fuel</th>
            </thead>
            <tr>
                <td>
                    <%= payloadnow %> Kg/<%= (int)Math.round(payloadnow/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, (int)(payloadnow/77)) %> passengers
                </td>
                <td>
                    <%= payload25 %> Kg/<%= (int)Math.round(payload25/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, (int)(payload25/77)) %> passengers
                </td>
                <td>
                    <%= payload50 %> Kg/<%= (int)Math.round(payload50/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, (int)(payload50/77)) %> passengers
                </td>
                <td>
                    <%= payload75 %> Kg/<%= (int)Math.round(payload75/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, (int)(payload75/77)) %> passengers
                </td>
                <td>
                    <%= payload100 %> Kg/<%= (int)Math.round(payload100/0.45359237) %> Lb<br/>
                    <%= Math.min(seats, (int)(payload100/77)) %> passengers
                </td>
            </tr>
        </table><br>
        <table border="1">
            <caption>Equipment</caption>
            <tr>
                <td align="center">VFR</td>
<%
        ModelBean[] Models = data.getModelById(aircraftData[0].getModelId());

        int equipment = aircraftData[0].getEquipment();

        if ((equipment & ModelBean.EQUIPMENT_IFR_MASK) != 0)
            out.println("<td>IFR</td>");
        if ((equipment & ModelBean.EQUIPMENT_AP_MASK) != 0)
            out.println("	<td>Autopilot</td>");
        if ((equipment & ModelBean.EQUIPMENT_GPS_MASK) != 0)
            out.println("	<td>GPS</td>");
%>
            </tr>
        </table><br>
        <table border="1">
            <caption>Hours</caption>
            <thead>
                <th>Engine</th>
                <th>TBO</th>
                <th>Since 100hr</th>
                <th>Airframe</th>
                <th>Max Rental</th>
            </thead>
            <tr>
                <td><%= engineHours %></td>
                <td><%= Models[0].getFueltype()==0 ? AircraftBean.TBO_RECIP/3600 : AircraftBean.TBO_JET/3600 %></td>
                <td><%= lastCheck %></td>
                <td><%= airFrame %></td>
                <td align="center"><%=aircraftData[0].getMaxRentTime()/3600%></td>
            </tr>
        </table>
	</form>
	</div>
	
	<div class="dataTable">
<%
	if (logs.length > 0)
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
        int totalDistance = 0;
        int totalFlightTime = 0;
        double totalMoney = 0;
        for (int c=0; c< logs.length; c++)
        {
            LogBean log = logs[c];
            minutes = log.getTotalEngineTime()/60;
            String engineTime = minutes == 0 ? "" : (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
            minutes = log.getFlightEngineTime()/60;
            String flightTime = minutes == 0 ? "" : (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
            String action = log.getType();
            totalDistance += log.getDistance();
            totalFlightTime += log.getFlightEngineTime();
            float money = 0;

            if(action.equals("disassembly") || action.equals("reassembly"))
                money = log.getIncome();

            if (action.equals("flight"))
            {
                money = log.getRentalCost() + log.getFuelCost();
            } else if (action.equals("refuel"))
            {
                money = -log.getFuelCost();
            } else if (action.equals("maintenance"))
            {
                money = -log.getMaintenanceCost();
            }
            totalMoney += money;
%>
	<tr <%= Data.oddLine(c) %>>
	<td><%= Formatters.getUserTimeFormat(user).format(log.getTime()) %></td>
	<td><%= log.getUser() == null ? "" : log.getUser() %></td>
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
            int newFrom = from - 5*Data.stepSize;
            if (newFrom < 0)
                newFrom = 0;
%>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + newFrom) %>">&lt;&lt;</a>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + (from-Data.stepSize)) %>">&lt;</a>
<%
        }
%>
		</td>
		<td align="right">
<%
        if ((from+Data.stepSize) < amount)
        {
            int newFrom = from+5*Data.stepSize;
            if ((newFrom + Data.stepSize) > amount)
                newFrom = amount-Data.stepSize;
%>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + (from+Data.stepSize)) %>">&gt;</a>
	<a href="<%= response.encodeURL("aircraftlog.jsp?" + linkOptions + "from=" + newFrom) %>">&gt;&gt;</a>
<%
        }
%>
		</td></tr>
	</table>
	<a class="link" href="javascript:void(window.open('<%= response.encodeURL("logviewer.jsp?aircraft=" + aircraft)%>','LogViewer','status=no,toolbar=no,height=750,width=680'))">[View maps]</a>
<%
	}
%>
</div>
</div>
</div>
</body>
</html>
