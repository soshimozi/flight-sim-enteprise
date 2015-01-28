<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import=" java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="../css/redmond/jquery-ui.css" rel="stylesheet"/>
    <link href="../css/tablesorter-style.css" rel="stylesheet"/>
    <link href="../css/Master.css" rel="stylesheet"/>

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>

    <script src='../scripts/jquery.tablesorter.js'></script>
    <script src="../scripts/jquery.tablesorter.widgets.js"></script>
    <script src='../scripts/parser-checkbox.js'></script>
    <script src='../scripts/parser-timeExpire.js'></script>
    <script src="../scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        var gmap = new PopupWindow();

    </script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#ownername", "#owner", <%= Accounts.ACCT_TYPE_ALL %>)

            $.extend($.tablesorter.defaults, {
                widthFixed: false,
                widgets : ['zebra','columns']
            });

            $('.fboTable').tablesorter();

        });

    </script>

</head>


<body>
<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />
<div id="wrapper">
    <div class="content">
<%
    String message = Helpers.getSessionMessage(request);
    if (message != null)
    {
%>
	    <div class="message"><%= message %></div>
<%
    }
%>
<%	
    if (request.getParameter("submit") == null && (message == null))
    {
%>
        <h2>Enter FBO's Owners Account</h2>
	    <div class="form" style="width: 400px">
	        <form method="post">
                <div>
                    <input type="hidden" id="owner" name="owner" value=""/>
                    <input type="hidden" name="submit" value="true" />
                    <input type="hidden" name="return" value="/admin/fboedit.jsp" />
                </div>
                Owner Name:
                <input type="text" id="ownername" name="ownername"/> <input type="submit" class="button" value="GO">
            </form>
        </div>
<%
    }
    else if (request.getParameter("submit") != null)
    {
        String Sid = request.getParameter("owner");
        if (Sid.length() == 0)
        {
            message = "Owner Not Found";
        }

        if (message != null)
        {
%>
        <div class="message"><%= message %></div>
<%
	        return;
    	}
%>

<div id="wrapper">
<div class="content">
<div class="dataTable">
<%
        UserBean account = null;
        String sId = request.getParameter("owner");
        if (sId != null && sId.length() > 0)
        {
            int id = Integer.parseInt(request.getParameter("owner"));
            account = Accounts.getAccountById(id);
        }

        List<FboBean> fbos = Fbos.getFboByOwner(account.getId(), "location");
%>
	<form method="post" action="/userctl" name="fboForm">
        <div>
            <input type="hidden" name="event" value="deleteFbo" />
            <input type="hidden" name="id" value="" />
            <input type="hidden" name="return" value="fbo.jsp<%= account.isGroup() ? "?id=" + account.getId() : "" %>" />
        </div>
    	<table class="fboTable">
	        <caption>
                <div>
	            FBO's owned by <%= account.getName() %>
	            <a href="../gmapfbo.jsp?fboOwner=<%= account.getId() %>">
                    <img src="../img/wmap.gif" width="50" height="32" border="0" align="absmiddle">
                </a>
                </div>
	        </caption>
	        <thead>
                <tr>
                    <th>Location</th>
                    <th>Name</th>
                    <th>Active</th>
                    <th class="numeric">Price</th>
                    <th>Lots</th>
                    <th>Supplies</th>
                    <th>S/Day</th>
                    <th>Days</th>
                    <th>100LL Fuel</th>
                    <th>JetA Fuel</th>
                    <th>Bldg. M.</th>
                    <th class="sorter-false">Action</th>
                </tr>
            </thead>
            <tbody>
<%
        for (FboBean fbo : fbos)
        {
            GoodsBean supplies = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_SUPPLIES);
            GoodsBean fuel = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_FUEL100LL);
            GoodsBean jeta = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_FUELJETA);
            GoodsBean buildingMaterials = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
            AirportBean ap = Airports.getAirport(fbo.getLocation());
            int daysAvail = supplies.getAmount() / fbo.getSuppliesPerDay(Airports.getFboSlots(fbo.getLocation()));
%>
                <tr>
                    <td><%= Airports.airportLink(ap, ap, response) %></td>
                    <td><%= fbo.getName() %></td>
                    <td><%= fbo.isActive() ? "Operational" : "Not operational" %></td>
                    <td class="numeric"><%= fbo.isForSale() ? Formatters.currency.format(fbo.getPrice()) + (fbo.getPriceIncludesGoods() ? " + goods" : "") : "" %></td>
                    <td class="numeric"><%= fbo.getFboSize() %></td>
                    <td class="numeric"><%= supplies != null ? (daysAvail > 14) ? supplies.getAmount() : "<span style=\"color: red;\">" + supplies.getAmount() + "</span>" : "" %></td>
                    <td class="numeric"><%= fbo.getSuppliesPerDay(Airports.getFboSlots(fbo.getLocation())) %></td>
                    <td class="numeric"><%= supplies != null ? (daysAvail > 14) ? daysAvail : "<span style=\"color: red;\">" + daysAvail + "</span>" : "" %></td>
                    <td class="numeric"><%= fuel != null ? fuel.getAmount() : "" %></td>
                    <td class="numeric"><%= jeta != null ? jeta.getAmount() : "" %></td>
                    <td class="numeric"><%= buildingMaterials != null ? buildingMaterials.getAmount() : "" %></td>
                    <td>
                        <a class="link" href="<%= response.encodeURL("/fbotransfer.jsp?id=" + fbo.getId()) %>">Transfer</a>
                    </td>
                </tr>
<%
        }
%>
            </tbody>
        </table>
    </form>

<% 
    }
%>
</div>
</div>
</div>
</body>
</html>
