<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
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

    String returnPage = request.getHeader("referer");

    int userId = Integer.parseInt(request.getParameter("userid"));
    UserBean edituser = Accounts.getAccountById(userId);

    if(!returnPage.contains("userid"))
        returnPage += "?userid=" + userId;
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
    <div class="content">
		<h2>Edit User Account</h2>
	
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
<%
    if(Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
                <tr>
                    <td>Level: </td>
                    <td>
                        <select name="level" class="formselect">
                            <option class="formselect" value="none" <%=edituser.getLevel() == UserBean.LEV_NONE ? "selected" : ""%>>None</option>
                            <option class="formselect" value="active" <%=edituser.getLevel() == UserBean.LEV_ACTIVE ? "selected" : ""%>>Active</option>
                            <option class="formselect" value="aca" <%=edituser.getLevel() == UserBean.LEV_ACA ? "selected" : ""%>>ACA</option>
                            <option class="formselect" value="csr" <%=edituser.getLevel() == UserBean.LEV_CSR ? "selected" : ""%>>CSR</option>
                            <option class="formselect" value="moderator" <%=edituser.getLevel() == UserBean.LEV_MODERATOR ? "selected" : ""%>>Moderator</option>
                        </select>
                    </td>
                </tr>
<%
    }
%>
                <tr>
                    <td>New Password: </td>
                    <td><input name="password" type="text" class="textarea" size="40" value = "" /></td>
                </tr>
                <tr>
                    <td><input type="submit" class="button" value="Update" /></td>
                </tr>
            </table>
		</form>
        Notes:<br>
		<ul class="footer">
            <li>Changing user names should only be done in rare situations.</li>
            <li>After changing exposure wait for maintenance cycle to see results on stats page.</li>
            <li>Leaving New Password blank will cause the current password to remain unchanged.</li>
		</ul>
		</div>	
    </div>
</div>
</body>
</html>
