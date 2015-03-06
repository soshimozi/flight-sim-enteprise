<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.beans.UserBean, net.fseconomy.dto.TrendHours, net.fseconomy.util.Helpers"
        %>
<%@ page import="java.util.List" %>
<%@ page import="net.fseconomy.dto.ClientFlightStats" %>
<%@ page import="net.fseconomy.util.Formatters" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
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

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css"/>
    <link rel="stylesheet" type="text/css" href="../css/tablesorter-style.css"/>
    <link rel="stylesheet" type="text/css" href="../css/Master.css"/>

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
    <script src="../scripts/AutoComplete.js"></script>
    <script type='text/javascript' src='../scripts/jquery.tablesorter.js'></script>
    <script type='text/javascript' src="../scripts/jquery.tablesorter.widgets.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#username", "#user", <%= Accounts.ACCT_TYPE_PERSON %>);

            $.extend($.tablesorter.defaults, {
                widthFixed: false,
                widgets: ['zebra', 'columns']
            });

            $('.flightTable').tablesorter();

        });

    </script>

</head>
<body>

<jsp:include flush="true" page="../top.jsp" />
<jsp:include flush="true" page="../menu.jsp" />

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
    List<ClientFlightStats> stats;
    stats = SimClientRequests.getCurrentFlightStats();

    if (!Helpers.isNullOrBlank(message))
    {
%>
        <div class="message"><%= message %></div>
<%
    }
%>
        <div class="dataTable">
            <h2>Flight stats - Last 200 Flights</h2><br/>
            <a href="/admin/admin.jsp">Return to Admin Page</a><br/>
            <table class="flightTable tablesorter-default tablesorter">
                <thead>
                <tr>
                    <th>Pilot</th>
                    <th>Client</th>
                    <th>Start</th>
                    <th>End</th>
                    <th>From</th>
                    <th>To</th>
                    <th>MakeModel</th>
                    <th>Dist</th>
                    <th>Stop Dist</th>
                    <th>Flt Time (cruise)</th>
                    <th>Real Time</th>
                    <th>Est. TC</th>
                </tr>
                </thead>
                <tbody>
<%
        for (ClientFlightStats item: stats)
        {
%>
                <tr>
                    <td><a href="checkclientflights.jsp?id=<%= item.userId %>"><%= Accounts.getAccountNameById(item.userId) %></a></td>
                    <td><%= item.client %></td>
                    <td><%=Formatters.dateyyyymmddhhmmss.format(item.startTime) %></td>
                    <td><%=Formatters.dateyyyymmddhhmmss.format(item.endTime) %></td>
                    <td><%= item.fromIcao %></td>
                    <td><%=item.toIcao %></td>
                    <td><%=item.makeModel %></td>
                    <td><%=Formatters.twoDecimals.format(item.distance) %></td>
                    <td><%=Formatters.twoDecimals.format(item.stopDistance) %></td>
                    <td><%=Formatters.getHourMin(item.flightTimeSeconds) %></td>
                    <td><%=Formatters.getHourMinSec(item.realTimeSeconds) %></td>
                    <td><%= item.estimatedTC %></td>
                </tr>
<%
        }
%>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
