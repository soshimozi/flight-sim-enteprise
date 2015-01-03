<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, java.util.Calendar, net.fseconomy.beans.*,net.fseconomy.data.*"
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


	String error = null;

    boolean servicecreated = false;
    ServiceProviderBean service = ServiceProviders.getServiceProviderByOwner(user.getId());
    if( service != null)
        servicecreated = true;

    boolean isServiceKey = false;

    String requestorKey = "";
    String accessKey = "";
    if(user.getReadAccessKey() != null)
    {
        accessKey = user.getReadAccessKey();
        requestorKey = accessKey;
    }

    String format = "xml";
    String aircraftreg = "483514";
    String aircraftownername;
    aircraftownername = user.getName();
    String aircraftmakemodel = "Cessna 172 Skyhawk";

    Calendar cal = Calendar.getInstance();
    int cyear = cal.get(Calendar.YEAR);
    int cmonth = 1;

    String singleicao = "CZFA";
    String multiicao = "CZFA-CEX4-CYMA";

    if (request.getParameter("submit") != null )
    {
        //handle read access key resets here
        if( request.getParameter("submit").contentEquals("ResetReadAccessKey") && request.getParameter("id") != null )
        {
            int userid = Integer.parseInt(request.getParameter("id"));
            try
            {
                String newAccessKey = ServiceProviders.createAccessKey();

                //User is changing his access key
                if(userid == user.getId())
                {
                    user.setReadAccessKey( newAccessKey );
                    Accounts.updateUser(user);
                    requestorKey = accessKey = user.getReadAccessKey();

                    error = "Read Access Key Updated.";
                }
                else
                {
                    //group access key change
                    //verify that the current user is the owner of the selected group
                    UserBean group = Accounts.getGroupById(userid);
                    if(group.isGroup() && Accounts.accountUltimateGroupOwner(userid) == user.getId())
                    {
                        //update the group access key
                        group.setReadAccessKey( newAccessKey );
                        Groups.updateGroup(group, user);
                        error = "Read Access Key Updated.";
                    }
                    else
                        error = "Reset ignored, not the owner!";
                }
            }
            catch (DataError e)
            {
                error = e.getMessage();
            }
        }
        else if( request.getParameter("submit").contentEquals("UpdateUrls") )
        {
            if(request.getParameter("isservice") != null)
                isServiceKey = request.getParameter("isservice").equals("true");

            requestorKey = request.getParameter("requestorkey");
            accessKey = request.getParameter("key");
            format = request.getParameter("dataformat");
            cmonth = Integer.parseInt(request.getParameter("month"));
            cyear = Integer.parseInt(request.getParameter("year"));
            if(request.getParameter("singleicao") != null && request.getParameter("singleicao").length() >= 3)
                singleicao = request.getParameter("singleicao");

            if(request.getParameter("multiicao") != null
                    && request.getParameter("multiicao").length() >= 3
                    && request.getParameter("multiicao").indexOf("-") > 0)
                multiicao = request.getParameter("multiicao");

            if(request.getParameter("aircraftreg") != null
                    && request.getParameter("aircraftreg").length() >= 3)
                aircraftreg = request.getParameter("aircraftreg");

            if(request.getParameter("aircraftownername") != null
                    && request.getParameter("aircraftownername").length() >= 3)
                aircraftownername = request.getParameter("aircraftownername");

            if(request.getParameter("aircraftmakemodel") != null
                    && request.getParameter("aircraftmakemodel").length() >= 3)
                aircraftmakemodel = request.getParameter("aircraftmakemodel");
        }
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script type="text/javascript">
        var isservice = <%=isServiceKey%>;
        var reqkey = "<%=requestorKey%>";
        var selectedkey = "<%=accessKey%>";
        var selectedformat = "<%=format%>";

        function setKey(key)
        {
            selectedkey = key;
            isservice = false;
        }
        function setRequestorKey(key, isserv)
        {
            //alert("setting reqkey");
            reqkey = key;
            isservice=isserv;
        }
        function setFormat(format)
        {
            selectedformat = format;
        }

        function doSubmit(action, id)
        {
            if (window.confirm("You are about to reset a ReadAccessKey! If this has been saved, in perhaps a service, or in a spreadsheet you will have to remember to change those to the new key. Are you sure?"))
            {
                var form = document.getElementById("accessKeyForm");
                form.submit.value = action;
                form.id.value = id;
                form.submit();
            }
        }
        function doSubmit2(action)
        {
            var form = document.getElementById("accessKeyForm");
            form.submit.value = action;
            form.isservice.value = isservice;
            form.requestorkey.value = reqkey;
            form.key.value = selectedkey;
            form.dataformat.value = selectedformat;
            form.submit();
        }
    </script>

</head>

<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	<div class="form" style="width: 600px">
		<form id="accessKeyForm" method="post" action="datafeeds.jsp">
			<div>
				<input type="hidden" name="submit"/>
				<input type="hidden" name="isservice"/>
				<input type="hidden" name="requestorkey"/>
				<input type="hidden" name="key"/>
				<input type="hidden" name="dataformat"/>
				<input type="hidden" name="id"/>
			</div>
<%
%>
			<table border="1" cellpadding="5">
				<thead>
				<tr>
					<th>Select</th>
					<th>Pilot/Group Name</th>
					<th>Access Key</th>
					<th>Reset Key</th>
				</tr>
				</thead>
				<caption>
				<span>
				View / Reset READ Access Keys<br/>
<%
	if(!servicecreated)
	{
%>
            <p>
		        <span class="font-size: 1"><a href="serviceproviderrequest.jsp">Request Service Provider Key</a></span>
            </p>
<%
	}
	else if(service.getStatus() == ServiceProviderBean.STATUS_ACTIVE)
	{
%>
            <p>
	    	    <span class="font-size: 2">Your Service Key: <%=service.getKey()%> </a></span>
            </p>
<%
	}
	else
	{
%>
            <p>
    		    <span class="font-size: 2">Your Service Key: <%=service.getStatusString()%> </a></span>
            </p>
<%
	}

	String checked; 
	if(requestorKey.equals(user.getReadAccessKey()) || user.getReadAccessKey() == null) 
		checked = "checked";
	else
		checked = "";
%>
			<span style="font-size: 12pt;">Select Requester Type:</span><br/>
			<input type="radio" name="rkey" onclick="setRequestorKey('<%= user.getReadAccessKey() %>', false)" <%= checked %> />
			User
<%
	if(servicecreated && requestorKey.equals(service.getKey())) 
		checked = "checked"; 
	else
		checked = "";
					
	if(servicecreated && service.getStatus() == ServiceProviderBean.STATUS_ACTIVE)
	{
%>			
			<input type="radio" name="rkey" onclick="setRequestorKey('<%= service.getKey() %>', true)" <%= checked %> />
			Service<br/><br/>
<%
	}
%>
				</span>
				</caption>
				<tr>
<%
	if(accessKey.equals(user.getReadAccessKey())) 
		checked = "checked";
	else
		checked = "";
%>				
					<td><input type="radio" name="accesskey" onclick="setKey('<%= user.getReadAccessKey() %>')" <%= checked %> /></td>
					<td><%= user.getName() %></td>
					<td><%= user.getReadAccessKey() %></td>
					<td>&nbsp;&nbsp;<input type="submit" value="Reset" onclick="doSubmit('ResetReadAccessKey', <%= user.getId() %>)"/></td>
				</tr>
<% 
	try
	{	
		//
		List<UserBean> groups = Accounts.getGroupsForUser(user.getId());
		if(groups != null)
		for (UserBean group : groups)
		{
			int groupid = group.getId();
			if(Accounts.accountUltimateGroupOwner(groupid) == user.getId())
			{
%>
				<tr>
<%
				if(accessKey != null && accessKey.equals(group.getReadAccessKey()))
					checked = "checked";
				else
					checked = "";
%>				
					<td><input type="radio" name="accesskey" onclick="setKey('<%= group.getReadAccessKey() %>')" <%= checked %> /></td>
					<td><%= group.getName() %></td>
					<td><%= group.getReadAccessKey() %></td>
					<td>&nbsp;&nbsp;<input type="submit" value="Reset" onclick="doSubmit('ResetReadAccessKey', '<%= group.getId() %>')"/></td>
				</tr>
<%			
			}
		}
	}
	catch (Exception e)
	{
		error = e.getMessage();
	}
%>				
				<tr>
					<td colspan="4">Select Format</td>
				</tr>
				<tr>
					<td colspan="4">
<%
	if(format.equals("xml")) 
		checked = "checked";
	else
		checked = "";
%>				
					<input type="radio" name="format" onclick="setFormat('xml')" <%= checked %> />&nbsp;&nbsp;XML&nbsp;&nbsp;&nbsp;&nbsp;
<%
	if(format.equals("csv")) 
		checked = "checked";
	else
		checked = "";
%>				
					<input type="radio" name="format" onclick="setFormat('csv')" <%= checked %> />&nbsp;&nbsp;CSV
					</td>
					
				</tr>
				<tr>
					<td colspan="4">Set Sample Parameters</td>
				</tr>
				<tr>
					<td colspan="4">
						Month&nbsp;&nbsp;
                   		<select name="month" class="formselect" >
<% 
	for ( int c = 1; c <= 12; c++ ) 
    { 
		String selected;
		if(cmonth == c)
			selected = "selected";
		else
			selected = "";
%>
                        	<option class="formselect" value="<%= c %>" <%= selected %>><%= c %></option>
<% 
	} 
%>        
                        </select>
						Year&nbsp;&nbsp;
                   		<select name="year" class="formselect">
<% 
	for ( int c = cyear; c >= 2005; c-- ) 
    { 
		String selected;
		if(cyear == c)
			selected = "selected";
		else
			selected = "";
%>
                        	<option class="formselect" value="<%= c %>" <%= selected %>><%= c %></option>
<% 
	} 
%>        
                        </select>
					</td>					
				</tr>
				<tr>
					<td colspan="4">
					Single ICAO&nbsp;&nbsp;<input name="singleicao" type="text" maxlength="4" size="4" value="<%= singleicao %>"/>&nbsp;&nbsp;
					Multi-ICAO&nbsp;&nbsp;<input name="multiicao" type="text" maxlength="50" size="50" value="<%= multiicao %>"/>
					</td>
				</tr>
				<tr>
					<td colspan="4">
					Aircraft Reg#&nbsp;&nbsp;<input name="aircraftreg" type="text" maxlength="20" size="20" value="<%= aircraftreg %>"/><br/>
					Aircraft Owner name&nbsp;&nbsp;<input name="aircraftownername" type="text" maxlength="50" size="50" value="<%= aircraftownername %>"/><br/>
					Aircraft MakeModel&nbsp;&nbsp;<input name="aircraftmakemodel" type="text" maxlength="50" size="50" value="<%= aircraftmakemodel %>"/><br/>
					</td>
				</tr>
				<tr>
					<td colspan="4">&nbsp;&nbsp;<input type="submit" value="Update Selections" onclick="doSubmit2('UpdateUrls')"/></td>
				</tr>
			</table>
		</form> 
	</div>
<%
	String base = net.fseconomy.servlets.Datafeed.DataFeedUrl + "/data?" + (isServiceKey ? "servicekey" : "userkey") + "=" + requestorKey + "&format=" + format + "&";
	String staticbase = net.fseconomy.servlets.Datafeed.DataFeedUrl + "/static";
%>
	<br/>
	<div style="font-weight: bold;font-size: 10pt;color: blue;">Note: All timestamps are returned in GMT</div>
	<div class="dataTable"> 
	<table>
	<caption>Data export</caption> 
	<thead>
	<tr>
		<th>Export type</th>
		<th>Example URL</th>
	</tr>
	</thead>
	<tbody>
<% 
//-------------------------------------------------------------------------------------------------
	String link = base + "query=feedstatsreset&admin=notforeveryone"; 
	if(user.getLevel() == 3)
	{
%>
	<tr style="background-color: #00AA33;">
		<td colspan="2">ADMIN Feeds</td>
	</tr>
	<tr>
		<td style="color: red;">Reset Feed Request Statistics</td>
		<td><%= link %></td>
	</tr>
	<% link = base + "query=CycleTimeStats&admin=notforeveryone"; %>
	<tr>
		<td>Maintenance Cycle Time Statistics</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
	<% link = base + "query=servicerequeststats&admin=notforeveryone"; %>
	<tr>
		<td>Service Request Statistics</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
	<% link = base + "query=feedrequeststats&admin=notforeveryone"; %>
	<tr>
		<td>Feed Request Statistics</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
	<% link = base + "query=feedhitstats&admin=notforeveryone"; %>
	<tr>
		<td>Feed Hit Statistics</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	}	
	//-------------------------------------------------------------------------------------------------
%>
	<tr>
	<td colspan="2">&nbsp;</td>
	</tr>
	<tr>
	<td style="background-color: #00AA33;" colspan="2">XML Only Feeds</td>
	</tr>
<% 
	link = base + "query=aircraft&search=status&aircraftreg=" + aircraftreg; 
%>
	<tr>
		<td>Aircraft Status By Registration</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
	<tr><td colspan="4">&nbsp;</td></tr>
	<tr>
	<td style="background-color: #00AA33;" colspan="2">Example Data Feed Links</td>
	</tr>
<% 
	link = base + "query=aircraft&search=configs"; 
%>
	<tr>
		<td>Aircraft Configs</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=aircraft&search=aliases"; 
%>
	<tr>
		<td>Aircraft Aliases</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=aircraft&search=forsale"; 
%>
	<tr>
		<td>Aircraft For Sale</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=aircraft&search=makemodel&makemodel=" + aircraftmakemodel; 
%>
	<tr>
		<td>Aircraft By MakeModel</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=aircraft&search=ownername&ownername=" + aircraftownername; 
%>
	<tr>
		<td>Aircraft By Owner Name</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=aircraft&search=registration&aircraftreg=" + aircraftreg; 
%>
	<tr>
		<td>Aircraft By Registration</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=aircraft&search=key&readaccesskey=" + accessKey; 
%>
	<tr>
		<td>Aircraft By Key</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=assignments&search=key&readaccesskey=" + accessKey;  
%>
	<tr>
		<td>Assignments By Key</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=commodities&search=key&readaccesskey=" + accessKey;
%>
	<tr>
		<td>Commodities By Key</td>  
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=Facilities&search=key&readaccesskey=" + accessKey; 
%>
	<tr>
		<td>Facilities By Key</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=fbos&search=key&readaccesskey=" + accessKey; 
%>
	<tr>
		<td>FBOs By Key</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=fbos&search=forsale"; 
%>
	<tr>
		<td>FBO's For Sale</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=fbos&search=monthlysummary&readaccesskey=" + accessKey + "&month=" + cmonth + "&year=" + cyear + "&icao=" + singleicao;  
%>
	<tr>
		<td>FBO Monthly Summary by ICAO</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=flightlogs&search=monthyear&readaccesskey=" + accessKey + "&month=" + cmonth + "&year=" + cyear;  
%>
	<tr>
		<td>Flight Logs By Key Month Year</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=flightlogs&search=monthyear&aircraftreg=" + aircraftreg + "&month=" + cmonth + "&year=" + cyear;  
%>
	<tr>
		<td>Flight Logs By Reg Month Year</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=flightlogs&search=id&readaccesskey=" + accessKey + "&fromid=2178373";  
%>
	<tr>
		<td>Flight Logs By Key From Id (Limit 500)</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=flightlogs&search=id&readaccesskey=" + accessKey + "&fromid=2178373&type=groupaircraft";  
%>
	<tr>
		<td>Flight Logs By Key From Id for ALL group aircraft (Limit 500)</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=flightlogs&search=id&aircraftreg=" + aircraftreg + "&fromid=2178373";  
%>
	<tr>
		<td>Flight Logs By Reg From Id (Limit 500)</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=group&search=members&readaccesskey=" + accessKey;  
%>
	<tr>
		<td>Group Members</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>	
	<tr>
	<td> 
<% 
	link = base + "query=icao&search=aircraft&icao=" + singleicao; 
%>
	</td>
	</tr>
	<tr>
		<td>ICAO Listing of Aircraft</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=icao&search=fbo&icao=" + singleicao; 
%>
	<tr>
		<td>ICAO Listing of FBO's</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=icao&search=jobsto&icaos=" + multiicao;  
%>
	<tr>
		<td>ICAO Jobs To</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=icao&search=jobsfrom&icaos=" + multiicao;  
%>
	<tr>
		<td>ICAO Jobs From</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=payments&search=monthyear&readaccesskey=" + accessKey + "&month=" + cmonth + "&year=" + cyear;  
%>
	<tr>
		<td>Payments By Month Year</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=payments&search=id&readaccesskey=" + accessKey + "&fromid=227763";  
%>
	<tr>
		<td>Payments From Id (Limited 500)</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
<% 
	link = base + "query=statistics&search=key&readaccesskey=" + accessKey;  
%>
	<tr>
		<td>Statistics By Key</td> 
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>		
	<tr>
	<td colspan="4">&nbsp;</td>
	</tr>		
	<tr>
	<td style="background-color: #00AA33;" colspan="4">Misc. Data</td>
	</tr>
<% 
	link = staticbase + "/library/datafeed_icaodata.zip";
%>
	<tr>
		<td>FSE ICAO Data Zip Archive (CSV format)</td>
		<td><a href="<%= link %>"><%= link %></a></td>
	</tr>
	</tbody>
	</table>
	</div>
</div>
</div>
<% 	
	if (error != null) 
	{ 
%>
	<div class="error"><%= error %></div>
<%	
	}
%>

</body>
</html>
