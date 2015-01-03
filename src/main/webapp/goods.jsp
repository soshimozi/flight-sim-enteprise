<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	if(!user.isLoggedIn())
	{
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
		return;
	}

	//setup return page if action used
    response.addHeader("referer", request.getRequestURI());

	String group = request.getParameter("groupid");
	UserBean account = user;
	if (group != null)
		account = Accounts.getAccountById(Integer.parseInt(group));
	
	if ((account.isGroup() && user.groupMemberLevel(Integer.parseInt(group)) < UserBean.GROUP_STAFF)
            || (!account.isGroup() && (account.getId()!= user.getId())))
	{
		// If group account only allow group staff or higher to display goods screen.
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
		return;
	}
	List<GoodsBean> goods = Goods.getGoodsForAccountAvailable(account.getId());
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

		$(function() {

			$.extend($.tablesorter.defaults, {
				widthFixed: false,
				widgets : ['zebra','columns']
			});

			$('.goodsTable').tablesorter();
		});

	</script>
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<div class="dataTable">			
			<table class="goodsTable tablesorter-default tablesorter">
				<caption>Goods owned by <%= account.getName() %></caption>
				<thead>
				<tr>
					<th>Location</th>
					<th>Commodity</th>
					<th>Amount</th>
					<th class="sorter-false">Action</th>
				</tr>
				</thead>
				<tbody>
<%
	for (GoodsBean good : goods)
	{
		int commodity = good.getType();
		String location = good.getLocation();
%>
				<tr>
					<td><a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + location) %>"><%= location %></a></td>
					<td><%= good.getCommodity() %></td>
					<td class="numeric"><%= good.getAmount() %> Kg</td>
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
