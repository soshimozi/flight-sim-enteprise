<%@ page language="java"
	import="java.util.*, java.text.*, net.fseconomy.data.*"
%>

<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
	String returnPage  = request.getHeader("referer");

	UserBean account = user;
	String location = request.getParameter("icao");
	String owner = request.getParameter("owner");
	String type = request.getParameter("type");
	
	if (owner.equals("0") && (type.equals("3") || type.equals("4")) )  
	{
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return;
	}

	String sId = request.getParameter("groupId");
	if (sId != null)
	{
		int id = Integer.parseInt(sId);
	    account = data.getAccountById(id);
	}
	
	String goods = "";
	if (type != null)
		goods = data.commodities[Integer.parseInt(type)].getName();
	
    String pricequote = (String)request.getAttribute("price");	
    if (pricequote == null)
    	pricequote ="$0";
    
    String amountquoted = (String)request.getAttribute("samount");	
    if (amountquoted == null)
	    amountquoted ="0";
    
    int goodsavail = data.getGoodsQty(location,(account.getId ()),(Integer.parseInt(type)));
%>
<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
	
	<link href="theme/Master.css" rel="stylesheet" type="text/css" />

	<script src="scripts/jquery.min.js"></script>

	<script>

		$(document).ready(function() {
			$("#getGoodsQuote").click(function(){
				var icao = $("#icao").val();
				var fueltype = $("#goodstype").val();
				var amount = $("#amount").val();
				var src = $("#owner").val();
				$.ajax({
					type: "GET",
					url: "/rs/goodsquote/"+fueltype+'/'+amount+'/'+icao+'/'+src,
					dataType: "json",
					success: function(response){
						$("#goodsamt").html($("#amount option:selected").text());
						$("#goodsprice").html(response);
					},
					 error: function(e){
					   alert('Error: ' + e);
					 }
					});
			});
		});
		
	</script>
	
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	
	<h2><br>Purchase <%= goods %> For <%= account.getName () %> at <%= location %></h2>
	<h3>Quantity you already own at this location = <%= goodsavail %>Kg</h3>
	
	<div class="content">	
		<div class="form" style="width: 500px">
			<h2>Buy goods</h2>
			<br/>
			<form method="post" action="userctl">		
				<div class="formgroup">
					Amount: <input name="amount" type="text" class="textarea" size="5"/> Kg
				</div>
				<div class="formgroup">
					<input type="submit" class="button" value="Buy" />
				</div>
				<input type="hidden" name="event" value="buyGoods"/>
				<input type="hidden" name="return" value="airport.jsp"/>
				<input type="hidden" name="account" value="<%= account.getId() %>"/>	
				<input type="hidden" name="icao" value="<%= location %>"/>	
				<input type="hidden" name="owner" value="<%= owner %>"/>	
				<input type="hidden" name="type" value="<%= type %>"/>	
				<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
			</form>
		</div>
	</div>
	
	<div class="form" style="width: 500px">
		<h2>Goods Price Quote</h2>
		<h3><i>No goods or money will be transferred</i></h3>
		
		<form>		
			<div class="formgroup">
				Amount: <input id="amount" type="text" class="textarea" size="5"/> Kg<br><br>
				The cost for <span id="goodsamt">0</span>Kg equals <span id="goodsprice">$0.00</span>
			</div>
			<div class="formgroup">
				<input type="button" class="button" id="getGoodsQuote" value="Get Quote" />
			</div>
			<input type="hidden" id="icao" value="<%= location %>"/>	
			<input type="hidden" id="owner" value="<%= owner %>"/>	
			<input type="hidden" id="goodstype" value="<%= type %>"/>
		</form>
	</div>
</div>
</body>
</html>
