package net.fseconomy.encryption;

import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;

import net.fseconomy.dto.AuthInfo;

public class Encryption
{
    //TODO this key needs to move into the database
    byte[] K = Hex.decode("000102030405060708090A0B0C0D0E0F");
    byte[] N = Hex.decode("000102030405060708090A0B");

    Key key;
    static Encryption encryption = null;

    private Encryption()
    {
        key = new SecretKeySpec(K, "AES");
    }

    public static Encryption getInstance()
    {
        if ( encryption == null )
            encryption = new Encryption();

        return encryption;
    }

    public String encryptAuthInfo(AuthInfo authInfo)
    {
        byte[] enc = null;

        try
        {
            Cipher in = Cipher.getInstance("AES/OCB/NoPadding", "BC");
            in.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(N));

            StringBuilder sb = new StringBuilder();
            sb.append(authInfo.guid).append("|").append(authInfo.name).append("|").append(authInfo.userId);
            byte[] b = sb.toString().getBytes();

            enc = in.doFinal(b);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        byte[] hexenc = Hex.encode(enc);

        return new String(hexenc);
    }

    public AuthInfo decryptAuthInfo(String token)
    {
        AuthInfo authInfo = null;

        try
        {
            Cipher out = Cipher.getInstance("AES/OCB/NoPadding", "BC");
            out.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(N));

            byte[] dec = out.doFinal( Hex.decode(token.getBytes()) );

            String s = new String(dec);
            String[] sa = s.split("\\|");

            authInfo = new AuthInfo();
            authInfo.guid = sa[0];
            authInfo.name = sa[1];
            authInfo.userId = Integer.parseInt(sa[2]);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return authInfo;
    }
}
