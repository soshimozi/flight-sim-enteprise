<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>
<%@ page import="net.fseconomy.util.Formatters" %>
<%@ page import="net.fseconomy.beans.*" %>
<%@ page import="net.fseconomy.data.*" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
    String sfboId = request.getParameter("fboid");
    if (sfboId == null)
    {
        return;
    }

    int fboId = Integer.parseInt(sfboId);
    FboBean fbo = Fbos.getFbo(fboId);

    int owner = fbo.getOwner();
    UserBean fboowner = Accounts.getAccountById(owner);
    int groupOwnerid = Accounts.accountUltimateGroupOwner(owner);

    String icao = fbo.getLocation();
    CachedAirportBean airportInfo = Airports.cachedAirports.get(icao);
    UserBean ultimateOwner = Accounts.getAccountById(groupOwnerid);
    int lots = fbo.getFboSize();
    int totalSpace = fbo.getFboSize() * Airports.getTotalFboSlots(fbo.getLocation());
    int rented = Facilities.getFacilityBlocksInUse(fboId);

    String price = Formatters.currency.format(fbo.getPrice());
    String fboRepairshop = ((fbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0 ? "Yes" : "No");
    String fboPassengerTerminal = ((fbo.getServices() & FboBean.FBO_PASSENGERTERMINAL) > 0 ? totalSpace + " gates (" + rented + " rented)" : "No Passenger Terminal");
    String fboname;
    if(fbo.getName().length() > 45)
        fboname = fbo.getName().substring(0, 45) + "...";
    else
        fboname = fbo.getName();

    String location = airportInfo.getTitle();
    String goodsincluded = "";

    if (fbo.getPriceIncludesGoods())
    {
        GoodsBean fuelbean = Goods.getGoods(icao, owner, GoodsBean.GOODS_FUEL100LL);
        GoodsBean jetabean = Goods.getGoods(icao, owner, GoodsBean.GOODS_FUELJETA);
        GoodsBean suppliesbean = Goods.getGoods(icao, owner, GoodsBean.GOODS_SUPPLIES);
        GoodsBean buildingmaterialsbean = Goods.getGoods(icao, owner, GoodsBean.GOODS_BUILDING_MATERIALS);

        int avgas = fuelbean != null ? fuelbean.getAmount() : 0;
        int jeta = jetabean != null ? jetabean.getAmount() : 0;
        int supplies = suppliesbean != null ? suppliesbean.getAmount() : 0;
        int bms = buildingmaterialsbean != null ? buildingmaterialsbean.getAmount() : 0;

        goodsincluded = "100LL Fuel: " + Formatters.oneDigit.format(avgas) + " KG<br>";
        goodsincluded = goodsincluded + "JetA Fuel: " + Formatters.oneDigit.format(jeta) + " KG<br>";
        goodsincluded = goodsincluded + "Supplies: " + Formatters.oneDigit.format(supplies) + " KG<br>";
        goodsincluded = goodsincluded + "Building Materials: " + Formatters.oneDigit.format(bms) + " KG<br>";
    }

    CachedAirportBean airport = Airports.cachedAirports.get(icao);
%>

<div class="row clearfix">
    <div class="col-sm-12 column">
        <div class="panel panel-primary">
            <h3 class="text-center"><%=Airports.airportLink(airport.getIcao(), airport.getIcao(), response)%></h3>
            <h3 class="text-center"><%=price%></h3>
            <h4 class="text-center"><%= location %></h4>
            <div class="row clearfix">
                <div class="col-xs-3 col-sm-3 col-md-3 column">
                    <div class="text-right">Fbo ID</div>
                    <div class="text-right">Name</div>
                    <div class="text-right">Lots</div>
<%
    if(fboowner.isGroup())
    {
%>
                    <div class="text-right">Group</div>
                    <div class="text-right">Owner</div>
<%
    }
    else
    {
%>
                    <div class="text-right">owner</div>
<%
    }
%>
                    <div class="text-right">Pax Term</div>
                    <div class="text-right">RepairShop</div>
                    <div class="text-right">Goods</div>
                </div>
                <div class="col-xs-9 col-sm-9 col-md-9 column">
                    <div class="text-left"><%=fboId%></div>
                    <div class="text-left" title="<%=fbo.getName()%>"><%=fboname%></div>
                    <div class="text-left"><%=lots%></div>
<%
    if(fboowner.isGroup())
    {
%>
                    <div class="text-left"><%=fboowner.getName()%></div>
                    <div class="text-left"><%=ultimateOwner.getName()%></div>
<%
    }
    else
    {
%>
                    <div class="text-left"><%=fboowner.getName()%></div>
<%
    }
%>
                    <div class="text-left"><%=fboPassengerTerminal%></div>
                    <div class="text-left"><%=fboRepairshop%></div>
                    <div class="text-left"><%=goodsincluded%></div>
                </div>
            </div>
        </div>
    </div>
</div>
