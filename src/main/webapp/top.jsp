<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	boolean isTestServer;
	if(System.getProperty("isTestServer") == null)
	{
		isTestServer = request.getRequestURL().toString().contains("8080");
		System.setProperty("isTestServer", isTestServer ? "true" : "false");
	}
	else
		isTestServer = 	"true".equals(System.getProperty("isTestServer"));

%>

<div class="header">
    <div style="background-color: lightcoral; color: #ffffff; display: <%=isTestServer ? "block" : "none"%>">
        Test Server!!
    </div>
<div id="login-block">
<%
	if (!user.isLoggedIn())
	{
%>
	<script type="text/javascript">
		function submit()
		{
			document.loginform.action="<%= response.encodeURL("/requestnewpassword.jsp") %>";
			document.loginform.submit();
		}
	</script>
	
	<form name="loginform" method="post" action="userctl">
		<div>
			<input type="hidden" name="offset" id="offset" value=""/>
		</div>
		
		<script type="text/javascript">
			document.getElementById("offset").value = (new Date()).getTimezoneOffset()/60 * (-1);
		</script>
	
		<div class="form-item inline">
			Username:<input type="text" maxlength="64" class="textarea" name="user"	id="name" value="" /><br>
			Password:  <input type="password" class="textarea" name="password" value="" /><br>
		</div>
		<div>
			I will follow the <a class="normal" href="http://fseconomy.net/tos" target="_blank">Rules of Fair Play</a><br>
		</div> 
		<div style="float:right;">
			<input type="submit" name="event" width="30" height="25" style="margin-top:7px;" value="Agree & Log in" /><br>
			<input type="button" onClick="location.href='requestnewpassword.jsp';" style="margin-top:8px;" width="30" class="button" value="Forgot Password" />
		</div>
	</form>
<%
	} 
	else 
	{
		Banking.reloadMoney(user);
		int hours = 48;
		double totalhours;
		String stotalhours;

        totalhours = Stats.getInstance().getNumberOfHours(user.getId(), hours);
        stotalhours = Formatters.twoDecimals.format(totalhours);
%>
	<form class="top" method="post" action="/userctl">
		<strong>Logged in as</strong>
		<span class="text"><jsp:getProperty name="user" property="name"/></span><br>
		<strong>Cash Balance:</strong> <span class="text"><%= Formatters.currency.format(user.getMoney()) %></span><br>
	    <strong>Bank Balance:</strong> <span class="text"><%= Formatters.currency.format(user.getBank())%></span><br>
<% 		
		if(totalhours > 30)
		{
%>
	 		<span class="warning"><strong>HOURS EXCEEDED! </strong></span><a class="normal" href="<%= response.encodeURL("hours.jsp") %>"> <%= stotalhours %></a> in last 48
<% 		
		} 
		else 
		{
%>
	 		<strong>Hours Flown: </strong><a class="normal" href="<%= response.encodeURL("hours.jsp") %>"> <%= stotalhours %></a> in last 48
<%
		}
%>
		<br>
		<input type="submit" name="event" class="button" value="Log out" />
	</form>
<%	
	}
%>
</div>
<div id="banner">
	<img src="/img/spacer.gif" border="0" />
</div>
</div>

