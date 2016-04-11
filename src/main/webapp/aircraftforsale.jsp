<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    //setup return page if action used
    String returnPage = request.getRequestURI();
    response.addHeader("referer", request.getRequestURI());

    int modelId = 0;
    String sModel = request.getParameter("model");
    if(sModel != null && Helpers.isInteger(sModel))
        modelId = Integer.parseInt(sModel);
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css" rel="stylesheet">
    <link type="text/css" href="css/redmond/jquery-ui.css" rel="stylesheet"/>
    <link href="css/Master.css" rel="stylesheet" type="text/css" />
    <link href="css/tablesorter-style.css" rel="stylesheet" type="text/css" />

    <script type='text/javascript' src="//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
    <script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
    <script type='text/javascript' src='scripts/jquery.tablesorter.js'></script>
    <script type='text/javascript' src="scripts/jquery.tablesorter.widgets.js"></script>
    <script type='text/javascript' src='scripts/parser-checkbox.js'></script>
    <script type='text/javascript' src='scripts/parser-timeExpire.js'></script>
    <script src="scripts/PopupWindow.js"></script>

    <script type='text/javascript'>
        var paramModelid = <%=modelId%>;

        var gmapfs = new PopupWindow();

        function loadPrivateSales()
        {
            $("#aircraftTable").html("<div>Loading... <img src='img/ajax-loader.gif'></div>");
            $("#aircraftTable").load( "aircraftforsaledata.jsp?action=privatesale" );
        }

        function loadSearch()
        {
            if(formValidation())
                return;

            var params = getSearchParameters();
            $("#aircraftTable").html("<div>Loading... <img src='img/ajax-loader.gif'></div>");
            $("#aircraftTable").load( "aircraftforsaledata.jsp?action=search" + params );
        }

        function getSearchParameters() {
            var params = "";

            params = getValue("model");
            params += getValue("lowPrice");
            params += getValue("highPrice");
            params += getValue("lowTime");
            params += getValue("highTime");
            params += getValue("lowPax");
            params += getValue("highPax");
            params += getValue("lowLoad");
            params += getValue("highLoad");
            params += getValue("from");
            params += getValue("distance");

            return params;
        }

        function getValue(name) {
            var item;
            if("model distance".indexOf(name) >= 0)
                item = $("select[name="+name+"]").val();
            else
                item = $("input[name="+name+"]").val();

            return item != undefined && item != "" ? "&" + name + "=" + item : "";
        }

        function purchaseAircraft()
        {
            var form = document.getElementById("formAircraftModal");
            var ebuyer = document.getElementById("groupSelect");

            form.account.value = ebuyer.options[ebuyer.selectedIndex].value;
            form.submit();
        }

        function selectAircraft(aircraftId)
        {
            var form = document.getElementById("formAircraftModal");
            form.id.value = aircraftId;

            $("#aircraftData").load( "aircraftdata.jsp?aircraftid=" + aircraftId );

            $("#myModal").modal('show');
        }

        function doSubmit(id, price, id2)
        {
            if (window.confirm("Do you want to buy " + id + " for " + price + "?"))
            {
                var form = document.getElementById("formAircraftModal");
                form.id.value = id;
                form.account.value = id2;
                form.submit();
            }
        }

        function formValidation() // returns true if error found
        {
            var errorList = "The following errors were found on the Search aircraft for sale form: \n\n";
            var errorFound = 0;
            var numericExpression = /^[0-9]+$/;

            if (!$("input[name=lowPrice]").val().match(numericExpression) && $("input[name=lowPrice]").val() != "") {
                errorList += "Low Price must be a number.\n";
                errorFound = 1;
            }

            if (!$("input[name=highPrice]").val().match(numericExpression) && $("input[name=highPrice]").val() != "") {
                errorList += "High Price must be a number.\n";
                errorFound = 1;
            }
            else if($("input[name=highPrice]").val() != ""){
                var a = $("input[name=highPrice]").val();
                var n = Number(a);
                if(n > 1000000000)
                    $("input[name=highPrice]").val(1000000000);
            }

            if (!$("input[name=lowTime]").val().match(numericExpression) && $("input[name=lowTime]").val() != "")
            {
                errorList += "Low Airframe Time must be a number.\n";
                errorFound = 1;
            }

            if (!$("input[name=highTime]").val().match(numericExpression) && $("input[name=highTime]").val() != "")
            {
                errorList += "High Airframe Time must be a number.\n";
                errorFound = 1;
            }
            else if($("input[name=highTime]").val() != ""){
                var a = $("input[name=highTime]").val();
                var n = Number(a);
                if(n > 1000000)
                    $("input[name=highTime]").val(1000000);
            }

            if (!$("input[name=lowPax]").val().match(numericExpression) && $("input[name=lowPax]").val() != "")
            {
                errorList += "Low Passenger Capacity must be a number.\n";
                errorFound = 1;
            }

            if (!$("input[name=highPax]").val().match(numericExpression) && $("input[name=highPax]").val() != "")
            {
                errorList += "High Passenger Capacity must be a number.\n";
                errorFound = 1;
            }
            else if($("input[name=highPax]").val() != ""){
                var a = $("input[name=highPax]").val();
                var n = Number(a);
                if(n > 1000)
                    $("input[name=highPax]").val(1000);
            }

            if (!$("input[name=lowLoad]").val().match(numericExpression) && $("input[name=lowLoad]").val() != "")
            {
                errorList += "Low Useful Load must be a number.\n";
                errorFound = 1;
            }

            if (!$("input[name=highLoad]").val().match(numericExpression) && $("input[name=highLoad]").val() != "")
            {
                errorList += "High Useful Load must be a number.\n";
                errorFound = 1;
            }
            else if($("input[name=highLoad]").val() != ""){
                var a = $("input[name=highLoad]").val();
                var n = Number(a);
                if(n > 1000000)
                    $("input[name=highLoad]").val(1000000);
            }

            if ($("input[name=from]").val().length > 4)
            {
                errorList += "Check your ICAO entry for in the search parameter Aircraft That Are Within XX NM from....\n";
                errorFound = 1;
            }

            if (errorFound == 1)
            {
                window.alert(errorList);
                return true;
            }
            else
            {
                return false;
            }
        }

        function clearSearch()
        {
            $("input[name=lowPrice]").val("");
            $("input[name=highPrice]").val("");
            $("input[name=lowTime]").val("");
            $("input[name=highTime]").val("");
            $("input[name=lowPax]").val("");
            $("input[name=highPax]").val("");
            $("input[name=lowLoad]").val("");
            $("input[name=highLoad]").val("");
            $("input[name=from]").val("");
            $("select[name=model]").val(0);
            $("select[name=distance]").val(0);

            $("#aircraftTable").html("");
        }

        $(function() {

            $.extend($.tablesorter.defaults, {
                widthFixed: false,
                widgets : ['zebra','columns']
            });

            $("input[name=searchBy]") // select the radio by its id
                .change(function(){ // bind a function to the change event
                    if( $(this).is(":checked") ){ // check if the radio is checked
                        var val = $(this).val(); // retrieve the value
                        if(val==1){
                            $("#divByModel").css("display", "block");
                            $("#divPax").css("display", "none");
                            $("#divLoad").css("display", "none");
                        }
                        else {
                            $("#divByModel").css("display", "none");
                            $("#divPax").css("display", "table-row");
                            $("#divLoad").css("display", "table-row");
                        }
                    }
                });

            if(paramModelid != 0)
            {
                $("select[name=model]").val(paramModelid);

                $('input:radio[name=searchBy]')[1].checked = true;
                $("#divByModel").css("display", "block");
                $("#divPax").css("display", "none");
                $("#divLoad").css("display", "none");

                loadSearch();
            }
        });

    </script>
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
    <div class="content">
<%
    String error = null;
    if (error != null)
    {
%>
        <div class="error"><%= error %></div>
<%
    }
%>
    </div>
    <div style="margin-bottom: 10px;width: 580px;">
        <button class="btn btn-info form-control" onclick="loadPrivateSales()"> View Private Sale Offers</button><br>
        note: Create a private sale on the aircraft edit page.
    </div>
    <div class="form" style="width: 580px">
        <div>
            <div>
                <h4>Search by:</h4>
                <div style="margin-left: 25px; margin-bottom: 10px;">
                <input type="radio" name="searchBy" value="0" checked> All Aircraft&nbsp&nbsp&nbsp&nbsp&nbsp
                <input type="radio" name="searchBy" value="1"> By Model
                </div>
                <div id="divByModel" style="display: none;">
                    Select Model
                    <select id="model" name="model" class="formselect form-control">
                        <option class="formselect" value=""></option>
<%
    List<ModelBean> models = Models.getAllModels();
    for (ModelBean model : models)
    {
%>
                        <option class="formselect" value="<%= model.getId() %>"><%= model.getMakeModel() %></option>
<%
    }
%>
                    </select>
                </div>
                <table>
                    <tr><td colspan="4">Leaving filter blank selects minimum or maximum value available.</td></tr>
                    <tr><td></td><td>Min</td><td></td><td>Max</td></tr>
                    <tr>
                        <td>By price range</td>
                        <td><input name="lowPrice" type="text" class="form-control" size ="10" value=""></td>
                        <td style="text-align: center;">to</td>
                        <td><input name="highPrice" type="text" class="form-control" size="10" value=""></td>
                    </tr>
                    <tr>
                        <td>By airframe time (hrs)</td>
                        <td><input name="lowTime" type="text" class="form-control" size="5" value=""></td>
                        <td style="text-align: center;">to</td>
                        <td><input name="highTime" type="text" class="form-control" size="5" value=""></td>
                    </tr>
                    <tr id="divPax">
                        <td>By passenger capacity</td>
                        <td><input name="lowPax" type="text" class="form-control" size="4" value=""></td>
                        <td style="text-align: center;">to</td>
                        <td><input name="highPax" type="text" class="form-control" size="4" value=""></td>
                    </tr>
                    <tr id="divLoad">
                        <td>By useful load (Kg)</td>
                        <td><input name="lowLoad" type="text" class="form-control" size="6" value=""></td>
                        <td style="text-align: center;">to</td>
                        <td><input name="highLoad" type="text" class="form-control" size="6" value=""></td>
                    </tr>
                    <tr>
                        <td>Aircraft that are within</td>
                        <td>
                            <select name="distance" class="form-control">
                                <option class="formselect" value=""></option>
                                <option class="formselect" value="10" >10</option>
                                <option class="formselect" value="20" >20</option>
                                <option class="formselect" value="50" >50</option>
                                <option class="formselect" value="100" >100</option>
                                <option class="formselect" value="250" >250</option>
                                <option class="formselect" value="500" >500</option>
                                <option class="formselect" value="1000" >1000</option>
                                <option class="formselect" value="2000" >2000</option>
                            </select>
                        </td>
                        <td style="padding: 3px;text-align: center;">NM from</td>
                        <td><input name="from" type="text" class="form-control" value="" size="4" ></td>
                    </tr>
                </table>
                <button class="btn btn-default form-control" style="margin-top: 10px;" onclick="loadSearch()">Search</button>
                <button class="btn btn-default form-control" style="margin-top: 10px;" onclick="clearSearch()">Reset</button>
            </div>
        </div>
    </div>
</div>
<div id="aircraftTable"></div>

<!-- Modal HTML -->
<div id="myModal" class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Aircraft Information and Purchase</h4>
            </div>
            <div class="modal-body">
                <form id="formAircraftModal" method="post" action="userctl" class="ui-front">
                    <input type="hidden" name="event" value="Market"/>
                    <input type="hidden" name="account" value=""/>
                    <input type="hidden" name="id" value=""/>
                    <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                    <div id="aircraftData">
                    </div>
                    <div>
                        Purchase for:
                        <select id="groupSelect">
                            <option value=""></option>
                            <option value="<%=user.getId()%>"><%= user.getName()%></option>
                            <%
                                List<UserBean> groups = Accounts.getGroupsForUser(user.getId());

                                for (UserBean group : groups)
                                {
                                    if (user.groupMemberLevel(group.getId()) >= UserBean.GROUP_STAFF)
                                    {
                            %>
                            <option value="<%=group.getId()%>"><%= group.getName()%></option>
                            <%
                                    }
                                }
                            %>
                        </select>

                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary" onclick="purchaseAircraft();">Purchase</button>
            </div>
        </div>
    </div>
</div>


</body>
</html>
