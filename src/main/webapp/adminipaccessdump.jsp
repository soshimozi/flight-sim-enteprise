<%@ page 
language="java"
contentType="text/html; charset=ISO-8859-1"
import="java.io.*"%>
<%

    FileReader fr = new FileReader("IPPageAccess.log");
    BufferedReader br = new BufferedReader(fr);

    // Process lines from file
    String line;
    while((line = br.readLine()) != null) 
    {
%>
	<%=line%><br />
<%    
    }
    fr.close();
%>