<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.text.*, java.util.*, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	List<AssignmentBean> assignments;
	
	boolean groupMode = false;
		
	String group = request.getParameter("groupId");
	String transfer = request.getParameter("transfer");
	String caption;

	Locale locale = request.getLocale();
    NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
	int assignmentsTotalPay = 0;
		
	int groupId = -1;
	int transferId = -1;

	if (transfer != null || group == null)
	{
        if (transfer != null)
        {
            transferId = Integer.parseInt(transfer);
        }
        else
        {
            transferId = user.getId();
        }
		
		UserBean account = Accounts.getAccountById(transferId);

		if((transfer != null && account.isGroup() && user.groupMemberLevel(transferId) < UserBean.GROUP_MEMBER)
            || (transfer != null && !account.isGroup() && (transferId != user.getId())))
		{
			// This is a group goods tranfer sceen and we are not a member of the group.
%>
            <script type="text/javascript">document.location.href="index.jsp"</script>
<%
			return;
		}

		UserBean transferAccount = Accounts.getAccountById(transferId);
		caption = "Transfer assignments for " + transferAccount.getName();
		
		assignments = Assignments.getAssignmentsForTransfer(transferId);
	} 
	else
	{

		groupId = Integer.parseInt(group);
		if (user.groupMemberLevel(groupId) < UserBean.GROUP_MEMBER) 
		{
			// This is a group assignment screen and we are not a member of the group.
%>
            <script type="text/javascript">document.location.href="index.jsp"</script>
<%
			return;
		}
		
		boolean userIsGroupStaff = user.groupMemberLevel(groupId) >= UserBean.GROUP_STAFF;
		assignments = Assignments.getAssignmentsForGroup(groupId, userIsGroupStaff);
		
		groupMode = true;
		
		UserBean groupAccount = Accounts.getAccountById(groupId);
		caption = "Group assignments for " + groupAccount.getName();
	} 
	
	//setup return page if action used
	String groupParam = groupId != -1 ? "?groupId=" + groupId : "?transfer=" + transferId;
	String returnPage = request.getRequestURI() + groupParam;
%>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel="stylesheet" type="text/css" href="css/Master.css" />
	<link rel="stylesheet" type="text/css" href="css/tablesorter-style.css" />
	<link rel="stylesheet" type="text/css" href="fancybox/jquery.fancybox-1.3.1.css" />
	<link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css" />
	
	<% //regressed jquery so that lightbox would work %>
	<script src="scripts/jquery.min.js"></script>
	<script src="scripts/jquery-ui.min.js"></script>
	<script src="https://maps.google.com/maps/api/js?sensor=false"></script>
	
	<script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
	<script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
	<script type='text/javascript' src='scripts/parser-checkbox.js'></script>
	<script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
	
	<script src="fancybox/jquery.fancybox-1.3.1.pack.js"></script>
	<script src="scripts/PopupWindow.js"></script>
	<script src="scripts/location-mapper.js"></script>
	<script src="scripts/AutoComplete.js"></script>
	
	<script type="text/javascript">
	
		$(function() 
		{
			initAutoComplete("#transfername", "#transfer", <%= Accounts.ACCT_TYPE_GROUP %>);
		});
		
		</script>
		
		<script type="text/javaScript">
		
		var gmap = new PopupWindow();
		
		function doSubmit(id)
		{
			var form = document.getElementById("assignmentForm");
			form.id.value = id;
			form.action = "<%= response.encodeURL("userctl") %>";
			form.submit();
		}
		function doSubmit2(id)
		{
			var form = document.getElementById("assignmentForm");
			form.id.value = id;
			form.submit();
		}
		function doSubmit3(id)
		{
			var form = document.getElementById("assignmentForm");
			if (!confirm("Do you want to delete these assignment?"))
				return;
			form.id.value = id;
			form.action = "<%= response.encodeURL("userctl") %>";
			form.type.value = "delete";
			form.submit();
		}
		function doSubmit4(id)
		{
			var form = document.getElementById("assignmentForm");
			form.id.value = id;
			form.action = "<%= response.encodeURL("userctl") %>";
			form.type.value = "unlock";
			form.submit();
		}
		function doSubmit5(id)
		{
			var form = document.getElementById("assignmentForm");
			form.id.value = id;
			form.action = "<%= response.encodeURL("userctl") %>";
			form.type.value = "unlockAll";
			form.submit();
		}
		function doSubmit6(id, id2)
		{
			var form = document.getElementById("assignmentForm");
			form.id.value = id;
			if (id2 != 0) 
			{
				form.groupId.value = id2;	
				form.action = "<%= response.encodeURL("userctl") %>";
				form.type.value = "move";
			}	
			form.submit();
		}
		function doSubmitComment(checkedItems)
		{
            if(!isOneOrMoreChecked(checkedItems))
			{
				alert("No assignments are selected");
				return;	
			}
			var comment = document.forms["assignmentForm"]["assignmentComment"].value;
			if(comment == "")
			{
				if(!confirm("The comment is blank, are you sure you want to reset the selected assignments?"))
					return;	
			}
			
			var form = document.getElementById("assignmentForm");
			form.id.value = checkedItems;
			form.action = "<%= response.encodeURL("userctl") %>";
			form.type.value = "comment";
			form.submit();
		}
		
		function isOneOrMoreChecked(checkboxes)
		{
			var okay = false;
			
		    for(var i = 0, l = checkboxes.length; i < l; i++)
		    {
		        if(checkboxes[i].checked)
		        {
		            okay=true;
		        }
		    }
		    return okay;
		
		}
		
		function checkAll()
		{
			var field = document.getElementById("assignmentForm").select;
			for (i = 0; i < field.length; i++)
			{
				if(!field[i].disabled)
					field[i].checked = true ;
			}
		    field.checked = true ;  // needed in case of only one box
		}
		function checkNone()
		{
			var field = document.getElementById("assignmentForm").select;
			for (i = 0; i < field.length; i++)
				field[i].checked = false ;
				
		    field.checked = false ;  // needed in case of only one box
		}
		</script>
		<script type="text/javascript">
		$(function() {
		
			$.extend($.tablesorter.defaults, {
				widthFixed: false,
				widgets : ['zebra','columns']
			});
		
			$('.assigmentTable').tablesorter();
		
		});
		
		var loc = {};
		var assignment = {};
		var i = 0;
		
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp">
	<jsp:param name="open" value="groups"/>
</jsp:include>

<div id="wrapper">
<div class="content">
	<form method="post" action="editassignment.jsp" id="assignmentForm" name="assignmentForm">
	<div>
	<input type="hidden" name="event" value="Assignment"/>
	<input type="hidden" name="type" value="add"/>
	<input type="hidden" name="id" />
	<input type="hidden" name="groupId" value="<%= groupId %>" />
	<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
<%	
	if (assignments != null && assignments.size() > 0)
	{
%>	
		<table  class="assigmentTable tablesorter-default tablesorter">
		<caption><%= caption %></caption>
		<thead>
		<tr>
		    <th class="sorter-checkbox" style="width: 35px;">Add</th>
			<th class="numeric" style="width: 75px;">Pay</th>
			<th class="numeric" style="width: 75px;">Pilot Fee</th>
			<th style="width: 60px;">Location</th>
			<th style="width: 60px;">From</th>
			<th style="width: 60px;">Dest</th>
			<th class="numeric" style="width: 35px;">NM</th>
			<th class="numeric" style="width: 45px;">Bearing</th>
			<th style="max-width: 400px;">Cargo</th>
			<th style="max-width: 400px;">Comment</th>
			<th style="width: 35px;">Type</th>
			<th style="width: 75px;" >Aircraft</th>
			<th class="sorter-timeExpire" style="width: 85px;">Expires</th>
			<th style="width: 60px;">Locked</th>
			<th class="sorter-false" style="width: 85px;">Action</th>
		</tr>
		</thead>
		<tbody>
<%
        int counter = 0;
		for (AssignmentBean assignment : assignments)
		{
			String aircraftReg = Aircraft.getAircraftRegistrationById(assignment.getAircraftId());
			String image = "img/set2_" + assignment.getActualBearingImage() + ".gif";

            AirportBean destination = assignment.getDestinationAirport();
			AirportBean location = assignment.getLocationAirport();
		
			UserBean lockedBy = null;
			if (assignment.getUserlock() != 0)
			{
				lockedBy = Accounts.getAccountById(assignment.getUserlock());
			} 
			else if (!groupMode && assignment.isGroup())
			{
				lockedBy = Accounts.getAccountById(assignment.getGroupId());
			}
			String locked = lockedBy == null ? "-" : lockedBy.getName();
			
			String icao = location.getIcao();
			String destIcao = destination.getIcao();
			
			AirportBean mapAirport = Airports.getAirport(icao);
			double latl = mapAirport.getLat();
			double lonl = mapAirport.getLon();
			
			AirportBean mapDestAirport = Airports.getAirport(destIcao);
			double destLatl = mapDestAirport.getLat();
			double destLonl = mapDestAirport.getLon();
			
			assignmentsTotalPay += assignment.calcPay();
%>
			<script type="text/javascript">
				if (typeof loc['<%=icao%>'] !== 'undefined') 
				{
					var len = loc['<%=icao%>'].length;
				} 
				else 
				{
					loc['<%=icao%>'] = [];
					len = 0;
				}
				
				loc['<%=icao%>'][len] = [];
				loc['<%=icao%>'][len].latl = <%=latl%>;
				loc['<%=icao%>'][len].lonl = <%=lonl%>;
				loc['<%=icao%>'][len].pay = "<%=Formatters.currency.format(assignment.calcPay())%>";
				loc['<%=icao%>'][len].cargo = "<%=assignment.getSCargo()%>";
				loc['<%=icao%>'][len].status = "selected";
				loc['<%=icao%>'][len].dist = <%=assignment.getActualDistance()%>;
				loc['<%=icao%>'][len].dest = [];
				loc['<%=icao%>'][len].dest.icao = '<%=destIcao%>';
				loc['<%=icao%>'][len].dest.latl = <%=destLatl%>;
				loc['<%=icao%>'][len].dest.lonl = <%=destLonl%>;
		
				var mapCenter = {latl: <%=latl%>, lonl: <%=lonl%>};
				
			</script>
			
			<tr>
			<td>
				<input type="checkbox" id="mycheckbox<%=counter%>" name="select" value="<%= assignment.getId() %>" <%=lockedBy!=null ? "disabled" : "" %>/>
			</td>
			<td class="numeric"><%= Formatters.currency.format(assignment.calcPay()) %></td>
			<td class="numeric"><%= Formatters.currency.format(assignment.getPilotFee()) %></td>
		
<%	
			if (assignment.getActive() == 1)
			{ 
%>		
				<td>[enroute]</td>
<%	
			} 
			else if (assignment.getActive() == 2)
			{ 
%>
				<td>
                    <a href="#" onclick="gmap.setSize(620,520);gmap.setUrl('gmap.jsp?icao=<%= location.getIcao() %>&icaod=<%= destination.getIcao() %>');gmap.showPopup('gmap');return false;" id="gmap">
                        <img src="<%= location.getDescriptiveImage(Fbos.getFboByLocation(assignment.getLocation())) %>" style="border-style: none; vertical-align:middle;" /></a><a title="<%= location.getTitle() %>" class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getLocation()) %>"><%= assignment.getLocation() %>
                    </a> [on hold]
                </td>
<%
			} 
			else 
			{ 
%>		
				<td>
                    <a href="#" onclick="gmap.setSize(620,520);gmap.setUrl('gmap.jsp?icao=<%= location.getIcao() %>&icaod=<%= destination.getIcao() %>');gmap.showPopup('gmap');return false;" id="gmap">
                        <img src="<%= location.getDescriptiveImage(Fbos.getFboByLocation(assignment.getLocation())) %>" style="border-style: none; vertical-align:middle;" /></a><a title="<%= location.getTitle() %>" class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getLocation()) %>"><%= assignment.getLocation() %>
                    </a>
                </td>
<%  
			} 
%>
			<td><img src="img/blankap.gif" style="vertical-align:middle;" /><a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getFrom()) %>"><%= assignment.getFrom() %></a></td>
			<td>
                <a href="#" onclick="gmap.setSize(620,520);gmap.setUrl('gmap.jsp?icao=<%= location.getIcao() %>&icaod=<%= destination.getIcao() %>');gmap.showPopup('gmap');return false;" id="gmap1">
                <img src="<%= destination.getDescriptiveImage(Fbos.getFboByLocation(assignment.getTo())) %>" style="border-style: none; vertical-align:middle;" /></a><a title="<%= destination.getTitle() %>" class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getTo()) %>"><%= assignment.getTo() %>
                </a>
            </td>
			<td class="numeric"><%= assignment.getActualDistance() %></td>
			<td class="numeric"><%= assignment.getActualBearing() %> <img src="<%= image %>" /></td>
			<td><%= assignment.getSCargo() %></td>
			<td><%= assignment.getComment() %></td>
			<td><%= assignment.getType() == AssignmentBean.TYPE_ALLIN ? "A" : "T" %></td>
			<td><%= aircraftReg == null ? "[not provided]" : aircraftReg %></td>
			<td><%= assignment.getSExpires() %></td>
			<td><%= locked %></td>
			<td>
<%		
			if ((assignment.deleteAllowed(user) && assignment.getActive() == 0) || (assignment.deleteAllowed(user) && assignment.getActive() == 2))
			{	
%>	
<%
		  		if (lockedBy != null) 
				{
					if (groupMode) 
					{ 
%>
						<a class="link" href="javascript:doSubmit4(<%= assignment.getId() %>)">Unlock</a>
<%
					} 
					else 
					{ 
%>	
						<a class="link" href="javascript:doSubmit5(<%= assignment.getId() %>)">Unlock</a>
<%				
					}
 				} 
 				else 
 				{ 
%>
					<a class="link" href="javascript:doSubmit2(<%= assignment.getId() %>)">Edit</a>
<%					if (groupMode && assignment.isFerry())
					{ 
%>
						<a class="link" href="javascript:doSubmit3(<%= assignment.getId() %>)">Delete</a>
<% 					}
				}
   			} 
%>
			</td>
		</tr>
<%
            counter++;
		} 
%>

	</tbody>
	</table>

	<div>
	<div>
<%		if( groupMode && user.groupMemberLevel(groupId) >= UserBean.GROUP_STAFF )
		{
%>	
	Warning: <br/>
	This will overwrite any existing comment for the selected assignments!<br/>
	<label>
        You must click the "Add comment" button.<br/>
	    <input name="assignmentComment" type="text" size="65" maxlength="250">
    </label>
	<input type="button" value="Add comment to selected assignments" onclick="doSubmitComment(this.form.select)" /><br/>
<%
		}
%>	
	</div>
	<br/>
	<a class="link" href="javascript:checkAll()">Select All</a>  |            
	<a class="link" href="javascript:checkNone()">De-Select</a>
	<input type="button" name="add_Selected" value="Add Selected to My Flight" onclick="doSubmit(this.form.select)" />
<%
		if (groupId != -1) 
		{ 
%>			<input type="button" name="cancel_Selected" value="Cancel Selected Assignments" onclick="doSubmit5(this.form.select)" /> 
<%		} 
		else 
		{ 
%>			<input type="button" name="delete_Selected" value="Delete Selected Assignments" onclick="doSubmit3(this.form.select)" />     
<%		}
%>
		<a href="#lb" id="show-map">Map my Flights</a> 
		<br/><br/>
<%		if( (groupMode && user.groupMemberLevel(groupId) >= UserBean.GROUP_STAFF) || (!groupMode && (user.groupMemberLevel(transferId) >= UserBean.GROUP_STAFF || transferId==user.getId())))
		{
			if(user.getMemberships() != null)
			{
%>				<br/>
				Move to Group (where I am Staff)<br/>
				<input type="button" id="addSelectedButton" value="Add Selected Assignments To ->" />
		<script type="text/javascript">
			$("#addSelectedButton").click(
				function(e)
				{
                    var el = $("#addToGroup");
					if (window.confirm("Are you sure you want to tranfer selected assignments to " + el.find("option:selected").text() + "?"))
					{
						doSubmit6(document.assignmentForm.select,el.val());
					}		 
				}
			);
		</script>		
				<select id="addToGroup" class="formselect">
<%
    Collection<Groups.groupMemberData> values = user.getMemberships().values();
    Groups.groupMemberData[] memberGroups = values.toArray(new Groups.groupMemberData[values.size()]);

            for (Groups.groupMemberData memberGroup : memberGroups)
            {
%>
                    <option class="formselect" value="<%= memberGroup.groupId%>"><%= memberGroup.groupName%>
                    </option>
<%
            }
%>
				</select>
<%
			}
%>
		<br/><br/>
		</div>
        <div>
        	Search All Groups<br/>
			<input type="button" id="transferButton" name="transferButton" value="Add Selected Assignments To ->" />
			<script type="text/javascript">
			$("#transferButton").click(
				function()
				{			 
					if (window.confirm("Are you sure you want to tranfer selected assignments to " + $("#transfername").val() + "?"))
					{
						doSubmit6(document.assignmentForm.select,$("#transfer").val());
					}		 
				}
			);
			</script>		
            <input type="text" id="transfername" name="transfername" />
            <input type="hidden" id="transfer" name="transfer" />
        </div>
<%		}
%>
		<br/><br/>
		
		Total Pay for Assignments:  <b>$ <%= numberFormat.format(assignmentsTotalPay) %></b>
<% 
	} 
	else 
	{ 
%>
		<div class="message">No assignments</div>
<% 
	} 
%>
<%	
	if (groupMode && user.groupMemberLevel(groupId) >= UserBean.GROUP_STAFF) 
	{	
%>
		<div class="formgroup">
		<input name="newassignment" type="submit" class="button" value="New assignment"/>
		</div>
<%	
	} 
%>
	</div>	
	</form>
</div>
</div>

<!-- This is the lightbox markup, keep the JSP stuff all intact, and make sure that the aircraft
	information table is between the curly braces of the JSP conditional clause -->
	<div style="display: none;">
		<div style="width: 100%; height: 100%;" id="lb">
			<table style="height: 100px; width: 880px; vertical-align: middle;" border="1">
				<tr><th colspan="2">Legend</th></tr>
				<tr>
					<td><img src="https://google-maps-icons.googlecode.com/files/airport.png" />Assignment Location</td>
					<td><img src="https://maps.google.com/mapfiles/kml/pal2/icon56.png" />Destination Airport</td>
				</tr>
				<tr><td colspan="2"><input type="checkbox" value="draw-lines" id="draw-lines" checked/><strong>Show Lines</strong></td></tr>
			</table>
			<div id="map_canvas" style="width:900px; height:550px">
				<div style="width: 220px; height: 19px; margin: 250px auto 0 auto;"><img src="img/ajax-loader.gif" /></div>
			</div>
		</div>
	</div>
	
</body>
</html>
