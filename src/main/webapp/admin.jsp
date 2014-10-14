<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%
    if(!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
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

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

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
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<div class="content">
<h2>Admin console</h2>
Database Maintenance status:<br/>
<%= MaintenanceCycle.status() %><br/>

<ul><b>Aircraft Utilities</b>
	<li><a href="fsmappings.jsp">Modify mappings of Flight Simulator aircraft</a></li>
	<li><a href="models.jsp">Modify aircraft models</a></li>
	<li><a href="admineditaircraft.jsp">Edit Aircraft Data</a></li>
	<li><a href="admineditshippedaircraft.jsp">Edit Shipped Aircraft</a></li>
</ul>
<ul><b>User Account Utilities</b>
	<li><a href="signup.jsp">Add User Account</a></li>
	<li><a href="admineditaccount.jsp">Edit User Account</a></li>
	<li><a href="lockaccount.jsp">Lock User Account</a></li>
	<li><a href="unlockaccount.jsp">Un-Lock User Account</a></li>
    <li><a href="adminuser48hourtrend.jsp">User 48 Hour Trend</a></li>
    <li><a href="adminfuelexploitcheck.jsp?numitems=100&pricepoint=10">Check for Fuel Exploit</a></li>
    <li><a href="adminclientipchecks.jsp">Client IP Checks</a></li>
    <li><a href="adminclientuseripduplicates.jsp">Client IPs used by multiple users</a></li>
</ul>
<ul><b>Other Utilities</b>
	<li><a href="admineditfbo.jsp">Transfer FBOs</a></li>
	<li><a href="admineditgoods.jsp">Adjust Goods</a></li>
	<li><a href="templates.jsp">Modify assignment templates</a></li>
	<li><a href="adminsignatures.jsp">Signature Templates</a></li>
	
</ul>
	<form method="post" action="admin.jsp" name="adminform">
	<div>
	<input type="hidden" name="issubmit" value="true"/>
	<input type="hidden" name="event" value="ResetFilter"/>

	<ul><b>Service/Ip Utilities</b>
		<li><a href="admindatafeedservices.jsp">Data Feed Service Providers</a></li>
        <br><br>
		<li><input type="button" class="button" onclick="UpdateFilter()" value="Update Filter Parameters"/></li>
	</ul>
	</div>
	</form>

</div>

</div>
</body>
</html>
