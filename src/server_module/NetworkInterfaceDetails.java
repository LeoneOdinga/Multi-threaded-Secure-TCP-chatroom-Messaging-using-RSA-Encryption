package server_module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;

import java.util.List;

public class NetworkInterfaceDetails 
{	
//	handling the port Numbers, either accept default port or choose a random port number, 
//	default port has been set to 7070
	
	private int defaultPort = 7070;
	private int randomPort =0;
	
	//a getter method for accessing the private attribute(defaultPort)
	public int getDefaultPort()
	{
		return defaultPort;
	}
	
	//Return random port numbers between 5000-10000
	public int getRandomPort(int lowerBound, int upperBound)
	{
		SecureRandom random = new SecureRandom();
		return random.nextInt(upperBound - lowerBound) + lowerBound;
	}
	
	public int getRandomPort()
	{
		randomPort = getRandomPort(5000,10000);
		return randomPort;
	}
	
	/*FUNCTION TO RETURN THE CANONICAL HOSTNAME OF THE HOST MACHINE*/
	public String getHostName()
	{
		InetAddress hostname = null;
		try
		{
		     hostname = InetAddress.getLocalHost();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return hostname.getCanonicalHostName();
	}
	
	/*FUNCTION TO RETURN THE LOCAL IP ADDRESS OF THE HOST MACHINE*/
	public String getLocalIP()
	{
		InetAddress localIp = null;
		try
		{
		    localIp = InetAddress.getLocalHost();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return localIp.getHostAddress();
	}
	
	/*FUNCTION TO RETURN THE HOST's PUBLIC IP ADDRESS /globally routable in the internet
	 * The host can be an end device or a server
	 * */
	public String getPublilcIP()
	{
		URL url = null;
		try
		{
			url = new URL("http://checkip.amazonaws.com");
		}
		catch(Exception e)
		{
			System.out.println("Error resolving the public IP address!");
		}
		BufferedReader bufferedReader = null;
		try 
		{
			bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
			return bufferedReader.readLine();
		}
		catch(Exception e)
		{
			System.out.println("Error resolving the public IP address!");
		}
		finally
		{
			if(bufferedReader != null)
			{
				try
				{
					bufferedReader.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	

	public static List<InetAddress> getAllAddresses() 
	{
		List<InetAddress> addrlist = new ArrayList<InetAddress>();
	    try 
	    {
		    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) 
			{
				NetworkInterface iface = interfaces.nextElement();
				// filters out 127.0.0.1 and inactive interfaces
				if (iface.isLoopback() || !iface.isUp()) continue;

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while(addresses.hasMoreElements()) 
				{
					InetAddress ipaddr = addresses.nextElement();
					addrlist.add( ipaddr );
				}
			}
		} catch (SocketException e)
	    {
			throw new RuntimeException(e);
		}
		return addrlist;
	}
	public static String getMacAddress(InetAddress ip)
	{
		String address = null;
		try
		{
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();
			
			StringBuilder sb = new StringBuilder();
			
			for(int i=0; i<mac.length; i++)
			{
				sb.append(String.format("%02X%s", mac[i], (i<mac.length)? "-": " "));
			}
			address = sb.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return address;
	}
}
