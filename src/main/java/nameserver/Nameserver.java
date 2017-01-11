package nameserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import cli.Command;
import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;
	private Thread shellThread;

	private boolean exit = false;

	private INameserver remoteObject;
	private String domain;

	private Registry registry;
	private INameserver exportObject;

	private NameserverList servers = new NameserverList();
	private UserInfoList userInfos = new UserInfoList();

	public Registry getRegistry() {
		return this.registry;
	}

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
	public Nameserver(String componentName, Config config, InputStream userRequestStream,
			PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		this.shell = new Shell(this.componentName, this.userRequestStream, this.userResponseStream);
		this.shell.register(this);
	}

	@Override
	public void run() {
		new Thread(this.shell).start();

		this.domain = config.listKeys().contains("domain") ? config.getString("domain") : "";
		this.exportObject = new NameserverRemote(this.domain, this.servers, this.userInfos);
		try {
			this.remoteObject = (INameserver) UnicastRemoteObject.exportObject(this.exportObject, 0);
			if (!this.config.listKeys().contains("domain")) {
				this.registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
				this.registry.bind(config.getString("root_id"), this.remoteObject);
			} else {

				this.registry = LocateRegistry.getRegistry(config.getString("registry.host"),
						config.getInt("registry.port"));

				INameserver root = (INameserver) this.registry.lookup(config.getString("root_id"));

				String[] reversed = domain.split("\\.");
				if (reversed.length > 1) {
					domain = "";
					for (int i = reversed.length - 1; i >= 0; i--) {
						domain = domain + "." + reversed[i];
					}
					domain = domain.substring(1, domain.length());
				}

				root.registerNameserver(domain, this.remoteObject, root);
			}

		} catch (RemoteException e) {
			throw new RuntimeException("Error while starting server." + e.getMessage(), e);
		} catch (AlreadyBoundException e) {
			throw new RuntimeException("Error while binding remote object to registry.", e);
		} catch (NotBoundException e) {
			throw new RuntimeException("Error while looking for server-remote-object.", e);
		} catch (AlreadyRegisteredException e) {
			throw new RuntimeException("Error while registering server.", e);
		} catch (InvalidDomainException e) {
			throw new RuntimeException("Error while validating domain.", e);
		}

		this.userResponseStream.println("Server is up: " + (this.domain.equals("") ? "Root" : this.domain));

		while (!exit) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Cannot be handled!
			}
		}
	}

	// INameserverCli
	@Command
	@Override
	public String exit() throws IOException {

		this.exit = true;

		try {
			UnicastRemoteObject.unexportObject(this.exportObject, true);
		} catch (NoSuchObjectException e) {
			System.err.println("Error while unexporting object: " + e.getMessage());
		}

		if (!this.config.listKeys().contains("domain")) {
			try {
				registry.unbind(config.getString("root_id"));
			} catch (Exception e) {
				System.err.println("Error while unbinding object: " + e.getMessage());
			}
		}

		return "Exited " + (this.domain.equals("") ? "Root" : this.domain);
	}

	@Command
	@Override
	public String nameservers() throws IOException {
		return this.servers.getNameserverListText();
	}

	@Command
	@Override
	public String addresses() throws IOException {
		return this.userInfos.getAdressesText();
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]), System.in, System.out);

		new Thread(nameserver).start();
	}

}
