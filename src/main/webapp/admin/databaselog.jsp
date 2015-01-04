<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import=" java.util.List, net.fseconomy.beans.*,  net.fseconomy.data.*, net.fseconomy.dto.DbLog"
%>
<%@ page import="net.fseconomy.util.Formatters" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    List<DbLog> logList = Logging.getDbLog(0, 100);
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="../css/Master.css" rel="stylesheet" type="text/css" />
    <link href="../css/tablesorter-style.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script type='text/javascript' src='../scripts/jquery.tablesorter.js'></script>
    <script type='text/javascript' src="../scripts/jquery.tablesorter.widgets.js"></script>

</head>
<body>

<jsp:include flush="true" page="../top.jsp" />
<jsp:include flush="true" page="../menu.jsp" />

<div id="wrapper">
    <div class="content">
        <div class="dataTable">
            <table>
                <colgroup>
                    <col style="width: 150px">
                    <col style="width: 40px">
                    <col>
                    <col>
                </colgroup>
                <caption>Database Logs</caption>
                <thead>
                <tr>
                    <th>Time</th>
                    <th>Type</th>
                    <th>Class</th>
                    <th>Message</th>
                </tr>
                </thead>
                <tbody>
<%
    for (DbLog log : logList)
    {
%>
                <tr>
                    <td><%= Formatters.dateyyyymmddhhmmss.format(log.timestamp) %></td>
                    <td><%= log.level %></td>
                    <td><%= log.callerClass.replace("net.fseconomy.", "") %></td>
                    <td><%= log.message %></td>
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
