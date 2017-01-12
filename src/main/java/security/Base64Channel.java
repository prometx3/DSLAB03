package security;

import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class Base64Channel extends SecureChannelDecorator{
	
	private String encMSG;
	
	public Base64Channel(SecureChannel bC) {
		super(bC);
	
	}
	
	
	/**
	 * encrypts message with base64 and sends it afterwards
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	@Override
	public void sendMessage(byte[] message) throws EncryptionException {		
		this.basicChannel.sendMessage(Base64Channel.encodeBase64Byte(message));
	}
	
	
	/**
	 * receives the base64 encoded message, decrypts it and returns the string
	 * @throws EncryptionException 
	 */
	public String receiveMessage() throws IOException, EncryptionException
	{
		
		return this.basicChannel.receiveMessage();
	}
	
	public String encryptedMessage()
	{
		return this.encMSG;	
	}
	
	public static String encodeBase64(String msg)
	{
		byte[] messageBytes = msg.getBytes(Charset.forName("UTF-8"));
		//encrypte bytes to base64
		byte[] base64Message = Base64.encode(messageBytes);
		return new String(base64Message,Charset.forName("UTF-8"));
	}
	public static String encodeBase64(byte[] msg)
	{		
		//encrypte bytes to base64
		byte[] base64Message = Base64.encode(msg);
		return new String(base64Message,Charset.forName("UTF-8"));
	}
	public static byte[] encodeBase64Byte(String msg)
	{
		byte[] messageBytes = msg.getBytes(Charset.forName("UTF-8"));
		//encrypte bytes to base64
		byte[] base64Message = Base64.encode(messageBytes);
		return base64Message;
	}
	public static byte[] encodeBase64Byte(byte[] msg)
	{
		//encrypte bytes to base64
		byte[] base64Message = Base64.encode(msg);
		return base64Message;
	}
	
	public static String decodeBase64(String msg)
	{
		byte[] messageBytes = msg.getBytes(Charset.forName("UTF-8"));
		byte[] message = Base64.decode(messageBytes);
		return new String(message,Charset.forName("UTF-8"));
	}
	public static byte[] decodeBase64Byte(String msg)
	{
		byte[] messageBytes = msg.getBytes(Charset.forName("UTF-8"));
		byte[] message = Base64.decode(messageBytes);
		return message;
	}
	
}
