<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.Helpers"
%>
<%@ page import="net.fseconomy.beans.UserBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    String returnPage = request.getRequestURI();
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css">
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="../scripts/jquery.min.js"></script>
    <script src="../scripts/jquery-ui.min.js"></script>
    <script src="../scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#username", "#user", <%= Accounts.ACCT_TYPE_PERSON %>)
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	String message = Helpers.getSessionMessage(request);
	if (message != null)
    {
%>
	    <div class="message"><%= message %></div>
<%
    }
%>

<%	
    if (request.getParameter("submit") == null || (message != null))
    {
%>
	<h2>Enter User Account</h2>
	<div class="form" style="width: 400px">
        <form method="post">
            <div>
                <input type="hidden" name="submit" value="true"/>
                <input type="hidden" name="return" value="/admin/accountedit.jsp"/>
            </div>
            <tr>
                <td>
                    Account Name :
                    <input type="hidden" id="user" name="user" value=""/>
                    <input type="text" id="username" name="username"/>	</td>
            </tr><br/>
            <tr>
                <input type="submit" class="button" value="GO" />
            </tr>
        </form>
	</div>
<%	
    }
    else if (request.getParameter("submit") != null)
    {
        int userid = Integer.parseInt(request.getParameter("user"));
        UserBean edituser = Accounts.getAccountById(userid);
        if (edituser == null)
        {
            message = "User Not Found";
        }
%>
<%
        if (message != null)
	    {
%>
        <div class="message"><%= message %></div>
<%
        }
%>
<%	    if (edituser != null)
	    {
%>		<h2>Edit User Account</h2>
	
	    <div class="form" style="width: 600px">
		<form method="post" action="/userctl">
            <div>
                <input type="hidden" name="event" value="editUser"/>
                <input type="hidden" name="return" value="/admin/accountedit.jsp"/>
                <input type="hidden" name="user" value="<%=edituser.getName()%>"/>
                <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
            </div>
		    <table>
                <tr>
                    <td>User Name: </td>
                    <td><input name="newuser" type="text" class="textarea" size="10" value ="<%=edituser.getName()%>" /></td>
                </tr>
                <tr>
                    <td>Email: </td>
                    <td><input name="email" type="text" class="textarea" size="40" value = "<%=edituser.getEmail()%>" /></td>
                </tr>
                <tr>
                    <td>Exposure: </td>
                    <td>
                        <select name="exposure" class="formselect">
                            <option class="formselect" value=<%= edituser.getExposure() %> ><%= edituser.getExposure() == 0 ? "Hidden" : "Visible" %></option>
                            <option class="formselect" value=<%= edituser.getExposure() == 0 ? 2 : 0 %>><%= edituser.getExposure() == 0 ? "Visible" : "Hidden" %></option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>New Password: </td>
                    <td><input name="password" type="text" class="textarea" size="40" value = "" /></td>
                </tr>
                <tr>
                    <td><input type="submit" class="button" value="Update" /></td>
                </tr>
            </table>
		</form>
		<ul class="footer">
            Notes:<br/>
            <li>Changing user names should only be done in rare situations.</li>
            <li>After changing exposure wait for maintenance cycle to see results on stats page.</li>
            <li>Leaving New Password blank will cause the current password to remain unchanged.</li>
		</ul>
		</div>	
<%
	    }
%>
<%
    }
%>
    </div>
</div>
</body>
</html>
