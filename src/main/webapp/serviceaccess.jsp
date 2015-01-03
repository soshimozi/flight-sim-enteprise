<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.beans.*, net.fseconomy.data.*"
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
    String returnPage = java.net.URLDecoder.decode(request.getParameter("returnpage"),"UTF-8").trim();

    String group = request.getParameter("groupid");
    UserBean account = user;
    if (group != null)
        account = Accounts.getAccountById(Integer.parseInt(group));

    boolean display = false;
    if ((account.isGroup() && user.groupMemberLevel(Integer.parseInt(group)) == UserBean.GROUP_OWNER)
     || (!account.isGroup() && (account.getId()== user.getId())))
    {
        display = true;
    }

    if(display)
    {
        //get services
        StringBuilder optionServiceProviders = new StringBuilder();
        optionServiceProviders.append("<option value=\"0\">Make Selection</option>/n");
        List<ServiceProviderBean> services =  ServiceProviders.getAccountAvailableServiceProviders(account.getId());
        for(ServiceProviderBean spb: services)
        {
            if(spb.getStatus() == ServiceProviderBean.STATUS_ACTIVE)
                optionServiceProviders.append("<option value=\"")
                    .append(spb.getId())
                    .append("\">")
                    .append(spb.getName())
                    .append("</option>")
                    .append("/n");
        }

        //get current service permissions
        StringBuilder trCurrentServiceProviders = new StringBuilder();
        ServiceAccessBean[] serviceAccess = ServiceProviders.getCurrentServiceProviderAccess(account.getId());
        if(serviceAccess.length != 0)
        for(ServiceAccessBean bean: serviceAccess)
        {
            String cash = bean.getCashaccess().isEmpty() ? "NONE" : bean.getCashaccess();
            String bank = bean.getBankaccess().isEmpty() ? "NONE" : bean.getBankaccess();
            String aircraft = bean.getAircraftaccess().isEmpty() ? "NONE" : bean.getAircraftaccess();

            trCurrentServiceProviders.append("<tr>")
                    .append("<td><span onclick=\"doEditServiceAccess(").append(bean.getServiceid()).append(")\">").append(bean.getServicename()).append("</span></td>")
                    .append("<td>").append(cash).append("</td>")
                    .append("<td>").append(bank).append("</td>")
                    .append("<td>").append(aircraft).append("</td>")
                    .append("<td><span onclick=\"doDeleteServiceAccess(").append(bean.getServiceid()).append(")\" ><img src=\"img/delete16x16.png\"></span></td>")
                    .append("</tr>");

        }
%>
<script>

    function doEditServiceAccess(serviceid)
    {
        var form = document.getElementById("editAccess");
        form.service.value = serviceid;
        form.submit();
    }

    function doDeleteServiceAccess(serviceid)
    {
        var form = document.getElementById("editAccess");
        form.service.value = serviceid;
        form.action = "userctl";
        form.event.value = "deleteServiceProviderAccess";
        form.submit();
    }

</script>

    <div class="form">
        <label>Please select the service to add:</label>
        <form id="AddService" name="AddService" method="post" action="userctl">
            <div>
                <input type="hidden" name="event" value="addServiceProviderAccess">
                <input type="hidden" name="id" value="<%=account.getId()%>">
                <input type="hidden" name="account" value="<%=account.getId()%>">
                <input type="hidden" name="returnpage" value="<%=returnPage%>">
                <select name="services">
                    <%= optionServiceProviders.toString() %>
                </select>
                <input type="submit" value="Add Service">
            </div>
        </form>
        <form id="editAccess" name="editAccess" method="post" action="editserviceaccess.jsp">
            <div>
                <input type="hidden" name="service" value="">
                <input type="hidden" name="event" value="deleteServiceProviderAccess">
                <input type="hidden" name="account" value="<%=account.getId()%>">
                <input type="hidden" name="returnpage" value="<%=returnPage%>">
            </div>
            <table class="table table-hover">
                <caption>Service Access Permissions</caption>
                <thead>
                <tr>
                    <th><span style="background-color: salmon">Click Service Name to change permissions.</span></th>
                    <th colspan="3"  style="text-align: center;background-color: lightgray">Permissions</th>
                </tr>
                <tr style="background-color: darkgrey">
                    <th>Service Name</th>
                    <th>Cash</th>
                    <th>Bank</th>
                    <th>Aircraft</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <%= trCurrentServiceProviders.toString() %>
                </tbody>
            </table>
        </form>
    </div>
<%
    }
%>