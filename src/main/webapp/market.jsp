<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	//setup return page if action used
	String returnPage = request.getRequestURI();
	response.addHeader("referer", request.getRequestURI());

	
	int modelId = (request.getParameter("model") == null || request.getParameter("model").equals("")) ? -1 : Integer.parseInt(request.getParameter("model"));
	int lowPrice = (request.getParameter("lowPrice") == null || request.getParameter("lowPrice").equals("")) ? -1 : Integer.parseInt(request.getParameter("lowPrice"));
	int highPrice = (request.getParameter("highPrice") == null || request.getParameter("highPrice").equals("")) ? -1: Integer.parseInt(request.getParameter("highPrice"));
	int lowTime = (request.getParameter("lowTime") == null || request.getParameter("lowTime").equals("")) ? -1 : Integer.parseInt(request.getParameter("lowTime"));
	int highTime = (request.getParameter("highTime") == null || request.getParameter("highTime").equals("")) ? -1 : Integer.parseInt(request.getParameter("highTime"));
	int distance = (request.getParameter("distance") == null || request.getParameter("distance").equals("")) ? -1 : Integer.parseInt(request.getParameter("distance"));
	int lowPax = (request.getParameter("lowPax") == null || request.getParameter("lowPax").equals("")) ? -1 : Integer.parseInt(request.getParameter("lowPax"));
	int highPax = (request.getParameter("highPax") == null || request.getParameter("highPax").equals("")) ? -1 : Integer.parseInt(request.getParameter("highPax"));
	int lowLoad = (request.getParameter("lowLoad") == null || request.getParameter("lowLoad").equals("")) ? -1 : Integer.parseInt(request.getParameter("lowLoad"));
	int highLoad = (request.getParameter("highLoad") == null || request.getParameter("highLoad").equals("")) ? -1 : Integer.parseInt(request.getParameter("highLoad"));

	String equipment = request.getParameter("equipment");
    if (equipment == null)
    {
        equipment = "all";
    }
	
	String fromParam = request.getParameter("from");
    if (fromParam != null && fromParam.equals(""))
    {
        fromParam = null;
    }
	
	boolean hasVfr = request.getParameter("hasVfr") != null;
	boolean hasIfr = request.getParameter("hasIfr") != null;
	boolean hasAp = request.getParameter("hasAp") != null;
	boolean hasGps = request.getParameter("hasGps") != null;
	
	boolean isSystemOwned = request.getParameter("isSystemOwned") != null;
	boolean isPlayerOwned = request.getParameter("isPlayerOwned") != null;
	
	boolean isSubmit = request.getParameter("submit") != null;
	boolean isSearch = (modelId != -1) || (lowPrice != -1) || (highPrice != -1) || (lowTime != -1) || (highTime != -1) || (distance != -1) || (lowPax != -1) || (highPax != -1) || (lowLoad != -1) || (highLoad != -1) || hasVfr || hasIfr || hasGps || hasAp || isSystemOwned || isPlayerOwned || !equipment.equals("all");
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="css/Master.css" rel="stylesheet" type="text/css" />
	<script src="scripts/AnchorPosition.js"></script>
	<script src="scripts/PopupWindow.js"></script>
	<script type='text/javascript' src='scripts/common.js'></script>
	<script type='text/javascript' src='scripts/css.js'></script>
	<script type='text/javascript' src='scripts/standardista-table-sorting.js'></script>

	<script type='text/javascript'>

			var gmapfs = new PopupWindow();
			
			function doSubmit(id, price, id2)
			{
				if (window.confirm("Do you want to buy " + id + " for " + price + "?"))
				{
					var form = document.getElementById("aircraftForm");
					form.id.value = id;
					form.account.value = id2;
					form.submit();
				}
			}
			
			function formValidation(thisForm)
			{	
				var errorList = "The following errors were found on the Search aircraft for sale form: \n\n";
				var errorFound = 0;
				var numericExpression = /^[0-9]+$/;
					
				if (!thisForm.lowPrice.value.match(numericExpression) && thisForm.lowPrice.value != "")
				{
					errorList += "Low Price must be a number.\n";
					errorFound = 1;
				}
				
				if (!thisForm.highPrice.value.match(numericExpression) && thisForm.highPrice.value != "")
				{
					errorList += "High Price must be a number.\n";
					errorFound = 1;
				}
				
				if (!thisForm.lowTime.value.match(numericExpression) && thisForm.lowTime.value != "")
				{
					errorList += "Low Airframe Time must be a number.\n";
					errorFound = 1;
				}
				
				if (!thisForm.highTime.value.match(numericExpression) && thisForm.highTime.value != "")
				{
					errorList += "High Airframe Time must be a number.\n";
					errorFound = 1;
				}
				
				if (!thisForm.lowPax.value.match(numericExpression) && thisForm.lowPax.value != "")
				{
					errorList += "Low Passenger Capacity must be a number.\n";
					errorFound = 1;
				}
				
				if (!thisForm.highPax.value.match(numericExpression) && thisForm.highPax.value != "")
				{
					errorList += "High Passenger Capacity must be a number.\n";
					errorFound = 1;
				}
				
				if (!thisForm.lowLoad.value.match(numericExpression) && thisForm.lowLoad.value != "")
				{
					errorList += "Low Useful Load must be a number.\n";
					errorFound = 1;
				}
				
				if (!thisForm.highLoad.value.match(numericExpression) && thisForm.highLoad.value != "")
				{
					errorList += "High Useful Load must be a number.\n";
					errorFound = 1;
				}
				
				if (thisForm.from.value.length > 4)
				{
					errorList += "Check your ICAO entry for in the search parameter Aircraft That Are Within XX NM from....\n";
					errorFound = 1;
				}
				
				if (errorFound == 1)
				{
					window.alert(errorList);
					return false;
				}
				else
				{
					return true;
				}
			}
			
			function disableVfr(thisForm)
			{
				thisForm.hasVfr.checked = false;
			}
			
			function disableIfr(thisForm)
			{
				thisForm.hasIfr.checked = false;	
			}
			
			function clearSearchForm(thisForm)
			{
				thisForm.lowPrice.value='';
				thisForm.highPrice.value='';
				thisForm.lowTime.value='';
				thisForm.highTime.value='';
				thisForm.lowPax.value='';
				thisForm.highPax.value='';
				thisForm.lowLoad.value='';
				thisForm.highLoad.value='';
				thisForm.from.value='';
			
				thisForm.equipment[0].checked=true;
				thisForm.hasVfr.checked=false;
				thisForm.hasIfr.checked=false;
				thisForm.hasAp.checked=false;
				thisForm.hasGps.checked=false;
				thisForm.isSystemOwned.checked=false;
				thisForm.isPlayerOwned.checked=false;
				
				thisForm.model.selectedIndex=0;
				thisForm.distance.selectedIndex=0;
			}
			
			function toggleEquipmentBoxes(thisForm)
			{	
				if (thisForm.equipment[2].checked)
				{
					thisForm.hasVfr.disabled=false;
					thisForm.hasIfr.disabled=false;
					thisForm.hasAp.disabled=false;
					thisForm.hasGps.disabled=false;		
				}
				else
				{
					thisForm.hasVfr.disabled=true;
					thisForm.hasIfr.disabled=true;
					thisForm.hasAp.disabled=true;
					thisForm.hasGps.disabled=true;
				}
			}
	
	</script>
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	String error = null;
	List<AircraftBean> aircraftList = null;
    Groups.groupMemberData[] staffGroups = user.getStaffGroups();
	
	if (isSubmit) 
	{		
		if (isSearch)
		{
			try
			{
                aircraftList = Aircraft.findAircraftForSale(modelId, lowPrice, highPrice, lowTime, highTime, lowPax, highPax, lowLoad, highLoad, distance, fromParam, hasVfr, hasIfr, hasAp, hasGps, isSystemOwned, isPlayerOwned, equipment);
			}
			catch(DataError e)
			{
				error = e.getMessage();	
			}
		}
		else
		{
            aircraftList = Aircraft.getAircraftForSale();
		}			

		if (error != null)
		{ 
%>
			<div class="error"><%= error %></div>
<%
		}
%>		
<div class="dataTable">		
	<form method="post" action="userctl" id="aircraftForm">
		<div>
		<input type="hidden" name="event" value="Market"/>
		<input type="hidden" name="id" />
		<input type="hidden" name="account" value="<%= user.getId() %>" />
		<input type="hidden" name="returnpage" value="<%=returnPage%>" />

		<table id="sortableTable0" class="sortable">
		<caption>Aircraft for sale  
<%
		if (isSearch)
		{
			StringBuffer queryURL = new StringBuffer("gmapmarket.jsp?");
			
			queryURL.append("modelId=");
			queryURL.append(modelId);
			queryURL.append("&lowPrice=");
			queryURL.append(lowPrice);
			queryURL.append("&highPrice=");
			queryURL.append(highPrice);	
			queryURL.append("&lowTime=");
			queryURL.append(lowTime);
			queryURL.append("&highTime=");
			queryURL.append(highTime);
			queryURL.append("&lowPax=");
			queryURL.append(lowPax);	
			queryURL.append("&highPax=");
			queryURL.append(highPax);				
			queryURL.append("&lowLoad=");
			queryURL.append(lowLoad);
			queryURL.append("&highLoad=");
			queryURL.append(highLoad);
			queryURL.append("&distance=");
			queryURL.append(distance);	
			
			if (fromParam != null )
			{
				queryURL.append("&from=");
				queryURL.append(fromParam);
			}
			
			queryURL.append("&hasVfr=");
			queryURL.append(hasVfr);
			queryURL.append("&hasIfr=");
			queryURL.append(hasIfr);
			queryURL.append("&hasAp=");
			queryURL.append(hasAp);
			queryURL.append("&hasGps=");
			queryURL.append(hasGps);
			queryURL.append("&isSystemOwned=");
			queryURL.append(isSystemOwned);
			queryURL.append("&isPlayerOwned=");
			queryURL.append(isPlayerOwned);
			queryURL.append("&equipment=");
			queryURL.append(equipment);
			
%>
			<a href="#" onclick="gmapfs.setSize(620,520);gmapfs.setUrl('<%= response.encodeURL(queryURL.toString()) %>');gmapfs.showPopup('gmapfs');return false;" id="gmapfs">
				<img src="img/wmap.gif" width="50" height="32" style="border-style: none; vertical-align:middle;" />
			</a>
<%			
		}
		else
		{
%>
			<a href="#" onclick="gmapfs.setSize(620,520);gmapfs.setUrl('<%= response.encodeURL("gmapmarket.jsp") %>');gmapfs.showPopup('gmapfs');return false;" id="gmapfs">
				<img src="img/wmap.gif" width="50" height="32" style="border-style: none; vertical-align:middle;" />
			</a>
<%
		}
%>			
		</caption>
				
		<thead>
		<tr>
			<th>Registration</th>
			<th>Type</th>
			<th>Equipment</th>
			<th>Location</th>
			<th>Price</th>
			<th>Airframe</th>
			<th>Action</th>
		</tr>
		</thead>
		<tbody>
<%
		for (AircraftBean aircraft : aircraftList)
		{
			String reg = aircraft.getRegistration();
			String reg2;
			String acLocation = "In Flight";
			String acICAO = acLocation;
			String price = Formatters.currency.format(aircraft.getSellPrice());
			if (aircraft.getLocation() != null)
			{		
				AirportBean location = Airports.getAirport(aircraft.getLocation());
				acLocation=location.getTitle();
				acICAO=aircraft.getLocation();
			}
			
			int owner=aircraft.getOwner();
            if (owner != 0)
            {
                reg2 = reg + "*";
            }
            else
            {
                reg2 = reg;
            }
			
			// Calculate the airframe time		
			int afminutes = aircraft.getAirframe()/60;
			String afTime = Formatters.twoDigits.format(afminutes / 60) + ":" + Formatters.twoDigits.format(afminutes % 60);
			
			// If the last 100hr check was > 75 hours ago, calculate the time until the next one
			int checkminutes = (aircraft.getTotalEngineTime() - aircraft.getLastCheck())/60;
			if (checkminutes > (75 * 60)) 
			{
				int temp = 6000 - checkminutes;
			    afTime += " (" + Formatters.twoDigits.format(temp / 60) + ":" + Formatters.twoDigits.format(temp % 60) + ")";
			}
%>
		<tr>
		<td><a class="normal" href="aircraftlog.jsp?registration=<%= reg %>"><%= reg2 %></a></td>
		<td><%= aircraft.getMakeModel() %></td>
		<td><%= aircraft.getSEquipment() %></td>
		<td><a title="<%=acLocation%>" class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + aircraft.getLocation()) %>"><%= acICAO %></a></td>
		<td><%= price %></td>
		<td><%= afTime %></td>
		<td><a class="link" href="javascript:doSubmit('<%= reg %>', '<%= price %>', <%= user.getId() %>)">Buy</a>
<%
            for (Groups.groupMemberData staffGroup : staffGroups)
            {
%>
            | <a class="link" href="javascript:doSubmit('<%= reg %>', '<%= price %>', <%= staffGroup.groupId %>)">Buy
                for <%= staffGroup.groupName %>
            </a>
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
		</div>
	</form>
		<ul class="footer">
		    <li>A number in brackets in the Airframe column means a 100hr check is required soon, in that much time</li>
		</ul>
<%
	}
%>	

</div>
<div class="form" style="width:  550px">
<form id="aircraftSearchForm" method="post" action="market.jsp" onsubmit="return formValidation(aircraftSearchForm)">
	<h2>Search aircraft for sale</h2>
	<table>
		<tr>
			<td>
				<div class="formgroup">
					By model <select name="model" class="formselect">
						<option class="formselect" value=""></option>						
						<%
							List<ModelBean> models = Models.getAllModels();
							for (ModelBean model : models)
							{
						%>
								<option class="formselect" value="<%= model.getId() %>" <%= model.getId() == modelId ? "selected" : ""%>><%= model.getMakeModel() %></option>
						<%		
							}
						%>
					</select>			
				</div>
				<div class="formgroup">
					By price range
					<input name="lowPrice" type="text" class="textarea" size ="10" value="<%= lowPrice == -1 ? "" : lowPrice %>"/>						
					to
					<input name="highPrice" type="text" class="textarea" size="10" value="<%= highPrice == -1 ? "" : highPrice %>"/>						
				</div>
				<div class="formgroup">
					By airframe time
					<input name="lowTime" type="text" class="textarea" size="5" value="<%= lowTime == -1 ? "" : lowTime %>"/>						
					to
					<input name="highTime" type="text" class="textarea" size="5" value="<%= highTime == -1 ? "" : highTime %>"/>
				</div>
				<div class="formgroup">
					By passenger capacity
					<input name="lowPax" type="text" class="textarea" size="4" value="<%= lowPax == -1 ? "" : lowPax %>"/>
					to
					<input name="highPax" type="text" class="textarea" size="4" value="<%= highPax == -1 ? "" : highPax %>"/>					
				</div>
				<div class="formgroup">
					By useful load
					<input name="lowLoad" type="text" class="textarea" size="6" value="<%= lowLoad == -1 ? "" : lowLoad %>"/>
					to
					<input name="highLoad" type="text" class="textarea" size="6" value="<%= highLoad == -1 ? "" : highLoad %>"/>
					kg
				</div>
				<div class="formgroup">
					Aircraft that are within
					<select name="distance" class="formselect">
						<option class="formselect" value=""></option>
						<option class="formselect" value="10" <%= distance == 10 ? "selected" : "" %>>10</option>
						<option class="formselect" value="20" <%= distance == 20 ? "selected" : "" %>>20</option>
						<option class="formselect" value="50" <%= distance == 50 ? "selected" : "" %>>50</option>
						<option class="formselect" value="100" <%= distance == 100 ? "selected" : "" %>>100</option>
						<option class="formselect" value="250" <%= distance == 250 ? "selected" : "" %>>250</option>
						<option class="formselect" value="500" <%= distance == 500 ? "selected" : "" %>>500</option>
						<option class="formselect" value="1000" <%= distance == 1000 ? "selected" : "" %>>1000</option>
						<option class="formselect" value="2000" <%= distance == 2000 ? "selected" : "" %>>2000</option>
					</select>
					NM from
					<input name="from" type="text" class="textarea" value="<%= fromParam == null ? "" : fromParam %>" size="4" />
				</div>			
			</td>
			<td></td>
			<td align="right">
				<div class="formgroup">		
					<table>
						<tr>
							<td>							
								<table border='1' width='100%'>
									<tr>
										<td>
											<table>											
												<tr>
													<td colspan="2">Equipment<br/>
													<input type="radio" name="equipment" value="all" <%= equipment.equals("all") ? "checked" : ""%> onclick="toggleEquipmentBoxes(aircraftSearchForm)" />All aircraft<br/>
													<input type="radio" name="equipment" value="vfrOnly" <%= equipment.equals("vfrOnly") ? "checked" : "" %> onclick="toggleEquipmentBoxes(aircraftSearchForm)"/>VFR only<br/>								
													<input type="radio" name="equipment" value="equipmentList" <%= equipment.equals("equipmentList") ? "checked" : "" %> onclick="toggleEquipmentBoxes(aircraftSearchForm)"/>Equipped with:
														<hr/>
													</td>
												</tr>																		
												<tr>										
													<td><input type="checkbox" name="hasVfr" value="vfr" <%= hasVfr ? "checked" : ""%><%= equipment.equals("equipmentList") ? "" : "disabled" %> onclick="disableIfr(aircraftSearchForm)"/> VFR</td>
													<td><input type="checkbox" name="hasAp" value="ap" <%= hasAp ? "checked" : ""%><%= equipment.equals("equipmentList") ? "" : "disabled" %> />AP</td>
												</tr>								
												<tr>
													<td><input type="checkbox" name="hasIfr" value="ifr" <%= hasIfr ? "checked" : ""%><%= equipment.equals("equipmentList") ? "" : "disabled" %> onclick="disableVfr(aircraftSearchForm)"/> IFR</td>
													<td><input type="checkbox" name="hasGps" value="gps" <%= hasGps ? "checked" : ""%><%= equipment.equals("equipmentList") ? "" : "disabled" %> />GPS</td>
												</tr>
											</table>
										</td>
									</tr>
								</table>					
								<table border='1' width='100%'>
									<tr>
										<td>
											<table>								
												<tr>
													<td>Owner</td>
												</tr>
												<tr>
													<td><input type="checkbox" name="isSystemOwned" value="systemOwned" <%= isSystemOwned ? "checked" : ""%>/> System</td>														
												</tr>
												<tr>
													<td><input type="checkbox" name="isPlayerOwned" value="playerOwned" <%= isPlayerOwned? "checked" : ""%>/> Player</td>											
												</tr>
											</table>
										</td>
									</tr>
								</table>
							</td>
						</tr>
					</table>						
				</div>
			</td>
		</tr>
	</table>	
	<div class="formgroup">
		<input type="hidden" name="submit" value="true" />
		<input type="submit" class="button" value="Go" />
		<input type="reset" class="button" value="Reset" />
		<input type="button" class="button" value="Clear" onclick="clearSearchForm(aircraftSearchForm)" />		
	</div>
	<ul class="footer">		
		<li>Submit a blank form to find ALL aircraft for sale.</li>
		<li>Please enter all numbers as digits only (no commas, dots, or currency symbols).</li>
		<li>Useful Load = MTOW - Empty Weight</li>
		<li>Empty/unchecked fields will not be used as search parameters (e.g. Price 100000 to blank will search for all aircraft greater than or equal to v$100,000).</li>
		<li>RESET will populate the form with the values present when the page loaded.</li>
	</ul>
</form>
</div>

</div>
</div>
</body>
</html>
