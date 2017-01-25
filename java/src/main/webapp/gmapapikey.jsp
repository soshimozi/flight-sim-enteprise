<%
    String server = request.getServerName();
    if (server.contains("localhost"))
    {
%>
        <script src="https://maps.google.com/maps?file=api&amp;v=2.58&amp;key=ABQIAAAAG82z-GJItM3IuG9EOBZAfRSVAaBWqUyeYUcKh2IdrcS-zyqYshS9pF6VeGdPA5RGfpaOt6r7Qr_ewg" type="text/javascript"></script>
<%
    }
    else if (server.contains("fseconomy.net"))
    {
%>
        <script src="https://maps.google.com/maps?file=api&amp;v=2.58&amp;key=AIzaSyDpmF0JgC7Oq-KJ-dxPM1eOFnNxhTzwQ2o"></script>
<% 
    }
    else
    {
%>	
        <script type="text/javascript">
            alert('API Key Not Found');
        </script>
<%	
    }
%>