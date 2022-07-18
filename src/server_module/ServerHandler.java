package server_module;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import server_module.Server.ClientHandler;

public class ServerHandler 
{
    public void clearOutput()
	{
		if (System.getProperty("os.name").toLowerCase().indexOf("win") != -1)
			// Clear command prompt
			try
			{
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			}
			catch (Exception err)
			{
				err.printStackTrace();
				System.out.println("Error clearing screen.");
			}
		else
			// Clear terminal
			System.out.println("\033[H\033[2J");
	}
    
  public String getTimestamp()
  {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss a");
	LocalDateTime now = LocalDateTime.now();
	return "[" + dtf.format(now) + "]";
  }
  public String getServerLaunchDate()
  {
	  DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss a");
	  LocalDateTime now = LocalDateTime.now();
	  System.out.println(dtf.format(now));
	  return dtf.format(now);
  }
  
  protected String getOnlineUsers(ArrayList<ClientHandler> clients)
	{
		String users = "\nOnline Users: [";
		for (ClientHandler client : clients)
			
			users += client.getUsername() + ", ";
		users = users.substring(0, users.length() - 2) + "]";
		return users;
	}
  
  protected ClientHandler getClient(String username, ArrayList<ClientHandler> clients)
	{
		for (ClientHandler client : clients)
			if (username.equals(client.getUsername()))
				return client;
		
		return null;
	}
  
	protected String[] extractMessageSegments(String msg, int numberOfArgs)
	{
		
		final int MIN_LEN = 3; 
		final int MSG_ARG_START = 3; 
		final int SEG_ARG_START = MIN_LEN + 1;
		
		// Create array for final segments
		String[] fSegments = new String[SEG_ARG_START + numberOfArgs];
		
		// Split raw segments at each space
		String[] rSegments = msg.split(" ");
		
		if (rSegments.length + 1 < fSegments.length && numberOfArgs > 0)
			return new String[] {"Too few arguments"};
		
		// Store the basic info
		fSegments[0] = rSegments[0];
		fSegments[1] = rSegments[1].substring(1, rSegments[1].length() - 1);
		fSegments[2] = "";
		fSegments[3] = rSegments.length > 2 && rSegments[2].indexOf("/") == 0 ? rSegments[2] : ""; // Store command if it begins with a /
		
		if (numberOfArgs > 0 && fSegments[3].length() == 0)
			return new String[] {"No command found"};
			
		for (int i = 0; i < numberOfArgs; i++)
			fSegments[SEG_ARG_START + i] = rSegments[MSG_ARG_START + i];
			
		int msgStart = MIN_LEN + fSegments[3].indexOf("/") + numberOfArgs;
		for (int i = msgStart; i < rSegments.length; i++)
		{
			
			if (i != msgStart)
				fSegments[2] += " ";
			
			fSegments[2] += rSegments[i];
			
		}
		return fSegments;
	}
	
	protected void log(String msg)
	{
		System.out.println(getTimestamp() + " " + msg);
	}
	
	protected boolean usernameIsValid(String username, ArrayList<ClientHandler>clients)
	{
		boolean verdict = true;
		for (ClientHandler client : clients)
			if (client.getUsername() != null && client.getUsername().equals(username))
			{
				verdict = false;
				break;
			}
		return verdict;
	}
	protected String hashPassword(String password)
	{
		try
		{
			MessageDigest msgDg = MessageDigest.getInstance("SHA-256");
			msgDg.update(password.getBytes());
			return String.format("%032x", new BigInteger(1, msgDg.digest()));
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			return null;
		}
	}
}