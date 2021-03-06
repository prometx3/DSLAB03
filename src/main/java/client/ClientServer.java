package client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;


 
public class ClientServer extends Thread
{ 
    private ServerSocket serverSocket;
    private List<ClientListener> listener;
    private Client client;
 
    public ClientServer(int serverPort, Client client)
    throws IOException
    {
        this.serverSocket = new ServerSocket(serverPort);
        this.client = client;
        this.listener = new ArrayList<>();        
    }
 
   
    public void run()
    {
    	try {
			getConnections();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void getConnections() throws IOException {
		
		while (true) {
			if(Thread.currentThread().isInterrupted())
			{
				client.writeLine("Thread interrupted!");
				for(ClientListener cl: listener)
				{
					cl.interrupt();
				}
				return;
			}
			try {
				Socket socket = serverSocket.accept();
				ClientListener cl = new ClientListener(socket,client);
				cl.start();
				listener.add(cl);
			} catch(SocketException e)
			{
				//do nothing
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	
    }
    
    public void close() throws IOException
    {
    	this.serverSocket.close();
    }
 
}
