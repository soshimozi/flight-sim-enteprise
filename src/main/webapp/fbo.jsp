<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters, net.fseconomy.util.Converters"
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

	UserBean account = null;
	String sId = request.getParameter("id");
	
	//setup return page if action used
	String groupParam = sId != null ? "?id="+sId : "";
	String returnPage = request.getRequestURI() + groupParam;
    response.addHeader("referer", request.getRequestURI() + groupParam);
    
	if (sId != null)
	{
		int id = Integer.parseInt(sId);
		account = Accounts.getAccountById(id);
        if (account != null)
        {
            if (!account.isGroup() || user.groupMemberLevel(id) < UserBean.GROUP_STAFF)
            {
                account = null;
            }
        }		
	}
    if (account == null)
    {
        account = user;
    }
	
	
	List<FboBean> fbos = Fbos.getFboByOwner(account.getId(), "location");
	List<AirportBean> airports = Airports.getAirportsForFboConstruction(account.getId());
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css'>
	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css'>
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

	function makeChart ( ) {
	    var months = { '1': 'Jan', '2': 'Feb', '3': 'Mar', '4': 'Apr', '5': 'May', '6': 'Jun', '7': 'Jul', '8': 'Aug', '9': 'Sep', '10': 'Oct', '11': 'Nov', '12': 'Dec' };
		
		// Manipulate returned value to be JavaScript friendly		
		// Sample data
		/*
		var data =
	    [
	        [
	            [2011, 7, 'KCLT', 122],
	            [2011, 6, 'KCLT', 117],
	            [2011, 5, 'KCLT', 162],
	            [2011, 4, 'KCLT', 153],
	            [2011, 3, 'KCLT', 120],
	            [2011, 2, 'KCLT', 117],
	            [2011, 1, 'KCLT', 162],
	            [2011, 12, 'KCLT', 120],
	            [2011, 11, 'KCLT', 110],
	            [2011, 10, 'KCLT', 110],
	            [2011, 9, 'KCLT', 140],
	            [2011, 8, 'KCLT', 153],
	            [2011, 7, 'KCLT', 110],
	            [2011, 6, 'KCLT', 140],
	            [2011, 5, 'KCLT', 120]
	        ],
	        
	        [
	            [2011, 7, 'YYZ', 123],
	            [2011, 6, 'YYZ', 116],
	            [2011, 5, 'YYZ', 132],
	            [2011, 4, 'YYZ', 152],
	            [2011, 3, 'YYZ', 120],
	            [2011, 2, 'YYZ', 167],
	            [2011, 1, 'YYZ', 162],
	            [2011, 12, 'YYZ', 120],
	            [2011, 11, 'YYZ', 110],
	            [2011, 10, 'YYZ', 110],
	            [2011, 9, 'YYZ', 140],
	            [2011, 8, 'YYZ', 153],
	            [2011, 7, 'YYZ', 110],
	            [2011, 6, 'YYZ', 140],
	            [2011, 5, 'YYZ', 120]
	        ]
	    ];
		*/
		
		var data = [ ];
	    var series = [ ];
	    var titles = [ ];
		
<%
		for(FboBean aFbo: fbos)
		{
%>
        data.push(<%=Airports.getAirportOperationDataJSON(aFbo.getLocation()) %>);
        titles.push('<%=aFbo.getLocation() %>');
<% 	    
        } 
%>		
		for ( var i = 0, d; d = data[i]; i++ ) {
			// Truncate array to only 12 elements - remove first element (current month)
			d.shift();
			
	        if (d.length > 12) {
	            d.length = 12;
	        }
	        d.reverse(); //change order to descending        
	        
	        var xAxisCategories = [ ],
	            dataToDisplay = [ ];
	        
	        for (var j = 0, n; n = d[j++];) {
	            var month = months[String(n['month'])], 			
	                range = n['ops'];
	            
	            xAxisCategories.push(month);
	            dataToDisplay.push(range);
	        }
	        
	        series.push({name: titles[i], data: dataToDisplay});
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
	                min: 0,
	                text: '<b>Operations</b>',
	                style: {
	                    fontWeight: 'normal'
	                }
	            }
	        },
			legend: {
				layout: 'vertical',
				floating: false,
				backgroundColor: '#FFFFFF',
				align: 'right',
				verticalAlign: 'middle',
				symbolWidth: 15,
				symbolPadding: 2
			},
			tooltip: {
				formatter: function() {
					return '<b>' + this.series.name + '</b><br/>' + this.x + ': ' + this.y;
				}
			},
	        series: series,
			title: {
				text: 'FBO Aircraft Operations'
			}
		});
	}
		
	</script>
	
	<script type="text/javascript">

		var gmap = new PopupWindow();

		function doSubmit2(fbodesc, id, bm)
		{
			if (window.confirm("Do you want to tear down " + fbodesc + "?\n" + bm + " KG of Building Materials will be recovered."))
			{
				var form = document.getElementById("fboForm");
				form.id.value = id;
				form.submit();
			}
		}
		
		function doSubmit3(fbodesc, id, bm)
		{
			if (window.confirm("Do you want to partially tear down " + fbodesc + "?\n" + bm + " KG of Building Materials will be recovered."))
			{
				var form = document.getElementById("fboForm");
				form.id.value = id;
				form.submit();
			}
		}

		function doSubmit4(fbodesc, id, bm)
		{
			if (window.confirm("Do you want to build up " + fbodesc + "?\n" + bm + " KG of Building Materials will be used."))
			{
				var form = document.getElementById("fboForm");
				form.id.value = id;
				form.event.value = "upgradeFbo";
				form.submit();
			}
		}
		
	</script>
	
	<script type="text/javascript">

		$(function() 
		{	
			$.extend($.tablesorter.defaults, 
			{
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.fboTable').tablesorter();	

			$.extend($.tablesorter.defaults,
			{
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.buildTable').tablesorter();

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
	<form method="post" action="userctl" id="fboForm">
        <div>
            <input type="hidden" name="event" value="deleteFbo"/>
            <input type="hidden" name="id" />
            <input type="hidden" name="returnpage" value="<%=returnPage%>" />	
            
            <table class="fboTable tablesorter-default tablesorter">
            <caption>
                FBO's owned by <%= account.getName() %>
				<a href="#" onclick="gmap.setSize(620,520);gmap.setUrl('gmapfbo.jsp?fboOwner=<%= account.getId() %>');gmap.showPopup('gmap');return false;" id="gmap">
					<img src="img/wmap.gif" width="50" height="32" style="border-style: none; vertical-align:middle;" />
				</a>
                <span class="airportOps btn btn-link">FBO Operations</span>
            </caption>
            <thead>
                <tr>
                    <th style="width: 65px;">ICAO</th>
                    <th>FBO Name</th>
                    <th>Status</th>
                    <th class="numeric" style="width: 45px;">Price</th>
                    <th class="numeric" style="width: 45px;">Lots</th>
                    <th class="numeric" style="width: 45px;">Supplies</th>
                    <th class="numeric" style="width: 45px;">S/Day</th>
                    <th class="numeric" style="width: 45px;">Days</th>
                    <th class="numeric" style="width: 55px;">100LL Kg</th>
                    <th class="numeric" style="width: 45px;">JetA Kg</th>
                    <th class="numeric" style="width: 45px;">BldM</th>
                    <th class="sorter-false">Options</th>	
                </tr>
            </thead>
            <tbody>
<%
    for (FboBean fbo : fbos)
	{
		GoodsBean supplies = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_SUPPLIES);
		GoodsBean fuel = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_FUEL100LL);
		GoodsBean jeta = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_FUELJETA);
		GoodsBean buildingmaterials = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
		CachedAirportBean ap = Airports.cachedAirports.get(fbo.getLocation());

		int daysAvail = 0;
		if(supplies != null)
			daysAvail = supplies.getAmount() / fbo.getSuppliesPerDay(Airports.getTotalFboSlots(fbo.getLocation()));
%>
    		<tr>
                <td class="nowrap"><%= Airports.airportLink(ap.getIcao(), ap.getIcao(), response) %></td>
                <td><%= fbo.getName() %></td>
                <td><%= fbo.isActive() ? "Open" : "<span style=\'color: red;\'>Closed</span>" %></td>
                <td class="numeric"><%= fbo.isForSale() ? Formatters.currency.format(fbo.getPrice()) + (fbo.getPriceIncludesGoods() ? " + goods" : "") : "" %></td>
                <td class="numeric"><%= fbo.getFboSize() %></td>
                <td class="numeric"><%= supplies != null ? (daysAvail > 14) ? supplies.getAmount() : "<span style=\"color: red;\">" + supplies.getAmount() + "</span>" : "" %></td>
                <td class="numeric"><%= fbo.getSuppliesPerDay(Airports.getTotalFboSlots(fbo.getLocation())) %></td>
                <td class="numeric"><%= supplies != null ? (daysAvail > 14) ? daysAvail : "<span style=\"color: red;\">" + daysAvail + "</span>" : "" %></td>
                <td class="numeric"><%= fuel != null ? fuel.getAmount() : "" %></td>
                <td class="numeric"><%= jeta != null ? jeta.getAmount() : "" %></td>
                <td class="numeric"><%= buildingmaterials != null ? buildingmaterials.getAmount() : "" %></td>
                <td>
                    <a class="link" href="<%= response.encodeURL("editfbo.jsp?id=" + fbo.getId()) %>">Edit</a>
					|<a class="link" href="<%= response.encodeURL("buybulksupplies.jsp?id=" + fbo.getId()) %>"><%=Fbos.doesBulkGoodsRequestExist(fbo.getId(), Fbos.FBO_ORDER_SUPPLIES) ? " Supplies Pending ":" Order Supplies " %></a>
                    |<a class="link" href="<%= response.encodeURL("buybulkfuel.jsp?id=" + fbo.getId()) %>"><%=Fbos.doesBulkGoodsRequestExist(fbo.getId(), Fbos.FBO_ORDER_FUEL) ? " Fuel Pending ":" Order Fuel " %></a>
<%		
		String paymentUrl;
        if (account.isGroup())
        {
            paymentUrl = "paymentlog.jsp?groupid=" + account.getId() + "&fboId=" + fbo.getId();
        }
        else
        {
            paymentUrl = "paymentlog.jsp?fboId=" + fbo.getId();
        }
%>
                    | <a class="link" href="<%= response.encodeURL(paymentUrl) %>">Payments</a>
                    | <a class="link" href="<%= response.encodeURL("fbolog.jsp?id=" + fbo.getId()) %>">Log</a>
<%		if (fbo.deleteAllowed(user))
		{
%>
				    | <a class="link" href="javascript:doSubmit<%= fbo.getFboSize() > 1 ? "3" : "2" %>('<%= "(" + fbo.getLocation() + ") " + Converters.escapeJavaScript(fbo.getName()) %>', '<%= fbo.getId() %>', '<%= fbo.recoverableBuildingMaterials() %>')">Tear Down</a>
<%		}

		if ((Fbos.getAirportFboSlotsAvailable(ap.getIcao()) > 0) && (buildingmaterials != null) && (buildingmaterials.getAmount() >= GoodsBean.CONSTRUCT_FBO))
		{
%>
			    	| <a class="link" href="javascript:doSubmit4('<%= "(" + fbo.getLocation() + ") " + Converters.escapeJavaScript(fbo.getName()) %>', '<%= fbo.getId() %>', '<%= GoodsBean.CONSTRUCT_FBO %>')">Build Up</a>
<%		} 
%>
		        </td>
			</tr>
<%
	}
%>
		    </tbody>
	        </table>
	    </div>
	</form>	
<%	
	if (airports.size() > 0)  
	{
%>
	<br/><br/>
	<form id="constructFboForm" method="post" action="fboconstruct.jsp?id=<%= account.getId() %>">
		<div>
	    <input name="owner" id="owner" type="hidden" value="<%= account.getId() %>" />
	    <input id="event" type="hidden" value="construct" />
		<table class="buildTable tablesorter-default tablesorter">
			<caption>Suitable locations for a new FBO</caption>
			<thead>
				<tr>
					<th>ICAO</th>
					<th>Name</th>
				</tr>
			</thead>
			<tbody>
<% 		
		for (AirportBean airport : airports)
		{	 
%>
			<tr>
				<td class="nowrap"><%= Airports.airportLink(airport.getIcao(), airport.getIcao(), response) %></td>
				<td><%= airport.getTitle() %></td>
			</tr>
<% 		
		}
%>
			</tbody>
		</table>
		Enter ICAO to build FBO at: 
	    <input name="location" id="location" type="text" value="" />
		<input id="mysubmit" type="submit" class="button" value="submit" />
		</div>
	</form>
<% 	
	}
%>

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
