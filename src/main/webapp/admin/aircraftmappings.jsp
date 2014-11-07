<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.HashMap, java.util.List, net.fseconomy.data.*"
%>
<%@ page import="net.fseconomy.beans.FSMappingBean" %>
<%@ page import="net.fseconomy.beans.ModelBean" %>
<%@ page import="net.fseconomy.beans.UserBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR) && !Accounts.needLevel(user, UserBean.LEV_ACA))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    String returnPage = request.getRequestURI();

    List<ModelBean> models = Models.getAllModels();
    HashMap<Integer, ModelBean> modelMap = new HashMap<>();

    String htmloptions = "<select><option value=\"\">[No change]</option><option value=\"0\">[Unmap]</option><option value=\"-1\">[Delete]</option>";

    for(ModelBean model: models)
    {
        modelMap.put(model.getId(), model);
        htmloptions += "<option value=\\\"" + model.getId() + "\\\">" + model.getMakeModel().replace("'", "\\\'") + "</option>";
    }
    htmloptions += "</select>";

    String allParam = request.getParameter("all");
    boolean all = allParam != null && (allParam.length() == 1 || allParam.length() == 4);

    String targetParam = request.getParameter("target");
    boolean target = targetParam != null && !targetParam.equals("");

    List<FSMappingBean> mappings;

    if (all)
        mappings = Aircraft.getFilteredMappings(allParam);
    else if (target)
        mappings = Aircraft.getMappingByFSAircraft(targetParam);
    else
        mappings = Aircraft.getRequestedMappings();
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script type="text/javascript">
        var HTMLoptions = '<%=htmloptions%>';

        //This fixes an issue where textnodes in firstChild are included
        function firstChildElement(el)
        {
            el = el.firstChild;
            while (el && el.nodeType !== 1)
                el = el.nextSibling;

            return el;
        }

        function onLoad()
        {
            for (c = 0; c < <%= mappings.size() %>; c++)
            {
                var obj = document.getElementById("map_" + c);
                var name = firstChildElement(obj).name;
                obj.innerHTML = HTMLoptions;
                obj.firstChild.name = name;
            }
        }

    </script>

</head>
<body onload="onLoad()">

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">

	<a href="<%= response.encodeURL("/admin/aircraftmappings.jsp") %>">Empty Mappings</a>&nbsp;&nbsp;
<%
	List<String> mappingfilters = Aircraft.getMappingsFilterList();
    for (String mappingfilter : mappingfilters)
    {
%>
        <a href="<%= response.encodeURL("/admin/aircraftmappings.jsp?all=" + mappingfilter) %>">
            <%= mappingfilter %>
        </a>&nbsp;&nbsp;
<%
    }
%>
    <div class="dataTable">
        <form method="post" action="/admin/aircraftmappings.jsp">
            <p>
                Search for <input name="target" type="text" class="textarea">
                in Flight Simulator ID
                <input type="submit" class="button" value="Search">
            </p>
        </form>
	
        <form method="post" action="/userctl">
            <div>
                <input type="hidden" name="event" value="mappings"/>
                <input type="hidden" name="returnpage" value="<%=returnPage%>" />
            </div>
            <table>
                <caption>
                    <%= all?"":(target?(targetParam + " "):"Empty ")%>Mappings
                </caption>
                <thead>
                    <tr>
                        <th>Flight Simulator ID</th>
                        <th>Mapping</th>
                        <th>New Mapping</th>
                    </tr>
                </thead>
                <tbody>
<%
    int counter = 0;
	for (FSMappingBean mappingBean : mappings)
	{
		String mapping;
		int mappingId = mappingBean.getModel();

        ModelBean model = modelMap.get(mappingId);

        if(model != null)
            mapping = model.getMakeModel();
        else
            mapping = "";
%>
                    <tr>
                        <td><%= mappingBean.getAircraft() %></td>
                        <td><%= mapping %></td>
                        <td>
                            <div id="map_<%= counter %>">
                                <select name="<%= "map"+mappingBean.getId() %>" class="formselect">

                                </select>
                            </div>
                        </td>
                    </tr>
<%
        counter++;
	}
%>
	            </tbody>
	        </table>
            <div class="formgroup">
                <input type="submit" class="button" value="Update">
            </div>
        </form>
    </div>
</div>
</div>
</body>
</html>
