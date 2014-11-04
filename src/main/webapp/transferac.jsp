<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="net.fseconomy.data.* "
%>

<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

	String returnPage = request.getHeader("referer");

	String reg = request.getParameter("registration");
	AircraftBean aircraft = data.getAircraftByRegistration(reg);
	String error = null;

	UserBean owneraccount = null;
	owneraccount = data.getAccountById(aircraft.getOwner());
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="/theme/redmond/jquery-ui.css" />
    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

	<script src="/scripts/jquery.min.js"></script>
	<script src="/scripts/jquery-ui.min.js"></script>
	<script src="/scripts/AutoComplete.js"></script>

	<script type="text/javascript">
		
		$(function() 
		{
			initAutoComplete("#buyername", "#buyer", <%= Data.ACCT_TYPE_ALL %>);
		});
		
	</script>
	
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
		<div class="form" style="width: 600px">
			<h2>Transfer Aircraft </h2>
			<form method="post" action="userctl">
				<div>
					<input type="hidden" id="event" name="event" value="transferAircraft"/>
				  	<input type="hidden" id="buyer" name="buyer" value=""/>
				  	<input type="hidden" name="reg" value="<%=reg%>"/>
				  	<input type="hidden" name="fname" value="<%= aircraft.getMakeModel() %>" />
			    	<input type="hidden" id="returnPage" name="returnpage" value="<%=returnPage%>"/>
		    	</div>
				<div class="formgroup high">
				  	<strong>Aircraft Registration:</strong> <%=reg%><br><br>
				  	<strong>Aircraft Make/Model:</strong> <%=aircraft.getMakeModel()%><br><br>
				  	<strong>From:</strong> <%=owneraccount.getName()%><br><br>					  
					<strong>To: </strong><br>
				  	<input type="text" id="buyername" name="buyername"/>
				</div>
				<div class="formgroup">
					<input type="submit" class="button" value="Transfer Aircraft"/>
				</div>
			</form>
		</div>
	</div>
</div>
</body>
</html>
