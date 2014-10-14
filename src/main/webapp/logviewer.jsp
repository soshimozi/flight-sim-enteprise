<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, java.util.*, net.fseconomy.util.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
<%
    Object[] output = null;

    String sGroupId = request.getParameter("group");
    String sUser = request.getParameter("pilot");
    String sAircraft = request.getParameter("aircraft");

    if (sAircraft != null)
        output = data.outputLog("aircraft='" + Converters.escapeSQL(sAircraft) + "'");
    else if (sUser != null)
    {
        if (sUser.equals(user.getName()))
            output = data.outputLog("user='" + Converters.escapeSQL(sUser) + "'");
    } else if (sGroupId != null)
    {
        int groupId = Integer.parseInt(sGroupId);
        if (user.groupMemberLevel(groupId) > UserBean.GROUP_INVITED)
            output = data.outputLog("groupId=" + groupId);
    }
    if (output == null)
    {
%>
<div class="error">No logs to view</div>
<%
        return;
    }

    String array = (String) output[0];
    List aircraft = (List) output[1];
    List pilots = (List) output[2];
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="scripts/wz_jsgraphics.js"></script>
    <script type="text/javascript">
        var log = [ <%= array %>];
        var jg;

        function formatFlight(id)
        {
            return log[id][8] + ' ' + log[id][0] + ' ' + log[id][2] + '->' + log[id][3] + ' ' + log[id][1];
        }
        function fillFlights()
        {
            var select = document.getElementById('flights');
            var aircraft = document.getElementById('aircraft');
            var pilots = document.getElementById('pilots');

            var element = 0;
            while (select.length > 0)
                select[0] = null;
            select.selectedIndex = 0;
            for (c = 0; c < log.length; c++)
            {
                if (aircraft && aircraft.selectedIndex > 0 && aircraft[aircraft.selectedIndex].value != log[c][1])
                    continue;
                if (pilots && pilots.selectedIndex > 0 && pilots[pilots.selectedIndex].value != log[c][0])
                    continue;

                select[element++] = new Option(formatFlight(c), c);
            }
        }
        function selectAll()
        {
            var select = document.getElementById('flights');
            for (c = 0; c < select.length; c++)
                select[c].selected = true;
        }
        function box(pixelsX, pixelsY)
        {
            this.minLat = 90;
            this.minLon = 180;
            this.maxLat = -90;
            this.maxLon = -180;
            this.pixelsX = pixelsX;
            this.pixelsY = pixelsY;
            this.check = function (lat, lon)
            {
                if (lat < this.minLat)
                    this.minLat = lat;
                if (lat > this.maxLat)
                    this.maxLat = lat;
                if (lon < this.minLon)
                    this.minLon = lon;
                if (lon > this.maxLon)
                    this.maxLon = lon;
            }
            this.fix = function()
            {
                var dlat = this.maxLat - this.minLat;
                var dlon = this.maxLon - this.minLon;
                var delta = 2 + (dlat > dlon ? dlat : dlon);
                this.maxLat += (delta - dlat)/2;
                this.minLat -= (delta - dlat)/2;
                this.maxLon += (delta - dlon)/2;
                this.minLon -= (delta - dlon)/2;
                this.pixelsPerDegreeX = this.pixelsX / (this.maxLon - this.minLon);
                this.pixelsPerDegreeY = this.pixelsY / (this.maxLat - this.minLat);
            }
            this.toX = function(Lon)
            {
                return Math.round((Lon - this.minLon) * this.pixelsPerDegreeX);
            }
            this.toY = function(Lat)
            {
                return Math.round((this.maxLat - Lat) * this.pixelsPerDegreeY);
            }

        }

        function show()
        {
            var flights = new Array();
            var select = document.getElementById('flights');
            var bounds = new box(640, 480);

            for (c = 0; c < select.length; c++)
                if (select[c].selected)
                    flights.push(log[select[c].value]);
            for (c = 0; c < flights.length; c++)
            {
                bounds.check(flights[c][4], flights[c][5]);
                bounds.check(flights[c][6], flights[c][7]);
            }
            bounds.fix();
            jg.clear();
            var output = document.getElementById('output');
            output.src='http://www2.demis.nl/wms/wms.asp?wms=WorldMap&Version=1.1.0&Format=image/gif&Request=GetMap&BBox=' +
                bounds.minLon + ',' + bounds.minLat + ',' + bounds.maxLon + ',' + bounds.maxLat + '&SRS=EPSG:4326&Width=640&Height=480&Layers=Bathymetry,Countries,Topography,Hillshading,Builtup+areas,Coastlines,Waterbodies,Rivers,Railroads,Highways,Roads,Borders';
            for (c = 0; c < flights.length; c++)
            {
                jg.drawLine(bounds.toX(flights[c][5]), bounds.toY(flights[c][4]), bounds.toX(flights[c][7]), bounds.toY(flights[c][6]));
            }
            jg.paint();
        }

        function init()
        {
            fillFlights();
            jg = new jsGraphics('outDiv');
            jg.setStroke(2);

        }
    </script>
</head>
<body onload="init()">
	<div id="logviewer">
	<div class="monospace">
	<form>
		<select id="flights" multiple="true" name="flights" size="10">
		</select>
<%
    if (aircraft.size() > 1)
    {
%>
		<select id="aircraft" size="10" onchange="fillFlights()">
			<option value="">[ All ]</option>
<%      for (Iterator i = aircraft.iterator(); i.hasNext(); )
        {
		    String ac = (String) i.next();
%>
		    <option value="<%= ac %>"><%= ac %></option>
<%
        }
%>
		</select>
<%
    }
%>
<%
    if (pilots.size() > 1)
    {
%>
		<select id="pilots" size="10" onchange="fillFlights()">
			<option value="" selected="true">[ All ]</option>
<%
        for (Iterator i = pilots.iterator(); i.hasNext(); )
        {
		    String ac = (String) i.next();
%>
		    <option value="<%= ac %>"><%= ac %></option>
<%
        }
%>
		</select>
<%
    }
%>
		<br/>
		<input type="button" class="button" onclick="selectAll()" value="Select all" />
		<input type="button" class="button" onclick="show()" value="Show" />
	</form>
	</div>
	<div id="outDiv" style="position:relative;height:480px;width:640;">
		<img id="output" width="640" height="480" src="img/empty.gif"/>
	</div>
	</div>
</body>
</html>
