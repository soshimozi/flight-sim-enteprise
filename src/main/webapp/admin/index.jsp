<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<%@ page import="net.fseconomy.beans.UserBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

    if(!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"/index.jsp\"</script>");
        return;
    }

    String issubmit = request.getParameter("issubmit");
    if(issubmit !=null && "true".contains(issubmit))
    {
        String event = request.getParameter("event");
        if(event.equals("ResetFilter"))
            net.fseconomy.servlets.FullFilter.updateFilter(data.dalHelper);
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <meta http-equiv="refresh" content="60" />

    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

    <script type="text/javascript">
        function UpdateFilter()
        {
            if (window.confirm("Do you really want to update the filter settings?"))
            {
                document.adminform.submit();
            }
        }
    </script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
    <div class="content">
        <h2>Admin console</h2>

        Database Maintenance status:<br/>
        <%= MaintenanceCycle.status() %><br/>

        <ul><b>Aircraft Utilities</b>
            <li><a href="/admin/aircraftmappings.jsp">Modify mappings of Flight Simulator aircraft</a></li>
            <li><a href="/admin/models.jsp">Modify aircraft models</a></li>
            <li><a href="/admin/aircraftedit.jsp">Edit Aircraft Data</a></li>
            <li><a href="/admin/aircraftshippededit.jsp">Edit Shipped Aircraft</a></li>
        </ul>

        <ul><b>User Account Utilities</b>
            <li><a href="createaccount.jsp">Add User Account</a></li>
            <li><a href="/admin/accountedit.jsp">Edit User Account</a></li>
            <li><a href="/admin/accountstatusedit.jsp">Change Account Status (lock/unlock)</a></li>
            <li><a href="/admin/checkuser48hourtrend.jsp">User 48 Hour Trend</a></li>
            <li><a href="/admin/checkfuelexploit.jsp?numitems=100&pricepoint=10">Check for Fuel Exploit</a></li>
            <li><a href="/admin/checkclientip.jsp">Client IP Checks</a></li>
            <li><a href="/admin/checkclientipduplicates.jsp">Client IPs used by multiple users</a></li>
        </ul>

        <ul><b>Other Utilities</b>
            <li><a href="/admin/fboedit.jsp">Transfer FBOs</a></li>
            <li><a href="/admin/goodsedit.jsp">Adjust Goods</a></li>
            <li><a href="/admin/templates.jsp">Modify assignment templates</a></li>
            <li><a href="/admin/signaturesedit.jsp">Signature Templates</a></li>
        </ul>

        <form method="post" action="index.jsp" name="adminform">
            <div>
                <input type="hidden" name="issubmit" value="true"/>
                <input type="hidden" name="event" value="ResetFilter"/>

                <ul><b>Service/Ip Utilities</b>
                    <li><a href="/admin/serviceproviders.jsp">Data Feed Service Providers</a></li>
                    <br><br>
                    <li><input type="button" class="button" onclick="UpdateFilter()" value="Update Filter Parameters"/></li>
                </ul>
            </div>
        </form>
    </div>
</div>
</body>
</html>
