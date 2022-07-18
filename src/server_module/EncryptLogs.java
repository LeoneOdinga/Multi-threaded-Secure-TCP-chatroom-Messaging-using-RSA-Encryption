package server_module;

import java.io.Console;
import java.util.InputMismatchException;
import java.util.Scanner;

import encryption_decryption_module.AES;

public class EncryptLogs 
{
  /**This will be a runnable tool used by the server admin to encrypt the server logs(can enrypt the logs at runtime! too)
   *Can also encrypt other files but for now, the server logs*/
  // The hashed password of the server Admin will be used to secure the file()
  
  public static void main(String[] args)
  {
	  System.out.println("========================================ENCRYPT/DECRYPT SERVER LOGS======================================");
	  System.out.println("WARNING!\nThis tool uses AES Encryption Algorithm and generates 256 bit secret key from the server password"
	  		+ "\nUse this tool with caution. If you do double encryption "
	  		+ "you need to decrypt the file twice and so on.\nThe Server Password must be correct to use this tool.\n"
	  		+ "FOR ENCRYPTION, USE IT ONLY WHEN ABOUT TO CLOSE THE SERVER(Or ON SERVER CLOSURE ) to avoid data loss!\n"
	  		+ "DECRYPT THE SERVER LOGS BEFORE STARTING SERVER! to avoid data loss");
	  Scanner sc = new Scanner(System.in);
	  //prompt the server admin for the username
	  System.out.println("\nEnter Your Username(SERVER ADMIN USERNAME): ");
	  String serverAdminUsername = sc.nextLine();
	  
	  //prompt the server admin for the server password
	  Console cons = System.console();
	  EncryptLogs encryptLogs = new EncryptLogs();
	  String serverAdminPassword = encryptLogs.getPasswordMasked(cons,"Enter the server password: ");
	  
	  //validate if the server admin password is correct before using the tool
	  LogManagement logManagement = new LogManagement();
	  AES aes = new AES();
	  
	  if(logManagement.serverAdminAuth(serverAdminUsername,serverAdminPassword))
	  {
		  //auth successful, so make the server admin encrypt or decrypt files(server log file)!
		  
		  //present options
		  System.out.println("\nWhat do you want to do?\n1. Encrypt Server Logs\n2. Decrypt Server Logs");
		  int choiceSelected =0;
		  try
		  {
			  choiceSelected = sc.nextInt();
		  }catch(InputMismatchException ime)
		  {
			  System.out.println(ime.getMessage());
		  }
		  
			  switch(choiceSelected)
			  {
			  case 1:
				  aes.encryptServerLogs(logManagement.logFileName, serverAdminPassword);
				  System.out.println("Encryption Successful!");
				  break;
			  case 2:
				  aes.decryptServerLogs(logManagement.logFileName, serverAdminPassword);
				  System.out.println("Decryption Successful!");
				  break;
			  default:
				  System.out.println("Invalid Option");
			  }
	  }
	  else
	  {
		  System.out.println("Server Authentication Failed. Try Again!");
		  System.exit(0);
	  }
	  sc.close();
  }
  protected String getPasswordMasked(Console cons, String msg)
  {
	  char[] passwd;
	  if(cons == null)
	  {
		  System.out.println("Run this tool in any terminal/cmd!");
		  System.exit(0);
	  }
	  while(true)
	  {
		  passwd = cons.readPassword("%s", msg);
		  if(passwd != null)
		  {
			  if(passwd.length >0)
			  {
				  return new String(passwd);
			  }
			  else
			  {
				  System.out.println("Invalid Input!");
				  System.exit(0);
			  }
		  }
	  } 
  }
}
