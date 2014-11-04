<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");
    String error = null;

    //setup return page if action used
    String sId = request.getParameter("id");
    String groupParam = sId != null ? "?id="+sId : "";
    String returnPage = request.getRequestURI() + groupParam;

    int groupId = Integer.parseInt(sId);
    UserBean group = data.getGroupById(groupId);

    if (request.getParameter("submit") == null)
    {
        if (user.groupMemberLevel(group.getId()) < UserBean.GROUP_OWNER)
        {
            // We are not a member of the group kick out to main menu.
            out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
            return;
        }
    }
    else if (error == null)
    {
        try
        {
            group.setExposedJoin("true".equals(request.getParameter("exposedJoin")));
            group.setExposedGrouplist("true".equals(request.getParameter("exposedGrouplist")));
            group.setBanList(request.getParameter("banList"));
            data.updateGroup(group, user);
%>
<jsp:forward page="groups.jsp"></jsp:forward>
<%
        }
        catch (DataError e)
        {
            error = e.getMessage();
        }
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap-theme.min.css">
    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/js/bootstrap.min.js"></script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
    if (error != null)
	{ 
%>		<div class="error"><%= error %></div>
<%
    }
%>
	<div class="form" style="width: 700px">
	<form method="post" action="editgroup.jsp">
	<input type="hidden" name="submit" value="true"/>
	<input type="hidden" name="id" value="<%= group.getId() %>"/>
	<input type="hidden" name="readAccessKey" value="<%= group.getReadAccessKey() %>"/>
	<input type="hidden" name="writeAccessKey" value="<%= group.getWriteAccessKey() %>"/> 
	<table>
	<caption>Edit Flight Group</caption>
	<tr>
		<td>Name</td><td><input name="name" type="text" class="textarea" value="<%= group.getName().replaceAll("\"","''") %>" size="40"/></td>
	</tr>

	<tr>
		<td>Comment</td><td><input name="comment" type="text" class="textarea" value="<%= group.getComment() == null?"" : group.getComment() %>" size="40"/></td>
	</tr>
	<tr>
		<td>Url of website</td><td><input name="url" type="text" class="textarea" value="<%= group.getUrl() == null?"":group.getUrl() %>" size="40"/></td>
	</tr>	
	<tr>
		<td>Default pilot fee</td><td><input name="defaultPilotFee" type="text" class="textarea" value="<%= group.getDefaultPilotFee() %>" size="5"/> 
		%</td>
	</tr>
	<tr>
		<td>Rental Ban List</td>
		<td colspan="5"><input name="banList" type="text" class="textarea" value="<%= group.getBanList() %>" maxlength="255" size="80" /> <br>* names separated by a space</td>
	</tr>
				
	<tr><td colspan="2"><input type="checkbox" name="exposedJoin" value="true" <%= group.isExposedJoin()?"checked" : "" %>>Group is private</td></tr>
	<tr><td colspan="2"><input type="checkbox" name="exposedGrouplist" value="true" <%= group.isExposedGrouplist()?"checked" : "" %>>Show group in group overview</td></tr>
	<tr><td><input type="submit" class="button" value="Update"/></td></tr>
	
	</table>
	</form>
	</div>

    <jsp:include flush="true" page="serviceaccess.jsp">
        <jsp:param name="groupId" value="<%=group.getId()%>" />
        <jsp:param name="returnpage" value="<%=returnPage%>" />
    </jsp:include>

</div>
</div>
</body>
</html>
