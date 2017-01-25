<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.*"
%>
<%@ page import="net.fseconomy.beans.AircraftBean" %>
<%@ page import="net.fseconomy.beans.UserBean" %>
<%@ page import="net.fseconomy.beans.ModelBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    UserBean owner = null;
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css">
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
    <script src="../scripts/AdminAutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#ownername", "#owner", <%= Accounts.ACCT_TYPE_ALL %>)
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
<%
    String message = Helpers.getSessionMessage(request);
    if (message != null)
    {
%>
        <div class="message"><%= message %></div>
<%
    }

    if (request.getParameter("submit") == null && (message == null))
    {
%>
    <h2>Enter Aircraft Registration</h2>
	<div class="form" style="width: 400px">
	    <form method="post">
	        <table>
	            <tr>
	                <td>
	                    Registration :
	                    <input name="registration" type="text" class="textarea" size="10" />
	                </td>
	            </tr>
	            <tr>
	                <td>
	                    <input type="submit" class="button" value="GO" />
	                    <input type="hidden" name="submit" value="true"/>
	                    <input type="hidden" name="return" value="/admin/aircraftedit.jsp"/>
	                </td>
	            </tr>
	        </table>
	    </form>
	</div>
<%
    }
    else if (request.getParameter("submit") != null)
    {
        String registration = request.getParameter("registration");
        AircraftBean aircraft = Aircraft.getAircraftById(Aircraft.getAircraftIdByRegistration(registration));
        if (aircraft == null)
        {
            message = "Aircraft Not Found";
        }
        else
        {
            owner = Accounts.getAccountById(aircraft.getOwner());
        }

        if (message != null)
        {
%>
    <div class="message"><%= message %></div>
<%
        }
	
        if (aircraft != null)
        {
%>
        <h2>Edit Aircraft Data</h2>

	    <div class="form" style="width: 600px">
		    <form method="post" action="/userctl">
		        <table>
		            <caption><%= aircraft.getRegistration() %></caption>
		                <tr><td colspan="3"><b>User Fields</b></td></tr>
		                <tr>
			                <td>New registration</td><td><input name="newreg" type="text" class="textarea" size="8" /></td>
		                </tr>
                    <tr>
			                <td>Home base</td><td><input name="home" type="text" class="textarea" value="<%= aircraft.getHome()%>" size="4"/></td>
		                </tr>
		                <tr>
                            <td>Distance bonus</td><td>$ <input name="bonus" type="text" class="textarea" value="<%= aircraft.getBonus()%>" size="4"/></td>
                        </tr>
                        <tr>
                            <td>Accounting</td>
                            <td>
                                <select name="accounting" class="formselect">
                                    <option class="formselect" value="1" <%= aircraft.getAccounting() == 1 ? "selected" : "" %>>Tacho</option>
                                    <option class="formselect" value="2" <%= aircraft.getAccounting() == 2 ? "selected" : "" %>>Hour</option>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <td>Wet price</td><td>$ <input name="rentalPriceWet" type="text" class="textarea" value="<%= aircraft.getRentalPriceWet()%>" size="4"/></td>
                        </tr>
                        <tr>
                            <td>Dry price</td><td>$ <input name="rentalPriceDry" type="text" class="textarea" value="<%= aircraft.getRentalPriceDry()%>" size="4"/></td>
                        </tr>
                        <tr>
                            <td>Max Rental Time</td>
                            <td>
                            <select name="maxRentTime" class="formselect">
<%
            int intervals = 1800;
            int seconds = 3600;
            for (int c=0; c< 19; c++, seconds+=intervals)
            {
                int minutes = seconds/60;
                int hours = minutes/60;
                String time = Formatters.twoDigits.format(hours) + ":" + Formatters.twoDigits.format(minutes%60);
%>
				                <option value="<%= seconds %>" <%= aircraft.getMaxRentTime() == seconds ? "selected" : "" %>><%= time %> Hours</option>
<%
            }
%>
                            </select>
                        </td>

                    </tr>
                    <tr>
                        <td>Equipment Installed</td>
                        <td>
                            <input name="equip-ifr" type="checkbox" class="formselect" value="1" <%= (aircraft.getEquipment() & ModelBean.EQUIPMENT_IFR_MASK) > 0 ? "checked" : "" %>>IFR</input>
                            <input name="equip-gps" type="checkbox" class="formselect" value="2" <%= (aircraft.getEquipment() & ModelBean.EQUIPMENT_GPS_MASK) > 0 ? "checked" : "" %>>GPS</input>
                            <input name="equip-ap" type="checkbox" class="formselect" value="4" <%= (aircraft.getEquipment() & ModelBean.EQUIPMENT_AP_MASK) > 0 ? "checked" : "" %>>AP</input>
                        </td>
                    </tr>
                    <tr>
                        <td>On sale for</td><td>$ <input name="sellPrice" type="text" class="textarea" value="<%= aircraft.getSellPrice() == 0 ? "" : (""+aircraft.getSellPrice()) %>" size="6"/></td>
                    </tr>
                    <tr>
                        <td>Advertise for ferry flight</td><td><input type="checkbox" name="advertiseFerry" value="true" <%= aircraft.isAdvertiseFerry() ? "checked" : "" %>/></td>
                    </tr>
                    <tr>
                        <td>Allow renters to make repairs</td><td><input type="checkbox" name="allowRepair" value="true" <%= aircraft.isAllowRepair() ? "checked" : "" %>/></td>
                    </tr>
                    <tr></tr>
                    <tr>
                        <td colspan="3"><b>System Fields</b></td>
                    </tr>
                    <tr>
                        <td>Owner:</td>
                        <td>
                            <input type="hidden" id="owner" name="owner" value="<%= owner !=null ? owner.getId() : "" %>"/>
                            <input type="text" id="ownername" name="ownername"  value="<%= owner !=null ? owner.getName() : "" %>" />
                        </td>
                    </tr>
                    <tr>
                        <td>Current Location   :</td><td><input name="location" type="text" class="textarea" value="<%= aircraft.getLocation()%>" size="4"/></td>
                    </tr>
                    <tr>
                        <td>User Lock</td><td><input name="userlock" type="text" class="textarea" value="<%= aircraft.getUserLock()%>" size="4"/></td>
                    </tr>

                    <tr>
                        <td>
                            <input type="submit" class="button" value="Update"/>
                            <input type="hidden" name="event" value="updateAircraft"/>
                            <input type="hidden" name="id" value="<%= aircraft.getId()%>"/>
                            <input type="hidden" name="returnpage" value="/admin/aircraftedit.jsp"/>
                        </td>
                    </tr>

                </table>
            </form>

            <ul class="footer">
                <li>Notes :</li>
                <li>New Registration will change the registration in the Aircraft, Payments, Log & Damage tables</li>
                <li>Owner is the numeric account id of the planes owner or zero for a system owned plane</li>
                <li>Null in Current Location means Aircraft is rented and a Flight is in progress</li>
                <li>User Lock is the numeric account id of the User who has the plane rented</li>
            </ul>

        </div>
<%	    }
    }
%>

</div>
</div>
</body>
</html>
