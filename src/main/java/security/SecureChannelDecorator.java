package security;

import java.io.IOException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public abstract class SecureChannelDecorator implements SecureChannel {
	
	protected SecureChannel basicChannel;
	
	public SecureChannelDecorator(SecureChannel bC) {
		this.basicChannel = bC;
	}
	
	public void sendMessage(byte[] message) throws EncryptionException
	{
		this.basicChannel.sendMessage(message);
	}
	
	public String receiveMessage() throws IOException, EncryptionException
	{
		return this.basicChannel.receiveMessage();		
	}
	
}
