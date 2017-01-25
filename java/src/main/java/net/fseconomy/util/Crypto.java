package net.fseconomy.util;

// Java code - Cipher mode CBC version.
// CBC version need Initialization vector IV.
// Reference from http://stackoverflow.com/questions/6669181/why-does-my-aes-encryption-throws-an-invalidkeyexception/6669812#6669812

import org.bouncycastle.util.encoders.Base64;
import org.jboss.resteasy.util.Hex;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto
{
    public static byte[] key_Array = "ZonkersScoobyDoo".getBytes();
    static byte[] iv = "0123456789abcdef".getBytes();

    public static String encrypt(String strToEncrypt)
    {
        try
        {
            Cipher _Cipher = Cipher.getInstance("AES/CBC/NOPADDING");

            // Initialization vector.
            // It could be any value or generated using a random number generator
            // and attached to the start or end of the encrypted message
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Key SecretKey = new SecretKeySpec(key_Array, "AES");
            _Cipher.init(Cipher.ENCRYPT_MODE, SecretKey, ivspec);
            byte[] ba = _Cipher.doFinal(strToEncrypt.getBytes());

            return Hex.encodeHex(ba);
        }
        catch (Exception e)
        {
            System.out.println("[Exception]:"+e.getMessage());
        }
        return null;
    }

    public static String decrypt(String EncryptedMessage)
    {
        try
        {
            Cipher _Cipher = Cipher.getInstance("AES/CBC/NOPADDING");

            // Initialization vector.
            // It could be any value or generated using a random number generator
            // and attached to the start or end of the encrypted message
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Key SecretKey = new SecretKeySpec(key_Array, "AES");
            _Cipher.init(Cipher.DECRYPT_MODE, SecretKey, ivspec);

            byte[] bytes = Hex.decodeHex(EncryptedMessage);
            return new String(_Cipher.doFinal(bytes));
        }
        catch (Exception e)
        {
            System.out.println("[Exception]:"+e.getMessage());
        }
        return null;
    }

    public static String getMD5(String toHash)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(toHash.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++)
            {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString().toUpperCase();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return "";
    }
}