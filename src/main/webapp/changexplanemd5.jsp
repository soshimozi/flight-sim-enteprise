<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>

<%
    Data data = (Data)application.getAttribute("data");
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<div class="content">
<%
	
	String message = (String) request.getAttribute("message");
	if (message != null) 
	{
%>
	<div class="message"><%= message %></div>
<%
	} 
	else
	{
		String qry = "SELECT svalue FROM sysvariables where variablename='XPlaneScriptMD5'";
		String currMD5 = (String)data.dalHelper.ExecuteScalar(qry);
%>


	<div class="form" style="width: 400px">
	<h2>Update XPlane MD5</h2>
	
	<form method="post" action="userctl">
	<div class="formgroup">
	Current MD5:
	<input name="MD5" type="text" class="textarea" size="50" id="MD5" value="<%=currMD5%>" />
	<br/> 
	Passcode (required to update):
	<input name="passcode" type="text" class="textarea" size="50" id="passcode" /> 
	<br/>
	</div>

	<div class="formgroup">
	<input type="submit" class="button" value="Update" />
	<input type="hidden" name="event" value="updateXPlaneMD5"/>
	<input type="hidden" name="return" value="changexplanemd5.jsp"/>
	</div>
	</form>
	</div>
<%
}
%>
</div>

</div>
</body>
</html>
