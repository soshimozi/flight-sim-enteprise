<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*" %>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<%
    UserBean Accounts[] = data.getAllExposedGroups();
%>
	<select name="transferTo" id="transferTo" class="formselect">
		<option class="formselect" value=""></option>                                
<%	for ( int c = 0; c < Accounts.length; c++ ) 
    { 
%>
        <option class="formselect" value="<%=Accounts[c].getId()%>"><%= Accounts[c].getName()%></option>
<%
    }
%>
    </select>
