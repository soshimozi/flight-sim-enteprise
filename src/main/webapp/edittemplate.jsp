<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.* "
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<jsp:useBean id="template" class="net.fseconomy.data.TemplateBean">
	<jsp:setProperty name="template" property="*"/>
</jsp:useBean>
<% 
	if (!Data.needLevel(user, UserBean.LEV_MODERATOR)) 
	{
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return; 
	}

	String error = null;

	if (request.getParameter("submit") == null)
	{
		String newModel = request.getParameter("newtemplate");
		if (newModel != null)
		{
			template = new TemplateBean();
			template.setId(-1);
			template.setComment("New template");
			template.setCommodity("Passengers");
			template.setTypeOfPay(TemplateBean.TYPE_TRIPONLY);
		} 
		else
		{
			TemplateBean[] result = data.getTemplateById(template.getId());
			template = result[0];
		}
	} 
	else if (error == null)
	{
		try
		{
			data.updateTemplate(template, user);
%>
			<jsp:forward page="templates.jsp"></jsp:forward>
<%		
		} 
		catch (DataError e)
		{
			error = e.getMessage();
		}
	}
%>
<%!
	String deviation(int current)
	{
		StringBuffer result = new StringBuffer();
		for (int c=0; c<=100; c+=5)
		{
			result.append("<option value=\"");
			result.append(c);
			result.append("\"");
			if (current == c)
				result.append(" selected");
			result.append(">");
			result.append(c);
			result.append("%</option>");
		}
		return result.toString();
	}
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
	if (error != null) 
	{ 
%>
		<div class="error"><%= error %></div>
<%	
	}
	
	int keepAlive = template.getTargetKeepAlive();
%>
	<div class="form" style="width: 500px">
		<form method="post" action="edittemplate.jsp">
			<input type="hidden" name="submit" value="true"/>
			<input type="hidden" name="event" value="editTemplate"/>
			<input type="hidden" name="id" value="<%= template.getId() %>"/>

			<table>
				<caption>Edit Template</caption>
				<tr>
					<td>Comment</td><td><input name="comment" type="text" class="textarea" value="<%= template.getComment() %>" size="40"/></td>
				</tr>
				<tr>
					<td>Frequency</td><td><input name="frequency" type="text" class="textarea" value="<%= template.getFrequency() %> " size="7"/></td>
				</tr>
				<tr>
					<td>Target Keep Alive</td>
					<td>
					<select name="targetKeepAlive" class="formselect">
						<option class="formselect" value="1" <%=  keepAlive == 1 ? "selected" : "" %>>1 day</option>
						<option class="formselect" value="2" <%=  keepAlive == 2 ? "selected" : "" %>>2 days</option>
						<option class="formselect" value="3" <%=  keepAlive == 3 ? "selected" : "" %>>3 days</option>
						<option class="formselect" value="4" <%=  keepAlive == 4 ? "selected" : "" %>>4 days</option>									
						<option class="formselect" value="5" <%=  keepAlive == 5 ? "selected" : "" %>>5 days</option>												
					</select>
					</td>
				</tr>
				<tr>
					<td>Commodity</td><td><input name="commodity" type="text" class="textarea" value="<%= template.getCommodity() %>" size="20"/></td>
				</tr>
				<tr>
					<td>Units</td>
					<td>
					<select name="units" class="formselect">
						<option class="formselect" value="1" <%= template.getUnits() == AssignmentBean.UNIT_PASSENGERS ? "selected" : "" %>>Passengers</option>
						<option class="formselect" value="2" <%= template.getUnits() == AssignmentBean.UNIT_KG ? "selected" : "" %>>KGs</option>						
					</select>
					
					</td>
				</tr>	
				<tr>
					<td>Target amount</td><td><input name="targetAmount" type="text" class="textarea" value="<%= template.getTargetAmount() %>" size="7"/>  
					Dev <select name="amountDev" class="formselect"><%= deviation(template.getAmountDev())%></select></td>
				</tr>
				<tr>
					<td>Target Pay</td><td><input name="targetPay" type="text" class="textarea" value="<%= template.getTargetPay() %>" size="7"/> 
					Dev <select name="payDev" class="formselect"><%= deviation(template.getPayDev())%></select></td>
				</tr>
				<tr>
					<td>Target Distance</td><td><input name="targetDistance" type="text" class="textarea" value="<%= template.getTargetDistance() %>" size="7"/>  
					Dev <select name="distanceDev" class="formselect"><%= deviation(template.getDistanceDev())%></select></td>
				</tr>	
				
				<tr>
					<td>From/To airports larger than</td><td><input name="matchMinSize" type="text" class="textarea" value="<%= template.getMatchMinSize() %>" size="7"/> 
					combined meters of rwys</td>
				</tr>
				<tr>
					<td>From/To airports smaller than</td><td><input name="matchMaxSize" type="text" class="textarea" value="<%= template.getMatchMaxSize() %>" size="7"/> 
					combined meters of rwys</td>
				</tr>
				<tr>
					<td>From</td>
					<td><textarea name="icaoSet1"><%= template.getIcaoSet1() == null ? "" : template.getIcaoSet1()  %></textarea></td>
				</tr>
				<tr>
					<td>To</td>
					<td><textarea name="icaoSet2"><%= template.getIcaoSet2() == null ? "" : template.getIcaoSet2()  %></textarea></td>
				</tr>	
				
				<tr>
					<td>Type</td>	
					<td>
					<select name="typeOfPay" class="formgroup">
						<option class="formselect" value="1" <%= template.getTypeOfPay() == TemplateBean.TYPE_ALLIN ? "selected" : "" %>>All in</option>
						<option class="formselect" value="2" <%= template.getTypeOfPay() == TemplateBean.TYPE_TRIPONLY ? "selected" : "" %>>Trip only</option>						
					</select>
					</td>
				</tr>
				<tr>
				<!-- All-In changes begin here -->
				<td colspan="2" style="border:1px solid #000;">
					<table>
						<tr>
							<td colspan="2">
								<div>
								<b>Filters - these values only apply to All-In jobs</b><br/>
								<div style="font-size:10pt">Notes:
									<ul>
							 			<li>Leaving the "to" field empty defaults to the maximum possible value.</li>
							 			<li>To set an exact match value, place the value in both the "From" and "To" fields.</li>
									</ul>
							 	</div>
							 	</div>
							 </td>
						 </tr>
						 <tr>
							 <td>Speed</td>
							 <td>
								 From: <input type="text" name="speedFrom" size="3" value="<%= template.getSpeedFrom() %>" maxlength="3"/> 
								 To: <input type="text" name="speedTo" size="3" value="<%= template.getSpeedTo() %>" maxlength="3"/>
							</td>
						</tr>
						<tr>
							<td>Seats</td>
							<td>
								From: <input type="text" name="seatsFrom" value="<%= template.getSeatsFrom() %>" size="3" maxlength="3"/> 
								To: <input type="text" name="seatsTo" size="3" value="<%= template.getSeatsTo() %>" maxlength="3"/></td>
						</tr>
					</table>
				</tr>
				<tr>
					<td><input type="submit" class="button" value="Update"/></td>
				</tr>			
			</table>
		</form>

		<ul class="footer">
			<li>For assignments between specific airports: frequency is the amount of assignments to appear in total</li>
			<li>For other assignments: frequency is the average amount of assignments per area of 140 x 140 NM</li>
			<li>Target pay is per unit per 100 NM</li>
		</ul>
	</div>
	
</div>
</div>
</body>
</html>
