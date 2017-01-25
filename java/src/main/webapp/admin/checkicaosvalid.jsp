<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="net.fseconomy.data.Airports" %>
<%@ page import="net.fseconomy.util.Helpers" %>
<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>
<%
    String output = "";
    String icaos = request.getParameter("icaos").trim();
    if(!Helpers.isNullOrBlank(icaos))
    {
        String[] tmp = icaos.split(",");

        List<String> list = new ArrayList<>();
        for (String icao : tmp)
            list.add(icao.trim());

        List<String> badIcaos = Airports.CheckValidIcaos(list);

        for (String icao : badIcaos)
            output += "<li>" + icao + "</li>";
    }
%>
<%= output %>
