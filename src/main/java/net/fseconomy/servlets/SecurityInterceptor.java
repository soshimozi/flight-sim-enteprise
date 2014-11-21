package net.fseconomy.servlets;

import static net.fseconomy.services.common.*;

import net.fseconomy.beans.UserBean;
import net.fseconomy.data.DALHelper;
import net.fseconomy.data.Data;
import net.fseconomy.services.Authenticator;
import org.infinispan.Cache;
import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.security.auth.login.LoginException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


@Provider
public class SecurityInterceptor implements ContainerRequestFilter
{
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException
    {
        PostMatchContainerRequestContext pmContext = (PostMatchContainerRequestContext) ctx;

        // When HttpMethod comes as OPTIONS, just acknowledge that it accepts...
        if ( ctx.getRequest().getMethod().equals( "OPTIONS" ) )
        {
            ctx.abortWith( Response.status(Response.Status.OK).build() );
        }

        //check if correct permissions for method
        Method method = pmContext.getResourceMethod().getMethod();

        //Access denied for all
        if(method.isAnnotationPresent(DenyAll.class))
            ctx.abortWith(ResponseAccessDenied());

        //Access allowed for all
        if(method.isAnnotationPresent(PermitAll.class))
            return;

        //for path access checks
        URI uri = ctx.getUriInfo().getRequestUri();

        //get key
        String serviceKey = ctx.getHeaders().getFirst("servicekey");
        if (uri.getPath().contains("/rest/api/") && !Authenticator.getInstance().isServiceKeyValid(serviceKey))
            ctx.abortWith(ResponseAccessDenied());

        if(uri.getPath().contains("/rest/fse/api/") && !uri.getPath().contains("/rest/fse/api/login"))
        {
            String authToken = ctx.getHeaders().getFirst("authtoken");

            if(authToken == null)
                ctx.abortWith(ResponseAccessDenied());

            if(!Authenticator.getInstance().isAuthTokenValid(authToken))
                ctx.abortWith(ResponseAccessDenied());

            //deny on invalid role
            //Verify user access
            if(method.isAnnotationPresent(RolesAllowed.class))
            {
                RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
                Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));

                rolesSet.forEach(System.out::println);

                //Is user valid?
                if( !isUserAllowed(Authenticator.getInstance().getUsernameFromToken(authToken), rolesSet))
                {
                    ctx.abortWith(ResponseAccessDenied());
                }
            }
        }

        //fall thru and process
    }

    private boolean isUserAllowed(final String username, final Set<String> rolesSet)
    {
        boolean flag =rolesSet.contains(getUserLevel(username));
        return flag;
    }

    public String getUserLevel(String userName)
    {
        try
        {
            String qry = "SELECT level FROM accounts a WHERE name = ?";
            String level = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), userName);
            return level;
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return "";
    }
}


