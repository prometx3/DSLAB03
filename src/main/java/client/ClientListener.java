package client;

import java.io.*;
import java.net.*;
import java.util.Arrays;

import cli.Shell;
 
public class ClientListener extends Thread
{
    private BufferedReader mIn;
    private PrintWriter mOut;
    private Socket socket;
    private Client client;
    
  
    public ClientListener(Socket socket, Client client)
    throws IOException
    {
    	this.client = client;
        this.mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.mOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.socket = socket;
    }
 
  
    public void run()
    {
		//System.out.println("ClientListener is waiting for input!");
		try {

			String message = mIn.readLine();
			if (message == null)
				return;

			client.writeLine(message);
			mOut.println("!ack");
			mOut.flush();
			
			//close streams and socket
			mIn.close();
			mOut.close();
			socket.close();


		} catch (IOException ioex) {
			// Problem reading from socket (communication is broken)
			client.writeLine(ioex.toString());
		}

    }
 
}
