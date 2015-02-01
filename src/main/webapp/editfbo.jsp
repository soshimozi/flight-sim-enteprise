<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="net.fseconomy.beans.*, net.fseconomy.data.*, java.util.List, net.fseconomy.util.Formatters"
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

	String returnPage = request.getHeader("referer");

	String sId = request.getParameter("id");
	int id;
	String error = null;
	
	FboBean fbo;
	
	id = Integer.parseInt(sId);
	fbo = Fbos.getFbo(id);
	
	CachedAirportBean airport = null;
	FboFacilityBean defaultPass = null;
	GoodsBean[] goodsList = null;
	
	if (fbo.updateAllowed(user))
	{	
		defaultPass = Fbos.getFboDefaultFacility(fbo);
	
		goodsList = new GoodsBean[Goods.getMaxCommodityId()+1];
		
		for (int c=0; c < Goods.commodities.length; c++)
		{
            if (Goods.commodities[c] == null)
            {
                continue;
            }
			String prefix = "g_" + c + "_";
			
			String buy = request.getParameter(prefix + "buy");
			String buyPrice = request.getParameter(prefix + "bp");
			String max = request.getParameter(prefix + "max");
			String sell = request.getParameter(prefix + "sell");
			String sellPrice = request.getParameter(prefix + "sp");
			String retain = request.getParameter(prefix + "retain");

            if (buyPrice == null || max == null || sellPrice == null || retain == null)
            {
                continue;
            }
			GoodsBean good = new GoodsBean();
			good.setLocation(fbo.getLocation());
			good.setOwner(fbo.getOwner());
			good.setType(c);
			good.setBuy("true".equals(buy));
			good.setSell("true".equals(sell));
			good.setPriceBuy(Double.parseDouble(buyPrice));
			good.setPriceSell(Double.parseDouble(sellPrice));
			good.setMax(Integer.parseInt(max));
			good.setRetain(Integer.parseInt(retain));
			goodsList[c] = good;
		}
				
        List<GoodsBean> goodsBeanList = Goods.getGoodsForFbo(fbo.getLocation(), fbo.getOwner());
		GoodsBean[] thegoods = goodsBeanList.toArray(new GoodsBean[goodsBeanList.size()]);
        for (GoodsBean thegood : thegoods)
        {
            if (goodsList[thegood.getType()] == null)
                goodsList[thegood.getType()] = thegood;
        }

		airport = Airports.cachedAirports.get(fbo.getLocation());

		for (int c=0; c < goodsList.length; c++)
		{
            if (Goods.commodities[c] == null)
            {
                continue;
            }
				
			if (goodsList[c] == null)
			{
				goodsList[c] = new GoodsBean(Goods.commodities[c], airport.getIcao(), airport.getSize(), airport.getPrice100ll(), 0, airport.getPriceJetA());
				goodsList[c].setBuy(false);
				goodsList[c].setSell(false);
			}
		}
	}
	else
	{
		error = "Permission denied.";
	}
%>

<!DOCTYPE html>
<html lang="en">
<head>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
	
	<title>FSEconomy terminal</title>
	
	<link href="css/Master.css" rel="stylesheet" type="text/css" />

	<script>
		function submitBuildRepair(form){
			if (window.confirm("Do you want to build a Repair Shop?\n2000 Kg of Building Materials will be used."))
			{
				form.event.value = "buildRepair";
				form.submit();
			}
		}
	
		function submitBuildPassenger(form){
			if (window.confirm("Do you want to build a Passenger Shop?\n2000 Kg of Building Materials will be used."))
			{
				form.event.value = "buildPassenger";
				form.submit();
			}
		}
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div class="content">
<%
	if (error != null) 
	{ 
%>
	<div class="error"><%= error %></div>
<%
		return;
	} 
%>
	<div class="form" style="width: 640px">
	<h2>Edit FBO at <%= fbo.getLocation() %></h2>
	<form id="editFboForm" method="post" action="userctl">
		<div>
			<input type="hidden" name="event" value="editFbo"/>
			<input type="hidden" name="location" value="<%= fbo.getLocation() %>"/>
			<input type="hidden" name="owner" value="<%= fbo.getOwner() %>"/>
			<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
		</div>

		<div class="invoiceInset">
		<a title="Upload new invoice background" href="javascript:void(window.open('<%= response.encodeURL("updatebg.jsp?id=" + fbo.getId()) %>','InvoicePaper','status=no,toolbar=no,height=450,width=500'))">
			<img width="200" src="<%= fbo.getInvoiceBackground() %>">
		</a><br>
		Current invoice background
		</div>
	
		<div class="formgroup high">
<%	
	if (fbo.getId() > 0) 
	{
%>	
			<input type="hidden" name="id" value="<%= fbo.getId() %>"/>
<% 	
	}
%>
			Name: <input name="name" type="text" class="textarea" value="<%= fbo.getName() %>" size="40" maxlength="255">
			<br/><br/><br/>
			Sale Price: $<input name="price" type="text" class="textarea" style="text-align:right" value="<%= fbo.isForSale() ? fbo.getPrice() : "" %>" size="10">.00<br/><br/>
		</div>
		<div class="formgroup high">
			<h3>Fuel</h3>
			100LL Price: <input name="fuel100ll" type="text" class="textarea" value="<%= fbo.getFuel100LL() %>" size="4">
			Per Gallon<br/>
			JetA Price: <input name="fueljeta" type="text" class="textarea" value="<%= fbo.getFueljeta() %>" size="4">
			Per Gallon
		</div>
		<div class="formgroup high">
			<h3>Repair Shop</h3>
<% 	
	if ((fbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0) 
	{ 
%>
			Profit margin: <input name="margin" type="text" class="textarea" value="<%= fbo.getRepairShopMargin() %>" size="5">%<br/>
			Equipment install margin: <input name="equipmentInstallMargin" type="text" class="textarea" value="<%= fbo.getEquipmentInstallMargin() %>" size="5">%
<%
	} 
	else 
	{ 
		GoodsBean goods = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
		int amount = goods == null ? 0 : goods.getAmount();
%>
			Repair shop not available. You can build a repair shop with <%= GoodsBean.CONSTRUCT_REPAIRSHOP %> Kg of building supplies.<br/>
<%
		if (amount >= GoodsBean.CONSTRUCT_REPAIRSHOP) 
		{
%>
			<input type="button" onclick="submitBuildRepair(editFboForm)" class="button" value="Build repair shop" />
<%
		} 
	} 
%>
		</div>
	
		<div class="formgroup high">
			<h3>Passenger Terminal</h3>
<% 	if ((fbo.getServices() & FboBean.FBO_PASSENGERTERMINAL) > 0) 
	{
%>
 			You own <%= fbo.getFboSize() * Airports.getTotalFboSlots(fbo.getLocation()) %> Terminal Gates<br />
			<%= defaultPass.getReservedSpace() %> gates are reserved for your own use (the rest can be rented out)
			<br />
			Monthly rent $<%= defaultPass.getRent() %>.00 per gate
			<br/><br/>
			Facilities can be <a href="<%= response.encodeURL("editfbofacility.jsp?facilityId=" + defaultPass.getId()) %>">edited</a> from the Facilities page
<%		
    }
	else
	{ 
		GoodsBean goods = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
		int amount = goods == null ? 0 : goods.getAmount();
%>
			Passenger terminal not available. You can build a passenger terminal with <%= GoodsBean.CONSTRUCT_PASSENGERTERMINAL %> Kg of building supplies.<br/>
<%
		if (amount >= GoodsBean.CONSTRUCT_PASSENGERTERMINAL) 
		{
%>
			<input type="button" onclick="submitBuildPassenger(editFboForm)" class="button" value="Build passenger terminal" />
<%
 		}
	}
%>
		</div>

		<div class="formgroup high">
			<h3>Trading</h3>
			<table>
			<thead>
				<tr>
					<th>Name</th>
					<th>Buy</th>
					<th>Price</th>
					<th>Max</th>
					<th>Sell</th>
					<th>Price</th>
					<th>Retain</th>
				</tr>
			</thead>
			<tbody>
<% 	for (int c=0; c < Goods.commodities.length; c++)
	{
		CommodityBean commodity = Goods.commodities[c];
		GoodsBean good = goodsList[c];
        if (commodity == null)
        {
            continue;
        }

		String prefix = "g_" + c + "_";
%>
				<tr>
					<td><%= commodity.getName() %></td>
					<td><input name="<%= prefix %>buy" type="checkbox" value="true" <%= good.isBuy() ? "checked" :"" %>></td>
					<td><input name="<%= prefix %>bp" type="text" class="textarea" value="<%= Formatters.twoDecimals.format(good.getPriceBuy()) %>" size="7"></td>
					<td><input name="<%= prefix %>max" type="text" class="textarea" value="<%= good.getMax() %>" size="10"></td>
					<td><input name="<%= prefix %>sell" type="checkbox" value="true" <%= good.isSell() ? "checked" :"" %>></td>
					<td><input name="<%= prefix %>sp" type="text" class="textarea" value="<%= Formatters.twoDecimals.format(good.getPriceSell()) %>" size="7"></td>
					<td><input name="<%= prefix %>retain" type="text" class="textarea" value="<%= good.getRetain() %>" size="10"></td>
				</tr>
<% 	
	}
%>
					</tbody>
					</table>
			<ul class="footer">
				<li>All trading prices are per Kg.</li>
				<li>Buy indicates that this FBO is willing to buy the goods from others.</li>
				<li>Sell indicates that this FBO is willing to sell the goods to others.</li>
				<li>Max is the maximum amount of goods this FBO will have in stock.</li>
				<li>Retain is the amount of goods that won't be sold to buyers.</li>
			</ul>
	
		</div>
		<div class="formgroup">
			<input type="submit" class="button" value="Update"/>
		</div>
	</form>
	</div>
	
</div>
</body>
</html>
