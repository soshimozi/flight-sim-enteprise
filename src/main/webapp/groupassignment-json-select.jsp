<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");
%>

<%
    List<UserBean> accounts = data.getAllExposedGroups();
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
