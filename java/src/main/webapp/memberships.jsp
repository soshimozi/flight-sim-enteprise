<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    String group = request.getParameter("groupid");
    int groupId = -1;
    if (group != null)
        groupId = Integer.parseInt(group);

    List<UserBean> members = Accounts.getUsersForGroup(groupId);
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css">
    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
    <script src="scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#membername", "#member", <%= Accounts.ACCT_TYPE_PERSON %>);
        });

    </script>

    <script type="text/javaScript">

        function doSubmit(id, level)
        {
            document.memberForm.id.value = id;
            document.memberForm.level.value = level;
            document.memberForm.submit();
        }

        function doSubmitDelete(id, name)
        {
            if (!confirm('Delete ' + name + ' from the group?'))
                return;
            document.memberForm.id.value = id;
            document.memberForm.event.value = "kickgroup";
            document.memberForm.submit();
        }

        function doSubmitReject(id, name)
        {
            if (!confirm('Reject ' + name + ' from the group?'))
                return;

            var msg = prompt("You must enter a short comment for rejection (TOS rules apply!):");
            if(msg == undefined || msg === "") {
                alert("Invalid message!");
                return;
            }

            document.memberForm.msg.value = msg;
            document.memberForm.id.value = id;
            document.memberForm.event.value = "groupreject";
            document.memberForm.submit();
        }

        function checkAll()
        {
            var state = document.memberForm.selectAll.checked;
            var list = document.memberForm.selected;
            for (i = 0; i < list.length; i++)
                list[i].checked = state;
        }

    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	<form method="post" action="userctl" name="memberForm">
	<input type="hidden" name="event" value="memberlevel" />
	<input type="hidden" name="level" />
	<input type="hidden" name="id"/>
    <input type="hidden" name="msg"/>
	<input type="hidden" name="groupid" value="<%= groupId %>"/>
	<input type="hidden" name="returnpage" value="memberships.jsp?groupid=<%= groupId %>" />
<div class="dataTable">		
	<table>
	<caption>Group memberships</caption>
	<thead>
	<tr>
        <th>Select</th>
		<th>Name</th>
		<th>Level</th>
		<th>Action</th>
	</tr>
	</thead>
	<tbody>
<%
	for (UserBean member : members)
	{
		int id = member.getId();
		String name = member.getName();
        Groups.reloadMemberships(member);
		int memberLevel = member.groupMemberLevel(groupId);
%>
	<tr>
        <td><input type="checkbox" name="selected" value="<%= id %>"/></td>
    	<td><%= member.getName() %></td>
	    <td><%= UserBean.getGroupLevelName(memberLevel) %></td>
	    <td>
<%
        if (memberLevel == UserBean.GROUP_REQUEST)
        {
%>
            <a class="link" href="javascript:doSubmitReject(<%= id %>, '<%= name %>')">Reject as member</a>
            <a class="link" href="javascript:doSubmit(<%= id %>, 'member')">Accept as member</a>
<%
        }
        if (memberLevel == UserBean.GROUP_INVITED)
        {
%>
	    <a class="link" href="javascript:doSubmitDelete(<%= id %>, '<%= name %>')">Delete Invitation</a>
<%
        }

	    if (memberLevel != UserBean.GROUP_REQUEST && memberLevel != UserBean.GROUP_INVITED)
        {
	        if (memberLevel != UserBean.GROUP_OWNER)
            {
	            if (memberLevel != UserBean.GROUP_STAFF)
                {
%>
            	<a class="link" href="javascript:doSubmit(<%= id %>, 'staff')">Change to staff</a>
<%
                }
                if (memberLevel != UserBean.GROUP_MEMBER)
                {
%>
	            <a class="link" href="javascript:doSubmit(<%= id %>, 'member')">Change to regular member</a>
<%
                }
%>
        	<a class="link" href="javascript:doSubmitDelete(<%= id %>, '<%= name %>')">Delete member</a>
	    </td>
<%
	        }
        }
%>
	</tr>
<%
    }
%>
	<tr>
        <td></td>
        <td></td>
        <td></td>
        <td><input name="selectAll" type="checkbox" onChange="checkAll()"> All</td>
    </tr>
	</tbody>
	</table>
	</div>

	<div class="form" style="width:400px">
        Pay a total of <input name="money" type="text" class="textarea" size="4" />
        to all selected members.
        <input type="submit" class="button" value="Pay"/> </br>
        Comment: <input name="comment" type="text" class="textarea" size="50" />
	</div>
	<br/>
	
	<div class="form" style="width:400px">
        Invite a new member:<br/>
        <input id="membername" name="membername" type="text" class="textarea" size="40" />
        <input type="hidden" id="member" name="member" />
        <input type="submit" class="button" value="Invite"/>
	</div>
	<br/>	

	<div class="form" style="width:400px">
        Send an email message to all selected members:
        <textarea name="email" cols="40" rows="10" class="textarea"></textarea>
        <input type="submit" class="button" value="Send"/>
	</div>
	<br/>
		
	</form>
</div>
</div>
</body>
</html>
