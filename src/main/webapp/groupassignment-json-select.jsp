<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
%>

<%
    List<UserBean> accounts = Accounts.getAllExposedGroups();
%>
	<select name="transferTo" id="transferTo" class="formselect">
		<option class="formselect" value=""></option>                                
<%	for (UserBean account : accounts)
    { 
%>
        <option class="formselect" value="<%=account.getId()%>"><%= account.getName()%></option>
<%
    }
%>
    </select>
