<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import = "net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    int parked = 0;
    int flying = 0;
    try
    {
        flying = Stats.getInstance().getNumberOfUsers("flying");
        parked = Stats.getInstance().getNumberOfUsers("parked");
    }
    catch(DataError e)
    {

    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel="shortcut icon" href="/favicon.ico" type="image/vnd.microsoft.icon"/>
	<link href="css/Master.css" rel="stylesheet" type="text/css" />

    <%--<script src="scripts/AnchorPosition.js"></script>--%>
    <script src="scripts/PopupWindow.js"></script>
    <script type="text/javascript">

        //Original javascript code by Daniel Insley at bravenet.com - modified by Paul Dahlen.
        function createtime()
        {
            var time = new Date();
            var gmtMS = time.getTime() + (time.getTimezoneOffset() * 60000);
            var gmtTime =  new Date(gmtMS);
            var zhours = gmtTime.getHours();
            var tfzhours = zhours;
            var lhours = time.getHours();
            var tflhours = lhours;
            var minutes = gmtTime.getMinutes();
            var seconds = gmtTime.getSeconds();
            var abbrev = "AM";
            var labbrev = "AM";

            if (tfzhours < 10)
                tfzhours = "0"+tfzhours;

            if (tflhours < 10)
                tflhours = "0"+tflhours;

            if (zhours>=12)
                abbrev="PM";

            if (zhours>12)
                zhours=zhours-12;

            if (zhours==0)
                zhours=12;

            if (lhours>=12)
                labbrev="PM";

            if (lhours>12)
                lhours=lhours-12;

            if (lhours==0)
                lhours=12;

            if (minutes<=9)
                minutes="0"+minutes;

            if (seconds<=9)
                seconds="0"+seconds;

            var ctime=" &nbsp; "+"UTC: "+zhours+":"+minutes+":"+seconds+" "+abbrev+"  ("+tfzhours+":"+minutes+":"+seconds+") &nbsp;";

            if(document.all)
            {
                document.all.clock.innerHTML=ctime;
            }
            else if (document.getElementById)
            {
                document.getElementById("clock").innerHTML=ctime;
            }
            else
            {
                document.write(tftime);
                document.write(ctime);
            }
        }

        if (!document.all && !document.getElementById)
            createtime();

        function loadtime()
        {
            if (document.all || document.getElementById)
                setInterval("createtime()",1000);
        }

        // Popup Windows
        var manual = new PopupWindow();
        var flying = new PopupWindow();
        var parked = new PopupWindow();
        var team = new PopupWindow();

        function onClickFlying()
        {
            var plus = <%= flying %> * 24;
            flying.setWindowProperties('toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable,alwaysRaised,dependent,titlebar=no');
            flying.setSize(170,50 + plus);
            flying.setUrl('<%= response.encodeURL("whoisflying.jsp")%>');
            flying.showPopup('flying');

            return false;
        }
        function onClickParked()
        {
            var plus = <%= parked %> * 24;
            parked.setWindowProperties('toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable,alwaysRaised,dependent,titlebar=no');
            parked.setSize(170,50 + plus);
            parked.setUrl('<%= response.encodeURL("whoisparked.jsp")%>');
            parked.showPopup('parked');

            return false;
        }

    </script>

</head>
<body onload="loadtime()">

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">

	<div class="content">
	
	<!-- SHORTCUTS BLOCK ======================================================= -->
	
	<div class="form" style="width: 300px; float: right">	
		<h3>Shortcuts</h3>
		<ul>
<%
	if (!user.isLoggedIn()) 
	{
%>
			<li><a href="http://www.fseconomy.net" onclick="this.target='_blank'">Sign Up For New Account</a></li>
<%	
	}
%>		
			<li>Read the <a onclick="this.target='_blank'" href="https://sites.google.com/site/fseoperationsguide/">Manual</a></li>
			<li>Go to the <a onclick="this.target='_blank'" href="http://www.fseconomy.net">FSE Community</a>.</li>
			<li><a href="<%= "/static/client/FSeconomy.zip"%>">Download</a> the FSUIPC Client</li>
			<li> <a href="/static/fsxclient/publish.htm" onclick="this.target='_blank'">Install</a> the SimConnect Client </li>
			<li>Join us on <a href="http://www.fseconomy.net/forum/teamspeak/41-teamspeak-instructions-read-this-first" onclick="this.target='_blank'">FSE TeamSpeak</a></li>
			<li style="margin-top: 10px;">Follow us on your favorite<br/> social media:<br/>
				<div style=" display: table; margin: 0 auto;">
				<a href="https://www.facebook.com/FSEconomy">
					<img style="border-style: none;" src="https://sites.google.com/site/fseoperationsguide/FSE-fb.png">
				</a>
				<a  href="https://twitter.com/FSEconomy">
					<img style="border-style: none;" src="https://sites.google.com/site/fseoperationsguide/fse-twitter.png">
				</a>
				<a  href="https://plus.google.com/+Fseconomy/">
					<img style="border-style: none;" src="https://sites.google.com/site/fseoperationsguide/FSE-gplus.png">
				</a>
				<a  href="https://www.youtube.com/fseconomy">
					<img style="border-style: none;" src="https://sites.google.com/site/fseoperationsguide/FSE-youtube.png" />
				</a>
				</div>
			</li>
		</ul>
		<div style="display: table; margin: 5px auto; border-style:inset; font-size: 10pt; font-family: Arial,sans-serif;" id="clock" ></div>
		<h3>Pilot and Activity Map</h3>
		<div style="display: table; margin: 0 auto;">
			<a href="http://server.fseconomy.net/static/html/index.html" target="_blank">
				<img style="border-style: none; box-shadow: 5px 5px 5px #888888;" src="/img/pilotactivitymap.png">
			</a>
		</div>

		<div style="display: table; margin: 0 auto;">
			<a style="display: table; margin: 0 auto;" href="http://vatsim.net" onclick="this.target='_blank'"><img style="border-style: none;" src="img/VATSIM_logo_small.gif" width="90" /></a><br/>
			<div style="font-size:x-small"><b>FSEconomy is a proud <a href="http://vatsim.net" onclick="this.target='_blank'">VATSIM</a> partner.</b></div>
		</div>
	</div>
	
	
	<!-- MAIN PAGE CONTENT ======================================================= -->
	
	<div class="news">
	
	
	<!-- NEWS ITEM Logged Out Only -->
<%
	if (!user.isLoggedIn()) 
	{ 
%>  
	<div>
	<h3>Welcome to the exciting world of <em>FSEconomy</em></h3>
	<p>FSEconomy is a environment where flight simulation enthusiasts can add a new aspect to their flying. Since 2005, FSEconomy has allowed over 4,000 registered simulator pilots to earn in-game virtual money by flying general aviation aircraft to and from nearly every airport on Earth. With those virtual earnings, pilots can purchase their own airplanes, join or start virtual businesses, open up FBOs, and more - all within the free world of FSEconomy. FSEconomy adds a new dimension to your flight simulation experience, and that doesn't even count all the people you will meet along the way.</p> 
	<p>If you would like to know more about this free community, just read the manual and visit our community forum. Links can be found on the right of this page.</p>
	</div>
<% 	} 
%>
	<!-- END NEW ITEM -->
	
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
	{
%>
	<div>
	<h3>Update - Building FBOs</h3>
	<p>
	There is a new requirement for building New FBO's.
	You must have at least 10kg of supplies on site to build, if you have negative supplies at that location you must bring in enough supplies to have positive supplies with a minimum of 10 Kg to build.
	</p>
	</div>
	<div>
	<h3>FSE Reporter - Issue Released!</h3>
	<p>
	<img src="https://sites.google.com/site/fsereporter/home/cover.png" style="width: 100px; float:left;margin:0 10px" /><br/>
	The FSE Reporter is FSE's official monthly publication. A wealth of content published by fellow FSE members, and about FSE members, is waiting for your perusal. Get paid to contribute! Advertise your flying group or business! 
	See the FSE Reporter website for details: <a href="https://sites.google.com/site/fsereporter/">https://sites.google.com/site/fsereporter/</a>
	</p>
	</div>
<% 	} 
%>
	<!-- END NEW ITEM -->
	
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
	{
%>
	<div>
	<h3>Player/Group Name Entry Fields</h3>
	<p>In an effort to both speed up page loading times and provide some new effiencies to our users, FSEconomy has adjusted how players interact with various entry forms. Developers have spent time modifying how you enter the name of who you wish to transfer funds, FBO's, airplanes, and goods to.</p>
	<p>Instead of providing a drop-down selection box with ALL users and groups listed, you are now presented with a text box where you will simply type the user or group name. After several characters are entered, you will be presented with the first ten autocompletion options to choose from - you can a) select one from the list, b) continue typing to refine the autocomplete options, or c) type all the way to completion. You can also copy/paste names into the field if you wish.</p>
	<p>Very important: you MUST click on a name in the list, even if you type the entire name or paste it in.</p>
	</div>
<% 	} 
%>
	<!-- END NEW ITEM -->
	
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
	{
%>
	<div>
	<h3>All-In Airliner Testing Continues...</h3>
	<p>FSEconomy has introduced the <b>Boeing 737-800</b> airframe to the testing process for All-In airline style jobs in FSEconomy. The 737-800 is a very popular airplane in the simulation world, owing this reputation to several world-class aircraft addons. Jobs are popping up all over the globe, so be certain to take advantage!</p>
	</div>
<% 	} 
%>
	<!-- END NEW ITEM --> 
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
	{
%>
	<div>
	<h3>Bulk Fuel Ordering Changes</h3>
	<p>In coordination with the recent allowance for all airports to receive bulk fuel, the FSEconomy coding staff has been hard at work refining this process. Effective immediately, several important changes have been made to the bulk fuel ordering process. Key highlights include limitations on who can buy wholesale fuel from the FSEconomy suppliers, bulk fuel delivery delays, and limits to the numbers of orders per day have all been implemented to further enhance the depth of FSEconomy. </p>
	</div>
<% 	} 
%>
	<!-- END NEW ITEM --> 
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
	{
%>
	<div>
	<h3>August Changes</h3>
	<p> The FSEconomy coders have been hard at work through the month of August, making several important changes to bring both balance and new features to the FSE world.</p>
	<b> Have you seen the Boeing 727's yet?</b>
	<p> FSEconomy is currently <i>testing</i> the inclusion of an airliner as an expansion of the "All-In" job types recently introduced. The FSE world has been seeded with several Boeing 727-100 aircraft, and they are exclusively associated with all-in jobs designed for them. The project is currently in its <b>public beta</b> phase, and is subject to change over time as it is refined. Use the airport search features to find the 727's and their jobs if you'd like to take one for a flight!</p>
	<b> Bank of FSE Interest Adjustment</b>
	<p> In an effort to better balance the economics of inflation within FSEconomy, the Board of Directors has opted to change how bank interest is paid. Personal bank balances up to and including $1,000,000 will continue to earn daily interest as they have in the past. Balances over $1M will only earn interest on the first million dollars saved in the bank. Group bank accounts will no longer generate interest.</p>
	<b> Airplane Sellback Changes</b>
	<p> Finally, as a new game dynamic, aircraft "system sellback" prices now have a depreciation modifier applied to them. The system sellback price will decrease from 70% of the base price at 00:00 airframe hours steadily down to a minimum of 25% of the base price at 10,000 hours.</p>   
	</div>
<% 	} 
%>
	<!-- END NEW ITEM --> 
	
	
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
	{
%>
	<div>
	<h3>The Return of "All-In" Jobs</h3>
	<p>FSEconomy has reintroduced the concept of pilot-for-hire (aka: all-in) jobs. These jobs are based on the concept of being hired as a charter pilot for the FSE system itself. The system will have selected a job, selected an airplane, and prepared compensation for the safe completion of the flight. All you have to do is fly it! All-in jobs will pop up at chosen airports and be assigned a specific airplane which matches the job's requirements.</p>
	<p>Simply add the all-in job to your assignment queue and rent the associated airplane to fly one! Remember, with all-in jobs, you are not the boss! Because of this, any other assignments in your "My Flights" page will need to stay on the ground, and only the all-in job should board the designated airplane. That being said, the cost of fuel, rental charges and additional crew fees will all be taken care of for you by "the boss". Once your job is completed you will be paid your job wages minus the <i>very</i> modest ground crew fees which might be owed to local FBO owners.</p>
	</div>
<% 	} 
%>
	<!-- END NEW ITEM --> 
	
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
	{
%>
	<div>
	<h3>Visit the Forums!</h3>
	<p>When asked why they continue to fly with FSEconomy, many of our pilots responded that the <em><a href="http://www.fseconomy.net">community</a></em> is just as much of a part of their FSE enjoyment as the flying.</p>
	<p>The FSE Forums are a great resource for buying and selling airplanes and FBO's, tips and tricks to maximize your enjoyment in FSE, and finding groups and organizations which you can join to further your experience! You can also get up-to-the-minute updates on any server issues, system enhancements and changes which may take place. </p>
	<p>Most of all, the FSE forums are the lifeblood of the FSE community. <b>Don't be shy</b>! Stop on by, take a look, and discover the "hidden" reason why FSEconomy can be such a fun and addicting extension for your flight simulation experience!</p>
	</div>
<% 	} 
%>
	<!-- END NEW ITEM --> 
	
	
	<!-- NEWS ITEM Logged Out Only -->
<%
	if (!user.isLoggedIn()) 
	{ 
%>  
	<div>
	<h3>FSEconomy is FSX and X-Plane Compatible</h3>
	<p>In addition to Microsoft Flight Simulator 2004 and FSX, FSEconomy is also available to <a href="http://www.x-plane.com" onclick="this.target='_blank'">X-Plane</a> users. Read more about it in the FSEconomy Manual.</p> 
	</div>
<% 	} 
%>
	<!-- END NEW ITEM --> 
	
	<!-- NEWS ITEM Logged In Only -->
<%
	if (user.isLoggedIn()) 
		{
%>
	<div>
	<h3>Donations:</h3>
	Coming soon!
	</div>
<% 	} 
%>
	<!-- END NEW ITEM --> 
	
	
	<!-- MAIN PAGE FOOTER  ======================================================= -->
	<div class="message">
	<p>FSEconomy is managed by an appointed Board of Directors and all members of our community agree to abide by the FSEconomy <a href="http://www.fseconomy.net/tos" onclick="this.target='_blank'">Etiquette and Rules of Fair Play</a> whenever they log on to the server.</p>
	<p>&copy; Copyright 2005-2012, <a href="#" onclick="team.setWindowProperties('toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=no,alwaysRaised,dependent,titlebar=no');team.setSize(200,325);team.setUrl('<%= response.encodeURL("team.htm")%>');team.showPopup('team');return false;" id="team">FSE Development Team</a>. You can redistribute and/or modify this source code under the terms of the <a href="gnu.jsp">GNU General Public License</a> as published by the Free Software Foundation; either version 2 of the License, or any later version.&nbsp; FSEconomy and its authors must be publically credited on any site that uses this source code or work derived from this source code. </p>
	</div>
	</div>
	</div>
</div>
</body>
</html>
