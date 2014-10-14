<%@ page language="java"
	import="java.text.*, net.fseconomy.data.*"
%>

<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
	//setup return page if action used
	String returnPage = request.getRequestURI();
    response.addHeader("referer", request.getRequestURI());

	String group = request.getParameter("groupId");
	UserBean account = user;
	if (group != null)
		account = data.getAccountById(Integer.parseInt(group));
	
	if (account.isGroup() && user.groupMemberLevel(Integer.parseInt(group)) < UserBean.GROUP_STAFF) 
	{
		// If group account only allow group staff or higher to display goods screen.
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return; 
	}
	if (!account.isGroup() && (account.getId()!= user.getId())) 
	{
		// If user account only allow the signed in user to display the goods screen.
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return; 
	}	
	GoodsBean[] goods = data.getGoodsForAccountAvailable(account.getId());
	NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
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
	
	<script>
		function doCalc(form) 
		{
			var kg = form.elements["kilograms"];
			var gal = form.elements["gallons"];
			if (parseInt(kg.value) > 0) 
			{
				gal.value = parseInt(kg.value)/2.68735
			}
			else 
			{
				kg.value = parseInt(gal.value)*2.68735
			}
		}
		
		function doReset(form) 
		{
			form.elements["kilograms"].value = 0;
			form.elements["gallons"].value = 0;
		}
	</script>
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<div class="dataTable">			
			<table id="sortableTable0" class="sortable">
				<caption>Goods owned by <%= account.getName() %></caption>
				<thead>
				<tr>
					<th>Location</th>
					<th>Commodity</th>
					<th>Amount</th>
					<th>Action</th>
				</tr>
				</thead>
				<tbody>
<%
	for (int c=0; c< goods.length; c++)
	{
		int commodity = goods[c].getType();
		String location = goods[c].getLocation();
%>
				<tr <%= Data.oddLine(c) %>>
					<td><a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + location) %>"><%= location %></a></td>
					<td><%= goods[c].getCommodity() %></td>
					<td class="numeric"><%= goods[c].getAmount() %> Kg</td>
					<td>
						<a class="link" href="<%= response.encodeURL("sellgoods.jsp?icao=" + location + "&owner=" + account.getId() + "&type=" + commodity) %>">Sell</a>
						<a class="link" href="<%= response.encodeURL("editassignment.jsp?from=" + location + "&owner=" + account.getId() + "&commodityId=" + commodity) %>">Create transfer assignment</a>
						<a class="link" href="<%= response.encodeURL("transfergoods.jsp?fromICAO=" + location + "&owner=" + account.getId() + "&commodityId=" + commodity) %>">Transfer to new Owner</a>
					</td>
				</tr>
<% 
	}
%>
				</tbody>
			</table>
		</div>
		<div class="dataTable">	
			<form name="calcgallons">
				<input name="kilograms" type="text" class="textarea" value="0" />
		      	<input name="calc" type="button" class="button" onclick="doCalc(this.form)" VALUE="Kilograms <-- Calculate --> Gallons">	
		      	<input name="gallons" type="text" class="textarea" value="0"/>
		      	<input name="calc" type="button" class="button" onclick="doReset(this.form)" VALUE="Clear">	
		    </form>		
		</div>
	</div>
</div>
</body>
</html>
