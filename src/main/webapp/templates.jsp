<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="theme/Master.css" rel="stylesheet" type="text/css" />
	<link href="theme/tablesorter-style.css" rel="stylesheet" type="text/css" />

	<script src="scripts/jquery.min.js"></script>
	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script src="scripts/jquery.tablesorter.widgets.js"></script>
	
	<script type="text/javascript">
		$(function()
        {
			$.extend($.tablesorter.defaults, {
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.templateTable').tablesorter();
		
		    $(".clickableRow").click(function() {
		            window.document.location = $(this).data("url");
		    });

		    $("#newTemplateButton").click(function() {
		            window.document.location = "edittemplate.jsp?newtemplate=1";
		    });
		});		
	</script>

</head>

<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<div class="content">
<div class="dataTable">	
<%
	TemplateBean[] templates = data.getAllTemplates();
%>
	<table class="templateTable tablesorter-default tablesorter">
		<caption>Assignment Templates <input id="newTemplateButton" style="margin-left: 15px;" type="button" class="button" value="New Template"/></caption>
		<thead>
			<tr>
				<td class="sorter-false" colspan="2"><span style="font-size: 12pt;color: gray">Click the desired row to edit</span></td>
			</tr>
			<tr>
				<th>Id</th>
				<th>Comment (Template Name)</th>
				<th>Commodity (Job Name)</th>
				<th>Freq</th>
				<th>Load Info</th>
				<th>Pay Info</th>
				<th>Distance</th>
				<th># Active</th>
			</tr>
		</thead>
		<tbody>
<%
	String type = null;

	for (int c=0; c < templates.length; c++)
	{		
		type = templates[c].getSUnits().toLowerCase() == "kg" ? "Kg" : "Pax";

		int amount = 0;
		Integer[] statArray = new Integer[4];
		if (MaintenanceCycle.assignmentsPerTemplate != null)
		{
			Integer[] tmpArray = (Integer[])MaintenanceCycle.assignmentsPerTemplate.get(new Integer(templates[c].getId()));
			if (tmpArray != null)
				statArray = tmpArray;
		}		
%>
			<tr class='clickableRow' data-url="edittemplate.jsp?id=<%= templates[c].getId() %>">
				<td><%= templates[c].getId() %></td>
				<td><%= templates[c].getComment() %></td>
				<td><%= templates[c].getCommodity() %></td>
				<td><%= templates[c].getFrequency() %></td>
				<td>
					<%= templates[c].getTargetAmount() %> 
					<%= type %> 
					<span style="color: gray; font-size: 12px;">(<%= templates[c].getAmountDev() %>%)</span>
				</td>
				<td>
					<%= templates[c].getTargetPay() %> 
					<span style="color: gray; font-size: 12px;">(<%= templates[c].getPayDev() %>%)</span>
				</td>
				<td>
					<%= templates[c].getTargetDistance() %> 
					<span style="color: gray; font-size: 12px;">(<%= templates[c].getDistanceDev() %>%)</span>
				</td>
				<td><span title="Max=<%= statArray[MaintenanceCycle.ASSGN_MAX] %>, Min=<%= statArray[MaintenanceCycle.ASSGN_MIN] %>, Avg=<%= statArray[MaintenanceCycle.ASSGN_AVG] %>"><%= statArray[MaintenanceCycle.ASSGN_COUNT] %></span></td>
			</tr>
<%
	}
%>
		</tbody>
	</table>
</div>
</div>
</div>
</body>
</html>
