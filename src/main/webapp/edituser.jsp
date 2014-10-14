<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.* "
%>
<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<%
    String error = null;
    if (request.getParameter("submit") != null)
    {
        try
        {
            //user.setExposedScore("true".equals(request.getParameter("exposedScore")));

            user.setEmail(request.getParameter("email"));
            user.setDateFormat(Integer.parseInt(request.getParameter("dateformat")));
            user.setShowPaymentsToSelf(Integer.parseInt(request.getParameter("paymentstoself")) == 1);
            user.setBanList(request.getParameter("banList"));
            data.updateUser(user);
            error = "Changes saved.";
        } catch (DataError e)
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

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<div class="content">
<%
    if (error != null)
    {
%>
	<div class="error"><%= error %></div>
<%
    }
%>
	<div class="form" style="width: 700px">
	<form method="post" action="edituser.jsp">
	<div>
	<input type="hidden" name="submit" value="true"/>
	<input type="hidden" name="id" value="<%= user.getId() %>"/>
	<table>
	<caption>Edit preferences</caption>
	<tr>
		<td>Email</td>
		<td><input name="email" type="text" class="textarea" value="<%= user.getEmail() %>" size="30" maxlength="45" /></td>
	</tr>
	<tr>
		<td>Payments to self in payment log</td>
		<td>
			<select class="formselect" name="paymentstoself">
				<option value="0" <%= !user.getShowPaymentsToSelf() ? "selected" : "" %> >Hide</option>
				<option value="1" <%= user.getShowPaymentsToSelf() ? "selected" : "" %> >Show</option>
			</select>
		</td>
	</tr>
	<tr>
		<td>Show time in:</td>
		<td>
			<select class="formselect" name="dateformat">
				<option value="0" <%= user.getUserTimezone() == 0 ? "selected" : "" %> >GMT</option>
				<option value="1" <%= user.getUserTimezone() != 0   ? "selected" : "" %> >Local</option>
			</select>
		</td>
	</tr>
	<tr>
		<td>
			Rental Ban List <br/>
		</td>
		<td colspan="5">
			<input name="banList" type="text" class="textarea" value="<%= user.getBanList() %>" maxlength="255" size="80" /> <br/>
			* names separated by a space
		</td>
	</tr>
	</table>
	<div class="formgroup">
		<input type="submit" class="button" value="Update"/>
	</div>
	</div>
	</form>
	</div>
</div>
</div>
</body>
</html>
