package net.fseconomy.util;

// Java code - Cipher mode CBC version.
// CBC version need Initialization vector IV.
// Reference from http://stackoverflow.com/questions/6669181/why-does-my-aes-encryption-throws-an-invalidkeyexception/6669812#6669812

import org.bouncycastle.util.encoders.Base64;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto
{
    public static String key = "0123456789abcdef";
    public static byte[] key_Array = null;

    public static String encrypt(String strToEncrypt)
    {
        try
        {
            Cipher _Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

            // Initialization vector.
            // It could be any value or generated using a random number generator
            // and attached to the start or end of the encrypted message
            byte[] iv = { 1, 3, 2, 4, 5, 6, 6, 5, 4, 3, 2, 1, 7, 5, 5, 3 };
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Key SecretKey = new SecretKeySpec(key.getBytes(), "AES");
            _Cipher.init(Cipher.ENCRYPT_MODE, SecretKey, ivspec);

            return Base64.toBase64String(_Cipher.doFinal(strToEncrypt.getBytes()));
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
            Cipher _Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

            // Initialization vector.
            // It could be any value or generated using a random number generator
            // and attached to the start or end of the encrypted message
            byte[] iv = { 1, 3, 2, 4, 5, 6, 6, 5, 4, 3, 2, 1, 7, 5, 5, 3 };
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Key SecretKey = new SecretKeySpec(key.getBytes(), "AES");
            _Cipher.init(Cipher.DECRYPT_MODE, SecretKey, ivspec);

            return new String(_Cipher.doFinal(Base64.decode(EncryptedMessage)));
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