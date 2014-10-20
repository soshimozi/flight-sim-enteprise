package net.fseconomy.servlets;

import net.fseconomy.data.Data;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Provider
public class SecurityInterceptor implements ContainerRequestFilter
{
    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String AUTHENTICATION_SCHEME = "Basic";
    private static final Response ACCESS_DENIED = new ServerResponse("Access denied for this resource", 401, new Headers<Object>());;
    private static final Response ACCESS_FORBIDDEN = new ServerResponse("This resource is not available.", 403, new Headers<Object>());;
    private static final Response SERVER_ERROR = new ServerResponse("INTERNAL SERVER ERROR", 500, new Headers<Object>());;


    @Override
    public void filter(ContainerRequestContext ctx) throws IOException
    {
        PostMatchContainerRequestContext pmContext = (PostMatchContainerRequestContext) ctx;

        Method method = pmContext.getResourceMethod().getMethod();

        //Access allowed for all
        if(method.isAnnotationPresent(PermitAll.class))
            return;

        //Access denied for all
        if(method.isAnnotationPresent(DenyAll.class))
            ctx.abortWith(ACCESS_FORBIDDEN);

        //Get request headers
        final MultivaluedMap<String, String> headers = ctx.getHeaders();

        //get key
        String key = headers.getFirst("servicekey");
        if(key == null)
            ctx.abortWith(ACCESS_DENIED);

        //check if correct permissions for method

        //deny on invalid role
        //Verify user access
        if(method.isAnnotationPresent(RolesAllowed.class))
        {
            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
            Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));

            rolesSet.forEach(System.out::println);

            //Is user valid?
//            if( ! isUserAllowed(username, password, rolesSet))
//            {
//                return ACCESS_DENIED;
//            }
        }
        //fall thru and process
    }

    private boolean isUserAllowed(final String username, final String password,	final Set<String> rolesSet)
    {
        boolean isAllowed = false;

        String userRole = "ADMIN";

        //get roles for key
        Data data = Data.getInstance();
//        AccessPermisions permissions = data.getAccessPermissionsByKey(key);


        //Step 2. Verify user role
        if(rolesSet.contains(userRole))
        {
            isAllowed = true;
        }

        return isAllowed;
    }

}


