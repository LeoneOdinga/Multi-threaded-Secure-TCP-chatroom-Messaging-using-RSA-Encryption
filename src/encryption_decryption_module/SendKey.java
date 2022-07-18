package encryption_decryption_module;

import java.io.Serializable;
import java.security.PublicKey;

//Implements a serializable interface for sending public key as a string object in binary form

public class SendKey implements Serializable
{
	private static final long serialVersionUID = 1L;
    private final String text;

 public SendKey(String text) 
 {
     this.text = text;
 }
 public SendKey(PublicKey clientPublicKey)
 {
	this.text = "";
 }

 public String getText() 
 {
     return text;
 }
}