<%@page language="java"
		contentType="text/html; charset=ISO-8859-1"
		import="java.util.*, org.apache.commons.lang3.math.*, net.fseconomy.beans.*, net.fseconomy.dto.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	if(user == null || !user.isLoggedIn())
	{
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
		return;
	}

	CachedAirportBean airport = null;

	//setup return page if action used
	String returnPage = null;
	String selectedIcao = request.getParameter("icao");

	//setup return page if action used
	if(selectedIcao != null)
	{
		selectedIcao = selectedIcao.toUpperCase();
		returnPage = request.getRequestURI() + "?icao=" + selectedIcao;
		airport = Airports.cachedAirports.get(selectedIcao);
		session.setAttribute("icao", selectedIcao);
	}
	else
	{
		selectedIcao = (String)session.getAttribute("icao");
		if(!Helpers.isNullOrBlank(selectedIcao))
		{
			returnPage = request.getRequestURI() + "?icao=" + selectedIcao;
			airport = Airports.cachedAirports.get(selectedIcao);
		}
	}

	response.addHeader("referer", request.getRequestURI());

	//This has to be here to allow the op chart json data to load
	boolean isSearch = request.getParameter("submit") != null;
	boolean hasAssignments = request.getParameter("assignments") != null;
	boolean toAirport = "1".equals(request.getParameter("toAirport"));
	boolean ferry = request.getParameter("ferry") != null;
	boolean hasFuel = request.getParameter("hasFuel") != null;
	boolean hasJeta = request.getParameter("hasJeta") != null;
	boolean hasRepair = request.getParameter("hasRepair") != null;
	boolean hasAcForSale = request.getParameter("hasAcForSale") != null;
	boolean hasFbo = request.getParameter("hasFbo") != null;
	String modelParam = request.getParameter("model");
	String nameParam = request.getParameter("name");
	String fromParam = request.getParameter("from");
	String registration = request.getParameter("registration");
	String distanceParam = request.getParameter("distance");
	String capableParam = request.getParameter("capable");
	boolean goodsMode = "buy".equals(request.getParameter("goodsMode"));
	String commodityParam = request.getParameter("commodity");
	int minAmount = NumberUtils.toInt(request.getParameter("minAmount"), 100);
	boolean isRentable = request.getParameter("rentable") != null;
	String sKgFilterQty = request.getParameter("KGFilterQty");
	String sKgFilter = request.getParameter("KGFilter");
	String sPaxFilterQty = request.getParameter("PaxFilterQty");
	String sPaxFilter = request.getParameter("PaxFilter");

	//Strings for the lat long display.
	String latNS;
	String lonEW;

	int modelId = -1;
	int distance = -1;
	int capable = -1;
	int commodity = 0;

	int minPax = -1;
	int maxPax = -1;
	int PaxFilter = 0;
	int PaxFilterQty = -1;

	if (!Helpers.isNullOrBlank(sPaxFilterQty))
		PaxFilterQty = Integer.parseInt(request.getParameter("PaxFilterQty"));

	if (!Helpers.isNullOrBlank(sPaxFilter))
		PaxFilter = Integer.parseInt(request.getParameter("PaxFilter"));

	if (PaxFilter == 1)
		minPax = PaxFilterQty;
	else if (PaxFilter == 2)
		maxPax = PaxFilterQty;

	int minKG = -1;
	int maxKG = -1;
	int KGFilter = 0;
	int KGFilterQty = -1;

	if (!Helpers.isNullOrBlank(sKgFilterQty))
		KGFilterQty = Integer.parseInt(sKgFilterQty);

	if (!Helpers.isNullOrBlank(sKgFilter))
		KGFilter = Integer.parseInt(sKgFilter);

	if (KGFilter == 1)
		minKG = KGFilterQty;
	else if (KGFilter == 2)
		maxKG = KGFilterQty;

	if (Helpers.isNullOrBlank(nameParam))
		nameParam = null;

	if (Helpers.isNullOrBlank(fromParam))
		fromParam = null;

	if (!Helpers.isNullOrBlank(commodityParam))
		commodity = Integer.parseInt(commodityParam);

	if (!Helpers.isNullOrBlank(capableParam))
		capable = Integer.parseInt(capableParam);

	if (!Helpers.isNullOrBlank(registration) && isSearch)
	{
		AircraftBean ac = Aircraft.getAircraftById(Aircraft.getAircraftIdByRegistration(registration));
		if (ac != null && ac.getLocation() != null)
		{
			selectedIcao = ac.getLocation();
			airport = Airports.cachedAirports.get(selectedIcao);
		}
	}

	HashSet<String> assignmentAircraftList = new HashSet<>();
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css'>
	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css'>
	<link rel='stylesheet prefetch' href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
	<link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css"/>
	<link rel="stylesheet" type="text/css" href="css/tablesorter-style.css"/>
	<link rel="stylesheet" type="text/css" href="css/Master.css"/>

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
	<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
	<script src="http://maps.googleapis.com/maps/api/js?libraries=visualization&sensor=false"></script>

	<script type='text/javascript' src="scripts/jquery.cookie.js"></script>
	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
	<script src="scripts/AutoComplete.js"></script>

	<script src="scripts/js/highcharts.js"> </script>
	<script src="scripts/PopupWindow.js"></script>

	<script type="text/javascript">

		function makeChart ( )
		{
			var months =
			{
				'1': 'Jan',
				'2': 'Feb',
				'3': 'Mar',
				'4': 'Apr',
				'5': 'May',
				'6': 'Jun',
				'7': 'Jul',
				'8': 'Aug',
				'9': 'Sep',
				'10': 'Oct',
				'11': 'Nov',
				'12': 'Dec'
			};

			// Manipulate returned value to be JavaScript friendly
			//data
			data = <%=Airports.getAirportOperationDataJSON(airport != null ? airport.getIcao() : "") %>;

			//sample data
			//var data = [{"year":2011,"month":8,"icao":"NSTU","ops":0},{"year":2011,"month":7,"icao":"NSTU","ops":0},{"year":2011,"month":6,"icao":"NSTU","ops":0},{"year":2011,"month":5,"icao":"NSTU","ops":0},{"year":2011,"month":4,"icao":"NSTU","ops":14},{"year":2011,"month":3,"icao":"NSTU","ops":13},{"year":2011,"month":2,"icao":"NSTU","ops":12},{"year":2011,"month":1,"icao":"NSTU","ops":10},{"year":2010,"month":12,"icao":"NSTU","ops":0},{"year":2010,"month":11,"icao":"NSTU","ops":0},{"year":2010,"month":10,"icao":"NSTU","ops":0},{"year":2010,"month":9,"icao":"NSTU","ops":0},{"year":2010,"month":8,"icao":"NSTU","ops":0},{"year":2010,"month":7,"icao":"NSTU","ops":0},{"year":2010,"month":6,"icao":"NSTU","ops":0},{"year":2010,"month":5,"icao":"NSTU","ops":0},{"year":2010,"month":4,"icao":"NSTU","ops":0},{"year":2010,"month":3,"icao":"NSTU","ops":0},{"year":2010,"month":2,"icao":"NSTU","ops":0},{"year":2010,"month":1,"icao":"NSTU","ops":0},{"year":2010,"month":12,"icao":"NSTU","ops":0},{"year":2010,"month":11,"icao":"NSTU","ops":0},{"year":2010,"month":10,"icao":"NSTU","ops":0},{"year":2010,"month":9,"icao":"NSTU","ops":0},{"year":2010,"month":8,"icao":"NSTU","ops":0}]

			// Truncate array to only 12 elements - remove the first entry in the array
			data.shift();
			if (data.length > 12)
			{
				data.length = 12;
			}
			data.reverse();

			var xAxisCategories = [ ],
					dataToDisplay = [ ];

			for (var i = 0, n; n = data[i++];)
			{
				var month = months[String(n['month'])],
						range = n['ops'];

				xAxisCategories.push(month);
				dataToDisplay.push(range);
			}

			var chart = new Highcharts.Chart({
				chart: {
					renderTo: 'chart-container',
					type: 'line',
					width: 580,
					height: 440
				},
				xAxis: {

					categories: xAxisCategories,
					title: {
						enabled: true,
						text: '<b>Months</b>',
						style: {
							fontWeight: 'normal'
						}
					}
				},
				yAxis: {
					title: {
						enabled: true,
						text: '<b>Operations</b>',
						style: {
							fontWeight: 'normal'
						}
					}
				},
				legend: {
					layout: 'vertical',
					floating: true,
					backgroundColor: '#FFFFFF',
					align: 'right',
					verticalAlign: 'top',
					y: 50,
					x: 0
				},
				tooltip: {
					formatter: function() {
						return '<b>' + this.series.name + '</b><br/>' + this.x + ': ' + this.y;
					}
				},
				series: [{
					name: '<%= airport != null ? airport.getIcao() : "" %>',
					data: dataToDisplay}],
				title: {
					text: 'Aircraft Operations for last 12 months'
				}
			});
		}

		var gmap = new PopupWindow();

		function doSubmit(id)
		{
			var form = document.getElementById("airportForm");
			form.id.value = id;
			form.submit();
		}
		function doSubmit2(id, type)
		{
			var form = document.getElementById("aircraftForm");
			form.id.value = id;
			form.rentalType.value = type;
			form.submit();
		}
		function doSubmit3(id, id2)
		{
			var form = document.getElementById("airportForm");
			form.id.value = id;
			if (id2 != 0)
			{
				form.groupid.value = id2;
				form.type.value = "move";
			}
			form.submit();
		}
		function showInactive()
		{
			var table = document.getElementById("fboInfo");
			var rows = table.tBodies[0].rows;
			for (var i = 0; i < rows.length; i++)
			{
				if (rows[i].className == "InactiveFBO")
				{
					if (rows[i].style.display == "none")
						rows[i].style.display = ""; //table-row
					else
						rows[i].style.display = "none";
				}
			}
		}
		function checkAll()
		{
			var field = document.getElementById("airportForm").select;
			for (i = 0; i < field.length; i++)
				field[i].checked = true;

			field.checked = true; //needed in case of only one row
		}

		function checkNone()
		{
			var field = document.getElementById("airportForm").select;
			for (var i = 0; i < field.length; i++)
				field[i].checked = false;

			field.checked = false; // needed in case of only one row
		}

		function formValidation(thisForm)
		{
			if (thisForm.dest.value == null || thisForm.dest.value =="")
			{
				window.alert("Please enter the ICAO of the destination airport");
				thisForm.dest.focus();
				return false;
			}
		}
	</script>

	<script type="text/javascript">
		var lastChecked = null;

		$(document).ready(function() {
			var $chkboxes = $('.chkbox');
			$chkboxes.click(function(e) {
				$chkboxes = $('.chkbox');
				if(!lastChecked) {
					lastChecked = this;
					return;
				}

				if(e.shiftKey) {
					var start = $chkboxes.index(this);
					var end = $chkboxes.index(lastChecked);

					$chkboxes.slice(Math.min(start,end), Math.max(start,end)+ 1).prop('checked', lastChecked.checked);

				}

				lastChecked = this;
			});
		});
	</script>

	<script type="text/javascript">
		$(function()
		{
			$.extend($.tablesorter.defaults,
					{
						widthFixed: false,
						widgets : ['zebra','columns']
					});

			$('.assigmentTable').tablesorter();

			$.extend($.tablesorter.defaults,
					{
						widthFixed: false,
						widgets : ['zebra','columns']
					});

			$('.aircraftTable').tablesorter();

			$.extend($.tablesorter.defaults,
					{
						widthFixed: false,
						widgets : ['zebra','columns']
					});

			$('.goodsearchTable').tablesorter()

			$('.airportOps').click(function () {
				$("#airportOpsModal").modal('show');
			});

			makeChart();

		});

	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<%
			if(airport != null)
			{
				session.setAttribute("requestedAirport", airport.getIcao());

				boolean aircraftArea = "1".equals(request.getParameter("aircraftArea"));
				boolean airportArea = "1".equals(request.getParameter("airportArea"));
				double fuelPrice = airport.getPrice100ll();
				double jetaPrice = airport.getPriceJetA();
				Groups.groupMemberData[] staffGroups = user.getStaffGroups();

				//make the lat and long human readable.
				//ABS() negative values and append the correct quadrant information.
				if (airport.getLatLon().lat >= 0)
				{
					latNS = airport.getLatLon().lat + "N";
				}
				else
				{
					latNS = airport.getLatLon().lat*-1 + "S";
				}
				if (airport.getLatLon().lon >= 0)
				{
					lonEW = airport.getLatLon().lon + "E";
				}
				else
				{
					lonEW = airport.getLatLon().lon*-1 + "W";
				}

				int elev = airport.getElev();
				String surfType = airport.getSurfaceTypeName();

		%>
		<div class="infoBlock">
			<table>
				<thead>
				<tr>
					<th colspan="3">Close airports</th>
				</tr>
				</thead>
				<tbody>
				<%
					List<CloseAirport> closestAirports = Airports.fillCloseAirports(airport.getIcao(), 0, 50);
					if(closestAirports.size() > 0)
					{
						for (CloseAirport ca : closestAirports)
						{
							String icao = ca.icao;
							int value = (int)Math.round(ca.distance);
							String URL = "airport.jsp?icao=" + icao;
							String image = Airports.getBearingImageURL(ca.bearing);
				%>
				<tr>
					<td><a href="<%= response.encodeURL(URL) %>"><%= icao %></a></td>
					<td><%= value %> nm</td>
					<td><img src="<%= image %>" /></td>
				</tr>
				<%
						}
					}
				%>
				</tbody>
			</table>
		</div>
		<%
			int bound = 7;
			int imagex = 150, imagey = 150;
			//int centerx = imagex/2-5;
			//int centery = imagey/2-5;
			//double lat = airport.getLat();
			//double lon = airport.getLon();
			double lat1 = airport.getLatLon().lat - bound;
			double lat2 = airport.getLatLon().lat + bound;
			double lon1 = airport.getLatLon().lon - bound;
			double lon2 = airport.getLatLon().lon + bound;
			String box = lon1 + "," + lat1 + "," + lon2 + "," + lat2;
			List<FboBean> fbos = Fbos.getFboByLocation(airport.getIcao());
			List<FboBean> fboinactive = Fbos.getInactiveFboByLocation(airport.getIcao());
		%>
		<div class="airportInfo">
			<table>
				<tr valign="top">
					<td valign="top">
						<table>
							<tr>
								<td>
									<h1><%=selectedIcao.toUpperCase()%></h1>
								</td>
								<td>
									<a href="#" onclick="gmap.setSize(620,520);gmap.setUrl('gmap.jsp?icao=<%= airport.getIcao() %>');gmap.showPopup('gmap');return false;" id="gmap">
										<img style="border-style: none;" title="<%= AirportBean.getTypeDescription(airport.getType(), airport.getSize()) %>" src="<%= Airports.getDescriptiveImage(airport, fbos.size() > 0) %>">
									</a>
								</td>
								<td>
									<a href="http://www.fscharts.com/?action=search&type=icao&term=<%= airport.getIcao() %>" onclick="this.target='_blank'">
										<img style="border-style: none;" src="img/fscharts.gif">
									</a>
								</td>
							</tr>
						</table>
						<div><%= airport.getTitle() %></div>
						<%
							int[] aopm = Airports.getAirportOperationsPerMonth(airport.getIcao());
							int currentops = aopm[0];
							int averageops = aopm[1];
						%>
						<div>Lat: <%= latNS %>, Long: <%= lonEW %></div>
						<div>Elev: <%= elev %>, Surface: <%= surfType %></div>
						<div class="airportOps btn btn-link">Aircraft Operations - Avg: <%=averageops%>, Current Month: <%=currentops%></div>

						<table id="fboInfo">
							<thead>
							<tr>
								<td><b>FBO</b></td>
								<td><b>100LL Fuel</b></td>
								<td><b>Gallons</b></td>
								<td><b>Jet-A Fuel</b></td>
								<td><b>Gallons</b></td>
								<td><b>Repair/Avionics</b></td>
							</tr>
							</thead>
							<tbody>
							<%
								String fuel = Formatters.currency.format(fuelPrice);
								String jeta = Formatters.currency.format(jetaPrice);

								boolean hasRepairShop = airport.getSize() >= AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE;
								if (airport.has100ll() || fbos.size() > 0 || hasRepairShop || fboinactive.size() > 0)
								{
									if (airport.has100ll() || hasRepairShop)
									{
							%>
							<tr>
								<td>Local market</td>
								<td><%= airport.has100ll() ? fuel : "&nbsp;" %></td>
								<td><%= airport.has100ll() ? "unlimited" : "&nbsp;" %></td>
								<td><%= airport.hasJetA() ? jeta : "&nbsp;" %></td>
								<td><%= airport.hasJetA() ? "unlimited" : "&nbsp;" %></td>
								<td><%= hasRepairShop ? "20% / 50%" : "" %></td>
							</tr>
							<%
								}

								for (FboBean aFbo : fbos)
								{
									GoodsBean fuelleft = Goods.getGoods(airport.getIcao(), aFbo.getOwner(), GoodsBean.GOODS_FUEL100LL);
									GoodsBean jetaleft = Goods.getGoods(airport.getIcao(), aFbo.getOwner(), GoodsBean.GOODS_FUELJETA);
									fuel = Formatters.currency.format(aFbo.getFuel100LL());
									jeta = Formatters.currency.format(aFbo.getFueljeta());
									UserBean fboowner = Accounts.getAccountById(aFbo.getOwner());
									String fboname = aFbo.getName();
									boolean UserIsFBOStaff = aFbo.updateAllowed(user);

									if (UserIsFBOStaff)
										fuel = "<a title=\"Edit FBO\" href=\"" + response.encodeURL("editfbo.jsp?id=" + aFbo.getId()) + "\">" + fuel + "</a>";

									if (UserIsFBOStaff || aFbo.logsVisibleToAll())
										fboname = "<a title=\"View FBO Log\" href=\"" + response.encodeURL("fbolog.jsp?id=" + aFbo.getId()) + "\">" + fboname + "</a>";

									if (aFbo.isForSale())
										fboname = fboname + "<img src=\"img/sale.gif\" border=\"0\" title=\"" + Formatters.currency.format(aFbo.getPrice()) + (aFbo.getPriceIncludesGoods() ? " + goods" : "") + "\">";

									if (!aFbo.getName().equalsIgnoreCase(fboowner.getName()))
										fboname = fboname + "<br><span class=\"small\"><i>" + fboowner.getName() + "</i></span>";

									if (UserIsFBOStaff)
									{
										GoodsBean suppliesleft = Goods.getGoods(airport.getIcao(), aFbo.getOwner(), GoodsBean.GOODS_SUPPLIES);
										int DaysSupplied = suppliesleft != null ? suppliesleft.getAmount() / aFbo.getSuppliesPerDay(Airports.getTotalFboSlots(aFbo.getLocation())) : 0;

										if (DaysSupplied > 14)
											fboname = fboname + "<br><span class=\"small\">" + DaysSupplied + " days supplies</span>";
										else
											fboname = fboname + "<br><span style=\"color: red;\" class=\"small\">" + DaysSupplied + " days supplies</span>";
									}
							%>
							<tr>
								<td><%= fboname %></td>
								<td><%= fuel %></td>
								<td>
									<%= (int) Math.floor(fuelleft != null ? fuelleft.getAmount() / Constants.GALLONS_TO_KG : 0) %>
								</td>
								<td><%= jeta %></td>
								<td>
									<%= (int) Math.floor(jetaleft != null ? jetaleft.getAmount() / Constants.GALLONS_TO_KG : 0) %>
								</td>
								<td>
									<%= (aFbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0 ? aFbo.getSServices() : "" %>
								</td>
							</tr>
							<%
								}
								if (fboinactive.size() > 0)
								{
									String inactiveStyle = ""; // " style=\"display: table-row;\" ";
									if (fbos.size() > 0)
									{
										String inactiveForSaleFlag = "";
										inactiveStyle = " style=\"display: none;\" ";
										for (FboBean aFboinactive : fboinactive)
										{
											if (aFboinactive.isForSale())
											{
												inactiveForSaleFlag = "<img src=\"img/sale.gif\" border=\"0\" title=\"For Sale\">";
												break;
											}
										}
							%>
							<tr>
								<td class="disabledtext" colspan="4">
									<a href="javascript:showInactive()"><%= fboinactive.size() %> closed <%= (fboinactive.size() > 1 ? "FBOs" : "FBO") + inactiveForSaleFlag %></a>
								</td>
							</tr>
							<%
								}

								for (FboBean aFboinactive : fboinactive)
								{
									GoodsBean fuelleft = Goods.getGoods(airport.getIcao(), aFboinactive.getOwner(), GoodsBean.GOODS_FUEL100LL);
									GoodsBean jetaleft = Goods.getGoods(airport.getIcao(), aFboinactive.getOwner(), GoodsBean.GOODS_FUELJETA);
									fuel = "Closed";
									jeta = "";
									UserBean fboowner = Accounts.getAccountById(aFboinactive.getOwner());
									String fboname = aFboinactive.getName();
									boolean UserIsFBOStaff = aFboinactive.updateAllowed(user);

									if (UserIsFBOStaff)
										fuel = "<a title=\"Edit FBO\" href=\"" + response.encodeURL("editfbo.jsp?id=" + aFboinactive.getId()) + "\">" + fuel + "</a>";

									if (UserIsFBOStaff || aFboinactive.logsVisibleToAll())
										fboname = "<a title=\"View FBO Log\" href=\"" + response.encodeURL("fbolog.jsp?id=" + aFboinactive.getId()) + "\">" + fboname + "</a>";

									if (aFboinactive.isForSale())
										fboname = fboname + "<img src=\"img/sale.gif\" border=\"0\" title=\"" + Formatters.currency.format(aFboinactive.getPrice()) + (aFboinactive.getPriceIncludesGoods() ? " + goods" : "") + "\">";

									if (!aFboinactive.getName().equalsIgnoreCase(fboowner.getName()))
										fboname = fboname + "<br><span class=\"small\"><i>" + fboowner.getName() + "</i></span>";
							%>
							<tr class="InactiveFBO" <%= inactiveStyle %>>
								<td class="disabledtext"><%= fboname %></td>
								<td class="disabledtext"><%= fuel %></td>
								<td class="disabledtext">
									<%= (int) Math.floor(fuelleft != null ? fuelleft.getAmount() / Constants.GALLONS_TO_KG : 0) %>
								</td>
								<td class="disabledtext"><%= jeta %></td>
								<td class="disabledtext">
									<%= (int) Math.floor(jetaleft != null ? jetaleft.getAmount() / Constants.GALLONS_TO_KG : 0) %>
								</td>
								<td class="disabledtext">&nbsp;</td>
							</tr>
							<%
										}
									}
								}
							%>
							<tr>
								<td></td>
							</tr>
							</tbody>
						</table>
					</td>
					<td valign="top">
						<!-- START MAP SECTION -->
						<div style="position: relative">
							<!-- commented out by gurka -->
							<!--   div id="indicator" style="position: absolute; top:0px; left:0px; visibility:hidden"><img style="position:absolute; top:0px; left:0px" src="img/bullet.gif" /></div -->
							<!-- <img src="http://www2.demis.nl/wms/wms.asp?wms=WorldMap&amp;Version=1.1.0&amp;Format=image/gif&amp;Request=GetMap&amp;BBox=<%= box %>&amp;SRS=EPSG:4326&amp;Width=<%= imagex %>&amp;Height=<%= imagey %>&amp;Layers=Bathymetry,Countries,Topography,Hillshading,Builtup+areas,Coastlines,Waterbodies,Rivers,Railroads,Highways,Roads,Borders" /> -->

							<!-- New Google Maps API Code by greggerm -->
							<img src="https://maps.googleapis.com/maps/api/staticmap?&zoom=6&size=230x150&maptype=terrain&markers=size:small|color:blue|<%= airport.getLatLon().lat %>,<%= airport.getLatLon().lon %>|&sensor=false" /><br/>
						</div>
						<!-- END MAP SECTION -->
						<%
							List<FboFacilityBean> carriers = Facilities.getFacilitiesForAirport(airport.getIcao());
							if (carriers.size() > 0)
							{
								String airline;
								boolean spaceAvailable = false;
								Set<String> carrierSet = new HashSet<>();
								for (FboFacilityBean carrier : carriers)
								{
									if (!"".equals(carrier.getName().trim()))
										carrierSet.add(carrier.getName());

									if (carrier.getIsDefault())
									{
										FboBean parentFbo = Fbos.getFbo(carrier.getFboId());

										if (Facilities.calcFacilitySpaceAvailable(carrier, parentFbo) > 0)
											spaceAvailable = true;
									}
								}

								String[] carrierNames = carrierSet.toArray(new String[carrierSet.size()]);
						%>
						<div>
							<b>Local Carriers:</b><br>
								<%
			for (int i = 0; i < carrierNames.length; i++)
			{
				airline = carrierNames[i].replaceAll(" ", "&nbsp;");
%>
								<%= airline %><%= (i < carrierNames.length - 1 ? ", " : "") %>
								<%
			}
			if (spaceAvailable)
			{
%>
							<br><br>
							<a href="fbofacilityrent.jsp?icao=<%= airport.getIcao() %>">Space available</a>
								<%
            }
%>
					</td>
				</tr>
				<%
					}

					int slotsAvailable = Fbos.getAirportFboSlotsAvailable(airport.getIcao());
					String realEstate;

					if (slotsAvailable == 1)
					{
						realEstate = "There is " + slotsAvailable + " lot remaining for FBO construction at this airport.";
					}
					else if (slotsAvailable > 1)
					{
						realEstate = "There are " + slotsAvailable + " lots remaining for FBO construction at this airport.";
					}
					else
					{
						realEstate = "There is no room for FBO construction at this airport.";
					}
				%>
				<tr>
					<td><span class="small"><i><%= realEstate %></i></span></td>
				</tr>
				<tr>
					<td>
						<table>
							<tr>
								<td>
									<form id="distanceForm" method="post" action="gmapdistance.jsp" onsubmit="return formValidation(this)">
										<div>
											<input name="depart" type="hidden" value="<%=airport.getIcao()%>" />
											<strong>Distance to:</strong>
											<input name="dest" type="text" class="textarea" size="4" maxlength="4" /> &nbsp;
											<input id="submit" type="submit" class="button" value="Get Map" />
										</div>
									</form>
								</td>
								<td>
									<form method="post" action="airport.jsp">
										<div>
											<strong>Go to:</strong>
											<input name="icao" type="text" class="textarea" size="4"/> &nbsp;
											<input name="submit" type="hidden" value="true" />
											<input type="submit" class="button" value="Go" />
										</div>
									</form>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td class="footer">View detailed maps by clicking on any Airport Icon.<br/>Sort columns by clicking the header, Shift-Click to sort Multiple columns. Control-click to reset.</td>
				</tr>
			</table>
		</div>
		<%
			List<AircraftBean> aircraftList;
			List<AssignmentBean> assignments;
			List<GoodsBean> goods = Goods.getGoodsAtAirport(airport.getIcao(), airport.getSize(), fuelPrice, jetaPrice);

			AssignmentBean capableAssignment = null;

			if (airportArea)
			{
				if (toAirport)
				{
					assignments = Assignments.getAssignmentsToArea(airport.getIcao(),
							Airports.closeAirportsWithAssignments(airport.getIcao(), false), minPax, maxPax, minKG, maxKG);
				}
				else
				{
					assignments = Assignments.getAssignmentsInArea(airport.getIcao(),
							Airports.closeAirportsWithAssignments(airport.getIcao(), true), minPax, maxPax, minKG, maxKG);
				}
			}
			else if (toAirport)
			{
				assignments = Assignments.getAssignmentsToAirport(airport.getIcao(), minPax, maxPax, minKG, maxKG);
			}
			else
			{
				assignments = Assignments.getAssignments(airport.getIcao(), minPax, maxPax, minKG, maxKG);
			}

			if (aircraftArea)
				aircraftList = Aircraft.getAircraftInArea(airport.getIcao(), closestAirports);
			else
				aircraftList = Aircraft.getAircraft(airport.getIcao());

			String URL = returnPage;
			String sToAirport = "&toAirport=" + (toAirport?1:0);
			String sAirportArea = "&airportArea=" + (airportArea?1:0);
			String sAircraftArea = "&aircraftArea=" + (aircraftArea?1:0);

			String captionToFrom = "<a href=\"" + response.encodeURL(URL + "&toAirport=" + (toAirport ? "0" : "1") + sAirportArea + sAircraftArea) + "\">" + (toAirport ? "to" : "from") + "</a>";
			String captionAirportArea = "<a href=\"" + response.encodeURL(URL + sToAirport + "&airportArea=" + (airportArea ? "0" : "1") + sAircraftArea) + "\">" + (airportArea ? "airports near " + airport.getName() : airport.getName()) + "</a>";
		%>
		<form method="post" action="userctl" id="airportForm">
			<div>
				<input type="hidden" name="event" value="Assignment"/>
				<input type="hidden" name="type" value="add"/>
				<input type="hidden" name="id" />
				<input type="hidden" name="groupid" />
				<input type="hidden" name="returnpage" value="<%= URL + sToAirport + sAirportArea + sAircraftArea %>" />
			</div>

			<table class="assigmentTable tablesorter-default tablesorter">
				<caption>Assignments&nbsp;<%= captionToFrom %>&nbsp;<%= captionAirportArea %></caption>
				<thead>
				<tr>
					<th class="sorter-checkbox" style="width: 35px;">Add</th>
					<th class="numeric" style="width: 75px;">Pay</th>
					<th style="width: 75px;">From</th>
					<th style="width: 75px;">Dest</th>
					<th style="width: 35px;">NM</th>
					<th style="width: 35px;">Brg</th>
					<th>Cargo</th>
					<th style="width: 35px;">Type</th>
					<th style="width: 75px;">Aircraft</th>
					<th class="sorter-timeExpire"  style="width: 80px;">Expires</th>
					<th class="sorter-false">Action</th>
				</tr>
				</thead>
				<tbody>
				<%		if (assignments.size() == 0)
				{
				%> 			    <tr><td colspan="10">No assignments</td></tr>
				<tr><td colspan="10">&nbsp;</td></tr>
				<%		}
				else
				{
					int counter = 0;
					for (AssignmentBean assignment : assignments)
					{
						counter++;
						int id = assignment.getId();

						if (id == capable)
							capableAssignment = assignment;

						String aircraftReg = Aircraft.getAircraftRegistrationById(assignment.getAircraftId());

						if (aircraftReg != null)
							assignmentAircraftList.add(aircraftReg);

						String image = "img/set2_" + assignment.getActualBearingImage() + ".gif";
						CachedAirportBean destination = assignment.getDestinationAirport();
						CachedAirportBean location = assignment.getLocationAirport();

						String ap = airport.getIcao();
						String locap = location.getIcao();
						String desap = destination.getIcao();
				%>
				<tr>
					<td>
						<input class="chkbox" type="checkbox" id="mycheckbox<%=counter%>" name="select" value="<%= id %>"/>
					</td>
					<td class="numeric"><%= Formatters.currency.format(assignment.calcPay()) %></td>
					<% 				if (ap.equals(locap))
					{
					%>					<td class="nowrap">
					<img src="img/blankap.gif" style="vertical-align:middle;" />
					<a title="<%= location.getTitle() %>" class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getLocation()) %>">
						<%= assignment.getLocation() %>
					</a>
				</td>
					<%				}
					else
					{
						if(!toAirport)
						{
					%>					<td class="nowrap">
					<a href="#" onclick="gmap.setSize(620,530);gmap.setUrl('<%= response.encodeURL("gmap.jsp?icao=" + location.getIcao()+"&icaod="+ airport.getIcao()) %>');gmap.showPopup('gmap');return false;" id="gmap">
						<img src="<%= Airports.getDescriptiveImage(location, Fbos.getAirportFboSlotsInUse(location.getIcao()) > 0) %>" style="border-style: none; vertical-align:middle;" />
					</a>
					<a title="<%= location.getTitle() %>" class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getLocation()) %>">
						<%= assignment.getLocation() %>
					</a>
				</td>
					<%					}
					else
					{
					%>					<td class="nowrap">
					<a href="#" onclick="gmap.setSize(620,530);gmap.setUrl('<%= response.encodeURL("gmap.jsp?icao=" + location.getIcao()+"&icaod="+ destination.getIcao()) %>');gmap.showPopup('gmap');return false;" id="gmap">
						<img src="<%= Airports.getDescriptiveImage(location, Fbos.getAirportFboSlotsInUse(location.getIcao()) > 0) %>" style="border-style: none; vertical-align:middle;" />
					</a>
					<a title="<%= location.getTitle() %>" class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getLocation()) %>">
						<%= assignment.getLocation() %>
					</a>
				</td>
					<%					}
					}

						if (ap.equals(desap))
						{
					%>					<td class="nowrap">
					<img src="img/blankap.gif" style="vertical-align:middle;" />
					<a class="normal" title="<%= destination.getTitle() %>" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getTo()) %>">
						<%= assignment.getTo() %>
					</a>
				</td>
					<%				}
					else
					{
					%>					<td class="nowrap">
					<a href="#" onclick="gmap.setSize(620,530);gmap.setUrl('<%= response.encodeURL("gmap.jsp?icao=" + location.getIcao()+"&icaod="+ destination.getIcao()) %>');gmap.showPopup('gmap');return false;" id="gmap">
						<img src="<%= Airports.getDescriptiveImage(destination, Fbos.getAirportFboSlotsInUse(destination.getIcao()) > 0) %>" style="border-style: none; vertical-align:middle;" />
					</a>
					<a class="normal" title="<%= destination.getTitle() %>" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getTo()) %>">
						<%= assignment.getTo() %>
					</a>
				</td>
					<%				}
					%>
					<td class="numeric"><%= assignment.getActualDistance() %></td>
					<td class="numeric"><%= assignment.getActualBearing() %><img src="<%= image %>" /></td>
					<td><%= assignment.getSCargo() %></td>
					<!-- AllIn Change -->
					<%
						String type = "";
						if(assignment.getType() == AssignmentBean.TYPE_ALLIN )
						{
							type = "A";
							if (assignment.isDirect())
								type += "&rarr;";
						}
						else
							type = "T";
					%>
					<td id="assignmentType-<%= id %>"><%= type %></td>
					<td class="nowrap"><%= aircraftReg == null ? "[N/A]" : aircraftReg %></td>
					<td class="nowrap">
						<%=assignment.isNoExt() ?"<i class=\"fa fa-bolt\" style=\"font-size: 14px;color: red\"></i>" : "" %>
						<%=assignment.isExtended() ?"<i class=\"fa fa-clock-o\" style=\"font-size: 14px;\"></i>" : "" %>
						<%= assignment.getSExpires() %>
					</td>
					<td>
						<a class="link" title="Show aircraft that can handle this assignment" href="<%= response.encodeURL(URL + sAirportArea + sToAirport + "&aircraftArea=1&capable=" + id) %>">Aircraft</a>
					</td>
				</tr>
				<%
						}
					}
				%>
				</tbody>
			</table>
			<div>
				<a class="link" href="javascript:checkAll()">Select All</a>  |
				<a class="link" href="javascript:checkNone()">De-Select</a>
				<input type="button" id="addSelectedButton" name="add_Selected" value="Add Selected Assignments To ->" />
				<select name="addToGroup" id="addToGroup" class="formselect">
					<option class="formselect" value="0">My Flight</option>
					<%
						for (Groups.groupMemberData staffGroup : staffGroups)
						{
					%>
					<option class="formselect" value="<%= staffGroup.groupId%>"><%= staffGroup.groupName%>
					</option>
					<%
						}
					%>			</select>
				<br>
				<input type="hidden" id="jsCountHolder" value="0" />
				<input type="hidden" id="jsCountHolder2" value="0" />
				<input type="hidden" id="whocares" value="0" />
			</div>
		</form>

		<script type="text/javascript">
			$("#addSelectedButton").click(function(e)
			{
				e.preventDefault();

				var h = 0;
				var h2 = 0;

				$.each($(".assigmentTable").find(":checkbox"), function()
				{
					if($(this).is(":checked"))
					{
						h2++;
						var t = $("#assignmentType-" + $(this).val()).html();
						if(t.indexOf('A') == 0)
							h++;
					}
				});

				if(h > 0 && h2 == h && $("#addToGroup").val() == 0)
				{
					//the amount of all-in jobs matches the total number of selected jobs
					if(h == 1)
						doSubmit3(document.getElementById("airportForm").select, $("#addToGroup").val());
					else
						alert('FSE Validation Error: cannot have more then 1 All-In job selected.');
				}
				else if(h == 0 )
				{
					doSubmit3(document.getElementById("airportForm").select,$("#addToGroup").val());
				}
				else
				{
					if($("#addToGroup").val() != 0)
						alert('FSE Validation Error: Cannot add All-In jobs to a group!');
					else
						alert('FSE Validation Error: Cannot have more then 1 All-In job selected and cannot mix All-In jobs with regular jobs');
				}
			});
		</script>

		<ul class="footer">
			<li>Pay is what the assignments pays you, excluding rental and flying cost.</li>
			<li>Types: A = All In, T = Trip only</li>
			<li>All In flights include all bills for aircraft/fuel/landing fees.</li>
		</ul>
		<form method="get" action="airport.jsp">
			<div>
				<input type="hidden" name="icao" value="<%= airport.getIcao() %>"/>
				<input type="hidden" name="aircraftArea" value="<%= (aircraftArea ? 1 : 0) %>"/>
			</div>
			<table>
				<tr>
					<td>Show assignments:</td>
					<td>
						<select class="formselect" name="toAirport">
							<option value="0"<%= (!toAirport ? " selected " : "") %>>from</option>
							<option value="1"<%= (toAirport ? " selected " : "") %>>to</option>
						</select>
						<select class="formselect" name="airportArea">
							<option value="0"<%= (!airportArea ? " selected " : "") %>>airport</option>
							<option value="1"<%= (airportArea ? " selected " : "") %>>the area</option>
						</select>
						<input type="submit" class="button" name="filter" value="Go" />
					</td>
				</tr>
				<tr>
					<td>Passengers:</td>
					<td>
						<select class="formselect" name="PaxFilter">
							<option value="0"<%= (PaxFilter == 0 ? " selected " : "") %>> </option>
							<option value="1"<%= (PaxFilter == 1 ? " selected " : "") %>>At least</option>
							<option value="2"<%= (PaxFilter == 2 ? " selected " : "") %>>At most</option>
						</select>
						<select class="formselect" name="PaxFilterQty">
							<option value="-1"<%= (PaxFilterQty == -1 ? " selected " : "") %>> </option>
							<%
								for (int i = 0; i <= 20; i++)
								{
							%>
							<option value="<%= i %>"<%= (PaxFilterQty == i ? " selected " : "") %>><%= i %> passengers</option>
							<%
								}
							%>		            </select>
					</td>
				</tr>
				<tr>
					<td>Cargo:</td>
					<td>
						<select class="formselect" name="KGFilter">
							<option value="0"<%= (KGFilter == 0 ? " selected " : "") %>> </option>
							<option value="1"<%= (KGFilter == 1 ? " selected " : "") %>>At least</option>
							<option value="2"<%= (KGFilter == 2 ? " selected " : "") %>>At most</option>
						</select>
						<select class="formselect" name="KGFilterQty">
							<option value="-1"<%= (KGFilterQty == -1 ? " selected " : "") %>> </option>
							<%
								for (int i = 0; i <= 500; i += 100)
								{
							%>
							<option value="<%= i %>"<%= (KGFilterQty == i ? " selected " : "") %>><%= i %> kg</option>
							<%
								}
								for (int i = 750; i <= 2000; i += 250)
								{
							%>
							<option value="<%= i %>"<%= (KGFilterQty == i ? " selected " : "") %>><%= i %> kg</option>
							<%
								}
								for (int i = 3000; i <= 10000; i += 1000)
								{
							%>
							<option value="<%= i %>"<%= (KGFilterQty == i ? " selected " : "") %>><%= i %> kg</option>
							<%		}
							%>		            </select>
					</td>
				</tr>
			</table>
		</form>
		<%
			if (aircraftList.size() == 0)
			{
		%>
		<div class="message">No aircraft to rent</div>
		<a class="link" href="<%= response.encodeURL(URL + sAirportArea + sToAirport + "&aircraftArea=1") %>">[Look in the area]</a>
		<%		}
		else
		{
		%>
		<form method="post" action="userctl" id="aircraftForm">
			<div>
				<input type="hidden" name="event" value="Aircraft"/>
				<input type="hidden" name="type" value="add"/>
				<input type="hidden" name="rentalType" />
				<input type="hidden" name="id" />
				<input type="hidden" name="reg" />
				<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
			</div>

			<table class="aircraftTable tablesorter-default tablesorter">
				<caption>Aircraft<%= capableAssignment == null ? "" : (" that can carry " + capableAssignment.getSCargo())%></caption>
				<thead>
				<tr>
					<th>Id</th>
					<th>Type</th>
					<th>Equipment</th>
					<%
						if (aircraftArea)
						{
					%>
					<th>Location</th>
					<%
						}
					%>
					<th>Home</th>
					<th class="numeric">Rental Price</th>
					<th class="numeric">Bonus</th>
					<th>Action</th>
				</tr>
				</thead>
				<tbody>

				<%
					for (AircraftBean aircraft : aircraftList)
					{
						if (aircraft.getShippingState() != 0 ||
								(capableAssignment != null && !aircraft.fitsAboard(capableAssignment)))
							continue;

						String reg2;
						String reg = aircraft.getRegistration();
						String saleprice = Formatters.currency.format(aircraft.getSellPrice());
						int owner=aircraft.getOwner();
						int sellprice=aircraft.getSellPrice();

						if (owner != 0)
							reg2 = reg + "*";
						else
							reg2 = reg;

						int priceDry= aircraft.getRentalPriceDry();
						int priceWet= aircraft.getRentalPriceWet();
						int image = aircraft.getBearingImage();
						String sImage = "";

						if (image != -1)
							sImage = "<img src=\"img/set2_" + image + ".gif\">";

						String price = "";
						if (priceDry > 0)
							price = "$" + priceDry + " Dry";

						if (priceWet > 0)
							price = price + ((priceDry > 0) ? "/" : "") + "$" + priceWet + " Wet";

						price = price + (aircraft.getAccounting() == AircraftBean.ACC_TACHO ? " [Tacho]" : " [Hour]");
						boolean isInDebt = aircraft.getFeeOwed() > 0;
				%>
				<tr>
					<td>
						<a class="normal" href="<%= response.encodeURL("aircraftlog.jsp?id=" + aircraft.getId()) %>"><%= reg2 %></a>
						<%			if (sellprice !=0)
						{
						%>
						<img src="img/sale.gif" style="border-style: none;" title="<%=saleprice%>" />
						<%
							}
							if (aircraft.isBroken())
							{
								if (aircraft.isAllowRepair())
								{
						%>
                    <span style="background: green">
                        <img src="img/repair.gif" style="border-style: none; vertical-align:middle;" />
                    </span>
						<%
						}
						else
						{
						%>
						<img src="img/repair.gif" style="border-style: none; vertical-align:middle;" />
						<%
								}
							}
						%>
					</td>
					<td><%= aircraft.getMakeModel() %></td>
					<td><%= aircraft.getSEquipment() %></td>
					<% 			if (aircraftArea)
					{
						if (aircraft.getLocation() == null)
						{
					%>
					<td><%= aircraft.getSLocation() %></td>
					<%
					}
					else
					{
					%>
					<td><a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + aircraft.getLocation()) %>"><%= aircraft.getSLocation() %></a></td>
					<%
							}
						}
					%>
					<td><a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + aircraft.getHome()) %>"><%= aircraft.getHome() %></a></td>
					<td class="numeric"><%= price %></td>
					<td class="numeric"><%= aircraft.isAdvertiseFerry() ? "(F) ":"" %><%= Formatters.currency.format(aircraft.getBonus()) %> <%= sImage %></td>

					<% //All-In check to see if reg # is assigned to a plane in the jobs list up top - disable the rent option
						if (!assignmentAircraftList.contains(aircraft.getRegistration()))
						{
							if (aircraft.getUserLock() > 0)
							{
					%>
					<td>[locked]</td>
					<%
					}
					else
					{
					%>
					<td>
						<%
							if (!isInDebt && priceDry > 0)
							{
						%>
						<a class="link" href="javascript:doSubmit2('<%= aircraft.getId() %>', 'dry')">Rent dry</a>
						<%					}
							if (!isInDebt && priceWet > 0)
							{
						%>
						<%= priceDry > 0 ? " | " : "" %><a class="link" href="javascript:doSubmit2('<%= aircraft.getId() %>', 'wet')">Rent wet</a>
						<%
							}
							if (!isInDebt && (priceDry + priceWet == 0) && aircraft.canAlwaysRent(user))
							{
						%>
						<a class="link" href="javascript:doSubmit2('<%= aircraft.getId() %>', 'wet')">Rent</a>
						<%
							}
						%>
					</td>
					<%
						}
					}
					else
					{
					%>
					<td>All-In Reserved</td>
					<%
							}
						}
					%>
				</tr>
				</tbody>
			</table>
		</form>
		<ul class="footer">
			<li>An * after the registration number means the plane is privately owned.</li>
			<li><img src="img/sale.gif" style="border-style: none; vertical-align:middle;"/> after the registration number means the plane is for sale.</li>
			<li>Dry rental price excludes fuel cost, you will be charged for fuel on arrival.</li>
			<li>Wet rental price includes fuel.</li>
			<li>[Hour] prices are per hour of engine running time.</li>
			<li>Bonus is the amount you pay/get per 100 miles you bring the aircraft away from/closer to its home.</li>
			<li>A bonus prefixed with (F) indicates an aircraft that is advertised for a ferry flight home. </li>
		</ul>
		<%
			if (!aircraftArea)
			{
		%>
		<a class="link" href="<%= response.encodeURL(URL + sAirportArea + sToAirport + "&aircraftArea=1") %>">[More in this area]</a>
		<%
				}
			}
		%>

		<%
			if (goods != null && goods.size() > 0)
			{
		%>
		<div class="dataTable">
			<table>
				<caption>Goods</caption>
				<thead>
				<tr>
					<th>Commodity</th>
					<th>Owner</th>
					<th>Buy</th>
					<th>Sell</th>
					<th>Amount available</th>
					<th>Max discount</th>
					<th>Sell limit</th>
					<th>Action</th>
				</tr>
				</thead>
				<tbody>
				<%
					for (GoodsBean good : goods)
					{
						//Ignore aircraft being shipped
						if (good.getType() == 99)
							continue;

						String buyPrice = good.isBuy() ? Formatters.currency.format(good.getPriceBuy()) : "-";
						String sellPrice = good.isSell() ? Formatters.currency.format(good.getPriceSell()) : "-";
						int amount = good.getAmountForSale();
						int max = good.getAmountAccepted();
						int discount = good.getMaxDiscount();
						String sAmount = amount == -1 ? "Unlimited" : ( amount + " Kg");
						String sLimit = max <0 ? "No limit" : (max + " Kg");
						String sDiscount = discount == 0 ? "-" : (discount + "% at " + good.getMaxDiscountAmount() + " Kg");
						String buyUrl = "buygoods.jsp?icao=" + airport.getIcao() + "&owner=" + good.getOwner() + "&type=" + good.getType();
				%>
				<tr>
					<td><%= good.getCommodity()%></td>
					<td><%= good.getOwnerName() %></td>
					<td class="numeric"><%= sellPrice %></td>
					<td class="numeric"><%= buyPrice %></td>
					<td class="numeric"><%= sAmount %></td>
					<td class="numeric"><%= sDiscount %></td>
					<td class="numeric"><%= sLimit %></td>
					<td>
						<a class="link" href="<%= response.encodeURL(buyUrl) %>">Buy</a>
						<%
							for (Groups.groupMemberData staffGroup : staffGroups)
							{
						%>
						| <a class="link" href="<%= response.encodeURL(buyUrl + "&groupid=" + staffGroup.groupId) %>">Buy
						for <%= staffGroup.groupName %>
					</a>
						<%
							}
						%>
					</td>
				</tr>
				<%
					}
				%>
				</tbody>
			</table>
			<%
				}
			%>
			<ul class="footer">
				<li>All prices are per Kilogram.</li>
				<li>The buy price is the price you pay for acquiring the goods.</li>
				<li>The sell price is the price you get for selling the goods.</li>
				<li>Sell limit is the amount that can be sold.</li>
			</ul>
		</div>
		<%
		}
		else
		{
			if (airport != null)
			{
		%>
		<div class="message">No information available</div>
		<%
			}
			if (isSearch)
			{
				if (modelParam != null && !modelParam.equals(""))
					modelId = Integer.parseInt(modelParam);

				if (distanceParam != null && !distanceParam.equals(""))
					distance = Integer.parseInt(distanceParam);

				List<CachedAirportBean> airports = null;
				try
				{
					if (hasAssignments || hasFuel || hasJeta || hasRepair || hasFbo || hasAcForSale || ferry || modelId != -1 || commodity > 0 || !Helpers.isNullOrBlank(nameParam) || !Helpers.isNullOrBlank(fromParam))
						airports = Airports.findAirports(hasAssignments, modelId, nameParam, distance, fromParam, ferry, goodsMode, commodity, minAmount, hasFuel, hasJeta, hasRepair, hasAcForSale, hasFbo, isRentable);
				}
				catch (DataError e)
				{
		%>
		<div class="error"><%= e.getMessage() %></div>
		<%
			}

			if (airports != null)
			{
				CachedAirportBean fromAirport = null;
				if (!Helpers.isNullOrBlank(fromParam))
				{
					String icao = fromParam.toUpperCase();
					fromAirport = Airports.cachedAirports.get(icao);
				}
		%>
		<div class="dataTable">
			<table class="goodssearchTable tablesorter-default tablesorter">
				<caption>Airports</caption>
				<thead>
				<tr>
					<th>ICAO</th>
					<%
						if (fromAirport != null)
						{
					%>
					<th class="numeric">NM</th>
					<th class="numeric">Bearing</th>
					<%
						}
					%>
					<th>Name</th>
					<th>Country</th>
				</tr>
				</thead>
				<tbody>
				<%
					for (CachedAirportBean ap : airports)
					{
				%>
				<tr>
					<%
						if (fromAirport == null || fromAirport.getIcao().equals(ap.getIcao()))
						{
					%>
					<td>
						<a href="#" onclick="gmap.setSize(620,530);gmap.setUrl('<%= response.encodeURL("gmap.jsp?icao=" + ap.getIcao()) %>');gmap.showPopup('gmap');return false;" id="gmap">
							<img src="<%= Airports.getDescriptiveImage(ap, Fbos.getAirportFboSlotsInUse(ap.getIcao()) > 0) %>" style="border-style: none; vertical-align:middle;" /></a><a href="<%= response.encodeURL("airport.jsp?icao=" + ap.getIcao()) %>"><%= ap.getIcao() %>
					</a>
					</td>
					<%
					}
					else
					{
					%>
					<td>
						<a href="#" onclick="gmap.setSize(620,530);gmap.setUrl('<%= response.encodeURL("gmap.jsp?icao=" + fromAirport.getIcao()+"&icaod="+ ap.getIcao()) %>');gmap.showPopup('gmap');return false;" id="gmap">
							<img src="<%= Airports.getDescriptiveImage(ap, Fbos.getAirportFboSlotsInUse(ap.getIcao()) > 0) %>" style="border-style: none; vertical-align:middle;" />
						</a>
						<a href="<%= response.encodeURL("airport.jsp?icao=" + ap.getIcao()) %>">
							<%= ap.getIcao() %>
						</a></td>
					<%
						}

						if (fromAirport != null)
						{
							DistanceBearing distanceBearing = Airports.getDistanceBearing(fromAirport.getIcao(), ap.getIcao());
							int toDistance = (int)Math.round(distanceBearing.distance);
							int toBearing = (int)Math.round(distanceBearing.bearing);
							String image = Airports.getBearingImageURL(toBearing);
					%>
					<td class="numeric"><%= toDistance %></td>
					<td class="numeric"><%= toBearing %> <img src="<%= image %>" /></td>
					<%
						}
					%>
					<td><%= ap.getTitle() %></td>
					<td><%= ap.getCountry() %></td>
				</tr>
				<%
					}
				%>
				</tbody>
			</table>
		</div>

		<script type="text/javascript">
			$(function() {

				$.extend($.tablesorter.defaults, {
					widthFixed: false,
					widgets : ['zebra','columns']
				});

				$('.goodssearchTable').tablesorter();

			});
		</script>

		<%
					}
				}
			}
			List<ModelBean> models = Models.getAllModels();
		%>

		<div class="form" style="width: 500px">
			<form method="post" action="airport.jsp">
				<h2>Search Airports</h2>

				<div class="formgroup">
					By ICAO: <input name="icao" type="text" class="textarea" size="4"/>
					&nbsp;
					By aircraft registration: <input name="registration" type="text" class="textarea" size="10"/>
				</div>

				<div class="formgroup">
					By name or city: <input name="name" type="text" class="textarea" value="<%= nameParam == null ? "" : nameParam %>" size="20"/>
				</div>

				<div class="formgroup">
					Airports that have this aircraft:
					<select name="model" class="formselect">
						<option class="formselect" value=""></option>
						<%
							for (ModelBean model : models)
							{
						%>
						<option class="formselect" value="<%= model.getId() %>" <%= model.getId() == modelId ? "selected" : ""%>><%= model.getMakeModel() %></option>
						<%
							}
						%>
					</select>
					<input type="checkbox" name="rentable" value="rentable"  <%= isRentable ? "checked" : "" %>/> rentable
				</div>
				<div class="formgroup">
					Airports that have:
					<table style="margin-left:20px">
						<tr>
							<td><input type="checkbox" name="hasFuel" value="fuel" <%= hasFuel ? "checked" : ""%>/> 100LL fuel</td>
							<td><input type="checkbox" name="hasJeta" value="jetafuel" <%= hasJeta ? "checked" : ""%>/> JetA fuel</td>
							<td><input type="checkbox" name="hasRepair" value="repair" <%= hasRepair ? "checked" : ""%>/> repairshop</td>
						</tr>
						<tr>
							<td><input type="checkbox" name="assignments" value="assignments" <%= hasAssignments ? "checked" : ""%>/> assignments</td>
							<td><input type="checkbox" name="ferry" value="ferry" <%= ferry ? "checked" : ""%>/> aircraft that need ferrying</td>
						</tr>
						<tr>
							<td><input type="checkbox" name="hasAcForSale" value="ac" <%= hasAcForSale ? "checked" : ""%>/> aircraft for sale</td>
							<td><input type="checkbox" name="hasFbo" value="fbo" <%= hasFbo ? "checked" : ""%>/> FBO</td>
						</tr>
					</table>
				</div>
				<div class="formgroup">
					Airports that are within
					<select name="distance" class="formselect">
						<option class="formselect" value="10" <%= distance == 10 ? "selected" : "" %>>10</option>
						<option class="formselect" value="20" <%= distance == 20 ? "selected" : "" %>>20</option>
						<option class="formselect" value="50" <%= distance == 50 ? "selected" : "" %>>50</option>
						<option class="formselect" value="100" <%= distance == 100 ? "selected" : "" %>>100</option>
						<option class="formselect" value="250" <%= distance == 250 ? "selected" : "" %>>250</option>
						<option class="formselect" value="500" <%= distance == 500 ? "selected" : "" %>>500</option>
						<option class="formselect" value="1000" <%= distance == 1000 ? "selected" : "" %>>1000</option>
						<option class="formselect" value="2000" <%= distance == 2000 ? "selected" : "" %>>2000</option>
					</select>
					NM from
					<input name="from" type="text" class="textarea" value="<%= fromParam == null ? "" : fromParam %>" size="4" />
				</div>
				<div class="formgroup">
					Airports that
					<select name="goodsMode" class="formselect">
						<option class="formselect" value="sell">Sell</option>
						<option class="formselect" value="buy">Buy</option>
					</select>
					<select name="commodity" class="formselect">
						<option class="formselect" value=""></option>
						<%
							for (int c=0; c < Goods.commodities.length; c++)
							{
								if (Goods.commodities[c] == null)
									continue;
						%>
						<option class="formselect" value="<%= c %>"><%= Goods.commodities[c].getName() %></option>
						<%
							}
						%>
					</select>
					Minimum Kg: <input name="minAmount" type="text" value="100" size="6" />
				</div>
				<div class="formgroup">
					<input type="hidden" name="submit" value="true" />
					<input type="submit" class="button" value="Go" />
					<input type="reset" class="button" value="Clear" />
				</div>
			</form>
		</div>
	</div>
</div>


<div class="modal fade" id="airportOpsModal">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button class="close" aria-hidden="true" type="button" data-dismiss="modal">×</button>
				<h4 class="modal-title">Airport Operations</h4>
			</div>
			<div class="modal-body">
				<div class="container">
					<div class="row" id="chart-container">
					</div>
				</div>
			</div>
			<div class="modal-footer">
				<button class="btn btn-default" type="button" data-dismiss="modal">Close</button>
			</div>
		</div>
	</div>
</div>

</body>
</html>
