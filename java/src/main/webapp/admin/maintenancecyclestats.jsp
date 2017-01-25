<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="net.fseconomy.data.*, net.fseconomy.beans.UserBean, net.fseconomy.util.Formatters"
%>

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
        <a href="/admin/admin.jsp">Return to Admin page</a><br/>

<%
    if(MaintenanceCycle.getCycleTimeData() == null)
    {
%>
        No stats available yet...
<%
    }
    else
    {
%>
        <h2>30 minute Cycle</h2>
        <div class="dataTable">

            <table id="sortableTableStats" class="sortable">
                <thead>
                <tr>
                    <th>LogStart</th>
                    <th>Hit Count</th>
                    <th>TotalTime</th>
                    <th>Min Time</th>
                    <th>Max Time</th>
                    <th>Avg Time</th>
                </tr>
                </thead>

                <tbody>
<%
        MaintenanceCycle.CycleTimeData ctd = MaintenanceCycle.getCycleTimeData();

        //dump out our HashMap data
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        sb.append("<td>").append(Formatters.dateyyyymmddhhmmss.format(ctd.logstarttime[0])).append("</td>")
                .append("<td>").append(ctd.hitcount[0]).append("</td>")
                .append("<td>").append(ctd.totaltime[0]).append("</td>")
                .append("<td>").append(ctd.mintime[0]).append("</td>")
                .append("<td>").append(ctd.maxtime[0]).append("</td>")
                .append("<td>").append(ctd.hitcount[0] == 0 ? 0 : ctd.totaltime[0] / ctd.hitcount[0]).append("</td>");
        sb.append("</tr>");
%>
                <%= sb.toString() %>
                </tbody>
            </table>
        </div>
        <h2>Daily Cycle</h2>
        <div class="dataTable">

            <table id="sortableTableStats" class="sortable">
                <thead>
                <tr>
                    <th>LogStart</th>
                    <th>Hit Count</th>
                    <th>TotalTime</th>
                    <th>Min Time</th>
                    <th>Max Time</th>
                    <th>Avg Time</th>
                </tr>
                </thead>

                <tbody>
<%
        //dump out our HashMap data
        sb.setLength(0);

        sb.append("<tr>")
        .append("<td>").append(Formatters.dateyyyymmddhhmmss.format(ctd.logstarttime[1])).append("</td>")
        .append("<td>").append(ctd.hitcount[1]).append("</td>")
        .append("<td>").append(ctd.totaltime[1]).append("</td>")
        .append("<td>").append(ctd.mintime[1]).append("</td>")
        .append("<td>").append(ctd.maxtime[1]).append("</td>")
        .append("<td>").append(ctd.hitcount[1] == 0 ? 0 : ctd.totaltime[1] / ctd.hitcount[1]).append("</td>")
        .append("</tr>");
%>
                <%= sb.toString() %>
                </tbody>
            </table>
        </div>
<%
    }
%>
    </div>
</div>
</body>
</html>
