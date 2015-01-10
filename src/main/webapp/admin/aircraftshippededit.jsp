<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.data.*, net.fseconomy.util.Formatters, net.fseconomy.beans.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    //check that we have right permissions, toss to index if not
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    // registration is only passed when we are processing an admin command to reset a shipped aircraft
    if (request.getParameter("id") != null)
    {
        int reassemToFrom;
        int id = Integer.parseInt(request.getParameter("id"));
        reassemToFrom = Integer.parseInt(request.getParameter("reassemtofrom"));

        if( reassemToFrom == 0 )
            Aircraft.finalizeAircraftShipment(id, true, true);   // process aircraft back to its original location
        else
            Aircraft.finalizeAircraftShipment(id, false, true); // process aircraft back to the shipped to location
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="../css/Master.css" rel="stylesheet" type="text/css" />
	<link rel="stylesheet" type="text/css" href="../css/tablesorter-style.css"/>

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script type='text/javascript' src='../scripts/common.js'></script>
	<script type='text/javascript' src='../scripts/css.js'></script>
	<script type='text/javascript' src='../scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="../scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='../scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='../scripts/parser-timeHrMin.js'></script>

    <script type="text/javascript">

        function doSubmit(id)
        {
            document.aircraftForm.id.value = id;
            document.aircraftForm.submit();
        }

        function doSubmit2(id, type)
        {
            document.aircraftForm.id.value = id;
            document.aircraftForm.rentalType.value = type;
            document.aircraftForm.submit();
        }

        function doSubmit3(form)
        {
            // Owner - value = aircraft reg , wet/dry
            if (form.selectedIndex > 5)
                doSubmit2(form.options[form.selectedIndex].value.substr(0,form.options[form.selectedIndex].value.indexOf(",")),form.options[form.selectedIndex].value.substr(form.options[form.selectedIndex].value.indexOf(",") + 1));
             else {
                location.href = form.options[form.selectedIndex].value;
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

			$('.shipmentTable').tablesorter();

		});

	</script>

</head>

<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
<div class="dataTable">	
	<form method="post" action="/admin/aircraftshippededit.jsp" name="aircraftForm">
	<input type="hidden" name="submit" value="true"/>
	
	<table class="shipmentTable tablesorter-default tablesorter">
	<caption>
	Aircraft currently being shipped 
	</caption>
	<thead>
	<tr>
		<th>Registration</th>
		<th>Type</th>
		<th>Location</th>
		<th>Owner</th>
		<th>Shipping State</th>
		<th>Shipping State - Next Update</th>
		<th>Shipped By</th>
		<th>Shipped To</th>
		<th class="sorter-false">Action</th>
	</tr>
	</thead>
	<tbody>
<%
	// Get all currently shipped aircraft for display
	List<AircraftBean> aircraftList = Aircraft.getShippedAircraft();
	
	for (AircraftBean aircraft : aircraftList)
	{
    	UserBean ownerbean = Accounts.getAccountById(aircraft.getOwner());
    	UserBean shipbybean = Accounts.getAccountById(aircraft.getShippedBy());
%>
	<tr>

	<td><a class="normal" href="<%= response.encodeURL("/aircraftlog.jsp?id=" + aircraft.getId()) %>"><%= aircraft.getRegistration() %></a>
<% 
		if (aircraft.isBroken())
        {
%>              
        	<img src='../img/repair.gif' border = 0 align = 'absmiddle'>
<%
		} 
%>    
    </td>
	<td><%= aircraft.getMakeModel() %></td>
	<td>
<% 		
		if (aircraft.getLocation() != null) 
		{ 
%>
	    	<a class="normal" href="<%= response.encodeURL("/airport.jsp?icao=" + aircraft.getLocation()) %>"><%= aircraft.getSLocation() %></a>
<%		} 
		else 
		{ 
%>
		    <%= aircraft.getSLocation() %>
<%
		}
%>	
	</td>
	<td><%= ownerbean.getName() %></td>
	<td><%= aircraft.getShippingStateString() %></td>
	<td><%= aircraft.getShippingState() == 1 || aircraft.getShippingState() == 3 ? Formatters.dateyyyymmddhhmmzzz.format(aircraft.getShippingStateNext()) : "N/A" %></td>
	<td><%= shipbybean.getName() %></td>
    <td><%= aircraft.getShippingTo() %></td>
	
		<td>
		<select name="Actionbox" class = "formselect" onchange = "doSubmit3(this)" >
		<option value="0">Select Action</option>
		<option value="<%= response.encodeURL("/admin/aircraftshippededit.jsp?id=" + aircraft.getId() + "&reassemtofrom=0") %>">Reassemble at Depart</option>
		<option value="<%= response.encodeURL("/admin/aircraftshippededit.jsp?id=" + aircraft.getId() + "&reassemtofrom=1") %>">Reassemble at Dest</option>
		</select>	
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
</div>
</body>
</html>
