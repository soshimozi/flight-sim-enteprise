<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import=" java.util.List, net.fseconomy.beans.*,  net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="../css/Master.css" rel="stylesheet" type="text/css" />
	<link href="../css/tablesorter-style.css" rel="stylesheet" type="text/css" />

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script type='text/javascript' src='../scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="../scripts/jquery.tablesorter.widgets.js"></script>
	
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
		            window.document.location = "/admin/templateedit.jsp?newtemplate=1";
		    });
		});		
	</script>

</head>
<body>

<jsp:include flush="true" page="../top.jsp" />
<jsp:include flush="true" page="../menu.jsp" />

<div id="wrapper">
<div class="content">
<div class="dataTable">	
<%
	List<TemplateBean> templates = Templates.getAllTemplates();
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
                <th># Grp Locked</th>
			</tr>
		</thead>
		<tbody>
<%
	String type;

	for (TemplateBean template : templates)
	{		
		type = template.getSUnits().toLowerCase().equals("kg") ? "Kg" : "Pax";

        Integer[] statArray = new Integer[5];
		if (MaintenanceCycle.assignmentsPerTemplate != null)
		{
			Integer[] tmpArray = MaintenanceCycle.assignmentsPerTemplate.get(template.getId());
			if (tmpArray != null)
				statArray = tmpArray;
		}		
%>
			<tr class='clickableRow' data-url="/admin/templateedit.jsp?id=<%= template.getId() %>">
				<td><%= template.getId() %></td>
				<td><%= template.getComment() %></td>
				<td><%= template.getCommodity() %></td>
				<td><%= template.getFrequency() %></td>
				<td>
					<%= template.getTargetAmount() %> 
					<%= type %> 
					<span style="color: gray; font-size: 12px;">(<%= template.getAmountDev() %>%)</span>
				</td>
				<td>
					<%= template.getTargetPay() %> 
					<span style="color: gray; font-size: 12px;">(<%= template.getPayDev() %>%)</span>
				</td>
				<td>
					<%= template.getTargetDistance() %> 
					<span style="color: gray; font-size: 12px;">(<%= template.getDistanceDev() %>%)</span>
				</td>
				<td><span title="Max=<%= statArray[MaintenanceCycle.ASSGN_MAX] %>, Min=<%= statArray[MaintenanceCycle.ASSGN_MIN] %>, Avg=<%= statArray[MaintenanceCycle.ASSGN_AVG] %>"><%= statArray[MaintenanceCycle.ASSGN_COUNT] %></span></td>
                <td><%= statArray[MaintenanceCycle.ASSGN_GLOCKED] %></td>
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
