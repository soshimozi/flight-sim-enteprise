<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.*"
%>
<%
    Data data = (Data)application.getAttribute("data");

    FboBean[] fbos = data.getFboForSale();
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FBOs For Sale Map</title>

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
	<%=fbos.length%> FBOs are for Sale.
	<script type="text/javascript">
		document.write('Your map data took <span id="endTime">0.0</span> seconds to load.');
		var loopTime=setInterval("currentTime()",100);
	</script>
	<br />
 	<img src="img/iconac.gif"/>&nbsp;1 Lot &nbsp;&nbsp;&nbsp;&nbsp;
	<img src="img/icondest.gif"/>&nbsp;2 Lot &nbsp;&nbsp;&nbsp;&nbsp;
	<img src="img/icongoods.gif"/>&nbsp;3 Lot &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
</form>
<div id="map" style="width: 640px; height: 480px;"></div>

<script type="text/javascript">
    var locations = 
        [
<%
for (int c=0; c < fbos.length; c++)
{
	AirportBean airport = data.getAirport(fbos[c].getLocation());
	double lat = airport.getLat();
	double lon = airport.getLon();
	String airportLink = Converters.escapeJavaScript(data.airportLink(airport, response));
	int sizeIcon = fbos[c].getFboSize() - 1;
	
	StringBuilder sb = new StringBuilder();
	sb.append("<div class=\"infowindow-content\">");
	//sb.append(airportLink);
	sb.append(airport.getIcao());
	sb.append("<br>");
	sb.append(Converters.escapeJavaScript(airport.getName()));
	sb.append(", ");
	sb.append(Converters.escapeJavaScript(airport.getCountry()));
	sb.append("<br>");
	sb.append(Converters.escapeJavaScript(data.getAccountNameById(fbos[c].getOwner())));
	sb.append("<br>");
	sb.append(Formatters.currency.format(fbos[c].getPrice()));
	sb.append("</div>");
%>
			[<%=lat%>, <%=lon%>, <%=sizeIcon%>, '<%=sb.toString()%>'], 
<%
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

    for (i = 0; i < locations.length; i++) {  
      marker = new google.maps.Marker({
        icon: markerImage[locations[i][2]],
        position: new google.maps.LatLng(locations[i][0], locations[i][1]),
        map: map
      });

      bindInfoWindow(marker, map, infowindow, locations[i][3], "");   

    }

    function bindInfoWindow(marker, map, infowindow, html, Ltitle) { 
        google.maps.event.addListener(marker, 'mouseover', function() {
                infowindow.setContent(html); 
                infowindow.open(map, marker); 

        });
        google.maps.event.addListener(marker, 'mouseout', function() {
            infowindow.close();

        }); 
    }     
    
	clearTimeout(loopTime);
  </script>

</body>
</html>
