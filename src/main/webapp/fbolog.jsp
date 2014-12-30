<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    String sFbo = request.getParameter("id");
    String sFrom = request.getParameter("from");
    int from = 0;
    if (sFrom != null)
        from = Integer.parseInt(sFrom);

    int fbo = Integer.parseInt(sFbo);
    String linkOptions = "id=" + fbo + "&";
    List<LogBean> logs = Logging.getLogForFbo(fbo, from, Constants.stepSize);
    int amount = Logging.getAmountLogForFbo(fbo);
    FboBean fboinfo = Fbos.getFbo(fbo);
    AirportBean airport = Airports.getAirport(fboinfo.getLocation());
    String paymentUrl = "paymentlog.jsp?groupid=" + fboinfo.getOwner() + "&fboId=" + fbo;
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="scripts/PopupWindow.js"></script>
    <script type="text/javascript"> var gmap = new PopupWindow(); </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	
	<div class="dataTable">
	<table>
		<caption>FBO log for <%= Airports.airportLink(airport, airport, response) %> | <%= fboinfo.getName() %> | <a class="link" href="<%= response.encodeURL(paymentUrl) %>">Payments...</a></caption>
		<thead>
			<tr>
				<th>Date</th>
				<th>Aircraft</th>
				<th>Action</th>
				<th>Revenue</th>
				<th>Action</th>
			</tr>
		</thead>
		<tbody>
<%
	AircraftBean aircraft;
	int fueltype;
	for (LogBean log : logs)
	{
		String action = log.getType();
		int aircraftId = log.getAircraftId();
		aircraft = Aircraft.getAircraftById(aircraftId);
		fueltype = aircraft.getFuelType();
		float money = 0;
		
		if (action.equals("refuel"))
		{
			money = log.getFuelCost();
		} 
		else if (action.equals("maintenance"))
		{
			money = log.getMaintenanceCost();
		}
%>
		<tr>
			<td><%= Formatters.getUserTimeFormat(user).format(log.getTime()) %></td>
			<td><a class="normal" href="<%= response.encodeURL("aircraftlog.jsp?registration=" + aircraft.getRegistration() ) %>"><%= aircraft.getRegistration() %></a></td>
			<td><%=log.getType().equals("refuel") ? (fueltype < 1 ? log.getSType() + " 100LL" : log.getSType() + " JetA") : log.getSType()%></td>
			<td class="numeric"><%= Formatters.currency.format(money) %></td>
			<td>
<%
		if (action.equals("maintenance") && !log.getSType().contains("Shipment")) 
		{
%>
				<a class="link" href="javascript:void(window.open('<%= response.encodeURL("maintenancelog.jsp?id=" + log.getId())%>','LogViewer','status=no,toolbar=no,height=705,width=640'))">View report</a>
<% 
		} 
%>
			</td>
		</tr>
<%
	}
%>
		<tr>
		<td colspan="7">
			<table width="100%">
				<tr>
				<td align="left">
<% 	if (from > 0) 
	{ 
		int newFrom = from - 5*Constants.stepSize;
		if (newFrom < 0)
			newFrom = 0;
%>
					<a href="<%= response.encodeURL("fbolog.jsp?" + linkOptions + "from=" + newFrom) %>">&lt;&lt;</a>
					<a href="<%= response.encodeURL("fbolog.jsp?" + linkOptions + "from=" + (from-Constants.stepSize)) %>">&lt;</a>
<% 	} 
%>
				</td>
				<td align="right">
<% 	if ((from+Constants.stepSize) < amount)
	{ 
		int newFrom = from+5*Constants.stepSize;
		if ((newFrom + Constants.stepSize) > amount)
			newFrom = amount-Constants.stepSize;
%>
					<a href="<%= response.encodeURL("fbolog.jsp?" + linkOptions + "from=" + (from+Constants.stepSize)) %>">&gt;</a>
					<a href="<%= response.encodeURL("fbolog.jsp?" + linkOptions + "from=" + newFrom) %>">&gt;&gt;</a>
<% } %>
				</td>
				</tr>
			</table>
		</td>
		</tr>
		</tbody>
	</table>
</div>
</div>
</div>
</body>
</html>
