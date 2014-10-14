<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
<%
    if(user == null || !user.isLoggedIn())
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
        return;
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
	String error = "";
	int ownerid = user.getId();
	String ownername = user.getName();
	int altid = -1;
	String name = "";
	String ip = "";
	String url = "";
	String desc = "";
	
	if (request.getParameter("submit") != null) 
	{ 
		//verify values
		if(request.getParameter("alternate") != null && !request.getParameter("alternate").isEmpty())
			altid = Integer.parseInt(request.getParameter("alternate"));
		
		name = request.getParameter("name");
		ip = request.getParameter("ip");
		url = request.getParameter("url");
		desc = request.getParameter("description");
		
		if(name == null || name.isEmpty())
			error = error + "Name missing<br/>";
		
		if(!ip.equals("none") && !Validate.IPAddress(ip))
			error = error + "IP missing or bad format, use 'none' if not available<br/>";
			
		if(url == null || url.isEmpty())
			error = error + "Url missing, use 'none' if not available<br/>";
		else if(url.length() > 200)
			error = error + "Url entry > 200 characters<br/>";
			
		if(desc == null || desc.isEmpty() || desc.equals("Please replace this text with a general description of your service."))
			error = error + "Description missing, please enter a short description of the purpose of your service<br/>";
		else if(desc.length() > 255)
			error = error + "Description entry > 255 characters<br/>";
			
		//if no errors process our request, otherwise let it fall through and display the missing parameters	
		if(error.isEmpty())
		{
			ServiceProviderBean service = new ServiceProviderBean();
			service.setOwner(ownerid, ownername);
			service.setAlternate(altid, ""); //don't need to set the name here for new record.
			service.setName(name);
			service.setIP(ip);
			service.setUrl(url);
			service.setDescription(desc);
			data.addServiceProvider(service);
			
			String msg = "A new request for a service key has been made for service: " + service.getName() + " (" + service.getOwnerName() + ")" + "\n\nThis is an automated notice.";
			try
			{
				data.doServiceProviderNotification(service, "FSE - New Service Key Request", msg, true);
			}
			catch (DataError e)
			{
				error = e.getMessage();
			}
			
			out.print("<script type=\"text/javascript\">document.location.href=\"datafeeds.jsp\"</script>");
			return; 
		}
	}
	if(desc == null || desc.isEmpty())
		desc = "Please replace this text with a general description of your service.";
	UserBean Accounts[] = data.getAccounts(true);
%>
	<h2>Service Provider Key Request</h2>
	<div class="textarea" style="width: 800px">
	Please note:<br/><br/>
	You account email must be active and you will need to respond to any queries sent to that 
	email in a timely fashion.<br/><br/>
	You will receive all responses to your request through your account email
	including any questions, the service key if approved, as well as any notifications of 
	issues with your service that might arise.<br/><br/>
	Be aware that if you do not respond to notifications of any problems that might arise 
	on that email account your service will be disabled.<br/><br/> 
	If you would not be able to response within 12 to 24 hours you might want to consider
	selecting a alternate contact that could be reached. 
	</div>
	<div class="form" style="width: 800px">
<%
	if(!error.equals(""))
	{
		out.print("<div class=\"error\">" + error + "<br/></div>");
	}
%>		
	<form method="post">
	<div>
	<input type="hidden" name="submit" value="true" />
	<input type="hidden" name="return" value="datafeedservicekeyrequest.jsp" />
	</div>
	<table>
		<tr>
			<td>Service Owner</td>
			<td>
				<%= user.getName()%>		
			</td>
		</tr>
		<tr>
			<td>Alternate Contact</td>
			<td>
				<select name="alternate" class="formselect">
					<option class="formselect" value= >Choose an account </option>
<%
		for (int c=0; c< Accounts.length; c++)
		{
			if(altid == Accounts[c].getId())
				out.print("<option class=\"formselect\" selected=\"selected\" value=\"" + Accounts[c].getId() + "\">" + Accounts[c].getName() + "</option>");
			else
				out.print("<option class=\"formselect\" value=\"" + Accounts[c].getId() + "\">" + Accounts[c].getName() + "</option>");
		}
%>		
		    	</select>
			</td>
			<td>(Optional)</td>
		</tr>
		<tr>
			<td>Service Name (Max characters 50)</td>
			<td><input name="name" type="text" size="50" value="<%=name%>"/></td> 
			<td>(Required)</td>			
		</tr>
		<tr>
			<td>Service IP</td>
			<td><input name="ip" type="text" size="20" value="<%=ip%>"/></td>
			<td>(use 'none' if not applicable**)</td>			
		</tr>
		<tr>
			<td>Service URL Address</td>
			<td><input name="url" type="text" size="50" maxlength="200" value="<%=url%>"/></td>
			<td>(Use 'none' if not applicable**, Max characters 200)</td>			
		</tr>
		<tr>
			<td>Service Description</td>
			<td>
				<textarea name="description" cols="50" rows="5">
<%=desc%>
				</textarea>
			</td>
			<td>(Required, Max characters 255)</td>		
		</tr>
		<tr>
			<td>
				<input type="submit" class="button" value="Send Request" />	
			</td>
		</tr>
	</table>
	</form>
	</div>
<div class="textarea" style="width: 800px">
	** Note:<br/><br/>
	An example of why you would not supply an IP and DNS name would be that you have
	created a application that runs on users computers, versus a 'server' computer.<br/><br/>
	In this case it might be more efficient for your application to request updates
	with a service key so that you are not using the users limited number of hits per day.<br/><br/>
	If that is the case, please note that in your description text.
	</div>
</div>

</div>

</body>
</html>
