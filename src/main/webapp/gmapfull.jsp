<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    String sType = request.getParameter("type");
    String sId = request.getParameter("id");
    String sTransferId = request.getParameter("transferid");
%>

<html>
<head>
    <title>FSE Maps</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css'>
    <link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css'>

    <style>
        .navbar-inverse {
            background-color: #222222
        }

        .navbar-inverse .navbar-nav > .active > a:hover, .navbar-inverse .navbar-nav > li > a:hover, .navbar-inverse .navbar-nav > li > a:focus {
            background-color: #000000
        }

        .navbar-inverse .navbar-nav > .active > a, .navbar-inverse .navbar-nav > .open > a, .navbar-inverse .navbar-nav > .open > a, .navbar-inverse .navbar-nav > .open > a:hover, .navbar-inverse .navbar-nav > .open > a, .navbar-inverse .navbar-nav > .open > a:hover, .navbar-inverse .navbar-nav > .open > a:focus {
            background-color: #080808
        }

        .dropdown-menu {
            background-color: #ffffff
        }

        .dropdown-menu > li > a:hover, .dropdown-menu > li > a:focus {
            background-color: #428bca
        }

        .navbar-inverse {
            background-image: none;
        }

        .dropdown-menu > li > a:hover, .dropdown-menu > li > a:focus {
            background-image: none;
        }

        .navbar-inverse {
            border-color: #080808
        }

        .navbar-inverse .navbar-brand {
            color: #999999
        }

        .navbar-inverse .navbar-brand:hover {
            color: #ffffff
        }

        .navbar-inverse .navbar-nav > li > a {
            color: #999999
        }

        .navbar-inverse .navbar-nav > li > a:hover, .navbar-inverse .navbar-nav > li > a:focus {
            color: #ffffff
        }

        .navbar-inverse .navbar-nav > .active > a, .navbar-inverse .navbar-nav > .open > a, .navbar-inverse .navbar-nav > .open > a:hover, .navbar-inverse .navbar-nav > .open > a:focus {
            color: #ffffff
        }

        .navbar-inverse .navbar-nav > .active > a:hover, .navbar-inverse .navbar-nav > .active > a:focus {
            color: #ffffff
        }

        .dropdown-menu > li > a {
            color: #333333
        }

        .dropdown-menu > li > a:hover, .dropdown-menu > li > a:focus {
            color: #ffffff
        }

        .navbar-inverse .navbar-nav > .dropdown > a .caret {
            border-top-color: #999999
        }

        .navbar-inverse .navbar-nav > .dropdown > a:hover .caret {
            border-top-color: #ffffff
        }

        .navbar-inverse .navbar-nav > .dropdown > a .caret {
            border-bottom-color: #999999
        }

        .navbar-inverse .navbar-nav > .dropdown > a:hover .caret {
            border-bottom-color: #ffffff
        }
    </style>

    <style>
        body {
            padding-top: 50px; /* for nav */
        }

        html, body {
            height: 100%;
            width: 100%;
        }

        #map-canvas {
            height: 100%;
            width: 100%;
        }

        .container-map {
            width: 100%;
            height: 100%;
        }
    </style>

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="http://maps.googleapis.com/maps/api/js?libraries=visualization&sensor=false"></script>

    <script>
        var map;
        var markers = [];
        function initMap() {
            var myOptions =
            {
                zoom: 2,
                center: new google.maps.LatLng(36.0, -15.0),
                mapTypeId: google.maps.MapTypeId.ROADMAP,
                disableDefaultUI: false,
                scrollwheel: true,
                draggable: true,
                navigationControl: true,
                mapTypeControl: true,
                scaleControl: true,
                disableDoubleClickZoom: false
            };

            map = new google.maps.Map(document.getElementById("map-canvas"), myOptions);
        }

        function mapAssignments(json)
        {
            json.mapAssignments.forEach(function(entry){
                createLines(entry);
                createMarkers(entry);
            });
            if(type === "myflight")
                createAircraft(json.mapAircraftInfo);
        }

        function mapAircraft(json)
        {
            json.mapAircraft.forEach(function(entry){
                createAircraftMarker(entry);
            });
        }

        // Create Markers

        var iconAircraft = {
            url: "img/aircraft.png",
            size: new google.maps.Size(32, 37),
            anchor: new google.maps.Point(16,18)
        };

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

        var infoWindow = new google.maps.InfoWindow();

        function createAircraft(entry){
            var loc = entry.latlon;
            var markerAc = new google.maps.Marker({
                map: map,
                icon: iconAircraft,
                position: new google.maps.LatLng(loc.lat, loc.lon),
                title: entry.icao + "\n" + entry.registration + "\n" + entry.makemodel + "\n" + entry.equipment + "\n" + entry.fuel
            });
        }

        function createLines(entry){
            var departLatLon = entry.departure.latlon;

            entry.destinations.forEach(function(dest){
                var destLatLon = dest.latlon;
                var line = new google.maps.Polyline({
                    map: map,
                    path: [new google.maps.LatLng(departLatLon.lat, departLatLon.lon), new google.maps.LatLng(destLatLon.lat, destLatLon.lon)],
                    geodesic: true
                });

            });

        }

        function createMarkers(entry){
            var depart = entry.departure;
            var markerDepart = new google.maps.Marker({
                map: map,
                icon: iconTakeoff,
                position: new google.maps.LatLng(depart.latlon.lat, depart.latlon.lon),
                title: depart.icao + " - " + depart.name
            });

            var assignmentTable = makeAssignmentTable(depart.icao, entry.assignments);

            google.maps.event.addListener(markerDepart, 'click', function(){
                infoWindow.setContent(assignmentTable);
                infoWindow.open(map, markerDepart);
            });


            markers.push(markerDepart);

            entry.destinations.forEach(function(dest){
                var markerDest = new google.maps.Marker({
                    map: map,
                    icon: iconLanding,
                    position: new google.maps.LatLng(dest.latlon.lat, dest.latlon.lon),
                    title: dest.icao + " - " + dest.name
                });

                markers.push(markerDest);
            });

        };

        function makeAssignmentTable(icao, assignments)
        {
            var assignmentTable = "<p>Assignments at <strong>" + icao + "</strong></p>";
            assignmentTable += "<table border='1' cellpadding='4' style='text-align:center;font-size:10px;'>";
            assignmentTable += "<tr><th>ICAO</th><th>Cargo</th><th>Pay</th><th>NM</th></tr>";
            assignments.forEach(function(item){
                assignmentTable += "<tr><td>" + item.icao + "</td><td>" + item.cargo + "</td><td>" + item.pay + "</td><td>" + item.distance + "</td></tr>";
            });
            assignmentTable += "</table>";

            return assignmentTable
        }

        //display selected marker info
        function openInfoWindow(){
            //e.preventDefault();
            google.maps.event.trigger(vm.selectedItem, 'click');
        }

        //google.maps.event.addDomListener(window, "load", initMap);

    </script>

    <script>
        var type = "<%=sType%>";
        var id = <%=sId == null ? "undefined" : sId %>;

        //load assignments
        function loadAssigments(type, id) {
            $.getJSON("assignmentsjson.jsp", {"type": type, "id": id})
                .success(function(json)
                {
                    console.log("JSON Data Loaded");

                    mapAssignments(json);
                })
                .fail(function(jqxhr, textStatus, error)
                {
                    var err = jqxhr.responseText;
                    alert( "Request Failed: " + err.replace(/^\s+|\s+$/gm, ''));
                });
        }

        //load assignments
        function loadAircraft(type, id) {
            $.getJSON("aircraftjson.jsp", {"type": type, "id": id})
                    .success(function(json)
                    {
                        console.log("JSON Data Loaded");

                        mapAssignments(json);
                    })
                    .fail(function(jqxhr, textStatus, error)
                    {
                        var err = jqxhr.responseText;
                        alert( "Request Failed: " + err.replace(/^\s+|\s+$/gm, ''));
                    });
        }

        $(function () {
            initMap();

            if(type == "group")
                loadAssigments("group", id);
            if(type == "myflight")
                loadAssigments("myflight", 0);
            if(type == "transfer")
                loadAssigments("transfer", id);
            if(type == "aircraft")
                loadAircraft("aircraft", id);
        });

    </script>
</head>
<body>
<header class="navbar navbar-inverse navbar-fixed-top bs-docs-nav" role="banner">
    <div class="container">
        <div class="navbar-header">
            <button class="navbar-toggle" type="button" data-toggle="collapse" data-target=".bs-navbar-collapse">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="./">FSEconomy</a>
        </div>
        <nav class="collapse navbar-collapse bs-navbar-collapse" role="navigation">
            <ul class="nav navbar-nav">

                <%--<li>--%>
                <%--<a href="#">Getting started</a>--%>
                <%--</li>--%>
                <%--<li class="dropdown">--%>
                <%--<a class="dropdown-toggle" href="#" data-toggle="dropdown">Dropdown <b class="caret"></b></a>--%>
                <%--<ul class="dropdown-menu">--%>
                <%--<li><a href="#">Action</a></li>--%>
                <%--<li><a href="#">Another action</a></li>--%>
                <%--<li><a href="#">Something else here</a></li>--%>
                <%--<li><a href="#">Separated link</a></li>--%>
                <%--<li><a href="#">One more separated link</a></li>--%>
                <%--</ul>--%>
                <%--</li>--%>

            </ul>
        </nav>
    </div>
</header>

<div class="container">
    <div class="row">

    </div>

</div>

<div class="container-map">
    <div id="map-canvas"></div>
</div>

</body>
</html>
