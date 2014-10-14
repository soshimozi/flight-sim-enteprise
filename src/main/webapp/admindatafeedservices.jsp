<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*,java.util.*,java.text.*"
%>
<%@ page import="net.fseconomy.util.Formatters" %>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
<%
    if (!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
        return;
    }

    String error = "";

    Date date = new Date();

    int id = -1;
    String action = "";
    if(request.getParameter("id") != null)
    {
        id = Integer.parseInt(request.getParameter("id"));
        action = request.getParameter("action").toLowerCase();

        ServiceProviderBean service = data.getServiceProviderById(id);

        if(action.equals("approve"))
        {
            service.setStatus(ServiceProviderBean.STATUS_ACTIVE);
        }
        if(action.equals("disable"))
        {
            service.setStatus(ServiceProviderBean.STATUS_DISABLED);
        }
        if(action.equals("reject"))
        {
            service.setStatus(ServiceProviderBean.STATUS_REJECTED);
        }
        if(action.equals("ban"))
        {
            service.setStatus(ServiceProviderBean.STATUS_BANNED);
        }
        String notes = Formatters.getUserTimeFormat(user).format(date) + " (" + user.getName() + ")" + " - Status changed to: " + action + "\n" + service.getNotes();
        service.setNotes(notes);
        data.updateServiceProvider(service);

        String msg = "This is a notification that the service for: " + service.getName() + " has had a status change.\nThe new status is: " + service.getStatusString() + ".\nIf you have any questions please contact administrator@fseconomy.com\n\nThis is an automated notice.";
        try
        {
            data.doServiceProviderNotification(service, "FSE - Service Status Change", msg, false);
        }
        catch (DataError e)
        {
            error = e.getMessage();
        }
        out.print("<script type=\"text/javascript\">document.location.href=\"admindatafeedservices.jsp\"</script>");
        return;
    }
    //setup for display
    //get the current service providers
    //presorted by status, owner
    ServiceProviderBean[] services = data.getServiceProviders();
%>
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script type="text/javascript">
        function doAction(form)
        {
            var val = form.options[form.selectedIndex].value;
            var sval = val.split("|");
            var action = sval[0];
            var id = sval[1];

            if( action == "edit")
            {
                var url = "admindatafeedserviceedit.jsp?id=" + id;
                location.href = url;
            }
            else if( action == "approve")
            {
                if (window.confirm("Are you sure you want to APPROVE this request?"))
                {
                    var url = "admindatafeedservices.jsp?id=" + id + "&action=approve";
                    location.href = url;
                }
            }
            else if( action == "disable")
            {
                if (window.confirm("Are you sure you want to DISABLE this service?"))
                {
                    var url = "admindatafeedservices.jsp?id=" + id + "&action=disable";
                    location.href = url;
                }
            }
            else if( action == "reject")
            {
                if (window.confirm("Are you sure you want to REJECT this request?"))
                {
                    var url = "admindatafeedservices.jsp?id=" + id + "&action=reject";
                    location.href = url;
                }
            }
            else if( action == "ban")
            {
                if (window.confirm("Are you sure you want to BAN this service?"))
                {
                    var url = "admindatafeedservices.jsp?id=" + id + "&action=ban";
                    location.href = url;
                }
            }
        }
    </script>
</head>

<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<%
	if (error != null) 
	{ 
%>
	<div class="error"><%= error %></div>
<%	
	}
%>
<div class="content">
	<h2>Admin - Service Providers</h2>
	<div class="textarea" style="width: 800px">
	Note: Status changes such as approving, rejecting, disabling, or banning all send an 
	email to the owner (and the alternate if entered) of the status change. They are directed
	to contact administrator@fseconomy.com with any questions.<br/><br/>
	
	Once the service is approved and active, the owner can make basic changes such as updating
	the url, ip, or description. All other options are disabled for them, however Admin can edit
	any field as needed.
	</div>
	<div class="form" style="width: 800px">
	<table border="1" cellpadding="5">
		<thead>
			<tr>
				<td>Action</td>
				<td>Status</td>
				<td>Name</td>
				<td>Owner</td>
				<td>Alt</td>
				<td>Key</td>
			</tr>
		</thead>
<%
		for (int c=0; c< services.length; c++)
		{ 
%>
		<tr>
			<td>
				<select name="primarycontact" class="formselect"  onchange = "doAction(this)">
					<option value="" >Choose an action </option>
					<option value="edit|<%=services[c].getId() %>" >View/Edit</option>
					<option value="approve|<%=services[c].getId() %>" >Approve</option>
					<option value="reject|<%=services[c].getId() %>" >Reject</option>
					<option value="disable|<%=services[c].getId() %>" >Disable</option>
					<option value="ban|<%=services[c].getId() %>" >Ban</option>
				</select>
			</td>
			<td>
				<%=services[c].getStatusString()%>
			</td>
			<td>
				<%=services[c].getName()%>
			</td>
			<td>
				<%=services[c].getOwnerName()%>
			</td>
			<td>
				<%=services[c].getAlternateName()%>
			</td>
			<td>
				<%=services[c].getKey()%>
			</td>
		</tr>
<%
		}
%>		
	</table>
	</div>
</div>

</div>

</body>
</html>
