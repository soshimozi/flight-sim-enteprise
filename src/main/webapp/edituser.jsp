<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.* "
%>
<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<%
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

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<div class="content">
<%
    String message = (String) request.getAttribute("message");
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
                <label for="email">email: </label>
                <input id="email" name="email" type="text" class="textarea" value="<%= user.getEmail()== null ? "" : user.getEmail() %>" size="30" maxlength="45" />
            </div><br>
            <div>
                <div>
                    Show payments to self in logs:<br>
                    <input type="radio" name="showPaymentsToSelf" id="r1" value="1" <%= user.getShowPaymentsToSelf() ? "checked" : "" %> /><label for="r1">Show</label>
                    <input type="radio" name="showPaymentsToSelf" id="r2" value="0" <%= !user.getShowPaymentsToSelf() ? "checked" : "" %>/><label for="r1">Hide</label>
                </div>
            </div><br>
            <div>
                <div>
                    Show log times in:<br>
                    <input type="radio" name="selectedTimeZone" id="r10" value="0" <%= user.getUserTimezone() == 0 ? "checked" : "" %> /><label for="r1">GMT</label>
                    <input type="radio" name="selectedTimeZone" id="r20" value="1" <%= user.getUserTimezone() != 0 ? "checked" : "" %>/><label for="r1">Local (<%= gmtOffset %>)</label>
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
</div>
</div>
</body>
</html>
