package client_module;

import java.util.*;

public class TerminalClient extends Client 
{
	private Scanner terminalInput;
	
	public TerminalClient(String serverIPAddress, String serverPort)
	{
		this.serverIPAddress = serverIPAddress;
		this.serverPort = Integer.parseInt(serverPort);
		this.username = "";
		this.terminalInput = new Scanner(System.in);
		
		connectToServer();
	}
	
	protected void sendClientRequests()
	{
		/*POLULATE AN OPTION LIST INTO ARRAY LIST FOR EASY NAVIGATION*/
		
		ArrayList<String> optionsList = new ArrayList<String>();
		optionsList.add("Send Private Encrypted Message");
		optionsList.add("View All connected Clients");
		optionsList.add("Your Username");
		optionsList.add("Clear Terminal");
		optionsList.add("Hidden Commands");
		optionsList.add("Exit Options Mode");
		optionsList.add("Close Connection");
		
		final String HIDDEN_COMMANDS="\nUSE THESE COMMANDS ONLY IN BROADCAST MODE!\nShow Public Key< /publicKey >\nShow Private Key < /privateKey>\n"
				+ "Show Encoded Public Key < /encodedPublicKey >\nShow Encoded Private Key < /encodedPrivateKey >"
				+ "\nShow Online and Offline Users < /printActiveUsers>\nDisplay only Online Users < /users >\n Close Connection < /exit >"
				+ "\nShow Your Username < /whoami >";
		
		boolean modeBroadcast =true;
		
		while (true)
		{   
			if(modeBroadcast)
			{
				printToConsole("\nYOU ARE IN BROADCAST MODE![Messages Sent are Publicly Available to all connected clients]\n");
				printToConsole("Type command => /options to open messaging options");
			}
			modeBroadcast= false;
			
			String msg = terminalInput.nextLine();
			
			/************************handle user interaction through options************************/
			
			if(msg.equals("/options"))
			{   
				printToConsole("=================YOU ARE IN OPTIONS MODE!=====================");
				printToConsole("Select the action to perform: \n");
				printOptionsList(optionsList);
				
				Scanner sc = new Scanner(System.in);
				while(true)
				{
					
					int user_choice =0;
					try
					{
						user_choice = sc.nextInt();
						sc.nextLine();	
					}catch(InputMismatchException ime){
						System.out.println("Input Error! Try Again");
						printOptionsList(optionsList);
						break;
					}
					
					if(user_choice == 1)
					{
						  printToConsole("=================YOU ARE IN PRIVATE MODE[Send Encrypted Private Message]!================");
						  String privateMessage = "";
						  String privateMessageSegments[] = {"/privateMessage","",""};
						  sendMessage("/users");
						  System.out.println("Enter the receiver's username: ");
						  String recvUsername = sc.nextLine();
						  privateMessageSegments[1] = recvUsername;
						  System.out.println("Enter Your Secret Message to: "+recvUsername);
						  String seretMessage = sc.nextLine();
						  privateMessageSegments[2] =seretMessage;
						  privateMessage+=privateMessageSegments[0]+" "+privateMessageSegments[1]+" "+privateMessageSegments[2];
						  sendMessage(privateMessage);
						  printToConsole("=======================END OF PRIVATE MODE[Back To Options Mode]=========================");
					}
					else if(user_choice == 2)
					{
						sendMessage("/users");
					}
					else if(user_choice == 3)
					{
						sendMessage("/whoami");
					}
					else if(user_choice == 4)
					{
						clearOutput();
					}
					else if(user_choice == 5)
					{
						System.out.println(HIDDEN_COMMANDS);
					}
					else if(user_choice ==6)
					{
						printToConsole("===========================ENDED OPTIONS MODE=======================");
						modeBroadcast = true;
						break;
					}
					else if(user_choice == 7)
					{
						sendMessage("/exit");
						modeBroadcast = true;
					}
					else
					{
						System.out.println("Input Error.Try Again");
					}
				}
			}
			else if(msg.equals("/privateKey"))
			{
				System.out.println(getPrivateKey());
				continue;
			}
			else if(msg.equals("/encodedPrivateKey"))
			{
				printEncodedKeys(getEncodedPrivateKey(),"PRIVATE");
				continue;
			}
			else if(msg.equals("/encodedPublicKey"))
			{
				printEncodedKeys(getEncodedPublicKey(),"PUBLIC");
			}
			else
			{
				sendMessage(msg);	
			}
			
			if (msg.equals("/exit"))
				break;
		}
		terminalInput.close();
	}
	  
	protected String getInput(String prompt)
	{
		System.out.print(prompt);
		return terminalInput.nextLine();
	}
	
	public void printToConsole(String msg)
	{
		System.out.println(msg);
	}
	
	public void alertMessage(String alert)
	{
		System.out.println(alert);
	}
	
	public void clearOutput()
	{
		if (System.getProperty("os.name").toLowerCase().indexOf("win") != -1)
		{
			try
			{
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
		else
		{
			System.out.println("\033[H\033[2J");
		}
	}
	public void printOptionsList(ArrayList<String> options)
	{
		int option_position =1;
		for(String option: options)
		{
			System.out.println(option_position+" "+option);
		    option_position++;	
		}
	}
}