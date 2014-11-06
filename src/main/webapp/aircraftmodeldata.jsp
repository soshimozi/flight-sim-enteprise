<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.dto.*,java.text.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

	String sId = request.getParameter("id");
	if(sId == null)
	{
		out.print("Invalid Model Id!");
		return;
	}

	int id = Integer.parseInt(sId);
	AircraftConfigs acconfig = Aircraft.getAircraftConfigs(id);
	
	double endurHr = (double)Math.round(((double)acconfig.fcaptotal/(double)acconfig.gph)*10.0)/10.0;
	double endurNm = (double)Math.round((endurHr * acconfig.cruisespeed)*10.0)/10.0;

	double fuelprice = acconfig.fueltype > 0 ? (Goods.currFuelPrice*Goods.currJetAMultiplier) : Goods.currFuelPrice;
	
	double costPerHr = (double)Math.round(((double)acconfig.gph * fuelprice)*100.0)/100.0;
	double costPerNm = (double)Math.round((costPerHr / (double)acconfig.cruisespeed)*100.0)/100.0;
	
	NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
%>

<style type="text/css">
.mysection {
	margin: 10px;
 	border:2px solid #a1a1a1;
    padding:10px 40px; 
    background:#dddddd;
    border-radius:25px;
    width:360px;
}
.myblock {
	display:table-cell;
	vertical-align:middle;
	text-align:center;
	width:320px;
	margin:auto;
	border:1px solid darkgray;
}
.formright{
	display:inline-block;
	vertical-align:middle;
	padding:0 10px;
	font-size:12pt;
	text-align:right;
	    width: 140px;
}
.formleft {
	display:inline-block;
	vertical-align:middle;
	padding:0 10px;
	font-size:12pt;
	text-align:left;
    width: 120px;
}

.fuel {
 	border:2px solid yellow;
    background:green;
    border-radius:2px;
    color: #fff;
}
.nofuel {
 	border:2px solid #bbbbbb;
    background: #dddddd;
    border-radius:2px;
    color: #aaa;
}

.myButton {
	-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;
	-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;
	box-shadow:inset 0px 1px 0px 0px #ffffff;
	background:-webkit-gradient(linear, left top, left bottom, color-stop(0.05, #ededed), color-stop(1, #dfdfdf));
	background:-moz-linear-gradient(top, #ededed 5%, #dfdfdf 100%);
	background:-webkit-linear-gradient(top, #ededed 5%, #dfdfdf 100%);
	background:-o-linear-gradient(top, #ededed 5%, #dfdfdf 100%);
	background:-ms-linear-gradient(top, #ededed 5%, #dfdfdf 100%);
	background:linear-gradient(to bottom, #ededed 5%, #dfdfdf 100%);
	filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#ededed', endColorstr='#dfdfdf',GradientType=0);
	background-color:#ededed;
	-moz-border-radius:12px;
	-webkit-border-radius:12px;
	border-radius:12px;
	border:1px solid #dcdcdc;
	display:inline-block;
	cursor:pointer;
	color:#777777;
	font-family:arial;
	font-size:15px;
	font-weight:bold;
	padding:6px 12px;
	text-decoration:none;
	text-shadow:0px 1px 0px #ffffff;
}
.myButton:hover {
	background:-webkit-gradient(linear, left top, left bottom, color-stop(0.05, #dfdfdf), color-stop(1, #ededed));
	background:-moz-linear-gradient(top, #dfdfdf 5%, #ededed 100%);
	background:-webkit-linear-gradient(top, #dfdfdf 5%, #ededed 100%);
	background:-o-linear-gradient(top, #dfdfdf 5%, #ededed 100%);
	background:-ms-linear-gradient(top, #dfdfdf 5%, #ededed 100%);
	background:linear-gradient(to bottom, #dfdfdf 5%, #ededed 100%);
	filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#dfdfdf', endColorstr='#ededed',GradientType=0);
	background-color:#dfdfdf;
}
.myButton:active {
	position:relative;
	top:1px;
}

</style>

<div class="container">
	<div>
		<div style="width: 400px; text-align: center; font-size: 14pt; font-weight: bold;"><%=acconfig.makemodel %></div>
		<div style="width: 400px; text-align: center;"><input type="button" id="aliasRequest" class="mybutton" value="Request Aliases"/></div>
	</div>
	<div class="mysection">
		<div style="font-size: 12pt; font-weight: bold;">Stats</div>
		<div class="myblock">
			<span class="formright">Additional Crew</span>
			<span class="formleft"><%=acconfig.crew %></span>
			
			<span class="formright">Seats</span>
			<span class="formleft"><%=acconfig.seats %></span>
			
			<span class="formright">Paxs</span>
			<span class="formleft"><%=acconfig.seats - (acconfig.crew > 0 ? 2 : 1) %></span>
			
			<span class="formright">Cruise</span>
			<span class="formleft"><%=acconfig.cruisespeed %></span>
			
			<span class="formright">Engines</span>
			<span class="formleft"><%=acconfig.engines %></span>
			
			<span class="formright">Fuel Type</span>
			<span class="formleft"><%=acconfig.fueltype > 0 ? "JetA" : "100LL" %></span>
			
			<span class="formright">GPH</span>
			<span class="formleft"><%=acconfig.gph %></span>
			
			<span class="formright">Total Fuel</span>
			<span class="formleft"><%=acconfig.fcaptotal %></span>
			
			<span class="formright">Payload - No Fuel</span>
			<span class="formleft"><%=acconfig.maxWeight-acconfig.emptyWeight %></span>
			
			<span class="formright"></span>
			<span class="formleft"></span>
			
			<span class="formright">Est Endurance NM</span>
			<span class="formleft"><%=endurNm %></span>
			
			<span class="formright">Est Endurance Hrs</span>
			<span class="formleft"><%=endurHr %></span>
			
			<span class="formright">Est Cost Per NM</span>
			<span class="formleft"><%=costPerNm %></span>
			
			<span class="formright">Est Cost per Hr</span>
			<span class="formleft"><%=costPerHr %></span>
			
			<span class="formright"></span>
			<span class="formleft"></span>
			
			<span class="formright">MTOW</span>
			<span class="formleft"><%=acconfig.maxWeight %></span>
			
			<span class="formright">Empty Weight</span>
			<span class="formleft"><%=acconfig.emptyWeight %></span>
			
			<span class="formright">Base Price</span>
			<span class="formleft"><%=Formatters.currency.format(acconfig.price) %></span>
		</div>
		<div style="font-size: 12pt; font-weight: bold;margin-top: 10px;">Fuel Tanks</div>
		<div class="myblock">
			<span class="formright">Ext 1</span>
			<span class="formleft <%=acconfig.fcapExt1 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapExt1 %></span>
			
			<span class="formright">L Tip</span>
			<span class="formleft <%=acconfig.fcapLeftTip > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapLeftTip %></span>
			
			<span class="formright">L Aux	</span>
			<span class="formleft <%=acconfig.fcapLeftAux > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapLeftAux %></span>
			
			<span class="formright">L Main</span>
			<span class="formleft <%=acconfig.fcapLeftMain > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapLeftMain %></span>
			
			<span class="formright">Center</span>
			<span class="formleft <%=acconfig.fcapCenter > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapCenter %></span>
			
			<span class="formright">Center 2</span>
			<span class="formleft <%=acconfig.fcapCenter2 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapCenter2 %></span>
			
			<span class="formright">Center 3</span>
			<span class="formleft <%=acconfig.fcapCenter3 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapCenter3 %></span>
			
			<span class="formright">R Main</span>
			<span class="formleft <%=acconfig.fcapRightMain > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapRightMain %></span>
			
			<span class="formright">R Aux</span>
			<span class="formleft <%=acconfig.fcapRightAux > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapRightAux %></span>
			
			<span class="formright">R Tip</span>
			<span class="formleft <%=acconfig.fcapRightTip > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapRightTip %></span>
			
			<span class="formright">Ext 2</span>
			<span class="formleft <%=acconfig.fcapExt2 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapExt2 %></span>
		</div>
	</div>
	<div class="container">
		<div id="aliasData">
		</div>
	</div>	
</div>
<script>
		$("#aliasRequest").click(function(){
			  $("#aliasData").load( "aircraftmodelaliasdata.jsp?id=<%= id %>");
			  $("#aliasRequest").hide();
			  scrollToAnchor();
		});
		
		function scrollToAnchor(){
		    $('html,body').animate({scrollTop: $("#aliasData").offset().top},'slow');
		}
</script>