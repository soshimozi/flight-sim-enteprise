<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, java.util.Calendar, net.fseconomy.data.*, net.fseconomy.util.*"
        %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    //setup return page if action used
    String returnPage = request.getRequestURI();
    response.addHeader("referer", request.getRequestURI());

    //String error = (String) request.getSession().getAttribute("message");

    List<UserBean> groups;

    //if(error == null)
    groups = Accounts.getGroupsForUser(user.getId());

    Calendar cal = Calendar.getInstance();
    int month = cal.get(Calendar.MONTH)+1;
    int year = cal.get(Calendar.YEAR);
%>

<!DOCTYPE html>
<html>
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />
    <link href="css/bootstrap-theme.min.css" rel="stylesheet" type="text/css" />
    <link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css" />
    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="scripts/jquery.min.js"></script>
    <script src="scripts/jquery-ui.min.js"></script>
    <script src="scripts/jquery.cookie.js"></script>
    <script src="scripts/bootstrap.min.js"></script>
    <script src="scripts/AutoComplete.js"></script>

    <script type="text/javaScript">

        var displayGroupsOnly = false;

        function doSubmit(sel)
        {
            sel.form.selectedBalance.value = sel.options[sel.selectedIndex].value;
            sel.form.submit();
        }

        $(function()
        {
            initAutoComplete("#accountname", "#account", <%= Accounts.ACCT_TYPE_ALL %>);

            displayGroupsOnly = $.cookie('displayGroupsOnly') === 'true';
            setGroupsOnly(displayGroupsOnly);

            $("#groupSelect").change(function() {
                $( "#groupSelect option:selected" ).each(function() {
                    $("#accountname").val($(this).text());
                    $("#account").val($(this).val());
                });
            });

        });

        function doDeposit(id, name)
        {
            var form = document.getElementById("formBankModal");
            form.accountid.value = id;
            //event
            form.event.value = "bankDeposit";
            //bankActionTitle
            $("#bankActionTitle").text(name + " - Deposit into Bank");
            //bankAction
            $("#bankAction").text("Deposit");
            //buttonBankAction
            $("#buttonBankAction").text("Deposit");

            $("#myModal").modal('show');
        }

        function doWithdrawal(id, name)
        {
            var form = document.getElementById("formBankModal");
            form.accountid.value = id;
            //event
            form.event.value = "bankWithdrawal";
            //bankActionTitle
            $("#bankActionTitle").text(name + " - Withdraw from Bank");
            //bankAction
            $("#bankAction").text("Withdraw");
            //buttonBankAction
            $("#buttonBankAction").text("Withdraw");

            $("#myModal").modal('show');
        }

        function doTransfer()
        {
            var form = document.getElementById("formBankModal");
            form.submit();
        }

        function setGroupsOnly(flag)
        {
            var ac = document.getElementById('byAutoComplete');
            var bg = document.getElementById('byGroup');
            var bgcb = document.getElementById('groupsOnly');

            if(flag){
                bg.style.display = 'block';
                ac.style.display = 'none';
                bgcb.checked = true;
            }
            else
            {
                ac.style.display = 'block';
                bg.style.display = 'none';
                bgcb.checked = false;
            }
        }

        function doGroupsOnly()
        {
            var ac = document.getElementById('byAutoComplete');
            var bg = document.getElementById('byGroup');
            if(ac.style.display == 'none'){
                ac.style.display = 'block';
                bg.style.display = 'none';
                $.cookie('displayGroupsOnly', false);
            }
            else
            {
                bg.style.display = 'block';
                ac.style.display = 'none';
                $.cookie('displayGroupsOnly', true);
            }
        }

    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<%
    String message = Helpers.getSessionMessage(request);
    if (message != null)
    {
%>
<div class="error"><%= message %></div>
<%
    }
%>

<div class="container">
    <div class="row clearfix">
        <div class="col-sm-12 column">
            <div class="page-header">
                <h3>
                    Summary of Personal and Group Bank Accounts<br>
                    <small>Group financial information is limited to group staff and group owners.</small>
                </h3>
            </div>
        </div>
    </div>
    <div class="row clearfix" style="border-bottom: 1px solid darkgray; padding-bottom: 3px">
        <div class="col-sm-4 col-md-4 column">
            <span class="label label-primary">Account</span>
        </div>
        <div class="col-sm-2 col-md-2 column vcenter text-right">
            <span class="label label-primary">Cash</span><span title="Click balance to deposit cash into Bank"> <img height="16" width="16" src="img/helpicon.png"></span>
        </div>
        <div class="col-sm-2 col-md-2 column vcenter text-right">
            <span class="label label-primary">Bank</span><span title="Click balance to withdraw cash out of Bank"> <img height="16" width="16" src="img/helpicon.png"></span>
        </div>
        <div class="col-sm-4 col-md-4 column">
            <span class="label label-primary">Action</span>
        </div>
    </div>

    <div class="row clearfix"  style="border-bottom: 1px solid darkgray; padding-bottom: 3px">
        <div class="col-sm-4 col-md-4 column">
            <jsp:getProperty name="user" property="name"/><br>
        </div>
        <div class="col-sm-2 col-md-2 column text-right">
            <button class="btn btn-default btn-text-right" style="width: 120px" onclick="doDeposit(<%=user.getId()%>, '<%=user.getName()%>');">
                <%= Formatters.currency.format(user.getMoney()) %>
            </button>
        </div>
        <div class="col-sm-2 col-md-2 column text-right" >
            <div class="btn btn-default btn-text-right" style="width: 120px" onclick="doWithdrawal(<%=user.getId()%>, '<%=user.getName()%>');">
                <%= Formatters.currency.format(user.getBank()) %>
            </div>
        </div>
        <div class="col-sm-4 col-md-4 column">
            <div style="padding-bottom: 3px">
                <a class="link" href="paymentlog.jsp">Payment Log</a> |
                <a class="link" href="paymentlog.jsp?month=<%= month %>&year=<%= year %>">Monthly Statement</a>
            </div>
            <div style="padding-bottom: 3px">
                <form method="post" action="userctl">
                    <div>
                        <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                    </div>
                    <div>
                        <input type="hidden" name="event" value="bankBalance"/>
                        <input type="hidden" name="uid" value="<%=user.getId()%>"/>
                        <input type="hidden" name="selectedBalance" value=""/>
                        <select name="Actionbox" class="formselect" onchange="doSubmit(this)" >
                            <option value="-1">Quick Transfers - Select action...</option>
                            <option value="0">Set cash to $0.00</option>
                            <option value="1">Set cash to $1,000</option>
                            <option value="5">Set cash to $5,000</option>
                            <option value="10">Set cash to $10,000</option>
                            <option value="20">Set cash to $20,000</option>
                            <option value="30">Set cash to $30,000</option>
                            <option value="40">Set cash to $40,000</option>
                            <option value="50">Set cash to $50,000</option>
                            <option value="60">Set cash to $60,000</option>
                            <option value="70">Set cash to $70,000</option>
                            <option value="80">Set cash to $80,000</option>
                            <option value="100">Set cash to $100,000</option>
                            <option value="999">Transfer all in bank to cash.</option>
                        </select>
                    </div>
                </form>
            </div>
        </div>
    </div>

<%
    //Add group accounts

    for (UserBean group : groups)
    {
        int id = group.getId();
        String name = group.getName();
        String url = group.getUrl();
        String grpmoney = Formatters.currency.format(group.getMoney());
        String grpbank = Formatters.currency.format(group.getBank());

        if (url != null)
            url = "<a href=\"" + url + "\" target=\"_blank\">" + name + "</a>";
        else
            url = name;

        int memberLevel = user.groupMemberLevel(id);
        if (memberLevel >= UserBean.GROUP_STAFF )
        {
%>
    <div class="row clearfix"  style="border-bottom: 1px solid darkgray; padding-bottom: 3px">
        <div class="col-sm-4 col-md-4 column">
            <%=url%>
        </div>
        <div class="col-sm-2 col-md-2 column text-right">
            <div class="btn btn-default btn-text-right" style="width: 120px" onclick="doDeposit(<%=id%>, '<%=name%>');">
                <%= grpmoney %>
            </div>
        </div>
        <div class="col-sm-2 col-md-2 column text-right">
            <div class="btn btn-default btn-text-right" style="width: 120px" onclick="doWithdrawal(<%=id%>, '<%=name%>');">
                <%= grpbank %>
            </div>
        </div>
        <div class="col-sm-4 col-md-4 column">
            <div style="padding-bottom: 3px">
                <a class="link" href="paymentlog.jsp?groupid=<%= id %>">Payment Log</a> |
                <a class="link" href="paymentlog.jsp?groupid=<%= id %>&month=<%= month %>&year=<%= year %>">Monthly Statement</a>
            </div>
            <div style="padding-bottom: 3px">
                <form method="post" action="userctl">
                    <div>
                        <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                    </div>
                    <div>
                        <input type="hidden" name="event" value="bankBalance"/>
                        <input type="hidden" name="uid" value="<%=id%>"/>
                        <input type="hidden" name="selectedBalance" value=""/>
                        <select name="Actionbox" class="formselect" onchange="doSubmit(this)" >
                            <option value="-1">Quick Transfers - Select action...</option>
                            <option value="0">Set cash to $0.00</option>
                            <option value="1">Set cash to $1,000</option>
                            <option value="5">Set cash to $5,000</option>
                            <option value="10">Set cash to $10,000</option>
                            <option value="20">Set cash to $20,000</option>
                            <option value="30">Set cash to $30,000</option>
                            <option value="40">Set cash to $40,000</option>
                            <option value="50">Set cash to $50,000</option>
                            <option value="60">Set cash to $60,000</option>
                            <option value="70">Set cash to $70,000</option>
                            <option value="80">Set cash to $80,000</option>
                            <option value="100">Set cash to $100,000</option>
                            <option value="999">Transfer all in bank to cash.</option>
                        </select>
                    </div>
                </form>
            </div>
        </div>
    </div>
<%
        }
    }
%>
    <div class="row clearfix"  style="padding: 5px">
        <div class="col-sm-12 column">
            <small>Personal account earned interest to date: <%= Formatters.currency.format(user.getEarnedInterest()) %></small>
        </div>
    </div>
</div>

<div class="container">
    <div class="row clearfix">
        <div class="col-sm-12 column">
            <div class="page-header">
                <h3>
                    Cash Transfers<br>
                    <small>Transfers are only allowed <span style="text-decoration: underline">from</span> accounts you own or have staff rights to.</small>
                </h3>
            </div>
        </div>
    </div>

    <form method="post" action="userctl">
        <div>
            <input type="hidden" name="event" value="bankTransfer" />
            <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
        </div>

        <div class="row clearfix"  style="padding-bottom: 3px">
            <div class="col-sm-3 column bs3-form-label">
            </div>
            <div class="col-sm-3 column">
                <label><input type="checkbox" id="groupsOnly" onclick="doGroupsOnly();"> Limit to my groups only</label>
            </div>
        </div>
        <div class="row clearfix"  style="padding-bottom: 3px">
            <div class="col-sm-3 column bs3-form-label">
                From Account:
            </div>
            <div class="col-sm-3 column">
                <select name="id">
                    <option value=""></option>
                    <option value="<%=user.getId()%>"><%= user.getName()%></option>
<%
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
        </div>
        <div class="row clearfix"  style="padding-bottom: 3px">
            <div class="col-sm-3 column bs3-form-label">
                To Account:
            </div>
            <div class="col-sm-3 column">
                <div id="byAutoComplete">
                    <input type="text" id="accountname" name="accountname"  size="50"/>
                    <input type="hidden" id="account" name="account" value=""/>
                </div>
                <div id="byGroup" style="display: none">
                    <select id="groupSelect">
                        <option value=""></option>
                        <option value="<%=user.getId()%>"><%= user.getName()%></option>
<%
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
            </div>
        </div>
        <div class="row clearfix"  style="padding-bottom: 3px">
            <div class="col-sm-3 column bs3-form-label">
                Amount:
            </div>
            <div class="col-sm-3 column">
                <input name="amount" type="text" size="10" />
            </div>
        </div>
        <div class="row clearfix"  style="padding-bottom: 3px">
            <div class="col-sm-3 column bs3-form-label">
                Comment:
            </div>
            <div class="col-sm-3 column">
                <input name="comment" type="text" size="50" />
            </div>
        </div>
        <div class="row clearfix"  style="padding-bottom: 3px">
            <div class="col-sm-3 column bs3-form-label">
                <input type="submit" class="btn btn-primary" value="Transfer" />
            </div>
        </div>
    </form>
</div>

</div>
<br>
<br>
<br>
<br>
<!-- Modal HTML -->
<div id="myModal" class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="bankActionTitle" class="modal-title">BANK ACTION TITLE HERE</h4>
            </div>
            <div class="modal-body">
                <form id="formBankModal" method="post" action="userctl" class="ui-front">
                    <input type="hidden" name="event" value="bankactionhere"/>
                    <input type="hidden" name="accountid" value=""/>
                    <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                    <div>
                        Enter Amount to <span id="bankAction">ACTION</span>:
                        <input type="text" id="amount" name="amount" value="" placeholder="0.00"/>
                        <br/>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary" onclick="doTransfer();"><span id="buttonBankAction">ACTION</span></button>
            </div>
        </div>
    </div>
</div>

</body>
</html>