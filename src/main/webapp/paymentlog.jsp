<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />
<jsp:useBean id="userMap" class="java.util.HashMap" scope="session" />

<%
    long tsStart = System.nanoTime();
    String sGroup = request.getParameter("groupId");
    String sFboId = request.getParameter("fboId");
    String aircraft = request.getParameter("aircraft");
    String sFrom = request.getParameter("from");
    String sMonth = request.getParameter("month");
    String sYear = request.getParameter("year");

    String linkOptions = "";
    String selector;
    int from = 0;
    int fboId = 0;
    int groupId = 0;
    int account;
    boolean groupPage = sGroup != null;

    if (sFboId != null)
        fboId = Integer.parseInt(sFboId);
    if (sFrom != null)
        from = Integer.parseInt(sFrom);


    if (!groupPage)
    {
        account = user.getId();
        selector = "pilot " + user.getName();
    }
    else
    {
        groupId = Integer.parseInt(sGroup);
        account = groupId;
        selector = "group " + getUser(account, userMap);
        linkOptions = "groupId=" + sGroup + "&";
    }
    String sFilter = "";
    FboBean filterFbo;

    if (sFboId != null)
    {
        fboId = Integer.parseInt(sFboId);
        linkOptions = linkOptions + "fboId=" + fboId + "&";
        filterFbo = Fbos.getFbo(fboId);
        sFilter = filterFbo.getLocation() + " - " + filterFbo.getName();
    }

    if ((aircraft != null) && !aircraft.equals(""))
    {
        linkOptions = linkOptions + "aircraft=" + aircraft + "&";
        sFilter = sFilter + " - " + aircraft;
    }
%>

<%!
    String getUser(int id, Map<Integer, String> userMap)
    {
        if (id == 0)
            return "FS Economy";

        String value = userMap.get(new Integer(id));
        if (value == null)
        {
            UserBean thisUser = Accounts.getAccountById(id);
            if (thisUser != null)
                value = thisUser.getName();
            userMap.put(new Integer(id), value);
        }
        return value == null ? " - " : value;
    }

    String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    double subTotal = 0;

    String lineitem(double amount)
    {
        subTotal += amount;
        return Formatters.currency.format(amount);
    }

    String doSubTotal()
    {
        String returnValue = Formatters.currency.format(subTotal);
        subTotal = 0;
        return returnValue;
    }
%>


<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="scripts/PopupWindow.js"></script>
    <script type="text/javascript">
        var gmap = new PopupWindow();
    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	if(account == -1)
	{
%>
	<div class="message">Invalid Account Id!</div>	
<%
	}
	else if (sMonth != null)
	{
		int month = Integer.parseInt(sMonth);
		int year = Integer.parseInt(sYear);
		int currMonth = Calendar.getInstance().get(Calendar.MONTH);
		int currYear = Calendar.getInstance().get(Calendar.YEAR);
		
		if(month > 12 || month < 1)
			month=currMonth;
		
		if(year > currYear || year < 1985)
			year = currYear;

		int nextMonth = month + 1, prevMonth = month - 1;
		int nextYear = year, prevYear = year;
		if (nextMonth > 12)
		{
			nextMonth = 1;
			nextYear++;
		} else if (prevMonth == 0)
		{
			prevMonth = 12;
			prevYear--;
		}
		double[][] statement = Banking.getStatement(new GregorianCalendar(year, month-1, 1), account, fboId, aircraft, user.getShowPaymentsToSelf());
		String nextString = "paymentlog.jsp?" + linkOptions + "month=" + nextMonth + "&year=" + nextYear;
		String prevString = "paymentlog.jsp?" + linkOptions + "month=" + prevMonth + "&year=" + prevYear;
		
		int flightOps = 0;
		if(groupId > 0)
			flightOps = Airports.getGroupOperationsByMonthYear(groupId,month,year);
		else if(account > 0)
			flightOps = Airports.getGroupOperationsByMonthYear(account,month,year);
%>
	<div class="dataTable">
	<table class="flightLog">
	<caption><%= getUser(account, userMap) %> - <%= monthNames[month-1] %> <%= year%> <%= !sFilter.equals("") ? " - " + sFilter : "" %></caption>
	<tbody>
	<tr><td class="type">Assignments</td><td></td></tr>
	<tr><td class="type indent">Rental expenses</td><td class="cost"><%= lineitem(statement[PaymentBean.RENTAL][1]) %></td></tr>
	<tr><td class="type indent">Assignment income</td><td class="cost"><%= lineitem(statement[PaymentBean.ASSIGNMENT][0]) %></td></tr>
	<tr><td class="type indent">Assignment expenses</td><td class="cost"><%= lineitem(statement[PaymentBean.ASSIGNMENT][1]) %></td></tr>
	<tr><td class="type indent">Pilot fees</td><td class="cost"><%= lineitem(statement[PaymentBean.PILOT_FEE][0] + statement[PaymentBean.PILOT_FEE][1]) %></td></tr>
	<tr><td class="type indent">Additional crew fees</td><td class="cost"><%= lineitem(statement[PaymentBean.CREW_FEE][0] + statement[PaymentBean.CREW_FEE][1]) %></td></tr>
	<tr><td class="type indent">Ground crew fees</td><td class="cost"><%= lineitem(statement[PaymentBean.FBO_ASSIGNMENT_FEE][1]) %></td></tr>
	<tr><td class="type indent">Booking fees</td><td class="cost"><%= lineitem(statement[PaymentBean.MULTIPLE_PT_TAX][1]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="type">Aircraft operations</td><td></td></tr>
	<tr><td class="type indent">Rental income</td><td class="cost"><%= lineitem(statement[PaymentBean.RENTAL][0]) %></td></tr>
	<tr><td class="type indent">Refueling 100LL</td><td class="cost"><%= lineitem(statement[PaymentBean.REASON_REFUEL][1]) %></td></tr>
	<tr><td class="type indent">Refueling JetA</td><td class="cost"><%= lineitem(statement[PaymentBean.REASON_REFUEL_JETA][1]) %></td></tr>
	<tr><td class="type indent">Landing fees</td><td class="cost"><%= lineitem(statement[PaymentBean.LANDING_FEE][0] + statement[PaymentBean.LANDING_FEE][1]) %></td></tr>
	<tr><td class="type indent">Expenses for maintenance</td><td class="cost"><%= lineitem(statement[PaymentBean.MAINTENANCE][1]) %></td></tr>
	<tr><td class="type indent">Equipment installation</td><td class="cost"><%= lineitem(statement[PaymentBean.EQUIPMENT][1]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="type">Aircraft sales</td><td></td></tr>
	<tr><td class="type indent">Aircraft sold</td><td class="cost"><%= lineitem(statement[PaymentBean.AIRCRAFT_SALE][0]) %></td></tr>
	<tr><td class="type indent">Aircraft bought</td><td class="cost"><%= lineitem(statement[PaymentBean.AIRCRAFT_SALE][1]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="type">FBO operations</td><td></td></tr>
	<tr><td class="type indent">Aircraft refueling 100LL</td><td class="cost"><%= lineitem(statement[PaymentBean.REASON_REFUEL][0]) %></td></tr>
	<tr><td class="type indent">Aircraft refueling JetA</td><td class="cost"><%= lineitem(statement[PaymentBean.REASON_REFUEL_JETA][0]) %></td></tr>
	<tr><td class="type indent">Ground crew fees</td><td class="cost"><%= lineitem(statement[PaymentBean.FBO_ASSIGNMENT_FEE][0]) %></td></tr>
	<tr><td class="type indent">Repairshop income</td><td class="cost"><%= lineitem(statement[PaymentBean.MAINTENANCE][0]) %></td></tr>
	<tr><td class="type indent">Repairshop expenses</td><td class="cost"><%= lineitem(statement[PaymentBean.MAINTENANCE_FBO_COST][0] + statement[PaymentBean.MAINTENANCE_FBO_COST][1]) %></td></tr>
	<tr><td class="type indent">Equipment installation</td><td class="cost"><%= lineitem(statement[PaymentBean.EQUIPMENT][0]) %></td></tr>
	<tr><td class="type indent">Equipment expenses</td><td class="cost"><%= lineitem(statement[PaymentBean.EQUIPMENT_FBO_COST][0] + statement[PaymentBean.EQUIPMENT_FBO_COST][1]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="type">Facilities</td><td></td></tr>
	<tr><td class="type indent">PT rent income</td><td class="cost"><%= lineitem(statement[PaymentBean.FBO_FACILITY_RENT][0]) %></td></tr>
	<tr><td class="type indent">PT rent expenses</td><td class="cost"><%= lineitem(statement[PaymentBean.FBO_FACILITY_RENT][1]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="type">FBO sales</td><td></td></tr>
	<tr><td class="type indent">FBO sold</td><td class="cost"><%= lineitem(statement[PaymentBean.FBO_SALE][0]) %></td></tr>
	<tr><td class="type indent">FBO bought</td><td class="cost"><%= lineitem(statement[PaymentBean.FBO_SALE][1]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="type">Goods bought</td><td></td></tr>
	<tr><td class="type indent">Wholesale 100LL</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_FUEL][1]) %></td></tr>
	<tr><td class="type indent">Wholesale JetA</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_JETA][1]) %></td></tr>
	<tr><td class="type indent">Building materials</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_BUILDING_MATERIALS][1]) %></td></tr>
	<tr><td class="type indent">Supplies</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_SUPPLIES][1]) %></td></tr>
	<tr><td></td><td></td></tr>
	
	<tr><td class="type">Goods sold</td><td></td></tr>
	<tr><td class="type indent">Wholesale 100LL</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_FUEL][0]) %></td></tr>
		<tr><td class="type indent">Wholesale JetA</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_JETA][0]) %></td></tr>
	<tr><td class="type indent">Building materials</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_BUILDING_MATERIALS][0]) %></td></tr>
	<tr><td class="type indent">Supplies</td><td class="cost"><%= lineitem(statement[PaymentBean.SALE_GOODS_SUPPLIES][0]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="type">Special</td><td></td></tr>
	<tr><td class="type indent">Group payments</td><td class="cost"><%= lineitem(statement[PaymentBean.GROUP_PAYMENT][0] + statement[PaymentBean.GROUP_PAYMENT][1]) %></td></tr>
	<tr><td class="type indent">Group deletion</td><td class="cost"><%= lineitem(statement[PaymentBean.GROUP_DELETION][0] + statement[PaymentBean.GROUP_DELETION][1]) %></td></tr>
	<tr class="total"><td class="space"></td><td class="cost"><b><%= doSubTotal() %></b></td></tr>
	
	<tr><td class="space"></td><td></td></tr>
	<tr class="total"><td>Total</td><td class="cost"><%= Formatters.currency.format(statement[0][0]) %></td></tr>
	<tr><td class="space"></td><td></td></tr>
	<tr><td class="space">Total Flight Operations this month: <%=flightOps == -1 ? "No FBOs Found!" : flightOps %></td><td></td></tr>
	<tr><td class="space"></td><td></td></tr>
	<tr><td><a href="<%= prevString %>">&lt;Previous month</a></td><td align="right"><a href="<%= nextString %>">Next month&gt;</a></td></tr>
	</tbody>
	</table>
	
	</div>

<%
	}
    else
    {
		int amount = Banking.getAmountPaymentsForUser(account, fboId, aircraft, user.getShowPaymentsToSelf());
		List<PaymentBean> logs = Banking.getPaymentsForUser(account, from, Constants.stepSize, fboId, aircraft, user.getShowPaymentsToSelf());
		if (logs.size() > 0)
		{
			GregorianCalendar now = new GregorianCalendar();
			String monthly = "paymentlog.jsp?" + linkOptions + "month=" + (now.get(Calendar.MONTH)+1) + "&year=" + now.get(Calendar.YEAR);
		
%>
	<div class="dataTable">
        <table>
            <caption>Payments for <%= selector %>  <%= !sFilter.equals("") ? " - " + sFilter : "" %></caption>
            <thead>
            <tr>
                <th>Date</th>
                <th>From</th>
                <th>To</th>
                <th>Amount</th>
                <th>Reason</th>
                <th>Airport</th>
                <th>Aircraft</th>
                <th>FBO</th>
                <th>Comments</th>
            </tr>
            </thead>
            <tbody>
<%
                for (PaymentBean log : logs)
                {
                    AirportBean airport = Airports.getAirport(log.getLocation());
                    FboBean fbo = Fbos.getFbo(log.getFboId());
                    AirportBean fboAirport = null;
                    if (fbo != null)
                        fboAirport = Airports.getAirport(fbo.getLocation());

%>
                <tr>
                    <td><%= Formatters.getUserTimeFormat(user).format(log.getTime()) %></td>
                    <td><%= getUser(log.getOtherParty(), userMap) %></td>
                    <td><%= getUser(log.getUser(), userMap) %></td>
                    <td class="numeric"><%= account != log.getOtherParty() ? Formatters.currency.format(log.getAmount()) : "<span style=\"color: red;\">" + Formatters.currency.format(-log.getAmount()) + "</span>" %></td>
                    <td><%= log.getSReason() %></td>
                    <td><%= Airports.airportLink(airport, response) %></td>
                    <td><a class="normal" href="<%= response.encodeURL( log.getAircraft() != null ? "aircraftlog.jsp?registration=" + log.getAircraft() : "" )%>"><%= log.getAircraft() != null ? log.getAircraft() : "" %></a></td>
                    <td><%= fbo != null ? Airports.airportLink(fboAirport, airport, fboAirport, response) + " " + fbo.getName() : (log.getFboId() > 0 ? "(-)" : "") %></td>
                    <td><%= log.getComment() == null ? "" : log.getComment()  %></td>
                </tr>
<%
                }
%>
                <tr>
                    <td colspan="7">
                        <table width="100%">
                        <tr>
                            <td align="left">
<%
                if (from > 0)
                {
                    int newFrom = from - 5*Constants.stepSize;
                    if (newFrom < 0)
                        newFrom = 0;
%>
                        <a href="<%= response.encodeURL("paymentlog.jsp?" + linkOptions + "from=" + newFrom) %>">&lt;&lt;</a>
                        <a href="<%= response.encodeURL("paymentlog.jsp?" + linkOptions + "from=" + (from-Constants.stepSize)) %>">&lt;</a>
<%
                }
%>
                            </td>
                            <td align="right">
<% 			    if ((from+Constants.stepSize) < amount)
                {
                    int newFrom = from+5*Constants.stepSize;
                    if ((newFrom + Constants.stepSize) > amount)
                        newFrom = amount-Constants.stepSize;
%>
                        <a href="<%= response.encodeURL("paymentlog.jsp?" + linkOptions + "from=" + (from+Constants.stepSize)) %>">&gt;</a>
                        <a href="<%= response.encodeURL("paymentlog.jsp?" + linkOptions + "from=" + newFrom) %>">&gt;&gt;</a>
<%
                }
%>
                            </td>
                        </tr>
                        </table>
                    </td>
                </tr>
        </tbody>
        </table>
        <a class="link" href="<%= monthly %>">[View monthly statements]</a>
	</div>
<%
		}
        else
        {
%>
	<div class="message">
	    No logs available yet.
	</div>
<%
		}
	}
%>
</div>
</div>
<%
    long tsEnd = System.nanoTime();
    long elapsed = System.nanoTime()-tsStart;
    String eTime = Formatters.twoDecimals.format(((double)tsEnd-tsStart) / 1000000000.0);
%>
    <div style="font-size: 9pt;">Elapsed Time: <%=eTime%></div>
</body>
</html>
