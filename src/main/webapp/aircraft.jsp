<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="net.fseconomy.beans.*, java.util.List, net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

	String returnPage = "";
	UserBean account = null;
	boolean showActions = true;
	String sGroupId = request.getParameter("id");
	
	//setup return page if action used
	String groupParam = sGroupId != null ? "?id=" + sGroupId : "";
	returnPage = request.getRequestURI() + groupParam;
    response.addHeader("referer", request.getRequestURI() + groupParam);

	if (sGroupId != null)
	{
		int groupId = Integer.parseInt(sGroupId);
		account = Accounts.getAccountById(groupId);
		if (account != null)
		{
			if (account.isGroup() == false || user.groupMemberLevel(groupId) < UserBean.GROUP_INVITED)
				account = null;
			
			if (account != null)
				showActions = user.groupMemberLevel(groupId) >= UserBean.GROUP_STAFF;
		}
	}
	
	if (account == null)
		account = user;
	
	List<AircraftBean> aircraftList = Aircraft.getAircraftOwnedByUser(account.getId());
%>

<!DOCTYPE html>
<html lang="en">
<head>
	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel="stylesheet" type="text/css" href="/theme/Master.css" />
	<link rel="stylesheet" type="text/css" href="/theme/tablesorter-style.css" />

	<script src="/scripts/jquery.min.js"></script>
	
	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="/scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
	
	<script src="/scripts/PopupWindow.js"></script>
	
	<script type="text/javascript">
		var gmapac = new PopupWindow();
		
		function doRental(reg, type)
		{
			var form = document.getElementById("aircraftForm");
			form.reg.value = reg;
			form.rentalType.value = type;
			form.elements["returnpage"].value = "myflight.jsp";
			form.submit();
		}
		
		function actions(sel)
		{
			var selItemText = sel[sel.selectedIndex].text;
			var selItemVal = sel[sel.selectedIndex].value;
			
			if( selItemText.indexOf("Rent") >= 0)
			{
				var s = selItemVal.split(",");
				var reg = s[0];
				var type = s[1];
				doRental(reg, type);
			}	
			else if(selItemText.indexOf("Return Lease") >= 0)
			{
				if(window.confirm("Are you sure you want to return this Lease?"))
				{
					var s = selItemVal.split(":");
					var reg = s[1].trim();
		
					var form = document.getElementById("aircraftForm");
					form.event.value="AircraftLeaseReturn";
					form.reg.value = reg;
					form.id.value = -1;
					form.submit();
				}
			}
			else 
			{
				var url = selItemVal;
				location.href = url;
			}		
		}
	</script>

	<script type="text/javascript">
	
		$(function() 
		{		
			$.extend($.tablesorter.defaults, 
			{
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.aircraftTable').tablesorter();		
		});
	
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	<form method="post" action="userctl" id="aircraftForm">
		<div>
			<input type="hidden" name="event" value="Aircraft"/>
			<input type="hidden" name="reg" />
			<input type="hidden" name="type" value="add" />
			<input type="hidden" name="rentalType" />
			<input type="hidden" name="returnpage" value="<%=returnPage%>" />
		</div>
	<table class="aircraftTable tablesorter-default tablesorter">
	<caption>
	Aircraft owned by <%= account.getName() %> 
<% 
	if(aircraftList.size() > 0) 
	{
%>
		<a href="#" onclick="gmapac.setSize(620,520);gmapac.setUrl('<%= response.encodeURL("gmapac.jsp?Id=" + account.getId()) %>');gmapac.showPopup('gmapac');return false;" id="gmapac"><img src="img/wmap.gif" style="border-style: none; vertical-align:middle;" /></a>	
<%
	}
%>
	</caption>
	<thead>
	<tr>
		<th style="width: 85px">Registration</th>
		<th >Type</th>
		<th style="width: 35px">Location</th>
		<th style="width: 35px">Home</th>
		<th class="numeric" style="width: 75px">Price</th>
		<th class="sorter-timeHrMin numeric" style="width: 45px">Engine Total</th>
		<th class="sorter-timeHrMin numeric" style="width: 45px">Since check</th>
		<th class="numeric" style="width: 75px">Rental Price</th>
		<th class="numeric" style="width: 45px">Bonus</th>
		<th class="sorter-false" style="width: 75px">Action</th>
	</tr>
	</thead>
	<tbody>
<%
	for (AircraftBean aircraft : aircraftList)
	{
		int priceDry= aircraft.getRentalPriceDry();
		int priceWet= aircraft.getRentalPriceWet();
        int tbo = aircraft.getFuelType()==0 ? AircraftBean.TBO_RECIP/3600 : AircraftBean.TBO_JET/3600;
		String price = "";
		
		if (priceDry > 0)
			price = "$" + priceDry + " Dry";
			
		if (priceWet > 0)
			price = price + ((priceDry > 0) ? "/" : "") + "$" + priceWet + " Wet";
			
		price = price + (aircraft.getAccounting() == AircraftBean.ACC_TACHO ? " [Tacho]" : " [Hour]");
		int minutes = (aircraft.getTotalEngineTime() - aircraft.getLastCheck())/60;
		int hours = minutes / 60;
		String lastCheck = (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
		minutes = (aircraft.getTotalEngineTime())/60;
        int ehours = minutes / 60;
		String engineTotal= (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
%>
	<tr>

	<td><a href="<%= response.encodeURL("aircraftlog.jsp?registration=" + aircraft.getRegistration()) %>"><%= aircraft.getRegistration() %></a>
        <% if (aircraft.isBroken())
              {%>              
              <img src='img/repair.gif' style="border-style: none; vertical-align:middle;" />
            <%} %>    
    </td>
	<td><%= aircraft.getMakeModel() %></td>
	<td>
<% 		
		if (aircraft.getLocation() != null) 
		{ 
%>
	        <a href="<%= response.encodeURL("airport.jsp?icao=" + aircraft.getLocation()) %>"><%= aircraft.getSLocation() %></a>
<%		
		} 
		else
		{ 
%>
		    <%= aircraft.getSLocation() %>
<%
		} 
%>	
	</td>
	<td><a href="<%= response.encodeURL("airport.jsp?icao=" + aircraft.getHome()) %>"><%= aircraft.getHome() %></a></td>
<%
		if(aircraft.getLessor() == 0)
		{
%>			
			<td class="numeric"><%= aircraft.getSellPrice() == 0? "Not for sale" : Formatters.currency.format(aircraft.getSellPrice()) %></td>
<%
		}
		else
		{
			if( account.getId() == aircraft.getLessor()) //this is the real owner
			{
				UserBean displayName = Accounts.getAccountById(aircraft.getOwner());
%>
			<td class="numeric">Leased to: <%=displayName.getName()%></td>
<%
			}
			else
			{
				UserBean displayName = Accounts.getAccountById(aircraft.getLessor());
%>
			<td class="numeric">Leased from: <%=displayName.getName()%></td>
<%
			}
		}
%>
    <td class="numeric" <%= ehours >= tbo ? " style=\"color: red;\"" : "" %>><%= engineTotal %></td>  
	<td class="numeric" <%= hours >= 90 ? " style=\"color: red;\"" : "" %>><%= lastCheck %></td>
	<td class="numeric"><%= price %></td>
	<td class="numeric"><%= Formatters.currency.format(aircraft.getBonus()) %></td>
<%		
		if(aircraft.getShippingState() == 0 && aircraft.getLessor() != account.getId())
		{
			if (showActions)  
			{ 
%>
	<td>
	<select onchange="actions(this);">
	<option value="0">Select Action</option>
	<option value="<%= response.encodeURL("editaircraft.jsp?registration=" + aircraft.getRegistration()) %>">Edit</option>
	<option value="<%= response.encodeURL("aircraftlog.jsp?registration=" + aircraft.getRegistration()) %>">Log</option>
	<option value="<%= response.encodeURL("maintenance.jsp?registration=" + aircraft.getRegistration()) %>">Maintenance</option>
	<option value="<%= response.encodeURL("transferac.jsp?registration=" + aircraft.getRegistration()) %>">Transfer</option>
<%
				if(aircraft.getLessor() == 0)
				{
%>
					<option value="<%= response.encodeURL("leaseac.jsp?registration=" + aircraft.getRegistration()) %>">Lease</option>
<%
					if (aircraft.getCanShip()) 
					{ 
%>
						<option value="<%= response.encodeURL("shipaircraft.jsp?registration=" + aircraft.getRegistration()) %>">Ship</option>
<% 		
					} 
				}
				else
				{
%>	
					<option value="LeaseReturn:<%=aircraft.getRegistration()%>">Return Lease</option>
<%
				}

				if (priceDry > 0) 
				{ 
%>
					<option value="<%= aircraft.getRegistration() %>,dry">Rent Dry</option>
<% 
				} 
%>
<% 		
				if (priceWet > 0) 
				{ 
%>
					<option value="<%= aircraft.getRegistration() %>,wet">Rent Wet</option>
<% 
				} 
%>
<% 	
				if (priceDry + priceWet == 0 && aircraft.canAlwaysRent(user)) 
				{ 
%>
					<option value="<%= aircraft.getRegistration() %>,wet">Rent</option>
<% 
				} 
%>
		</select>
		</td>
<%	
			} 
			else 
			{ 
%>
	<td>
		<select id="actionSelect" onchange="actions(this);" >
		<option value="0">Select Action</option>
<% 	
				if (priceDry > 0) 
				{ 
%>
					<option value="<%= aircraft.getRegistration() %>,dry">Rent Dry</option>
<% 
				} 
%>
<% 	
				if (priceWet > 0) 
				{ 
%>
					<option value="<%= aircraft.getRegistration() %>,wet">Rent Wet</option>
<% 
				} 
%>
<% 	
				if (priceDry + priceWet == 0 && aircraft.canAlwaysRent(user)) 
				{ 
%>
					<option value="<%= aircraft.getRegistration() %>,wet">Rent</option>
<% 
				} 
%>
	</select>	
	</td>
<%		
			} 
		}
		else
		{
			if(aircraft.getShippingState() != 0)
			{
%>
				<td><%=aircraft.getShippingStateString()%></td>
<%
			}
			if(aircraft.getLessor() != 0 && showActions)
			{
%>
				<td>
				<select name="actionSelect" class = "formselect" onchange = "actions(this)" >
					<option value="0">Select Action</option>
					<option value="LeaseReturn:<%=aircraft.getRegistration()%>">Return Lease</option>
				</select>
				</td>	
<%		
			}
		}
%>
	</tr>
<%
	}
%>
	</tbody>
	</table>
	</form>
</div>
</div>
</body>
</html>
