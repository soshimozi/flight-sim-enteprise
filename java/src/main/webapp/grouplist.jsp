<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    String returnPage = "groups.jsp";
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css" rel="stylesheet">
    <link href="css/redmond/jquery-ui.css" rel="stylesheet"/>
    <link href="css/Master.css" rel="stylesheet"/>

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
    <script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>

    <script type="text/javaScript">
        function doSubmit(id, event, action, form, groupname)
        {
            if (event === "joingroup" && !confirm("Sending a request to join [" + groupname + "]\n\nAre you sure?")) {
                this.event.preventDefault()
                return false;
            }
            else {
                form.id.value = id;
                form.event.value = event;
                form.action.value = action;
                form.submit();
            }
        }
    </script>

</head>

<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
    <div class="content">
        <div class="dataTable">
            <form method="post" action="userctl" name="groupForm">
                <input type="hidden" name="action" />
                <input type="hidden" name="event" />
                <input type="hidden" name="id"/>
                <input type="hidden" name="returnpage" value="grouplist.jsp" />

                <table>
                    <caption>Groups</caption>
                    <thead>
                    <tr>
                        <th>Action</th>
                        <th>Name</th>
                        <th>Description</th>
                    </tr>
                    </thead>
                    <tbody>
<%
    List<UserBean> groups = Accounts.getAllExposedGroups();
    Groups.reloadMemberships(user);

    for (UserBean group : groups)
    {
        int id = group.getId();
        String name = group.getName();
        String url = group.getUrl();
        if (url != null)
            url = "<a href=\"" + url + "\" target=\"_blank\">" + name + "</a>";
        else
            url = name;

%>
                    <tr>
                        <td style="height:38px; vertical-align: middle">
<%
        int memberLevel = user.groupMemberLevel(id);
        if (memberLevel == -1 && group.isExposedJoin())
        {
%>
                            <button class="btn btn-default" style="margin:2px;" onclick="doSubmit(<%= id %>, 'joingroup', '', this.form, <%= "'" + Converters.escapeJavaScript(name.replaceAll("\"" , "''")) + "'" %>)">Join</button>
<%
        }
        else if(memberLevel == UserBean.GROUP_REQUEST)
        {
%>
                            Requested
<%
        }
        else if(memberLevel == UserBean.GROUP_INVITED)
        {
%>
                            Invited
<%
        }
%>
                        </td>
                        <td style="width: 250px;vertical-align: middle;"><%= url %></td>
                        <td style="vertical-align: middle;"><%= group.getComment() %></td>
                    </tr>
<%
    }
%>
                    </tbody>
                </table>
            </form>
        </div>
    </div>
</div>

</body>
</html>
