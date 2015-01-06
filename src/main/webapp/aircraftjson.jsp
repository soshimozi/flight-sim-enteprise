<%@ page import="net.fseconomy.beans.AssignmentBean" %>
<%@ page import="net.fseconomy.beans.UserBean" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.*" %>
<%@ page import="net.fseconomy.util.Formatters" %>
<%@ page import="net.fseconomy.beans.AircraftBean" %>
<%@ page import="net.fseconomy.dto.*" %>
<%@ page import="net.fseconomy.data.*" %>
<%@ page import="net.fseconomy.util.GlobalLogger" %>
<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>
<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>
<%
  String type = request.getParameter("type");
  String sId = request.getParameter("id");
  String output;
  int role;

  List<AircraftBean> aircraftList;
  MapAircraftInfo aircraftInfo = null;

  try
  {
    switch (type)
    {
      case "aircraft":
        if (sId == null)
          throw new Exception("Missing id");

        int id = Integer.parseInt(sId);

        //check if proper access
        UserBean owner = Accounts.getAccountById(id);
        if (owner.isGroup())
        {
          role = Groups.getRole(id, user.getId());
          if (role < UserBean.GROUP_MEMBER)
            throw new Exception("No permission");
        }
        else if (id != user.getId())
          throw new Exception("No permission");

        aircraftList = Aircraft.getAircraftOwnedByUser(id);
        break;
      default:
        throw new Exception("Invalid parameters.");
    }

    //List<MapAssignments> mapList = new ArrayList<>();
    MapAssignments mapAssignments = null;
    MapData mapData = new MapData();
    mapData.mapAircraftInfo = null;
    mapData.mapAssignments = new ArrayList<>();

    String lastLocation = "";
    String lastDestination = "";
    boolean locChanged = false;
    boolean destChanged = false;
    AirportInfo depart = null;
    AirportInfo dest = null;

    for (AssignmentBean assignment : assignments)
    {
      if(assignment.getActive() == Assignments.ASSIGNMENT_HOLD)
        continue;

      //has location changed
      if(!assignment.getLocation().equals(lastLocation))
      {
        locChanged = true;
        lastLocation = assignment.getLocation();
        depart = Airports.cachedAPs.get(lastLocation);
      }

      //has destination changed
      if(!assignment.getTo().equals(lastDestination))
      {
        destChanged = true;
        lastDestination = assignment.getTo();
        dest = Airports.cachedAPs.get(lastDestination);
      }

      String distance = Integer.toString(Airports.findDistance(lastLocation, lastDestination)) + " NM";
      MapAssignment mapAssignment = new MapAssignment(lastDestination, assignment.getSCargo(), Formatters.currency.format(assignment.getPay()), distance);

      if(locChanged)
      {
        if(mapAssignments != null)
          mapData.mapAssignments.add(mapAssignments);

        mapAssignments = new MapAssignments(depart);
      }

      if(destChanged)
        mapAssignments.destinations.add(dest);

      mapAssignments.assignments.add(mapAssignment);

      locChanged = false;
      destChanged = false;
    }

    mapData.mapAssignments.add(mapAssignments);
    if(aircraftInfo != null)
      mapData.mapAircraftInfo = aircraftInfo;

    response.setContentType("application/json");
    Gson gson = new Gson();
    output = gson.toJson(mapData);

  }
  catch (Exception e)
  {
    response.setStatus(400);
    output = "Error: " + e.getMessage();
    GlobalLogger.logJspLog(output);
  }
%><%= output %>