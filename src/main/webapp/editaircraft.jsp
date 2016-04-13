<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
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

	String returnPage = request.getHeader("referer");

	String error = null;

	int id = Integer.parseInt(request.getParameter("id"));
	String mPrice;
	String iPrice;
	
	AircraftBean aircraft = Aircraft.getAircraftById(id);

	String sellToName = "";
	if(aircraft.getSellToId() != 0)
		sellToName = Accounts.getAccountNameById(aircraft.getSellToId());

	mPrice = Formatters.currency.format(aircraft.getMinimumPrice());
	iPrice = Integer.toString(aircraft.getMinimumPrice());
	
	int userlock = aircraft.getUserLock();
	if (!aircraft.changeAllowed(user)) 
	{
		error = "Permission denied";
	} 
	else if (userlock > 0) 
	{
		UserBean lockuser = Accounts.getAccountById(userlock);
		error = "Can not edit. Aircraft is rented by " + lockuser.getName();
	}
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css" rel="stylesheet">
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css">
	<link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css" />
	<link href="css/Master.css" rel="stylesheet" type="text/css" />

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
	<script src="scripts/jquery.cookie.js"></script>
	<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
	<script src="scripts/AutoComplete.js"></script>

	<script type="text/javascript">

		/*
		 decimal_sep: character used as deciaml separtor, it defaults to '.' when omitted
		 thousands_sep: char used as thousands separator, it defaults to ',' when omitted
		 */
		Number.prototype.toMoney = function(decimals, decimal_sep, thousands_sep)
		{
			var n = this,
					c = isNaN(decimals) ? 2 : Math.abs(decimals),
					d = decimal_sep || '.',
					t = (typeof thousands_sep === 'undefined') ? ',' : thousands_sep,
					sign = (n < 0) ? '-' : '',
					i = parseInt(n = Math.abs(n).toFixed(c)) + '',
					j = ((j = i.length) > 3) ? j % 3 : 0;

			return sign + (j ? i.substr(0, j) + t : '') + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : '');
		}

		function sellNow(price)
		{
			var p = Number(price);
			if (window.confirm("Do you want to sell this aircraft for " + p.toMoney(2) + "?"))
			{
				document.aircraftform.action="<%= response.encodeURL("userctl") %>";
				document.aircraftform.event.value = "sell";
				document.aircraftform.submit();
			}
		}
		
		function checkprice()
		{
<%	
	if(aircraft.getLessor() == 0)
	{
%>
			if( document.aircraftform.sellPrice.value != "" && (parseInt(document.aircraftform.sellPrice.value) < <%=iPrice%>))
			{
				var price = Number(document.aircraftform.sellPrice.value);
				if (!window.confirm("You have set the sell price of: $" + price.toMoney(2) + " to less then System Buyback Price of: <%=mPrice%>, are you sure?"))
					return;
			}

			if( document.aircraftform.saletype.value == "1"
					&& document.aircraftform.selltoid.value == "0")
			{
				alert("You have selected Private sale but have not selected who to sell to!");
				return;
			}

<%
	}
%>
			document.aircraftform.submit();
		}

        $(document).ready(function()
        {
            $(".regCheck").keypress(function (e) {
                if (String.fromCharCode(e.which).match(/[^0-9a-zA-Z\-]/g)) return false;
            });

			initAutoComplete("#selltoname", "#selltoid", <%= Accounts.ACCT_TYPE_ALL %>);

			$("input[name=saletype]") // select the radio by its id
					.change(function(){ // bind a function to the change event
						if( $(this).is(":checked") ){ // check if the radio is checked
							var val = $(this).val(); // retrieve the value
							if(val==1){
								$("#divSellTo").css("display", "block")
							}
							else {
								$("#divSellTo").css("display", "none")
							}
						}
					});
		});


	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
<% 
	if (error != null) 
	{ 
%>
		<div class="error"><%= error %></div>
<%	
	} 
	else 
	{ 
%>
		<div class="form" style="width: 400px">
			<form method="post" action="userctl" name="aircraftform">
				<div>
					<input type="hidden" name="event" value="editAircraft"/>
					<input type="hidden" name="id" value="<%=id%>"/>
					<input type="hidden" name="returnpage" value="<%=returnPage%>" />
					<table>
						<caption><%= aircraft.getRegistration() %></caption>
<%
		if(aircraft.getLessor() == 0)
		{
%>	
					<tbody>
					<tr>
						<td>New registration</td>
                        <td><input name="newreg" type="text" class="textarea regCheck" size="8"></td>
					</tr>	
<%
		}
%>	
					<tr>
						<td>Home base</td>
                        <td><input name="home" type="text" class="textarea" value="<%= aircraft.getHome()%>" size="4"></td>
					</tr>
					<tr>
						<td>Distance bonus</td>
                        <td>$ <input name="bonus" type="text" class="textarea" value="<%= aircraft.getBonus()%>" size="4"></td>
					</tr>
					<tr>
						<td>Accounting</td>	
						<td>
						By Hour Rental
						</td>
					</tr>
					<tr>
						<td>Wet price</td>
                        <td>$ <input name="rentalPriceWet" type="text" class="textarea" value="<%= aircraft.getRentalPriceWet()%>" size="4"></td>
					</tr>
					<tr>
						<td>Dry price</td>
                        <td>$ <input name="rentalPriceDry" type="text" class="textarea" value="<%= aircraft.getRentalPriceDry()%>" size="4"></td>
					</tr>
					<tr>
						<td>Max Rental Time</td>
						<td>
							<select name="maxRentTime" class="formselect">
<%
		int intervals = 1800;
		int seconds = 3600;
	
		for (int c=0; c< 19; c++, seconds+=intervals)
		{
			int minutes = seconds/60;
			int hours = minutes/60;
			String time = Formatters.twoDigits.format(hours) + ":" + Formatters.twoDigits.format(minutes%60);
%>
								<option value="<%= seconds %>" <%= aircraft.getMaxRentTime() == seconds ? "selected" : "" %>><%= time %> Hours</option>
<%
		}
%>
							</select>
						</td>
					
					</tr>
<%
		if(aircraft.getLessor() == 0)
		{
%>	
					<tr>
						<td colspan="2">
							<div class="form" style="width:300px;">
								Sale Type:<br>
								<input type="radio" name="saletype" value="0" <%=!aircraft.isPrivateSale() ? "checked" : ""%>> Public&nbsp&nbsp&nbsp&nbsp&nbsp
								<input type="radio" name="saletype" value="1" <%=aircraft.isPrivateSale() ? "checked" : ""%>> Private<br>
								<div id="divSellTo" class="formgroup" <%= aircraft.isPrivateSale() ? "style=\"display: block;\"" : "style=\"display: none;\""%>>
									Enter Account:
									<input type="hidden" id="selltoid" name="selltoid" value="<%=aircraft.getSellToId()%>"/>
									<input type="text" id="selltoname" name="selltoname" style="padding: 2px; margin-bottom: 15px;" value="<%=sellToName%>"/>
									<br/>
								</div>

								On sale for
								$ <input name="sellPrice" type="text" class="textarea" value="<%= aircraft.getSellPrice() == 0 ? "" : (""+aircraft.getSellPrice()) %>" size="6"><br>
								*Blank or 0 = not for sale
							</div>
						</td>
					</tr>
<%
		}
%>	
					<tr>
						<td>Advertise for ferry flight</td>
                        <td><input type="checkbox" name="advertiseFerry" value="true" <%= aircraft.isAdvertiseFerry() ? "checked" : "" %>></td>
					</tr>
			  		<tr>
			    		<td>Allow renters to make repairs</td>
                        <td><input type="checkbox" name="allowRepair" value="true" <%= aircraft.isAllowRepair() ? "checked" : "" %>></td>
			  		</tr>
			  
					<tr>
						<td colspan="2"><input type="button" class="button" onclick="checkprice()" value="Update"/>
<%
		if(aircraft.getLessor() == 0)
		{
%>	
							<br />
							<br />
							<br />
							<input type="button" class="button" onclick="sellNow('<%= mPrice %>')" value="Sell to BANK now for <%= mPrice %>"/>
<%
		}
%>		
						</td>
					</tr>
					</tbody>
					</table>
				</div>
			</form>
		</div>
<%
	} 
%>
	</div>
</div>
</body>
</html>
