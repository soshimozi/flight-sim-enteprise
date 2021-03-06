<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
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

    int modelId = (request.getParameter("modelId") == null || request.getParameter("modelId").equals("")) ? -1 : Integer.parseInt(request.getParameter("modelId"));
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
        equipment = "all";

    String fromParam = request.getParameter("from");
    if (fromParam != null && fromParam.equals(""))
        fromParam = null;

    boolean hasVfr = (request.getParameter("hasVfr") != null && Boolean.parseBoolean(request.getParameter("hasVfr")));
    boolean hasIfr = (request.getParameter("hasIfr") != null && Boolean.parseBoolean(request.getParameter("hasIfr")));
    boolean hasAp = (request.getParameter("hasAp") != null && Boolean.parseBoolean(request.getParameter("hasAp")));
    boolean hasGps = (request.getParameter("hasGps") != null && Boolean.parseBoolean(request.getParameter("hasGps")));

    boolean isSystemOwned = request.getParameter("isSystemOwned") != null;
    boolean isPlayerOwned = request.getParameter("isPlayerOwned") != null;

    boolean isSearch = (modelId != -1) || (lowPrice != -1) || (highPrice != -1) || (lowTime != -1) || (highTime != -1) || (distance != -1) || (lowPax != -1) || (highPax != -1) || (lowLoad != -1) || (highLoad != -1) || hasVfr || hasIfr || hasGps || hasAp || isSystemOwned || isPlayerOwned || !equipment.equals("all");

    List<AircraftBean> aircraftList = null;
    String message = null;
    if (isSearch)
    {
        try
        {
            aircraftList = Aircraft.findAircraftForSale(modelId, lowPrice, highPrice, lowTime, highTime, lowPax, highPax, lowLoad, highLoad, distance, fromParam, hasVfr, hasIfr, hasAp, hasGps, isSystemOwned, isPlayerOwned, equipment);
        }
        catch(DataError e)
        {
            message = "Error: " + e.getMessage();
        }
    }
    else
    {
        aircraftList = Aircraft.getAircraftForSale();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>Aircraft Map</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <script src="https://maps.google.com/maps/api/js?sensor=false&key=AIzaSyDpmF0JgC7Oq-KJ-dxPM1eOFnNxhTzwQ2o" type="text/javascript"></script>

    <script type="text/javascript">
        var startTime=new Date();
        function currentTime(){
          var a=Math.floor((new Date()-startTime)/100)/10;
          if (a%1==0) a+=".0";
          document.getElementById("endTime").innerHTML=a;
        }
    </script>

    <style type="text/css">
        div.infowindow-content {
            min-width: 200px;
        }
    </style>

</head>
<body text="#000080" bgcolor="#FFFFFF" background="">

<%
if (message != null) 
{
%>	<div class="message"><%= message %></div>
<%
}
%>
<form name="fboForm" id="fboForm" method="post" action="userctl">
	<%=aircraftList.size()%> aircraft meet your criteria.  &nbsp;&nbsp;
	<script type="text/javascript">
	<!-- Begin
	document.write('Your map data took <span id="endTime">0.0</span> seconds to load.');
	var loopTime=setInterval("currentTime()",100);
	// End -->
	</script>
</form>
	<img src="img/iconac.gif"/>&nbsp;Bank Sale&nbsp;&nbsp;&nbsp;&nbsp;
	<img src="img/icondest.gif"/>&nbsp;Private Sale
<div id="map" style="width: 640px; height: 480px;"></div>
  <script type="text/javascript">
    var locations = 
        [
<%
	StringBuilder sb = new StringBuilder();
    for (AircraftBean aircraft : aircraftList)
    {
        if(aircraft.getLocation() == null)
            continue;

        boolean bankOwned = aircraft.getOwner() == 0;
        CachedAirportBean airportInfo = Airports.cachedAirports.get(aircraft.getLocation());
        double lat = airportInfo.getLatLon().lat;
        double lon = airportInfo.getLatLon().lon;

        CachedAirportBean airport = Airports.cachedAirports.get(aircraft.getLocation());
        String airportLink = Converters.escapeJavaScript(Airports.airportLink(airport.getIcao(), response));

        sb.append("<div class=\"infowindow-content\">");
        sb.append(airportLink);
        sb.append("<br>");
        sb.append(Converters.escapeJavaScript(Accounts.getAccountNameById(aircraft.getOwner())));
        sb.append("<br>");
        sb.append(aircraft.getRegistration());
        sb.append("<br>");
        sb.append(Converters.escapeJavaScript(aircraft.getMakeModel()));
        sb.append("<br>");
        sb.append(Formatters.oneDecimal.format(aircraft.getAirframeHours()));
        sb.append(" hrs Airframe");
        sb.append("<br>");
        sb.append(Formatters.oneDecimal.format(aircraft.getEngineHours()));
        sb.append(" hrs Engine Time");
        sb.append("<br>");
        sb.append(Formatters.oneDecimal.format(aircraft.getHoursSinceLastCheck()));
        sb.append(" hrs Since 100hr");
        sb.append("<br>");
        sb.append(Formatters.oneDecimal.format(aircraft.getTotalFuel()));
        sb.append(" of ");
        sb.append(Formatters.oneDecimal.format(aircraft.getTotalCapacity()));
        sb.append(" Gallons");
        sb.append("<br>");
        sb.append(aircraft.getSEquipment());
        sb.append("<br>");
        sb.append(Formatters.currency.format(aircraft.getSellPrice()));
        sb.append(" Sell Price");
        sb.append("<br>");
        sb.append(Formatters.currency.format(aircraft.getMinimumPrice()));
        sb.append(" Buyback Price");
        sb.append("</div>");
%>
			[<%=lat%>, <%=lon%>, <%=bankOwned ? 0 : 1%>, '<%=sb.toString()%>'], 
<%
        sb.setLength(0);
    }
%>
        ];

    var markerImage = [
                   	new google.maps.MarkerImage('img/iconac.png',      
                 	      	new google.maps.Size(12, 20), 	// This marker is 20 pixels wide by 32 pixels tall.
                 	      	new google.maps.Point(0,0), 		// The origin for this image is 0,0.
                 	      	new google.maps.Point(6, 20)), 	// The anchor for this image is the base of the flagpole at 0,32.
                   	new google.maps.MarkerImage('img/icondest.png',      
                   	      	new google.maps.Size(12, 20), 	// This marker is 20 pixels wide by 32 pixels tall.
                   	      	new google.maps.Point(0,0), 		// The origin for this image is 0,0.
                   	      	new google.maps.Point(6, 20)) 	// The anchor for this image is the base of the flagpole at 0,32.
               		];
  	      
    var map = new google.maps.Map(document.getElementById('map'), {
      zoom: 1,
      center: new google.maps.LatLng(0, 0),
      mapTypeId: google.maps.MapTypeId.Terrain
    });

    var infowindow = new google.maps.InfoWindow({
        maxWidth: 350
    });

    var marker, i;
	var markerBounds = new google.maps.LatLngBounds();
	  
	for (i = 0; i < locations.length; i++) {
       	var point = new google.maps.LatLng(locations[i][0], locations[i][1]);

   		marker = new google.maps.Marker({
        	icon: markerImage[locations[i][2]],
        	position: point,
        	map: map
       	});

     	google.maps.event.addListener(marker, 'click', (function(marker, i) {
         	return function() {
           		infowindow.setContent(locations[i][3]);
           		infowindow.open(map, marker);
         	}
       	})(marker, i));             

     	markerBounds.extend(point);
   	}

    map.fitBounds(markerBounds);
    
	clearTimeout(loopTime);
  </script>

</body>
</html>
