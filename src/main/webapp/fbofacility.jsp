<%@ page language="java"
	import="java.text.*, net.fseconomy.data.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>

<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
	UserBean account = null;
	String sId = request.getParameter("id");

	//setup return page if action used
	String groupParam = sId != null ? "?id="+sId : "";
	String returnPage = request.getRequestURI() + groupParam;
    response.addHeader("referer", request.getRequestURI() + groupParam);

	if (sId != null)
	{
		int id = Integer.parseInt(sId);
		account = data.getAccountById(id);
		if (account != null)
		{
			if (account.isGroup() == false || user.groupMemberLevel(id) < UserBean.GROUP_STAFF)
				account = null;	
		}				
	}
	
	if (account == null)
		account = user;	
	
	FboFacilityBean[] facilities = data.getFboFacilitiesByOccupant(account.getId());
%>
<!DOCTYPE html>
<html lang="en">
<head>
	<title>FSEconomy terminal</title>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>    
	
	<link rel="stylesheet" type="text/css" href="theme/Master.css" />
	<link rel="stylesheet" type="text/css" href="theme/tablesorter-style.css" />
	
	<script src="scripts/jquery.min.js"></script>
	
	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
	
	<script src="scripts/PopupWindow.js"></script>

	<script type="text/javascript"> var gmap = new PopupWindow(); </script>
	
	<script type="text/javascript">
	
		function doSubmit1(id) 
		{
			if (window.confirm("Reduce the size this facility? (no refunds)")) 
			{
				document.facilityForm.facilityId.value = id;
				document.facilityForm.submit();
			}
		}
		
		function doSubmit2(id) 
		{
			if (window.confirm("Close this facility? (no refunds)")) 
			{
				document.facilityForm.facilityId.value = id;
				document.facilityForm.submit();
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
		
			$('.facilitiestable').tablesorter();		
		});
		
	</script>	
	
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<form method="post" action="userctl" name="facilityForm">
		<div>
			<input type="hidden" name="event" value="deleteFboFacility"/>
			<input type="hidden" name="facilityId">
			<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
		</div>
		<table class="facilitiestable tablesorter-default tablesorter">
			<caption>Facilities for <%= account.getName() %></caption>
			<thead>
			<tr>
				<th>Location</th>
				<th>Carrier / Parent FBO</th>
				<th>Commodity</th>
				<th class="numeric" >Pax Jobs</th>
				<th class="numeric" >Size</th>
				<th>Type</th>
				<th>Destinations</th>
				<th>FBO Status</th>
				<th class="numeric" >Supplies</th>
				<th class="sorter-false" >Action</th>
			</tr>
			</thead>
			<tbody>
<%
	NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
	for (int c=0; c < facilities.length; c++)
	{
		FboBean fbo = data.getFbo(facilities[c].getFboId());
		AirportBean ap = data.getAirport(facilities[c].getLocation());
		
		String sizedesc = null;
		if (facilities[c].getIsDefault())
		{
			int totalSpace = fbo.getFboSize() * ap.getFboSlots();
			int rented = data.getFboFacilityBlocksInUse(fbo.getId());
			sizedesc = totalSpace + " gates (" + rented + " rented)";
		} else {
			sizedesc = facilities[c].getSize() + " gates";
		}
		int suppliedDays = data.getGoodsQty(fbo, GoodsBean.GOODS_SUPPLIES) / fbo.getSuppliesPerDay(ap);
		int availJobs = data.getFacilityJobCount(facilities[c].getOccupant(), facilities[c].getLocation()); 
%>
				<tr <%= Data.oddLine(c) %>>
					<td style="width: 80px;"><%= data.airportLink(ap, ap, response) %></td>	
					<td><%= facilities[c].getName() %><br/><span style="font-size:9px; font-weight: bold;"><%= fbo.getName() %></span></td>
					<td>
<%
		String commodities = facilities[c].getCommodity();
		if (commodities == null)
			commodities = "";
		String[] items = commodities.trim().split(",\\ *");
		for (int i = 0; i < items.length; i++)
		{
%>
						<%= i > 0 ? "<br />" : "" %><%= items[i] %><%= i < items.length - 1 ? "," : "" %>
<%
		}
%>
					</td>
					<td style="width: 80px; text-align: center;"><%= availJobs %></td>
					<td><%= sizedesc %></td>
					<td style="width: 60px; text-align: center;"><%= facilities[c].getIsDefault() ? "Owned" : "Rented" %></td>
					<td><%= facilities[c].getParametersDesc() %></td>
					<td style="width: 80px; text-align: center;"><%= fbo.isActive() ? "Active" : "Closed" %></td>
					<td style="width: 80px; text-align: center;"><%= suppliedDays > 14 ? suppliedDays + " days" : "<span style=\"color: red;\">" + suppliedDays + " days</span>" %></td>
					<td>
						<a class="link" href="<%= response.encodeURL("editfbofacility.jsp?facilityId=" + facilities[c].getId()) %>">Edit</a>
<%
		if (!facilities[c].getIsDefault())
		{
			String link;
			if (facilities[c].getSize() > 1)
				link = "| <a class=\"link\" href=\"javascript:doSubmit1(" + facilities[c].getId() + ")\">Shrink</a>";
			else
				link = "| <a class=\"link\" href=\"javascript:doSubmit2(" + facilities[c].getId() + ")\">Move&nbsp;out</a>";
%>
						<%= link %>
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

</body>
</html>
