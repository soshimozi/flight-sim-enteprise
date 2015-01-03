<%@page language="java"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
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

	String returnPage = request.getParameter("returnpage");

	String error = null;
	
	boolean step1 = false;
	boolean step2 = false;
	boolean step3 = false;

	List<FboBean> departfbos;
	List<FboBean> destfbos = null;
	
	int departSvc = -1;
	int destSvc = -1;	
	
	String totalcost = "";

	int id = Integer.parseInt(request.getParameter("id"));
	String shipto = request.getParameter("shipTo");
	
	AircraftBean aircraft;

	aircraft = Aircraft.getAircraftShippingInfoById(id);
	
	AirportBean departairport = Airports.getAirport(aircraft.getLocation());
	departfbos = Fbos.getFboForRepair( departairport, Fbos.FBO_REPAIR_MARGIN );

	boolean isRented = aircraft.getUserLock() != 0;
	boolean isForSale = aircraft.getSellPrice() != 0;
	boolean hasActiveShopDepart = false;
	boolean hasActiveShopDest;

    for (FboBean departfbo : departfbos)
    {
        if (departfbo.isActive() && (departfbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0)
            hasActiveShopDepart = true;
    }

	if(isRented)
	{
		step1 = true;
		error = "Selected aircraft is rented, unable to ship.";
	}
	else if(isForSale)
	{
		step1 = true;
		error = "Shipped Aircraft can not be on the market. If you want to ship this aircraft make the sell price 0";
	}
	else if(!hasActiveShopDepart)
	{
		step1 = true;
		error = "No active repairshop at departure airport, you need to move your aircraft to an active FBO repair facility.";
	}	
	else if ("step1".equals(request.getParameter("submit")))
	{
        if (shipto != null)
            shipto = shipto.toUpperCase();

		//check that we can find the destination airfield
		AirportBean destairport = Airports.getAirport(shipto);
		if( destairport == null )
		{
			error = "Invalid ICAO entered for destination. Please try again.";
			
			//Start of shipping
			step1 = true;
		}
		else
		{
			//Move to step 2
			step2 = true;
			
			//need to get find the list of active repair stations for departure and destination airfields
			destfbos = Fbos.getFboForRepair( destairport );

			hasActiveShopDest = false;
            for (FboBean destfbo : destfbos)
            {
                if (destfbo.isActive() && (destfbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0)
                    hasActiveShopDest = true;
            }

			if(!hasActiveShopDest)
			{
				step2 = false;
				step1 = true;
				error = "No active repairshop at destination airport, you will need to select a different destination.";
			}
		}
	}
	else if ("step2".equals(request.getParameter("submit")))
	{	
		//need to get find the list of active repair stations for departure and destination airfields
		String depart = request.getParameter("departService");
		String dest = request.getParameter("destService");

		if( depart == null )
			error = "Unable to ship aircraft as there is no active repair services available at " + aircraft.getLocation();

		if( dest == null )
		{
            if (error == null)
                error = "Unable to ship aircraft as there is no active repair services available at " + shipto;
            else
                error = error + "<br/>Unable to ship aircraft as there is no active repair services available at " + shipto;
		}
		
		if( depart == null || depart.contains("-1"))
			error = "You did not select a repair shop at departure!";

		if( dest == null || dest.contains("-1"))
		{
            if (error == null)
                error = " You did not select a repair shop at the destination!";
            else
                error = error + "<br/>You did not select a repair shop at the destination!";
		}
		
		if( error != null )
		{
			//Start of shipping
			step1 = true;			
		}
		else
		{
			//Move to step 3
			step3 = true; //submit form to userctl
		}

		// Check that there were no errors, if not setup for confirming
		if( error == null )
		{
			int departMargin;
			int destMargin;
			
			departSvc = Integer.parseInt(depart);
			destSvc = Integer.parseInt(dest);

			FboBean fromfbo = Fbos.getFbo(departSvc);
			FboBean tofbo = Fbos.getFbo(destSvc);

            departMargin = departSvc == 0 ?	departMargin = 25 : fromfbo.getRepairShopMargin();
			destMargin = destSvc == 0 ?	destMargin = 25 : tofbo.getRepairShopMargin();
			
			double[] shippingcost = aircraft.getShippingCosts(1);

			double departcost = shippingcost[0] * (1.0+(departMargin/100.0));
			double destcost = shippingcost[1] * (1.0+(destMargin/100.0));

			double cost = departcost + destcost;
			totalcost = Formatters.currency.format(cost);
		}
	}
	else
	{
		//Start of shipping
		step1 = true;
		returnPage = request.getHeader("referer");
		
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
<% 
		if (error != null) 
		{ 
%>
		<div class="error"><%= error %></div>
<%	
		} 
		else if(step1)
		{
%>
		<div class="textarea">
		Step 1 - You have selected a <b><%= aircraft.getMakeModel() %></b>, Registration number: <b><%= aircraft.getRegistration() %></b> for shipment.
		<br/>
		<br/>
		There are costs to shipping your aircraft that are not recoverable once you confirm shipping. 
		The total cost to Disassemble/Reassemble your aircraft packed in a single crate will be <b><%= aircraft.getShippingCostTotalString(1)%></b>.
		That cost does not include the repair shop markup at your shipping departure and destination airfields 
		The total cost of shipping which includes the markup will be shown at the final step when you confirm or cancel the shipment.
		<br/>
		<br/>
		The aircraft will be packed in a single crate, that crate will weight <b><%= aircraft.getEmptyWeight() %> kg</b> and require the aircraft moving it to have at the minimum that amount of space, and space for fuel to move it.
		<% //You can break the aircraft down into multiple crates to reduce the size of the aircraft required but it does cost extra for the additional crating material. (up to 10 crates)  
		//It is recommended that you use as few crates as possible to move your aircraft as you will have to wait for all crates to be delivered before reassembly will occur. %>
		<br/>
		<br/>
		There is also a delay while the aircraft is packed for shipping or is being reassembled. For this aircraft it will be <b>~<%=aircraft.getShippingStateHours() %> hour(s)</b> each.
		Finally, once shipping is confirmed, a contract is placed that guarantees reassembly at the destination whether or not the FBO is still operational or not. (Do note however, it must be open when you initiate shipping!)	
		<br/>
		<br/>
		Please Note!!
		<ul class="footer">
			<li><div style="color:red; font-weight:bold">Once you have agreed to ship the aircraft, the aircraft will no longer allow changes, or be available for use until it reaches its destination and is reassembled.</div></li> 
			<% //<li>If the crated aircraft has left the departure airport you will not be able to cancel the shipment.</li> %>
			<li>A <b>Goods Transfer Assignment</b> will be created, that you will be able to assigned Pilot pay (0.00 default) and/or move the assignment to a group, if desired, once the required packing time is completed.</li>
		</ul>
		</div>
<%
			if( hasActiveShopDepart )
			{
%>		
		<form method="post" action="shipaircraft.jsp">
			<div>
				<input type="hidden" name="submit" value="step1"/>
				<input type="hidden" name="id" value="<%= aircraft.getId() %>"/>
			    <input type="hidden" id="returnPage" name="returnpage" value="<%=returnPage%>"/>
				Shipping Step 1
				<table class="form">
					<tr><td>From</td><td><%= aircraft.getLocation() %></td></tr>
					<tr><td>To</td><td><input type="text" class="textarea" name="shipTo" size="4" value=""/></td></tr>
					<tr><td></td><td></td></tr>
					<tr><td><input class="button" type="submit" value="Next"/></td></tr>	
				</table>
			</div>
		</form>	
<%
			}
		}
		if(step2)
		{
%>
		<div class="textarea">
			Step 2 - Shipping <b><%= aircraft.getMakeModel() %></b>, Registration number: <b><%= aircraft.getRegistration() %></b><br/><br/>
			<form method="post" action="shipaircraft.jsp">
				<div>
					<input type="hidden" name="submit" value="step2"/>
					<input type="hidden" name="id" value="<%= aircraft.getId() %>"/>
					<input type="hidden" name="shipTo" value="<%= shipto %>"/>
				    <input type="hidden" id="returnPage" name="returnpage" value="<%=returnPage%>"/>
				</div>
				<table class="form">
					<tr>
						<td>Select shipping services from <%= aircraft.getLocation() %>:</td>
						<td>
							<select name="departService" class="formselect">
								<option class="formselect" value="-1">Please Select an FBO</option>
<%
            for (FboBean departfbo : departfbos)
            {
                if (departfbo.isActive() && (departfbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0)
                {
%>
                                <option class="formselect"
                                        value="<%= departfbo.getId() %>"><%= departfbo.getName() + " " + departfbo.getSServices()  %>
                                </option>
<%
                }
            }
%>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<td>Select shipping services from <%= shipto %>:</td>
						<td>
							<select name="destService" class="formselect">
								<option class="formselect" value="-1">Please Select an FBO</option>
<%
                                for (FboBean destfbo : destfbos)
                                {
                                    if (destfbo.isActive() && (destfbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0)
                                    {
%>
                                <option class="formselect"
                                        value="<%= destfbo.getId() %>"><%= destfbo.getName() + " " + destfbo.getSServices()  %>
                                </option>
                                <%
                                        }
                                    }
%>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<td><input class="button" type="submit" value="Next"/></td>
						<td></td>
						<td></td>
					</tr>
				</table>
			</form>
		</div>
<%
		}
		if(step3)
		{
%>
		<div class="textarea">
			Step 3 - Shipping <b><%= aircraft.getMakeModel() %></b>, Registration number: <b><%= aircraft.getRegistration() %></b>
			<br/>
			<br/>
			The total cost including selected repair shop markups to prepare your aircraft for shipping, and to reassemble it at its destination will be <b><%= totalcost %></b>.<br/><br/>
			Note: The actual cost of transporting/moving the aircraft to the destination is independent of the above cost, and is whatever you arrange with a pilot or group with a large enough aircraft to ship the crate.
			<br/>
			<br/>
			The packed aircraft crate will weight <b><%= aircraft.getEmptyWeight() %> kg</b> and require the aircraft moving it to have at the minimum that load capacity with fuel.
			<br/>
			<br/>
			There will be a delay before the assignment becomes available of approximately <b><%= aircraft.getShippingStateHours() %> hours</b> while the aircraft is being packed.
			When the assignment reachs its destination, unpacking will begin and take approximately  <b><%= aircraft.getShippingStateHours() %> hours</b>, at which point the aircraft will become ready for use.
		
			Please note that during shipping all fluids are removed, and the aircraft will have no fuel aboard upon reassembly.
		</div>
		<div style="color:red; font-weight:bold; margin:10px">To cancel, select the Aircraft, or any other menu item.</div>
		<div style="color:red; font-weight:bold; margin:10px">Once the shipping process is started, it MUST be delivered to the destination to reassemble!</div>
		<form method="post" action="userctl">
			<div>
				<input type="hidden" name="event" value="shipAircraft"/>
				<input type="hidden" name="id" value="<%= aircraft.getId() %>"/>
				<input type="hidden" name="shipTo" value="<%= shipto %>"/>
				<input type="hidden" name="repairFrom" value="<%= departSvc %>"/>
				<input type="hidden" name="repairTo" value="<%= destSvc %>"/>				
			    <input type="hidden" id="returnPage" name="returnpage" value="<%=returnPage%>"/>
			</div>
			<div>
				<input class="button" type="submit" value="Confirm"/>
			</div>	 
		</form>
<%
		}
%>
	</div>
</div>
</body>
</html>
