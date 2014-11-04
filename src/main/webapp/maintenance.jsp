<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

	String registration = request.getParameter("registration");

	//setup return page if action used
	String returnPage = "maintenance.jsp?registration=" + registration;

 	AircraftBean aircraft = data.getAircraftByRegistration(registration);
 	List<LogBean> logs = data.getLogForMaintenanceAircraft(aircraft.getRegistration());
  
    int tetminutes = (aircraft.getTotalEngineTime()) / 60;
 	int lcminutes = (aircraft.getTotalEngineTime() - aircraft.getLastCheck()) / 60;
    int afminutes = (aircraft.getAirframe()) / 60;
    int minutes = 0;
    
    String totalEngineTime = (Formatters.twoDigits.format(tetminutes / 60) + ":" + Formatters.twoDigits.format(tetminutes % 60));
 	String lastCheck = (Formatters.twoDigits.format(lcminutes / 60) + ":" + Formatters.twoDigits.format(lcminutes % 60));
    String airFrameTime = (Formatters.twoDigits.format(afminutes / 60) + ":" + Formatters.twoDigits.format(afminutes % 60));
    
	List<FboBean> fbos = null;
	if(aircraft.getLocation() != null && !aircraft.getLocation().equals(""))
		fbos = data.getFboForRepair(data.getAirport(aircraft.getLocation()));
 %>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
    
	<link href="/theme/Master.css" rel="stylesheet" type="text/css" />
	
	<script type="text/javascript">
		function doSubmit(id, price, fbo)
		{
			if (window.confirm("Do you want to perform this maintenance for " + price + "?"))
			{
				var form = document.getElementById("maintenanceForm");
				form.maintenanceType.value = id;
				form.fbo.value = fbo;
				form.submit();
			}
		}
		
		function doSubmit2(id, price, fbo)
		{
			if (window.confirm("Do you want to install this equipment for " + price + "?"))
			{
				var form = document.getElementById("equipmentForm");
				form.equipmentType.value = id;
				form.fbo.value = fbo;
				form.submit();
			}
		}
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<div class="dataTable">
			<table>
				<caption>Aircraft Data</caption>
				<tbody>
					<tr>
					  	<td>Registration</td>
					  	<td><%=aircraft.getRegistration()%></td>
					</tr>
					<tr>
					  	<td>Type</td>
					  	<td><%=aircraft.getMakeModel()%></td>
					</tr>
					<tr>
					  	<td>Location</td>
					  	<td><%=aircraft.getSLocation()%></td>
					</tr>
					<tr>
					  	<td>Home</td>
					  	<td><%=aircraft.getHome()%></td>
					</tr>
					<tr>
					  	<td>Equipment</td>
					  	<td>
					  		<ul class="equipment">
					    	<li>VFR</li>
<%
   	int equipment = aircraft.getEquipment();

   	if ((equipment & ModelBean.EQUIPMENT_IFR_MASK) != 0)
   		out.println("<li>NAV1, NAV2, NDB</li>");
   	
   	if ((equipment & ModelBean.EQUIPMENT_AP_MASK) != 0)
   		out.println("<li>Autopilot</li>");
   	
   	if ((equipment & ModelBean.EQUIPMENT_GPS_MASK) != 0)
   		out.println("<li>GPS</li>");
%>
					  		</ul>
					  	</td>
					</tr>
					<tr>
					  	<td>Total Engine Time</td>
					  	<td><%=totalEngineTime%></td>
					</tr>
					<tr>
					  	<td>Time since last check</td>
					  	<td><%=lastCheck%></td>
					</tr>
					<tr>
					  	<td>TBO</td>
					  	<td><%=aircraft.getFuelType()==0 ? AircraftBean.TBO_RECIP/3600 : AircraftBean.TBO_JET/3600%></td>
					</tr>
					<tr>
					  	<td>Airframe Hours</td>
					  	<td><%=airFrameTime%></td>
					</tr>
				</tbody>
			</table>
		</div>
		<div class="dataTable">
<%	
	if (equipment == ModelBean.EQUIPMENT_MASK_ALL) 
	{
%>
			<h2>Equipment available</h2>
			All equipment installed. 
<%	
	} 
	else 
	{
%>
			<form method="post" action="userctl" id="equipmentForm">
				<div>
					<input type="hidden" name="event" value="equipment" />
					<input type="hidden" name="reg" value="<%=aircraft.getRegistration()%>" />
					<input type="hidden" name="equipmentType" />
					<input type="hidden" name="fbo" />
					<input type="hidden" name="returnpage" value="<%=returnPage%>" />
					
					<table>
					  <caption>Equipment available</caption>
					  <thead>
					    <tr>
					      <th>Equipment</th>
					      <th>Facility</th>
					      <th>Price</th>
					      <th>Action</th>
					    </tr>
					  </thead>
					  <tbody>
<%
		if ((equipment & ModelBean.EQUIPMENT_IFR_MASK) == 0) 
		{
			for (FboBean fbo : fbos) 
			{
				String price = Formatters.currency.format(aircraft.getEquipmentPriceFBO(ModelBean.EQUIPMENT_IFR_MASK, fbo));
%>
					    <tr>
					      	<td>IFR package</td>
					      	<td><%=fbo.getName()%></td>
					      	<td><%=price%></td>
					      	<td>
					      		<input type="button" class="button" onclick="doSubmit2(<%=ModelBean.EQUIPMENT_IFR_MASK%>, '<%=price%>', <%=fbo.getId()%>) "	value="Install" />
					       	</td>
					    </tr>
<%
			}
		}
	
		if ((equipment & ModelBean.EQUIPMENT_AP_MASK) == 0) 
		{
			for (FboBean fbo : fbos) 
			{
				String price = Formatters.currency.format(aircraft.getEquipmentPriceFBO(ModelBean.EQUIPMENT_AP_MASK, fbo));
%>
					    <tr>
					      	<td>Autopilot</td>
					      	<td><%=fbo.getName()%></td>
					      	<td><%=price%></td>
					      	<td>
					      		<input type="button" class="button"	onclick="doSubmit2(<%=ModelBean.EQUIPMENT_AP_MASK%>, '<%=price%>', <%=fbo.getId()%>) " value="Install" />
					        </td>
					    </tr>
<%
		}
	}

		if ((equipment & ModelBean.EQUIPMENT_GPS_MASK) == 0) 
		{
			for (FboBean fbo : fbos) 
			{
				String price = Formatters.currency.format(aircraft.getEquipmentPriceFBO(ModelBean.EQUIPMENT_GPS_MASK, fbo));
%>
					    <tr>
					      	<td>GPS</td>
					      	<td><%=fbo.getName()%></td>
					      	<td><%=price%></td>
					      	<td>
					      		<input type="button" class="button"	onclick="doSubmit2(<%=ModelBean.EQUIPMENT_GPS_MASK%>, '<%=price%>', <%=fbo.getId()%>) "	value="Install" />
					        </td>
					    </tr>
<%
			}
		}
%>
					  </tbody>
					</table>
				</div>
			</form>
<%
	}
%>
		</div>		
		
		<div class="dataTable">
<%
	if (fbos == null)
	{
%>
			<h2>Error</h2>
			Check that the aircraft is not in flight. 
<%	
	}
	else if(fbos.size() > 0) 
	{
%>
			<form method="post" action="userctl" id="maintenanceForm">
			<div>
				<input type="hidden" name="event" value="maintenance" />
				<input type="hidden" name="reg" value="<%=aircraft.getRegistration()%>" />
				<input type="hidden" name="maintenanceType" />
				<input type="hidden" name="fbo" />
				<input type="hidden" name="returnpage" value="<%=returnPage%>" />
			
				<table>
				<caption>Maintenance available</caption>
				<thead>
				    <tr>
				      	<th>Type</th>
				      	<th>Facility</th>
				      	<th>Estimated Quote</th>
				      	<th>Action</th>
				    </tr>
				</thead>
				<tbody>
<%
	    int[] conditionPrice = aircraft.getConditionPrice(aircraft,AircraftMaintenanceBean.MAINT_100HOUR);
	    int addedPrice=0;
	    boolean repair = false;
	    if (aircraft.isBroken())
	      repair = true;
	    for (int i = 0; i < conditionPrice.length; i++) 
	    {
	          addedPrice += conditionPrice[i];
	    }
	
		for (FboBean fbo : fbos) 
	    {
	            int price100hr = aircraft.getMaintenancePrice(AircraftMaintenanceBean.MAINT_100HOUR,  fbo);
	            int totalAddedPrice = (addedPrice + Math.round((addedPrice * (1+ fbo.getRepairShopMargin())/100)));
	            int estPrice = price100hr + totalAddedPrice;
	            String sPrice100hr = Formatters.currency.format(price100hr);                
	            String sAddedPrice = Formatters.currency.format(totalAddedPrice);
				String sEstPrice = Formatters.currency.format(estPrice);   
	
	  		if (!repair)
			{
%>
				  
			    <tr>
				     <td>100 Hour check</td>
				     <td><%=fbo.getName()%></td>
				     <td><%=sEstPrice%></td>
				     <td><input type="button" class="button" onclick="doSubmit(<%=AircraftMaintenanceBean.MAINT_100HOUR%>, '<%="Estimated Cost of" + sEstPrice%>', <%=fbo.getId()%>) " value="Perform" /></td>
			    </tr>
				
<%
		 	}
		}
		
		for (FboBean fbo : fbos) 
		{
		  	String priceER = Formatters.currency.format(aircraft.getMaintenancePrice(AircraftMaintenanceBean.MAINT_REPLACEENGINE,fbo));
		  	if (!repair)
		  	{
%>
				<tr>
					<td>Engine Replacement</td>
				    <td><%=fbo.getName()%></td>
				    <td><%=priceER%></td>
				    <td><input type="button" class="button" onclick="doSubmit(<%=AircraftMaintenanceBean.MAINT_REPLACEENGINE%>, '<%=priceER%>', <%=fbo.getId()%>) " value="Perform" /></td>
				</tr>
<%
			}
  		}

		for (FboBean fbo : fbos)
		{
		  	String priceER = Formatters.currency.format(aircraft.getMaintenancePrice(AircraftMaintenanceBean.MAINT_FIXAIRCRAFT,fbo));
		    if (repair) 
		    {
%>

			    <tr>
			      	<td>Aircraft Repair</td>
			      	<td><%=fbo.getName()%></td>
			      	<td><%="under $5000"%></td>
			      	<td><input type="button" class="button" onclick="doSubmit(<%=AircraftMaintenanceBean.MAINT_FIXAIRCRAFT%>, 'under $5000', <%=fbo.getId()%>) " value="Perform" /></td>
			    </tr>
<%
			}
 		}
%>
				
				
				</tbody>
				</table>
			</div>
			</form>
<%
	} 
	else 
	{
%>
		<h2>Maintenance available</h2>
		No maintenance facilities at current airport. 
<%
	}
%>
		</div>
		
		<div class="dataTable">
<%
	if (logs.size() > 0)
	{
%>
		<table>
		  <caption>Maintenance log</caption>
		  <thead>
		    <tr>
		      <th>Date</th>
		      <th>Action</th>
		      <th>Total engine time</th>
		      <th>Cost</th>
		      <th>Action</th>
		    </tr>
		  </thead>
		  <tbody>
<%
		int totalDistance = 0;
		int totalFlightTime = 0;
		double totalMoney = 0;
		
		for (LogBean log : logs)
		{
			minutes = log.getTotalEngineTime() / 60;
			String engineTime = minutes == 0 ? "" : (Formatters.twoDigits.format(minutes / 60) + ":" + Formatters.twoDigits.format(minutes % 60));
%>
		    	<tr>
		      		<td><%=Formatters.getUserTimeFormat(user).format(log.getTime())%></td>
		      		<td><%=log.getSType()%></td>
		      		<td><%=engineTime%></td>
		      		<td><%=Formatters.currency.format(log.getMaintenanceCost())%></td>
		      		<td><a class="link" href="javascript:void(window.open('<%=response.encodeURL("maintenancelog.jsp?id=" + log.getId())%>','LogViewer','status=no,toolbar=no,height=705,width=640'))">View report</a></td>
		    </tr>
<%
		}
%>
		  </tbody>
		</table>
<%
	}
%>
		</div>
	</div>
</div>
</body>
</html>
