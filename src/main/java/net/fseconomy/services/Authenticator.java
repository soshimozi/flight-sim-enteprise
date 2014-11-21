package net.fseconomy.services;

import net.fseconomy.data.DALHelper;
import net.fseconomy.data.Data;
import net.fseconomy.servlets.UserCtl;
import org.infinispan.Cache;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Authenticator
{

    private static Authenticator authenticator = null;

    // An authentication token storage which stores <service_key, auth_token>.
    private final Map<String, String> authorizationTokensStorage = new HashMap<>();

    private Authenticator()
    {
    }

    public static Authenticator getInstance()
    {
        if ( authenticator == null )
            authenticator = new Authenticator();

        return authenticator;
    }

    public String login( String username, String password )
    {
        if (isUsernamePasswordValid(username, password))
        {
            String authToken = UUID.randomUUID().toString();
            authorizationTokensStorage.put( authToken, username );

            return authToken;
        }

        return null;
    }

    /**
     * The method that pre-validates if the client which invokes the REST API is
     * from a authorized and authenticated source.
     *
     * @param authToken The authorization token generated after login
     * @return TRUE for acceptance and FALSE for denied.
     */
    public boolean isAuthTokenValid( String authToken )
    {
        return authorizationTokensStorage.containsKey(authToken);
    }

    public boolean logout(String userName, String authToken )
    {
        if ( !isAuthTokenValid( authToken ) )
            return false;

        String username = authorizationTokensStorage.get(authToken);
        if(!username.equals(userName))
            return false;

        authorizationTokensStorage.remove( authToken );

        return true;
    }

    public boolean isUsernamePasswordValid(String userName, String password)
    {
        boolean result = false;

        try
        {
            String qry = "SELECT count(`id`) > 0 as found FROM accounts a WHERE name = ? and password = PASSWORD(?)";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), userName, password);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public String getUsernameFromToken(String authToken)
    {
        return authorizationTokensStorage.get(authToken);
    }

    public boolean isServiceKeyValid(String serviceKey)
    {
        boolean result = false;
        Cache<String, String> serviceKeyCache = UserCtl.cacheManager.getCache("ServiceKey-cache");

        if(serviceKey.equals(Data.adminApiKey) || serviceKeyCache.get(serviceKey) == "")
        {
            return true;
        }
        else
        {
            try
            {
                String qry = "SELECT count(`key`) > 0 as found FROM serviceproviders sp WHERE sp.key = ?";
                result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), serviceKey);
                if(result)
                    serviceKeyCache.put(serviceKey, "");
            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
        }

        return result;
    }

}
