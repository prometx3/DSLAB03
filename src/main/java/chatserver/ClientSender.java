package chatserver;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import security.Base64Channel;
import security.BasicTCPChannel;
import security.EncryptionException;
import security.SecureChannel;
 
public class ClientSender
{
    private PrintWriter mOut;
    private SecureChannel secureChannel;
    
    public ClientSender(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher)
    throws IOException
    {    
        Socket socket = aClientInfo.mSocket;
       // mOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
       
    }
 
    /**
     * 
     */
    public synchronized void sendMessage(String aMessage)
    {
    	// mOut.println(aMessage);
         //mOut.flush();  
    	if(secureChannel == null)
    	{
    		System.out.println("secureChannel has not been set!");
    		return;
    	}
    	//send message with secure channel
    	try {
			this.secureChannel.sendMessage(aMessage.getBytes(Charset.forName("UTF-8")));
		} catch (EncryptionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
    
    public synchronized void setSecureChannel(SecureChannel secureChannel)
    {
    	this.secureChannel = secureChannel;
    }
 
}
