<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
<%
    if (!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
        return;
    }

    ModelBean[] models = data.getAllModels();
    FSMappingBean[] mappings = data.getRequestedMappings();
%>
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script type='text/javascript' src='scripts/common.js'></script>
    <script type='text/javascript' src='scripts/css.js'></script>
    <script type='text/javascript' src='scripts/standardista-table-sorting.js'></script>

</head>
<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<div class="content">
<div class="dataTable">	
	<table  id="sortableTableModels" class="sortable">
	<caption>Aircraft models</caption>
	<thead>
	<tr>
		<th>Model</th>
		<th>Crew</th>
		<th>Price</th>
		<th>Rental</th>
		<th>Accounting</th>
		<th>Max Rent Time</th>
		<th>Bonus</th>
		<th>Amount</th>
		<th>For Sale</th>
		<th>AirportSize</th>
		<th>Action</th>
	</tr>
	</thead>
	<tbody>
<%
	for (int c=0; c < models.length; c++)
	{
		ModelBean model = models[c];
		int minutes = model.getMaxRentTime()/60;
		String rentTime = Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60);
%>
	<tr <%= Data.oddLine(c) %>>
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
	<a class="link" href="editmodel.jsp?id=<%= model.getId() %>">Edit</a>
	</td>
	<td></td>
	</tr>
<%
	}
%>
	</tbody>
	</table>
	<div class="formgroup">
	<form method="post" action="editmodel.jsp">
	Create new model based on <select name="newmodel" class="formselect">
	<option value=""></option>
<%
	for (int c=0; c < mappings.length; c++) {
%>
	<option class="formselect" value="<%= mappings[c].getId() %>"><%= mappings[c].getAircraft() %></option>
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
