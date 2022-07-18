package encryption_decryption_module;

import java.util.ArrayList;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import server_module.LogManagement;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Scanner;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**Password based AES encryption code courtesy of: https://github.com/mkyong/core-java/blob/master/java-crypto/src/main/java/com/mkyong/crypto/encryptor/EncryptorAesGcmPassword.java**/

public class AES 
{
    public static byte[] getRandomNonce(int numBytes) 
    {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }
    // AES secret key
    public static SecretKey getAESKey(int keysize) throws NoSuchAlgorithmException
    {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keysize, SecureRandom.getInstanceStrong());
        return keyGen.generateKey();
    }

    // AES 256 bits secret key derived from a password
    public static SecretKey getAESKeyFromPassword(char[] password, byte[] salt)throws NoSuchAlgorithmException, InvalidKeySpecException 
    {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        // iterationCount = 65536
        // keyLength = 256
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;

    }
    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";

    private static final int TAG_LENGTH_BIT = 128; // must be one of {128, 120, 112, 104, 96}
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    // return a base64 encoded AES encrypted text
    public static String encrypt(byte[] pText, String password) throws Exception 
    {
        // 16 bytes salt
        byte[] salt = getRandomNonce(SALT_LENGTH_BYTE);

        // GCM recommended 12 bytes iv?
        byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

        // secret key from password
        SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

        // ASE-GCM needs GCMParameterSpec
        cipher.init(Cipher.ENCRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] cipherText = cipher.doFinal(pText);

        // prefix IV and Salt to cipher text
        byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
                .put(iv)
                .put(salt)
                .put(cipherText)
                .array();

        // string representation, base64, send this string to other for decryption.
        return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);
    }

    // we need the same password, salt and iv to decrypt it
    private static String decrypt(String cText, String password) throws Exception 
    {
        byte[] decode = Base64.getDecoder().decode(cText.getBytes(UTF_8));

        // get back the iv and salt from the cipher text
        ByteBuffer bb = ByteBuffer.wrap(decode);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);

        byte[] salt = new byte[SALT_LENGTH_BYTE];
        bb.get(salt);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        // get back the aes key from the same password and salt
        SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

        cipher.init(Cipher.DECRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText, UTF_8);

    }
    //create a function to read the file contents we want to encrypt into an array list of string
    
    public static ArrayList<String> readFile(String filename)
    {
    	File fileObj = new File(filename);
    	Scanner myReader = null;
    	String fileLine = null;
    	ArrayList<String>returnedList = new ArrayList<String>();
    	try {
			myReader = new Scanner(fileObj);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	while(myReader.hasNext())
    	{
    		fileLine = myReader.nextLine();
    	    //add the file line to the array list
            returnedList.add(fileLine);
    	}
    	//now return the array list -> returnedList to the caller
    	return returnedList;
    }
    
    //create a function to encrypt the server logs
    public void encryptServerLogs(String filename, String serverAdminPassword)
    {
    	LogManagement logManagement = new LogManagement();
    	//read the server logs file into memory
        ArrayList<String>logFileData = readFile(filename);
       
        //delete all the contents of the current file
        PrintWriter pw = null;
		try {
			pw = new PrintWriter(filename);
		} catch (FileNotFoundException e) {
		   System.out.println(e.getMessage());
		}finally {pw.close();};
		
        //now replace the file(0bytes) with the encrypted content
        for(String data: logFileData)
        {
        	try {
				logManagement.writeToFile(filename, encrypt(data.getBytes(UTF_8), serverAdminPassword));
			} catch (Exception e) {
				continue;
			}
        }
    }
    //create a function to decrypt the server logs
    public void decryptServerLogs(String filename, String serverAdminPassword)
    {
    	LogManagement logManagement = new LogManagement();
    	
    	//now let us attempt to recover our encrypted file
        ArrayList<String>encryptedLogData = readFile(filename);
        //delete all the encrypted content
        PrintWriter pwt = null;
		try {
			pwt = new PrintWriter(filename);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}finally {pwt.close();};
        
        //now replace the file witht the original(decrypted )content
        for(String encryptedData: encryptedLogData)
        {
        	try {
				logManagement.writeToFile(filename, decrypt(encryptedData, serverAdminPassword));
			} catch (Exception e) {
			   continue;
			}
        }
    }
}