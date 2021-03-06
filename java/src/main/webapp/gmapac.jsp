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


    String sId = request.getParameter("Id");
    int Id = Integer.parseInt(sId);

    List<AircraftBean> aircraftList = Aircraft.getAircraftOwnedByUser(Id);
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

<form name="fboForm" id="fboForm" method="post" action="userctl">
	<%=aircraftList.size()%> aircraft meet your criteria.  &nbsp;&nbsp;
	<script type="text/javascript">
	<!-- Begin
	document.write('Your map data took <span id="endTime">0.0</span> seconds to load.');
	var loopTime=setInterval("currentTime()",100);
	// End -->
	</script>
</form>
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
        sb.append(aircraft.getMakeModel());
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
			[<%=lat%>, <%=lon%>, 0, '<%=sb.toString()%>'],
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
