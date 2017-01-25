<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import=" java.util.List, net.fseconomy.beans.*,  net.fseconomy.data.*, net.fseconomy.dto.DbLog"
%>
<%@ page import="net.fseconomy.util.Formatters" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.SQLException" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }
    String error = null;
    String sId = request.getParameter("id");
    int id = 0;
    try
    {
        id = Integer.parseInt(sId);
    }
    catch (NumberFormatException nfe)
    {
        error = "Bad template id! Must be an integer.";
    }

    ResultSet rs = null;

    try
    {
        String qry = "SELECT assignments.id, assignments.creation, assignments.expires, assignments.commodity, assignments.amount, assignments.units, assignments.fromicao, assignments.toicao, assignments.distance, CONCAT(models.make, ' ',  models.model) as makemodel, aircraft.id, aircraft.registration"
        + " FROM assignments LEFT JOIN aircraft on aircraft.id=assignments.aircraftid LEFT JOIN models on models.id=aircraft.model"
        + " WHERE fromTemplate = " + id + " ORDER BY makemodel, assignments.fromicao, assignments.distance;";
        rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

    }
    catch (SQLException e)
    {
        e.printStackTrace();
    }

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

    <script type="text/javascript">

        $(function() {

            $.extend($.tablesorter.defaults, {
                widthFixed: false,
                widgets : ['zebra','columns']
            });

            $('.myTable').tablesorter();
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="../top.jsp" />
<jsp:include flush="true" page="../menu.jsp" />

<div id="wrapper">
    <div style="color: red;">
<%
    if(error != null)
    {
%>
    <%=error%>
<%

    }
%>
    </div>
    <div class="content">
        <a href="/admin/templates.jsp">Return to Templates</a>
        <div class="dataTable">
            <table class="myTable tablesorter-default tablesorter">
                <colgroup>
                    <col style="width: 150px">
                    <col style="width: 40px">
                    <col>
                    <col>
                </colgroup>
                <caption>Assignments for template <%= id %></caption>
                <thead>
                <tr>
                    <th>Line</th>
                    <th>AssignmentID</th>
                    <th>Created</th>
                    <th>Expires</th>
                    <th>Commodity</th>
                    <th>Amount</th>
                    <th>Units</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Distance</th>
                    <th>MakeModel</th>
                    <th>Registration</th>
                </tr>
                </thead>
                <tbody>
                <%
                    try
                    {
                        int count = 0;
                        while(rs.next())
                        {
                            count++;
                %>
                    <tr>
                        <td><%= count %></td>
                        <td><%= rs.getInt("id") %></td>
                        <td><%= Formatters.dateyyyymmddhhmmss.format(rs.getTimestamp("creation")) %></td>
                        <td><%= Formatters.dateyyyymmddhhmmss.format(rs.getTimestamp("expires")) %></td>
                        <td><%= rs.getString("commodity") %></td>
                        <td><%= rs.getInt("amount") %></td>
                        <td><%= rs.getString("units") %></td>
                        <td><%= rs.getString("fromicao") %></td>
                        <td><%= rs.getString("toicao") %></td>
                        <td><%= rs.getInt("distance") %></td>
                        <td><%= rs.getString("makemodel") %></td>
                        <td><%= rs.getString("registration") %></td>
                    </tr>
                <%
                        }
                    }
                    catch (SQLException e)
                    {
                        e.printStackTrace();
                    }
                %>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
