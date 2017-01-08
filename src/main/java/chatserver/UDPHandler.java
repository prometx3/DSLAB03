package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UDPHandler implements Runnable {

	private DatagramSocket socket;
	private DatagramPacket pack; 
	private ServerDispatcher dispatcher;
	
	private final int sendBufferSize = 1024;
	
	public UDPHandler(DatagramSocket socket, DatagramPacket pack, ServerDispatcher dispatcher)
	{
		this.socket = socket;
		this.pack =pack;
		this.dispatcher = dispatcher;
	}
	
	
	@Override
	public void run() {
		 Thread.currentThread().setName("udphandler");
		String message = new String(pack.getData(), 0, pack.getLength());
		List<String> clients = parseMessage(message);
		if (clients != null) {
			InetAddress IPAddress = pack.getAddress();
			int port = pack.getPort();
			/*
			 * try { Thread.currentThread(); Thread.sleep(10000); } catch
			 * (InterruptedException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
			try {
				Collections.sort(clients);
				for (String name : clients) {
					sendAnswer(name, IPAddress, port);
				}
				//end list -> let the client know 
				sendAnswer("end", IPAddress, port);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	private List<String> parseMessage(String message)
	{		
		 if(message.equals("!list"))
		 {
			 List<String> clientNames = new ArrayList<>();
			 //get all clients online
			 List<ClientInfo> clients = dispatcher.getClients();
			 for(ClientInfo ci: clients)
			 {
				 if(ci.loggedIn)
				 {
					 clientNames.add(ci.userName);
				 }				
			 }
			 return clientNames;
		 }
		 return null;
	}
	private void sendAnswer(String message, InetAddress address, int port) throws IOException
	{
		  byte[] sendData = new byte[sendBufferSize];		 
          sendData = message.getBytes();
          DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, address, port);
          socket.send(sendPacket);
	}

}
