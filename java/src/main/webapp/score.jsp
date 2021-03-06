<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.dto.*, net.fseconomy.data.*, net.fseconomy.util.*, java.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    String type = request.getParameter("type");
    boolean group = type != null && type.equals("groups");
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css" />
    <link href="css/Master.css" rel="stylesheet" type="text/css" />
    <link href="css/tablesorter-style.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
    <script src="scripts/jquery.tablesorter.widgets.js"></script>
    <script type='text/javascript' src='scripts/parser-timeHrMin.js'></script>

    <script type="text/javascript">
        $(function()
        {
            $.extend($.tablesorter.defaults, {
                debug: false,
                widthFixed: true,
                widgets: ['zebra']
            });

            $('.statsTable').tablesorter();
        });
    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	<table class="statsTable tablesorter-default tablesorter">
	    <caption><%= group ? "Groups" : "Pilots" %> - <span style="font-size: 10pt">Note: IE is VERY Slow...(+20secs)</span></caption>
	    <thead>
	    <tr>
		    <th>Name</th>
<%
    if (group)
	{
%>
            <th class="sorter-text">Owner</th>
		    <th class="sorter-currency numeric">Money</th>
<%
    }
%>
            <th class="sorter-digit numeric">Flights</th>
		    <th class="sorter-digit numeric">Miles flown</th>
		    <th class="sorter-timeHrMin numeric">Time flown</th>
		    <th class="sorter-false">Owned aircraft</th>
	    </tr>
	    </thead>
	    <tbody>
<%	
	List<Statistics> stats = Stats.getInstance().getStatistics();
	if(stats != null)
	{
		for (Statistics entry : stats)
		{
            if (entry.group != group)
            {
                continue;
            }
			
			int minutes = entry.totalFlightTime/60;
			String time = (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
%>
			<tr>
			<td><%= entry.accountName  %></td>
<% 			if (group) 
			{ 
%>				<td><%= entry.owner %></td>
				<td class="numeric"><%= Formatters.currency.format(entry.money) %></td>
<%
            }
%>		
			<td class="numeric"><%= entry.flights %></td>
			<td class="numeric"><%= entry.totalMiles %></td>
			<td class="numeric"><%= time %></td>
			<td>
<%
			if (entry.aircraft != null) 
			{
                for (AircraftBean ac : entry.aircraft)
                {
%>                    <%= ac.getRegistration() %> (<%= ac.getMakeModel() %>)<br/>
<%
                }
			}
%>
            </td>
			</tr>
<%
        }
	}
	else
	{
%>
        <tr><td colspan="4">Statistics still being generated</td></tr>
<%
    }
%>
	</tbody>
	</table>
</div>
</div>
</body>
</html>
