package client_module;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.*;
import java.time.format.*;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import encryption_decryption_module.RSA;
import encryption_decryption_module.SendKey;

public abstract class Client 
{
	protected String serverIPAddress;          //defines the server's IP address
	protected int serverPort;                  //defines the port to which the server listens for incoming connections
	protected String username ="Anonymous";    //client specifies their username on connecting to the server
	protected String password;                 //client specifies the password for authentication
	protected Socket socket;                   //creating a socket object to the client end
	
	protected BufferedReader fromServerStream; //stream to receive input from the server
	protected PrintWriter toServerStream;      //stream to write to the server
	
	PrivateKey privateKey;                     //defines the clients private key(Not shared with the server)
    PublicKey publicKey;                       //defines the clients public key(sent to a server)
    
    private String encodedPrivateKey = null;   //private member variable for privateKey(encoded format)
    private String encodedPublicKey = null;    //private member variable for  public Key(encoded format)
    
    
	/*******************Default constructor for a client object****************/
	public Client()
	{
		this("localhost", "7070", "");
	}
	
    /****************Main constructor for a client object************************/
	public Client(String serverIPAddress, String port, String username)
	{
		this.serverIPAddress = serverIPAddress;
		this.serverPort = Integer.parseInt(port);
		this.username = username;
	}
	/*********************Getters and setters for the Client Class****************/
	protected PrivateKey getPrivateKey()
	{
		return privateKey;
	}
	private void setEncodedPrivateKey(String encodedPrivateKey)
	{
		this.encodedPrivateKey = encodedPrivateKey;
	}
	protected String getEncodedPrivateKey()
	{
		return encodedPrivateKey;
	}
	private void setEncodedPublicKey(String encodedPublicKey)
	{
		this.encodedPublicKey = encodedPublicKey;
	}
	public String getEncodedPublicKey()
	{
		return this.encodedPublicKey;
	}
	
	/***************Method to connect to the server socket**********************/
	public void connectToServer()
	{
		try
		{
			//instantiate a new socket object
			this.socket = new Socket();
			
			//define an end-point for the socket object to connect to(Basically the server's end-point)
			InetSocketAddress endpoint = new InetSocketAddress(this.serverIPAddress,this.serverPort);
			
			//connect to the end-point allowing a timeout of 10 seconds maximum
			this.socket.connect(endpoint, 10000);
            
			//initialize data streams to convey data to and from the server
			this.fromServerStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.toServerStream = new PrintWriter(this.socket.getOutputStream(), true);
			
			
			/**************GENERATE PRIVATE AND PUBLIC KEY PAIRS AND SEND PUBLIC KEY TO SERVER********/
			
			/**************Generate private and public keys*****************/
		    
		    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair pair = generator.generateKeyPair();
			privateKey = pair.getPrivate();
			publicKey = pair.getPublic();
		    
			//instantiate a new RSA object with the generated key pairs for each client 
	        RSA rsa = new RSA(privateKey,publicKey);
	        
	        publicKey= rsa.getPublicKey(); //real  public key without encoding
	        privateKey  = rsa.getPrivateKey(); //real private key without encoding
	        
	        /*****************************END OF KEY GENERATION*****************************************/
	        
	        //Get String Encoding equivalent of public and private keys
	        String publicKeyEncoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
	        setEncodedPublicKey(publicKeyEncoded);
	     
	        String privKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
	        setEncodedPrivateKey(privKey);

	        // get the output stream from the socket.
	        OutputStream outputStream = socket.getOutputStream();
	        // create an object output stream from the output stream so we can send an object through it
	        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

	        // send the public key to the server in string encoded format
	        List<SendKey> key = new ArrayList<>();
	        key.add(new SendKey(publicKeyEncoded));
	        
	        objectOutputStream.writeObject(key);
	        
	        //**************END OF KEY GENERATION AND TRANSMISSION OF PUBLIC KEY TO SERVER***********//
	        
	        
	        //*******************************BEGIN CLIENT SEGMENT***********************************//
			while (true)
			{   
				//read output stream generated from the server
				String response = this.fromServerStream.readLine();
                
				//if the server cannot hold more clients, close this client's connection
				if (response.equals("Maximum number of clients reached."))
				{
					alertMessage("Maximum number of clients reached. Please try again later.");
					//TO DO: put this close in a function and pass the args
					this.fromServerStream.close();
					this.toServerStream.close();
					this.socket.close();
					System.exit(0);
				}
				
				if(username == null)
					System.exit(0);
				
				if (this.username.equals(""))
					this.username = getInput(response);

				// Send username to server
				this.toServerStream.println(this.username);
				
				response = this.fromServerStream.readLine();
				// prompt for password
				Console cons = System.console();
				password = getPasswordMasked(cons,response);
				
				//send password to the server
				this.toServerStream.println(this.hashPassword(password));
				
				response = this.fromServerStream.readLine();
                
				//Username accepted to be okay
				if (response.equals("Username approved. Welcome."))
				{
					Thread.sleep(3000);
					clearOutput();
					
					printToConsole(response);
					printToConsole("");
                    
					//run a loop to print all the cached messages from the server's ArrayList of past messages(Broadcast messages)
					while (!response.equals("-- End of Message History --"))
					{
						response = this.fromServerStream.readLine();
						printToConsole(response);
					}
					printToConsole("");
			        
					break;
				}
			    else if(response.equals("You Have Not Set Up Your Account."))
			    {
			    	System.out.println("Record Not Found.Setting up your account: ");
			    	System.out.println("Do you want to set up a new account(y/n)? ");
			    	Scanner sc = new Scanner(System.in);
			    	String opt = sc.nextLine();
			    	if(opt.startsWith("y") || opt.startsWith("Y"))
			    	{
			    		String setUsername = getInput("Enter your Username: ");
				    	String setPassword = getInput("Enter Your Password: ");
				    	this.toServerStream.println(setUsername);
				    	this.toServerStream.println(hashPassword(setPassword));
			    	}
			    	else
			    	{
			    		System.exit(0);
			    	}
			    	
			    	if(this.fromServerStream.readLine().equals("Name Exists! Try Again"))
			    	{
			    		System.out.println("Username Exists. Try Again!");
			    		System.exit(0);	
			    	}
			    	break;
			    }
				else
				{
					alertMessage(response);
					this.username = getInput("Please enter a valid username: ");
					if (this.username == null || this.username.length() == 0)
						System.exit(0);
				}
		        
			}
			
			//CREATE A DOWNLINK THREAD. COMMUNICATION FROM THE SERVER TO EACH CLIENT THREAD
			Thread downlink = new Thread() 
			{
				@Override
				public void run()
				{
					try
					{
						receiveServerResponse();
					}
					catch (Exception e)
					{
						alertMessage("Error Fetching response from Server!");
					}
				}
			};
			
			downlink.start();
			
		   //CREATE AN UPLINK THREAD FOR CLIENTS TO SEND REQUESTS TO THE SERVER
			
			Thread uplink = new Thread() 
			{
				@Override
				public void run()
				{
					try
					{
						sendClientRequests();
					}
					catch (Exception e)
					{
						alertMessage("Error sending request to the server!");
					}
				}
			};
			
			uplink.start();
		}
		catch (Exception e)
		{
			handleConnectionExceptions(e);
		}
	}
	
	//send message to server with timestamp
	public void sendMessage(String msg)
	{
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
		LocalDateTime now = LocalDateTime.now();

		this.toServerStream.println("[" + dtf.format(now) + "] <" + username + "> " + msg);
	}
	
	//wait for server's responses
	public void receiveServerResponse() throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
	{
		//run a loop to continuously wait for server's responses
		while (true)
		{
			String incoming = this.fromServerStream.readLine();
			String encryptedText = null;
			String decryptedText = null;
			
			if (incoming == null)
			{
				clearOutput();
				System.exit(0);
				break;
			}
			else if(incoming.equals("Private"))
			{
				try
				{
					 encryptedText = this.fromServerStream.readLine();
					 
					 RSA rsa = new RSA();
					 
					 decryptedText = rsa.decrypt(encryptedText, privateKey);
					 
					 printToConsole("\nYou have received an encrypted private message from one of the clients");
					 
					 printToConsole("\nEncrypted Text:"); printCipherText(encryptedText);
					 
					 printToConsole("\nYour Encoded Private Key");printEncodedKeys(getEncodedPrivateKey(),"PRIVATE");
					 
					 printToConsole("\nDecryption Success! Ciphertext automatically decrypted using your private key!\n");
					 
					 printToConsole("See The Decrypted Messsage Below\n");
					 
					 printToConsole(decryptedText);
					 
				}
				catch(Exception e)
				{
					printToConsole("Error Decrypting Message."); //sloppy
				}
			}
			else
			{
				printToConsole(incoming);
			}
					
		}
	}
	private void handleConnectionExceptions(Exception e)
	{
		String message = "";
		if (e.getMessage().indexOf("refused") != -1)
			message = "Connection refused.Try Again!";
		
		else if (e.getMessage().indexOf("reset") != -1)
			message = "Connection reset.Connection closed by the server!";
		else if (e.getMessage().indexOf("timed") != -1)
			message = "Connection timed out. Connect to the correct server port if you disabled automatic discovery!";
		else
			message = "Unhandled error:\n" + e.getMessage();

		alertMessage(message);
	}
	public void printEncodedKeys(String keyString, String keyType)
	{
		System.out.println("============================START OF "+keyType+" KEY============================== ");
		for(int i=0; i<keyString.length(); i++)
		{
			if(i%75 == 0)
				System.out.println();
			System.out.print(keyString.charAt(i));
			
		}
		System.out.println("\n============================END OF "+keyType+" KEY==============================");
	}
	public void printCipherText(String cipherText)
	{
		System.out.println("==========================START OF CIPHERTEXT===============================");
		for(int i=0; i<cipherText.length(); i++)
		{
			if(i%75 == 0)
				System.out.println();
			System.out.print(cipherText.charAt(i));
		}
		System.out.println("\n\n=========================END OF CIPHERTEXT==================================");
	}
	private String hashPassword(String password)
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
	
	/*SET OF ABSTRACT METHODS DEFINED BY THE DIFFERENT VERSIONS OF CLIENT SUB-CLASSES
	 * */

	abstract void sendClientRequests();

	abstract String getInput(String prompt);

	abstract void printToConsole(String msg);
	
	abstract void alertMessage(String alert);
  
	abstract void clearOutput();
    
	/*******************Start the execution of the client program*************************/
	
	public static void main(String[] args) throws Exception
	{
				Scanner clientInput = new Scanner(System.in);
				int userChoice = 0;
				
			    while(true)
			    {
			    	try
					{
				       //Connect to the server using either automatic network discovery or manual configuration
						System.out.println("Please Enter your preferred method for connecting to the server:");
						System.out.println("\n1.Automatic Server Discovery(Recommended). \n2.Manual Connection.");
						
						userChoice = clientInput.nextInt();
						
						if(userChoice == 1)
						{
							ClientDiscoveryService discoveryClient = new ClientDiscoveryService();
							String serverReply = discoveryClient.call();
							String [] serverDetails = serverReply.split("\\s");
							
							String serverIP = serverDetails[0];
							String serverPort = serverDetails[2];
							
							new TerminalClient(serverIP, serverPort);
							break;
						}
						else
						{   clientInput.nextLine();
							System.out.print("Enter the server's IP address: ");
							String serverIP = clientInput.nextLine();
							System.out.print("Enter the server's port number: ");
							String serverPort = clientInput.nextLine();	
							
							new TerminalClient(serverIP,serverPort);
							break;
						}
					}
					catch(InputMismatchException e)
					{
						System.out.println("Please enter a digit(1 or any other digit: ");
					}	
			    }
	}
}