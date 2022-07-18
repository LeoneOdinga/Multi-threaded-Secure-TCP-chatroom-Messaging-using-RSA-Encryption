package server_module;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class LogManagement 
{
	
	/***********Define the filename where the server logs and public keys would be stored*************/
	public final String logFileName = "serverLogs.txt";
	
	protected final String publicKeyLog = "publicKeys.txt";
	
	
	/*******************Define the file to store the server admin passwords***************************/
	protected final String serverPasswordFile = "serverPass.txt";
	
	/*******************Define the file to store the clients hashed passwords*************************/
	protected final String clientHashedPasswords = "clientPass.txt";
	
	
	public LogManagement() {};
	
	/*******************check is a given file exists**************************************
	 */
	public boolean fileExist(File fileObj)
	{
		return fileObj.exists();
	}
	/*****************Create(if not file exists) or write to a log file******************/
	
	public void writeToFile(String fileName, String logs)
	{
		File fileObj = new File(fileName);
		try
		{
			if(!fileExist(fileObj))
			{
				fileObj.createNewFile();
				System.out.println(fileName+"created in the path => "+fileObj.getAbsolutePath());
			}
			PrintWriter printWriter = new PrintWriter(new FileWriter(fileObj,true));
			printWriter.println(logs);
			printWriter.close();
		}
		catch(IOException ioe)
		{
			System.out.println(ioe.getMessage());
		}
	}
	public void writeToPassFile(String filename)
	{
		ServerHandler serverHandler =  new ServerHandler();
		
		File fileObj = new File(filename);
		try
		{
			if(!fileExist(fileObj))
			{
			   //prompt user to set the server password
				Scanner sc = new Scanner(System.in);
				System.out.println("Pass File Not Found. Please set the server password");
				
				System.out.println("Enter your Username(SERVER ADMIN)");
				String username = sc.nextLine();
				
				System.out.println("Enter Your Password: ");
				String plainTextPass = sc.nextLine();
				
				//hash the plaintext password
				String hashedPassword = serverHandler.hashPassword(plainTextPass);
				
				//create the serverpass file and store this password
				fileObj.createNewFile();
				
				//write the hashed password to the file
				PrintWriter printWriter = new PrintWriter(new FileWriter(fileObj,true));
				printWriter.println(username+" "+hashedPassword);
				printWriter.close();
				sc.close();
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	public boolean serverAdminAuth(String username, String password)
	{
		boolean authenticationPassed = false;
		LogManagement logManagement = new LogManagement();
		File fileObj = new File(logManagement.serverPasswordFile);
		ServerHandler serverHandler = new ServerHandler();
		Scanner myReader = null;
		
		try {
			myReader = new Scanner(fileObj);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
		String data = null;
		
		ArrayList<String> authArray = new ArrayList<String>();
		
		while(myReader.hasNext())
		{
			data = myReader.nextLine();
			authArray.add(data);
		}
		
		for(String str: authArray)
		{
			String serverAuth[] = str.split("\\s");
			String serverAdminUsername = serverAuth[0];
			if(serverAdminUsername.equals(username))
			{
				System.out.println("Server Admin Username VALID! ");
				String serverAdminPass = serverAuth[1];
				
				authenticationPassed = serverHandler.hashPassword(password).equals(serverAdminPass)? true:false;
				break;
			}
			else
			{
				System.out.println("Invalid Username. Try Again");
				System.exit(0);
			}
		}
		return authenticationPassed;
	}
	public boolean nameExistInFile(String username, String filename)
	{
		File fileObj = new File(filename);
		Scanner myReader = null;
		boolean nameExist = false;
		
		try {
			myReader = new Scanner(fileObj);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
		String data = null;
		ArrayList<String>dataArray = new ArrayList<String>();
		while(myReader.hasNext())
		{
			data = myReader.nextLine();
			dataArray.add(data);
		}
		for(String str: dataArray)
		{
			String clientHash[] = str.split("\\s");
			String clientUsername = clientHash[0];
			
			if(clientUsername.equals(username))
			{
				nameExist = true;
				break;
			}	
		}
		return nameExist;
	}
	
	public boolean clientAuth(String tempUsername, String clientPassword) throws IOException 
	{
		boolean authenticationPassed = false;
		ServerHandler serverHandler = new ServerHandler();
		LogManagement logManagement = new LogManagement();
		File fileObj = new File(logManagement.clientHashedPasswords);
		
		if(!fileExist(fileObj))
		{
			fileObj.createNewFile();
		}
		Scanner myReader = null;
		
		try {
			myReader = new Scanner(fileObj);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
		String data = null;
		
		ArrayList<String> authArray = new ArrayList<String>();
		
		while(myReader.hasNext())
		{
			data = myReader.nextLine();
			authArray.add(data);
		}
		
		for(String str: authArray)
		{
			if(str.contains(tempUsername))
			{
				String clientAuth[] = str.split("\\s");
				String clientPass = clientAuth[1];
				
				authenticationPassed = clientPassword.equals(clientPass)? true:false;
			}
		}
		return authenticationPassed;
	}
}
