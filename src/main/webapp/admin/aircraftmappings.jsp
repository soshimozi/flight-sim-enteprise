<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.HashMap, java.util.List, net.fseconomy.data.*, net.fseconomy.beans.* "
        %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
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

    String htmloptions = "<select id=\"selectMap\">";

    for(ModelBean model: models)
    {
        modelMap.put(model.getId(), model);
        htmloptions += "<option value=\"" + model.getId() + "\">" + model.getMakeModel().replace("'", "\\\'") + "</option>";
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

    <link href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css" rel="stylesheet" >
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>

    <script type="text/javascript">
        var HTMLoptions = '<%=htmloptions%>';

        function doSubmit(me, mapid)
        {
            var form = document.getElementById("formMapping");
            form.mapevent.value = me;
            form.mapid.value = mapid;
            form.submit();
        }

        function doMapSelect(id, model)
        {
            var form = document.getElementById("formMapping");
            form.mapid.value = id;

            var form1 = document.getElementById("formMapModal");
            form1.selectMap.value = model;

            $("#myModal").modal('show');
        }

        function doMapSave()
        {
            var form = document.getElementById("formMapModal");
            var modelid = form.selectMap.options[form.selectMap.selectedIndex].value;

            var form1 = document.getElementById("formMapping");
            form1.mapevent.value = "map";
            form1.modelid.value = modelid;
            form1.submit();
        }

    </script>

</head>
<body>

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
                    <label>
                        Search for <input name="target" type="text" class="textarea"> in Flight Simulator ID
                    </label>
                    <input type="submit" class="button" value="Search">
                </p>
            </form>

            <form id="formMapping" method="post" action="/userctl">
                <div>
                    <input type="hidden" name="event" value="mappings"/>
                    <input type="hidden" name="mapevent" value=""/>
                    <input type="hidden" name="mapid" value=""/>
                    <input type="hidden" name="modelid" value=""/>
                    <input type="hidden" name="returnpage" value="<%=returnPage%>" />
                </div>
                <table>
                    <caption>
                        <%= all ? "" : (target ? (targetParam + " ") : "Empty")%> Mappings
                    </caption>
                    <thead>
                    <tr>
                        <td class="sorter-false" colspan="1"><span style="font-size: 12pt;color: gray">Click title to edit mapping</span></td>
                    </tr>
                    <tr>
                        <th>Flight Simulator ID</th>
                        <th>Mapping</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <%
                        for (FSMappingBean mappingBean : mappings)
                        {
                            String mapping;
                            int mappingId = mappingBean.getModel();

                            ModelBean model = modelMap.get(mappingId);

                            if(model != null)
                            {
                                mapping = model.getMakeModel();
                            }
                            else
                            {
                                mapping = "";
                            }
                    %>
                    <tr>
                        <td><a onclick="doMapSelect(<%= mappingBean.getId() %>, <%= mappingBean.getModel() %>);"><%= mappingBean.getAircraft() %></a></td>
                        <td><%= mapping %></td>
                        <td>
                            <div>
                                <%
                                    if(!mapping.equals(""))
                                    {
                                %>
                                <a class="btn btn-sm btn-primary"  onclick="doSubmit('unmap', <%= mappingBean.getId() %>);">[Unmap]</a>
                                <%
                                    }
                                %>
                                <a class="btn btn-sm btn-primary"  onclick="doSubmit('delete', <%= mappingBean.getId() %>);">[Delete]</a>
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

<!-- Modal HTML -->
<div id="myModal" class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Aircraft Mapping</h4>
            </div>
            <div class="modal-body">
                <p>Please select the desired model to map.</p>
                <form id="formMapModal">
                    <label>
                        Model:&nbsp;<%=htmloptions%>
                    </label>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary" onclick="doMapSave();">Save changes</button>
            </div>
        </div>
    </div>
</div>

</body>
</html>
