<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
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

	String icao = request.getParameter("icao");

    String SfacilityId = request.getParameter("facilityId");
    int facilityId = -1;
    boolean madeFacilitySelection = SfacilityId != null;

    int blocks = -1;
    int occupantId = -1;
    int suppliedDays = 0;
    boolean madeBlocksSelection = false;

    FboBean fbo = null;
    AirportBean airport = Airports.getAirport(icao);
    Airports.fillAirport(airport);
    List<FboFacilityBean> facilities = Fbos.getFboDefaultFacilitiesForAirport(icao);
    FboFacilityBean facility = null;

    if (madeFacilitySelection)
    {
        facilityId = Integer.parseInt(SfacilityId);
        for (FboFacilityBean bean : facilities)
        {
            if (bean.getId() == facilityId)
            {
                facility = bean;
                break;
            }
        }

        fbo = Fbos.getFbo(facility.getFboId());
        suppliedDays = Goods.getGoodsQty(fbo, GoodsBean.GOODS_SUPPLIES) / fbo.getSuppliesPerDay(fbo.getFboSize());
        madeBlocksSelection = request.getParameter("selectBlocks") != null;

        if (madeBlocksSelection)
        {
            blocks = Integer.parseInt(request.getParameter("blocks"));
            occupantId = Integer.parseInt(request.getParameter("occupantId"));
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
	if ((facilities.size() == 0) || (madeFacilitySelection && (facility == null)))
	{ 
%>
	<div class="message">No facilities available.</div>
<%  } 
	else if (!madeFacilitySelection) 
	{
%>	
	<table>
		<caption><%= airport.getIcao() %> - Passenger Facilities for Rent</caption>
		<thead>
			<tr>
				<th>FBO</th>
				<th>Gates available</th>
				<th>Rent</th>
				<th>Action</th>
			</tr>
		</thead>
		<tbody>
<%
		for (FboFacilityBean bean : facilities)
		{ 
			if (bean.getUnits() == AssignmentBean.UNIT_PASSENGERS)
			{
				fbo = Fbos.getFbo(bean.getFboId());
				int spaceAvailable = Fbos.calcFboFacilitySpaceAvailable(bean, fbo);
				String rentURL = "fbofacilityrent.jsp?icao=" + airport.getIcao() + "&facilityId=" + bean.getId();
				String rentLink = "<a href=\"" + rentURL + "\">Rent</a>";
%>
			<tr>
				<td><%= fbo.getName() %></td>
				<td><%= spaceAvailable %> gates</td>
				<td><%= Formatters.currency.format(bean.getRent()) %></td>
				<td><%= spaceAvailable < 1 ? "" : rentLink %></td>
			</tr>
<%
			}
		}
%>
		</tbody>
	</table>
<%
	} 
	else if (!madeBlocksSelection) 
	{
        Groups.groupMemberData[] staffGroups = user.getStaffGroups();
		int spaceAvailable = Fbos.calcFboFacilitySpaceAvailable(facility, fbo);
%>
	<form method="post" action="fbofacilityrent.jsp" name="rentForm">
	<input type="hidden" name="icao" value="<%= icao %>" />
	<input type="hidden" name="facilityId" value="<%= facilityId %>" />
	
	<table>
	<caption>Renting Passenger Facilities from <%= airport.getIcao() %> - <%= fbo.getName() %></caption>
	<tbody>
		<tr>
			<td>Supplies</td><td><%= suppliedDays > 14 ? suppliedDays + " days" : "<span style=\"color: red;\">" + suppliedDays + " days</span>" %></td>
		</tr>
		<tr>
			<td>Monthly price per gate</td>
			<td><%= Formatters.currency.format(facility.getRent()) %></td>
		</tr>
		<tr>
			<td>Select number of gates</td>
			<td>
				<select class="formselect" name="blocks">
<%
		for (int i = 1; i <= spaceAvailable; i++)
		{
%>
				<option value="<%= i %>"<%= i == 1 ? " selected='selected' " : "" %>><%= i %> gates</option>
<%
		}
%>
				</select>
			</td>
		</tr>
		<tr>
			<td>Select Renter</td>
			<td>
				<select class="formselect" name="occupantId">
				<option value="<%= user.getId() %>" selected="selected" ><%= user.getName() %></option>
<%
        for (Groups.groupMemberData staffGroup : staffGroups)
        {
%>
                    <option value="<%= staffGroup.groupId %>"><%= staffGroup.groupName %>
                    </option>
<%
        }
%>
				</select>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td>
				<input name="selectBlocks" type="submit" class="button" value="Continue" />
			</td>
		</tr>
	</tbody>
	</table>
	</form>
<%
	} 
	else 
	{
		UserBean occupant = Accounts.getAccountById(occupantId);
%>
	<form method="post" action="userctl" name="rentForm">
	<input type="hidden" name="event" value="rentFboFacility" />
	<input type="hidden" name="facilityId" value="<%= facilityId %>" />
	<input type="hidden" name="blocks" value="<%= blocks %>" />
	<input type="hidden" name="occupantId" value="<%= occupantId %>" />
	<input type="hidden" name="return" value="fbofacility.jsp?id=<%= occupantId %>" />
	
	<table>
	<caption>Renting Passenger Facilities from <%= airport.getIcao() %> - <%= fbo.getName() %></caption>
	<tbody>
		<tr>
			<td>Supplies</td><td><%= suppliedDays > 14 ? suppliedDays + " days" : "<span style=\"color: red;\">" + suppliedDays + " days</span>" %></td>
		</tr>
		<tr>
			<td>Monthly price per gate</td>
			<td><%= Formatters.currency.format(facility.getRent()) %></td>
		</tr>
		<tr>
			<td>Selected number of gates</td>
			<td><%= blocks %> gates</td>
		</tr>
		<tr>
			<td>Renter</td>
			<td><%= occupant.getName() %></td>
		</tr>
		<tr>
			<td>Total monthly rent</td>
			<td><b><%= Formatters.currency.format(facility.getRent() * blocks)  %></b></td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td>
				<input name="confirmRent" type="submit" class="button" value="Confirm" />
			</td>
		</tr>
	</tbody>
	</table>
	</form>
<%
	}
%>
	</div>
</div>
</div>
</body>
</html>
