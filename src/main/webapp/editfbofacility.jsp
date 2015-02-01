<%@ page language="java"
         contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.data.*"
%>
<%@ page import="net.fseconomy.beans.FboFacilityBean" %>
<%@ page import="net.fseconomy.beans.FboBean" %>
<%@ page import="net.fseconomy.beans.CachedAirportBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	if(!user.isLoggedIn())
	{
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
		return;
	}

	String returnPage =  request.getHeader("referer");

	int facilityId = Integer.parseInt(request.getParameter("facilityId"));		
	FboFacilityBean facility = Fbos.getFboFacility(facilityId);

	String error = null;
	List<FboFacilityBean> renters = null;
	FboFacilityBean landlord = null;
	FboBean fbo = null;
	CachedAirportBean airport = null;
	
	if (facility.updateAllowed(user))
	{	
		fbo = Fbos.getFbo(facility.getFboId());
	
		if (facility.getIsDefault())
			renters = Fbos.getFboRenterFacilities(fbo);
		else 
			landlord = Fbos.getFboDefaultFacility(fbo);
		
		if (session.getAttribute(facility.getLocation() + "Rent") == null)
		{
			session.setAttribute(facility.getLocation() + "Rent", facility.getRent());
		}
		
		session.setAttribute(facility.getLocation() + "Rent", request.getParameter("pd_rent"));
			
		airport = Airports.cachedAirports.get(fbo.getLocation());
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
	
	<link href="css/Master.css" rel="stylesheet" type="text/css" />

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
	String sizedesc;
	if (facility.getIsDefault())
	{
		int totalSpace = fbo.getFboSize() * Airports.getTotalFboSlots(fbo.getLocation());
		int rented = Fbos.getFboFacilityBlocksInUse(fbo.getId());
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
				<td colspan="2"><%= Fbos.calcFboFacilitySpaceAvailable(facility, fbo) %> gates</td>
			</tr>
			<tr>
				<td>Reserve</td>
				<td>
					<select class="formselect" name="pd_reservedSpace">
<%		int passSpace = fbo.getFboSize() * Airports.getTotalFboSlots(fbo.getLocation());
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
					$<input name="pd_rent" type="text" class="textarea" value="<%= facility.getRent() %>" size="5">.00
				</td>
				<td>per gate.</td>
			</tr>
			<tr>
				<td>Renewals</td>
				<td colspan="2">
					<input name="pd_allowRenew" type="checkbox" value="true" <%= facility.getAllowRenew() ? "checked" :"" %>> allowed*
				</td>
			</tr>
			<tr>
				<td colspan="3"><i><span class="small">This is global. If unchecked, no renters will be able to renew.</span></i></td>
			</tr>
<%		
		if (renters != null && renters.size() > 0)
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
			for (FboFacilityBean renter : renters)
			{
%>			
			<tr>
				<td>
					<input type="hidden" name="pr_<%= renter.getId() %>_facilityId" value="<%= renter.getId() %>">
					<input name="pr_<%= renter.getId() %>_allowRenew" type="checkbox" value="true" <%= renter.getAllowRenew() ? "checked" :"" %>>
				</td>
				<td>
					<%= renter.getSize() %> gates
				</td>
				<td>
					<%= renter.getName() %>, <%= renter.getCommodity() %>
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
		int spaceAvailable = Fbos.calcFboFacilitySpaceAvailable(landlord, fbo);
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
				<td colspan="2"><input name="pd_renew" type="checkbox" value="true" <%= facility.getRenew() ? "checked" :"" %>></td>
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
					<input name="pd_name" type="text" class="textarea" value="<%= facility.getName() %>" maxlength="45" size="30">
				</td>
				<td valign="top">&nbsp;</td>
				<td valign="top">
					<!-- <input name="x" type="text" class="textarea" value="<%= facility.getCommodity() %>" maxlength="255" size="50" /> -->
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
%>
<textarea name="pd_commodity" rows="5" cols="80"  style="width: 350px"><%= output %></textarea>
					<br />
					<i>
						<span class="small">For varying names separate with a comma<br />
							ex: Passengers, Campers, Vacationers</span>
					</i> 
				</td>
			</tr>
			<tr>
				<td colspan="3">
				Don't accept parties of more than <input name="pd_maxUnitsPerTrip" type="text" class="textarea" value="<%= facility.getMaxUnitsPerTrip() %>" maxlength="2" size="3"> passengers
				</td>
			</tr>
			<tr>
				<td colspan="3">
				Unclaimed jobs expire in <input name="pd_daysActive" type="text" class="textarea" value="<%= facility.getDaysActive() %>" maxlength="2" size="3"> days&nbsp;&nbsp;&nbsp;&nbsp;
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
				<td colspan="5"><input name="pd_icaoset" type="text" class="textarea" value="<%= (facility.getIcaoSet() != null ? facility.getIcaoSet() : "") %>" maxlength="255" size="50"></td>
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
				<td><input name="pd_minDistance" type="text" class="textarea" value="<%= facility.getMinDistance() %>" maxlength="4" size="3"></td>
				<td><input name="pd_maxDistance" type="text" class="textarea" value="<%= facility.getMaxDistance() %>" maxlength="4" size="3"></td>
				<td>&nbsp;</td>
				<td>
<%
	int matchId;
	int matchSize = facility.getMatchMinSize();
	if (matchSize < CachedAirportBean.MIN_SIZE_MED)
		matchId = 1;
	else if (matchSize < CachedAirportBean.MIN_SIZE_BIG)
		matchId = 2;
	else
		matchId = 3;
%>
					<select class="formselect" name="pd_matchMinSize">
						<option value="0"<%= (matchId == 1 ? " selected " : "") %>>Airstrip</option>
						<option value="<%= CachedAirportBean.MIN_SIZE_MED %>"<%= (matchId == 2 ? " selected " : "") %>>Small Airport</option>
						<option value="<%= CachedAirportBean.MIN_SIZE_BIG %>"<%= (matchId == 3 ? " selected " : "") %>>Large Airport</option>
		            </select>
				</td>
				<td>
<%
	matchSize = facility.getMatchMaxSize();
	if (matchSize < CachedAirportBean.MIN_SIZE_MED)
		matchId = 1;
	else if (matchSize < CachedAirportBean.MIN_SIZE_BIG)
		matchId = 2;
	else
		matchId = 3;
%>
					<select class="formselect" name="pd_matchMaxSize">
						<option value="<%= CachedAirportBean.MIN_SIZE_MED - 1 %>"<%= (matchId == 1 ? " selected " : "") %>>Airstrip</option>
						<option value="<%= CachedAirportBean.MIN_SIZE_BIG - 1 %>"<%= (matchId == 2 ? " selected " : "") %>>Small Airport</option>
						<option value="99999"<%= (matchId == 3 ? " selected " : "") %>>Large Airport</option>
		            </select>
				</td>
			</tr>
			<tr>
				<td colspan="5">
					<input name="pd_allowWater" type="checkbox" value="true" <%= facility.getAllowWater() ? "checked" :"" %>>
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
