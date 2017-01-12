package security;

import java.io.IOException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public interface SecureChannel {
	
	public void sendMessage(byte[] message) throws EncryptionException;
	
	public String receiveMessage() throws IOException, EncryptionException;
	
	

}
