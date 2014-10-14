<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.*"
%>
<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="scripts/AnchorPosition.js"></script>
    <script src="scripts/PopupWindow.js"></script>
    <script type='text/javascript' src='scripts/common.js'></script>
    <script type='text/javascript' src='scripts/css.js'></script>
    <script type='text/javascript' src='scripts/standardista-table-sorting.js'></script>
    <script type="text/javascript">

        var gmapfs = new PopupWindow();
        var gmap = new PopupWindow();

        function doSubmit(fbodesc, id, price, id2)
        {
            if (window.confirm("Do you want to buy " + fbodesc + " for " + price + "?"))
            {
                document.fboForm.id.value = id;
                document.fboForm.account.value = id2;
                document.fboForm.submit();
            }
        }

    </script>
</head>

<body>
<jsp:include flush="true" page="top.jsp" />
<div id="wrapper">
<jsp:include flush="true" page="menu.jsp">
</jsp:include>

<div class="content">
<div class="dataTable">	
<%
	FboBean[] fbo = data.getFboForSale();
	Data.groupMemberData[] staffGroups = user.getStaffGroups();
%>
	<form method="post" action="userctl" name="fboForm">
	<input type="hidden" name="event" value="MarketFbo"/>
	<input type="hidden" name="id">
	<input type="hidden" name="account" value="<%= user.getId() %>"/>
	<input type="hidden" name="return" value="marketfbo.jsp" />

	<table id="sortableTable0" class="sortable">
	<caption>FBOs for sale  
	<a href="#" onclick="gmapfs.setSize(690,535);gmapfs.setUrl('<%= response.encodeURL("gmapmarketfbo.jsp") %>');gmapfs.showPopup('gmapfs');return false;" name="gmapfs" id="gmapfs"><img src="img/wmap.gif" width="50" height="32" border="0" align="absmiddle" /></a>
	</caption>
	<thead>
	<tr>
		<th>Name</th>
		<th>ICAO</th>
		<th>Location</th>
		<th>Goods Included</th>
		<th>Price</th>
		<th>Action</th>
	</tr>
	</thead>
	<tbody>
<%
	for (int c=0; c < fbo.length ; c++)
	{
		int fboid = fbo[c].getId();
		String price = Formatters.currency.format(fbo[c].getPrice());
		int owner=fbo[c].getOwner();
		UserBean fboowner = data.getAccountById(owner);
		String icao = fbo[c].getLocation();
		AirportBean airport = data.getAirport(icao);
		FboFacilityBean [] facility = data.getFboDefaultFacilitiesForAirport(icao);
		int groupOwnerid = data.accountUltimateGroupOwner(owner);
		UserBean ultimateOwner = data.getAccountById(groupOwnerid);
		int totalSpace = fbo[c].getFboSize() * airport.getFboSlots();
		int rented = data.getFboFacilityBlocksInUse(fboid);
 	    String fboservices = "<br>Lots=" + fbo[c].getFboSize() + ",Repair Shop=" + ((fbo[c].getServices() & FboBean.FBO_REPAIRSHOP) > 0 ? "Yes" : "No") + "<br>" + ((fbo[c].getServices() & FboBean.FBO_PASSENGERTERMINAL) > 0 ? totalSpace + " gates (" + rented + " rented)" : "No Passenger Terminal");
		String fboname = fbo[c].getName() + "<br><span class=\"small\"><i>" + fboowner.getName() + (fboowner.isGroup() ? "(" + ultimateOwner.getName() + ")" : "") + fboservices + "</i></span>";
		String location = airport.getCity() + "<br />" + airport.getCountry();
		String icaopopup = "<a HREF=\"#\" onClick=\"gmap.setSize(620,520);gmap.setUrl('gmap.jsp?icao=" + icao + "');gmap.showPopup('gmap');return false;\" NAME=\"gmap\" ID=\"gmap\"><img src=\"" + airport.getDescriptiveImage(fbo)  + "\" align=\"absmiddle\" border=\"0\"/></a>";
		String goodsincluded = "";

		if (fbo[c].getPriceIncludesGoods())
        {
			GoodsBean fuel = data.getGoods(icao, owner, GoodsBean.GOODS_FUEL100LL);
			GoodsBean jeta = data.getGoods(icao, owner, GoodsBean.GOODS_FUELJETA);
			GoodsBean supplies = data.getGoods(icao, owner, GoodsBean.GOODS_SUPPLIES);
			GoodsBean buildingmaterials = data.getGoods(icao, owner, GoodsBean.GOODS_BUILDING_MATERIALS);

			if ((fuel != null ? fuel.getAmount() : 0) > 0)
				goodsincluded = "100LL Fuel: " + Formatters.oneDigit.format(fuel.getAmount()) + " KG";

			if ((jeta != null ? jeta.getAmount() : 0) > 0)
            {
				if (!"".equals(goodsincluded))
					goodsincluded = goodsincluded + "<br>";

				goodsincluded = goodsincluded + "JetA Fuel: " + Formatters.oneDigit.format(jeta.getAmount()) + " KG";
			}
			if ((supplies != null ? supplies.getAmount() : 0) != 0)
            {
				if (!"".equals(goodsincluded))
					goodsincluded = goodsincluded + "<br>";

				goodsincluded = goodsincluded + "Supplies: " + Formatters.oneDigit.format(supplies.getAmount()) + " KG";
			}
			if ((buildingmaterials != null ? buildingmaterials.getAmount() : 0) > 0)
            {
				if (!"".equals(goodsincluded))
					goodsincluded = goodsincluded + "<br>";

				goodsincluded = goodsincluded + "Building Materials: " + Formatters.oneDigit.format(buildingmaterials.getAmount()) + " KG";
			}
		}
%>
	<tr <%= Data.oddLine(c) %>>
	<td><%= fboname %></td>
	<td><%= data.airportLink(airport, airport, response) %></td>
	<td><%= data.sortHelper(airport.getCountry() + ", " + airport.getState() + ", " + airport.getCity()) %><%= location %></td>
	<td><%= goodsincluded %></td>
	<td style="text-align: right;"><%= price %></td>
	<td><a class="link" href="javascript:doSubmit('<%= "(" + icao + ") " + Converters.escapeJavaScript(fbo[c].getName()) %>', '<%= fboid %>', '<%= price %>', <%= user.getId() %>)">Buy</a>
<%
        for (int loop=0; loop < staffGroups.length; loop++)
        {
%>
		| <a class="link" href="javascript:doSubmit('<%= "(" + icao + ") " + Converters.escapeJavaScript(fbo[c].getName()) %>', '<%= fboid %>', '<%= price %>', <%= staffGroups[loop].groupId %>)">Buy for <%= staffGroups[loop].groupName %></a>
<%
        }
%>
	</td>
	</tr>
<%
	}
%>
	</tbody>
	</table>
	</form>
</div>
</div>
</div>
</body>
</html>
