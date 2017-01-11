package chatserver;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Arrays;

import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

public class ClientListener extends Thread {
	private ServerDispatcher mServerDispatcher;
	private ClientInfo mClientInfo;
	private BufferedReader mIn;
	private boolean shutdown = false;
	private INameserverForChatserver nameserverRoot;

	public ClientListener(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher,
			INameserverForChatserver nameserverRoot) throws IOException {
		this.nameserverRoot = nameserverRoot;
		mClientInfo = aClientInfo;
		mServerDispatcher = aServerDispatcher;
		Socket socket = aClientInfo.mSocket;
		mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	/**
	 * Until interrupted, reads messages from the client socket
	 */
	public void run() {
		Thread.currentThread().setName("ClientListener");
		try {
			while (!Thread.currentThread().isInterrupted() && !shutdown) {
				String message = mIn.readLine();
				if (message == null)
					break;

				// process message from client
				parseMessage(message);
			}
		} catch (IOException ioex) {
			// Problem reading from socket (communication is broken)
			System.out.println(ioex.getMessage());
			shutdown = true;
		}
		try {
			this.shutdown = true;
			this.mIn.close();
			this.mClientInfo.mSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		mServerDispatcher.deleteClient(mClientInfo);
	}

	private void parseMessage(String msg) {
		String[] parts = msg.split("\\s");
		// check if msg has form <command> <info>
		if (parts.length >= 2) {
			String command = parts[0];

			switch (command) {

			case "!login": {
				String response = "!!login Wrong username or password.";
				if (mClientInfo.loggedIn || mServerDispatcher.alreadyLoggedIn(parts[1])) {
					// already logged in, either this client oder another client
					// with the same name
					// has already logged in -> dont allow double logged in
					// users
					response = "!!login Already logged in.";
					mClientInfo.mClientSender.sendMessage(response);
					return;
				}
				if (parts.length == 3) {
					if (mServerDispatcher.checkClientLogin(parts[1], parts[2])) {
						// set clientinfo loggedIn to true -> user is loggedIn
						mClientInfo.loggedIn = true;
						// set username
						mClientInfo.userName = parts[1];
						// send successful response including username
						response = "!login Successfully logged in.";
					}

				}
				Thread.currentThread();

				mClientInfo.mClientSender.sendMessage(response);
				break;

			}
			case "!send": {
				if (mClientInfo.loggedIn) {
					// send to all clients
					StringBuilder builder = new StringBuilder();
					builder.append("!public ");
					builder.append(mClientInfo.userName + " says: ");
					for (int i = 1; i < parts.length; i++) {
						builder.append(parts[i] + " ");
					}
					mServerDispatcher.broadcast(mClientInfo, builder.toString());
				} else {
					mClientInfo.mClientSender.sendMessage("!public Not logged in.");
				}
				break;
			}
			case "!register": {
				String response = "!!register Not logged in!";
				if (mClientInfo.loggedIn) {
					String[] addrPort = parts[1].split(":");

					try {
						if (addrPort.length == 2) {
							String reversedUsername = mClientInfo.userName;
							String[] reversed = reversedUsername.split("\\.");
							if (reversed.length > 1) {
								reversedUsername = "";
								for (int i = reversed.length - 1; i >= 0; i--) {
									reversedUsername = reversedUsername + "." + reversed[i];
								}
								reversedUsername = reversedUsername.substring(1, reversedUsername.length());
							}

							nameserverRoot.registerUser(reversedUsername, parts[1]);
							response = "!!register Successfully registered address for " + mClientInfo.userName + ".";
						} else {
							response = "!!register Wrong Address, Port Format! (IP:port)";
						}
					} catch (RemoteException e) {
						System.out.println("Internal problemo");
						response = "!!register Server Error!";
					} catch (AlreadyRegisteredException e) {
						System.out.println("User problemo");
						response = "!!register already registered!";
					} catch (InvalidDomainException e) {
						System.out.println("Domain problemo");
						response = "!!register can't be handled!";
					}
					// if (addrPort.length == 2) {
					// mClientInfo.privateAddress = parts[1];
					// response = "!register Successfully registered address for
					// " + mClientInfo.userName + ".";
					// } else {
					// response = "!!register Wrong Address, Port Format!
					// (IP:port)";
					// }
				}
				mClientInfo.mClientSender.sendMessage(response);
				break;
			}
			case "!lookup": {
				String response = "!!lookup Not logged in!";
				if (mClientInfo.loggedIn) {
					String address = mServerDispatcher.lookUpAddress(parts[1]);
					if (address != null) {
						response = "!lookup " + address;
					} else {
						response = "!!lookup Wrong username or user not registered.";
					}
				}
				mClientInfo.mClientSender.sendMessage(response);
				break;
			}

			}
		} else if (parts.length == 1) {
			// logout command consists of only one part, which is the command
			switch (parts[0]) {
			case "!logout": {
				String response = "";
				if (!mClientInfo.loggedIn) {
					response = "!!logout Not logged in.";
				} else {
					mClientInfo.loggedIn = false;
					mClientInfo.userName = "";
					response = "!logout Successfully logged out.";
				}
				mClientInfo.mClientSender.sendMessage(response);
				break;
			}
			}
		}

	}

}
