package client_module;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class ClientDiscoveryService implements Callable<String> 
{
	private static final int MAX_PACKET_SIZE = 2048;  //maximum packet size 2bytes
	private static final int TIMEOUT = 2000;   //2 seconds timeout for each reply
	private static int DISCOVERY_PORT =8888; //synchronized with client's discovery service
	private static String  DISCOVERY_REQUEST ="Where-is-LEO_SERVER?";  //the service the client requests
	private static String SERVER_NAME ="LEO_SERVER ";   //name of the discovery server
   
   // creates a UDP socket object and returns it!
   DatagramSocket createSocket() 
   {
		DatagramSocket socket = null;
		try 
		{
			socket = new DatagramSocket();
			socket.setBroadcast(true);
			socket.setSoTimeout(TIMEOUT);
		} catch (SocketException e) 
		{
			System.out.println("Error creating UDP Broadcast socket..."+e.getMessage());
			throw new RuntimeException(e);
		}
		return socket;
	}
   
	public String call() 
	{
		// Packet for receiving response from server
		byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveBuffer,receiveBuffer.length);

		DatagramSocket socket = createSocket();
		
		byte[] packetData = DISCOVERY_REQUEST.getBytes();
		
		InetAddress broadcastAddress = null;
		try 
		{
			broadcastAddress = InetAddress.getByName("255.255.255.255");
			
		} catch (UnknownHostException e) 
		{
			System.out.println(e.getMessage());
		}
		
		int servicePort = DISCOVERY_PORT;
		DatagramPacket packet = new DatagramPacket(packetData,packetData.length, broadcastAddress, servicePort);
		
		// use a loop so we can resend broadcast after timeout
		String result = "";
		while(true) 
		{
			try 
			{
				System.out.println("Performing a network sevice discovery to identify the server...");
				
				try 
				{
					Thread.sleep(900);
				} catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
				
				socket.send(packet);
				
				System.out.println("Sent discovery packet to "+broadcastAddress.getHostAddress()+":"+servicePort);

				// wait for reply
				socket.receive(receivePacket);
				
				System.out.println("Successfully received reply from "+receivePacket.getAddress().getHostAddress());
				
				String reply = new String(receivePacket.getData());
				
				String [] serverDetails = reply.split("\\s");
				
				String serverName = serverDetails[0];
				
				System.out.println("Server Name: "+serverName);
				
				int k = reply.indexOf(SERVER_NAME);
				if (k<0)
				{
					System.out.println("Reply does not contain prefix "+SERVER_NAME);
					break;
				}
				k += SERVER_NAME.length();
				result = reply.substring(k).trim();
				
				break;
			}
			catch(SocketTimeoutException e) 
			{
				System.out.println(e.getMessage());
			}
			catch (IOException e)
			{
				System.out.println(e.getMessage());
				break;
			}
		}
		if (socket != null)
			socket.close();
		
		return result;
	}
}