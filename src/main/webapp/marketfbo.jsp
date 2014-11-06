<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

    String returnPage = request.getRequestURI();
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="/scripts/AnchorPosition.js"></script>
    <script src="/scripts/PopupWindow.js"></script>
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
<jsp:include flush="true" page="menu.jsp"></jsp:include>

<div id="wrapper">
<div class="content">
<div class="dataTable">	
<%
	List<FboBean> fbos = Fbos.getFboForSale();
    Accounts.groupMemberData[] staffGroups = user.getStaffGroups();
%>
	<form method="post" action="userctl" name="fboForm">
	<input type="hidden" name="event" value="MarketFbo"/>
	<input type="hidden" name="id">
	<input type="hidden" name="account" value="<%= user.getId() %>"/>
	<input type="hidden" name="returnpage" value="marketfbo.jsp" />

	<table id="sortableTable0" class="sortable">
	<caption>FBOs for sale  
	<a href="#" onclick="gmapfs.setSize(690,535);gmapfs.setUrl('<%= response.encodeURL("gmapmarketfbo.jsp") %>');gmapfs.showPopup('gmapfs');return false;" id="gmapfs"><img src="img/wmap.gif" width="50" height="32" border="0" align="absmiddle" /></a>
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
	for (FboBean fbo : fbos)
	{
		int fboid = fbo.getId();
		String price = Formatters.currency.format(fbo.getPrice());
		int owner=fbo.getOwner();
		UserBean fboowner = Accounts.getAccountById(owner);
		String icao = fbo.getLocation();
		AirportBean airport = Airports.getAirport(icao);
		int groupOwnerid = Accounts.accountUltimateGroupOwner(owner);
		UserBean ultimateOwner = Accounts.getAccountById(groupOwnerid);
		int totalSpace = fbo.getFboSize() * airport.getFboSlots();
		int rented = Fbos.getFboFacilityBlocksInUse(fboid);
 	    String fboservices = "<br>Lots=" + fbo.getFboSize() + ",Repair Shop=" + ((fbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0 ? "Yes" : "No") + "<br>" + ((fbo.getServices() & FboBean.FBO_PASSENGERTERMINAL) > 0 ? totalSpace + " gates (" + rented + " rented)" : "No Passenger Terminal");
		String fboname = fbo.getName() + "<br><span class=\"small\"><i>" + fboowner.getName() + (fboowner.isGroup() ? "(" + ultimateOwner.getName() + ")" : "") + fboservices + "</i></span>";
		String location = airport.getCity() + "<br />" + airport.getCountry();
		String goodsincluded = "";

		if (fbo.getPriceIncludesGoods())
        {
			GoodsBean fuel = Goods.getGoods(icao, owner, GoodsBean.GOODS_FUEL100LL);
			GoodsBean jeta = Goods.getGoods(icao, owner, GoodsBean.GOODS_FUELJETA);
			GoodsBean supplies = Goods.getGoods(icao, owner, GoodsBean.GOODS_SUPPLIES);
			GoodsBean buildingmaterials = Goods.getGoods(icao, owner, GoodsBean.GOODS_BUILDING_MATERIALS);

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
	<tr>
	<td><%= fboname %></td>
	<td><%= Airports.airportLink(airport, airport, response) %></td>
	<td><%= data.sortHelper(airport.getCountry() + ", " + airport.getState() + ", " + airport.getCity()) %><%= location %></td>
	<td><%= goodsincluded %></td>
	<td style="text-align: right;"><%= price %></td>
	<td><a class="link" href="javascript:doSubmit('<%= "(" + icao + ") " + Converters.escapeJavaScript(fbo.getName()) %>', '<%= fboid %>', '<%= price %>', <%= user.getId() %>)">Buy</a>
<%
        for (int loop=0; loop < staffGroups.length; loop++)
        {
%>
		| <a class="link" href="javascript:doSubmit('<%= "(" + icao + ") " + Converters.escapeJavaScript(fbo.getName()) %>', '<%= fboid %>', '<%= price %>', <%= staffGroups[loop].groupId %>)">Buy for <%= staffGroups[loop].groupName %></a>
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
