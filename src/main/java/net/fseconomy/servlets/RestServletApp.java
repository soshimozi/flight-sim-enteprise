package net.fseconomy.servlets;

import java.util.Set;
import java.util.HashSet;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


@ApplicationPath("/rest")
public class RestServletApp extends Application
{
    private Set<Object> singletons = new HashSet<>();
    private Set<Class<?>> perCall = new HashSet<>();

    public RestServletApp()
    {
        singletons.add(new RestServlet());
        singletons.add(new FSERestServlet());
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        return perCall;
    }

    @Override
    public Set<Object> getSingletons()
    {
        return singletons;
    }
}

