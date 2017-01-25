<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*"
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

    String error = null;

    //setup return page if action used
    String returnPage = request.getParameter("returnpage");

    String sservice = request.getParameter("service");
    String saccount = request.getParameter("account");
    int serviceId = Integer.parseInt(sservice);
    int accountId = Integer.parseInt(saccount);

    UserBean account;

    //check permissions
    if(accountId != user.getId())
        account = Accounts.getAccountById(accountId);
    else
        account = user;

    if (account.isGroup() && user.groupMemberLevel(accountId) != UserBean.GROUP_OWNER)
    {
        // We are not the owner of the group kick out to main menu.
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    //get current service permissions
    ServiceAccessBean bean = ServiceProviders.getServiceProviderAccess(serviceId, accountId);
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap-theme.min.css">
    <link href="css/bootstrap-switch.min.css" rel="stylesheet" />
    <link href="css/Master.css" rel="stylesheet" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/js/bootstrap.min.js"></script>
    <script src="scripts/bootstrap-switch.min.js"></script>

    <script>
        $(document).ready(function() {
            $("[name='cash-read']").bootstrapSwitch();
            $("[name='cash-transfer']").bootstrapSwitch();
            $("[name='bank-read']").bootstrapSwitch();
            $("[name='bank-deposit']").bootstrapSwitch();
            $("[name='bank-withdraw']").bootstrapSwitch();
            $("[name='aircraft-purchase']").bootstrapSwitch();
            $("[name='aircraft-transfer']").bootstrapSwitch();
            $("[name='aircraft-lease']").bootstrapSwitch();
            $("[name='aircraft-edit']").bootstrapSwitch();
            $("[name='aircraft-sale']").bootstrapSwitch();
        });
    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
    <div class="content">
<%
    if (error != null)
    {
%>		<div class="error"><%= error %></div>
<%
    }
%>
        <div class="form" style="width: 700px">
            <form id="editAccess" name="editAccess" method="post" action="userctl">
                <div>
                    <input type="hidden" name="event" value="updateServiceProviderAccess">
                    <input type="hidden" name="service" value="<%=bean.getServiceid()%>">
                    <input type="hidden" name="account" value="<%=bean.getAccountid()%>">
                    <input type="hidden" name="returnpage" value="<%=returnPage%>">
                </div>
                <table class="table">
                    <caption><%=bean.getServicename()%> - Service Access Permissions</caption>
                    <thead>
                    <tr>
                        <th colspan="3"  style="text-align: center;background-color: lightgray">Permissions</th>
                    </tr>
                    <tr style="background-color: darkgrey">
                        <th style="text-align: center">Cash*</th>
                        <th style="text-align: center">Bank</th>
                        <th style="text-align: center">Aircraft</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td style="text-align: right;">
                            READ <input id="cash-read" name="cash-read" type="checkbox" <%=bean.getCashaccess().contains("READ") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                            TRANSFER <input id="cash-transfer" name="cash-transfer" type="checkbox" <%=bean.getCashaccess().contains("TRANSFER") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                        </td>
                        <td style="text-align: right;">
                            READ <input id="bank-read" name="bank-read" type="checkbox" <%=bean.getBankaccess().contains("READ") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                            DEPOSIT <input id="bank-deposit" name="bank-deposit" type="checkbox" <%=bean.getBankaccess().contains("DEPOSIT") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                            WITHDRAW <input id="bank-withdraw" name="bank-withdraw" type="checkbox" <%=bean.getBankaccess().contains("WITHDRAW") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                        </td>
                        <td style="text-align: right;">
                            PURCHASE <input id="aircraft-purchase" name="aircraft-purchase" type="checkbox" <%=bean.getAircraftaccess().contains("PURCHASE") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                            TRANSFER <input id="aircraft-transfer" name="aircraft-transfer" type="checkbox" <%=bean.getAircraftaccess().contains("TRANSFER") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                            LEASE <input id="aircraft-lease" name="aircraft-lease" type="checkbox" <%=bean.getAircraftaccess().contains("LEASE") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                            EDIT <input id="aircraft-edit" name="aircraft-edit" type="checkbox" <%=bean.getAircraftaccess().contains("EDIT") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                            SALE <input id="aircraft-sale" name="aircraft-sale" type="checkbox" <%=bean.getAircraftaccess().contains("SALE") ? "checked" : ""%> data-size="mini" data-inverse="true" data-off-text="Deny" data-on-text="Allow" data-on-color="success" data-off-color="danger"><br>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <input type="submit" value="Update"><br>
                * Services require NO special access to send you money. They only require you to allow access to transfer money out of your cash account.
            </form>
        </div>
    </div>
</div>
</body>
</html>
