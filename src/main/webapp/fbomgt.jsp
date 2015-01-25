<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
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
			
			var data = [ ];
		    var series = [ ];
		    var titles = [ ];
			
<%
        for(FboBean aFbo: fbos)
        {
%>
            data.push(<%= Airports.getAirportOperationDataJSON(aFbo.getLocation()) %>);
            titles.push('<%= aFbo.getLocation() %>');
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

		$(function() {
		
			$.extend($.tablesorter.defaults, {
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.fboTable').tablesorter();

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

<div class="content dataTable">
	<table class="fboTable tablesorter-default tablesorter">

	<caption>
	FBO Management for: <%= account.getName() %>
		<a href="#" onclick="gmap.setSize(620,520);gmap.setUrl('gmapfbo.jsp?fboOwner=<%= account.getId() %>');gmap.showPopup('gmap');return false;" id="gmap">
			<img src="img/wmap.gif" width="50" height="32" style="border-style: none; vertical-align:middle;" />
		</a>
		<span class="airportOps btn btn-link">FBO Operations</span>
	</caption>

	<thead>
		<tr>
			<td class="sorter-false" colspan="7">&nbsp;</td>		
			<th class="sorter-false" colspan="2" style="border: 1px solid white;"><span style="margin-left: 10px; text-align: center">Supply</span></th>
			<th class="sorter-false" colspan="2" style="border: 1px solid white;"><span style="margin-left: 10px; text-align: center">Build</span></th>
			<th class="sorter-false" colspan="4" style="border: 1px solid white;"><span style="margin-left: 10px; text-align: center">100LL [<a class="link" href="<%= response.encodeURL("editfuelprices.jsp") + groupParam %>">edit</a>]</span></th>
			<th class="sorter-false" colspan="4" style="border: 1px solid white;"><span style="margin-left: 10px; text-align: center">Jet-A</span></th>
			<td class="sorter-false" >&nbsp;</td>	
		</tr>
			
		<tr>
			<th>ICAO</th>
			<th>FBO Name</th>
			<th title="Current active pax count for this FBO">Paxs</th>
			<th>OnSite</th>
			<th>S/D</th>
			<th>Days</th>		
			<th>Shop</th>
					
			<th>buy</th>
			<th>sell</th>
			
			<th>buy</th>
			<th>sell</th>
					
			<th>ppg</th>
			<th>gal</th>
			<th>buy</th>
			<th>sell</th>
			
			<th>ppg</th>
			<th>gal</th>
			<th>buy</th>
			<th>sell</th>
					
			<th class="sorter-false" >Options</th>	
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
		int availJobs = Fbos.getFacilityJobCount(fbo.getOwner(), fbo.getLocation());
%>
	<tr>
	<td class="nowrap"><%= Airports.airportLink(ap, ap, response) %></td>
	<td><%= supplies != null ? ((supplies.getAmount() / fbo.getSuppliesPerDay(ap) < 1) ? "<span style=\'color: red;\'><small>" + fbo.getName() + "</small></span>" : "<small>" + fbo.getName() + "</small>"): fbo.getName() %></td>

	<td><%= availJobs %></td>		
	<td><%= ap.getSize() > 2999 ? ((ap.getSize() > 4999) ? "BdM/Sp" : "Supply") : "<span style=\'color: gray;\'><small>NONE</small></span>" %></td>
	
	<td class="numeric"><%= fbo.getSuppliesPerDay(ap) %></td>
	<td class="numeric"><%= supplies != null ? ((supplies.getAmount() / fbo.getSuppliesPerDay(ap) > 14) ? supplies.getAmount() / fbo.getSuppliesPerDay(ap) : "<span style=\'color: red;\'>" + supplies.getAmount() / fbo.getSuppliesPerDay(ap)+ "</span>" ): "" %></td>

	<td><%= fbo.getServices() == 1 | fbo.getServices() == 5 ? "<small>" + fbo.getRepairShopMargin() + "%/" + fbo.getEquipmentInstallMargin() + "%</small>" : "<span style=\'color: gray;\'><small>No Shop</small></span>" %></td>
	
	<td class="numeric"><%= supplies != null ? (supplies.getSaleFlag() == 2 | supplies.getSaleFlag() == 3 ?  Formatters.currency.format(supplies.getPriceBuy()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>
	<td class="numeric"><%= supplies != null ? (supplies.getSaleFlag() == 1 | supplies.getSaleFlag() == 3 ?  Formatters.currency.format(supplies.getPriceSell()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>

	<td class="numeric"><%= buildingmaterials != null ? (buildingmaterials.getSaleFlag() == 2 | buildingmaterials.getSaleFlag() == 3 ?  Formatters.currency.format(buildingmaterials.getPriceBuy()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>
	<td class="numeric"><%= buildingmaterials != null ? (buildingmaterials.getSaleFlag() == 1 | buildingmaterials.getSaleFlag() == 3 ?  Formatters.currency.format(buildingmaterials.getPriceSell()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>

	<td class="numeric"><%= fuel != null ? (Formatters.currency.format(fbo.getFuel100LL())): "" %></td>
	<td><%= fuel != null ? (fuel.getAmount() != 0 ? (((int)Math.floor(fuel.getAmount() / 2.68735) < 1000) ? "<span style=\'color: firebrick;\'>" + (int)Math.floor(fuel.getAmount() / 2.68735) + "</span>" : (int)Math.floor(fuel.getAmount() / 2.68735)) : "<span style=\'color: gray;\'><small>NONE</small></span>"): "" %></td>
	<td class="numeric"><%= fuel != null ? (fuel.getSaleFlag() == 2 | fuel.getSaleFlag() == 3 ?  Formatters.currency.format(fuel.getPriceBuy()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>
	<td class="numeric"><%= fuel != null ? (fuel.getSaleFlag() == 1 | fuel.getSaleFlag() == 3 ?  Formatters.currency.format(fuel.getPriceSell()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>
	
	<td class="numeric"><%= jeta != null ? (Formatters.currency.format(fbo.getFueljeta())): "" %></td>
	<td><%= jeta != null ? (jeta.getAmount() != 0 ? (((int)Math.floor(jeta.getAmount() / 2.68735) < 1000) ? "<span style=\'color: firebrick;\'>" + (int)Math.floor(jeta.getAmount() / 2.68735) + "</span>" : (int)Math.floor(jeta.getAmount() / 2.68735)) : "<span style=\'color: gray;\'><small>NONE</small></span>"): "" %></td>
	<td class="numeric"><%= jeta != null ? (jeta.getSaleFlag() == 2 | jeta.getSaleFlag() == 3 ?  Formatters.currency.format(jeta.getPriceBuy()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>
	<td class="numeric"><%= jeta != null ? (jeta.getSaleFlag() == 1 | jeta.getSaleFlag() == 3 ?  Formatters.currency.format(jeta.getPriceSell()) : "<span style=\'color: gray;\'><small>NFS</small></span>"): "" %></td>

	<td>
		<a class="link" href="<%= response.encodeURL("editfbo.jsp?id=" + fbo.getId()) %>">Edit FBO</a>
		<a class="link" href="<%= response.encodeURL("buyBulkFuel.jsp?id=" + fbo.getId()) %>"><%=Fbos.doesBulkFuelRequestExist(fbo.getId()) ? " Order Pending ":" Order Fuel " %></a>
	</td>
	</tr>
<%
	}
%>
	</tbody>
	</table>	
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
