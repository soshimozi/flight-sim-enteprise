<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="net.fseconomy.data.*, java.util.*, net.fseconomy.beans.UserBean, net.fseconomy.servlets.Datafeed, net.fseconomy.util.*"
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
        <h2>Service Provider requests from <%= Formatters.dateyyyymmddhhmmss.format(Datafeed.StatsFrom)%></h2>
        <div class="dataTable">

            <a href="/admin/admin.jsp">Return to Admin page</a><br/>

            <table id="sortableTableStats" class="sortable">
                <thead>
                <tr>
                    <th>Requestor</th>
                    <th>Last Request</th>
                    <th># Served</th>
                    <th># Rejected</th>
                </tr>
                </thead>

                <tbody>
                <%
                    //dump out our HashMap data
                    StringBuilder sb = new StringBuilder();

                    for (Map.Entry<String, Datafeed.Requestor> theRequestor : Datafeed.dataFeedRequestors.entrySet())
                    {
                        Datafeed.Requestor requestor = theRequestor.getValue();

                        if(!requestor.isservice)
                            continue;

                        sb.append("<tr>");

                        sb.append("<td>").append(Converters.XMLHelper.protectSpecialCharacters(requestor.requestorname)).append("</td>")
                                .append("<td>").append(requestor.ts.toString()).append("</td>")
                                .append("<td>").append(requestor.servedcount).append("</td>")
                                .append("<td>").append(requestor.rejectedcount).append("</td>");

                        sb.append("<tr>");
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
