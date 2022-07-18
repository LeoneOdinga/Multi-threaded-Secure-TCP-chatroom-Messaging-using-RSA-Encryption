package encryption_decryption_module;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSA 
{
   private PrivateKey privateKey;
   private PublicKey publicKey;
   
   
   public RSA(PrivateKey privateKey, PublicKey publicKey)
   {
	   this.privateKey = privateKey;
	   this.publicKey = publicKey;
   }
   
   public RSA() throws NoSuchAlgorithmException
   {
	   KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
	   generator.initialize(2048);
	   KeyPair pair = generator.generateKeyPair();
	   this.privateKey = pair.getPrivate();
	   this.publicKey = pair.getPublic();
   }
   
   public PublicKey getPublicKey()
   {
	   return publicKey;
   }
   
   public PrivateKey getPrivateKey()
   {
	   return privateKey;
   }
   
   public String encrypt(String message, PublicKey public_Key) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
   {
	   byte[] messageToBytes = message.getBytes();
	   
	   Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	   cipher.init(Cipher.ENCRYPT_MODE, public_Key);
	   
	   byte[] encryptedBytes = cipher.doFinal(messageToBytes);
	   return encode(encryptedBytes);
   }
    
   private String encode(byte[] data)
   {
	   return Base64.getEncoder().encodeToString(data);
   }
   public String decrypt(String encryptedMessage, PrivateKey privateKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException
   {
	   byte[] encryptedBytes = decode(encryptedMessage);
	   Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	   cipher.init(Cipher.DECRYPT_MODE, privateKey);
	   
	   byte[] decryptedMessage = cipher.doFinal(encryptedBytes);
	   
	   return new String(decryptedMessage, "UTF-8");
   }
   public byte[] decode(String data)
   {
	   return Base64.getDecoder().decode(data);
   }
   
   public PublicKey revertEncodedPublicKey(String publicKey) throws InvalidKeySpecException, NoSuchAlgorithmException
   {
	   byte[] publicBytes = Base64.getDecoder().decode(publicKey);
	   X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
	   KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	   PublicKey pubKey = keyFactory.generatePublic(keySpec);
	   
	   return pubKey;
   }
   public PrivateKey revertEncodedPrivateKey(String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException
   {
	   byte[] privateBytes = Base64.getDecoder().decode(privateKey);
	   X509EncodedKeySpec keySpec = new X509EncodedKeySpec(privateBytes);
	   KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	   PrivateKey privKey = keyFactory.generatePrivate(keySpec);
	   
	   return privKey;
   }
}
