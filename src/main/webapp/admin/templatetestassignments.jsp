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
    boolean generate = request.getParameter("generate") != null ? request.getParameter("generate").contains("1") : false;

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

    if(id != 0 && generate)
    {
        MaintenanceCycle mc = MaintenanceCycle.getInstance();
        mc.processTemplateTest(id);
    }

    try
    {
        String qry = "SELECT testassignments.*, CONCAT(models.make, ' ',  models.model) as makemodel, models.cruisespeed as tas, aircraft.id, aircraft.registration"
                + " FROM testassignments LEFT JOIN aircraft on aircraft.id=testassignments.aircraftid LEFT JOIN models on models.id=aircraft.model"
                + " WHERE fromTemplate = " + id + " ORDER BY makemodel, testassignments.fromicao, testassignments.distance;";
        rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

    } catch (SQLException e)
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
        <a href="/admin/templates.jsp">Return to Templates</a><br>
        <form method="post">
            <input type="hidden" name="generate" value="1">
            <input type="submit" value="Generate">
        </form>
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
                    <th>Pay</th>
                    <th>Pay (Hr)</th>
                    <th>Pay (Nm)</th>
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
                            AssignmentBean assignment = new AssignmentBean(rs);

                            float tas = (float)rs.getInt("tas");
                            float pay = assignment.calcPay();
                            float payHr = pay / (assignment.getDistance() / tas);
                            float payNm = pay / assignment.getDistance();
                %>
                    <tr>
                        <td><%= count %></td>
                        <td><%= rs.getInt("id") %></td>
                        <td><%= Formatters.dateyyyymmddhhmmss.format(rs.getTimestamp("creation")) %></td>
                        <td><%= Formatters.dateyyyymmddhhmmss.format(rs.getTimestamp("expires")) %></td>
                        <td><%= rs.getString("commodity") %></td>
                        <td><%= rs.getInt("amount") %></td>
                        <td><%= rs.getString("units") %></td>
                        <td><%= Formatters.currency.format(pay) %></td>
                        <td><%= Formatters.currency.format(payHr) %></td>
                        <td><%= Formatters.currency.format(payNm) %></td>
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
