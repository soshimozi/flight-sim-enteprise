<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	//setup return page if action used
	String returnPage = request.getHeader("referer");

	String message = null;

	int id = Integer.parseInt(request.getParameter("id"));
    
	AircraftBean aircraftData = Aircraft.getAircraftById(id);
	ModelBean modelData = Models.getModelById(aircraftData.getModelId());

    if (aircraftData.getLocation() == null)
        message = "Flight in Progress! Refueling Not Allowed.";

    if (message != null)
    {
%>
    <div class="message"><%= message %></div>
<%
        return;
    }

    List<FboBean> fbos = Fbos.getFboByLocation(aircraftData.getLocation());
    AirportBean airport = Airports.getAirport(aircraftData.getLocation());
    int fueltype = aircraftData.getFuelType();

    GoodsBean fuelDrums = Goods.getGoods(aircraftData.getLocation(), user.getId(), GoodsBean.GOODS_FUEL100LL);

    if (fueltype > 0)
        fuelDrums = Goods.getGoods(aircraftData.getLocation(), user.getId(), GoodsBean.GOODS_FUELJETA);

    Airports.fillAirport(airport);

    boolean defuelAllowed = aircraftData.changeAllowed(user);
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
		if(!defuelAllowed && !airport.isAvgas() && fbos.size() == 0 && fuelDrums == null && modelData.getFuelSystemOnly() == 0)  
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
			for (FboBean fbo : fbos)
			{ 
				int fuelQty;
				if (fueltype > 0)
				{
					fuelQty = Goods.getGoodsQty(fbo, GoodsBean.GOODS_FUELJETA);
				} 
				else 
				{
					fuelQty = Goods.getGoodsQty(fbo, GoodsBean.GOODS_FUEL100LL);
				}
%>
				<tr>
					<td><%= fbo.getName() %></td>
					<td><%= (int)Math.floor(fuelQty / Constants.GALLONS_TO_KG) %> Gallons</td>
					<td><%= fueltype > 0 ? Formatters.currency.format(fbo.getFueljeta()) : Formatters.currency.format(fbo.getFuel100LL()) %></td>
				</tr>
<%	 		
			}
	
			if (airport.isAvgas() || modelData.getFuelSystemOnly() == 1)
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
					<td><%= (int)Math.floor(fuelDrums.getAmount()/Constants.GALLONS_TO_KG) %> Gallons</td>
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
			int fuelquarter = (int)Math.floor(aircraftData.getTotalCapacity()* .25);
			int fuelhalf = (int)Math.floor(aircraftData.getTotalCapacity()* .5);
			int fueltquarter = (int)Math.floor(aircraftData.getTotalCapacity()* .75);
			int fuelfull = (int)Math.floor(aircraftData.getTotalCapacity());
%>
			<table>
				<caption>Refuel Aircraft</caption>
				<tbody>
					<tr><td>Registration</td><td><%= aircraftData.getRegistration() %></td></tr>
					<tr><td>Type</td><td><%= aircraftData.getMakeModel() %></td></tr>
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
			  			<td><%= Formatters.oneDigit.format(aircraftData.getTotalFuel())%> Gallons</td>
					</tr>
				</tbody>
			</table>
		
			<div class="content">
				<form method="post" action="userctl" id="refuelForm">
					<div>
						<input type="hidden" name="event" value="refuel" />
						<input type="hidden" name="id" value="<%= aircraftData.getId() %>" />
						<input type="hidden" name="type" value="<%= fueltype %>" />
						<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
					</div>
					<div>
						Buy from
						<select name="provider" class="formselect">
<%
				ModelBean mb = Models.getModelById(aircraftData.getModelId());
				for (int c=0; c < fbos.size() && mb.getFuelSystemOnly() == 0; c++)
				{ 
%>
						<option class="formselect" value="<%= fbos.get(c).getId() %>"><%= fbos.get(c).getName() %></option>
<%		 		
				}
				if (airport.isAvgas() || modelData.getFuelSystemOnly() == 1)
				{ 
%>	
						<option class="formselect" value="0">Local market</option>
<% 				
				} 
				if (fuelDrums != null && mb.getFuelSystemOnly() == 0)
				{ 
%>		
						<option class="formselect" value="-1">Local fuel drum</option>
<%				}
%>
						<option class="formselect" value="" selected="selected">Choose Provider </option>
<%				
				if (defuelAllowed ||  mb.getFuelSystemOnly() == 1)
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
