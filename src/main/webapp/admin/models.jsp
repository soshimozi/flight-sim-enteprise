<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
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

    List<ModelBean> models = Models.getAllModels();
    List<FSMappingBean> mappings = Aircraft.getRequestedMappings();
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="../css/Master.css" rel="stylesheet" type="text/css" />
	<link rel="stylesheet" type="text/css" href="../css/tablesorter-style.css"/>

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script type='text/javascript' src='../scripts/common.js'></script>
    <script type='text/javascript' src='../scripts/css.js'></script>
	<script type='text/javascript' src='../scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="../scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='../scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='../scripts/parser-timeHrMin.js'></script>
	<script type="text/javascript">

		$(function()
		{
			$.extend($.tablesorter.defaults,
					{
						widthFixed: false,
						widgets : ['zebra','columns']
					});

			$('.modelTable').tablesorter();

		});

	</script>
</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
<div class="dataTable">	
	<table class="modelTable tablesorter-default tablesorter">
	<caption>Aircraft models</caption>
	<thead>
	<tr>
		<th>Model</th>
		<th class="numeric">Crew</th>
		<th class="numeric">Price</th>
		<th class="numeric">Rental</th>
		<th>Accounting</th>
		<th class="sorter-timeHrMin numeric">Max Rent Time</th>
		<th class="numeric">Bonus</th>
		<th class="numeric">Amount</th>
		<th class="numeric">For Sale</th>
		<th class="numeric">AirportSize</th>
		<th class="sorter-false">Action</th>
	</tr>
	</thead>
	<tbody>
<%
	for (ModelBean model : models)
	{
		int minutes = model.getMaxRentTime()/60;
		String rentTime = Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60);
%>
	<tr>
	<td><%= model.getMakeModel() %></td>
	<td><%= model.getCrew() %></td>
	<td><%= model.getPrice() %></td>
	<td><%= model.getRental() %></td>	
	<td><%= model.getSAccounting() %></td>	
	<td><%= rentTime %></td>
	<td><%= model.getBonus() %></td>	
	<td><%= model.getAmount() %></td>
	<td><%= model.getNumSell() %></td>
	<td><%= model.getMinAirportSize() %></td>	
	<td>
	<a class="link" href="/admin/modeledit.jsp?id=<%= model.getId() %>">Edit</a>
	</td>
	<td></td>
	</tr>
<%
	}
%>
	</tbody>
	</table>
	<div class="formgroup">
	<form method="post" action="/admin/modeledit.jsp">
	Create new model based on <select name="newmodel" class="formselect">
	<option value=""></option>
<%
	for (FSMappingBean map : mappings)
    {
%>
	<option class="formselect" value="<%= map.getId() %>"><%= map.getAircraft() %></option>
<%
	}
%>
	</select>	
	<input type="submit" class="button" value="Create">
	</form>
	</div>	
</div>
</div>
</div>
</body>
</html>
