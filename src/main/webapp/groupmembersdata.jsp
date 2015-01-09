<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>
<%@ page import="net.fseconomy.beans.UserBean" %>
<%@ page import="net.fseconomy.data.Accounts" %>
<%@ page import="java.util.List" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
  int groupId = Integer.parseInt(request.getParameter("groupid"));
  List<UserBean> members = Accounts.getUsersForGroup(groupId);
%>
<select id="transferto" name="transferto">
<%
    for(UserBean member: members)
    {
%>
    <option value="<%=member.getId()%>"><%=member.getName()%></option>
<%
    }
%>

</select>
