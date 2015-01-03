package net.fseconomy.services;

import net.fseconomy.data.DALHelper;
import net.fseconomy.data.Data;
import net.fseconomy.dto.AuthInfo;
import net.fseconomy.encryption.Encryption;
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
    private final Map<String, AuthInfo> authorizationTokensStorage = new HashMap<>();

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
        AuthInfo authInfo = new AuthInfo();
        authInfo.name = username;

        if (isUsernamePasswordValid(username, password, authInfo))
        {
            authInfo.guid = UUID.randomUUID().toString();
            String authToken = Encryption.getInstance().encryptAuthInfo(authInfo);
            authorizationTokensStorage.put( authToken, authInfo );

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
    public boolean isAuthTokenValid(String authToken)
    {
        return authorizationTokensStorage.containsKey(authToken);
    }

    public boolean logout(String authToken )
    {
        if ( !isAuthTokenValid( authToken ) )
            return false;

        AuthInfo tokenAuthInfo = Encryption.getInstance().decryptAuthInfo(authToken);
        AuthInfo authInfo = authorizationTokensStorage.get(authToken);
        if(!authInfo.name.equals(tokenAuthInfo.name))
            return false;

        authorizationTokensStorage.remove( authToken );

        return true;
    }

    public boolean isUsernamePasswordValid(String userName, String password, AuthInfo authInfo)
    {
        boolean result = false;

        try
        {
            String qry = "SELECT id FROM accounts a WHERE name = ? and password = PASSWORD(?)";
            int id = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), userName, password);
            if(id > 0)
            {
                authInfo.userId = id;
                result = true;
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public String getUsernameFromToken(String authToken)
    {
        AuthInfo authInfo = authorizationTokensStorage.get(authToken);
        return authInfo.name;
    }

    public boolean isServiceKeyValid(String serviceKey)
    {
        boolean result = false;
        Cache<String, String> serviceKeyCache = UserCtl.cacheManager.getCache("ServiceKey-cache");

        if(serviceKey.equals(Data.adminApiKey) || serviceKeyCache.get(serviceKey) != null)
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
