package security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class BasicTCPChannel implements SecureChannel {

	private Socket socket;
	//writer for sending tcp messages
	private PrintWriter out;		
	//reader for receiving messages
	private BufferedReader in;
	
	public BasicTCPChannel(Socket sock) throws IOException
	{
		this.socket = sock;
		//setup reader and writer with socket
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}
	

	@Override
	public String receiveMessage() throws IOException {		
		String line = this.in.readLine();
		return line;
		//return this.in.readLine();
	}


	@Override
	public void sendMessage(byte[] message) throws EncryptionException {
		//convert to strind and send 
		String msg = new String(message,Charset.forName("UTF-8"));
		this.out.println(msg);		
	}
	
    public void close()
    {
    	try
    	{
    		this.socket.close();
    	}
    	catch(Exception e)
    	{
    		
    	}
    }

	

}
