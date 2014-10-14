<%@ page language="java"
	import="net.fseconomy.data.*"
%>

<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<%	
	String returnPage =  request.getHeader("referer");

	int facilityId = Integer.parseInt(request.getParameter("facilityId"));		
	FboFacilityBean facility = data.getFboFacility(facilityId);

	String error = null;
	FboFacilityBean[] renters = null;
	FboFacilityBean landlord = null;
	FboBean fbo = null;
	AirportBean airport = null;
	
	if (facility.updateAllowed(user))
	{	
		fbo = data.getFbo(facility.getFboId());
	
		if (facility.getIsDefault())
			renters = data.getFboRenterFacilities(fbo);
		else 
			landlord = data.getFboDefaultFacility(fbo);
		
		int iSessionRent=0;
		
		if (session.getAttribute(facility.getLocation() + "Rent") == null)
		{
			session.setAttribute(facility.getLocation() + "Rent", facility.getRent());
			iSessionRent=facility.getRent();
		}
		
		session.setAttribute(facility.getLocation() + "Rent", request.getParameter("pd_rent"));
			
		airport = data.getAirport(fbo.getLocation());
		data.fillAirport(airport);
	}
	else
	{
		error = "Permission denied.";
	}
%>
<!DOCTYPE html>
<html lang="en">
<head>
	
	<title>FSEconomy terminal</title>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
	
	<link href="theme/Master.css" rel="stylesheet" type="text/css" />	

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

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
	<h2>Edit Facility</h2>
	<form method="post" action="userctl">
	<div>	
		<input type="hidden" name="event" value="editFboFacility"/>
		<input type="hidden" name="facilityId" value="<%= facilityId %>"/>
		<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
	</div>
	<div class="formgroup high">
	<table>
		<tbody>
			<tr>
				<td colspan="3"><b>Passenger Terminal</b></td>
			</tr>
			<tr>
				<td>Location</td><td colspan="2"><%= airport.getIcao() %> - <%= fbo.getName() %></td>
			</tr>
<%
	String sizedesc = null;
	if (facility.getIsDefault())
	{
		int totalSpace = fbo.getFboSize() * airport.getFboSlots();
		int rented = data.getFboFacilityBlocksInUse(fbo.getId());
		sizedesc = totalSpace + " gates (" + rented + " rented)";
	} 
	else 
	{
		sizedesc = facility.getSize() + " gates";
	}
%>
			<tr>
				<td>Size</td><td colspan="2"><%= sizedesc %></td>
			</tr>		
<% 	
	if (facility.getIsDefault()) 
	{
%>		
			<tr>
				<td>Space available</td>
				<td colspan="2"><%= data.calcFboFacilitySpaceAvailable(facility, fbo, airport) %> gates</td>
			</tr>
			<tr>
				<td>Reserve</td>
				<td>
					<select class="formselect" name="pd_reservedSpace">
<%		int passSpace = fbo.getFboSize() * airport.getFboSlots();
		for (int i = 0; i <= passSpace; i++)
		{
%>						<option value="<%= i %>"<%= (facility.getReservedSpace() == i ? " selected " : "") %>><%= i %> gates</option>
<%		}
%>
			    	</select> 
				</td>
				<td>for your own use <i><span class="small">(the rest can be rented out)</span></i></td>
			</tr>
			<tr>
				<td>Monthly Rent</td>
				<td>
					$<input name="pd_rent" type="text" class="textarea" value="<%= facility.getRent() %>" size="5"/>.00
				</td>
				<td>per gate.</td>
			</tr>
			<tr>
				<td>Renewals</td>
				<td colspan="2">
					<input name="pd_allowRenew" type="checkbox" value="true" <%= facility.getAllowRenew() ? "checked" :"" %> /> allowed*
				</td>
			</tr>
			<tr>
				<td colspan="3"><i><span class="small">This is global. If unchecked, no renters will be able to renew.</span></i></td>
			</tr>
<%		
		if (renters.length > 0)
		{
%>			
			<tr>
				<td colspan="3">&nbsp;</td>
			</tr>
			<tr>
				<td colspan="3"><b>Renters</b></td>
			</tr>
			<tr>
				<td>Allow Renew*</td>
				<td>Size</td>
				<td>Carrier, Commodity</td>
			</tr>
<%			
			for (int i = 0; i < renters.length; i++)
			{
%>			
			<tr>
				<td>
					<input type="hidden" name="pr_<%= renters[i].getId() %>_facilityId" value="<%= renters[i].getId() %>"/>
					<input name="pr_<%= renters[i].getId() %>_allowRenew" type="checkbox" value="true" <%= renters[i].getAllowRenew() ? "checked" :"" %> />
				</td>
				<td>
					<%= renters[i].getSize() %> gates
				</td>
				<td>
					<%= renters[i].getName() %>, <%= renters[i].getCommodity() %> 
				</td>
			</tr>
<%			
			}
%>			
			<tr>
				<td colspan="3">
					<i><span class="small">*Both the global allow renewals, and the renter allow renewal must be checked for a renter to be able to renew.</span></i>
				</td>
			</tr>
<%		
		}
	} 
	else 
	{
		int spaceAvailable = data.calcFboFacilitySpaceAvailable(landlord, fbo, airport);
%>		
			<tr>
				<td>Space available</td>
				<td><%= spaceAvailable %> gates.</td>
				<td>
<%		
		if (spaceAvailable > 0)
		{
%>				Rent more space for this facility
					<select class="formselect" name="rentBlocks">
						<option value="0" selected ></option>
<%
			for (int i = 1; i <= spaceAvailable; i++)
			{
%>						<option value="<%= i %>" ><%= i %> gates</option>
<%			
			}
%>
					</select>
					<input name="doRent" type="submit" class="button" value="Rent" />
<%		
		}
%>
				</td>
			</tr>
			<tr>
				<td>Monthly Rent</td>
				<td colspan="2">$<%= landlord.getRent() %>.00 per gate.</td>
			</tr>
			<tr>
				<td>Renewals</td>
				<td colspan="2">The FBO is <%= landlord.getAllowRenew() && facility.getAllowRenew() ? "" :"<b>not</b> " %>allowing renewals</td>
			</tr>
			<tr>
				<td>Auto renew every month</td>
				<td colspan="2"><input name="pd_renew" type="checkbox" value="true" <%= facility.getRenew() ? "checked" :"" %> /></td>
			</tr>
<%	
	}
%>	
		</tbody>
	</table>	
	<br />
	<br />
	
	<table>
		<tbody>
			<tr><td colspan="3"><b>Assignment template</b></td></tr>
			<tr>
				<td>Carrier Name</td>
				<td>&nbsp;</td>
				<td>Commodity</td>
			</tr>
			<tr>
				<td valign="top">
					<input name="pd_name" type="text" class="textarea" value="<%= facility.getName() %>" maxlength="45" size="30"/>
				</td>
				<td valign="top">&nbsp;</td>
				<td valign="top">
					<!-- <input name="x" type="text" class="textarea" value="<%= facility.getCommodity() %>" maxlength="255" size="50" /> -->
					<textarea name="pd_commodity" rows="5" cols="80"  style="width: 350px">
<%
		String commodities = facility.getCommodity();
		if (commodities == null)
			commodities = "";
		
		String[] items = commodities.trim().split(",\\ *");
		String output = "";
		for (int i = 0; i < items.length; i++) 
		{ 
			output += items[i].trim() + (i < items.length - 1 ? ", " : "");
		}
		out.print(output);
%>
					</textarea>
					<br />
					<i>
						<span class="small">For varying names separate with a comma<br />
							ex: Passengers, Campers, Vacationers</span>
					</i> 
				</td>
			</tr>
			<tr>
				<td colspan="3">
				Don't accept parties of more than <input name="pd_maxUnitsPerTrip" type="text" class="textarea" value="<%= facility.getMaxUnitsPerTrip() %>" maxlength="2" size="3" /> passengers
				</td>
			</tr>
			<tr>
				<td colspan="3">
				Unclaimed jobs expire in <input name="pd_daysActive" type="text" class="textarea" value="<%= facility.getDaysActive() %>" maxlength="2" size="3" /> days&nbsp;&nbsp;&nbsp;&nbsp;
				<!-- Claimed jobs expire in <input name="pd_daysClaimedActive" type="text" class="textarea" value="<%= facility.getDaysClaimedActive() %>" maxlength="2" size="3" /> 
				additional days -->
				</td>
			</tr>
			<tr>
				<td colspan="3">
				<input name="pd_publicByDefault" type="checkbox" value="true" <%= facility.getPublicByDefault() ? "checked" :"" %> />
				Assignments are public<br />
				<i><span class="small">When unchecked, assignments will be placed in your assignment queue.</span></i>
				</td>
			</tr>
		</tbody>
	</table>
	<br />
	
	<table>
		<tbody>
			<tr>
				<td colspan="5"><b>Option A - ICAO List</b></td>
			</tr>
			<tr>
				<td colspan="5">ICAO list of destinations</td>
			</tr>
			<tr>
				<td colspan="5"><input name="pd_icaoset" type="text" class="textarea" value="<%= (facility.getIcaoSet() != null ? facility.getIcaoSet() : "") %>" maxlength="255" size="50" /></td>
			</tr>
			<tr>
				<td colspan="5"><i><span class="small">ex: KBOS, KJFK, KBWI</span></i></td>
			</tr>
			<tr>
				<td colspan="5">&nbsp;</td>
			</tr>
			<tr>
				<td colspan="5"><b>Option B - Random Airports*</b></td>
			</tr>
			<tr>
				<td colspan="2" align="center">Distance</td>
				<td>&nbsp;</td>
				<td colspan="2" align="center">Airport Size</td>
			</tr>
			<tr>
				<td width="10%">Min</td>
				<td width="10%">Max</td>
				<td>&nbsp;</td>
				<td width="30%">Min</td>
				<td width="30%">Max</td>
			</tr>
			<tr>
				<td><input name="pd_minDistance" type="text" class="textarea" value="<%= facility.getMinDistance() %>" maxlength="4" size="3"/></td>
				<td><input name="pd_maxDistance" type="text" class="textarea" value="<%= facility.getMaxDistance() %>" maxlength="4" size="3"/></td>
				<td>&nbsp;</td>
				<td>
<%
	int matchId;
	int matchSize = facility.getMatchMinSize();
	if (matchSize < AirportBean.MIN_SIZE_MED)
		matchId = 1;
	else if (matchSize < AirportBean.MIN_SIZE_BIG) 
		matchId = 2;
	else
		matchId = 3;
%>
					<select class="formselect" name="pd_matchMinSize">
						<option value="0"<%= (matchId == 1 ? " selected " : "") %>>Airstrip</option>
						<option value="<%= AirportBean.MIN_SIZE_MED %>"<%= (matchId == 2 ? " selected " : "") %>>Small Airport</option>
						<option value="<%= AirportBean.MIN_SIZE_BIG %>"<%= (matchId == 3 ? " selected " : "") %>>Large Airport</option>
		            </select>
				</td>
				<td>
<%
	matchSize = facility.getMatchMaxSize();
	if (matchSize < AirportBean.MIN_SIZE_MED)
		matchId = 1;
	else if (matchSize < AirportBean.MIN_SIZE_BIG) 
		matchId = 2;
	else
		matchId = 3;
%>
					<select class="formselect" name="pd_matchMaxSize">
						<option value="<%= AirportBean.MIN_SIZE_MED - 1 %>"<%= (matchId == 1 ? " selected " : "") %>>Airstrip</option>
						<option value="<%= AirportBean.MIN_SIZE_BIG - 1 %>"<%= (matchId == 2 ? " selected " : "") %>>Small Airport</option>
						<option value="99999"<%= (matchId == 3 ? " selected " : "") %>>Large Airport</option>
		            </select>
				</td>
			</tr>
			<tr>
				<td colspan="5">
					<input name="pd_allowWater" type="checkbox" value="true" <%= facility.getAllowWater() ? "checked" :"" %> />
					Allow Seabases to be selected
				</td>
			</tr>
			<tr>
				<td colspan="5">&nbsp;</td>
			</tr>
			<tr>
				<td colspan="5"><i><span class="small">*Random airports are only selected when the ICAO list is empty.</span></i></td>
			</tr>
		</tbody>
	</table>

	</div>
	
		<div class="formgroup">
			<input type="submit" class="button" value="Update"/>
		</div>
	</form>
</div>

</body>
</html>
