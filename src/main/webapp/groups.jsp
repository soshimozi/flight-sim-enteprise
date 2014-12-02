<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    String returnPage = "groups.jsp";
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />
    <link href="css/bootstrap-theme.min.css" rel="stylesheet" type="text/css" />
    <link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css" />
    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="scripts/jquery.min.js"></script>
    <script src="scripts/jquery-ui.min.js"></script>
    <script src="scripts/bootstrap.min.js"></script>
    <script src="scripts/AutoComplete.js"></script>

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

        function doTransferSelect(id)
        {
            var form = document.getElementById("formTransferModal");
            form.groupid.value = id;

            $("#myModal").modal('show');
        }

        function doTransfer()
        {
            var form = document.getElementById("formTransferModal");
            form.submit();
        }

        $(function()
        {
            <%--$('#myModal').on('shown.bs.modal', function () {--%>
                <%--initAutoComplete("input[name='username']", "input[name='userid']", <%= Accounts.ACCT_TYPE_PERSON %>);--%>
            <%--})--%>
            initAutoComplete("#username", "#userid", <%= Accounts.ACCT_TYPE_PERSON %>);
        });


    </script>

</head>

<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	List<UserBean> invitations = Accounts.getGroupsThatInviteUser(user.getId());
	if (invitations.size() > 0)
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
		for (UserBean group : invitations)
		{
			int id = group.getId();
%>
	<tr>
	<td><%= group.getName() %></td>
	<td><%= group.getComment() %></td>
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
	
	List<UserBean> groups = allGroups ? Accounts.getAllExposedGroups() : Accounts.getGroupsForUser(user.getId());
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
    Groups.reloadMemberships(user);

	for (UserBean group : groups)
	{
		int id = group.getId();
		String name = group.getName();
		String url = group.getUrl();
		if (url != null)
			url = "<a href=\"" + url + "\" target=\"_blank\">" + name + "</a>";
		else
			url = name;
				
%>
	<tr>
	<td><%= url %></td>
	<td><%= group.getComment() %></td>
	<td>
<%
		int memberLevel = user.groupMemberLevel(id);
		if (memberLevel == -1) 
		{
			if (!group.isExposedJoin())
			{
%>
				<a class="link" href="javascript:doSubmit(<%= id %>, 'joingroup')">Join</a>
<% 			} 
		} 
		else 
		{ 
			if (memberLevel == UserBean.GROUP_OWNER ) 
			{
%>
                <a class="link" href="editgroup.jsp?id=<%= id %>">Edit</a>
                <a class="link" onclick="doTransferSelect(<%= id %>, '<%=name%>');">Transfer</a>
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

<!-- Modal HTML -->
<div id="myModal" class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Transfer Group</h4>
            </div>
            <div class="modal-body">
                <div class="panel panel-danger">
                    <div class="panel-heading">
                        <h3 class="panel-title">Warning!</h3>
                    </div>
                    <div class="panel-body">
                        This will transfer all group assets including money, aircraft, and FBOs to the new owner.
                        Triple check that you have selected the correct pilot to transfer the group. If you make a mistake that will be between you and the person that you transferred the group to.
                    </div>
                </div>
                <form id="formTransferModal" method="post" action="userctl" class="ui-front">
                    <input type="hidden" name="event" value="transfergroup"/>
                    <input type="hidden" name="groupid" value=""/>
                    <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                    <div>
                        Enter Account:
                        <input type="hidden" id="userid" name="userid" value="-1"/>
                        <input type="text" id="username" name="username" placeholder="Type Pilot Name here"/>
                        <br/>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary" onclick="doTransfer();">Transfer</button>
            </div>
        </div>
        </div>
    </div>
</div>

</body>
</html>
