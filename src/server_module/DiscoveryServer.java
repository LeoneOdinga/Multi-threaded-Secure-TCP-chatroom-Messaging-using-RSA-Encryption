package server_module;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryServer implements Runnable 
{
	LogManagement logManagement = new LogManagement();
	
	// how much data to accept from a broadcast client.
	private static final int MAX_PACKET_SIZE = 2048;
	private DatagramSocket socket;
	
	private static int DISCOVERY_PORT =8888;
	private static String  DISCOVERY_REQUEST ="Where-is-LEO_SERVER?";
	private static String SERVER_NAME ="LEO_SERVER ";
	
	public static void main(String[] args) 
	{
		DiscoveryServer server = new DiscoveryServer();
		server.run();
	}
	
	@Override
	public void run() 
	{
		Server server = new Server();
		ServerHandler serverHandler = new ServerHandler();
		
	    final String DISCOVERY_SERVER_PREFIX ="DISCOVERY SERVER";
		
		// quit if we get this many consecutive receive errors.
		// reset the counter after successfully receiving a packet.
		final int max_errors = 5;
		int errorCount = 0;
		
		NetworkInterfaceDetails netDetails = new NetworkInterfaceDetails();
		
		final String MY_IP = netDetails.getLocalIP();
		System.out.println(DISCOVERY_SERVER_PREFIX+" using "+MY_IP+" for receiving UDP broadcast packets...");
		logManagement.writeToFile(logManagement.logFileName, DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" using "+MY_IP+" for receiving UDP broadcast packets...");
		
		// Keep a socket open to listen to all UDP trafic that is
		// destined for this port.
		try 
		{
			InetAddress addr = InetAddress.getByName( "0.0.0.0" );
			socket = new DatagramSocket(DISCOVERY_PORT, addr);
			socket.setBroadcast(true);
			
		} catch (Exception e) 
		{
			System.out.println(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Could not create UDP socket on port " + DISCOVERY_PORT);
			server.serverLogs.add(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Could not create UDP socket on port " + DISCOVERY_PORT);
			return;
		}
			
		System.out.printf(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" listening on UDP port %d\n", DISCOVERY_PORT);
		logManagement.writeToFile(logManagement.logFileName,DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" listening on UDP port: "+ DISCOVERY_PORT);
			
		while (true)
		{
			// Receive a packet
			byte[] recvBuf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
			try 
			{
				// wait for a packet
				socket.receive(packet);
			} catch (IOException e) 
			{
				System.out.println(e.getMessage());
				// this is to avoid infinite loops when exception is raised.
				errorCount++;
				if (errorCount >= max_errors) return;
				// try again
				continue;
			}
			
			// Packet received
			errorCount = 0;    // reset error counter 
			InetAddress clientAddress = packet.getAddress();
			int clientPort = packet.getPort();
			
			System.out.println(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Packet recieved from "+clientAddress.getHostAddress()+":"+clientPort);
			logManagement.writeToFile(logManagement.logFileName,DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Packet recieved from "+clientAddress.getHostAddress()+":"+clientPort);
			
			System.out.println(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Received data: "+new String(packet.getData()));
			logManagement.writeToFile(logManagement.logFileName,DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Received data: "+new String(packet.getData()));
	
			// See if the packet holds the correct signature string
			String message = new String(packet.getData()).trim();
			if (message.startsWith(DISCOVERY_REQUEST)) 
			{
				
				String reply =  SERVER_NAME + MY_IP+" PORT "+server.returnRandomPort();
				byte[] sendData = reply.getBytes();

				// Send the response
				DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length, clientAddress, clientPort);
				try 
				{
					socket.send(sendPacket);
					System.out.println(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Reply sent to "+clientAddress.getHostAddress()+":"+clientPort);
					logManagement.writeToFile(logManagement.logFileName,DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Reply sent to "+clientAddress.getHostAddress()+":"+clientPort);
					
				} catch(IOException e)
				{
					System.out.println(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Error sending service reply..."+e.getMessage());
					logManagement.writeToFile(logManagement.logFileName,DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Error sending service reply..."+e.getMessage());
				}
			}
			else 
			{
				System.out.println(DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Packet from "+clientAddress.getHostAddress()+":"+clientPort+"is not a discovery packet");
				logManagement.writeToFile(logManagement.logFileName,DISCOVERY_SERVER_PREFIX+serverHandler.getTimestamp()+" Packet from "+clientAddress.getHostAddress()+":"+clientPort+"is not a discovery packet");
				
			}
		}
	}	
}