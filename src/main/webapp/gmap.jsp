<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    boolean isDest = false;
    boolean isFBO;
    double latd=0;
    double lond=0;

    String icao = request.getParameter("icao");
    AirportBean airportd = Airports.getAirport(icao);
    AirportBean airportl = Airports.getAirport(icao);
    List<AssignmentBean> assignments;
    List<FboBean> fboList;
    AssignmentBean assignment;
    AirportBean destination;
    GoodsBean fuelleft;

    double latl = airportl.getLat();
    double lonl = airportl.getLon();

    String icaod = request.getParameter("icaod");
    String type;
    String[] jobs;
    String image;

    double fuelprice;

    if(icaod != null)
    {
        airportd = Airports.getAirport(icaod);
        latd = airportd.getLat();
        lond = airportd.getLon();
        Airports.fillAirport(airportd);
        isDest = true;
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FS Economy Google Map</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
<%
//<jsp:include flush="true" page="gmapapikey.jsp"></jsp:include>

    String server = request.getServerName();
    if (server.contains("localhost"))
    {
%>
    <script src="https://maps.google.com/maps?file=api&amp;v=2.58&amp;key=ABQIAAAAG82z-GJItM3IuG9EOBZAfRSVAaBWqUyeYUcKh2IdrcS-zyqYshS9pF6VeGdPA5RGfpaOt6r7Qr_ewg" type="text/javascript"></script>
<%
    }
    else if (server.contains("fseconomy.net"))
    {
%>
    <script src="https://maps.google.com/maps?file=api&amp;v=2.58&amp;key=AIzaSyDpmF0JgC7Oq-KJ-dxPM1eOFnNxhTzwQ2o"></script>
<% 
    }
    else
    {
%>	
<script type="text/javascript">
	alert('API Key Not Found');
</script>
<%	
    }
%>
<script type="text/javascript"> 
	//SET BEARING - Function by Mike Williams
		
     var degreesPerRadian = 180.0 / Math.PI;
     var radiansPerDegree = Math.PI / 180.0;
 
     // Returns the bearing in degrees between two points.
     // North = 0, East = 90, South = 180, West = 270.
     function bearing( from, to ) 
     {
       // See T. Vincenty, Survey Review, 23, No 176, p 88-93,1975.
       // Convert to radians.
       var lat1 = from.latRadians();
       var lon1 = from.lngRadians();
       var lat2 = to.latRadians();
       var lon2 = to.lngRadians();

       // Compute the angle.
       var angle = - Math.atan2( Math.sin( lon1 - lon2 ) * Math.cos( lat2 ), Math.cos( lat1 ) * Math.sin( lat2 ) - Math.sin( lat1 ) * Math.cos( lat2 ) * Math.cos( lon1 - lon2 ) );
       if ( angle < 0.0 )
			angle  += Math.PI * 2.0;

       // And convert result to degrees.
       angle = angle * degreesPerRadian;
       angle = angle.toFixed(1);

       return angle;
     }

</script>

</head>
<body onload="load()" onunload="GUnload()" text="#000080" bgcolor="#FFFFFF" background="">

<script type="text/javascript">
function load() 
{
	if (GBrowserIsCompatible()) 
	{

	// ==================================================
	// A function to create a tabbed marker and set up the event window
	// This version accepts a variable number of tabs, passed in the arrays htmls[] and labels[]
	function createTabbedMarker(point,htmls,labels) 
	{
		var marker = new GMarker(point,icondest);
		GEvent.addListener(marker, "click", 
			function() 
			{
			// adjust the width so that the info window is large enough for this many tabs
				if (htmls.length > 2) 
				{
					htmls[0] = '<div style="width:'+htmls.length*88+'px">' + htmls[0] + '</div>';
				}
				var tabs = [];
				for (var i=0; i<htmls.length; i++) 
				{
					tabs.push(new GInfoWindowTab(labels[i],htmls[i]));
				}
				marker.openInfoWindowTabsHtml(tabs);
			}
		);
		
		return marker;
	}

//-----------------------------------------------------------------------------------------------------
	var iconloc = new GIcon();
	iconloc.image = "img/iconac.png";
	iconloc.shadow = "img/iconshadow.png";
	iconloc.iconSize = new GSize(12, 20);
	iconloc.shadowSize = new GSize(20, 20);
	iconloc.iconAnchor = new GPoint(6, 20);
	iconloc.infoWindowAnchor = new GPoint(5, 1);
	
//-----------------------------------------------------------------------------------------------------
	var icondest = new GIcon();
	icondest.image = "img/icondest.png";
	icondest.shadow = "img/iconshadow.png";
	icondest.iconSize = new GSize(12, 20);
	icondest.shadowSize = new GSize(20, 20);
	icondest.iconAnchor = new GPoint(6, 20);
	icondest.infoWindowAnchor = new GPoint(5, 1);
//--------------------------------------------------------------------------------------------------
	var map = new GMap2(document.getElementById("map"));
//	map.setCenter(new GLatLng(<%=latl%>,<%=lonl%>), 5);
	map.setCenter(new GLatLng(0,0),0);
	var bounds = new GLatLngBounds();

	map.enableDoubleClickZoom();
	map.enableContinuousZoom();
//----------------------------------------------------------------------------------------------------
	map.addControl(new GSmallZoomControl());
	map.addControl(new GMapTypeControl());
	map.addControl(new GScaleControl());
	map.addControl(new GOverviewMapControl());
	
//----------------------------------------------------------------------------------------------------
    var pointloc = new GLatLng(<%=latl%>,<%=lonl%>);
	var markerloc = new GMarker(pointloc,iconloc);
	var jobstring="";
	var fbostring="";
	var ap="";
	
	bounds.extend(pointloc);

	GEvent.addListener(markerloc, "click", 
		function() 
		{
  			markerloc.openInfoWindowHtml("<font face='Verdana' size='1'><%=airportl.getIcao()%><br><%=airportl.getName()%><br><%=airportl.getCity()%>, <%=airportl.getCountry()%></font>");
		}
	);
	
	map.addOverlay(markerloc); 	

	if(<%=isDest%>)
	{
		var pointdest = new GLatLng(<%=latd%>,<%=lond%>);
		ap = "<font face='Verdana' size='1'><%=airportd.getIcao()%><br><%=airportd.getName()%><br><%=airportd.getCity()%>, <%=airportd.getCountry()%></font>";
<%		assignments = Assignments.getAssignments(icaod, -1, -1, -1, -1);
		
		if (assignments.size() != 0)
		{
		    int counter = 0;
			jobs = new String[assignments.size()];
			for (AssignmentBean bean : assignments)
			{
				assignment = bean;
				destination = assignment.getDestinationAirport();
				image = "<img src='img/set2_"+ assignment.getBearingImage() + ".gif'>";
				type = assignment.getType() == AssignmentBean.TYPE_ALLIN ? "A" : "T";
				jobs[counter] = image+" | $"+assignment.calcPay()+" | "+destination.getIcao()+" | "+assignment.getDistance()+"nm | "+
				Converters.escapeJavaScript(assignment.getSCargo())+" | "+type+" | Ex: "+assignment.getSExpires();
%>
                jobstring+="<%=jobs[counter]%>"+"<br>";
<%
                counter++;
            }
		}
			
		fboList = Fbos.getFboByLocation(icaod);
		fuelprice = airportd.getFuelPrice();
		String fuel = Formatters.currency.format(fuelprice);
		isFBO = false;
		
		if(fboList.size() != 0)
		{
			isFBO = true;
			for (FboBean fbo : fboList) 
			{				
		        String temp  ;
				fuelleft = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_FUEL100LL);
				int fuelgallons = 0;
				if (fuelleft != null)
					fuelgallons = (int)Math.floor(fuelleft.getAmount()/Constants.GALLONS_TO_KG);
				
				temp = Converters.escapeJavaScript(fbo.getName())+" |  Fuel: " + Formatters.currency.format(fbo.getFuel100LL())+" | "+ fuelgallons + " gals";
				if ((fbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0)
					temp +=" | Repairs";
%>
				fbostring+="<%= temp %>"+"<br>";
<%			}
		}
		
		if (airportd.isAvgas()) 
		{
			isFBO = true;
%>			fbostring+="Local | <%=fuel%> | unlimited |";
<%		}
		
		if (airportd.getSize() >= AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE)
		{
			isFBO = true;
%>			fbostring+= " Repairs ";
<%		}		 
			
		if( assignments.size() > 0 && isFBO)
		{
%>
            var markerdest = createTabbedMarker(pointdest, [ap, "<span  style="font-family: Verdana; font-size: xx-small; ">"+fbostring+"</font><br>", "<span  style="font-family: Verdana; font-size: xx-small; ">"+jobstring+"</font><br>"],["Airport","FBOs","Jobs"]);
<%
        }
		else if( assignments.size() == 0 && isFBO)
		{
%>
            var markerdest = createTabbedMarker(pointdest, [ap, "<span  style="font-family: Verdana; font-size: xx-small; ">"+fbostring+"</font><br>"],["Airport","FBOs"]);
<%
        }
		else if( assignments.size() > 0 && !isFBO)
		{
%>
            var markerdest = createTabbedMarker(pointdest, [ap, "<span  style="font-family: Verdana; font-size: xx-small; ">"+jobstring+"</font><br>"],["Airport","Jobs"]);
<%
        }
		else 
		{ 
%>
            var markerdest = createTabbedMarker(pointdest, [ap],["Airport"]);
<%
        }
%>	

		//SET DISTANCE & BEARING
		var degrees = Math.round(bearing(pointloc,pointdest));
		var msg = "Trip: " + '<font color="#0080C0"><%=icao%></font>' + " to " + '<font color="#0080C0"><%=icaod%></font>' + "&nbsp;&nbsp;&nbsp;&nbsp;Distance: "+ "<span style="color: #0080C0; ">" + Math.round(pointloc.distanceFrom(pointdest) * 0.000539956803) + " NM </font>&nbsp;&nbsp;&nbsp;&nbsp;Bearing: "+ "<span style="color: #0080C0; ">" + degrees + "&#186;</font>";
		document.getElementById("mypoint").innerHTML = msg;  
						
		bounds.extend(pointdest);	
		map.addOverlay(markerdest);	
		map.setZoom(map.getBoundsZoomLevel(bounds)-1);
		var clat = (bounds.getNorthEast().lat() + bounds.getSouthWest().lat()) /2;
		var clng = (bounds.getNorthEast().lng() + bounds.getSouthWest().lng()) /2;
		map.setCenter(new GLatLng(clat,clng));	
	} 
	else 
	{
		map.setZoom(map.getBoundsZoomLevel(bounds)-4);
		var clat = (bounds.getNorthEast().lat() + bounds.getSouthWest().lat()) /2;
		var clng = (bounds.getNorthEast().lng() + bounds.getSouthWest().lng()) /2;
		map.setCenter(new GLatLng(clat,clng));	
		map.setMapType(G_HYBRID_TYPE);
	}
	}
}
    </script>

	<table border="0" cellpadding="0" style="border-collapse: collapse" width="720" id="table6">
		<tr><td><div id="map" style="width: 600px; height: 500px"></div></td></tr>
		<!-- Div to hold Lat-Lon when I'm using the function -->
		<tr><td><div id="mypoint" style="font-family:Arial, Helvetica, sans-serif;font-size:14pt;line-height:1.1em"></div ></td></tr>
	</table>

</body>
</html>
