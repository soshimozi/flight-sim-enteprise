package net.fseconomy.services;

import net.fseconomy.data.DALHelper;
import net.fseconomy.data.Data;
import net.fseconomy.dto.AuthInfo;
import net.fseconomy.encryption.Encryption;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Authenticator
{
    private static Authenticator authenticator = null;

    // An authentication token storage which stores <authtoken, authinfo>.
    private static HashMap<String, AuthInfo> tokenCache = new HashMap<>();
    private static HashMap<String, String> serviceKeyCache = new HashMap<>();

    public Authenticator()
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
        int userId = getUserId(username, password);
        if (userId != 0)
        {
            //does token already exist?
            String foundToken = getKeyByValue(tokenCache, userId);
            if(foundToken != null)
                return foundToken;

            AuthInfo authInfo = new AuthInfo();
            authInfo.name = username;
            authInfo.userId = userId;
            authInfo.guid = UUID.randomUUID().toString();

            String authToken = Encryption.getInstance().encryptAuthInfo(authInfo);
            tokenCache.put(authToken, authInfo);

            return authToken;
        }

        return null;
    }

    public static String getKeyByValue(Map<String, AuthInfo> map, int value)
    {
        for (Map.Entry<String, AuthInfo> entry : map.entrySet())
        {
            if (value == entry.getValue().userId)
                return entry.getKey();
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
        return tokenCache.containsKey(authToken);
    }

    public boolean logout(String authToken )
    {
        if ( !isAuthTokenValid( authToken ) )
            return false;

        AuthInfo tokenAuthInfo = Encryption.getInstance().decryptAuthInfo(authToken);
        AuthInfo authInfo = tokenCache.get(authToken);
        if(!authInfo.name.equals(tokenAuthInfo.name))
            return false;

        tokenCache.remove( authToken );

        return true;
    }

    // return of 0 indicates not found
    public int getUserId(String userName, String password)
    {
        int result = 0;

        try
        {
            String qry = "SELECT id FROM accounts a WHERE name = ? and password = PASSWORD(?)";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), userName, password);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public int getUserIdFromToken(String authToken)
    {
        AuthInfo authInfo = tokenCache.get(authToken);
        return authInfo.userId;
    }

    public String getUsernameFromToken(String authToken)
    {
        AuthInfo authInfo = tokenCache.get(authToken);
        return authInfo.name;
    }

    public boolean isServiceKeyValid(String serviceKey)
    {
        boolean result = false;

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
