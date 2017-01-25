<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*"
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

	//setup return page if action used
	UserBean account = user;
	String sGroupId = request.getParameter("groupid");

	String groupParam = sGroupId != null ? "?transferid=" + sGroupId : "";
	String returnPage = "goodsassignments.jsp" + groupParam;
	response.addHeader("referer", request.getRequestURI() + groupParam);

	if (sGroupId != null)
		account = Accounts.getAccountById(Integer.parseInt(sGroupId));
	
	if ((account.isGroup() && user.groupMemberLevel(Integer.parseInt(sGroupId)) < UserBean.GROUP_STAFF)
            || (!account.isGroup() && (account.getId()!= user.getId())))
	{
		// If group account only allow group staff or higher to display goods screen.
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
		return;
	}
	List<GoodsBean> goods = Goods.getGoodsForAccountAvailable(account.getId());
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css'>
	<link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css'>
	<link rel="stylesheet" type="text/css" href="css/tablesorter-style.css"/>
	<link rel="stylesheet" type="text/css" href="css/Master.css"/>

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
	<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>

	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>

	<script>

		$(function() {

			$.extend($.tablesorter.defaults, {
				widthFixed: false,
				widgets : ['zebra','columns']
			});

			$('.goodsTable').tablesorter();

			$('.newgoodsassignment').click(function () {
				var params = this.getAttribute("data-querystring");

				$("#transferData").load("newgoodsassignmentdata.jsp" + params, function () {
					newGoodsAssignmentInit('<%=returnPage%>');
				});

				$("#transferModal").modal('show');
			});

			$('.sellgoods').click(function () {
				var url = this.getAttribute("data-url");
				document.location = url;
			});

			$('.transfergoods').click(function () {
				var url = this.getAttribute("data-url");
				document.location = url;
			});

			$('.calcKgsToGals').click(function () {
				var amt = $('.amount').val();
				var result = amt / 2.68735;
				var rounded = Math.ceil((result +.0005) * 1000) / 1000;
				$('.results').val(rounded);
			});

			$('.calcGalsToKgs').click(function () {
				var amt = $('.amount').val();
				var result = amt * 2.68735;
				var rounded = Math.ceil((result +.0005) * 1000) / 1000;
				$('.results').val(rounded);
			});

			$(document).ready(function()
			{
				$(".amount").keypress(function (e) {
					if (String.fromCharCode(e.which).match(/[^0-9.]/g)) return false;
				});
			});

			$(".calculator").submit(function(e) {
				e.preventDefault();
			});
		});

	</script>
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<div class="dataTable">			
			<table class="goodsTable tablesorter-default tablesorter">
				<caption>Goods owned by <%= account.getName() %></caption>
				<thead>
				<tr>
					<th>Location</th>
					<th>Commodity</th>
					<th>Amount</th>
					<th class="sorter-false">Action</th>
				</tr>
				</thead>
				<tbody>
<%
	for (GoodsBean good : goods)
	{
		int commodity = good.getType();
		String location = good.getLocation();
%>
				<tr>
					<td><a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + location) %>"><%= location %></a></td>
					<td><%= good.getCommodity() %></td>
					<td class="numeric"><%= good.getAmount() %> Kg</td>
					<td>
						<input class="btn btn-xs btn-info sellgoods" type="button" value="Sell" title="Sells goods to local FBO."
							   data-url="<%= response.encodeURL("sellgoods.jsp?icao=" + location + "&owner=" + account.getId() + "&type=" + commodity) %>"/>
						<input class="btn btn-xs btn-info newgoodsassignment" type="button" value="New assignment" title="Creates new transfer assignment for the selected goods."
								data-querystring="?ownerid=<%=account.getId()%><%=sGroupId != null ? "&groupid="+sGroupId : ""%>&commodityid=<%=good.getType()%>&fromicao=<%=good.getLocation()%>"/>
						<input class="btn btn-xs btn-info transfergoods" type="button" value="Transfer" title="Transfers goods to new owner"
							   data-url="<%= response.encodeURL("transfergoods.jsp?fromICAO=" + location + "&owner=" + account.getId() + "&commodityId=" + commodity) %>"/>
					</td>
				</tr>
<% 
	}
%>
				</tbody>
			</table>
		</div>

		<div class="container">
			<div class="row clearfix">
				<div class="panel panel-default panel-info align-left" style="width: 400px;">
					<div class="panel-heading">
						<h3 class="panel-title">Conversion Calculator</h3>
					</div>
					<div class="panel-body">

						<form class="calculator form-horizontal">
							<div class="form-group">
								<label for="amount" class="col-sm-2 control-label">Units</label>
								<div class="col-sm-4">
									<input type="text" class="amount form-control" id="amount" placeholder="0.0">
								</div>
							</div>
							<div class="form-group">
								<label for="results" class="col-sm-2 control-label">Result</label>
								<div class="col-sm-4">
									<input type="number" class="results form-control" id="results" placeholder="0.0" disabled>
								</div>
							</div>
							<div class="form-group">
								<div class="col-sm-offset-2 col-sm-4">
									<button class="calcKgsToGals btn btn-xs btn-success" style="margin:5px;">Kgs to Gals</button>
									<button class="calcGalsToKgs btn btn-xs btn-success" style="margin:5px;">Gals to Kgs</button>
								</div>
							</div>
						</form>

					</div>
				</div>
			</div>

		</div>
	</div>
</div>

<!-- Modal HTML -->
<div id="transferModal" class="modal fade">
	<div class="modal-dialog">
		<div class="modal-content ui-front"  style="width: 500px;">
			<div id="transferData">

			</div>
		</div>
	</div>
</div>


</body>
</html>
