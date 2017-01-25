<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>
<%@ page import="net.fseconomy.beans.AssignmentBean" %>
<%@ page import="net.fseconomy.beans.UserBean" %>
<%@ page import="net.fseconomy.data.Accounts" %>
<%@ page import="net.fseconomy.data.Airports" %>
<%@ page import="net.fseconomy.data.Assignments" %>
<%@ page import="net.fseconomy.data.Fbos" %>
<%@ page import="net.fseconomy.util.Formatters" %>
<%@ page import="java.util.List" %>
<%@ page import="net.fseconomy.beans.CachedAirportBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
    List<AssignmentBean> assignments;

    int transferId = Integer.parseInt(request.getParameter("transferid"));
    String returnPage = request.getParameter("returnPage");
    int assignmentsTotalPay = 0;

    assignments = Assignments.getAssignmentsForTransfer(transferId);

    UserBean accountBean = Accounts.getAccountById(transferId);
    String caption = "Transfer assignments for " + accountBean.getName();

%>

        <table class="assignmentTable tablesorter-default tablesorter">
            <caption><%= caption %>
            </caption>
            <thead>
            <tr>
                <td class="sorter-false"></td>
                <td class="sorter-false" colspan="3">
                    Click Pay Amt to edit
                </td>
            </tr>
            <tr>
                <th style="width: 12em;">Add<br>
                    <small class="text-muted">Locked</small>
                </th>
                <th class="numeric" style="width: 75px;">Pay<br>
                    <small class="text-muted">Pilot Fee</small>
                </th>
                <th style="width: 75px;">Location</th>
                <th style="width: 60px;">From</th>
                <th style="width: 75px;">Dest</th>
                <th class="numeric" style="width: 35px;">NM</th>
                <th class="numeric" style="width: 45px;">Bearing</th>
                <th style="width: 20em;">Cargo</th>
                <th>Comment</th>
                <th class="sorter-timeExpire" style="width: 85px;">Expires</th>
            </tr>
            </thead>
            <tbody>
<%
    int counter = 0;
    for (AssignmentBean assignment : assignments)
    {
        String image = "img/set2_" + assignment.getActualBearingImage() + ".gif";

        CachedAirportBean destination = assignment.getDestinationAirport();
        CachedAirportBean location = assignment.getLocationAirport();

        String lockedBy = null;
        if (assignment.getUserlock() != 0)
        {
            lockedBy = Accounts.getAccountNameById(assignment.getUserlock());
        }
        else if (assignment.getGroupId() != 0)
        {
            lockedBy = Accounts.getAccountNameById(assignment.getGroupId());
        }

        String icao = location.getIcao();
        String destIcao = destination.getIcao();

        CachedAirportBean airportInfo = Airports.cachedAirports.get(icao);
        double latl = airportInfo.getLatLon().lat;
        double lonl = airportInfo.getLatLon().lon;

        airportInfo = Airports.cachedAirports.get(destIcao);
        double destLatl = airportInfo.getLatLon().lat;
        double destLonl = airportInfo.getLatLon().lon;

        assignmentsTotalPay += assignment.calcPay();
%>

<script type="text/javascript">
    if (typeof loc['<%=icao%>'] !== 'undefined') {
        var len = loc['<%=icao%>'].length;
    }
    else {
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
                <td class="tdClick">
<%
        if (lockedBy == null)
        {
%>
                    <input type="checkbox" id="mycheckbox<%=counter%>" name="select" value="<%= assignment.getId() %>">
                    <small class="text-muted">Open</small>
<%
        }
        else
        {
%>
                    <div class="bg-danger" title="<%=lockedBy%>">
                        <input type="checkbox" id="mycheckbox<%=counter%>" name="select"
                               value="<%= assignment.getId() %>">
                        <%=lockedBy.length() > 12 ? lockedBy.substring(0, 12) + "..." : lockedBy%>
                    </div>
<%
        }
%>
                </td>
                <td class="numeric editassignment" data-id="<%=assignment.getId()%>">
                    <%= Formatters.currency.format(assignment.calcPay()) %>
                    <small class="text-muted">
                        <%= Formatters.currency.format(assignment.getPilotFee()) %>
                    </small>
                </td>
<%
        if (assignment.getActive() == 1)
        {
%>
                <td>[enroute]</td>
<%
        }
        else
        {
%>
                <td>
                    <a href="#"
                       onclick="gmap.setSize(620,520);gmap.setUrl('gmap.jsp?icao=<%= location.getIcao() %>&icaod=<%= destination.getIcao() %>');gmap.showPopup('gmap');return false;"
                       id="gmap">
                        <img src="<%= Airports.getDescriptiveImage(location, Fbos.getAirportFboSlotsInUse(assignment.getLocation()) > 0) %>"
                             style="border-style: none; vertical-align:middle;"/>
                    </a>
                    <a title="<%= location.getTitle() %>"
                       href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getLocation()) %>">
                        <%= assignment.getLocation() %>
                    </a>
                    <small><%=assignment.getActive() == 2 ? "[on hold]" : ""%>
                    </small>
                </td>
<%
        }
%>
                <td>
                    <img src="img/blankap.gif" style="vertical-align:middle;"/>
                    <a class="normal" href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getFrom()) %>">
                        <%= assignment.getFrom() %>
                    </a>
                </td>
                <td>
                    <a href="#"
                       onclick="gmap.setSize(620,520);gmap.setUrl('gmap.jsp?icao=<%= location.getIcao() %>&icaod=<%= destination.getIcao() %>');gmap.showPopup('gmap');return false;"
                       id="gmap1">
                        <img src="<%= Airports.getDescriptiveImage(destination, Fbos.getAirportFboSlotsInUse(assignment.getTo()) > 0) %>"
                             style="border-style: none; vertical-align:middle;"/>
                    </a>
                    <a title="<%= destination.getTitle() %>"
                       href="<%= response.encodeURL("airport.jsp?icao=" + assignment.getTo()) %>">
                        <%= assignment.getTo() %>
                    </a>
                </td>
                <td class="numeric"><%= assignment.getActualDistance() %></td>
                <td class="numeric"><%= assignment.getActualBearing() %> <img src="<%= image %>"/></td>
                <td><%= assignment.getSCargo() %></td>
                <td><%= assignment.getComment() %></td>
                <td><%= assignment.getSExpires() %></td>
            </tr>
<%
        counter++;
    }
%>
            </tbody>
        </table>
        Total Pay for Assignments: <strong><%= Formatters.currency.format(assignmentsTotalPay) %>
    </strong>
        <br>
    </div>

