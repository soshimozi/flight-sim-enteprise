<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.dto.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	String sId = request.getParameter("id");
	if(sId == null)
	{

%>
        "Invalid Model Id!"
<%
		return;
	}

	int id = Integer.parseInt(sId);
	AircraftConfig acconfig = Aircraft.getAircraftConfigs(id);
	
	double endurHr = (double)Math.round(((double)acconfig.fcaptotal/(double)acconfig.gph)*10.0)/10.0;
	double endurNm = (double)Math.round((endurHr * acconfig.cruisespeed)*10.0)/10.0;

	double fuelprice = acconfig.fueltype > 0 ? (Goods.currFuelPrice*Goods.currJetAMultiplier) : Goods.currFuelPrice;
	
	double costPerHr = (double)Math.round(((double)acconfig.gph * fuelprice)*100.0)/100.0;
	double costPerNm = (double)Math.round((costPerHr / (double)acconfig.cruisespeed)*100.0)/100.0;
%>

<style type="text/css">

.fuel {
    width: 75px;
 	border:1px solid yellow;
    background:green;
    border-radius:2px;
    color: #fff;
    text-align: center;
}
.nofuel {
    width: 75px;
 	border: 1px solid #bbbbbb;
    background: #dddddd;
    border-radius:2px;
    color: #aaa;
    text-align: center;
}
.fuelLabel {
    padding: 1px;
}

</style>

<div class="row clearfix">
	<div class="col-sm-12 column">
        <div class="panel panel-primary">
            <h3 class="text-center">
                <%=acconfig.makemodel %><br>
                <button class="btn btn-primary btn-xs" id="aliasRequest">
                    Request Aliases
                </button>
            </h3>
            

            <div class="panel panel-default" style="margin: 10px">
                <h4>Stats</h4>
                <div class="row clearfix">
                    <div class="col-xs-6 col-sm-6 col-md-6 column">
                        <div class="text-right">Additional Crew</div>
                        <div class="text-right">Seats</div>
                        <div class="text-right">Paxs</div>
                        <div class="text-right">Cruise</div>
                        <div class="text-right">Engines</div>
                        <div class="text-right">Fuel Type</div>
                        <div class="text-right">GPH</div>
                        <div class="text-right">Total Fuel</div>
                        <div class="text-right">Payload - No Fuel</div>
                        <div class="text-right">Est Endurance NM</div>
                        <div class="text-right">Est Endurance Hrs</div>
                        <div class="text-right">Est Cost Per NM</div>
                        <div class="text-right">Est Cost per Hr</div>
                        <div class="text-right">MTOW</div>
                        <div class="text-right">Empty Weight</div>
                        <div class="text-right">Base Price</div>
                    </div>
                    <div  class="col-xs-6 col-sm-6 col-md-6 column">
                        <div class="text-left"><%=acconfig.crew %></div>
                        <div class="text-left"><%=acconfig.seats %></div>
                        <div class="text-left"><%=acconfig.seats - (acconfig.crew > 0 ? 2 : 1) %></div>
                        <div class="text-left"><%=acconfig.cruisespeed %></div>
                        <div class="text-left"><%=acconfig.engines %></div>
                        <div class="text-left"><%=acconfig.fueltype > 0 ? "JetA" : "100LL" %></div>
                        <div class="text-left"><%=acconfig.gph %></div>
                        <div class="text-left"><%=acconfig.fcaptotal %></div>
                        <div class="text-left"><%=acconfig.maxWeight-acconfig.emptyWeight %></div>
                        <div class="text-left"><%=endurNm %></div>
                        <div class="text-left"><%=endurHr %></div>
                        <div class="text-left"><%=costPerNm %></div>
                        <div class="text-left"><%=costPerHr %></div>
                        <div class="text-left"></div>
                        <div class="text-left"><%=acconfig.maxWeight %></div>
                        <div class="text-left"><%=acconfig.emptyWeight %></div>
                        <div class="text-left"><%=Formatters.currency.format(acconfig.price) %></div>                        
                    </div>
                </div>
                <h4>Fuel Tanks</h4>
                <div class="row clearfix">
                    <div class="col-xs-6 col-sm-6 col-md-6 column">
                        <div class="fuelLabel text-right">Ext 1</div>
                        <div class="fuelLabel text-right">L Tip</div>
                        <div class="fuelLabel text-right">L Aux	</div>
                        <div class="fuelLabel text-right">L Main</div>
                        <div class="fuelLabel text-right">Center</div>
                        <div class="fuelLabel text-right">Center 2</div>
                        <div class="fuelLabel text-right">Center 3</div>
                        <div class="fuelLabel text-right">R Main</div>
                        <div class="fuelLabel text-right">R Aux</div>
                        <div class="fuelLabel text-right">R Tip</div>
                        <div class="fuelLabel text-right">Ext 2</div>
                    </div>
                    <div class="col-xs-6 col-sm-6 col-md-6 column">
                        <div class="text-left <%=acconfig.fcapExt1 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapExt1 %></div>
                        <div class="text-left <%=acconfig.fcapLeftTip > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapLeftTip %></div>
                        <div class="text-left <%=acconfig.fcapLeftAux > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapLeftAux %></div>
                        <div class="text-left <%=acconfig.fcapLeftMain > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapLeftMain %></div>
                        <div class="text-left <%=acconfig.fcapCenter > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapCenter %></div>
                        <div class="text-left <%=acconfig.fcapCenter2 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapCenter2 %></div>
                        <div class="text-left <%=acconfig.fcapCenter3 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapCenter3 %></div>
                        <div class="text-left <%=acconfig.fcapRightMain > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapRightMain %></div>
                        <div class="text-left <%=acconfig.fcapRightAux > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapRightAux %></div>
                        <div class="text-left <%=acconfig.fcapRightTip > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapRightTip %></div>
                        <div class="text-left <%=acconfig.fcapExt2 > 0 ? "fuel" : "nofuel" %>"><%=acconfig.fcapExt2 %></div>
                    </div>
                </div>                
            </div>
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