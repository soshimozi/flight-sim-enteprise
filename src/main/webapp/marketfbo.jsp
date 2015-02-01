<%@page language="java"
		contentType="text/html; charset=ISO-8859-1"
		import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
		%>
<%@ page import="java.util.HashMap" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	if(!user.isLoggedIn())
	{
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
		return;
	}

	//setup return page if action used
	String returnPage = request.getRequestURI();
	response.addHeader("referer", request.getRequestURI());
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css" rel="stylesheet">
	<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css" rel="stylesheet">
	<link type="text/css" href="css/redmond/jquery-ui.css" rel="stylesheet"/>
	<link href="css/Master.css" rel="stylesheet"/>
	<link href="css/tablesorter-style.css" rel="stylesheet"/>

	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
	<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
	<script src='scripts/jquery.tablesorter.js'></script>
	<script src="scripts/jquery.tablesorter.widgets.js"></script>
	<script src='scripts/parser-checkbox.js'></script>
	<script src='scripts/parser-timeExpire.js'></script>
	<script src="scripts/PopupWindow.js"></script>


	<script type="text/javascript">

		var gmapfs = new PopupWindow();
		var gmap = new PopupWindow();

		function purchaseFbo()
		{
			var form = document.getElementById("formFboModal");
			var ebuyer = document.getElementById("groupSelect");

			form.accountid.value = ebuyer.options[ebuyer.selectedIndex].value;
			form.submit();
		}

		$(function() {

			$.extend($.tablesorter.defaults, {
				widthFixed: false,
				widgets : ['zebra','columns']
			});

			$('.fboTable').tablesorter();
		});

		function selectFbo(fboId)
		{
			var form = document.getElementById("formFboModal");
			form.fboid.value = fboId;

			$("#fboData").load( "fbodata.jsp?fboid=" + fboId );

			$("#myModal").modal('show');
		}

	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<div class="dataTable">
			<form method="post" action="userctl" name="fboForm">
				<input type="hidden" name="event" value="MarketFbo"/>
				<input type="hidden" name="id">
				<input type="hidden" name="account" value="<%= user.getId() %>"/>
				<input type="hidden" name="returnpage" value="marketfbo.jsp" />

				<table class="fboTable tablesorter-default tablesorter">
					<caption>FBOs for sale
						<a href="#" onclick="gmapfs.setSize(690,535);gmapfs.setUrl('<%= response.encodeURL("gmapmarketfbo.jsp") %>');gmapfs.showPopup('gmapfs');return false;" id="gmapfs"><img src="img/wmap.gif" width="50" height="32" border="0" align="absmiddle" /></a>
					</caption>
					<thead>
					<tr>
						<th colspan="1" class="sorter-false disabledtext" style="background-color: lightsalmon">Click Name for full information and to purchase</th>
					</tr>
					<tr>
						<th>Name</th>
						<th>ICAO</th>
						<th>Location</th>
						<th></th>
						<th>Price</th>
					</tr>
					</thead>
					<tbody>
<%
	List<FboBean> fbos = Fbos.getFboForSale();

	//Get hashmap of airports in list
	HashMap<String, CachedAirportBean> aps = Airports.getAirportsFromFboList(fbos);
	for (FboBean fbo : fbos)
	{
		String fboname = fbo.getName();
		String price = Formatters.currency.format(fbo.getPrice());
		CachedAirportBean airport = aps.get(fbo.getLocation());
%>
					<tr>
						<td onclick="selectFbo(<%=fbo.getId()%>)"><%= fboname %></td>
						<td><%= Airports.airportLink(airport.getIcao(), airport.getIcao(), response) %></td>
						<td colspan="2"><%= airport.getTitle() %></td>
						<td style="text-align: right;"><%= price %></td>
					</tr>
<%
	}
%>
					</tbody>
				</table>
			</form>
		</div>
	</div>
</div>

<!-- Modal HTML -->
<div id="myModal" class="modal fade">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				<h4 class="modal-title">FBO Information and Purchase</h4>
			</div>
			<div class="modal-body">
				<form id="formFboModal" method="post" action="userctl" class="ui-front">
					<input type="hidden" name="event" value="purchaseFbo"/>
					<input type="hidden" name="accountid" value=""/>
					<input type="hidden" name="fboid" value=""/>
					<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
					<div id="fboData">
					</div>
					<div>
						Purchase for:
						<select id="groupSelect">
							<option value=""></option>
							<option value="<%=user.getId()%>"><%= user.getName()%></option>
							<%
								List<UserBean> groups = Accounts.getGroupsForUser(user.getId());

								for (UserBean group : groups)
								{
									if (user.groupMemberLevel(group.getId()) >= UserBean.GROUP_STAFF)
									{
							%>
							<option value="<%=group.getId()%>"><%= group.getName()%></option>
							<%
									}
								}
							%>
						</select>

					</div>
				</form>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
				<button type="button" class="btn btn-primary" onclick="purchaseFbo();">Purchase</button>
			</div>
		</div>
	</div>
</div>

</body>
</html>
