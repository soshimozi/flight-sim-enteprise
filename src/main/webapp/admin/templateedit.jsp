<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.* "
%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="net.fseconomy.dto.AircraftConfig" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />
<jsp:useBean id="template" class="net.fseconomy.beans.TemplateBean">
    <jsp:setProperty name="template" property="*"/>
</jsp:useBean>

<%
	if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
	{
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
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
			template = Templates.getTemplateById(template.getId());
		}
	} 
	else if (error == null)
	{
		try
		{
			String saId[] = request.getParameterValues("surfType");
			List<Integer> list = new ArrayList<>();

			if(saId != null && saId.length > 0)
			{
				for (String s : saId)
					list.add(Integer.parseInt(s));
			}

			template.setAllowedSurfaceTypes(list);
			template.setFilterModelSet(request.getParameter("filterset"));

			Templates.updateTemplate(template, user);
%>
			<jsp:forward page="/admin/templates.jsp" />
<%
		}
		catch (DataError e)
		{
			error = e.getMessage();
		}
	}

	List<Integer> surfList = template.getAllowedSurfaceTypes();
%>
<%!
	String deviation(int current)
	{
		StringBuilder result = new StringBuilder();
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

    <link rel='stylesheet prefetch' href='//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css'>
    <link rel="stylesheet" type="text/css" href="../css/Master.css"/>

	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>

	<script>

		function checkAll() {
			var field = document.getElementById("templateForm").surfType;
			for (i = 0; i < field.length; i++) {
				field[i].checked = true;
			}
			field.checked = true;  // needed in case of only one box
		}

		function uncheckAll() {
			var field = document.getElementById("templateForm").surfType;
			for (i = 0; i < field.length; i++)
				field[i].checked = false;

			field.checked = false;  // needed in case of only one box
		}

        function CheckIcaos(setId)
        {
            var icaotext = $(setId).val();
            $.ajax({
                type: "POST",
                url: "checkicaosvalid.jsp",
                data: {"icaos": icaotext},
                success:
                    function(data, status)
                    {
                        var errorId = setId + "Errors"
                        var passedId = setId + "Passed"
                        var errorList = setId + "ErrorList"

                        if(data.indexOf('<li>') >= 0)
                        {
                            $(errorList).html(data);
                            $(errorId).show();
                            $(passedId).hide();
                        }
                        else
                        {
                            $(passedId).show();
                            $(errorId).hide();
                        }
                    }
                }
            );
        }

		$(function()
		{
			$('#selectAll').click(function () {
				checkAll();
			});
			$('#deselectAll').click(function () {
				uncheckAll();
			});

			$("#r1").click(function() {
				var test = $(this).val();
				$("#tableFilterParams").show();
				$("#tableFilterModels").hide();
			});
			$("#r2").click(function() {
				var test = $(this).val();
				$("#tableFilterParams").hide();
				$("#tableFilterModels").show();
			});

			function optionSort(a, b) {
				return $(a).data("index") - $(b).data("index");
			}

			$("#btnAdd").click(function () {
				$("#models > option:selected").each(function () {
					$(this).remove().appendTo("#modelset");
				});

				var opts = $("#modelset option").get();
				$("#modelset").html(opts.sort(optionSort));
			});

			$("#btnRemove").click(function () {
				$("#modelset > option:selected").each(function () {
					$(this).remove().appendTo("#models");
				});

				var opts = $("#models option").get();
				$("#models").html(opts.sort(optionSort));
			});

			$('#templateForm').submit(function() {
				var s = "";

				$("#modelset option").each(function () {
					if(s !== "")
							s += ", ";
					s += this.value;
				});

				$("#filterset").val(s);
			});

		});

	</script>
</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

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
		<form method="post" action="/admin/templateedit.jsp" id="templateForm">
			<input type="hidden" name="submit" value="true"/>
			<input type="hidden" name="event" value="editTemplate"/>
			<input type="hidden" name="id" value="<%= template.getId() %>"/>
			<input type="hidden" name="filterset" id="filterset" value="" />

			<table>
				<caption>Edit Template</caption>
				<tr>
					<td>Active</td>
					<td>
						<select name="active" class="formgroup">
							<option class="formselect" value="false" <%= !template.getActive() ? "selected" : "" %>>No</option>
							<option class="formselect" value="true" <%= template.getActive() ? "selected" : "" %>>Yes</option>
						</select>
					</td>
				</tr>
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
<%
	for(int i = 1, e = TemplateBean.MAX_KEEPALIVE_DAYS;i <= e; i++)
	{
%>
						<option class="formselect" value="<%= i %>" <%=  keepAlive == i ? "selected" : "" %>><%= i %> day</option>
<%
	}
%>
					</select>
					</td>
				</tr>
				<tr>
					<td>Allow Time Extension</td>
					<td>
						<select name="noExt" class="formgroup">
							<option class="formselect" value="false" <%= !template.getNoExt() ? "selected" : "" %>>Yes</option>
							<option class="formselect" value="true" <%= template.getNoExt() ? "selected" : "" %>>No</option>
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
					<td>
						Target Distance
					</td>
					<td>
						<input name="targetDistance" type="text" class="textarea" value="<%= template.getTargetDistance() %>" size="7"/>
						Dev <select name="distanceDev" class="formselect"><%= deviation(template.getDistanceDev())%></select>
					</td>
				</tr>

				<tr>
					<td>Runway Surface Type</td>
					<td>
						<div style="border: 1px solid #000000">
							<div><small>Note: if none are selected it will default to ALL</small></div>
							<hr>
							<div style="text-align: center;"><span id="selectAll" style="font-size: small;">Select All</span> <span id="deselectAll" style="font-size: small;">Deselect All</span><br></div>
<%
	for(AirportBean.Surface s: AirportBean.Surface.getValues())
	{
		int index = s.getIndex();
%>
							<label>&nbsp;&nbsp;<input type="checkbox" name="surfType" value="<%=index%>" <%=surfList.contains(index) ? "checked" : ""%>> <%=s.getName()%></label><br>
<%
	}
%>
						</div>
					</td>
				</tr>

				<tr>
					<td>From/To runways minimum length</td><td><input name="matchMinSize" type="text" class="textarea" value="<%= template.getMatchMinSize() %>" size="7"/>
					ft</td>
				</tr>
				<tr>
					<td>From/To runways maximum length</td><td><input name="matchMaxSize" type="text" class="textarea" value="<%= template.getMatchMaxSize() %>" size="7"/>
					ft</td>
				</tr>
				<tr>
					<td style="vertical-align: middle">From</td>
					<td>
						<textarea rows="4" cols="50" name="icaoSet1" id="icaoSet1"><%= template.getIcaoSet1() == null ? "" : template.getIcaoSet1()  %></textarea><br>
						<div class="btn btn-link" onclick="CheckIcaos('#icaoSet1');">Check</div><br>
                        <div id="icaoSet1Passed" style="display: none">No Errors</div>
						<div id="icaoSet1Errors" style="display: none">
							Bad ICAO codes
							<ul id="icaoSet1ErrorList">
							</ul>
						</div>
					</td>
				</tr>
				<tr>
					<td style="vertical-align: middle">To</td>
                    <td>
						<textarea rows="4" cols="50"  name="icaoSet2" id="icaoSet2"><%= template.getIcaoSet2() == null ? "" : template.getIcaoSet2()  %></textarea><br>
                        <div class="btn btn-link" onclick="CheckIcaos('#icaoSet2');">Check</div><br>
                        <div id="icaoSet2Passed" style="display: none">No Errors</div>
						<div id="icaoSet2Errors" style="display: none">
							Bad ICAO codes
							<ul id="icaoSet2ErrorList">
							</ul>
						</div>
					</td>
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
                    <td>All-In Direct Flight</td>
                    <td>
                        <select name="direct" class="formgroup">
                            <option class="formselect" value="false" <%= !template.getDirect() ? "selected" : "" %>>Non-Direct</option>
                            <option class="formselect" value="true" <%= template.getDirect() ? "selected" : "" %>>Direct</option>
                        </select>
                    </td>
                </tr>
				<tr>
				<!-- All-In changes begin here -->
				<td colspan="2" style="border:1px solid #000;padding: 4px;">
					<div>
						<b>Filters - these values only apply to All-In jobs</b>
					</div>
					<div style="display: none;">
						Percentage of Aircraft Matching filters to use:<br>
						<input name="percentToUse" type="text" class="textarea" value="<%= template.getPercentToUse() %>" size="7"/>%
					</div>
					<div>
						Select filter by:<br>
						<input type="radio" name="filterByModels" id="r1" value="false" <%= !template.isFilterByModels() ? "checked" : "" %> /><label>Seats & Speeds</label>
						<input type="radio" name="filterByModels" id="r2" value="true" <%= template.isFilterByModels() ? "checked" : "" %>/><label>Aircraft Models</label>
					</div>
					<table id="tableFilterParams" style="display: <%= template.isFilterByModels() ? "none" : "initial" %>;">
						<tr>
							<td colspan="2">
								<div style="font-size:10pt">Notes:
									<ul>
							 			<li>Please enter both TO and FROM values for template to work correctly</li>
							 			<li>To set an exact match value, place the value in both the "From" and "To" fields.</li>
									</ul>
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
					<table id="tableFilterModels" style="display: <%= template.isFilterByModels() ? "initial" : "none"%>;" >
						<tr>
							<td colspan="3">Select the aircraft model</td>
						</tr>
						<tr>
							<td>
								<select name="models" id="models" multiple="multiple" rows=2 style="width: 200px; height: 200px; margin: auto;">
<%
	List<AircraftConfig> list = Aircraft.getAircraftConfigs();
	List<Integer> filterList = new ArrayList<>();
	int index = 0;
	for(AircraftConfig ac: list)
	{
		index++;
		if(template.isInFilterModelSet(ac.modelId))
		{
			filterList.add(index);
			continue;
		}
%>
									<option value=<%=ac.modelId%> data-index=<%=index%>><%=ac.makemodel%></option>
<%
	}
%>
								</select>
							</td>
							<td>
								<div style="align-content: center">
								<input id="btnAdd" type="button" value=">>" /><br><br>
								<input id="btnRemove" type="button" value="<<" />
								</div>
							</td>
							<td>
								<select name="modelset" id="modelset" multiple="multiple" rows=2 style="width: 200px; height: 200px; margin: auto;">
<%
	Integer[] idx = filterList.toArray(new Integer[filterList.size()]);
	index = 0;
	for(AircraftConfig ac: list)
	{
		if(!template.isInFilterModelSet(ac.modelId))
			continue;
%>
									<option value=<%=ac.modelId%> data-index=<%=idx[index]%>><%=ac.makemodel%></option>
<%
		index++;
	}
%>
								</select>
							</td>
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
