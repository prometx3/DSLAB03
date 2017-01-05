package chatserver;

import java.io.*;
import java.net.*;
import java.util.*;
 
public class ClientSender
{
    private PrintWriter mOut;
 
    public ClientSender(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher)
    throws IOException
    {    
        Socket socket = aClientInfo.mSocket;
        mOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }
 
    /**
     * 
     */
    public synchronized void sendMessage(String aMessage)
    {
    	 mOut.println(aMessage);
         mOut.flush();      
    }
 
}
