<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%
    Data data = (Data)application.getAttribute("data");

    if (!Data.needLevel(user, UserBean.LEV_MODERATOR) && !Data.needLevel(user, UserBean.LEV_ACA))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
        return;
    }

    String returnPage = request.getRequestURI();

    String htmloptions = "<select><option value=\"\">[No change]</option><option value=\"0\">[Unmap]</option><option value=\"-1\">[Delete]</option>";
    ModelBean[] indexedModels, models = data.getAllModels();
    int max = 0;

    for(ModelBean model1: models)
    {
        if (model1.getId() > max)
            max = model1.getId();
    }

    indexedModels = new ModelBean[max + 1];
    for(ModelBean model: models)
    {
        htmloptions += "<option value=\\\"" + model.getId() + "\\\">" + model.getMakeModel().replace("'","\\\'") + "</option>";
        indexedModels[model.getId()] = model;
    }
    htmloptions += "</select>";

    String allParam = request.getParameter("all");
    boolean all = allParam != null && (allParam.length() == 1 || allParam.length() == 4);

    String targetParam = request.getParameter("target");
    boolean target = targetParam != null && !targetParam.equals("");

    FSMappingBean[] mappings;

    if (all)
    {
        mappings = data.getFilteredMappings(allParam);
    }
    else if (target)
    {
        mappings = data.getMappingByFSAircraft(targetParam);
    }
    else
    {
        mappings = data.getRequestedMappings();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

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
            for (c = 0; c < <%= mappings.length %>; c++)
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
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">

	<a href="<%= response.encodeURL("fsmappings.jsp") %>">Empty Mappings</a>&nbsp;&nbsp;
<%
	String[] mappingfilters = data.getMappingsFilterList();
    for (String mappingfilter : mappingfilters)
    {
%>
        <a href="<%= response.encodeURL("fsmappings.jsp?all=" + mappingfilter) %>">
            <%= mappingfilter %>
        </a>&nbsp;&nbsp;
<%
    }
%>
    <div class="dataTable">
        <form method="post" action="fsmappings.jsp">
            <p>
                Search for <input name="target" type="text" class="textarea">
                in Flight Simulator ID
                <input type="submit" class="button" value="Search">
            </p>
        </form>
	
        <form method="post" action="userctl">
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
	for (int c=0; c < mappings.length; c++)
	{
		String mapping;
		int mappingId = mappings[c].getModel();

        if (indexedModels[mappingId] != null)
            mapping = indexedModels[mappingId].getMakeModel();
        else
            mapping = "";
%>
                    <tr <%= Data.oddLine(c) %>>
                        <td><%= mappings[c].getAircraft() %></td>
                        <td><%= mapping %></td>
                        <td>
                            <div id="map_<%= c %>">
                                <select name="<%= "map"+mappings[c].getId() %>" class="formselect">

                                </select>
                            </div>
                        </td>
                    </tr>
<%
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
