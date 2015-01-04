<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="net.fseconomy.beans.*, net.fseconomy.data.*"
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

	String returnPage = request.getHeader("referer");

	int id = Integer.parseInt(request.getParameter("id"));
	AircraftBean aircraft = Aircraft.getAircraftById(id);

	UserBean owneraccount;
	owneraccount = Accounts.getAccountById(aircraft.getOwner());
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="css/Master.css" rel="stylesheet" type="text/css" />

	<link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css" />
	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
	<script src="scripts/AutoComplete.js"></script>

	<script type="text/javascript">
	
		$(function() 
		{
			initAutoComplete("#lesseename", "#lessee", <%= Accounts.ACCT_TYPE_ALL %>);
		});
	
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<div class="form" style="width: 600px">
		<h2>Lease Aircraft </h2>
		<form method="post" action="userctl">
			<div>
				<input type="hidden" name="event" value="leaseAircraft"/>
				<input type="hidden" name="id" value="<%=aircraft.getId()%>"/>
				<input type="hidden" name="owner" value="<%= aircraft.getOwner() %>"/>
			    <input type="hidden" id="lessee" name="lessee" value=""/>
			    <input type="hidden" id="returnPage" name="returnpage" value="<%=returnPage%>"/>
			</div>
			<div class="formgroup high">
			  	<strong>Aircraft Registration:</strong> <%=aircraft.getRegistration()%><br /><br />
				<strong>Aircraft Make/Model:</strong> <%=aircraft.getMakeModel()%><br /><br />
				<strong>From:</strong> <%=owneraccount.getName()%><br /><br />				  
				
				<strong>To: </strong>
				<input type="text" id="lesseename" name="lesseename"/><br />
			</div>
					
			<div class="formgroup">
				<input type="submit" class="button" value="Set Lessee"/>
			</div>		
		</form>
		</div>
	</div>
</div>
</body>
</html>
