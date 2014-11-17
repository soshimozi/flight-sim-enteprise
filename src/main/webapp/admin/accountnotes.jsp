<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, java.util.List, net.fseconomy.dto.AccountNote, net.fseconomy.beans.UserBean"
        %>
<%@ page import="net.fseconomy.util.Formatters" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    List<AccountNote> list = null;
    String saccount = request.getParameter("id");
    if(saccount != null)
    {
        int accountId = Integer.parseInt(saccount);
        list = Accounts.getAccountNoteList(accountId);
    }
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
<%
    if(list == null || list.size() == 0)
    {
%>
        <b>Currently notes currently available.</b>
<%
    }
    else
    {
%>
        <h2>Account notes for <%=list.get(0).accountName%></h2>

        <div class="dataTable">
            <a href="/admin/admin.jsp">Return to Admin page</a><br/>
            <table id="sortableTableStats" class="sortable">
                <thead>
                <tr>
                    <th>Created by</th>
                    <th>Date</th>
                    <th>Note</th>
                </tr>
                </thead>

                <tbody>
                <%
                    for (AccountNote item : list)
                    {
                %>
                <tr>
                    <td>
                        <%= item.createdByName %>
                    </td>
                    <td>
                        <%= Formatters.dateyyyymmddhhmmss.format(item.created) %>
                    </td>
                    <td>
                        <%= item.note %>
                    </td>
                </tr>
                <%
                    }
                %>
                </tbody>
            </table>
        </div>
<%
    }
%>
    </div>
</div>
</body>
</html>
