package net.fseconomy.servlets;

import static net.fseconomy.services.common.*;

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
            ctx.abortWith(ResponseAccessDenied());

        //get key
        String key = ctx.getHeaders().getFirst("servicekey");
        if(key == null)
            ctx.abortWith(ResponseAccessDenied());

         //check if correct permissions for method

        //deny on invalid role
        //Verify user access
//        if(method.isAnnotationPresent(RolesAllowed.class))
//        {
//            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
//            Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
//
//            rolesSet.forEach(System.out::println);
//
//            //Is user valid?
//            if( ! isUserAllowed(username, password, rolesSet))
//            {
//                return ACCESS_DENIED;
//            }
//        }
        //fall thru and process
    }

//    private boolean isUserAllowed(final String username, final String password,	final Set<String> rolesSet)
//    {
//        boolean isAllowed = false;
//
//        serviceData.hasPermission()
////        String userRole = "ADMIN";
//
//        //get roles for key
////        Data data = Data.getInstance();
////        AccessPermisions permissions = data.getAccessPermissionsByKey(key);
//
//
//        //Step 2. Verify user role
////        if(rolesSet.contains(userRole))
////        {
////            isAllowed = true;
////        }
//
//        return isAllowed;
//    }

}


