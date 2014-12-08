<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, java.util.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(user == null || !user.isLoggedIn() || !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    String error = "";
    int ownerid;
    String ownername;
    int altid;
    String altname;
    String name;
    String ip;
    String url;
    String desc;
    String notes;
    String newnote;

    int id = Integer.parseInt(request.getParameter("id"));
    ServiceProviderBean service = ServiceProviders.getServiceProviderById(id);

    if (request.getParameter("submit") != null)
    {
        //verify values
        if(request.getParameter("owner") == null || request.getParameter("owner").isEmpty())
            error = error + "Owner parameter missing<br/>";

        if(request.getParameter("alt") == null || request.getParameter("alt").isEmpty())
            error = error + "Alternate parameter missing<br/>";

        ownerid = Integer.parseInt(request.getParameter("owner"));
        ownername = request.getParameter("ownername");
        altid = Integer.parseInt(request.getParameter("alt"));
        altname = request.getParameter("altname");
        name = request.getParameter("name");
        ip = request.getParameter("ip");
        url = request.getParameter("url");
        desc = request.getParameter("description");
        newnote = request.getParameter("newnote");

        if(name == null || name.isEmpty())
            error = error + "Name missing<br/>";

        if(!ip.equals("none") && !Validate.isIPAddress(ip))
            error = error + "IP missing or bad format, use 'none' if not available<br/>";

        if(url == null || url.isEmpty())
            error = error + "Url missing, use 'none' if not available<br/>";
        else if(url.length() > 200)
            error = error + "Url entry > 200 characters<br/>";

        if(desc == null || desc.isEmpty() || desc.equals("Please replace this text with a general description of your service."))
            error = error + "Description missing, please enter a short description of the purpose of your service<br/>";
        else if(desc.length() > 255)
            error = error + "Description entry > 255 characters<br/>";

        Date date = new Date();
        if(	newnote != null && !newnote.isEmpty())
            notes = Formatters.getUserTimeFormat(user).format(date) + " (" + user.getName() + ")" + " - " + newnote + "\n" + service.getNotes();
        else
            notes = service.getNotes();

        //if no errors process our request, otherwise let it fall through and display the missing parameters
        if(error.isEmpty())
        {
            service.setOwner(ownerid, ownername);
            service.setAlternate(altid, altname.isEmpty() ? null : altname);
            service.setName(name);
            service.setIP(ip);
            service.setUrl(url);
            service.setDescription(desc);
            service.setNotes(notes);
            ServiceProviders.updateServiceProvider(service);

            System.out.println("newnote = " + newnote);
            if(newnote == null || newnote.isEmpty())
            {
%>
                <script type="text/javascript">document.location.href="/admin/serviceprovidersedit.jsp"</script>
<%
                return;
            }
        }
    }
    else
    {
        //System.out.println("Ownername: " + service.getOwnerName());
        ownername = service.getOwnerName();
        ownerid = service.getOwner();
        altname = service.getAlternateName() == null ? "" : service.getAlternateName();
        altid = service.getAlternate();
        name = service.getName();
        ip = service.getIP();
        url = service.getUrl();
        desc = service.getDescription();
    }
    if(desc == null || desc.isEmpty())
        desc = "Please replace this text with a general description of your service.";

    notes = service.getNotes().replaceAll("\n","<br/><br/>");

%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css" />
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="../scripts/jquery.min.js"></script>
    <script src="../scripts/jquery-ui.min.js"></script>
    <script src="../scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#ownername", "#owner", <%= Accounts.ACCT_TYPE_PERSON %>);
            initAutoComplete("#altname", "#alt", <%= Accounts.ACCT_TYPE_PERSON %>);
        });

    </script>

</head>

<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">

	<h2>Admin - Service Provider Edit</h2><br/>
	<a href="/admin/serviceproviders.jsp">Return to Service Providers Page</a><br/>
	<div class="form" style="width: 800px">
<%
	if(!error.equals(""))
	{
%>
        <div class=\"error\"><%= error %><br/></div>
<%
	}
%>		
	<form method="post">
	<div>
	<input type="hidden" name="submit" value="true" />
	<input type="hidden" name="return" value="/admin/datafeedservicekeyrequest.jsp" />
	</div>
	<table>
		<tr>
			<td>Service Owner</td>
			<td>
	    	<input type="hidden" id="owner" name="owner" value="<%= ownerid %>"/>
	    	<input type="text" id="ownername" name="ownername" value="<%= ownername %>"/>
			</td>
		</tr>
		<tr>
			<td>Alternate Contact</td>
			<td>
	    	<input type="hidden" id="alt" name="alt" value="<%= altid %>"/>
	    	<input type="text" id="altname" name="altname" value="<%= altname %>"/>
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
				<input type="submit" class="button" value="Update" />	
			</td>
		</tr>
		<tr>
			<td>
				&nbsp;	
			</td>
		</tr>
		<tr>
			<td colspan="3">
				<textarea name="newnote" cols="50" rows="5"></textarea>
			</td>
		</tr>
		<tr>
			<td>
				<input type="submit" class="button" value="Add Note" />	
			</td>
		</tr>
		<tr>
			<td colspan="3">
				<div class="textarea">
					<%=notes%>
				</div>
			</td>
		</tr>
	</table>
	</form>
	</div>
</div>

</div>

</body>
</html>
