<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.*"
%>
<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script type="text/javaScript">
        function doSubmit(id, event, groupname)
        {
            if (event == "deletegroup" && !confirm("Do you want to delete Flight Group " + groupname + "?"))
                return;

            if (event == "cancelgroup" && !confirm("Do you want to leave Flight Group " + groupname + "?"))
                return;

            groupForm.id.value = id;
            groupForm.event.value = event;
            groupForm.action = "userctl";
            groupForm.submit();
        }

        function doSubmit2(id)
        {
            groupForm.id.value = id;
            groupForm.action = "editgroup.jsp";
            groupForm.submit();
        }

        function doSubmit3(id, action)
        {
            inviteForm.id.value = id;
            inviteForm.action.value = action;
            inviteForm.submit();
        }
    </script>

</head>

<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<div class="content">
<%
	UserBean invitations[] = data.getGroupsThatInviteUser(user.getId());
	if (invitations.length > 0) 
	{
%>
<div class="dataTable">
	<form method="post" action="userctl" name="inviteForm">
	<input type="hidden" name="event" value="invitation" />
	<input type="hidden" name="action" />
	<input type="hidden" name="id"/>
	<input type="hidden" name="return" value="groups.jsp" />
	
	<table>
	<caption>Invitations</caption>
	<thead>
	<tr>
		<th>Name</th>
		<th>Description</th>
		<th>Action</th>
	</tr>
	</thead>
	<tbody>
<%
		for (int c=0; c < invitations.length; c++)
		{
			int id = invitations[c].getId();
%>
	<tr>
	<td><%= invitations[c].getName() %></td>
	<td><%= invitations[c].getComment() %></td>
	<td>
		<a class="link" href="javascript:doSubmit3(<%= id %>, 'accept')">Accept</a>
		<a class="link" href="javascript:doSubmit3(<%= id %>, 'delete')">Delete</a>
	</td>
	</tr>
<%
		}
%>
	</tbody>
	</table>	
	</form>
</div>
<% 
	}
%>
<div class="dataTable">	
<%
	boolean allGroups = request.getParameter("all") != null;
	
	UserBean[] groups = allGroups ? data.getAllExposedGroups() : data.getGroupsForUser(user.getId());
%>
	<form method="post" name="groupForm">
	<input type="hidden" name="event" />
	<input type="hidden" name="id"/>
	<input type="hidden" name="return" value="groups.jsp" />
	
	<table>
	<caption>Groups</caption>
	<thead>
	<tr>
		<th>Name</th>
		<th>Description</th>
		<th>Action</th>
	</tr>
	</thead>
	<tbody>
<%
	data.reloadMemberships(user);

	for (int c=0; c < groups.length; c++)
	{
		int id = groups[c].getId();
		String name = groups[c].getName();
		String url = groups[c].getUrl();
		if (url != null)
			url = "<a href=\"" + url + "\" target=\"_blank\">" + name + "</a>";
		else
			url = name;
				
%>
	<tr <%= Data.oddLine(c) %>>
	<td><%= url %></td>
	<td><%= groups[c].getComment() %></td>
	<td>
<%
		int memberLevel = user.groupMemberLevel(id);
		if (memberLevel == -1) 
		{
			if (!groups[c].isExposedJoin()) 
			{
%>
				<a class="link" href="javascript:doSubmit(<%= id %>, 'joingroup')">Join</a>
<% 			} 
		} 
		else 
		{ 
			if (memberLevel == UserBean.GROUP_OWNER ) 
			{
%>				<a class="link" href="javascript:doSubmit2(<%= id %>)">Edit</a>
				<a class="link" href="javascript:doSubmit(<%= id %>, 'deletegroup', <%= "'" + Converters.escapeJavaScript(name.replaceAll("\"" , "''")) + "'" %>)">Delete</a>
<%  		} 
			else 
			{ 
%>				<a class="link" href="javascript:doSubmit(<%= id %>, 'cancelgroup', <%= "'" + Converters.escapeJavaScript(name.replaceAll("\"" , "''")) + "'" %>)">Leave</a>
<%  		} 
		}
%>
	</td>
	</tr>
<%	}
%>
	</tbody>
	</table>	
	</form>
	<form method="post" action="creategroup.jsp" name="newgroupForm">
		<div class="formgroup">
		<input name="newgroup" type="submit" class="button" value="New group"/>
		</div>
	</form>
</div>
</div>
</div>
</body>
</html>
