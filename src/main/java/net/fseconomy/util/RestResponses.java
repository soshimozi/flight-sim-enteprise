package net.fseconomy.util;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;

import javax.ws.rs.core.Response;

/**
 * Created by smobley on 10/20/2014.
 */
public class RestResponses
{
    public static final String AUTHORIZATION_PROPERTY = "Authorization";
    public static final String AUTHENTICATION_SCHEME = "Basic";
    public static final Response ACCESS_DENIED = new ServerResponse("Access denied for this resource", 401, new Headers<Object>());;
    public static final Response ACCESS_BADREQUEST = new ServerResponse("Bad request parameters", 400, new Headers<Object>());;
    public static final Response ACCESS_FORBIDDEN = new ServerResponse("This resource is not available", 403, new Headers<Object>());;
    public static final Response SERVER_ERROR = new ServerResponse("INTERNAL SERVER ERROR", 500, new Headers<Object>());;
}
