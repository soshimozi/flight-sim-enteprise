<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*"
%>
<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    String content;
    String style = "";
    String sId = request.getParameter("id");
    AircraftMaintenanceBean maintenance = null;
    int id;

    try
    {
        if (sId != null)
        {
            id = Integer.parseInt(sId);
            maintenance = Aircraft.getMaintenance(id);
        }

        if (maintenance == null)
            throw new DataError("No record found.");

        content = "<div class=\"maintenanceLog\">" + maintenance.report() + "</div>";
        style="style=\"background:url(" + maintenance.getFbo().getInvoiceBackground()+ "); background-repeat:no-repeat; height:755px\"";
    }
    catch (DataError e)
    {
        content = "<div class=\"error\">" + e.getMessage() + "</div>";
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body <%= style %>>

<div id="wrapper">
<div class="content">
	<%= content %>
</div>
</div>
</body>
</html>
