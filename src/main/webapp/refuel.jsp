<%@page language="java"
        import="net.fseconomy.data.*, net.fseconomy.util.*"
%>

<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%
	//setup return page if action used
	String returnPage = request.getHeader("referer");

	String message = null;
	String aircraft = request.getParameter("registration");
	AircraftBean[] aircraftData = data.getAircraftByRegistration(aircraft);
	ModelBean[] modelData = data.getModelById(aircraftData[0].getModelId());

    if (aircraftData[0].getLocation() == null)
        message = "Flight in Progress! Refueling Not Allowed.";

    if (message != null)
    {
%>
<div class="message"><%= message %></div>
<%
        return;
    }

    FboBean[] fbo = data.getFboByLocation(aircraftData[0].getLocation());
    AirportBean airport = data.getAirport(aircraftData[0].getLocation());
    int fueltype = aircraftData[0].getFuelType();

    GoodsBean fuelDrums = data.getGoods(aircraftData[0].getLocation(), user.getId(), GoodsBean.GOODS_FUEL100LL);

    if (fueltype > 0)
        fuelDrums = data.getGoods(aircraftData[0].getLocation(), user.getId(), GoodsBean.GOODS_FUELJETA);

    data.fillAirport(airport);

    boolean defuelAllowed = aircraftData[0].changeAllowed(user);
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
<div id="wrapper">
	<div class="content">
		<div class="dataTable">
<%		
		if(!defuelAllowed && !airport.isAvgas() && fbo.length == 0 && fuelDrums == null && modelData[0].getFuelSystemOnly() == 0)  
		{ 
%>
			<div class="message">No fuel available</div>
<% 		
		} 
		else 
		{ 
%>			
			<table>
<%			
			if (fueltype > 0) 
			{ 
%>
				<caption>Suppliers of JetA fuel</caption>
<% 			
			} 
			else 
			{ 
%>
				<caption>Suppliers of 100LL fuel</caption>
<%			
			} 
%>
				<thead>
					<tr>
						<th>Name</th><th>Amount available</th><th>Price</th>
					</tr>
				</thead>
				<tbody>
<%
			for (int c=0; c < fbo.length; c++)
			{ 
				int fuelQty = 0;
				if (fueltype > 0)
				{
					fuelQty = data.getGoodsQty(fbo[c], GoodsBean.GOODS_FUELJETA);
				} 
				else 
				{
					fuelQty = data.getGoodsQty(fbo[c], GoodsBean.GOODS_FUEL100LL);
				}
%>
				<tr>
					<td><%= fbo[c].getName() %></td>
					<td><%= (int)Math.floor(fuelQty / Data.GALLONS_TO_KG) %> Gallons</td>
					<td><%= fueltype > 0 ? Formatters.currency.format(fbo[c].getFueljeta()) : Formatters.currency.format(fbo[c].getFuel100LL()) %></td>
				</tr>
<%	 		
			}
	
			if (airport.isAvgas() || modelData[0].getFuelSystemOnly() == 1) 
			{ 
%>
				<tr>
					<td>Local Market</td>
					<td>Unlimited</td>
					<td><%= fueltype > 0 ? Formatters.currency.format(airport.getJetAPrice()) : Formatters.currency.format(airport.getFuelPrice()) %></td>
				</tr>
<%	 		
			} 
			if (fuelDrums != null) 
			{
%>
				<tr>
					<td>Local fuel drum</td>
					<td><%= (int)Math.floor(fuelDrums.getAmount()/Data.GALLONS_TO_KG) %> Gallons</td>
					<td>-</td>
				</tr>
<%			
			}
%>
				</tbody>
			</table>
		</div>
		
		<div class="dataTable">
<%
			int fuelquarter = (int)Math.floor(aircraftData[0].getTotalCapacity()* .25); 
			int fuelhalf = (int)Math.floor(aircraftData[0].getTotalCapacity()* .5); 
			int fueltquarter = (int)Math.floor(aircraftData[0].getTotalCapacity()* .75); 
			int fuelfull = (int)Math.floor(aircraftData[0].getTotalCapacity());
%>
			<table>
				<caption>Refuel Aircraft</caption>
				<tbody>
					<tr><td>Registration</td><td><%= aircraft %></td></tr>
					<tr><td>Type</td><td><%= aircraftData[0].getMakeModel() %></td></tr>
					<tr><td>Fuel Type</td><td><%= fueltype < 1 ? "100LL" : "JetA" %></td></tr>
					<tr><td colspan="2">&nbsp;</td></tr>
					<tr>
			  			<td>Fuel Capacity</td>
			  			<td>Gallons</td>
					</tr>
					<tr>
			  			<td>25% </td>
			  			<td><%= fuelquarter %> </td>
					</tr>
					<tr>
						<td>50% </td>
			  			<td><%= fuelhalf %> </td>
			  		</tr>
					<tr>	
						<td>75% </td>
			  			<td><%= fueltquarter %></td>
					</tr>
					<tr>
			  			<td>100%</td>
			  			<td><%= fuelfull  %></td>
					</tr>
					<tr><td> </td><td> </td></tr>
					<tr>				
			  			<td>Current Fuel</td>
			  			<td><%= Formatters.oneDigit.format(aircraftData[0].getTotalFuel())%> Gallons</td>
					</tr>
				</tbody>
			</table>
		
			<div class="content">
				<form method="post" action="userctl" id="refuelForm">
					<div>
						<input type="hidden" name="event" value="refuel" />
						<input type="hidden" name="id" value="<%= aircraft %>" />
						<input type="hidden" name="type" value="<%= fueltype %>" />
						<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
					</div>
					<div>
						Buy from
						<select name="provider" class="formselect">
<%
				ModelBean[] mb = data.getModelById(aircraftData[0].getModelId());
				for (int c=0; c < fbo.length && mb[0].getFuelSystemOnly() == 0; c++) 
				{ 
%>
						<option class="formselect" value="<%= fbo[c].getId() %>"><%= fbo[c].getName() %></option>
<%		 		
				}
				if (airport.isAvgas() || modelData[0].getFuelSystemOnly() == 1)  
				{ 
%>	
						<option class="formselect" value="0">Local market</option>
<% 				
				} 
				if (fuelDrums != null && mb[0].getFuelSystemOnly() == 0)
				{ 
%>		
						<option class="formselect" value="-1">Local fuel drum</option>
<%				}
%>
						<option class="formselect" value="" selected="selected">Choose Provider </option>
<%				
				if (defuelAllowed ||  mb[0].getFuelSystemOnly() == 1) 
				{
%>			
						<option class="formselect" value="-2">De-Fuel</option>
<%				}
%>
						</select>
						<br/><br/>
						Fill to <input name="fuel" type="text" class="textarea" size="5" /> 
						Gallons <br/><br/>
						<input type="submit" class="button" value="Refuel" />
					</div>
				</form>
<% 
			}
%>
			</div>
		</div>
	</div>
	
</div>
</body>
</html>
