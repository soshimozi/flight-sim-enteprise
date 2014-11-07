<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters, net.fseconomy.util.Converters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

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

	<link href="css/Master.css" rel="stylesheet" type="text/css" />

	<% //regressed jquery so that lightbox would work %>
	<script src="scripts/jquery.min.js"></script>
	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script src="scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
	<link href="css/tablesorter-style.css" rel="stylesheet" type="text/css" />
	
	<script src="fancybox/jquery.fancybox-1.3.1.pack.js"></script>
	<script charset="iso-8859-1" src="scripts/js/highcharts.js"> </script>
	<link href="fancybox/jquery.fancybox-1.3.1.css" rel="stylesheet" type="text/css" />
	
	<script src="scripts/PopupWindow.js"></script>
	<script src="scripts/Master.jsp"></script>
	<script type="text/javascript"> var gmap = new PopupWindow(); </script>

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
	            width: 690,
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
		
	$(document).ready(function() {
	    $('#aircraft-operations').fancybox({
	        width: '700px',
	        height: '450px',
	        onStart: function () {
	            document.getElementById('chart-popup').style.display = 'block';
	        },
	        onClosed: function () {
	            document.getElementById('chart-popup').style.display = 'none';
	        }
	    });
	    
	    makeChart();
	});
	</script>
	
	<script type="text/javascript">
		
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
		});
		
		$(function() 
		{	
			$.extend($.tablesorter.defaults, 
			{
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.buildTable').tablesorter();	
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
                <a href="gmapfbo.jsp?fboOwner=<%= account.getId() %>"><img src="img/wmap.gif" width="50" height="32" style="border-style: none; vertical-align:middle;" /></a>
                <a id="aircraft-operations" href="#chart-popup" style="padding-left:10px;">FBO Operations</a>
            </caption>
            <thead>
                <tr>
                    <th style="width: 55px;">ICAO</th>
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
		AirportBean ap = Airports.getAirport(fbo.getLocation());
%>
    		<tr>
                <td><%= Airports.airportLink(ap, ap, response) %></td>
                <td><%= fbo.getName() %></td>
                <td><%= fbo.isActive() ? "Open" : "<span style=\'color: red;\'>Closed</span>" %></td>
                <td class="numeric"><%= fbo.isForSale() ? Formatters.currency.format(fbo.getPrice()) + (fbo.getPriceIncludesGoods() ? " + goods" : "") : "" %></td>
                <td class="numeric"><%= fbo.getFboSize() %></td>
                <td class="numeric"><%= supplies != null ? ((supplies.getAmount() / fbo.getSuppliesPerDay(ap) > 14) ? supplies.getAmount() : "<span style=\"color: red;\">" + supplies.getAmount() + "</span>") : "" %></td>
                <td class="numeric"><%= fbo.getSuppliesPerDay(ap) %></td>
                <td class="numeric"><%= supplies != null ? ((supplies.getAmount() / fbo.getSuppliesPerDay(ap) > 14) ? supplies.getAmount() / fbo.getSuppliesPerDay(ap) : "<span style=\"color: red;\">" + supplies.getAmount() / fbo.getSuppliesPerDay(ap)+ "</span>" ): "" %></td>
                <td class="numeric"><%= fuel != null ? fuel.getAmount() : "" %></td>
                <td class="numeric"><%= jeta != null ? jeta.getAmount() : "" %></td>
                <td class="numeric"><%= buildingmaterials != null ? buildingmaterials.getAmount() : "" %></td>
                <td>
                    <a class="link" href="<%= response.encodeURL("editfbo.jsp?id=" + fbo.getId()) %>">Edit</a>
                    |<a class="link" href="<%= response.encodeURL("buyBulkFuel.jsp?id=" + fbo.getId()) %>"><%=Fbos.doesBulkFuelRequestExist(fbo.getId()) ? " Order Pending ":" Order Bulk Fuel " %></a>
<%		
		String paymentUrl;
        if (account.isGroup())
        {
            paymentUrl = "paymentlog.jsp?groupId=" + account.getId() + "&fboId=" + fbo.getId();
        }
        else
        {
            paymentUrl = "paymentlog.jsp?fboId=" + fbo.getId();
        }
%>
                    | <a class="link" href="<%= response.encodeURL(paymentUrl) %>">Payments</a>
                    | <a class="link" href="<%= response.encodeURL("fbolog.jsp?id=" + fbo.getId()) %>">Log</a>
                    | <a class="link" href="<%= response.encodeURL("fbotransfer.jsp?id=" + fbo.getId()) %>">Transfer</a>
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
	<form id="constructFboForm" method="post" action="fbo.jsp?id=<%= account.getId() %>">
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
				<td><%= Airports.airportLink(airport, airport, response) %></td>
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
<br>
<br>
<div id="chart-popup" style="display:none;width:700px;height:450px;">
	<div id="chart-container"></div>
</div>

</body>
</html>
