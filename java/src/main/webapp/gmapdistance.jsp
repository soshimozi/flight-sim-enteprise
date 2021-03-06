<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*, net.fseconomy.dto.DistanceBearing"
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

    String depart = request.getParameter("depart");
    String dest = request.getParameter("dest");

    if(Helpers.isNullOrBlank(depart)
    || Helpers.isNullOrBlank(dest) )
    {
        request.getSession().setAttribute("message", "Missing parameter.");
        request.getRequestDispatcher("error.jsp").forward(request, response);
    }

    depart = depart.trim().toUpperCase();
    dest = dest.trim().toUpperCase();

    if(!Airports.isValidIcao(depart))
    {
        request.getSession().setAttribute("message", "Invalid departure ICAO.");
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }

    if(!Airports.isValidIcao(dest))
    {
        request.getSession().setAttribute("message", "Invalid destination ICAO.");
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }

    DistanceBearing distanceBearing = Airports.getDistanceBearing(depart, dest);
    int distance = (int)(distanceBearing.distance + .5);
    int bearing = (int)(distanceBearing.bearing + .5);

    CachedAirportBean apDepart = Airports.cachedAirports.get(depart);
    CachedAirportBean apDest = Airports.cachedAirports.get(dest);
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>Distance Map</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <script src="https://maps.google.com/maps/api/js?sensor=false&key=AIzaSyDpmF0JgC7Oq-KJ-dxPM1eOFnNxhTzwQ2o" type="text/javascript"></script>

    <script type="text/javascript">
        var startTime=new Date();
        function currentTime(){
          var a=Math.floor((new Date()-startTime)/100)/10;
          if (a%1==0)
              a+=".0";

          document.getElementById("endTime").innerHTML=a;
        }
    </script>

</head>
<body text="#000080" bgcolor="#FFFFFF" background="">

<form name="fboForm" id="fboForm" method="post" action="userctl">
	<script type="text/javascript">
	<!-- Begin
	document.write('Your map data took <span id="endTime">0.0</span> seconds to load.');
	var loopTime=setInterval("currentTime()",100);
	// End -->
	</script>
</form>
<span style="color: red"><%=depart%></span> to <span style="color: red"><%=dest%></span>&nbsp;&nbsp;&nbsp;&nbsp;Distance: <span style="color: red"><%=distance%> NM </span>&nbsp;&nbsp;&nbsp;&nbsp;Bearing: <span style="color: red"><%=bearing%>&#186;</span>;
<div id="map" style="width: 640px; height: 480px;"></div>
  <script type="text/javascript">
    var locations = 
        [
<%
StringBuilder sb = new StringBuilder();

	double latDepart = apDepart.getLatLon().lat;
	double lonDepart = apDepart.getLatLon().lon;
	
	sb.append("<div class=\"infowindow-content\">");
    sb.append("<a href=\"airport.jsp?icao=" + depart + "\">");
	sb.append(Converters.escapeJavaScript(apDepart.getTitle()));
	sb.append("</a>");
	sb.append("</div>");
	String departInfo = sb.toString();
	
	double latDest = apDest.getLatLon().lat;
	double lonDest = apDest.getLatLon().lon;
	
	sb = new StringBuilder();
	sb.append("<div class=\"infowindow-content\">");
    sb.append("<a href=\"airport.jsp?icao="+ dest + "\">");
	sb.append(Converters.escapeJavaScript(apDest.getTitle()));
	sb.append("</a>");
	sb.append("</div>");

	String destInfo = sb.toString();
%>
			[<%=latDepart%>, <%=lonDepart%>, 0, '<%=departInfo%>'], 
			[<%=latDest%>, <%=lonDest%>, 1, '<%=destInfo%>'] 
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
