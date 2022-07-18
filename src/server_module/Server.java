package server_module;

import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import encryption_decryption_module.RSA;
import encryption_decryption_module.SendKey;

public class Server 
{
	private int port;
	private int clientNumber;
	private ArrayList<ClientHandler> clients;
	private ArrayList<String> messageCache;
	private static int MAX_CLIENTS = 100;
	private final int MAX_CACHE_SIZE = 100;
	
	private final String HELP_TEXT = "\nOptions Menu\t => /options \nExit\t => /exit\n";
    
	/****************Get the network details of the server****************/
	static NetworkInterfaceDetails netDetails = new NetworkInterfaceDetails();
	private String LAN_IP = netDetails.getLocalIP();
	private String publicIP = netDetails.getPublilcIP();
	private String serverHostName = netDetails.getHostName();
	
	/**************Get the default and the random port numbers************/
	private static final int DEFAULT_PORT = netDetails.getDefaultPort();
	private static int randomPort = netDetails.getRandomPort();
	
	/********Create a serverHandler object to assist the main server class with some of its methods***/
	ServerHandler serverHandler = new ServerHandler();
	private final String SERVER_PREFIX ="SERVER ";
	
	/***Define a HashMap Data structure to store the public keys from different clients as key-value pairs
	 * HashMap<Username,PublicKey>
	 ****/
	public HashMap<String,String>key_store = new HashMap<String,String>();
	
	/*Define an Array List to store the server logs before being transfered to the log file*/
	protected ArrayList<String>serverLogs = new ArrayList<String>();
	
	/*Define a getter for randomPort for other classes to use to make reference 
	 * to the chosen random port from this class*/
	
	public int returnRandomPort()
	{
	   return randomPort;
	}
	
	/*********Constructor to create a server object with a port specifed to it***/
	public Server(int port)
	{
		this.port = port;
		clients = new ArrayList<ClientHandler>();
		messageCache = new ArrayList<String>();
		
		clientNumber = 1;
	}
	
	/*A default constructor to initialize a server socket witht the default port*/
	public Server()
	{
		this(DEFAULT_PORT);
	}
	public ArrayList<String>getServerLogs()
	{
		return serverLogs;
	}
	
	public void startServer()
	{
		serverHandler.clearOutput();
		/**********************Begin server and start logging server information************/
		serverLogs.add("==================="+"[SERVER ADMIN] Started Server at exactly"+serverHandler.getServerLaunchDate()+"==========================");
		
		System.out.println(SERVER_PREFIX+serverHandler.getTimestamp()+"Server Has Started");
		serverLogs.add(SERVER_PREFIX+serverHandler.getTimestamp()+"Server Has Started");		
		
		System.out.println(SERVER_PREFIX+serverHandler.getTimestamp()+"Server listening on LAN IP Address: "+LAN_IP);
		serverLogs.add(SERVER_PREFIX+serverHandler.getTimestamp()+"Server listening on LAN IP Address: "+LAN_IP);
		
		System.out.println(SERVER_PREFIX+serverHandler.getTimestamp()+"Server's Public IP address: "+publicIP);
		serverLogs.add(SERVER_PREFIX+serverHandler.getTimestamp()+"Server's Public IP address: "+publicIP);
		
		System.out.println(SERVER_PREFIX+serverHandler.getTimestamp()+"Server running on TCP PORT: "+port);
		serverLogs.add(SERVER_PREFIX+serverHandler.getTimestamp()+"Server running on TCP PORT: "+port);
		
		System.out.println(SERVER_PREFIX+serverHandler.getTimestamp()+"Server's Host Name: "+serverHostName);
		serverLogs.add(SERVER_PREFIX+serverHandler.getTimestamp()+"Server's Host Name: "+serverHostName);
		
		serverLogs.add(SERVER_PREFIX+serverHandler.getTimestamp()+"Maximum Client Capacity: "+MAX_CLIENTS+" clients");
		
		LogManagement logManagement = new LogManagement();
				
		for(String log: serverLogs)
		{
			logManagement.writeToFile(logManagement.logFileName, log);
		}
		
		/*********Creating a thread to listens to clients' broadcast packets***********/
		Thread receiveClientsBroadcasts = new Thread()
		{
			@Override
			public void run()
			{
				DiscoveryServer discoveryServer = new DiscoveryServer();
				discoveryServer.run();
			}
		};
		
		receiveClientsBroadcasts.start();
		
		ServerSocket listener = null;
		try
		{
			listener = new ServerSocket(port);
			
			while (true)
				if (clients.size() < MAX_CLIENTS)
				{
					clients.add(new ClientHandler(clientNumber++, listener.accept()));
					clients.get(clients.size() - 1).start();
				}
				else
				{
					Socket socket = listener.accept();
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println("Maximum number of clients reached.");
					logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+serverHandler.getTimestamp()+"Maximum number of clients reached.");
				}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			broadcastMsg("shutdown");
			try
			{
				listener.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public class ClientHandler extends Thread 
	{
		public int clientNumber;
		public String username = "Anonymous";
		private BufferedReader fromClientStream;
		private PrintWriter toClientStream;
		private Socket socket;
		public String clientPublicKey;
		public String clientIpAddress;
		public int clientPortNumber;
		
		LogManagement logManagement = new LogManagement();
		
		public ClientHandler(int clientNumber, Socket socket)
		{
			this.clientNumber = clientNumber;
			this.socket = socket;
		}
		public String getUsername()
		{
			return username;
		}
		public String getPublicKey()
		{
			return clientPublicKey;
		}
		
		@Override
		public void run()
		
		{
			try
			{
				/**************************Initialize the stream buffers**********************************/
				fromClientStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				toClientStream = new PrintWriter(socket.getOutputStream(), true);
				
				/***************Extract the remote client's IP address on successful connection*****************/
				/********The remote socket address has 2 turple parameters(IP address, Port Number)*************/
				InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
				clientIpAddress = socketAddress.getAddress().getHostAddress();
				
				clientPortNumber = socketAddress.getPort();
				
				// get the input stream from the connected socket
		        InputStream inputStream = socket.getInputStream();
		        // create a DataInputStream so we can read data from it.
		        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

				// read the list of public keys from the socket
		        List<SendKey> key_list = null;
		        
				try 
				{
					key_list = (List<SendKey>) objectInputStream.readObject();
				} catch (ClassNotFoundException e) 
				{
					e.printStackTrace();
				}
				
				for(SendKey key: key_list)
				{
				  clientPublicKey = key.getText();
				}
				
				while (true)
				{
					directMsg("Please enter a valid username: ");
					String tempUsername = fromClientStream.readLine();
					directMsg("Please Enter Your Password: ");
					String clientPassword = fromClientStream.readLine();
					
				    //check whether the username is already online, if so, shut this login attempt
					for(int i=0; i< serverHandler.getOnlineUsers(clients).length(); i++)
					{
						if(serverHandler.getOnlineUsers(clients).contains(tempUsername))
						{
							System.out.println("Username Already Logged In. Try Again!");
							close(true);
							serverHandler.clearOutput();
							break;
						}
					}
					
					logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+serverHandler.getTimestamp()+tempUsername+" attempted to join the server!");
					
					if (serverHandler.usernameIsValid(tempUsername,clients) && tempUsername.length() > 0 && logManagement.clientAuth(tempUsername,clientPassword))
					{  
							directMsg("Username approved. Welcome.");
							
							serverHandler.log(tempUsername + " has connected to the server as client Number: " + clientNumber );
							serverHandler.log(tempUsername+ " Connected Successfuly via IP ADDRESS: "+clientIpAddress+" and TCP PORT NUMBER: "+clientPortNumber);
							logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+serverHandler.getTimestamp()+tempUsername + " has connected to the server as client Number: " + clientNumber);
							logManagement.writeToFile(logManagement.logFileName, SERVER_PREFIX+serverHandler.getTimestamp()+tempUsername+" Connected Successfully with IP ADDRESS: "+clientIpAddress+" and TCP PORT NUMBER: "+clientPortNumber);
							username = tempUsername;
							
							break;	
					}
					else
					{   
						directMsg("You Have Not Set Up Your Account.");	
						String setUsername = fromClientStream.readLine();
						String setPassword = fromClientStream.readLine();
						if(!logManagement.nameExistInFile(setUsername, logManagement.clientHashedPasswords))
						{
							logManagement.writeToFile(logManagement.clientHashedPasswords, setUsername+" "+setPassword);
							directMsg("Username approved. Welcome.");
							serverHandler.log(setUsername+ " Connected Successfuly via IP ADDRESS: "+clientIpAddress+" and TCP PORT NUMBER: "+clientPortNumber);
							logManagement.writeToFile(logManagement.logFileName, SERVER_PREFIX+serverHandler.getTimestamp()+setUsername+" Connected Successfully with IP ADDRESS: "+clientIpAddress+" and TCP PORT NUMBER: "+clientPortNumber);
							username = setUsername;
							break;
						}
						directMsg("Name Exists! Try Again");
					}
					
				}
				
				directMsg("logging public key and username details..");
				
				//storing the public key to the hashmap
				key_store.put(username, clientPublicKey);
				logManagement.writeToFile(logManagement.logFileName, SERVER_PREFIX+serverHandler.getTimestamp()+username+" ["+clientIpAddress+"]"+" Public Key is: "+clientPublicKey);
				
				//store the public key to a public key log file
				logManagement.writeToFile(logManagement.publicKeyLog,SERVER_PREFIX+serverHandler.getTimestamp()+username+" ["+clientIpAddress+"]"+"\t"+clientPublicKey);
				
				// Send cached messages
				for (String msg : messageCache)
					directMsg(msg);
				
				directMsg("-- End of Message History --");
				
				// Notify group of join
				broadcastMsg(username + " has joined the server.");
				
				// Send the list of online users
				directMsg(serverHandler.getOnlineUsers(clients));
				
				// Wait for messages from client
				while (true)
				{
					// Get the message from the client
					String msg = fromClientStream.readLine();
					
					if (msg == null || msg.equals(""))
						continue;
					
					String command = serverHandler.extractMessageSegments(msg, 0)[3];
					
					if (!command.equals(""))
					{
						if (command.equals("/exit"))
						{
							close(true);
							break;
						}
						else if(command.equals("/whoami"))
						{
							directMsg("Your Username: "+username);
						}
						else if(command.equals("/publicKey"))
						{
							RSA rsa = new RSA();
							directMsg("Your Public Key: "+rsa.revertEncodedPublicKey(key_store.get(username)));
							logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+serverHandler.getTimestamp()+username+" requested for public key");
						}
						else if(command.equals("/printUsersPublicKeys"))
						{
							for(String key: key_store.keySet())
							  {
								  directMsg(key+"\t\t"+key_store.get(key)+"\n");
							  }
							logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+serverHandler.getTimestamp()+username+" User Tried to view all public keys of connected clients!");
						}
						else if(command.equals("/printActiveUsers"))
						{
							directMsg(printActiveUsers());
						}
						else if (command.equals("/privateMessage"))
						{
							// Extract segments with one expected argument
                             String[] segments = serverHandler.extractMessageSegments(msg,1);
							if(segments.length == 1)
							{
								if(segments[0].equals("Too few arguments") || segments[0].equals("No command found"))
								 {
									directMsg("\nUsage: /pmsg <user> <message>");
								 }
								else
								{
									directMsg("\nUnknown error extracting message segments.");
								}
								continue;
							}
							
							ClientHandler c = serverHandler.getClient(segments[4],clients);
							
							if(c != null)
							{
								RSA rsa = new RSA();
								String pmsg = segments[0]+"Secret Message From "+getUsername()+": "+ segments[2];
								
								c.directMsg("Private");
								
								c.directMsg(rsa.encrypt(pmsg,rsa.revertEncodedPublicKey(key_store.get(segments[4]))));
								
								directMsg(pmsg);
							}
							else
								serverMsg("\"" + segments[4] + "\" is not online.");
							
						}
						else if (command.equals("/users"))
							// Return the list of users online
							directMsg(serverHandler.getOnlineUsers(clients));
						else if (command.equals("/help"))
							// Prints all possible commands available
							directMsg(HELP_TEXT);
						
						else
							directMsg("\tInvalid command: \"" + command + "\"\n" + HELP_TEXT);
					}
					else
					{
							broadcastMsg(msg);
							messageCache.add(msg);
						
						if (messageCache.size() > MAX_CACHE_SIZE)
							messageCache.remove(0);
					}
				}
			}
			catch (IOException e)
			{
				serverHandler.log("Client Number " + clientNumber + " Connection is reset!" + e);
				logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+serverHandler.getTimestamp()+username+"Client Number: ["+clientNumber+"]"+" Connection is reset!");
				
				if (clients.contains(this))
					close(true);
				else
					close(false);
			} catch (InvalidKeyException e) {
				System.out.println(e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				System.out.println(e.getMessage());
			} catch (NoSuchPaddingException e) {
				System.out.println(e.getMessage());
			} catch (IllegalBlockSizeException e) {
				System.out.println(e.getMessage());
			} catch (BadPaddingException e) {
				System.out.println(e.getMessage());
			} catch (InvalidKeySpecException e) {
				System.out.println(e.getMessage());
			} 
		}
		public void directMsg(String msg)
		{
			toClientStream.println(msg);
		}
		private void serverMsg(String msg)
		{
			directMsg(serverHandler.getTimestamp()+ " <# server #> " + msg);
		}
		private void close(boolean notify)
		{
			try
			{
				clients.remove(this);
				key_store.remove(username);
				fromClientStream.close();
				toClientStream.close();
				socket.close();
				serverHandler.log(username + " has left the server.");
				logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+serverHandler.getTimestamp()+username+"Closed connection with server");
				
				if (notify)
					broadcastMsg(username + " has left the server.");
			}
			catch (Exception e)
			{
				serverHandler.log("Error closing socket #" + clientNumber + ": " + e);
				logManagement.writeToFile(logManagement.logFileName,SERVER_PREFIX+username+"Error Closing socket for clientNo "+clientNumber);
			}
		}
	}
	private void broadcastMsg(String msg)
	{
		for (ClientHandler client : clients)
			try
			{
				client.directMsg(msg);
			}
			catch (Exception e)
			{
				System.out.println("Error sending message \"" + msg + "\": " + e.getMessage());
			}
	}
	public String printActiveUsers()
	{
		LogManagement logManagement = new LogManagement();
		
		//declare a list of current online users
		ArrayList<String>current_online_users = new ArrayList<String>();
		
		//declare a list to hold the total users
		ArrayList<String>registered_users = new ArrayList<String>();
		
		//first get the total number of online users
		for(ClientHandler client: clients)
		{
	       current_online_users.add(client.getUsername());
		}
		
		//get a list of total registered clients from the file
		File fileObj = new File(logManagement.clientHashedPasswords);
		Scanner myReader = null;
		
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
			
			//add the usernames to the list of total users
			
			registered_users.add(clientUsername);
		}
		/*perform the steps to get the list of online and offline clients using O(N) time complexity*/
		int numberOfRegisteredUsers = registered_users.size();
		int numberOfOnlineUsers = current_online_users.size();
		int numberOfOfflineUsers = numberOfRegisteredUsers-numberOfOnlineUsers;
		
		String userStats = "";
		
		userStats+="=================ALL ONLINE USERS ("+numberOfOnlineUsers+") Users==================\n";
		
		for(String online_user: current_online_users)
		{
		   userStats+=online_user+"[ONLINE] \n";
		}
		userStats+="=================ALL OFFLINE USERS ("+numberOfOfflineUsers+") Users==================\n";
		for(String registered_user: registered_users)
		{
			if(linearSearch(current_online_users, registered_user)) //Binary search not used for obvious reasons
			{
				continue;
			}
			else
			{
				userStats+=registered_user+" [OFFLINE] \n";
			}
		}
		return userStats;
	}
	public boolean linearSearch(ArrayList<String>incomingArray, String searchElement)
	{
		boolean isFound = false;
		for(int i=0; i<incomingArray.size(); i++)
		{
			if(incomingArray.get(i).equals(searchElement))
			{
				isFound = true;
				break;
			}
		}
		return isFound;
	}
	private static void authenticateServerAdmin(int portConfigSelection)
	{  
		ServerHandler serverHandler = new ServerHandler();
		LogManagement logManagement = new LogManagement();
		EncryptLogs maskPassword = new EncryptLogs();
		Scanner pass = new Scanner(System.in);
		Console cons = System.console();
		
		logManagement.writeToPassFile(logManagement.serverPasswordFile);
		
		System.out.println("Enter the Server Admin Username: ");
		String username = pass.nextLine();
		
		String password = maskPassword.getPasswordMasked(cons,"Enter the server password: ");
		
		if(!logManagement.serverAdminAuth(username, password))
		{
		    System.out.println("Server Authentication Failed! Try Again.");
		    System.exit(0);
		}
		//server auth success, start the server with the default port number
		System.out.println("Server Authentication Success!");
		logManagement.writeToFile(logManagement.logFileName, "SERVER"+serverHandler.getTimestamp()+" Server Admin started server using Default port number");
		
		if(portConfigSelection ==1)
		{
			new Server().startServer();
		}
		else if(portConfigSelection ==2)
		{
			new Server(randomPort).startServer();
		}
		
		pass.close();
	}
	public static void main(String[] args)
	{
		LogManagement logManagement = new LogManagement();
		ServerHandler serverHandler = new ServerHandler();
		
		System.out.println("CHOOSE THE TYPE OF SERVER PORT CONFIGURATION BELOW:\n1. DEFAULT TCP PORT[7070]\n"
				+ "2. RANDOM PORT[5000-10000] (Recommended)");
		
		int max_trials = 1;
		while(true)
		{
			try
			{
				Scanner input = new Scanner(System.in);
				int userInput = input.nextInt();
				//make a decision for the default port selection
				if(userInput == 1)
				{   
					authenticateServerAdmin(userInput);
					break;
				}
				//make a decision for the random port selection
				else if(userInput == 2)
				{   
                    authenticateServerAdmin(userInput); 
					break;
				}
				else
				{
					max_trials++;
					
					if(max_trials > 3)
					{
						System.out.println("Server shutting down! Try again");
						logManagement.writeToFile(logManagement.logFileName, "SERVER"+serverHandler.getTimestamp()+" Server Admin Exceeded Maximum Limit for starting the server!");
						System.exit(0);
					}	
					else
						System.out.println("Please enter 1 or 2! You have "+(4-max_trials)+" trials remaining");
						continue;
				}
			}
			catch(Exception InputMismatchException)
			{
				System.out.println("Enter a digit(1 or 2 for default port and random port respectively)");
				max_trials ++;
			}	
			if(max_trials > 3) 
				System.exit(0);
				
		}
	}
}