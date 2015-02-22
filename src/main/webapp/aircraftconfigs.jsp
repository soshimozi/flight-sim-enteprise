<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.dto.*, net.fseconomy.data.*, java.util.List, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    List<AircraftConfig> aircraftList = Aircraft.getAircraftConfigs();

    String sview = request.getParameter("view");
    int view = 0;
    if (sview != null)
        view = Integer.parseInt(sview);
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />
    <link href="css/tablesorter-style.css" rel="stylesheet" type="text/css" />

    <script type='text/javascript' src="//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
    <script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
    <script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
    <script type='text/javascript' src='scripts/parser-checkbox.js'></script>
    <script type='text/javascript' src='scripts/parser-timeExpire.js'></script>

    <script type="text/javascript">

        $(function() {

            $.extend($.tablesorter.defaults, {
                widthFixed: false,
                widgets : ['zebra','columns']
            });

            $('.configTable').tablesorter();
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<h3>Notes:</h3>
	<ul class="footer">
		<li>Weights are in kg.</li>
		<li>Fuel quantities are in U.S. Gallons</li>
		<li>Cruise Speed is in Knots</li>
		<li>Base Price is the FSE Suggested Price for VFR-equipped Aircraft</li>
		<li>(*) on makemodel name indicates aircraft is not shipable! All other aircraft can be crated and shipped</li>
	</ul>
<%
	if (view == 1)
    {
%>
	<table class="configTable tablesorter-default tablesorter">
	<caption>Aircraft Configurations (<a href="aircraftconfigs.jsp">detailed...</a>)</caption>
	<thead>
	<tr>
		<th>Make &amp; Model</th>
		<th>Addtl Crew</th>
		<th>Seats</th>
		<th>Cruise</th>
		<th>Fuel</th>
		<th>Fuel Type</th>
		<th>GPH</th>
		<th>Payload (0 fuel)</th>
		<th>Base Price</th>
	</tr>
	</thead>
	<tbody>
<%
        for (AircraftConfig aircraft : aircraftList)
        {
        String price = Formatters.currency.format(aircraft.price);
        int totalFuel = aircraft.fcapExt1 + aircraft.fcapLeftTip + aircraft.fcapLeftAux + aircraft.fcapLeftMain +
                aircraft.fcapCenter + aircraft.fcapCenter2 + aircraft.fcapCenter3 +
                aircraft.fcapRightMain + aircraft.fcapRightAux + aircraft.fcapRightTip + aircraft.fcapExt2;

        double fType = aircraft.fueltype;
        String fuelType = "100LL";
        if (fType > 0)
        {
            fuelType = "JetA";
        }
%>
	<tr>
<%
        if (!aircraft.canShip) //add * to denote that model can be shipped to listing
		    aircraft.makemodel += "*";
%>
	<td><%= aircraft.makemodel %></td>
	<td><%= aircraft.crew %></td>
	<td><%= aircraft.seats %></td>
	<td><%= aircraft.cruisespeed %></td>
	<td><%= totalFuel %></td>
	<td><%= fuelType %></td>
	<td><%= aircraft.gph %></td>
	<td><%= aircraft.maxWeight - aircraft.emptyWeight %></td>
	<td><%= price %></td>
	</tr>
<%
	    }
%>
	</tbody>
	</table>
<%	
	}
    else
    {
%>
	<table  class="configTable tablesorter-default tablesorter">
	<caption>Aircraft Configurations (<a href="aircraftconfigs.jsp?view=1">simpler...</a>)</caption>
	<thead>
	<tr>
		<th>Make &amp; Model</th>
		<th>Addtl Crew</th>
		<th>Seats</th>
		<th>Cruise</th>
		<th>Ext 1</th>
		<th>L Tip</th>
		<th>L Aux</th>
		<th>L Main</th>
		<th>Center</th>
		<th>Center 2</th>
		<th>Center 3</th>
		<th>R Main</th>
		<th>R Aux</th>
		<th>R Tip</th>
		<th>Ext 2</th>
		<th>GPH</th>
		<th>Fuel Type</th>
		<th>MTOW</th>
		<th>Empty Weight</th>
		<th>Base Price*</th>
	</tr>
	</thead>
	<tbody>
<%
        for (AircraftConfig aircraft : aircraftList)
        {
            String price = Formatters.currency.format(aircraft.price);
            double fType = aircraft.fueltype;
            String fuelType = "100LL";
            if (fType > 0)
            {
                fuelType = "JetA";
            }
%>
	<tr>
<%
            if (!aircraft.canShip) //add * to denote that model can be shipped to listing
                aircraft.makemodel += "*";
%>
	<td><%= aircraft.makemodel %></td>
	<td><%= aircraft.crew %></td>
	<td><%= aircraft.seats %></td>
	<td><%= aircraft.cruisespeed %></td>
	<td><%= aircraft.fcapExt1 %></td>
	<td><%= aircraft.fcapLeftTip %></td>
	<td><%= aircraft.fcapLeftAux %></td>
	<td><%= aircraft.fcapLeftMain %></td>
	<td><%= aircraft.fcapCenter %></td>
	<td><%= aircraft.fcapCenter2 %></td>
	<td><%= aircraft.fcapCenter3 %></td>
	<td><%= aircraft.fcapRightMain %></td>
	<td><%= aircraft.fcapRightAux %></td>
	<td><%= aircraft.fcapRightTip %></td>
	<td><%= aircraft.fcapExt2 %></td>
	<td><%= aircraft.gph %></td>
	<td><%= fuelType %></td>
	<td><%= aircraft.maxWeight %></td>
	<td><%= aircraft.emptyWeight %></td>
	<td><%= price %></td>
	</tr>
<%
	    }
%>
	</tbody>
	</table>
<%
    }
%>
</div>
</div>
</body>
</html>
