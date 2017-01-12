package chatserver;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import security.AESChannel;
import security.EncryptionException;
import security.RSAChannel;
import security.SecureChannel;
import security.Base64Channel;
import security.BasicTCPChannel;
import util.Config;
import util.Keys;
 
public class ClientListener extends Thread
{
    private ServerDispatcher mServerDispatcher;
    private ClientInfo mClientInfo;
    private BufferedReader mIn;  
    private boolean shutdown = false;
	private INameserverForChatserver nameserverRoot;
	
    //encryption
    private boolean RSA = true;
    private SecureChannel secureChannel;
    private PrivateKey privateKey;
    
    private Config conf;
    
    public ClientListener(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher, INameserverForChatserver nameserverRoot, PrivateKey privateKey, Config conf)
    throws IOException
    {
		this.nameserverRoot = nameserverRoot;
        this.mClientInfo = aClientInfo;
        this.mServerDispatcher = aServerDispatcher;
        this.privateKey = privateKey;
        this.conf = conf;
        Socket socket = aClientInfo.mSocket;
        
      //  mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        //first 2 messages exchanged between sever and client must be RSA encrypted so we can set the channel to RSA
        try {
			this.secureChannel = new RSAChannel(new Base64Channel(new BasicTCPChannel(socket)),privateKey);
		} catch (EncryptionException e) {			
			System.out.println(e.getMessage());
		}      
    }
 
    /**
     * Until interrupted, reads messages from the client socket
     */
    public void run()
    {    	
    	Thread.currentThread().setName("ClientListener");
        try {
			if (handleHandshake()) {
				//if the handshake was handled correctly -> messages will be send over the secure AES channel
				while (!Thread.currentThread().isInterrupted() && !shutdown) {
					// String message = mIn.readLine();
					// receiving message through secure channel
					String message = this.secureChannel.receiveMessage();
					if (message == null)
						break;

					// process message from client
					parseMessage(message);
				}
			}
        } catch (IOException ioex) {
           // Problem reading from socket (communication is broken)
        	System.out.println(ioex.getMessage());        	
        	shutdown = true;        	
        } catch (EncryptionException e) {
        	System.out.println(e.getMessage());        	
        	shutdown = true;        	
		}
        try {
        	 this.shutdown = true;
			 //this.mIn.close();
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
				/*String response = "!!login Wrong username or password.";
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
				*/break;

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
					//reset secure channel to accept RSA encrypted messages 
					try {
						this.secureChannel = new RSAChannel(new Base64Channel(
								new BasicTCPChannel(mClientInfo.mSocket)),
								privateKey);
					} catch (Exception e) {
						System.out.println(e.getMessage());
					} 
				}
				mClientInfo.mClientSender.sendMessage(response);
				break;
			}
			}
		}

    	
    }
    private boolean handleHandshake() throws IOException, EncryptionException
    {
    	// receiving message through secure channel
		String message = this.secureChannel.receiveMessage();
		String[] parts = message.split("\\s");
		if(parts.length > 2)
		{
			//load the public key for the given username -> if the key cant be loaded -> print error	
			PublicKey pubUserKey = null;
			String username = parts[1];
			
			try {
				pubUserKey = Keys.readPublicPEM(new File(conf.getString("keys.dir") + "/" + username + ".pub.pem"));
			} catch (IOException e1) {
				//TODO: send response to user 
				System.out.println(e1.getMessage());
				return false;
			}
			if(this.mServerDispatcher.alreadyLoggedIn(username))
			{
				//already logged in -> 
				SecureChannel secChannel =  new RSAChannel(new Base64Channel(new BasicTCPChannel(mClientInfo.mSocket)),pubUserKey,privateKey);
				secChannel.sendMessage("!fail Already logged in!".getBytes(Charset.forName("UTF-8")));
				return false;
			}
			//the reponse syntac is !ok <client-challenge> <chatserver-challenge> <secret-key> <iv-parameter>
			String response = "!ok " + parts[2];
			// generates a 32 byte secure random number as chatserver challenge
			SecureRandom secureRandom = new SecureRandom();
			final byte[] chatserverChallenge = new byte[32];
			secureRandom.nextBytes(chatserverChallenge);
			
			// generate the IV parameter
			final byte[] ivParameter = new byte[16];
			secureRandom.nextBytes(ivParameter);
			
			//generate the secret key
			KeyGenerator generator;
			try {
				generator = KeyGenerator.getInstance("AES");
				// KEYSIZE is in bits
				generator.init(256);
				SecretKey key = generator.generateKey();
				
				String chatserverChallengeB64 = Base64Channel.encodeBase64(chatserverChallenge);
				String secretKey = Base64Channel.encodeBase64(key.getEncoded());
				String ivParam = Base64Channel.encodeBase64(ivParameter);
				response += " " + chatserverChallengeB64 + " " + secretKey + " " + ivParam;					
				
				//send the whole response string back to the client, encrypted with the clients public key and RSA
				SecureChannel secChannel =  new RSAChannel(new Base64Channel(new BasicTCPChannel(mClientInfo.mSocket)),pubUserKey,privateKey);
				secChannel.sendMessage(response.getBytes(Charset.forName("UTF-8")));
				//not switch to AES -> next message should be AES encoded and should just be the server challenge
				secChannel = new AESChannel(new Base64Channel(new BasicTCPChannel(mClientInfo.mSocket)),secretKey,ivParam);
				String clientResponse = secChannel.receiveMessage();
				if(clientResponse.equals(chatserverChallengeB64))
				{
					//everything worked fine -> the communication between client and server should now be done over the AES channel
					this.secureChannel = secChannel;
					mClientInfo.mClientSender.setSecureChannel(secureChannel);
					//login user
					// set clientinfo loggedIn to true -> user is loggedIn
					mClientInfo.loggedIn = true;
					// set username
					mClientInfo.userName = username;
					System.out.println("AES Encryption Channel should be established!");
					return true;
				}
				
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				throw new EncryptionException(e.getMessage());
			}
		}
			
		
    	return false;
    }
 
}
