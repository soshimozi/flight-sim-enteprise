<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.util.Helpers "
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

    String returnToPage = request.getRequestURI();

    int offset = user.getTimeZone().getRawOffset();
    String gmtOffset = String.format("%s%02d:%02d", offset >= 0 ? "+" : "", offset / 3600000, (offset / 60000) % 60);

%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap-theme.min.css">
    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/js/bootstrap.min.js"></script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
    String message = Helpers.getSessionMessage(request);
    if (message != null)
    {
%>
        <div class="error"><%= message %></div>
<%
    }
%>
	<div class="form" style="width: 700px">
	<form method="post" action="userctl">
        <div>
            <input type="hidden" name="event" value="updateAcct"/>
            <input type="hidden" name="returnpage" value="<%=returnToPage%>"/>

            <h3>Edit preferences</h3>
            <div>
                <label>email: </label>
                <input id="email" name="email" type="text" class="textarea" value="<%= user.getEmail()== null ? "" : user.getEmail() %>" size="30" maxlength="45" />
            </div><br>
            <div>
                <div>
                    Show payments to self in logs:<br>
                    <input type="radio" name="showPaymentsToSelf" id="r1" value="1" <%= user.getShowPaymentsToSelf() ? "checked" : "" %> /><label>Show</label>
                    <input type="radio" name="showPaymentsToSelf" id="r2" value="0" <%= !user.getShowPaymentsToSelf() ? "checked" : "" %>/><label>Hide</label>
                </div>
            </div><br>
            <div>
                <div>
                    Show log times in:<br>
                    <input type="radio" name="selectedTimeZone" id="r10" value="0" <%= user.getUserTimezone() == 0 ? "checked" : "" %> /><label>GMT</label>
                    <input type="radio" name="selectedTimeZone" id="r20" value="1" <%= user.getUserTimezone() != 0 ? "checked" : "" %>/><label>Local (<%= gmtOffset %>)</label>
                </div>
            </div><br>
            <div>
                <div>
                    Rental Ban List (*):
                    <input name="banList" type="text" class="textarea" value="<%= user.getBanList() == null ? "" : user.getBanList() %>" maxlength="255" size="80" /> <br/>
                    * names separated by a space
                </div>
            </div>
            <div class="formgroup">
                <input type="submit" class="button" value="Update"/>
            </div>
            </div>
        </form>
	</div>

    <jsp:include flush="true" page="serviceaccess.jsp">
        <jsp:param name="returnpage" value="<%=returnToPage%>" />
    </jsp:include>

</div>
</div>
</body>
</html>
