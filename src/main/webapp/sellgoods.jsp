<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
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
	String returnPage = request.getHeader("referer");

	UserBean account = user;
	String location = request.getParameter("icao");
	String owner = request.getParameter("owner");
	String sType = request.getParameter("type");
	AirportBean airport = Airports.getAirport(location);
	Airports.fillAirport(airport);
	
	int type = Integer.parseInt(sType);
	List<GoodsBean> salesPoints = Goods.getGoodsAtAirportToSell(location, type, airport.getSize(), airport.getFuelPrice(), airport.getJetAPrice());
	
	if (owner != null)
	{
		int id = Integer.parseInt(owner);
		if (id != user.getId())
		{
			account = Accounts.getAccountById(id);
		}
	}
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
	
	<link href="css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	<div class="dataTable">	
<%
	if (salesPoints.size() > 0)
	{ 
%>
	<table>
		<caption>Interested buyers</caption>
		<thead>
			<tr>
				<th>Name</th>
				<th>Price</th>
				<th>Maximum amount</th>
			</tr>
		</thead>
		<tbody>
<%
		for (GoodsBean good : salesPoints)
		{ 
			int max = good.getAmountAccepted();
			String limit;
			if (max >= 0)
				limit = max + " Kg";
			else
				limit = "unlimited";
%>
		<tr>
			<td><%= good.getOwnerName() %></td>
			<td><%= Formatters.currency.format(good.getPriceBuy()) %></td>
			<td><%= limit %></td>
		</tr>
<%
		} 
%>
		</tbody>
	</table>
	</div>
	<div class="form" style="width: 500px">
		<h2>Sell goods</h2>
		<br/>
		<form method="post" action="userctl">
		<div class="formgroup">
			Sell to: 
			<select name="owner" class="formselect">
<%
		for (GoodsBean good : salesPoints)
		{ 
%>
				<option class="formselect" value="<%= good.getOwner() %>"><%= good.getOwnerName() %></option>
<%
		} 
%>			</select>
		</div>
		<div class="formgroup">
			Amount: <input name="amount" type="text" class="textarea" size="5"/> Kg
		</div>
		<div class="formgroup">
			<input type="submit" class="button" value="Sell" />
		</div>
		<input type="hidden" name="event" value="sellGoods"/>
		<input type="hidden" name="account" value="<%= account.getId() %>"/>	
		<input type="hidden" name="icao" value="<%= location %>"/>	
		<input type="hidden" name="type" value="<%= type %>"/>	
		<input type="hidden" name="returnpage" value="<%=returnPage%>"/>

		</form>
<% 
	} 
	else 
	{
%>
		<div class="message">Nobody accepts the goods</div>	
<%
	}
%>
	</div>
</div>

</div>
</body>
</html>
