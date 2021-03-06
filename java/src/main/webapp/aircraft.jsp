<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="net.fseconomy.beans.*, java.util.List, net.fseconomy.data.*, net.fseconomy.util.Formatters"
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

	String returnPage;
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
			if (!account.isGroup() || user.groupMemberLevel(groupId) < UserBean.GROUP_INVITED)
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

	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css'>
	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css'>
	<link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css"/>
	<link rel="stylesheet" type="text/css" href="css/tablesorter-style.css"/>
	<link rel="stylesheet" type="text/css" href="css/Master.css"/>

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
	<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>


	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
	
	<script src="scripts/PopupWindow.js"></script>
	
	<script type="text/javascript">
		var gmapac = new PopupWindow();
		
		function doRental(id, type)
		{
			var form = document.getElementById("aircraftForm");
			form.id.value = id;
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
				var id = s[0];
				var type = s[1];
				doRental(id, type);
			}
			else if (selItemText.indexOf("Transfer") >= 0)
			{
				selectAircraft(selItemVal);
			}
			else if(selItemText.indexOf("Return Lease") >= 0)
			{
				if(window.confirm("Are you sure you want to return this Lease?"))
				{
					var s = selItemVal.split(":");
					var id = s[1].trim();
		
					var form = document.getElementById("aircraftForm");
					form.event.value="AircraftLeaseReturn";
					form.id.value = id;
					form.submit();
				}
			}
			else 
			{
                location.href = selItemVal;
			}		
		}

		function transferAircraft()
		{
			var form = document.getElementById("formAircraftModal");
			var ebuyer = document.getElementById("groupSelect");

			form.accountid.value = ebuyer.options[ebuyer.selectedIndex].value;
			form.submit();
		}

		function selectAircraft(aircraftId)
		{
			var form = document.getElementById("formAircraftModal");
			form.aircraftid.value = aircraftId;

			$("#aircraftData").load( "aircraftdata.jsp?aircraftid=" + aircraftId );

			$("#myModal").modal('show');
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
			<input type="hidden" name="id" />
			<input type="hidden" name="type" value="add" />
			<input type="hidden" name="rentalType" />
			<input type="hidden" name="returnpage" value="<%=returnPage%>" />
		</div>
	<table class="aircraftTable tablesorter-default tablesorter">
	<caption>
	Aircraft owned by <%= account.getName() %> 
<%
	int totalFees = 0;
	boolean isInDebt = false;
	for (AircraftBean aircraft : aircraftList)
	{
		if(aircraft.getLessor() == 0 || aircraft.getLessor() == account.getId())
		{
			totalFees += aircraft.getMonthlyFee();
			if (aircraft.getFeeOwed() > 0)
				isInDebt = true;
		}
	}
	if(aircraftList.size() > 0)
	{
%>
		<a href="#" onclick="gmapac.setSize(620,520);gmapac.setUrl('<%= response.encodeURL("gmapac.jsp?Id=" + account.getId()) %>');gmapac.showPopup('gmapac');return false;" id="gmapac">
            <img src="img/wmap.gif" style="border-style: none; vertical-align:middle;">
        </a>
<%
	}
%>
		<br>
		<h5 <%= isInDebt ? " style=\"color: red;\"" : "" %>>Total monthly aircraft fees: <%=Formatters.currency.format(totalFees)%></h5>
	</caption>
	<thead>
	<tr>
		<th style="width: 125px">Registration</th>
		<th >Type</th>
		<th style="width: 35px">Location</th>
		<th style="width: 35px">Home</th>
		<th class="numeric" style="width: 45px">Fuel Loaded</th>
		<th class="numeric" style="width: 100px">Price</th>
		<th class="sorter-timeHrMin numeric" style="width: 45px">Engine Total</th>
		<th class="sorter-timeHrMin numeric" style="width: 45px">Since check</th>
		<th class="numeric" style="width: 150px">Rental Price</th>
		<th class="numeric" style="width: 45px">Bonus</th>
		<th class="numeric" style="width: 75px">Monthly Fee</th>
		<th class="sorter-false" style="width: 75px">Action</th>
	</tr>
	</thead>
	<tbody>
<%
	isInDebt = false;

	for (AircraftBean aircraft : aircraftList)
	{
		int priceDry= aircraft.getRentalPriceDry();
		int priceWet= aircraft.getRentalPriceWet();
        int tbo = aircraft.getFuelType()==0 ? AircraftBean.TBO_RECIP/3600 : AircraftBean.TBO_JET/3600;
		String price = "";
		String monthlyFee = "NA";

		if (priceDry > 0)
			price = "$" + priceDry + " Dry";
			
		if (priceWet > 0)
			price = price + ((priceDry > 0) ? "/" : "") + "$" + priceWet + " Wet";


		if(aircraft.getLessor() == 0 || aircraft.getLessor() == account.getId())
			monthlyFee = Formatters.currency.format(aircraft.getMonthlyFee());

		isInDebt = aircraft.getFeeOwed() > 0;
		if(isInDebt && monthlyFee.contentEquals("NA"))
			monthlyFee = "Fee Owed";

		int minutes = (aircraft.getTotalEngineTime() - aircraft.getLastCheck())/60;
		int hours = minutes / 60;
		String lastCheck = (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
		minutes = (aircraft.getTotalEngineTime())/60;
        int ehours = minutes / 60;
		String engineTotal= (Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60));
%>
	<tr>

	<td><a href="<%= response.encodeURL("aircraftlog.jsp?id=" + aircraft.getId()) %>"><span title="Serial #: <%= aircraft.getId() %>"><%= aircraft.getRegistration() + (aircraft.getLessor() > 0 ? " (L)" : "")%></span></a>
<%
	if (aircraft.isBroken())
    {
%>
              <img src='img/repair.gif' style="border-style: none; vertical-align:middle;" />
<%
	} %>
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
	<td class="numeric"><%= Math.round((aircraft.getTotalFuel() / aircraft.getTotalCapacity()) * 100.0) %>%</td>
<%
		if(aircraft.getLessor() == 0)
		{
			String priceField = "";
			if(aircraft.getSellPrice() > 0 && aircraft.isPrivateSale())
				priceField = Formatters.currency.format(aircraft.getSellPrice()) + " to " + Accounts.getAccountNameById(aircraft.getSellToId());
			else if(aircraft.getSellPrice() > 0)
				priceField = Formatters.currency.format(aircraft.getSellPrice());
			else
				priceField = "NFS";
%>			
			<td class="numeric"><%= priceField %></td>
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
	<td class="numeric" <%= isInDebt ? " style=\"color: red;\"" : "" %>><%= monthlyFee %></td>
<%
		if(aircraft.getShippingState() == 0 && aircraft.getLessor() != account.getId())
		{
			if (showActions)  
			{ 
%>
	<td>
	<select onchange="actions(this);">
	<option value="0">Select Action</option>
	<option value="<%= response.encodeURL("editaircraft.jsp?id=" + aircraft.getId()) %>">Edit</option>
	<option value="<%= response.encodeURL("aircraftlog.jsp?id=" + aircraft.getId()) %>">Log</option>
	<option value="<%= response.encodeURL("maintenance.jsp?id=" + aircraft.getId()) %>">Maintenance</option>
<%
				if(aircraft.getLessor() == 0)
				{
%>
					<option value="<%= response.encodeURL("leaseac.jsp?id=" + aircraft.getId()) %>">Lease</option>
<%
					if (aircraft.getCanShip()) 
					{ 
%>
						<option value="<%= response.encodeURL("shipaircraft.jsp?id=" + aircraft.getId()) %>">Ship</option>
<% 		
					}
					int realowner = aircraft.getOwner();
					if(Accounts.isGroup(aircraft.getOwner()))
						realowner = Accounts.accountUltimateOwner(aircraft.getOwner());

					if (realowner == user.getId())
					{
%>
					<option value="<%=aircraft.getId()%>">Transfer</option>
<%
					}
				}
				else
				{
%>	
					<option value="LeaseReturn:<%=aircraft.getId()%>">Return Lease</option>
<%
				}

				if (!isInDebt && priceDry > 0)
				{ 
%>
					<option value="<%= aircraft.getId() %>,dry">Rent Dry</option>
<% 
				} 
%>
<% 		
				if (!isInDebt && priceWet > 0)
				{ 
%>
					<option value="<%= aircraft.getId() %>,wet">Rent Wet</option>
<% 
				} 
%>
<% 	
				if (!isInDebt && priceDry + priceWet == 0 && aircraft.canAlwaysRent(user))
				{ 
%>
					<option value="<%= aircraft.getId() %>,wet">Rent</option>
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
					<option value="<%= aircraft.getId() %>,dry">Rent Dry</option>
<% 
				} 
%>
<% 	
				if (priceWet > 0) 
				{ 
%>
					<option value="<%= aircraft.getId() %>,wet">Rent Wet</option>
<% 
				} 
%>
<% 	
				if (priceDry + priceWet == 0 && aircraft.canAlwaysRent(user)) 
				{ 
%>
					<option value="<%= aircraft.getId() %>,wet">Rent</option>
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
					<option value="LeaseReturn:<%=aircraft.getId()%>">Return Lease</option>
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
	<h5>(L) indicates Leased Aircraft</h5>
	</form>
</div>
</div>

<!-- Modal HTML -->
<div id="myModal" class="modal fade">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				<h4 class="modal-title">Transfer Aircraft</h4>
			</div>
			<div class="modal-body">
				<form id="formAircraftModal" method="post" action="userctl" class="ui-front">
					<input type="hidden" name="event" value="transferAircraft"/>
					<input type="hidden" name="accountid" value=""/>
					<input type="hidden" name="aircraftid" value=""/>
					<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
					<div id="aircraftData">
					</div>
					<div>
						Transfer  to:
						<select id="groupSelect">
							<option value=""></option>
							<option value="<%=user.getId()%>"><%= user.getName()%></option>
							<%
								List<UserBean> groups = Accounts.getGroupsOwnedByUser(user.getId());

								for (UserBean group : groups)
								{
									if (user.groupMemberLevel(group.getId()) >= UserBean.GROUP_STAFF)
									{
							%>
							<option value="<%=group.getId()%>"><%= group.getName()%></option>
							<%
									}
								}
							%>
						</select>

					</div>
				</form>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
				<button type="button" class="btn btn-primary" onclick="transferAircraft();">Transfer Aircraft</button>
			</div>
		</div>
	</div>
</div>

</body>
</html>
