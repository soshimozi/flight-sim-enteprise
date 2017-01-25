<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    errorPage="error.jsp"
	    import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />
<jsp:useBean id="fbo" class="net.fseconomy.beans.FboBean" scope="session" />
<jsp:useBean id="dataError" class="net.fseconomy.data.DataError" scope="session" />

<%
	if(!user.isLoggedIn())
	{
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
		return;
	}

	String returnPage = request.getParameter("returnpage");

	String amount100ll = request.getParameter("100LLAmount");
	String amountJetA = request.getParameter("JetAAmount");
	
	double dAmount100ll = Integer.parseInt(amount100ll) / Constants.GALLONS_TO_KG;
	double dAmountJetA = Integer.parseInt(amountJetA) / Constants.GALLONS_TO_KG;
	
	String fboID = request.getParameter("id");
	int days = Fbos.calculateShippingDay(GoodsBean.GOODS_ORDER_FUEL_MIN, GoodsBean.GOODS_ORDER_FUEL_MAX);
	
	//check if enough v$ exists to pay for the request
	double price=0;
			
	//use the account for the owner of the FBO
	FboBean fboAccount = Fbos.getFboByID(Integer.parseInt(fboID));
	UserBean account = Accounts.getAccountById(fboAccount.getOwner());

	if (Integer.parseInt(amount100ll) > 0)
		price = Goods.quoteOrder(fboAccount.getLocation(), GoodsBean.GOODS_FUEL100LL, Integer.parseInt(amount100ll), true);
	
	if (Integer.parseInt(amountJetA) > 0)
		price += Goods.quoteOrder(fboAccount.getLocation(), GoodsBean.GOODS_FUELJETA, Integer.parseInt(amountJetA), true);
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="css/Master.css" rel="stylesheet" type="text/css" />
	
	<script>
		function checkDecline(form)
        {
			var ret = confirm('Are you sure you want to cancel the order? You will be required to wait 24 hours before placing another order.');
            var sel100ll = document.getElementById("fuel100ll");
			if(ret)
			{
				form.event.value = "";
				form.submit();
			}
		}
	</script>
	
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<%	
	if (price > account.getMoney())
	{
%>
	<div class="form" style="width: 500px">
		<h5>Insufficient funds error: The price for this order comes to <span style="color: blue;"><%=Formatters.currency.format(price)%></span> and you only have <span style="color: blue;"><%=Formatters.currency.format(account.getMoney())%></span> in your <span style="color: blue;"><%=account.getName() %></span> account.<br/>
			Please try again when you have enough funds in your Cash Balance.</h5>
	</div> 
<%
		//reset the flag so user is not penalized for not having enough on hand funds and can make another request
		Fbos.resetBulkGoodsOrder(Integer.parseInt(fboID), Fbos.FBO_ORDER_FUEL);
		
		return;				
	}
	else 
	{
		if (Fbos.doesBulkGoodsRequestExist(fbo.getId(), Fbos.FBO_ORDER_FUEL) )
		{ 
%>
	<div class="form" style="width: 500px">
		<h5>A request for bulk fuel for <span style="color: blue;"><%=fboAccount.getLocation() %></span> was made on <span style="color: blue;"><%=Formatters.dateyyyymmddhhmmzzz.format(fboAccount.getBulkFuelOrderTimeStamp())%>.</span></h5>
<%			if (fboAccount.getBulkFuelDeliveryDateTime() != null) 
			{
%>  
		<h5>Delivery is scheduled for <span style="color: blue;"><%=Formatters.dateyyyymmddhhmmzzz.format(fboAccount.getBulkFuelDeliveryDateTime()) %></span></h5> 
<%
			} 
%>
		<h5>You can only make bulk fuel requests every 24 hours or until your order has been delivered.  Please try again later.</h5>
	</div> 
<%
		}
		else 
		{		
		//we have enough money - proceed - log this request
			Fbos.logBulkGoodsRequest(Integer.parseInt(fboID), Fbos.FBO_ORDER_FUEL);
%>
	<div class="form" style="width: 600px">
		<h3>Bulk Order Confirmation</h3>
		<form method="post" action="userctl">
			<div>
				You are placing an order for the FBO: <b>'<%=fboAccount.getName() %>' </b> located at <b><%=fboAccount.getLocation() %></b> airport
				<h5>Order Details</h5>
				<ul>
				  <li>100LL Fuel Quantity: <%=amount100ll %> Kg | <%=(int)Math.round(dAmount100ll) %> gallons</li>
				  <li>JetA Fuel Quantity: <%=amountJetA %> Kg | <%=(int)Math.round(dAmountJetA) %> gallons</li>
				  <li>Cost: <%=Formatters.currency.format(price) %></li>
				  <li>Estimated delivery date: <%=Fbos.deliveryDateFormatted(days) %></li>
				</ul>
			</div>
				
			<div class="formgroup">
				<input type="hidden" name="event" value="confirmBulkFuel"/>
				<input type="submit" name="action" class="button"/>
				<input type="button" name="action" class="button" value="Decline" onClick="checkDecline(this.form)"/>
				<input type="hidden" name="fboID" value="<%=fboID %>"/>
				<input type="hidden" name="amount100ll" value="<%=amount100ll %>"/>
				<input type="hidden" name="amountJetA" value="<%=amountJetA %>"/>
				<input type="hidden" name="daysOut" value="<%=days %>"/>
				<input type="hidden" name="accountToPay" value="<%=account.getId() %>"/>
				<input type="hidden" name="location" value="<%=fboAccount.getId() %>"/>
				<input type="hidden" name="price" value="<%=(int)price %>"/>
				<input type="hidden" name="icao" value="<%=fboAccount.getLocation() %>"/>
				<input type="hidden" name="returnpage" value="<%=returnPage%>" />
			</div>
		</form>
	</div>
<%
		}
	}
%>
</div>
</body>
</html>
