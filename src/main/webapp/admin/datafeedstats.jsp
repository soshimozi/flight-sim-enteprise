<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="java.text.*, net.fseconomy.data.*, java.util.*"
        %>

<%@ page import="net.fseconomy.beans.UserBean" %>
<%@ page import="net.fseconomy.servlets.Datafeed" %>
<%@ page import="net.fseconomy.util.Converters" %>
<%@ page import="net.fseconomy.util.Formatters" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }
%>
<html>
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css" />
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
    <div class="content">
        <h2>Datafeed requests from <%= Formatters.dateyyyymmddhhmmss.format(Datafeed.StatsFrom)%></h2>
        <div class="dataTable">

            <a href="/admin/admin.jsp">Return to Admin page</a><br/>

            <table id="sortableTableStats" class="sortable">
                <thead>
                <tr>
                    <th>Name</th>
                    <th>Hit Count</th>
                    <th>Avg Time</th>
                    <th>Min Time</th>
                    <th>Max Time</th>
                </tr>
                </thead>

                <tbody>
<%
    //dump out our HashMap data
    StringBuilder sb = new StringBuilder();

    Set<Map.Entry<String, Datafeed.FeedHitData>> myset = Datafeed.userFeedProcessTimes.entrySet();

    //dump out our HashMap data
    for (Map.Entry<String, Datafeed.FeedHitData> aMyset : myset)
    {
        Datafeed.FeedHitData fd = aMyset.getValue();

        sb.append("<tr>");
        sb.append("<td>").append(fd.name).append("</td>")
                .append("<td>").append(fd.hitcount).append("</td>")
                .append("<td>").append(fd.totaltime / fd.hitcount).append("</td>")
                .append("<td>").append(fd.mintime).append("</td>")
                .append("<td>").append(fd.maxtime).append("</td>");
        sb.append("</tr>");
    }
%>
                <%= sb.toString() %>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
