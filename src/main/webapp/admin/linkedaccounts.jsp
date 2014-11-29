<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, java.util.List, net.fseconomy.dto.LinkedAccount, net.fseconomy.beans.UserBean"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    List<LinkedAccount> list = Accounts.getLinkedAccountList();
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

        <h2>Linked Account List</h2>

        <div class="dataTable">
            <a href="/admin/admin.jsp">Return to Admin page</a><br/>
            <table id="sortableTableStats" class="sortable">
                <thead>
                <tr>
                    <th>Link Set</th>
                    <th>Account Name</th>
                    <th>Status</th>
                    <th>Notes</th>
                </tr>
                </thead>

                <tbody>
                <%
                    for (LinkedAccount item : list)
                    {
                %>
                <tr>
                    <td>
                        <%= item.linkId %>
                    </td>
                    <td>
                        <%= item.getAccountName() %>
                    </td>
                    <td>
                        <%= item.getStatus() %>
                    </td>
                    <td>
                        <a href="accountnotes.jsp?id=<%=item.accountId%>">See Account notes</a>
                    </td>
                </tr>
                <%
                    }
                %>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
