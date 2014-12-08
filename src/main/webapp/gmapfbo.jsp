<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.* "
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    boolean showFbo = request.getParameter("fboCheck")!= null;
    boolean showFuel = request.getParameter("fuelCheck") != null;
    boolean showRepair = request.getParameter("repairCheck") != null;
    boolean showInactive = request.getParameter("inactiveCheck") != null;
    boolean facilityPT = request.getParameter("facilityPTCheck") != null;
    boolean facilityCargo = request.getParameter("facilityCargoCheck") != null;
    String region = request.getParameter("region");
    String state = request.getParameter("state");
    String country = request.getParameter("country");
    String icao = request.getParameter("icao");
    String name = request.getParameter("name");
    String sFboOwner = request.getParameter("fboOwner");
    int fboOwner = 0;

    if (sFboOwner != null)
        fboOwner = Integer.parseInt(sFboOwner);
    if (region != null && region.equals(""))
        region = null;
    if (state != null && state.equals(""))
        state = null;
    if (country != null && country.equals(""))
        country = null;
    if (icao != null && icao.equals(""))
        icao = null;
    if (name != null && name.equals(""))
        name = null;

    List<AirportBean> airports = null;
    String message = null;

    try
    {
        airports = Airports.findCertainAirports(region, state, country, icao, name, showFuel, showRepair, showFbo, showInactive, facilityPT, facilityCargo, fboOwner);
    }
    catch(DataError e)
    {
        message = "Error: " + e.getMessage();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FBO Maps</title>

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
%>
        <div class="message"><%= message %></div>
<%
    }
%>
<form name="fboForm" id="fboForm" method="post" action="userctl">
	<%=airports.size()%> Airports found

<script type="text/javascript">
    document.write('Your map data took <span id="endTime">0.0</span> seconds to load.');
    var loopTime=setInterval("currentTime()",100);
</script>

	<br />
 	<img src="img/iconac.gif"/>&nbsp;Airport &nbsp;&nbsp;&nbsp;&nbsp;
	<img src="img/icondest.gif"/>&nbsp;Services Available &nbsp;&nbsp;&nbsp;&nbsp;
	<img src="img/icongoods.gif"/>&nbsp;Goods Available&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<input type="hidden" name="return" value="fbomap.jsp" />
	<input name="submit" type="button" onclick="window.location='fbomap.jsp';" class="button" value="New Map" />
	
</form>
<div id="map" style="width: 640px; height: 480px;"></div>

<script type="text/javascript">
    var locations = 
        [
<%

    StringBuilder sb = new StringBuilder();
    for (AirportBean airport : airports)
    {
        double lat = airport.getLat();
        double lon = airport.getLon();

        String airportLink = Converters.escapeJavaScript(Airports.airportLink(airport, response));

        boolean hasServices = airport.hasServices();
        boolean hasGoodsForSale = airport.hasGoodsForSale();

        int iconToUse = hasGoodsForSale ? 2 : hasServices ? 1 : 0;

        sb.append("<div class=\"infowindow-content\">");
        sb.append(airportLink);
        sb.append("</div>");
%>
			[<%=lat%>, <%=lon%>, <%=iconToUse%>, '<%=sb.toString()%>'],
<%
        sb.setLength(0);
    }
%>
        ];

    var image = new google.maps.MarkerImage('img/iconac.png',      
    	      new google.maps.Size(12, 20), 	// This marker is 20 pixels wide by 32 pixels tall.
      	      new google.maps.Point(0,0), 		// The origin for this image is 0,0.
      	      new google.maps.Point(6, 20)); 	// The anchor for this image is the base of the flagpole at 0,32.

    var markerImage = [
    	new google.maps.MarkerImage('img/iconac.png',      
  	      	new google.maps.Size(12, 20), 	// This marker is 20 pixels wide by 32 pixels tall.
  	      	new google.maps.Point(0,0), 		// The origin for this image is 0,0.
  	      	new google.maps.Point(6, 20)), 	// The anchor for this image is the base of the flagpole at 0,32.
    	new google.maps.MarkerImage('img/icondest.png',      
    	      	new google.maps.Size(12, 20), 	// This marker is 20 pixels wide by 32 pixels tall.
    	      	new google.maps.Point(0,0), 		// The origin for this image is 0,0.
    	      	new google.maps.Point(6, 20)), 	// The anchor for this image is the base of the flagpole at 0,32.
	   	new google.maps.MarkerImage('img/icongoods.png',      
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
