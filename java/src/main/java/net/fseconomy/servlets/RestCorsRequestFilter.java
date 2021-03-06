package net.fseconomy.servlets;

//import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
//import javax.ws.rs.container.PreMatching;
//import javax.ws.rs.core.HttpHeaders;
//import javax.ws.rs.core.MultivaluedMap;
//import javax.ws.rs.core.Response;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
        import javax.ws.rs.ext.Provider;
import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Set;

@Provider
@PreMatching
public class RestCorsRequestFilter implements ContainerRequestFilter
{
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException
    {
        // When HttpMethod comes as OPTIONS, just acknowledge that it accepts...
        if ( ctx.getRequest().getMethod().equals( "OPTIONS" ) )
        {
            // Just send a OK signal back to the browser
            ctx.abortWith( Response.status(Response.Status.OK).build() );
        }

    }

}


