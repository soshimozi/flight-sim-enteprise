<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, java.text.*, net.fseconomy.data.*, java.net.*"%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%
    String sGroup = request.getParameter("groupId");

    String groupName = "";
    String mapViewer = "";
    String selector;
    boolean groupPage = sGroup != null;
    SimpleDateFormat dateFormat = user.getDateFormat();
    dateFormat.setTimeZone(user.getTimeZone());
    int groupId = -1;

    if (!groupPage)
    {
        selector = "pilot " + user.getName();
        mapViewer = "pilot=" + user.getName();
    } else
    {
        groupId = Integer.parseInt(sGroup);
        UserBean[] group = data.getGroupById(groupId);
        if (group.length > 0)
            groupName = group[0].getName();
        selector = "group " + groupName;
        mapViewer = "group=" + group[0].getId();
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
<body class="yui-skin-sam">
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">

<div class="content">
	<div id="panel">
		<div class="hd"></div>
		<div class="bd">
			<table class="flightLog">
			<tbody>
				<tr><td class="logHead" id="logDate"></td><td class="logHead cost" id="logFromTo"></td></tr>
				<tr><td class="space"></td><td></td></tr>
				<tr><td class="type">Income</td><td class="cost" id="logIncome"></td></tr>
				<tr><td class="space"></td><td></td></tr>
				<tr><td class="type">Rental</td><td class="cost"></td></tr>
				<tr><td class="type indent">Amount</td><td class="cost" id="logRentalUnits"></td></tr>
				<tr><td class="type indent">Cost per unit</td><td class="cost ul" id="logRentalPrice"></td></tr>
				<tr><td class="type">Total rental cost</td><td class="cost total" id="logRentalCost"></td></tr>
				<tr><td class="space"></td><td></td></tr>
				<tr><td class="type">Additional cost</td><td class="cost"></td></tr>
				<tr><td class="type indent">Fuel</td><td class="cost" id="logFuelCost"></td></tr>
				<tr><td class="type indent">Landing Fee</td><td class="cost" id="logLandingFee"></td></tr>
				<tr>
				  <td class="type indent">Additional Crew </td>
				  <td class="cost" id="logCrewCost"></td></tr>
				<tr>
				  <td class="type indent">Ground Crew Fee</td><td class="cost ul" id="logFboAssignmentFee"></td></tr>
				<tr><td class="type">Total additional cost</td><td class="cost total" id="logAdditionalCost"></td></tr>
				<tr><td class="space"></td><td></td></tr>
				<tr><td class="type">Distance bonus</td><td class="cost total" id="logBonus"></td></tr>	
				<tr class="total"><td class="type">Earnings this flight</td><td class="cost total" id="logTotal"></td></tr>
		
				<tr><td class="space"></td><td></td></tr>
				<tr id="group1"><td class="type total" id="logGroupName"></td><td class="cost total" id="logPaidToGroup"></td></tr>
				<tr id="group2"><td class="type total">Paid to pilot</td><td class="cost total" id="logPaidToPilot"></td></tr>
				<tr><td class="space"></td><td></td></tr>
		
			</tbody>
			</table>
		</div>	
	</div>
<%
	String base = response.encodeURL("http://" + InetAddress.getLocalHost().getHostName() + "/fseconomy/xml?query="  + (groupPage ? ("groupLog&id=" + groupId) : "userLog"));	 
	String resultElement = groupPage ? "groupLog" : "userLog";
%>
	<h3>All flights for <%= selector %></h3>
	<div id="dataTable"></div>
	<div id="paging"></div>
	<a class="link" href="javascript:void(window.open('<%= response.encodeURL("logviewer.jsp?" + mapViewer) %>','LogViewer','status=no,toolbar=n,height=750,width=680'))">[View maps]</a>
	<script type="text/javascript">
		var myPanel = new YAHOO.widget.Panel("panel", {
			modal: true,
			fixedcenter: true,
			close: true,
			visible: false
		});
		myPanel.setHeader("Log");
		myPanel.render();
		
		var wait = new YAHOO.widget.Panel("wait",  
                     { width: "240px", 
                       fixedcenter: true, 
                       close: false, 
                       draggable: false, 
                       zindex:4,
                       modal: true,
                       visible: false
                     } 
                 );
		wait.setHeader("Loading log entry");
		wait.setBody("<img src=\"http://us.i1.yimg.com/us.yimg.com/i/us/per/gr/gp/rel_interstitial_loading.gif\"/>");
		wait.render(document.body);
		
		var formatInt = function(value) {
			return (value < 10 ? "0" : "") + value;
		}
		var sDate = function(number) {
			var value = new Date(parseInt(number));
			return value.getFullYear() + "." + formatInt(1 + value.getMonth()) + "." + formatInt(value.getDate()) +
				" " + formatInt(value.getHours()) + ":" + formatInt(value.getMinutes()) + ":" + formatInt(value.getSeconds());
		}
		var sMoney = function(number) {
			return "$ " + parseFloat(number).toFixed(2);
		}
		var sDuration = function(number) {
			var left = parseInt(number);
			var hours = Math.floor(left/3600);
			left -= 3600 * hours;
			var minutes = Math.floor(left/60);
			left -= 60 * minutes;
			return formatInt(hours) + ":" + formatInt(minutes) + ": " + formatInt(left)
		}
		
		var formatDuration = function(elCell, oRecord, oColumn, oData) {
			elCell.innerHTML = sDuration(oData); 
		}
		var formatMoney = function(elCell, oRecord, oColumn, oData) {
			elCell.innerHTML = sMoney(oData); 
		}

		var formatDate = function(elCell, oRecord, oColumn, oData) {
			elCell.innerHTML = sDate(oData); 
		}
		
		var columnDefs = [
			{ key: "time", label: "Date", formatter: formatDate },
			{ key: "pilot", label: "Pilot" },
			{ key: "from", label: "From" },
			{ key: "to", label: "To" },
			{ key: "aircraft", label: "Aircraft" },
			{ key: "duration", label: "Duration", formatter: formatDuration },
			{ key: "distance", label: "Distance", formatter: "number" },
			{ key: "income", label: "Earnings", formatter: formatMoney }	
		];
		var dataSource = new YAHOO.util.DataSource("<%= base %>&");
		dataSource.responseType = YAHOO.util.DataSource.TYPE_XML; 
		dataSource.responseSchema = {
		    resultNode: "<%= resultElement %>",
		    fields: ["time", "pilot", "from", "to", "duration", "aircraft", "distance", "income", "totalEngineTime",
		    	 "fuelCost", "rentalCost", "landingCost", "bonus", "rentalPrice", "crewCost", "fboAssignmentFee", "pilotFee", "rentalUnits", "forGroup"],
		    metaNode: "results",
		    metaFields: {
		        totalRecords: "amount"
		        
		    }
		};
		
		var buildQueryString = function (state,dt) {
			return "from=" + state.pagination.recordOffset +
				"&amount=" + state.pagination.rowsPerPage;			 
		}

		var paginator = new YAHOO.widget.Paginator(
				{
					rowsPerPage    : 15,
					containers	   : ['paging'],
					pageLinks	   : 7,
					rowsPerPageOptions: [15,25, 40, 50],
					template	   : "<strong>{CurrentPageReport}</strong> {FirstPageLink} {PreviousPageLink} {PageLinks} {NextPageLink} {LastPageLink} {RowsPerPageDropdown}"
				});
				
		var myDataTable = new YAHOO.widget.DataTable("dataTable", columnDefs, dataSource, {
			initialRequest: 'from=0&amount=15',
			generateRequest: buildQueryString,
			paginationEventHandler : YAHOO.widget.DataTable.handleDataSourcePagination,
			paginator:	paginator
			}
		);
		myDataTable.subscribe("rowMouseoverEvent", myDataTable.onEventHighlightRow); 
		myDataTable.subscribe("rowMouseoutEvent", myDataTable.onEventUnhighlightRow); 
		myDataTable.subscribe("rowClickEvent", function(oArgs) {	
            wait.show();	
			var oCallback = {
				success: function(oRequest,oResponse,oPayload) {
					wait.hide();
					var log = oResponse.results[0];
					var additionalCost = - parseFloat(log.fuelCost) - parseFloat(log.landingCost) - parseFloat(log.crewCost) - parseFloat(log.fboAssignmentFee);
					var total = parseFloat(log.income) - parseFloat(log.rentalCost) + additionalCost + parseFloat(log.bonus);
					var paidToPilot = parseFloat(log.pilotFee);
					var paidToGroup = total - paidToPilot;
					
					var el=function(e) { return document.getElementById(e); }
					
					el("logDate").innerHTML = sDate(log.time);
					el("logFromTo").innerHTML =  log.from + " &rarr; " + log.to + " (" + sDuration(log.duration) + ")";
					el("logIncome").innerHTML = sMoney(log.income);
					el("logRentalUnits").innerHTML = log.rentalUnits;
					el("logRentalPrice").innerHTML = sMoney(log.rentalPrice);
					el("logRentalCost").innerHTML = sMoney(-log.rentalCost);
					el("logCrewCost").innerHTML = sMoney(-log.crewCost);
					el("logLandingFee").innerHTML = sMoney(-log.landingCost);
					el("logFuelCost").innerHTML = sMoney(-log.fuelCost);
					
					el("logAdditionalCost").innerHTML = sMoney(-additionalCost);
					el("logFboAssignmentFee").innerHTML = sMoney(-log.fboAssignmentFee);
					el("logBonus").innerHTML = sMoney(log.bonus);
					el("logPaidToPilot").innerHTML = sMoney(paidToPilot);
					el("logPaidToGroup").innerHTML = sMoney(paidToGroup);
					el("logTotal").innerHTML = sMoney(total);
					if (log.forGroup == "")
					{
						el("group1").style.display = "none";
						el("group2").style.display = "none";
					}  else
					{
						el("logGroupName").innerHTML="Paid to group " + log.forGroup;
						el("group1").style.display = "";
						el("group2").style.display = "";											
					}
					myPanel.cfg.setProperty("visible", true); 
					
				},
				failure: function(oRequest,oResponse,oPayload) {},
				scope: this,
				argument: "test"
			}
			dataSource.sendRequest("amount=1&from=" + (oArgs.target.rowIndex + paginator.getStartIndex() - 1), oCallback);
		}); 
	</script>
</div>

</div>
</body>
</html>
