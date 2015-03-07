<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
    	import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*, java.util.*"
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

	//setup return page if action used
	String returnPage = request.getRequestURI();
	response.addHeader("referer", request.getRequestURI());

	Map<String, Integer> info = null;
	double pilothours = 0; 
	boolean grounded = pilothours > 30;
	int assignmentsTotalPay=0;
	int assignmentsHoldPay=0;
	int assignmentsTotalAll;
	
	String event = request.getParameter("event");
	
	if (event != null && "holdrental".contentEquals(event))
	{
		int id = Integer.parseInt(request.getParameter("id"));
		boolean hold = Boolean.parseBoolean(request.getParameter("hold"));
		Aircraft.setHoldRental(id, user.getId(), !hold);
	}
	
	List<AssignmentBean> assignments = Assignments.getAssignmentsForUser(user.getId());
	AircraftBean aircraft = Aircraft.getAircraftForUser(user.getId());
	boolean haveAircraft = aircraft != null;
	
	boolean isAllInPresent = false;
	for (AssignmentBean assignment : assignments)
	{
        if (assignment.getType() == 1)
            isAllInPresent = true;
	}
	
	pilothours = Stats.getInstance().getNumberOfHours(user.getId(), 48);
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
	<script src="http://maps.googleapis.com/maps/api/js?libraries=visualization&sensor=false"></script>

	<script type='text/javascript' src="scripts/jquery.cookie.js"></script>
	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
	<script src="scripts/AutoComplete.js"></script>

	<script>

		function doSubmit(id)
		{
		     document.lflightForm.id.value = id;
		     document.lflightForm.submit();
		}
		function doSubmit1(id)
		{
		     document.hflightForm.id.value = id;
		     document.hflightForm.submit();
		}
		function doSubmit2(id)
		{
		     document.aircraftForm.id.value = id;
		     document.aircraftForm.type.value = "remove";
		     document.aircraftForm.submit();
		}
		function doSubmit3(id)
		{
		     document.aircraftForm.id.value = id;
		     document.aircraftForm.type.value = "refuel";
		     document.aircraftForm.submit();
		}
		function doSubmit4(id)
		{
		     document.groupForm.id.value = id;
		     document.groupForm.submit();
		}
		function doSubmit5(id)
		{
		     document.lflightForm.id.value = id;
		     document.lflightForm.type.value = "hold";
		     document.lflightForm.submit();
		}
		function doSubmit8(id)
		{
		     document.hflightForm.id.value = id;
		     document.hflightForm.type.value = "load";
		     document.hflightForm.submit();
		}
		function doSubmitX(id,hold)
		{
			var form = document.getElementById("aircraftForm");
			
			form.action = "myflight.jsp";
			form.event.value = "holdrental";
			form.id.value = id;
			form.hold.value = hold;

			form.submit();
		}
		function checkAll(field)
		{
			for (i = 0; i < field.length; i++)
		     	field[i].checked = true ;
		
			field.checked = true ;  // needed in case of only one box
		}
		function checkNone(field)
		{
			for (i = 0; i < field.length; i++)
		     	field[i].checked = false ;
		
		   	field.checked = false ;  // needed in case of only one box
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
		
			$('.holdTable').tablesorter();		
		});
		
		$(function() 
		{		
			$.extend($.tablesorter.defaults, 
			{
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.assignmentTable').tablesorter();

			$('.mapassignment').click(function () {
				var depart = this.getAttribute("data-depart");
				var dest = this.getAttribute("data-dest");

				$("#mapData").load("gmapassignmentpv.jsp?dest=" + dest + "&depart=" + depart);

				$("#mapModal").modal('show');
			});

			$('#mapModal').on('shown.bs.modal', function () {
				initMap();
			});

		});
		
	</script>
</head>

<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	if (haveAircraft)
	{
		try
		{
			info = Flights.getMyFlightInfo(aircraft, user.getId());
		}
		catch(DataError e)
		{
			//eat it			
		}
	}
	else if (!grounded)
	{
%>
		<h2>Status: No aircraft</h2>
		You need to rent an aircraft before you can start your flight.<br />
<%
	}

	UserBean theGroup = null;
	Integer grp;
	
	grp = info != null ? info.get("group") : null;
	int grpId = grp == null ? -1 : grp;
	boolean groupFlight = grpId != -1;

    if (groupFlight)
    {
        theGroup = Accounts.getGroupById(grpId);
    }

	if (haveAircraft && aircraft.getLocation() == null)
	{
%>
		<h2>Status: Flight in progress</h2>
<%
        if(groupFlight)
        {
%>
        <h2 style="color: green;">This flight is being flown for <span style="color: green; font-size: larger;">"<%=theGroup.getName() %>".</span></h2>
<%
        }
        else
        {
%>
        <h2 style="color: navy;">This flight is being flown for <span style="color: navy; font-size: larger;">"<%=user.getName() %>".</span></h2>
<%
        }
%>
        <br>
		There is a flight in progress. If you cannot complete this flight you can cancel it.
		<form method="post" action="userctl">
			<input type="hidden" name="event" value="Cancel"/>
			<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
			<input type="submit" class="button" value="Cancel" />
		</form>
<%
	}
	else if (haveAircraft && !grounded)
	{
	    List<UserBean> groups = Accounts.getGroupsForUser(user.getId());
%>
		<h2>Status: Ready for departure from
            <a href="<%= response.encodeURL("airport.jsp?icao=" + aircraft.getLocation()) %>">
                <%= aircraft.getLocation() %>
            </a>
        </h2>
    <br>
<%
		if (groups.size() > 0)
		{
	      	Integer group = info.get("group");
	      	UserBean thisGroup = null;
	      	int groupId = group == null ? -1 : group;
	      	if (groupId != -1)
	      	{
                for (UserBean group1 : groups)
                {
                    if (group1.getId() == groupId)
                        thisGroup = group1;
                }
	      	}

			if (thisGroup == null)
	    	{
%>
                <h2 style="color: navy;">This flight will be flown for <span style="font-size: larger;">"<%=user.getName() %>".</span></h2>
<%
				if (!info.containsKey("hasAssignment"))
     			{
%>
             		You cannot fly for a group unless you have an assignment aboard.
<%
				}
     			else
     			{	
%>		        	<form method="post" action="userctl" name="groupForm">
			        <input type="hidden" name="event" value="flyForGroup"/>
		    	    <input type="hidden" name="id"/>
					<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
<%		            if (!isAllInPresent) 
    	    		{ 
%>      	         	If you want to fly this flight for a group, select its name:
            	    	<ul>
<%
                        for (UserBean group1 : groups)
                        {
%>
                            <li><a class="link" href="javascript:doSubmit4(<%= group1.getId()%>)">Fly
                                for <%=group1.getName() %>
                            </a></li>
<%
                        }
%>
                        </ul>
<%
                    }
        	    	else 
            		{ 
%>
                        All-In job in queue, cannot be flown for a group.
<%
                    }
%>
                </form>
<%
                }
			}
    		else
    		{
%>
                <h2 style="color: green;">This flight will be flown for <span style="font-size: larger;">"<%=thisGroup.getName() %>".</span></h2>
<%
            }
		}
	} 
	else if (grounded) 
	{ 
%>
        <h2>Status: Flight crew grounded</h2>
        Pilot hours flown in last 48: <b><a class="normal" href="<%= response.encodeURL("hours.jsp") %>"> <%=Formatters.oneDecimal.format(pilothours) %></a></b>
<%
    }

	if (haveAircraft)
	{
		double aircrafthours = aircraft.getHoursSinceLastCheck();
		double enginehours = aircraft.getEngineHours();
	    int tbo = aircraft.getFuelType()==0 ? AircraftBean.TBO_RECIP/3600 : AircraftBean.TBO_JET/3600;

        if (aircraft.getOwner() == 0)
        {
            aircrafthours = 0;
        }

		if (aircraft.isBroken())
	    {
%>
            <div class="dataTable" style="color: red">
				<b>WARNING:</b> This aircraft is prohibited from commercial operations until repairs have been made.<br>
           		Please fly to the nearest Repair Station and have your aircraft serviced.<br>
           		<%=aircraft.getAllowFix()==0 ? "  Renters are not authorized to make repairs." :  "  Renters are authorized to make repairs." %>
         	</div>
<%
        }

    	if (aircrafthours > 90  || enginehours > tbo-20)
    	{
%>
	       	<div class="dataTable" style="color: red" >
                <b>WARNING:</b> This aircraft is prohibited from commercial
                operations if 100-hour inspection time is exceeded or if total engine
                time exceeds TBO.<br><br>
                Hours since last 100-hour inspection: <%=aircrafthours > 90 ? "<b>":""%><%= Formatters.oneDecimal.format(aircrafthours) %><%= aircrafthours > 90 ? "</b>":""%><br>
                Total Engine Time: <%=enginehours > tbo-20 ? "<b>":""%> <%=Formatters.oneDecimal.format(enginehours) %><%= enginehours > tbo-20 ? "</b>":""%><br>
                TBO: <%=tbo%>
	        </div>
<%
        }

    	boolean isNegCost;
    	int rentalCost = aircraft.wasWetRent() ? aircraft.getRentalPriceWet() : aircraft.getRentalPriceDry();

	    // 4/21/11 - Airboss
	    //changed to only show if both cash and balance values would not cover aircraft cost as per Jimmy's request
	    if( groupFlight)
	    {
	      	isNegCost =((theGroup.getMoney()+theGroup.getBank()) < (rentalCost*.5));
	    }
	    else
	    {
	      	double usableBank = user.getBank() + user.getMoney();
            if (usableBank < 0)
            {
                usableBank = 40000 + usableBank;
            }
            else
            {
                usableBank += 40000;
            }

	        isNegCost = (usableBank < (rentalCost*.5));
	    }
	
		if(isNegCost)
		{
%>
            <div class="dataTable" style="color: red" >
       		    <b>WARNING:</b> <%= groupFlight ? "\"" + theGroup.getName() + "\"" : "Your" %> cash balance provides less then 30 minutes of flight time at a rental cost of <%= Formatters.currency.format(rentalCost) %> per hour!!
        	</div>
<%
        }
	}

	if(assignments.size() == 0)
	{ 
%>
        <div class="message">Holding Area - Empty</div>
<% 
	} 
	else 
	{ 
%>		<form method="post" action="userctl" name="hflightForm">
	    <input type="hidden" name="event" value="Assignment"/>
	    <input type="hidden" name="id" />
	    <input type="hidden" name="type" value="remove" />
		<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
	    
	    <table class="holdTable tablesorter-default tablesorter">
	    <caption>Holding Area</caption>
	    <thead>
	    <tr>
	    	<th class="sorter-checkbox" style="width: 35px">Select</th>
			<th class="numeric" style="width: 75px">Pay</th>
			<th style="width: 80px">Location</th>
			<th style="width: 80px">From</th>
			<th style="width: 80px">Dest</th>
			<th class="numeric" style="width: 35px">NM</th>
			<th class="numeric" style="width: 50px">Brg</th>
			<th style="max-width: 400px">Cargo</th>
			<th style="width: 35px">Type</th>
			<th style="width: 75px">Aircraft</th>
           	<th class="sorter-timeExpire" style="width: 75px">Expires</th>
           	<th style="width: 75px">Status</th>
           	<th>Job Comments</th>
	    </tr>
	    </thead>
	    <tbody>
<%
        int counter = 0;
     	for (AssignmentBean bean : assignments)
     	{
     		if (bean.getActive() == 2)
			{
                String image = "img/set2_" + bean.getActualBearingImage() + ".gif";
           		String aircraftReg = Aircraft.getAircraftRegistrationById(bean.getAircraftId());
           		String status;
           		
           		if (bean.getActive() > 0)
           		{
                    if (bean.getActive() == 2)
                    {
                        status = "On Hold";
                    }
                    else
                    {
                        status = "Enroute";
                    }
           		}
           		else if (info != null && info.containsKey((new Integer(bean.getId())).toString()))
           		{
                 	status = "Departing";
           		}
           		else
           		{
                 	status = "Ready";
           		}
           		
           		assignmentsHoldPay += bean.calcPay();
           		
           		CachedAirportBean destination = bean.getDestinationAirport();
           		CachedAirportBean from = bean.getFromAirport();
           		CachedAirportBean location = bean.getLocationAirport();
%>
		     	<tr>
		     	<td>
					<input type="checkbox" id="mycheckbox<%=counter%>" name="select" value="<%=bean.getId() %>"/>
				</td>
		        <td class="numeric"><%=Formatters.currency.format(bean.calcPay()) %></td>
		        <td>
		        	<img class="mapassignment" data-depart="<%= bean.getLocation()%>" data-dest="<%= bean.getTo()%>"
						 src="<%=Airports.getDescriptiveImage(location, Fbos.getAirportFboSlotsInUse(bean.getLocation()) > 0)%>"
						 style="border-style: none; vertical-align:middle;" />
					<a class="normal" title="<%=location.getTitle() %>" href="<%= response.encodeURL("airport.jsp?icao="+ bean.getLocation()) %>">
		        	<%= bean.getLocation() %>
		        	</a>
				</td>
					<td>
						<a class="normal" title="<%=from.getTitle() %>" href="<%= response.encodeURL("airport.jsp?icao=" + bean.getFrom()) %>">
		        			<%= bean.getFrom() %>
						</a>
		        </td>
		        <td>
	        		<img class="mapassignment" data-depart="<%= bean.getLocation()%>" data-dest="<%= bean.getTo()%>"
						 src="<%=Airports.getDescriptiveImage(destination, Fbos.getAirportFboSlotsInUse(bean.getTo()) > 0)%>" style="border-style: none; vertical-align:middle;" />
		        	<a class="normal" title="<%=destination.getTitle() %>" href="<%=response.encodeURL("airport.jsp?icao=" + bean.getTo()) %>">
		        		<%=bean.getTo() %>
		        	</a>
		        </td>
		        <td class="numeric"><%= bean.getActualDistance()%></td>
		        <td class="numeric">
		        	<%= bean.getActualBearing() %>
		        	<img src="<%= image %>">
		        </td>
		        <td><%= bean.getSCargo() %></td>
		        <td><%= bean.getType() == AssignmentBean.TYPE_ALLIN ? "A" : "T" %></td>
		        <td><%= aircraftReg == null ? "[N/A]" : aircraftReg%></td>
		        <td><%= bean.getSExpires() %></td>
		        <td><%= status %></td>
		        <td><%= bean.getComment() %></td>
		     </tr>
<%
            }
            counter++;
		}
%>
    </tbody>
    </table>
    <br>
    <div>
    <a class="link" href="javascript:checkAll(document.hflightForm.select)">Select All</a>	|
    <a class="link" href="javascript:checkNone(document.hflightForm.select)">De-Select</a>
    <input type="button" name="load_Selectedx" value="Load Selected Assignments"  id="load_Selectedx">
    <input type="button" name="cancel_Selectedx" value="Cancel Selected Assignments" onclick="doSubmit1(document.hflightForm.select)">
    <br><br>
    <input type="hidden" name="countLoadedAllIn" id="countLoadedAllIn" value="0" disabled>
    </div>
    </form>
    
<%
		if(assignmentsHoldPay != 0)
		{
%>    <p>Total Value in Holding Area: <b><%=Formatters.currency.format(assignmentsHoldPay) %></b>
    
    
<%	
		}
	} 
%>
<script type="text/javascript">
   $("#load_Selectedx").click(function(e){
       e.preventDefault();
       //check if there are All-in jobs in the loaded table
       $.each($(".assignmentTable").find("td"), function(){
           if($(this).hasClass('assignmentType')){
               if($(this).html() == 'A'){
       $("#countLoadedAllIn").val("1");
               }
           }
       });
    if($("#countLoadedAllIn").val() == "0"){
  		doSubmit8(document.hflightForm.select);
  	}else{
  		alert('You cannot add jobs when an All-In job is loaded.');
  	}
   });
</script>


<%-- chuck229 --%>
	<script type="text/javascript">
		var loc = {};
		var assignment = {};
		var i = 0;
	</script>

<% 
	if (assignments.size() == 0)
	{ 
%>
     	<div class="message">No assignments selected</div>
<% 	} 
	else 
	{ 
%>		<form method="post" action="userctl" name="lflightForm">
    	<input type="hidden" name="event" value="Assignment"/>
    	<input type="hidden" name="id" />
    	<input type="hidden" name="type" value="remove" />
		<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
    	
    	<table class="assignmentTable tablesorter-default tablesorter">
    	<caption>Loading Area</caption>
    	<thead>
    	<tr>
	    	<th class="sorter-checkbox" style="width: 35px;">Select</th>
			<th class="numeric" style="width: 75px;">Pay</th>
			<th style="width: 80px;">Location</th>
			<th style="width: 80px;">From</th>
			<th style="width: 80px;">Dest</th>
			<th class="numeric" style="width: 35px;">NM</th>
			<th class="numeric" style="width: 50px;">Brg</th>
			<th style="max-width: 400px;">Cargo</th>
			<th style="width: 35px;">Type</th>
			<th style="width: 75px;">Aircraft</th>
           	<th class="sorter-timeExpire" style="width: 75px;">Expires</th>
           	<th style="width: 75px;">Status</th>
           	<th style="max-width: 400px;">Job Comments</th>
	   	</tr>
     	</thead>
     	<tbody>
<%
		//All-In processing - see if any of the assignments in queue are of type=1
		for (AssignmentBean assignment : assignments)
    	{
            if (assignment.getType() == 1)
            {
                isAllInPresent = true;
            }
     	}

		//normal processing resumes here
        int counter = 0;
     	for (AssignmentBean assignment : assignments)
     	{
     		if (assignment.getActive() != 2)
     		{
           		//AssignmentBean assignment = assignments[c];
           		String image = "img/set2_" +assignment.getActualBearingImage() + ".gif";
           		String aircraftReg = Aircraft.getAircraftRegistrationById(assignment.getAircraftId());
           		String status;
           		
           		//All-In logic
           		if (!isAllInPresent) 
           		{ //do normal processing as before
           			if (assignment.getActive() > 0 )
           			{
                        if (assignment.getActive() == 2)
                        {
                            status = "On Hold";
                        }
                        else
                        {
                            status = "Enroute";
                        }
           			}
             		else if (info != null && info.containsKey((Integer.toString(assignment.getId()))))
             		{
                   		status = "Departing";
             		}
             		else
             		{
                   		status = "Selected";
             		}
           		}
           		else 
           		{
           			if (isAllInPresent && assignment.getType() == 1 && assignment.getActive() == 0) //this isthe actual All-In job so set it to Ready to Depart
           			{
               			status = "Onboard";
           			}
               		else
               		{
               			if (assignment.getActive() > 0)
               			{
                            if (assignment.getActive() == 2)
                            {
                                status = "On Hold";
                            }
                            else
                            {
                                status = "Enroute";
                            }
               			}
               			else
               			{
               				status = "Selected"; //everything has to be ignoredcause there is an All-In job
               			}
               		}
           		}
           		assignmentsTotalPay += assignment.calcPay();

           		CachedAirportBean destination = assignment.getDestinationAirport();
           		CachedAirportBean from = assignment.getFromAirport();
           		CachedAirportBean location = assignment.getLocationAirport();

				String icao = location.getIcao();
				String destIcao = destination.getIcao();
				CachedAirportBean airportInfo = Airports.cachedAirports.get(icao);
				double lat1 = airportInfo.getLatLon().lat;
				double lon1 = airportInfo.getLatLon().lon;
				airportInfo = Airports.cachedAirports.get(destIcao);
				double destLatl = airportInfo.getLatLon().lat;
				double destLonl = airportInfo.getLatLon().lon;
%>
	<script type="text/javascript">
		if (typeof loc['<%=icao%>'] != 'undefined') {
			var len = loc['<%=icao%>'].length;
		} 
		else {
			loc['<%=icao%>'] = [];
			len = 0;
		}
		loc['<%=icao%>'][len] = [];
		loc['<%=icao%>'][len].latl = <%=lat1%>;
		loc['<%=icao%>'][len].lonl = <%=lon1%>;
		loc['<%=icao%>'][len].pay = "<%=Formatters.currency.format(assignment.calcPay())%>";
		loc['<%=icao%>'][len].cargo = "<%=assignment.getSCargo()%>";
		loc['<%=icao%>'][len].status =
		"<%=status%>".replace(/\s/g,'').toLowerCase();
		loc['<%=icao%>'][len].dist = <%=assignment.getActualDistance()%>;
		loc['<%=icao%>'][len].dest = [];
		loc['<%=icao%>'][len].dest.icao = '<%=destIcao%>';
		loc['<%=icao%>'][len].dest.latl = <%=destLatl%>;
		loc['<%=icao%>'][len].dest.lonl = <%=destLonl%>;
		var mapCenter = {latl: <%=lat1%>, lonl: <%=lon1%>};
	</script>

	     		<tr>
	     			<td>
				        <input type="checkbox" id="mycheckbox<%=counter%>" name="select" value="<%=assignment.getId() %>" <%=assignment.getActive() != 0 ? "disabled" : "" %> />
					</td>
		        	<td class="numeric"><%=Formatters.currency.format(assignment.calcPay()) %></td>
		       	 	<td>
	        			<img  class="mapassignment" data-depart="<%= assignment.getLocation()%>" data-dest="<%= assignment.getTo()%>"
							 src="<%=Airports.getDescriptiveImage(location, Fbos.getAirportFboSlotsInUse(assignment.getLocation()) > 0)%>"
							 style="border-style: none; vertical-align:middle;"/>
						<a title="<%=location.getTitle() %>" href="<%= response.encodeURL("airport.jsp?icao="+ assignment.getLocation()) %>">
		        			<%= assignment.getLocation() %>
		        		</a>
		       		</td>
		        	<td>
						<a title="<%=from.getTitle() %>" href="<%= response.encodeURL("airport.jsp?icao=" +assignment.getFrom()) %>">
		        			<%= assignment.getFrom() %>
		        		</a>
					</td>
		        	<td>
						<img  class="mapassignment" data-depart="<%= assignment.getLocation()%>" data-dest="<%= assignment.getTo()%>"
							 src="<%=Airports.getDescriptiveImage(destination, Fbos.getAirportFboSlotsInUse(assignment.getTo()) > 0)%>"
							 style="border-style: none; vertical-align:middle;" />
		        		<a title="<%=destination.getTitle() %>" href="<%=response.encodeURL("airport.jsp?icao=" + assignment.getTo()) %>">
		        			<%=assignment.getTo() %>
		        		</a>
		        	</td>
		        	<td class="numeric"><%= assignment.getActualDistance()%></td>
		        	<td class="numeric">
		        		<%= assignment.getActualBearing() %>
		        		<img src="<%= image %>">
		        	</td>
		        	<td><%= assignment.getSCargo() %></td>
		        	<td class="assignmentType"><%= assignment.getType() == AssignmentBean.TYPE_ALLIN ? "A" : "T" %></td>
		        	<td><%= aircraftReg == null ? "[N/A]" : aircraftReg%></td>
		        	<td><%= assignment.getSExpires() %></td>
		        	<td><%= status %></td>
		        	<td><%= assignment.getComment() %></td>
				</tr>
<%
     			}
                counter++;
     		}
%>
     </tbody>
     </table>
     <br>
     <a class="link" href="javascript:checkAll(document.lflightForm.select)">Select All</a>|
     <a class="link" href="javascript:checkNone(document.lflightForm.select)">De-Select</a>
     <input type="button" name="hold_Selected" value="Hold Selected Assignments" onClick="doSubmit5(document.lflightForm.select)" />
     <input type="button" name="add_Selected" value="Cancel Selected Assignments" onClick="doSubmit(this.form.select)" />
     <a href="gmapfull.jsp?type=myflight" target="_blank">Map My Assignments</a>
     <br><br>
     </form>
<%
     		if (info != null) 
     		{
           		int weightKg = info.get("weight");
           		int passengerCount = info.get("passengers");

     			int additionalcrew = aircraft.getCrew();
     			int crewseats;
     			double payLoad = aircraft.getMaxWeight() - aircraft.getEmptyWeight() - (77 * (1 + additionalcrew));
     			int payloadnow = (int)Math.round(payLoad - aircraft.getTotalFuel() * Constants.GALLONS_TO_KG);

                if (additionalcrew > 0)
                {
                    crewseats = 2;
                }
                else
                {
                    crewseats = 1;
                }
     			
     			int seats = (aircraft.getSeats() - crewseats);
     			int seatsleft = (seats - passengerCount);
 				int weightleft = (payloadnow - weightKg + (77 * (1 + additionalcrew)));

     			if (weightleft > 0) 
     			{
%>
           			<p>Pilot, crew &amp; payload weight for next flight: 
           			<b><%=weightKg %> Kg</b>/<span style="color: #666666; "><%=(int)Math.round(weightKg/0.45359237) %> Lb</span> (<span
                                style="color: #003300; "><%= weightleft %> Kg remains available at current fuel load</span>)
           			</p>
<%
				}
 				else if (weightleft == 0)
 				{
%>			        <p>Pilot, crew &amp; payload weight for next flight: 
					<b><%=weightKg %> Kg</b>/<span style="color: #666666; "><%=(int)Math.round(weightKg/0.45359237) %> Lb</span> (<span
            			style="color: #003300; ">Payload at weight limit for current fuel load</span>)
					</p>
<%
				}
 				else 
 				{
%>        			<p>Pilot, crew &amp; payload weight for next flight: 
					<b><%=weightKg %> Kg</b>/<span style="color: #666666; "><%=(int)Math.round(weightKg/0.45359237) %> Lb</span> (<span
            			style="color: #660000; ">Flight overweight by <%= (weightKg - payloadnow - (77 *(1 + additionalcrew))) %> Kg for current fuel load</span>)
					</p>
<%
				}

     			if (seatsleft > 1) 
     			{
%>         			<p>Passenger count for next flight: 
					<b><%= passengerCount%></b> (<span style="color: #003300; "><%= seatsleft %>  seats available</span>)
					</p>
<%
				}
 				else if (seatsleft == 1) 
 				{
%>		     		<p>Passenger count for next flight: 
					<b><%= passengerCount %></b>(<span style="color: #003300; ">1 seat available</span>)
					</p>
<%
				}
 				else 	
 				{
%>					<p>Passenger count for next flight: 
					<b><%= passengerCount %></b>(<span style="color: #660000; ">No seats available</span>)
					</p>
<%
				}
     		}
			assignmentsTotalAll = assignmentsTotalPay + assignmentsHoldPay;
			
			if(assignmentsHoldPay != 0)
			{
%>
			<p>Total Value in Loading Area: <b><%=Formatters.currency.format(assignmentsTotalPay) %></b></p>
<%
			}
%>
			<p>Total Value of All Assignments: <b><%=Formatters.currency.format(assignmentsTotalAll) %></b></p>
			Total Number of Assignments: <b><%= assignments.size() %></b>
     		<ul class="footer">
           	<li>Status:
           		<ul>
               		<li>Enroute: The assignment is currently enroute</li>
               		<li>Waiting for departure: The assignment will be flown next flight</li>
               		<li>Selected: The assignment will not be flown next flight</li>
           		</ul>
           	</li>
     		</ul>
<% 		}
%>
<div class="dataTable"> 
<%	
	if (!haveAircraft)
	{ 
%>		<div class="message">No aircraft selected</div>
<%	} 
	else 
	{ 
%>
	   	<form method="post" action="userctl" name="aircraftForm" id="aircraftForm">
	   	<input type="hidden" name="event" value="Aircraft"/>
	   	<input type="hidden" name="type" value="remove"/>
	   	<input type="hidden" name="id" />
	   	<input type="hidden" name="hold" />
		<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
	   	<table>
	        <caption>
	       	Aircraft - <input type="button" onclick="doSubmitX('<%=aircraft.getId() %>', <%= aircraft.getHoldRental()%>)" name="holdRental" value="<%=aircraft.getHoldRental() ? "Hold" : "Release"%>" /> when no assignments on board.
	        </caption>           
	     	<thead>
	     	<tr>
				<th>Registration</th><th>Type</th><th>Equipment</th><th>Location</th><th>Fuel</th><th>Rental Price</th><th>Action</th>
	     	</tr>
	     	</thead>
	     	<tbody>
<%
		boolean departed = aircraft.isDeparted();
		boolean wet = aircraft.wasWetRent();
		int price = wet ? aircraft.getRentalPriceWet() :
		aircraft.getRentalPriceDry();
		if (!departed) 
		{
			String icao = aircraft.getSLocation();
			CachedAirportBean airportInfo = Airports.cachedAirports.get(icao);
			double lat1 = airportInfo.getLatLon().lat;
			double lon1 = airportInfo.getLatLon().lon;
%>
		<script type="text/javascript">
			loc['plane'] = [];
			loc['plane'][0] = [];
			loc['plane'][0].icao = '<%=icao%>';
			loc['plane'][0].latl = <%=lat1%>;
			loc['plane'][0].lonl = <%=lon1%>;
			loc['plane'][0].status = 'plane';
			var mapCenter = {latl: <%=lat1%>, lonl: <%=lon1%>}
		</script>
		
<%			}
%>      	<tr>
           	<td><a class="normal" href="<%=response.encodeURL("aircraftlog.jsp?id=" + aircraft.getId()) %>"><%= aircraft.getRegistration()%></a></td>
           	<td><%= aircraft.getMakeModel() %></td>
           	<td><%= aircraft.getSEquipment() %></td>
           	<td><a class="link" href="<%=response.encodeURL("airport.jsp?icao=" + aircraft.getSLocation())%>"> <%= aircraft.getSLocation() %></a></td>
           	<td><%= Formatters.oneDigit.format(aircraft.getTotalFuel()) %> Gal(<%= (int)Math.round(100.0 *aircraft.getTotalFuel()/(double)aircraft.getTotalCapacity())%>%)</td>
           	<td><%= "$" + price + " " + (aircraft.getAccounting() == AircraftBean.ACC_TACHO ? "Tacho" : "Hour") + (wet ? "/Wet" : "/Dry")	%></td>
			<td>
<% 
		if (!departed)
	   	{
%>
<% 			if (!isAllInPresent)
       		{ 
%>     			<a class="link" href="javascript:doSubmit2('<%=aircraft.getId() %>')">Cancel</a>
<%			}
%>     			<a class="link" href="<%=response.encodeURL("refuel.jsp?id=" + aircraft.getId()) %>">Refuel</a>
<% 		
			if (aircraft.isBroken() && aircraft.isAllowRepair())
    		{
%>         		<a class="link" href="<%= response.encodeURL("maintenance.jsp?id=" + aircraft.getId()) %>">Fix</a>
<%			}
		}
%>			</td>
			</tr>
     </tbody>
     </table>
     </form>
</div>
<div class="dataTable">
<%
		int additionalcrew = aircraft.getCrew();
		double fuelCap = aircraft.getTotalCapacity();
		double payLoad = aircraft.getMaxWeight() - aircraft.getEmptyWeight() - (77 * (1 + additionalcrew));
		int fuelCapInt = (int)fuelCap;
		int payload75 = (int)Math.round(payLoad - fuelCap * 0.75 * Constants.GALLONS_TO_KG);
		int payload100 = (int)Math.round(payLoad - fuelCap * Constants.GALLONS_TO_KG);
		int payloadnow = (int)Math.round(payLoad - aircraft.getTotalFuel() * Constants.GALLONS_TO_KG);
		int crewseats;
    	if (additionalcrew > 0)
    	{
        	crewseats = 2;
    	}
    	else
    	{
        	crewseats = 1;
    	}
	int seats = aircraft.getSeats() - crewseats;
%>

     <table>
	     <caption>Fuel Calculations for <%=aircraft.getRegistration()%></caption>
	     <tr>
	     	<td>Max Passenger/Cargo Payload with Current Fuel Load: <b><%=payloadnow %> Kg</b> or <b><%= Math.min(seats, payloadnow/77) %> pax</b></td>
	     </tr>
	     <tr>
	     	<td>Max Allowable Gallons for <%=aircraft.getRegistration()%>: <b><%= fuelCapInt %></b></td>
	     </tr>
	     <tr>
	     	<td>Max Allowable Passengers for <%=aircraft.getRegistration()%>: <b><%= seats %></b></td>
	     </tr>
	     <tr><td>&nbsp;</td></tr>
	     <tr>
	     	<td><span style="color: #FF0033; "><strong>CAUTION:</strong>  On larger aircraft, Max Fuel can be off by as much as 5 gallons due to floatingpoint issues.</span></td>
	     </tr>
     </table>
     <form>
	     <table>
		     <tr>
			     <td>Enter Payload (Kg):</td>
			     <td><input id="mypayload" class="textarea" value="0" size="5" maxlength="5" /></td>
			     <td># Pax:</td>
			     <td>
			     	<select id="mypax">
<%
    	for (int c=0; c < seats +1; c++)
    	{
%>
						<option value="<%= c %>"><%= c %></option>
<%
		}
%>	
     				</select>
     			</td>
     			<td>
     				<input id="calcFuel" type="button" class="button" value="Calculate" />
     			</td>
     			<td>Max Fuel:</td>
     			<td> <input id="mygallons" class="textarea" size="15" readonly /></td>
     		</tr>
     	</table>
     </form>
     <form>
     	<table>
     		<tr>
     			<td>Enter Fuel (Gal):</td>
     			<td><input id="entgallons" class="textarea" size="5" maxlength="5" ></td>
     			<td></td>
     			<td></td>
     			<td><input id="calcLoad" TYPE="BUTTON" class="button" VALUE="Calculate"></td>
     			<td>Max Payload:</td>
     			<td><input id="expayload" class="textarea" size="16" readonly ></td>
     		</tr>
     	</table>
     </form>

<script>

	$("#calcFuel").click(function () {
		var pl = parseInt(Math.min($("#mypayload").val()));
		var pax = parseInt($("#mypax").val());
		var totalpayload = pax * 77 + pl;
		var percent = Math.round(100 - (25 * (totalpayload - <%= payload100%>) / (<%= payload75 %> - <%= payload100  %>)));
		var cap = <%= fuelCap %> * percent / 100;
		var maxgallons = cap;

		if (maxgallons <= 0)
		{
			$("#mygallons").val('Exceeds Limits');
		}
		else
		{
			$("#mygallons").val(Math.min(maxgallons, <%= fuelCap %>) + ' gallons');
		}
	});

	$("#calcLoad").click(function ()
	{
		var gals = $("#entgallons");
		var pload = $("#expayload");
		var percent = Math.min(gals.val() / <%= fuelCap %> * 100, 100);
		var maxpayload = Math.floor((100 - percent) * ((<%= payload75 %> - <%= payload100 %>) / 25) + <%= payload100 %>);
		var maxpax = Math.min(<%=seats%>, Math.floor(maxpayload / 77));

		if (gals.val() > <%= fuelCap %>) {
			pload.val('Exceeds Limits');
		}
		else {
			pload.val(maxpayload + ' Kg or ' + maxpax + ' pax');
		}
	});

</script>

<% 
	} 
%>
</div>
</div>
</div>

<div class="modal fade" id="mapModal">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button class="close" aria-hidden="true" type="button" data-dismiss="modal">×</button>
				<h4 class="modal-title">Assignment Map</h4>
			</div>
			<div class="modal-body">
				<div class="container">
					<div class="row" id="mapData">
					</div>
				</div>
			</div>
			<div class="modal-footer">
				<button class="btn btn-default" type="button" data-dismiss="modal">Close</button>
			</div>
		</div>
	</div>
</div>

</body>
</html>

