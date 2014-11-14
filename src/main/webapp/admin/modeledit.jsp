<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />
<jsp:useBean id="model" class="net.fseconomy.beans.ModelBean">
    <jsp:setProperty name="model" property="*"/>
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
        String newModel = request.getParameter("newmodel");
        if (newModel != null)
        {
            model = new ModelBean();
            model.setId(-1);
            if(!newModel.contentEquals(""))
            {
                List<FSMappingBean> result = Aircraft.getMappingById(Integer.parseInt(newModel));
                if (result.size() > 0)
                    model.setCapacity(result.get(0).getCapacity());
            }
            model.setMake("");
            model.setModel("");
        }
        else
        {
            model = Models.getModelById(model.getId());
        }
    }
    else if (error == null)
    {
        try
        {
            Models.updateModel(model, user);
%>
<jsp:forward page="/admin/models.jsp"></jsp:forward>
<%
        }
        catch (DataError e)
        {
            error = e.getMessage();
        }
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

</head>


<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
<% 	if (error != null) 
	{ 
%>
	<div class="error"><%= error %></div>
<%	} 
%>
	<div class="form" style="width: 700px">
	<form method="post" action="/admin/modeledit.jsp">
	<input type="hidden" name="submit" value="true"/>
	<input type="hidden" name="event" value="editAircraft"/>
	<input type="hidden" name="id" value="<%= model.getId() %>"/>
	<table>
	<caption>Edit Model</caption>
	<tr>
		<td>Can Ship</td>
		<td>
		<select name="canShip" class="formselect">
			<option class="formselect" value="0" <%= model.getCanShip()==0 ? "selected" : "" %>>No</option>
			<option class="formselect" value="1"  <%= model.getCanShip()==1 ? "selected" : "" %>>Yes</option>
		</select>		
		</td>
	</tr>
	<tr>
		<td>Make</td><td><input name="make" type="text" class="textarea" value="<%= model.getMake() %>" size="20"/></td>
	</tr>
	<tr>
		<td>Model</td><td><input name="model" type="text" class="textarea" value="<%= model.getModel() %>" size="20"/></td>
	</tr>
	<tr>
		<td>Additional Crew</td>
		<td><input name="crew" type="text" class="textarea" value="<%= model.getCrew() %>" size="4"/></td>
	</tr>
	<tr>
		<td>Number of engines</td><td><input name="engines" type="text" class="textarea" value="<%= model.getEngines() %>" size="4"/></td>
	</tr>
	<tr>
		<td>Number of seats</td><td><input name="seats" type="text" class="textarea" value="<%= model.getSeats() %>" size="4"/></td>
	</tr>
	<tr>
		<td>Empty weight</td><td><input name="emptyWeight" type="text" class="textarea" value="<%= model.getEmptyWeight() %>" size="8"/> 
		Kg</td>
	</tr>
	
	<tr>
		<td>Max takeoff weight</td><td><input name="maxWeight" type="text" class="textarea" value="<%= model.getMaxWeight() %>" size="8"/> 
		Kg</td>
	</tr>
	<tr>
		<td>Cruise speed</td><td><input name="cruise" type="text" class="textarea" value="<%= model.getCruise() %>" size="7"/></td>
	</tr>
	<tr>
		<td>Distance bonus</td><td>$ <input name="bonus" type="text" class="textarea" value="<%= model.getBonus()%>" size="7"/></td>
	</tr>
	<tr>
		<td>Consumption</td><td><input name="gph" type="text" class="textarea" value="<%= model.getGph()%>" size="7"/> 
		Gallons per Hour</td>
	</tr>
	<tr>
		<td>Amount to appear</td><td><input name="amount" type="text" class="textarea" value="<%= model.getAmount()%>" size="7"/> </td>
	</tr>
	<tr>
		<td>Amount For Sale</td><td><input name="numSell" type="text" class="textarea" value="<%= model.getNumSell()%>" size="7"/> </td>
	</tr>
	<tr>
		<td>Appear at airports larger than</td><td><input name="minAirportSize" type="text" class="textarea" value="<%= model.getMinAirportSize()%>" size="7"/> </td>
	</tr>
	
	<tr>
		<td>Equipment</td>	
		<td>
		<select name="equipment" class="formselect">
			<option class="formselect" value="0" <%= model.getEquipment() == ModelBean.EQUIPMENT_VFR_ONLY ? "selected" : "" %>>VFR only</option>
			<option class="formselect" value="1" <%= model.getEquipment() == ModelBean.EQUIPMENT_VFR_IFR ? "selected" : "" %>>Some VFR/Some IFR</option>
			<option class="formselect" value="2" <%= model.getEquipment() == ModelBean.EQUIPMENT_IFR_ONLY ? "selected" : "" %>>IFR only</option>						
		</select>
		</td>
	</tr>
	<tr>
		<td>Accounting</td>	
		<td>
		By Hour Rental
		</td>
	</tr>
	<tr>
		<td>Fuel Type</td>	
		<td>
		<select name="fueltype" class="formselect">
			<option class="formselect" value="0" <%= model.getFueltype() == 0 ? "selected" : "" %>>100LL</option>
			<option class="formselect" value="1"  <%= model.getFueltype() == 1 ? "selected" : "" %>>Jet A</option>
		</select>
		</td>
	</tr>
	<tr>
		<td>Refuel from System only</td>
		<td>
		<select name="fuelSystemOnly" class="formselect">
			<option class="formselect" value="0" <%= model.getFuelSystemOnly()==0 ? "selected" : "" %>>No</option>
			<option class="formselect" value="1"  <%= model.getFuelSystemOnly()==1 ? "selected" : "" %>>Yes</option>
		</select>		
		</td>
	</tr>
	<tr>
		<td>Target Dry Rental Price</td><td>$ <input name="rental" type="text" class="textarea" value="<%= model.getRental()%>" size="7"/></td>
	</tr>
	<tr>
		<td>Default Max Rental Time</td>
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
			<option class="formselect" value="<%= seconds %>" <%= model.getMaxRentTime() == seconds ? "selected" : "" %>><%= time %> Hours</option>
<%	}	
%>
		</select>
		</td>
		
	</tr>
	<tr>
		<td>Target marketprice</td><td>$ <input name="price" type="text" class="textarea" value="<%= model.getPrice() %>" size="9"/></td>
	</tr>
	<tr>
		<td>Price of a single engine</td><td>$ <input name="enginePrice" type="text" class="textarea" value="<%= model.getEnginePrice() %>" size="9"/></td>
	</tr>	
	<tr>
		<td colspan="2">
		<h3>Fuel capacities</h3>
		<table>
		<thead>
		<tr>
			<th colspan="4">Left</th>
			<th colspan="3">Center</th>
			<th colspan="4">Right</th>
		</tr>
		<tr>
			<th>Main</th><th>Aux</th><th>Tip</th><th>Ext</th>
			<th>1</th><th>2</th><th>3</th>
			<th>Main</th><th>Aux</th><th>Tip</th><th>Ext</th>
		</tr>
		</thead>
		<tbody>
		<tr>
			<td><input name="leftMain" type="text" class="textarea" value="<%= model.getCap(1) %>" size="4"/></td>
			<td><input name="leftAux" type="text" class="textarea" value="<%= model.getCap(2) %>" size="4"/></td>						
			<td><input name="leftTip" type="text" class="textarea" value="<%= model.getCap(3) %>" size="4"/></td>
			<td><input name="leftExt1" type="text" class="textarea" value="<%= model.getCap(9) %>" size="4"/></td>
			<td><input name="center" type="text" class="textarea" value="<%= model.getCap(0) %>" size="4"/></td>
			<td><input name="center2" type="text" class="textarea" value="<%= model.getCap(7) %>" size="4"/></td>
			<td><input name="center3" type="text" class="textarea" value="<%= model.getCap(8) %>" size="4"/></td>					
			<td><input name="rightMain" type="text" class="textarea" value="<%= model.getCap(4) %>" size="4"/></td>
			<td><input name="rightAux" type="text" class="textarea" value="<%= model.getCap(5) %>" size="4"/></td>
			<td><input name="rightTip" type="text" class="textarea" value="<%= model.getCap(6) %>" size="4"/></td>											
			<td><input name="rightExt2" type="text" class="textarea" value="<%= model.getCap(10) %>" size="4"/></td>				
		</tr>
		</tbody>
		</table>
		</td>
	</tr>

	
	<tr><td><input type="submit" class="button" value="Update"/></td></tr>
	
	</table>
	</form>
	<ul class="footer">
		<li>Target rental price excludes fuel</li>
		<li>For aircraft with equipment = some IFR/some VFR, rental price excludes equipment</li>
		<li>Empty weight is the weight of the aircraft without fuel or payload</li>
		<li>Max takeoff weight is the maximum total takeoff weight</li>
	</ul>

	</div>
</div>
</div>
</body>
</html>
