<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.text.*, net.fseconomy.data.*"
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
%>
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="theme/redmond/jquery-ui.css">
    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script type='text/javascript' src='scripts/common.js'></script>
    <script type='text/javascript' src='scripts/css.js'></script>
    <script type='text/javascript' src='scripts/standardista-table-sorting.js'></script>

    <script src="scripts/PopupWindow.js"></script>
    <script type="text/javascript"> var gmap = new PopupWindow(); </script>

    <script src="scripts/jquery.min.js"></script>
    <script src="scripts/jquery-ui.min.js"></script>
    <script src="scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#ownername", "#owner", <%= Data.ACCT_TYPE_ALL %>)
        });

    </script>

</head>


<body>
<jsp:include flush="true" page="top.jsp" />
<div id="wrapper">
<jsp:include flush="true" page="menu.jsp" />
<div class="content">
<%
UserBean Accounts[] = data.getAccounts();
String message = (String) request.getAttribute("message");

if (message != null) 
{
%>
	<div class="message"><%= message %></div>
<%
}
%>
<%	
if (request.getParameter("submit") == null && (message == null)) 
{
%>	<h2>Enter FBO's Owners Account</h2>
	<div class="form" style="width: 400px">
	<form method="post">
	<tr>
	<td>
	Owner Name :
	    <input type="hidden" id="owner" name="owner" value=""/>
	    <input type="text" id="ownername" name="ownername"/>
	</td>
	</tr>
	<input type="submit" class="button" value="GO" />
	<input type="hidden" name="submit" value="true" />
	<input type="hidden" name="return" value="admineditfbo.jsp" />
	</form>
	</div>
<%
} 
else if (request.getParameter("submit") != null) 
{
	String Sid = request.getParameter("owner");
	if (Sid.length() == 0) 
	{
		message = "Owner Not Found";
	}

	if (message != null) 
	{ 
%>		<div class="message"><%= message %></div>
<%		return;	
	}
%>

<div id="wrapper">
<div class="content">
<div class="dataTable">	
<%
	UserBean account = null;
	String sId = request.getParameter("owner");
	if (sId != null && sId.length() > 0)
	{
		int id = Integer.parseInt(request.getParameter("owner"));
		account = data.getAccountById(id);
	}

	FboBean[] fbo = data.getFboByOwner(account.getId(), "location");
	AirportBean[] airports = data.getAirportsForFboConstruction(account.getId());
%>
	<form method="post" action="userctl" name="fboForm">
	<input type="hidden" name="event" value="deleteFbo" />
	<input type="hidden" name="id" value="" />
	<input type="hidden" name="return" value="fbo.jsp<%= account.isGroup() ? "?id=" + account.getId() : "" %>" />
	
	<table id="sortableTablefbo0" class="sortable">
	<caption>
	FBO's owned by <%= account.getName() %>
	<A HREF="gmapfbo.jsp?fboOwner=<%= account.getId() %>"><img src="img/wmap.gif" width="50" height="32" border="0" align="absmiddle" /></a>
	</caption>
	<thead>
	<tr>
		<th>Location</th>
		<th>Name</th>
		<th>Active</th>
		<th>Price</th>
		<th>Lots</th>
		<th>Supplies</th>
		<th>S/Day</th>
		<th>Days</th>
		<th>100LL Fuel</th>
		<th>JetA Fuel</th>
		<th>Bldg. M.</th>
		<th>Status</th>	
	</tr>
	</thead>
	<tbody>
<%
	NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
	for (int c=0; c < fbo.length; c++)
	{
		GoodsBean supplies = data.getGoods(fbo[c].getLocation(), fbo[c].getOwner(), GoodsBean.GOODS_SUPPLIES);
		GoodsBean fuel = data.getGoods(fbo[c].getLocation(), fbo[c].getOwner(), GoodsBean.GOODS_FUEL100LL);
		GoodsBean jeta = data.getGoods(fbo[c].getLocation(), fbo[c].getOwner(), GoodsBean.GOODS_FUELJETA);
		GoodsBean buildingmaterials = data.getGoods(fbo[c].getLocation(), fbo[c].getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
		AirportBean ap = data.getAirport(fbo[c].getLocation());
	%>
	<tr <%= Data.oddLine(c) %>>
		<td><%= data.airportLink(ap, ap, response) %></td>	
		<td><%= fbo[c].getName() %></td>
		<td><%= fbo[c].isActive() ? "Operational" : "Not operational" %></td>
		<td class="numeric"><%= fbo[c].isForSale() ? Formatters.currency.format(fbo[c].getPrice()) + (fbo[c].getPriceIncludesGoods() ? " + goods" : "") : "" %></td>
		<td class="numeric"><%= fbo[c].getFboSize() %></td>
		<td class="numeric"><%= supplies != null ? ((supplies.getAmount() / fbo[c].getSuppliesPerDay(ap) > 14) ? supplies.getAmount() : "<span style=\"color: red;\">" + supplies.getAmount() + "</span>") : "" %></td>
		<td class="numeric"><%= fbo[c].getSuppliesPerDay(ap) %></td>
		<td class="numeric"><%= supplies != null ? ((supplies.getAmount() / fbo[c].getSuppliesPerDay(ap) > 14) ? supplies.getAmount() / fbo[c].getSuppliesPerDay(ap) : "<span style=\"color: red;\">" + supplies.getAmount() / fbo[c].getSuppliesPerDay(ap)+ "</span>" ): "" %></td>
		<td class="numeric"><%= fuel != null ? fuel.getAmount() : "" %></td>
		<td class="numeric"><%= jeta != null ? jeta.getAmount() : "" %></td>
		<td class="numeric"><%= buildingmaterials != null ? buildingmaterials.getAmount() : "" %></td>
		<td> | <a class="link" href="<%= response.encodeURL("admintransferfbo.jsp?id=" + fbo[c].getId()) %>">Transfer</a></td>
	</tr>
<%	}
%>
</tbody>
</table>
</form>

<% 
} 
%>
</div>
</div>

</body>
</html>
