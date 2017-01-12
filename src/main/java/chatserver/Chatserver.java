package chatserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
<<<<<<< HEAD
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
=======
import java.security.PrivateKey;
import java.security.Security;
>>>>>>> stage2_dev
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cli.Command;
import cli.Shell;
import nameserver.INameserverForChatserver;
import util.Config;
import util.Keys;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private Shell shell;
	private ServerDispatcher dispatcher;

	// tcp socket
	private ServerSocket serverSocket;
	// udp socket
	private DatagramSocket serverUDPSocket;
	private UDPListener udpListener;

	private boolean shutdown = false;
	// threading
	private ExecutorService executor;

	private INameserverForChatserver nameserver;

	
	//encryption
	private PrivateKey privateKey;


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
	public Chatserver(String componentName, Config config, InputStream userRequestStream,
			PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

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

		Config conf = new Config("user");

		try {
			Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
			nameserver = (INameserverForChatserver) registry.lookup(config.getString("root_id"));

			dispatcher = new ServerDispatcher(conf, nameserver);
		} catch (RemoteException e) {
			throw new RuntimeException(
					"Error while obtaining registry/server-remote-object.", e);
		} catch (NotBoundException e) {
			throw new RuntimeException(
					"Error while looking for server-remote-object.", e);
		}
		executor = Executors.newCachedThreadPool();
	}

	@Override
	public void run() {
		Thread.currentThread().setName("chatserver");
		// TODO
		new Thread(shell).start();

		this.userResponseStream.println(getClass().getName() + " up and waiting for commands!");

		// setting up server sockets and waiting for connections

		try {
			// load private key for the server
			privateKey = Keys.readPrivatePEM(new File(config.getString("key")));
			// setting up server sockets and waiting for connections
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			serverUDPSocket = new DatagramSocket(config.getInt("udp.port"));
			udpListener = new UDPListener(serverUDPSocket, dispatcher, shell);
			udpListener.start();

			// handle incoming connections from clients
			getConnections();
		} catch (IOException e) {
			this.userResponseStream.println(getClass().getName() + ": " + e);
		}
	}

	public void getConnections() throws IOException {
		
			while (!shutdown) {
				if(Thread.currentThread().isInterrupted())
				{
					shell.writeLine("Thread interrupted!");
					break;
				}
				try {
					Socket socket = serverSocket.accept();
					ClientInfo clientInfo = new ClientInfo();
					clientInfo.mSocket = socket;
					ClientListener clientListener = new ClientListener(
							clientInfo, dispatcher, nameserver, privateKey,config);
					ClientSender clientSender = new ClientSender(clientInfo,
							dispatcher);
					clientInfo.mClientListener = clientListener;
					clientInfo.mClientSender = clientSender;
					// clientListener.start();
					executor.execute(clientListener);
					dispatcher.addClient(clientInfo);
				} 
				catch(SocketException e)
				{
					//could be thrown because exit closes the socket
					shell.writeLine(e.getMessage());
					shutdown = true;
					return;
				}
				catch (IOException ioe) {
					shell.writeLine(ioe.getMessage());
					shutdown = true;
					serverSocket.close();
					return;
				}
				

			}

		}

	}

	@Override
	@Command
	public String users() throws IOException {

		List<ClientInfo> clients = dispatcher.getClients();
		List<String> users = new ArrayList<>();
		for (ClientInfo ci : clients) {
			if (ci.loggedIn) {
				users.add(ci.userName + " online");
			}

		}
		Collections.sort(users);
		for (String s : users) {
			shell.writeLine(s);
		}
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		shell.writeLine("Exiting server!");
		dispatcher.exitCommand();
		this.udpListener.close();
		this.shutdown = true;
		this.serverSocket.close();

		executor.shutdown();
		try {
			if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
		// also close the shell
		shell.close();
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {

		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		
		String providerName = "BC";

	    if (Security.getProvider(providerName) == null) {
	      System.out.println(providerName + " provider not installed");
	    } else {
	      System.out.println(providerName + " is installed.");
	    }

		// TODO: start the chatserver
		new Thread((Runnable) chatserver).start();
	}

}
