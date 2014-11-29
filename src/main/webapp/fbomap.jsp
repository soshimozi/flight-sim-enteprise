<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.data.* "
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />
<jsp:useBean id="airport" class="net.fseconomy.beans.AirportBean">
    <jsp:setProperty name="airport" property="icao"/>
</jsp:useBean>

<%
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FS Economy Terminal</title>

    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />

	<link href="css/Master.css" rel="stylesheet" type="text/css" />

	<script src="scripts/AnchorPosition.js"></script>
	<script src="scripts/PopupWindow.js"></script>

	<script type="text/javascript">
		var gmapfbo = new PopupWindow();
		
		function formValidation(form){
			if(notEmpty(form.depart)){
				if(notEmpty(form.dest)){
					return true;
				}
			}
			return false;
		}
		
		function notEmpty(elem){
		var str = elem.value;
		if(str.length == 0){
			alert("Please fill in both ICAO text boxes.");
			return false;} else {
			return true;}
		
		}
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp">
	<jsp:param name="open" value="fbo"/>
</jsp:include>

<%
	List<String> noDupeStates = null;
	List<String> noDupeCountries = null;
	
	List<String> regions = Airports.getSearchRegions();
	
	try
	{
		noDupeStates = Data.getDistinctColumnData("state", "airports");
	} catch (DataError e)
	{
%>
		<div class="error"><%= e.getMessage() %></div>
<%
	}

	try
	{
		noDupeCountries = Data.getDistinctColumnData("country", "airports");
	} catch (DataError e)
	{
%>
		<div class="error"><%= e.getMessage() %></div>
<%
	}
%>
	<!-- FORM FOR PICKING MAP VARIABLES -->
	<div class="form" style="width: 700px">
		<form name="fboForm" id="fboForm" method="post" action="gmapfbo.jsp">
			<h2>FBO Maps</h2>
			<div class="formgroup">
				Airports With: 
				<input type="checkbox" name="fboCheck" value="checkbox" checked="true"/>
				Privately Owned FBO &nbsp;
				<input type="checkbox" name="inactiveCheck" value="checkbox" />
				Inactive FBO &nbsp;
				<input type="checkbox" name="fuelCheck" value="checkbox" />
				Local Fuel &nbsp;
				<input type="checkbox" name="repairCheck" value="checkbox" />
				Local Repair<br />
				<input type="checkbox" name="facilityPTCheck" value="checkbox" />
				Gates for Rent<br />
			</div>
			<div class="formgroup">
				ICAO: 
				<input name="icao" type="text" class="textarea" size="4" maxlength="4" />
				 &nbsp;Airport Name: 
				<input name="name" type="text" class="textarea" size="41" />
			</div>
			<div class="formgroup">	
				In Country: 
				<select name="country" class="formselect">
					<option class="formselect" value=""></option>
<%
	for (String country : noDupeCountries)
	{
%>
					<option class="formselect" value="<%= country %>"><%= country %></option>
<%
    }
%>
				</select>
				&nbsp;State:
				<select name="state" class="formselect">
					<option class="formselect" value=""></option>
<%
	for (String states : noDupeStates)
	{
%>
					<option class="formselect" value="<%= states %>"><%= states %></option>
<%
    }
%>
				</select>
			</div>
			<div class="formgroup">
				In Region:
				<select name="region" class="formselect">
				    <option value=""></option>
<%      
		for (String region : regions)
		{
%>
					<option value="<%= region %>"><%= region %></option>
<%      
		}
%>
				</select>
			</div>
			<div class="formgroup margin-left:auto; margin-right:auto;">	
				<input name="submit" type="submit" class="button" value="Get Map" >
			</div>
			<input type="hidden" name="return" value="gmapfbo.jsp" />
		</form>
	</div>
	<div class="form" style="width: 700px">
		<form name="distanceForm" id="distanceForm" method="post" action="gmapdistance.jsp" onsubmit="return formValidation(this)">
			<h2>Distance Map </h2>
			<div class="formgroup">	
				Distance from ICAO: 
				<input name="depart" type="text" class="textarea" size="4" maxlength="4" />
				&nbsp;to ICAO: 
				<input name="dest" type="text" class="textarea" size="4" maxlength="4" />
				&nbsp;
				<input name="submit" type="submit" class="button" value="Get Map" >
			</div>
		</form>
	</div>

</body>
</html>
