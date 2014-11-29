<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*,net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />
<jsp:useBean id="group" class="net.fseconomy.beans.UserBean">
    <jsp:setProperty name="group" property="*"/>
</jsp:useBean>

<%
    String error = null;

    //initial page load
    if (request.getParameter("submit") == null)
    {
        group = new UserBean();
        group.setId(-1);
        group.setName("");
        group.setComment("");
        group.setExposedGrouplist(true);
        group.setExposedScore(true);
        group.setExposedJoin(true);
        group.setBanList("");
    }
    else if (error == null) //submitted new group
    {
        try
        {
            group.setExposedJoin("true".equals(request.getParameter("exposedJoin")));
            group.setExposedGrouplist("true".equals(request.getParameter("exposedGrouplist")));
            group.setBanList(request.getParameter("banList"));
            Accounts.CreateGroup(group, user);
%>
    <jsp:forward page="groups.jsp" />
<%
            return;
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

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">

<div class="content">
<% 	if (error != null) 
	{ 
%>		<div class="error"><%= error %></div>
<%	}
%>
	<div class="form" style="width: 700px">
	<form method="post" action="creategroup.jsp">
	<input type="hidden" name="submit" value="true"/>
	<input type="hidden" name="id" value="<%= group.getId() %>"/>
	<input type="hidden" name="readAccessKey" value="<%= group.getReadAccessKey() %>"/>
	<input type="hidden" name="writeAccessKey" value="<%= group.getWriteAccessKey() %>"/> 
	
	<table>
	<caption>New Flight Group</caption>
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
		<td>Default pilot fee</td><td><input name="defaultPilotFee" type="text" class="textarea" value="<%= group.getDefaultPilotFee() %>" size="5"/>%</td>
	</tr>
	<tr>
		<td>Rental Ban List</td>
		<td colspan="5"><input name="banList" type="text" class="textarea" value="<%= group.getBanList() %>" maxlength="255" size="80" /> <br>* names separated by a space</td>
	</tr>				
	<tr>
		<td colspan="2"><input type="checkbox" name="exposedJoin" value="true" <%= group.isExposedJoin()?"checked" : "" %>>Group is private</td>
	</tr>
	<tr>
		<td colspan="2"><input type="checkbox" name="exposedGrouplist" value="true" <%= group.isExposedGrouplist()?"checked" : "" %>>Show group in group overview</td>
	</tr>	
	<tr>
		<td><input type="submit" class="button" value="Update"/></td>
	</tr>
	
	</table>
	</form>
	</div>
</div>
</div>
</body>
</html>
