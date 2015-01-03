<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.util.Formatters, net.fseconomy.beans.*, net.fseconomy.data.*"
%>
<%@ page import="net.fseconomy.util.Constants" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    List<LogBean> logs;
    String sGroup = request.getParameter("groupid");
    String sFrom = request.getParameter("from");

    String linkOptions = "";
    String groupName = "";
    String mapViewer;
    String selector;
    int from = 0;
    int amount;
    boolean groupPage = sGroup != null;

    if (sFrom != null)
        from = Integer.parseInt(sFrom);

    if (from < 0) // airboss 8/22/13 - prevent negative numbers
        from = 0;

    if (!groupPage)
    {
        selector = "pilot " + user.getName();
        logs = Logging.getLogForUser(user, from, Constants.stepSize);
        amount = Logging.getAmountLogForUser(user);
        mapViewer = "pilot=" + user.getName();
    }
    else
    {
        int groupId = Integer.parseInt(sGroup);
        amount = Logging.getAmountLogForGroup(groupId);
        logs = Logging.getLogForGroup(groupId, from, Constants.stepSize);
        UserBean group = Accounts.getGroupById(groupId);

        selector = "group " + group.getName();
        linkOptions = "groupid=" + sGroup + "&";
        mapViewer = "group=" + group.getId();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css"/>

</head>
<body>

<jsp:include flush="true" page="top.jsp"/>
<jsp:include flush="true" page="menu.jsp"/>

<%
    if (logs.size() > 0)
    {
        String sid = request.getParameter("id");

        if (sid != null)
        {
            int id = Integer.parseInt(sid);
            LogBean log = null;
            for (LogBean lb : logs)
            {
                if (lb.getId() == id)
                {
                    log = lb;
                    break;
                }
            }


            double total = (log.getIncome()) - log.getRentalCost() - log.getFuelCost() - log.getLandingCost() - log.getCrewCost() - log.getFboAssignmentFee() - log.getmptTax() + log.getBonus();
            int minutes = log.getFlightEngineTime() / 60;
            String rentalCost;
            String flightTime = Formatters.twoDigits.format(minutes / 60) + ":" + Formatters.twoDigits.format(minutes % 60);

            if (log.getAccounting() == 1)
                rentalCost = "" + log.getFlightEngineTicks();
            else
                rentalCost = flightTime;

            boolean isGroup = log.getGroupId() > 0;
            double paidToPilot = log.getPilotFee();
            double paidToGroup = total - paidToPilot;
            if (isGroup)
            {
                UserBean group = Accounts.getGroupById(log.getGroupId());
                if (group != null)
                    groupName = group.getName();
            }
%>

<div id="wrapper">
    <div class="content">
        <table class="flightLog">
            <tbody>
            <tr>
                <td class="logHead"><%= Formatters.getUserTimeFormat(user).format(log.getTime())%>
                </td>
                <td class="logHead cost"><%= log.getFrom() + " &rarr; " + log.getTo()%> (<%=  flightTime %>)</td>
            </tr>
            <tr>
                <td class="space"></td>
                <td></td>
            </tr>
            <tr>
                <td class="type">Income</td>
                <td class="cost"><%= Formatters.currency.format(log.getIncome()) %>
                </td>
            </tr>
            <tr>
                <td class="space"></td>
                <td></td>
            </tr>
            <tr>
                <td class="type">Rental</td>
                <td class="cost"></td>
            </tr>
            <tr>
                <td class="type indent">Amount</td>
                <td class="cost"><%= rentalCost %></td>
            </tr>
            <tr>
                <td class="type indent">Cost per unit</td>
                <td class="cost ul"><%= Formatters.currency.format(log.getRentalPrice()) %></td>
            </tr>
            <tr>
                <td class="type">Total rental cost</td>
                <td class="cost total"><%= Formatters.currency.format(-log.getRentalCost()) %></td>
            </tr>
            <tr>
                <td class="space"></td>
                <td></td>
            </tr>
            <tr>
                <td class="type">Additional cost</td>
                <td class="cost"></td>
            </tr>
            <tr>
                <td class="type indent">Fuel</td>
                <td class="cost"><%= Formatters.currency.format(-log.getFuelCost()) %></td>
            </tr>
            <tr>
                <td class="type indent">Landing Fee</td>
                <td class="cost"><%= Formatters.currency.format(-log.getLandingCost()) %></td>
            </tr>
            <tr>
                <td class="type indent">Additional Crew</td>
                <td class="cost"><%= Formatters.currency.format(-log.getCrewCost()) %></td>
            </tr>
            <tr>
                <td class="type indent">Ground Crew Fee</td>
                <td class="cost"><%= Formatters.currency.format(-log.getFboAssignmentFee()) %></td>
            </tr>
            <tr>
                <td class="type indent">Booking Fee</td>
                <td class="cost ul"><%= Formatters.currency.format(-log.getmptTax()) %></td>
            </tr>
            <tr>
                <td class="type">Total additional cost</td>
                <td class="cost total"><%= Formatters.currency.format(-log.getFuelCost() - log.getLandingCost() - log.getCrewCost() - log.getFboAssignmentFee() - log.getmptTax()) %></td>
            </tr>
            <tr>
                <td class="space"></td>
                <td></td>
            </tr>
            <tr>
                <td class="type">Distance bonus</td>
                <td class="cost total"><%= Formatters.currency.format(log.getBonus()) %></td>
            </tr>
            <tr class="total">
                <td class="type">Earnings this flight</td>
                <td class="cost total"><%= Formatters.currency.format(total) %></td>
            </tr>
<%
            if (isGroup)
            {
%>
            <tr>
                <td class="space"></td>
                <td></td>
            </tr>
            <tr>
                <td class="type total">Paid to group "<%= groupName %>"</td>
                <td class="cost total"><%= Formatters.currency.format(paidToGroup) %></td>
            </tr>
            <tr>
                <td class="type total">Paid to pilot</td>
                <td class="cost total"><%= Formatters.currency.format(paidToPilot) %></td>
            </tr>
            <tr>
                <td class="space"></td>
                <td></td>
            </tr>
<%
            }
%>
            </tbody>
        </table>
<%
        }
%>
        <div class="dataTable">
            <table>
                <caption>All flights for <%= selector %></caption>
                <thead>
                <tr>
                    <th>Date</th>
<%
        if (groupPage)
        {
%>
                    <th>Pilot</th>
<%
        }
%>
                    <th>From</th>
                    <th>To</th>
                    <th>Aircraft</th>
                    <th>Type</th>
                    <th>Duration</th>
                    <th>Distance</th>
                    <th>Earnings</th>
                    <th>Action</th>
                </tr>
                </thead>
                <tbody>
<%
        for (LogBean log : logs)
        {
            int minutes = log.getFlightEngineTime() / 60;
            String flightTime = Formatters.twoDigits.format(minutes / 60) + ":" + Formatters.twoDigits.format(minutes % 60);

            double total = (log.getIncome()) - log.getRentalCost() - log.getFuelCost() - log.getLandingCost() - log.getCrewCost() - log.getFboAssignmentFee() + log.getBonus() - log.getmptTax();

            AircraftBean aircraft = Aircraft.getAircraftById(log.getAircraftId());
            String username = Accounts.getAccountNameById(log.getUserId());
%>
                <tr>
                    <td><%= Formatters.getUserTimeFormat(user).format(log.getTime()) %></td>
<%
            if (groupPage)
            {
%>
                    <td><%= username %></td>
<%
            }
%>
                    <td>
                        <a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + log.getFrom()) %>"><%= log.getFrom() %></a>
                    </td>
                    <td>
                        <a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + log.getTo()) %>"><%= log.getTo() %></a>
                    </td>
                    <td>
                        <a class="normal" href="<%= response.encodeURL("aircraftlog.jsp?id=" + aircraft.getId()) %>"><%= aircraft.getRegistration() %></a>
                    </td>
                    <td><%= aircraft.getMakeModel() %></td>
                    <td><%= flightTime %></td>
                    <td><%= log.getDistance() %></td>
                    <td><%= Formatters.currency.format(total) %></td>
                    <td>
                        <a class="link" href="<%= response.encodeURL("log.jsp?" + linkOptions + "from=" + from + "&id=" + log.getId()) %>">View</a>
                    </td>
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
            int newFrom = from - 5 * Constants.stepSize;
            if (newFrom < 0)
            {
                newFrom = 0;
            }
%>
                                    <a href="<%= response.encodeURL("log.jsp?" + linkOptions + "from=" + newFrom) %>">&lt;&lt;</a>
                                    <a href="<%= response.encodeURL("log.jsp?" + linkOptions + "from=" + (from-Constants.stepSize)) %>">&lt;</a>
<%
        }
%>
                                </td>
                                <td align="right">
<%
        if ((from + Constants.stepSize) < amount)
        {
            int newFrom = from + 5 * Constants.stepSize;
            if ((newFrom + Constants.stepSize) > amount)
            {
                newFrom = amount - Constants.stepSize;
            }
%>
                                    <a href="<%= response.encodeURL("log.jsp?" + linkOptions + "from=" + (from+Constants.stepSize)) %>">&gt;</a>
                                    <a href="<%= response.encodeURL("log.jsp?" + linkOptions + "from=" + newFrom) %>">&gt;&gt;</a>
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
            <a class="link" href="javascript:void(window.open('<%= response.encodeURL("logviewer.jsp?" + mapViewer) %>','LogViewer','status=no,toolbar=n,height=750,width=680'))">[View maps]</a>
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
%>
    </div>

</div>
</body>
</html>
