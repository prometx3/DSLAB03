package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import util.Config;
import cli.Command;
import cli.Shell;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;
	private String privateMessage;
	private String userName;
	private Socket socket;
	private DatagramSocket udpSocket;
	
	private String lastMsg;
	
	//client server for accepting private tcp messages if registered ip
	private ClientServer clientServer;
	
	private BlockingQueue<String> responsesQueue;
	
	private ClientCommunication clientCommunicator;
	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		privateMessage = "";
		userName = "";
		lastMsg = "";
		
		this.responsesQueue = new ArrayBlockingQueue<String>(1024);
		/*
		 * First, create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		/*
		 * Next, register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		shell.register(this);	
		
	}

	@Override
	public void run() {
		Thread.currentThread().setName("client");
		new Thread(shell).start();
		try {
			socket = new Socket(config.getString("chatserver.host"),
					config.getInt("chatserver.tcp.port"));
			udpSocket = new DatagramSocket();
			clientCommunicator = new ClientCommunication(socket, udpSocket,
					this);
			clientCommunicator.start();
			
			
			System.out.println(getClass().getName()
					+ " up and waiting for commands!");
			
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
		
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		clientCommunicator.send("!login " + username + " " + password);
		try {
			String response = responsesQueue.take();
			String checkedResp = checkResponse("!login","!!login",response);
			if(checkedResp != null)
			{
				this.userName = username;
			}
		} catch (InterruptedException e) {		
			shell.writeLine(e.getMessage());
		} catch (IllegalResponseException e) {
			shell.writeLine(e.getMessage());
		}
		//writeLine(clientCommunicator.receiveMessage());
		//writeLine(in.readLine());
		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {
		clientCommunicator.send("!logout");
		try {
			String response = responsesQueue.take();
			checkResponse("!logout","!!logout",response);			
		} catch (InterruptedException e) {		
			shell.writeLine(e.getMessage());
		} catch (IllegalResponseException e) {
			shell.writeLine(e.getMessage());
		}
		//also end client listener
		if(clientServer != null)
		{
			clientServer.interrupt();
		}
		return null;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		clientCommunicator.send("!send " + message);
		return null;
	}

	@Override
	@Command
	public String list() throws IOException {
		// TODO Auto-generated method stub
		List<String> clients = clientCommunicator.getList("!list", config.getString("chatserver.host"), config.getInt("chatserver.udp.port"));	
		for(String s:clients)
		{
			writeLine(s + " *online*");
		}
		return null;
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		//lookup address from user -> server response with special command which
		//will trigger callback from client communicator 
		//clientCommunicator.send("lookupMSG " + username);
		//set private message for callback 
		String addressPort = this.lookAddressUp(username);
		if (addressPort != null) {
			// got an address
			String[] addrPort = addressPort.split(":");

			writeLine("Sending private message to: " + addrPort[0] + ":"
					+ addrPort[1]);
			sendTCPMessage(addrPort[0], Integer.valueOf(addrPort[1]),this.userName + " whispers: " + message);

		}
		return null;
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {		
		lookAddressUp(username);		
		return null;
	}
	private String lookAddressUp(String username) throws IOException
	{
		clientCommunicator.send("!lookup " + username);
		try {
			String response = responsesQueue.take();
			String checkedResp = checkResponse("!lookup","!!lookup",response);
			if(checkedResp != null)
			{
				return checkedResp;
			}
		} catch (InterruptedException e) {		
			shell.writeLine(e.getMessage());
		} catch (IllegalResponseException e) {
			shell.writeLine(e.getMessage());
		}
		return null;
	}
	@Override
	@Command
	public String register(String privateAddress) throws IOException {		
		//check if username is set -> only set if already logged in
		if(this.userName.equals(""))
		{
			shell.writeLine("Not logged in!");
			return null;
		}
		//open server socket
		String[] split = privateAddress.split(":");
		if(split.length == 2)
		{
			String port = split[1];
			try
			{
				clientServer = new ClientServer(Integer.valueOf(port),this);
			}
			catch(Exception e)
			{
				shell.writeLine(e.getMessage());
				return null;
			}
			clientServer.start();
			clientCommunicator.send("!register " + privateAddress);
			try {
				String response = responsesQueue.take();
				checkResponse("!register","!!register",response);
				
			} catch (InterruptedException e) {		
				shell.writeLine(e.getMessage());
			} catch (IllegalResponseException e) {
				shell.writeLine(e.getMessage());
			}
		}		
		else
		{
			shell.writeLine("Wrong (IP:Port) Format!");
		}
		return null;
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		if(clientCommunicator.getLastMsg().equals(""))
		{
			shell.writeLine("No message received!");
			return null;
		}		
		shell.writeLine(clientCommunicator.getLastMsg());
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		
		shell.writeLine("Exiting client!");		
		//closes sockets 
		clientCommunicator.close();
				
		
		//socket.close();
		//udpSocket.close();
		
		if(clientServer != null)
		{
			clientServer.interrupt();
		}
		shell.writeLine("Client closed, please exit shell!");
		shell.close();		
		
		return null;
	}
	
	public void connectionLost()
	{
		try {
			this.exit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * sends one tcp message to the given host(and port)
	 * @param host
	 * @param port
	 */
	public void sendTCPMessage(String host,int port, String msg)
	{		
		try {
			Socket tcpSock = new Socket(host,port);
			PrintWriter pw = new PrintWriter(tcpSock.getOutputStream(), true);
			pw.println(msg);
			pw.flush();
			pw.close();
			tcpSock.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeLine(String msg)
	{
		try {
			shell.writeLine(msg);
			//System.out.println(msg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void setUserName(String userName)
	{
		this.userName = userName;
	}
	
	
	public void addToQueue(String msg)
	{
		this.responsesQueue.add(msg);
	}
	
	private String checkResponse(String check, String fail, String response) throws IllegalResponseException, IOException
	{
		String[] split = response.split("\\s");
		if(split.length >= 2)
		{
			if(split[0].equals(check))
			{
				shell.writeLine(arrayToString(split,1));
				return arrayToString(split,1);
			}
			else if(split[0].equals(fail))
			{
				shell.writeLine(arrayToString(split,1));
				return null;
			}
		}
		throw new IllegalResponseException("Illegal response from Server!");
	}
	public String arrayToString(String[] array,int indexToStart)
	{
		StringBuilder builder = new StringBuilder();
		for(int i = indexToStart; i < array.length;i++) {
			if(i != (array.length-1))
			{
				builder.append(array[i] + " ");
			}
			else
			{
				builder.append(array[i]);
			}
		}
		return builder.toString();
	}
	
	
	
	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		
		new Thread((Runnable) client).start();
	}
	
	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
