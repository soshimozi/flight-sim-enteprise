<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

	String returnPage = request.getHeader("referer");

	int baseBulkFuelKg=5000;
	int baseBulkFuelGal=1860;
	UserBean account = user;			
			
	if (user == null || user.getId() == 0 || user.getId() == -1) 
	{
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return;
	}
	
	String fboID = request.getParameter("id");
	String location = request.getParameter("icao");
	boolean orderExists;
	
	//use the account for the owner of the FBO
	FboBean fboAccount = Fbos.getFboByID(Integer.parseInt(fboID));
	account = Accounts.getAccountById(fboAccount.getOwner());
	
	//get current fuel levels
	GoodsBean fuelleft = Goods.getGoods(fboAccount.getLocation(), fboAccount.getOwner(), GoodsBean.GOODS_FUEL100LL);
	GoodsBean jetaleft = Goods.getGoods(fboAccount.getLocation(), fboAccount.getOwner(), GoodsBean.GOODS_FUELJETA);
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="/theme/Master.css" rel="stylesheet" type="text/css" />
	
	<script src="/scripts/jquery.min.js"></script>

	<script>
		function checkForm()
		{
			var sel100ll = document.getElementById("fuel100ll");
			var selJetA = document.getElementById("fuelJetA");
			if(sel100ll.selectedIndex == 0 && selJetA.selectedIndex == 0)
			{
				alert("You have not selected a fuel amount to order!");
				return false;
			}
			
			return true;
		}
		
		$(document).ready(function() {
			$("#getFuelQuote").click(function(){
				var icao = $("#icao").val();
				var fueltype = $("#type").val();
				var amount = $("#amount").val();
				$.ajax({
					type: "GET",
					url: "/rs/fuelquote/"+fueltype+'/'+amount+'/'+icao,
					dataType: "json",
					success: function(response){
						$("#fuelamt").html($("#amount option:selected").text());
						$("#fueltype").html($("#type option:selected").text());
						$("#fuelprice").html(response);
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
<%		
	if (Fbos.doesBulkFuelRequestExist(Integer.parseInt(fboID)) )
	{
%>
	<div class="form" style="width: 500px">
		<h5>A request for bulk fuel for <span style="color: blue;"><%=fboAccount.getLocation() %></span> was made on <span style="color: blue;"><%=Formatters.dateyyyymmddhhmmzzz.format(fboAccount.getBulkFuelOrderTimeStamp())%> for <%=fboAccount.getBulk100llOrdered() %> Kg of 100LL and <%=fboAccount.getBulkJetAOrdered() %> Kg of JetA.</span></h5>
<%
		if (fboAccount.getBulkFuelDeliveryDateTime() != null) 
		{
%>  
		<h5>Delivery is scheduled for <span style="color: blue;"><%=Formatters.dateyyyymmddhhmmzzz.format(fboAccount.getBulkFuelDeliveryDateTime()) %></span> </h5> <%
		} 
%>
		<h5>You can only make bulk fuel requests every 24 hours or until your order has been delivered.  Please try again later.</h5>
	</div>
<%
	} 
	else 
	{
%>
	<div class="form" style="width: 500px">
		<form method="post" action="buyBulkFuelConfirm.jsp"  onsubmit="return checkForm()">
			<div>
				<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
			</div>
			<div>
				<h2>Bulk Fuel Order</h2>
				You currently have the following fuel levels at <span style="color: blue;"> <%=fboAccount.getLocation() %>:</span><br/>
				<ul>
				<li><span style="color: blue;">100LL: <%=(int)Math.floor(fuelleft != null ? fuelleft.getAmount() : 0) %> Kg | <%=(int)Math.floor(fuelleft != null ? fuelleft.getAmount()/Data.GALLONS_TO_KG : 0) %> gallons </span></li>
			  	<li><span style="color: blue;">JetA: <%=(int)Math.floor(jetaleft != null ? jetaleft.getAmount() : 0) %> Kg | <%=(int)Math.floor(jetaleft != null ? jetaleft.getAmount()/Data.GALLONS_TO_KG : 0) %> gallons </span> </li>
			  	</ul>
				<br/>
				<h3>Specify the quantity for each fuel type you want to order</h3>
				100LL
				<select id="fuel100ll" name="100LLAmount">
<%
		int i;
		for (i=0; i<=10; i++)
		{
%>					<option value="<%=baseBulkFuelKg*i %>"> <%=baseBulkFuelKg*i %> Kg | <%=(int)Math.round((baseBulkFuelKg*i)/Data.GALLONS_TO_KG) %> Gal  </option>
<%
		}
%> 
				</select> 
				JetA
				<select id="fuelJetA" name="JetAAmount">
<%
		for (i=0; i<=10; i++)
		{
%>
					<option value="<%=baseBulkFuelKg*i %>"> <%=baseBulkFuelKg*i  %> Kg | <%=(int)Math.round((baseBulkFuelKg*i)/Data.GALLONS_TO_KG)  %> Gal  </option>
<%
		}
%> 
				</select>
				<input type="submit" class="button" value="Submit" />
				<h5>Note, once you click the submit button, you will be given an estimated  delivery date between 24-72 hours.  You can accept or decline the offer.
				If you accept the offer, you will pay for your order immediately, and you cannot re-order any of either type at this location until delivery is complete.
				If you decline the offer, you cannot return to this screen for 24 hours.</h5>
				<h5><span style="color: red;">Please be sure you want to proceed before clicking submit!</span></h5>
				<input type="hidden" name="id" value="<%=fboID %>" />
			</div>
		</form>
	</div>
	<div class="form" style="width: 500px">
		<form>
			<div>
				<h2>Price Quote</h2>
				<h3><i>No goods or money will be transferred</i></h3>
				Type:
				<select id="type">
			 		<option value="3"> 100LL </option>
			 		<option value="4"> JetA </option>
				</select>  	
				Amount:
				<select id="amount">
<%
				//loop through adding the values in the drop down for quantities
		for (i=1; i<=10; i++)
		{
%>	
					<option value="<%=baseBulkFuelKg*i %>"> <%=baseBulkFuelKg*i %> Kg | <%=(int)Math.round((baseBulkFuelKg*i)/Data.GALLONS_TO_KG) %> Gal  </option>
<%
		}
%> 
				</select><br/>
				<h5>The cost for <span id="fuelamt">0</span> of <span id="fueltype"></span> equals <span id="fuelprice"></span></h5>
				<input type="button" id="getFuelQuote" class="button" value="Get Quote"/>	
				<input type="hidden" id="icao" value="<%= fboAccount.getLocation() %>"/>	
			</div>
		</form>
	</div>
<%
	} 
%>
</div>
</body>
</html>