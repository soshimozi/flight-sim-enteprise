<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.Formatters, java.util.List, net.fseconomy.dto.*, net.fseconomy.beans.UserBean"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    String sId = request.getParameter("userid");

    //setup return page if action used
    String groupParam = sId != null ? "?userid="+sId : "";
    String returnPage = request.getRequestURI() + groupParam;
    response.addHeader("referer", returnPage);

    List<AccountNote> noteList = null;
    List<LinkedAccount> linkedList = null;
    List<TrendHours> trendList = null;
    List<String> ipList = null;
    UserBean account = null;
    int accountId = -1;
    String flights = "Not avail.";
    boolean accountLocked = false;

    if(sId != null)
    {
        accountId = Integer.parseInt(sId);
        account = Accounts.getAccountById(accountId);

        accountLocked = account.isLocked();
        noteList = Accounts.getAccountNoteList(accountId);
        linkedList = Accounts.getLinkedAccountList(accountId);
        trendList = Data.getTrendHoursQuery(account.getId(), 10);
        ipList = SimClientRequests.getClientRequestCountsByAccountId(accountId);

        if (Stats.statsmap != null && Stats.statsmap.containsKey(account.getName()))
            flights = Integer.toString(Stats.statsmap.get(account.getName()).flights);
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="../css/Master.css" rel="stylesheet" type="text/css" />
    <link href="../css/bootstrap.min.css" rel="stylesheet">

    <script type="text/javascript" src="../scripts/jquery.min.js"></script>
    <script type="text/javascript" src="../scripts/bootstrap.min.js"></script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
    <div class="content">
        <div class="container">

        <div class="row clearfix">
            <div class="col-md-12 column">
                <h3>Admin User Manager</h3>
            </div>
        </div>

        <div class="row clearfix" style="margin: 10px">
            <div class="col-md-4 column">
                <div class="datatable" style="border: 1px solid">
                    <b>User Information</b> <a href="accountedit.jsp?userid=<%= accountId %>">Edit Account</a>
                    <table>
                        <tr>
                            <td>User: </td>
                            <td><%=account.getName()%> </td>
                        </tr>
                        <tr>
                            <td>Email: </td>
                            <td><%=account.getEmail()%></td>
                        </tr>
                        <tr>
                            <td>Status: </td>
                            <td>
                                <form method="post" action="/userctl">
                                    <div>
                                        <input type="hidden" name="event" value="<%=accountLocked ? "unlockAccount" : "lockAccount"%>"/>
                                        <input type="hidden" name="userid" value="<%= accountId %>"/>
                                        <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                                    </div>
                                    <%=accountLocked ? "Locked" : "Unlocked"%> <input type="submit" class="button" value="<%=accountLocked ? "Unlock" : "Lock"%> Account" />
                                </form>
                            </td>
                        </tr>
                        <tr>
                            <td>Exposure: </td>
                            <td><%=account.getExposure() == 0 ? "Hidden" : "Visible"%></td>
                        </tr>
<%
    if(Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
                        <tr>
                            <td>Level: </td>
                            <td><%=account.getLevelString(account.getLevel())%></td>
                        </tr>
<%
    }
%>
                    </table>
                </div>
            </div>
            <div class="col-md-4 column">
                <div class="datatable" style="border: 1px solid">
                    <b>User Stats</b>
                    <table>
                        <tr>
                            <td>Created: </td>
                            <td><%=Formatters.datemmddyy.format(account.getCreated())%></td>
                        </tr>
                        <tr>
                            <td>Last Logon: </td>
                            <td><%=account.getLogon() != null ? Formatters.datemmddyy.format(account.getLogon()) : "never"%></td>
                        </tr>
                        <tr>
                            <td>Flights: </td>
                            <td><%= flights %></td>
                        </tr>
                        <tr>
                            <td>Cash: </td>
                            <td><%= Formatters.currency.format(account.getMoney()) %></td>
                        </tr>
                        <tr>
                            <td>Bank: </td>
                            <td><%= Formatters.currency.format(account.getBank()) %></td>
                        </tr>
                    </table>
                </div>
            </div>
            <div class="col-md-4 column">
            </div>
        </div>

        <div class="row clearfix" style="margin: 10px">
            <div class="col-md-12 column">
                <div class="row clearfix">
                    <div class="col-md-4 column">
                        <div class="datatable" style="border: 1px solid">
                            <b>IP Addresses</b>
                            <table>
                                <thead>
                                <tr>
                                    <th>IP</th>
                                    <th style="padding-left: 20px">Hits</th>
                                </tr>
                                </thead>
                                <tbody>
                                <%
                                    for (String item: ipList)
                                    {
                                        String[] s = item.split("\\|");
                                %>
                                <tr>
                                    <td>
                                        <%= s[0] %>
                                    </td>
                                    <td style="padding-left: 20px">
                                        <a href="/admin/checkclientiplisting.jsp?searchby=account&searchfor=<%= account.getName() %>"><%= s[1] %></a>
                                    </td>
                                </tr>
                                <%
                                    }
                                %>
                                </tbody>
                            </table>
                        </div>

                    </div>
                    <div class="col-md-4 column">
                        <div class="datatable" style="border: 1px solid">
                            <b>Account Links</b>
<%
    if(linkedList == null || linkedList.size() == 0)
    {
%>
                            <a href="accountlink.jsp?id=<%= accountId %>">Link Account</a><br>
                            <ul>
                                <li>No account links.</li>
                            </ul>

<%
    }
    else
    {
%>
                            <b>Linked Accounts (LinkId: <%= linkedList.get(0).linkId%>)</b>
                            <ul>
<%
      for (LinkedAccount item : linkedList)
        {
%>
                                <li><%=item.accountName%></li>
<%
        }
%>
                            </ul>
                            <form method="post" action="/userctl">
                                <div>
                                    <input type="hidden" name="event" value="unlinkAccount"/>
                                    <input type="hidden" name="userid" value="<%= accountId %>"/>
                                    <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                                </div>
                                <input type="submit" class="button" value="Unlink Account" />
                            </form>
<%
    }
%>
                        </div>

                    </div>
                    <div class="col-md-4 column">
                    </div>
                </div>
            </div>
        </div>
        <div class="row clearfix" style="margin: 10px">
            <div class="col-md-12 column">
                <div style="margin-bottom: 10px">
                    <b>Rental Ban List:</b> <span><%=account.getBanList() == null ? "none" : account.getBanList()%></span>
                </div>
                <div class="dataTable" style="border: 1px solid">
                    <b>Recent 48 Hour Trend</b><br/>
<%
    if(trendList == null || trendList.size() == 0)
    {
%>
        <b>No Flights Recorded</b>
<%
    }
    else
    {
%>
                    <table id="sortableTableStats" class="sortable" cellpadding="5">
                        <thead>
                        <tr>
                            <th>Date</th>
                            <th>Duration</th>
                            <th>Last 48 Hours</th>
                            <th>**Over 30 Hours</th>
                        </tr>
                        </thead>
                        <tbody>
<%
        for (TrendHours item: trendList)
        {
%>
                        <tr>
                            <td><%= item.logdate %></td>
                            <td><%= item.duration %></td>
                            <td><%= ((item.last48Hours > 20.0) ? "<HTML><font color=Red><b>" : "") + item.last48Hours + ((item.last48Hours > 20.0) ? "</font></HTML></b>" : "") %></td>
                            <td><%= ((item.last48Hours > 30.0) ? "<b>**</b>" : "") %></td>
                        </tr>
<%
        }
%>
                        </tbody>
                    </table>
                    <a href="checkuser48hourtrend.jsp?id=<%= accountId %>">View more</a>
<%
    }
%>
                </div>
            </div>
        </div>
        <div class="row clearfix">
            <div class="col-md-12 column">
                <div class="datatable" style="margin-bottom: 10px">
                    <form method="post" action="/userctl">
                        <div>
                            <input type="hidden" name="event" value="addAccountNote"/>
                            <input type="hidden" name="userid" value="<%= accountId %>"/>
                            <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                        </div>
                        <input type="submit" class="button" value="Add Note">
                        <input name="note" type="text" class="textarea" value="" maxlength="255" size="60" />
                    </form>

<%
    if(noteList == null || noteList.size() == 0)
    {
%>
        <b>No notes currently available.</b>
<%
    }
    else
    {
%>
                    <table id="sortableTableStats" class="sortable" cellpadding="5"  style="border: 1px solid; min-width: 500px">
                        <thead>
                        <tr>
                            <th>Created by</th>
                            <th>Date</th>
                            <th>Note</th>
                        </tr>
                        </thead>

                        <tbody>
<%
        for (AccountNote item : noteList)
        {
%>
                        <tr>
                            <td>
                                <%= item.createdByName %>
                            </td>
                            <td>
                                <%= Formatters.dateyyyymmddhhmmss.format(item.created) %>
                            </td>
                            <td>
                                <%= item.note %>
                            </td>
                        </tr>
<%
        }
%>
                        </tbody>
                    </table>
                </div>
<%
    }
%>

            </div>
        </div>
        </div>
    </div>
</div>
</body>
</html>
