<%@ page import="net.fseconomy.data.Airports" %>
<%@ page import="net.fseconomy.beans.CachedAirportBean" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    String sDepart = request.getParameter("depart");
    String sDest = request.getParameter("dest");
    CachedAirportBean depart = Airports.cachedAirports.get(sDepart);
    CachedAirportBean dest = Airports.cachedAirports.get(sDest);
%>

<div class="container-map">
    <div id="map-canvas" style="height: 380px; width: 570px;"></div>
</div>

<script>

    var map;
    var markers = [];

    var iconLanding = {
        url: "img/airport-landing-up.png",
        anchor: new google.maps.Point(16,2),
        size: new google.maps.Size(32, 37)
        //origin: new google.maps.Point(0,0)
    };

    var iconTakeoff = {
        url: "img/airport-takeoff.png",
        size: new google.maps.Size(32, 37),
        origin: new google.maps.Point(0,0)
    };

    function initMap() {
        var myOptions =
        {
            zoom: 6,
            center: new google.maps.LatLng(<%=depart.getLatLon().lat%>, <%=depart.getLatLon().lon%>),
            mapTypeId: google.maps.MapTypeId.TERRAIN,
            disableDefaultUI: false,
            scrollwheel: true,
            draggable: true,
            navigationControl: true,
            mapTypeControl: true,
            scaleControl: true,
            disableDoubleClickZoom: false
        };

        var canvas = document.getElementById("map-canvas");
        map = new google.maps.Map(canvas, myOptions);

        var line = new google.maps.Polyline({
            map: map,
            path: [new google.maps.LatLng(<%=depart.getLatLon().lat%>, <%=depart.getLatLon().lon%>), new google.maps.LatLng(<%=dest.getLatLon().lat%>, <%=dest.getLatLon().lon%>)],
            geodesic: true
        });

        var markerDepart = new google.maps.Marker({
            map: map,
            icon: iconTakeoff,
            position: new google.maps.LatLng(<%=depart.getLatLon().lat%>, <%=depart.getLatLon().lon%>),
            title: "<%=depart.getIcao()%> - <%=depart.getTitle()%>"
        });

        var markerDest = new google.maps.Marker({
            map: map,
            icon: iconLanding,
            position: new google.maps.LatLng(<%=dest.getLatLon().lat%>, <%=dest.getLatLon().lon%>),
            title: "<%=dest.getIcao()%> - <%=dest.getTitle()%>"
        });

        //google.maps.event.trigger(map, 'resize');
    }

</script>
