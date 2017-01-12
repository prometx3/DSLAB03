package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import security.Base64Channel;
import security.BasicTCPChannel;
import security.EncryptionException;
import security.SecureChannel;
import util.Keys;
import cli.Shell;

public class ClientCommunication extends Thread {
	
	private Socket sock;			
	private DatagramSocket udpSocket;
	private Client client;		
	private BufferedReader in;		
	private PrintWriter out;		
	
	private SecureChannel secureChannel;
	
	private String lastPubMsg;
	
	public ClientCommunication(Socket sock, DatagramSocket udpSock, Client client) throws IOException {
		this.sock = sock;
		this.udpSocket = udpSock;
		this.client = client;
		
		//in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		//out = new PrintWriter(sock.getOutputStream(), true);
		
		lastPubMsg = "";
	}

	public void send(String msg) throws EncryptionException {
		if(secureChannel == null)
		{
			throw new EncryptionException("SecureChannel not set!");
		}
	
		//send with secure channel
		this.secureChannel.sendMessage(msg.getBytes(Charset.forName("UTF-8")));		
	}
	public String receiveMessage() throws IOException
	{
		return in.readLine();
	}
	public List<String> getList( String msg, String address, int port) throws IOException
	{
			InetAddress IPAddress = InetAddress.getByName(address);
			byte[] sendData = new byte[msg.getBytes().length];
			byte[] receiveData = new byte[1024];
			sendData = msg.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, IPAddress, port);
			
			this.udpSocket.send(sendPacket);
			List<String> clients = new ArrayList<>();
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			while(true)
			{
				this.udpSocket.receive(receivePacket);
				String answer = new String(receivePacket.getData(),0, receivePacket.getLength());
				if(answer.equals("end"))
				{
					break;
				}
				clients.add(answer);				
			}
			return clients;
	}
	public void run() {
		Thread.currentThread().setName("main");
		if(secureChannel == null)
		{
			client.writeLine("SecureChannel not set!");
			return;
		}
		// Get lines from server; print to console
		try {
			String line;
			while ((line = this.secureChannel.receiveMessage()) != null && !Thread.currentThread().isInterrupted()) {				
				if (line.equals("close")) {
					//this.close();
					client.exit();
					return;
				}
				parseMessage(line);				
			}
		}
		catch (IOException e) {		
			client.writeLine(e.getMessage() + ": Cleaning ClientCommunication up!");				
		} catch (EncryptionException e) {
			// TODO Auto-generated catch block
			client.writeLine(e.getMessage() + ": Cleaning ClientCommunication up!");	
		}
		// Clean up
		try {
			this.close();
		}
		catch (Exception e) {
			client.writeLine(e.getMessage());
		}
	}	
	
	private void parseMessage(String msg)
	{
		String[] split = msg.split("\\s");
		
		if (split.length >= 2) {
			String command = split[0];
			switch (command) {
			case "!public": {
				// server sends public messages coded with !send command
				// so we can save the last message
				this.lastPubMsg = arrayToString(split, 1);
				client.writeLine(this.lastPubMsg);
				return;
			}		
			}
		}
		//if it has not been public message
		//add it to the blocking queue
		client.addToQueue(msg);
			
	
	}
	public String arrayToString(String[] array,int indexToStart)
	{
		StringBuilder builder = new StringBuilder();
		for(int i = indexToStart; i < array.length;i++) {
		    builder.append(array[i] + " ");
		}
		return builder.toString();
	}
	
	public String getLastMsg()
	{
		return this.lastPubMsg;
	}
	
	public void setSecureChannel(SecureChannel secChannel)
	{
		this.secureChannel = secChannel;
	}
	
	public void close() throws IOException, NullPointerException, EncryptionException {
		Thread.currentThread().interrupt();
		
		//this.in.close();
		//this.out.close();
		if(!this.sock.isClosed())
		{
			this.sock.close();
		}
		if(!this.udpSocket.isClosed())
		{
			this.udpSocket.close();
		}
	}
}
